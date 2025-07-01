@file:Suppress("TooGenericExceptionThrown")

package com.axians.eaf.eventing.consumer

import com.axians.eaf.eventing.config.NatsEventingProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import io.nats.client.Connection
import io.nats.client.JetStream
import io.nats.client.Message
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests for the idempotent projector functionality using Testcontainers. Tests the
 * complete end-to-end flow including database operations, idempotency, and tenant isolation.
 */
@Testcontainers
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [IdempotentProjectorIntegrationTest.TestConfig::class])
@ActiveProfiles("test")
class IdempotentProjectorIntegrationTest {
    @Configuration
    @EnableAsync
    @EnableConfigurationProperties(NatsEventingProperties::class)
    open class TestConfig {
        @Bean
        open fun taskExecutor(): org.springframework.core.task.TaskExecutor {
            val executor =
                org.springframework.scheduling.concurrent
                    .ThreadPoolTaskExecutor()
            executor.corePoolSize = 2
            executor.maxPoolSize = 2
            executor.setQueueCapacity(500)
            executor.setThreadNamePrefix("Async-")
            executor.initialize()
            return executor
        }

        @Bean
        open fun objectMapper(): ObjectMapper =
            ObjectMapper().apply {
                registerKotlinModule()
                registerModule(JavaTimeModule())
            }

        @Bean @Primary
        open fun mockConnection(): Connection = mockk(relaxed = true)

        @Bean @Primary
        open fun mockJetStream(): JetStream = mockk(relaxed = true)

        @Bean
        open fun idempotentProjectorService(
            processedEventRepository: ProcessedEventRepository,
            objectMapper: ObjectMapper,
        ): IdempotentProjectorService = IdempotentProjectorService(processedEventRepository, objectMapper)

        @Bean
        open fun eafProjectorEventHandlerProcessor(): EafProjectorEventHandlerProcessor =
            EafProjectorEventHandlerProcessor()

        @Bean
        open fun processedEventRepository(
            namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
        ): ProcessedEventRepository = JdbcProcessedEventRepository(namedParameterJdbcTemplate)

        @Bean
        open fun namedParameterJdbcTemplate(dataSource: DataSource): NamedParameterJdbcTemplate =
            NamedParameterJdbcTemplate(dataSource)

        @Bean
        open fun dataSource(): DataSource {
            postgres.start()
            val hikariConfig =
                com.zaxxer.hikari.HikariConfig().apply {
                    jdbcUrl = postgres.jdbcUrl
                    username = postgres.username
                    password = postgres.password
                    driverClassName = postgres.driverClassName
                }
            return com.zaxxer.hikari.HikariDataSource(hikariConfig)
        }
    }

    companion object {
        @Container
        @JvmStatic
        private val postgres =
            PostgreSQLContainer("postgres:15-alpine")
                .withDatabaseName("eaf_test")
                .withUsername("test")
                .withPassword("test")
    }

    @Autowired private lateinit var processedEventRepository: ProcessedEventRepository

    @Autowired private lateinit var idempotentProjectorService: IdempotentProjectorService

    @Autowired private lateinit var namedParameterJdbcTemplate: NamedParameterJdbcTemplate

    @Autowired private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        createProcessedEventsTable()
        ProjectorRegistry.clear()
    }

    @AfterEach
    fun tearDown() {
        namedParameterJdbcTemplate.jdbcTemplate.execute("TRUNCATE TABLE processed_events")
        ProjectorRegistry.clear()
    }

    @Test
    fun `should process event successfully and mark as processed`() =
        runTest {
            val projectorName = "test-projector"
            val eventId = UUID.randomUUID()
            val tenantId = "tenant-123"
            val event = TestUserCreatedEvent("user-1", "test@example.com", "Test User", Instant.now())
            val eventEnvelope =
                com.axians.eaf.eventing.EventEnvelope(
                    eventId = eventId.toString(),
                    eventType = event::class.java.simpleName,
                    timestamp = Instant.now(),
                    tenantId = tenantId,
                    payload = event,
                )
            val rawMessage = createMockMessage(eventEnvelope, "$tenantId.events.user.created")

            val testProjector = TestProjector()
            registerTestProjector(testProjector, projectorName, "events.user.created")

            assertFalse(processedEventRepository.isEventProcessed(projectorName, eventId, tenantId))

            idempotentProjectorService.processNatsMessage(projectorName, rawMessage)

            await().atMost(5, TimeUnit.SECONDS).until {
                runBlocking {
                    processedEventRepository.isEventProcessed(projectorName, eventId, tenantId)
                }
            }

            assertEquals(1, testProjector.processedEvents.size)
            assertEquals(event.userId, testProjector.processedEvents[0].userId)
        }

    @Test
    fun `should skip processing when event already processed (idempotency)`() =
        runTest {
            val projectorName = "test-projector"
            val eventId = UUID.randomUUID()
            val tenantId = "tenant-123"
            val event = TestUserCreatedEvent("user-1", "test@example.com", "Test User", Instant.now())
            val eventEnvelope =
                com.axians.eaf.eventing.EventEnvelope(
                    eventId = eventId.toString(),
                    eventType = event::class.java.simpleName,
                    timestamp = Instant.now(),
                    tenantId = tenantId,
                    payload = event,
                )
            val rawMessage = createMockMessage(eventEnvelope, "$tenantId.events.user.created")

            val testProjector = TestProjector()
            registerTestProjector(testProjector, projectorName, "events.user.created")

            processedEventRepository.markEventAsProcessed(projectorName, eventId, tenantId)

            idempotentProjectorService.processNatsMessage(projectorName, rawMessage)

            await().pollDelay(1, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted {
                assertEquals(0, testProjector.processedEvents.size)
            }
        }

    @Test
    fun `should enforce tenant isolation`() =
        runTest {
            val projectorName = "test-projector"
            val eventId = UUID.randomUUID()
            val tenant1 = "tenant-1"
            val tenant2 = "tenant-2"

            processedEventRepository.markEventAsProcessed(projectorName, eventId, tenant1)

            assertTrue(processedEventRepository.isEventProcessed(projectorName, eventId, tenant1))
            assertFalse(processedEventRepository.isEventProcessed(projectorName, eventId, tenant2))
        }

    @Test
    fun `should handle processing errors and nak message`() =
        runTest {
            val projectorName = "error-projector"
            val eventId = UUID.randomUUID()
            val tenantId = "tenant-123"
            val event = TestUserCreatedEvent("user-1", "test@example.com", "Test User", Instant.now())
            val eventEnvelope =
                com.axians.eaf.eventing.EventEnvelope(
                    eventId = eventId.toString(),
                    eventType = event::class.java.simpleName,
                    timestamp = Instant.now(),
                    tenantId = tenantId,
                    payload = event,
                )
            val rawMessage = createMockMessage(eventEnvelope, "$tenantId.events.user.created")

            val errorProjector = ErrorProjector()
            registerErrorProjector(errorProjector, projectorName)

            idempotentProjectorService.processNatsMessage(projectorName, rawMessage)

            await().atMost(5, TimeUnit.SECONDS).until {
                runBlocking {
                    !processedEventRepository.isEventProcessed(projectorName, eventId, tenantId)
                }
            }
        }

    @Test
    fun `should handle different projectors independently`() =
        runTest {
            // Given
            val eventId = UUID.randomUUID()
            val tenantId = "tenant-123"
            val projector1Name = "projector-1"
            val projector2Name = "projector-2"

            // When - mark event as processed for projector1 only
            processedEventRepository.markEventAsProcessed(projector1Name, eventId, tenantId)

            // Then - should be independent
            assertTrue(processedEventRepository.isEventProcessed(projector1Name, eventId, tenantId))
            assertFalse(processedEventRepository.isEventProcessed(projector2Name, eventId, tenantId))
        }

    @Test
    fun `processor should discover and register annotated projector`() {
        val testBean = AnnotatedTestProjector()
        val processor = EafProjectorEventHandlerProcessor()
        processor.postProcessAfterInitialization(testBean, "annotatedTestProjector")
        val registeredProjector =
            ProjectorRegistry.getProjector("AnnotatedTestProjector-handleUserCreated")
        assertTrue(registeredProjector != null)
        assertEquals("test.subject", registeredProjector?.subject)
        assertEquals(
            "AnnotatedTestProjector-handleUserCreated-consumer",
            registeredProjector?.durableName,
        )
    }

    private fun createMockMessage(
        payload: Any,
        subject: String,
    ): Message {
        val rawPayload = objectMapper.writeValueAsBytes(payload)
        val msg: Message = mockk()
        every { msg.data } returns rawPayload
        every { msg.subject } returns subject
        every { msg.ack() } returns Unit
        every { msg.nak() } returns Unit
        return msg
    }

    private fun createProcessedEventsTable() {
        namedParameterJdbcTemplate.jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS processed_events (
                projector_name VARCHAR(255) NOT NULL,
                event_id UUID NOT NULL,
                tenant_id VARCHAR(255) NOT NULL,
                processed_at TIMESTAMPTZ NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
                PRIMARY KEY (projector_name, event_id, tenant_id)
            );

            CREATE INDEX IF NOT EXISTS idx_processed_events_tenant_projector
            ON processed_events (tenant_id, projector_name);
            """.trimIndent(),
        )
    }

    private fun registerTestProjector(
        projector: TestProjector,
        projectorName: String,
        subject: String,
    ) {
        val method =
            TestProjector::class.java.getDeclaredMethod(
                "handleUserCreated",
                Any::class.java,
                UUID::class.java,
                String::class.java,
            )

        ProjectorRegistry.registerProjector(
            ProjectorDefinition(
                originalBean = projector,
                originalMethod = method,
                beanName = "testProjectorBean",
                projectorName = projectorName,
                subject = subject,
                durableName = "$projectorName-consumer",
                deliverPolicy = io.nats.client.api.DeliverPolicy.All,
                maxDeliver = 5,
                ackWait = 30000,
                maxAckPending = 10,
                eventType = TestUserCreatedEvent::class.java,
            ),
        )
    }

    private fun registerErrorProjector(
        projector: ErrorProjector,
        projectorName: String,
    ) {
        val method =
            ErrorProjector::class.java.getDeclaredMethod(
                "handleUserCreated",
                Any::class.java,
                UUID::class.java,
                String::class.java,
            )

        ProjectorRegistry.registerProjector(
            ProjectorDefinition(
                originalBean = projector,
                originalMethod = method,
                beanName = "testErrorProjectorBean",
                projectorName = projectorName,
                subject = "events.user.created",
                durableName = "$projectorName-consumer",
                deliverPolicy = io.nats.client.api.DeliverPolicy.All,
                maxDeliver = 5,
                ackWait = 30000,
                maxAckPending = 10,
                eventType = TestUserCreatedEvent::class.java,
            ),
        )
    }
}

/** Test projector that collects processed events. */
class TestProjector {
    val processedEvents = mutableListOf<TestUserCreatedEvent>()

    fun handleUserCreated(
        @Suppress("UNUSED_PARAMETER") event: Any,
        @Suppress("UNUSED_PARAMETER") eventId: UUID,
        @Suppress("UNUSED_PARAMETER") tenantId: String,
    ) {
        processedEvents.add(event as TestUserCreatedEvent)
    }
}

/** Test projector that always throws an exception. */
class ErrorProjector {
    fun handleUserCreated(
        @Suppress("UNUSED_PARAMETER") event: Any,
        @Suppress("UNUSED_PARAMETER") eventId: UUID,
        @Suppress("UNUSED_PARAMETER") tenantId: String,
    ): Unit = throw RuntimeException("Simulated processing error")
}

/** Test projector with actual annotation for testing discovery. */
class AnnotatedTestProjector {
    @EafProjectorEventHandler(
        "test.subject",
        projectorName = "AnnotatedTestProjector-handleUserCreated",
    )
    fun handleUserCreated(
        @Suppress("UNUSED_PARAMETER") event: Any,
        @Suppress("UNUSED_PARAMETER") eventId: UUID,
        @Suppress("UNUSED_PARAMETER") tenantId: String,
    ) {
        // no-op
    }
}

/** Test event for integration tests. */
data class TestUserCreatedEvent(
    val userId: String,
    val email: String,
    val fullName: String,
    val createdAt: Instant,
)

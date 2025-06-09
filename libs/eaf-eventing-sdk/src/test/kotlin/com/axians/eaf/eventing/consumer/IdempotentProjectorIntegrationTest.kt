package com.axians.eaf.eventing.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests for the idempotent projector functionality using Testcontainers.
 * Tests the complete end-to-end flow including database operations, idempotency, and tenant isolation.
 */
@Testcontainers
class IdempotentProjectorIntegrationTest {
    companion object {
        @Container
        @JvmStatic
        private val postgres =
            PostgreSQLContainer("postgres:15-alpine")
                .withDatabaseName("eaf_test")
                .withUsername("test")
                .withPassword("test")
    }

    private lateinit var dataSource: DataSource
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate
    private lateinit var transactionTemplate: TransactionTemplate
    private lateinit var processedEventRepository: ProcessedEventRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var idempotentProjectorService: IdempotentProjectorService
    private lateinit var processor: EafProjectorEventHandlerProcessor

    @BeforeEach
    fun setUp() {
        // Set up database connection
        dataSource = createDataSource()
        jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
        transactionTemplate = TransactionTemplate(DataSourceTransactionManager(dataSource))

        // Create the processed_events table
        createProcessedEventsTable()

        // Initialize services
        objectMapper =
            ObjectMapper().apply {
                registerKotlinModule()
                registerModule(JavaTimeModule())
            }

        processedEventRepository = JdbcProcessedEventRepository(jdbcTemplate)
        idempotentProjectorService = IdempotentProjectorService(objectMapper, processedEventRepository)
        processor = EafProjectorEventHandlerProcessor(objectMapper, processedEventRepository)

        // Clear the projector registry
        ProjectorRegistry.clear()
    }

    @AfterEach
    fun tearDown() {
        // Clean up database
        jdbcTemplate.jdbcTemplate.execute("TRUNCATE TABLE processed_events")
        ProjectorRegistry.clear()
    }

    @Test
    fun `should process event successfully and mark as processed`() =
        runTest {
            // Given
            val projectorName = "test-projector"
            val eventId = UUID.randomUUID()
            val tenantId = "tenant-123"
            val event = TestUserCreatedEvent("user-1", "test@example.com", "Test User", Instant.now())

            val mockContext = mockk<MessageContext>()
            every { mockContext.tenantId } returns tenantId
            every { mockContext.getHeader("eventId") } returns eventId.toString()
            every { mockContext.ack() } returns Unit

            // Register a real projector
            val testProjector = TestProjector()
            registerTestProjector(testProjector, projectorName)

            // Verify event is not processed initially
            assertFalse(processedEventRepository.isEventProcessed(projectorName, eventId, tenantId))

            // When
            idempotentProjectorService.processEvent(projectorName, event, mockContext)

            // Then
            assertTrue(processedEventRepository.isEventProcessed(projectorName, eventId, tenantId))
            verify { mockContext.ack() }
            assertEquals(1, testProjector.processedEvents.size)
            assertEquals(event.userId, testProjector.processedEvents[0].userId)
        }

    @Test
    fun `should skip processing when event already processed (idempotency)`() =
        runTest {
            // Given
            val projectorName = "test-projector"
            val eventId = UUID.randomUUID()
            val tenantId = "tenant-123"
            val event = TestUserCreatedEvent("user-1", "test@example.com", "Test User", Instant.now())

            val mockContext = mockk<MessageContext>()
            every { mockContext.tenantId } returns tenantId
            every { mockContext.getHeader("eventId") } returns eventId.toString()
            every { mockContext.ack() } returns Unit

            val testProjector = TestProjector()
            registerTestProjector(testProjector, projectorName)

            // Mark event as already processed
            processedEventRepository.markEventAsProcessed(projectorName, eventId, tenantId)

            // When
            idempotentProjectorService.processEvent(projectorName, event, mockContext)

            // Then
            verify { mockContext.ack() }
            assertEquals(0, testProjector.processedEvents.size) // Should not have processed
        }

    @Test
    fun `should enforce tenant isolation`() =
        runTest {
            // Given
            val projectorName = "test-projector"
            val eventId = UUID.randomUUID()
            val tenant1 = "tenant-123"
            val tenant2 = "tenant-456"

            // When - mark event as processed for tenant1
            processedEventRepository.markEventAsProcessed(projectorName, eventId, tenant1)

            // Then - should not be considered processed for tenant2
            assertTrue(processedEventRepository.isEventProcessed(projectorName, eventId, tenant1))
            assertFalse(processedEventRepository.isEventProcessed(projectorName, eventId, tenant2))
        }

    @Test
    fun `should handle processing errors and nak message`() =
        runTest {
            // Given
            val projectorName = "error-projector"
            val eventId = UUID.randomUUID()
            val tenantId = "tenant-123"
            val event = TestUserCreatedEvent("user-1", "test@example.com", "Test User", Instant.now())

            val mockContext = mockk<MessageContext>()
            every { mockContext.tenantId } returns tenantId
            every { mockContext.getHeader("eventId") } returns eventId.toString()
            every { mockContext.nak() } returns Unit

            // Register a projector that throws an exception
            val errorProjector = ErrorProjector()
            registerErrorProjector(errorProjector, projectorName)

            // When
            idempotentProjectorService.processEvent(projectorName, event, mockContext)

            // Then
            verify { mockContext.nak() }
            assertFalse(processedEventRepository.isEventProcessed(projectorName, eventId, tenantId))
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
        // Given
        val testBean = AnnotatedTestProjector()

        // When
        processor.postProcessAfterInitialization(testBean, "annotatedTestProjector")

        // Then
        val registeredProjector = ProjectorRegistry.getProjector("AnnotatedTestProjector-handleUserCreated")
        assertTrue(registeredProjector != null)
        assertEquals("test.subject", registeredProjector?.subject)
        assertEquals("AnnotatedTestProjector-handleUserCreated-consumer", registeredProjector?.durableName)
    }

    private fun createDataSource(): DataSource {
        val hikariConfig =
            com.zaxxer.hikari.HikariConfig().apply {
                jdbcUrl = postgres.jdbcUrl
                username = postgres.username
                password = postgres.password
                driverClassName = postgres.driverClassName
            }
        return com.zaxxer.hikari.HikariDataSource(hikariConfig)
    }

    private fun createProcessedEventsTable() {
        jdbcTemplate.jdbcTemplate.execute(
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
    ) {
        val method =
            TestProjector::class.java.getDeclaredMethod(
                "handleUserCreated",
                TestUserCreatedEvent::class.java,
                UUID::class.java,
                String::class.java,
            )

        val projectorDef =
            ProjectorDefinition(
                originalBean = projector,
                originalMethod = method,
                beanName = "testProjector",
                projectorName = projectorName,
                subject = "test.subject",
                durableName = "$projectorName-consumer",
                deliverPolicy = io.nats.client.api.DeliverPolicy.All,
                maxDeliver = 3,
                ackWait = 30000L,
                maxAckPending = 1000,
                eventType = TestUserCreatedEvent::class.java,
            )

        ProjectorRegistry.registerProjector(projectorDef)
    }

    private fun registerErrorProjector(
        projector: ErrorProjector,
        projectorName: String,
    ) {
        val method =
            ErrorProjector::class.java.getDeclaredMethod(
                "handleUserCreated",
                TestUserCreatedEvent::class.java,
                UUID::class.java,
                String::class.java,
            )

        val projectorDef =
            ProjectorDefinition(
                originalBean = projector,
                originalMethod = method,
                beanName = "errorProjector",
                projectorName = projectorName,
                subject = "test.subject",
                durableName = "$projectorName-consumer",
                deliverPolicy = io.nats.client.api.DeliverPolicy.All,
                maxDeliver = 3,
                ackWait = 30000L,
                maxAckPending = 1000,
                eventType = TestUserCreatedEvent::class.java,
            )

        ProjectorRegistry.registerProjector(projectorDef)
    }
}

/**
 * Test projector that collects processed events.
 */
class TestProjector {
    val processedEvents = mutableListOf<TestUserCreatedEvent>()

    fun handleUserCreated(
        event: TestUserCreatedEvent,
        eventId: UUID,
        tenantId: String,
    ) {
        processedEvents.add(event)
    }
}

/**
 * Test projector that always throws an exception.
 */
class ErrorProjector {
    fun handleUserCreated(
        event: TestUserCreatedEvent,
        eventId: UUID,
        tenantId: String,
    ): Unit = throw RuntimeException("Simulated processing error")
}

/**
 * Test projector with actual annotation for testing discovery.
 */
class AnnotatedTestProjector {
    @EafProjectorEventHandler("test.subject")
    fun handleUserCreated(
        event: TestUserCreatedEvent,
        eventId: UUID,
        tenantId: String,
    ) {
        // Test projector logic
    }
}

/**
 * Test event for integration tests.
 */
data class TestUserCreatedEvent(
    val userId: String,
    val email: String,
    val fullName: String,
    val createdAt: Instant,
)

package com.axians.eaf.controlplane.integration.security

import com.axians.eaf.eventing.NatsEventPublisher
import kotlinx.coroutines.runBlocking
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Integration tests for security context propagation from commands through events.
 *
 * This test class verifies that:
 * 1. Security context is properly propagated to events
 * 2. All events contain correct security metadata
 * 3. Multi-tenant isolation is maintained
 * 4. Audit trail completeness is preserved
 * 5. Performance requirements are met
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(SecurityContextTestConfiguration::class)
class SecurityContextIntegrationTest {
    @Autowired private lateinit var natsEventPublisher: NatsEventPublisher

    @Autowired private lateinit var testEventCapture: TestEventCapture

    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:15-alpine")
                .withDatabaseName("test_db")
                .withUsername("test_user")
                .withPassword("test_pass")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
        }
    }

    @BeforeEach
    fun setUp() {
        testEventCapture.reset()
        TestSecurityContext.clearContext()
    }

    @AfterEach
    fun tearDown() {
        TestSecurityContext.clearContext()
    }

    @Test
    @WithMockEafUser(tenantId = "TENANT_A", userId = "user-123", email = "test@example.com")
    fun `should propagate security context to events`() =
        runBlocking {
            // Given
            val latch = CountDownLatch(1)
            testEventCapture.setExpectedEvents(1, latch)

            // When - Publish an event
            natsEventPublisher.publish(
                subject = "test.user.created",
                tenantId = "TENANT_A",
                event =
                    TestUserCreatedEvent(
                        "user-123",
                        "test@example.com",
                        "Test",
                        "User",
                        "TENANT_A",
                    ),
            )

            // Then - Wait for event and verify
            assertTrue(
                latch.await(5, TimeUnit.SECONDS),
                "Event should be captured within timeout",
            )

            val capturedEvents = testEventCapture.getCapturedEvents()
            assertEquals(1, capturedEvents.size, "Should capture exactly one event")

            val event = capturedEvents.first()
            assertNotNull(event.metadata["tenant_id"])
            assertNotNull(event.metadata["user_id"])
            assertNotNull(event.metadata["correlation_id"])
            assertNotNull(event.metadata["request_timestamp"])

            assertEquals("TENANT_A", event.metadata["tenant_id"])
            assertEquals("user-123", event.metadata["user_id"])
            assertEquals("test@example.com", event.metadata["user_email"])
        }

    @Test
    @WithMockEafUser(tenantId = "TENANT_B", userId = "admin-456", roles = ["USER", "ADMIN"])
    fun `should include user roles in event metadata`() =
        runBlocking {
            // Given
            val latch = CountDownLatch(1)
            testEventCapture.setExpectedEvents(1, latch)

            // When
            natsEventPublisher.publish(
                subject = "test.admin.action",
                tenantId = "TENANT_B",
                event = TestAdminActionEvent("admin-456", "delete_user"),
            )

            // Then
            assertTrue(latch.await(5, TimeUnit.SECONDS))

            val event = testEventCapture.getCapturedEvents().first()
            assertTrue(event.metadata["user_roles"].toString().contains("ADMIN"))
        }

    @Test
    fun `should handle missing security context gracefully`() =
        runBlocking {
            // Given - No security context set
            val latch = CountDownLatch(1)
            testEventCapture.setExpectedEvents(1, latch)

            // When
            natsEventPublisher.publish(
                subject = "test.system.operation",
                tenantId = "SYSTEM",
                event = TestSystemEvent("system_maintenance"),
            )

            // Then
            assertTrue(latch.await(5, TimeUnit.SECONDS))

            val event = testEventCapture.getCapturedEvents().first()
            assertEquals("SYSTEM", event.metadata["tenant_id"])
            assertEquals("SYSTEM", event.metadata["user_id"])
            assertTrue(event.metadata.containsKey("system_operation"))
        }

    @Test
    @WithMockEafUser(tenantId = "TENANT_A", userId = "user-123")
    fun `should reconstruct complete audit trail from events`() =
        runBlocking {
            // Given
            val latch = CountDownLatch(3)
            testEventCapture.setExpectedEvents(3, latch)

            // When - Simulate a business operation with multiple events
            natsEventPublisher.publish(
                "test.user.created",
                "TENANT_A",
                TestUserCreatedEvent(
                    "user-123",
                    "test@example.com",
                    "Test",
                    "User",
                    "TENANT_A",
                ),
            )
            natsEventPublisher.publish(
                "test.user.activated",
                "TENANT_A",
                TestUserActivatedEvent("user-123"),
            )
            natsEventPublisher.publish(
                "test.email.sent",
                "TENANT_A",
                TestEmailSentEvent("user-123", "welcome"),
            )

            // Then
            assertTrue(latch.await(10, TimeUnit.SECONDS))

            val events = testEventCapture.getCapturedEvents()
            assertEquals(3, events.size)

            // Verify audit trail completeness
            testEventCapture.verifySecurityMetadata()
            testEventCapture.verifyTenantIsolation("TENANT_A")

            // Verify correlation data consistency
            val correlationIds = events.map { it.metadata["correlation_id"] }.distinct()
            assertEquals(
                1,
                correlationIds.size,
                "All events should share the same correlation ID",
            )
        }

    @Test
    @WithMockEafUser(tenantId = "TENANT_A", userId = "user-123")
    fun `should maintain tenant isolation in concurrent operations`() =
        runBlocking {
            // Given
            val eventCount = 10
            val latch = CountDownLatch(eventCount)
            testEventCapture.setExpectedEvents(eventCount, latch)

            // When - Simulate concurrent operations
            repeat(eventCount) { index ->
                natsEventPublisher.publish(
                    "test.concurrent.operation",
                    "TENANT_A",
                    TestConcurrentEvent("operation-$index"),
                )
            }

            // Then
            assertTrue(latch.await(15, TimeUnit.SECONDS))

            val events = testEventCapture.getCapturedEvents()
            assertEquals(eventCount, events.size)

            // Verify all events belong to correct tenant
            events.forEach { event ->
                assertEquals(
                    "TENANT_A",
                    event.metadata["tenant_id"],
                    "Event ${event.eventId} should belong to TENANT_A",
                )
            }

            testEventCapture.verifyTenantIsolation("TENANT_A")
        }

    @Test
    @WithMockEafUser(tenantId = "TENANT_A", userId = "user-123")
    fun `should meet performance requirements for event publishing`() =
        runBlocking {
            // Given
            val eventCount = 100
            val maxDurationMs = 500L // 5ms per event max
            val latch = CountDownLatch(eventCount)
            testEventCapture.setExpectedEvents(eventCount, latch)

            // When
            val startTime = System.currentTimeMillis()

            repeat(eventCount) { index ->
                natsEventPublisher.publish(
                    "test.performance.event",
                    "TENANT_A",
                    TestPerformanceEvent("perf-$index"),
                )
            }

            // Then
            assertTrue(
                latch.await(30, TimeUnit.SECONDS),
                "Performance test should complete within timeout",
            )

            val duration = System.currentTimeMillis() - startTime
            assertTrue(
                duration < maxDurationMs,
                "Publishing $eventCount events took ${duration}ms, should be under ${maxDurationMs}ms",
            )

            // Verify all events were captured correctly
            assertEquals(eventCount, testEventCapture.getEventCount())
            testEventCapture.verifySecurityMetadata()
        }

    @Test
    fun `should provide comprehensive event statistics for debugging`() =
        runBlocking {
            // Given
            TestSecurityContext.setContext(
                "TENANT_A",
                "user-123",
                "test@example.com",
                arrayOf("USER"),
            )

            val latch = CountDownLatch(5)
            testEventCapture.setExpectedEvents(5, latch)

            // When - Publish different types of events
            natsEventPublisher.publish(
                "test.user.created",
                "TENANT_A",
                TestUserCreatedEvent(
                    "user-123",
                    "test@example.com",
                    "Test",
                    "User",
                    "TENANT_A",
                ),
            )
            natsEventPublisher.publish(
                "test.user.created",
                "TENANT_A",
                TestUserCreatedEvent(
                    "user-456",
                    "user2@example.com",
                    "Test",
                    "User",
                    "TENANT_A",
                ),
            )
            natsEventPublisher.publish(
                "test.email.sent",
                "TENANT_A",
                TestEmailSentEvent("user-123", "welcome"),
            )
            natsEventPublisher.publish(
                "test.email.sent",
                "TENANT_A",
                TestEmailSentEvent("user-456", "welcome"),
            )
            natsEventPublisher.publish(
                "test.admin.action",
                "TENANT_A",
                TestAdminActionEvent("admin-789", "review"),
            )

            // Then
            assertTrue(latch.await(10, TimeUnit.SECONDS))

            val stats = testEventCapture.getEventStatistics()
            assertEquals(5, stats.totalEvents)
            assertEquals(2, stats.eventsByType["TestUserCreatedEvent"])
            assertEquals(2, stats.eventsByType["TestEmailSentEvent"])
            assertEquals(1, stats.eventsByType["TestAdminActionEvent"])
            assertEquals(5, stats.eventsByTenant["TENANT_A"])
        }
}

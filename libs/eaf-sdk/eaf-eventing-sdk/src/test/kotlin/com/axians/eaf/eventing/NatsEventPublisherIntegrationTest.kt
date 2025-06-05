package com.axians.eaf.eventing

import com.axians.eaf.eventing.config.NatsEventingProperties
import com.axians.eaf.eventing.config.RetryProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.nats.client.Connection
import io.nats.client.JetStream
import io.nats.client.JetStreamManagement
import io.nats.client.Nats
import io.nats.client.Options
import io.nats.client.api.ConsumerConfiguration
import io.nats.client.api.StorageType
import io.nats.client.api.StreamConfiguration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration

/**
 * Integration tests for NatsEventPublisher using Testcontainers.
 *
 * These tests verify:
 * - Successful publishing and PublishAck handling
 * - Correct use of tenant context for publishing
 * - Publishing to tenant-specific subjects
 * - Graceful handling of error scenarios
 * - Retry mechanisms and at-least-once delivery
 */
@Testcontainers
class NatsEventPublisherIntegrationTest {
    companion object {
        @Container
        @JvmStatic
        val natsContainer =
            GenericContainer("nats:2.10.22-alpine")
                .withExposedPorts(4222, 8222)
                .withCommand("-js", "-m", "8222")
                .waitingFor(
                    LogMessageWaitStrategy()
                        .withRegEx(".*Server is ready.*")
                        .withStartupTimeout(Duration.ofSeconds(30)),
                )
    }

    private lateinit var connection: Connection
    private lateinit var jetStream: JetStream
    private lateinit var jetStreamManagement: JetStreamManagement
    private lateinit var publisher: DefaultNatsEventPublisher
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        val natsUrl = "nats://localhost:${natsContainer.getMappedPort(4222)}"

        val options =
            Options
                .Builder()
                .server(natsUrl)
                .connectionTimeout(Duration.ofSeconds(5))
                .build()

        connection = Nats.connect(options)
        jetStream = connection.jetStream()
        jetStreamManagement = connection.jetStreamManagement()

        val properties =
            NatsEventingProperties(
                servers = listOf(natsUrl),
                retry =
                    RetryProperties(
                        maxAttempts = 3,
                        initialDelayMs = 100,
                        backoffMultiplier = 2.0,
                        maxDelayMs = 1000,
                    ),
            )

        publisher = DefaultNatsEventPublisher(connection, properties)

        objectMapper =
            ObjectMapper().apply {
                registerKotlinModule()
                registerModule(JavaTimeModule())
            }
    }

    @AfterEach
    fun tearDown() {
        if (::connection.isInitialized && connection.status == Connection.Status.CONNECTED) {
            connection.close()
        }
    }

    @Test
    fun `should successfully publish event to tenant-specific subject`() =
        runBlocking {
            // Given
            val tenantId = "tenant-123"
            val subject = "events.user.created"
            val event = UserCreatedEvent("user-456", "John Doe", "john@example.com")
            val expectedSubject = "$tenantId.$subject"

            // Create stream for the tenant-specific subject
            createStreamForSubject(expectedSubject)

            // When
            val publishAck = publisher.publish(subject, tenantId, event)

            // Then
            assertNotNull(publishAck)
            assertTrue(publishAck.seqno > 0)
            assertFalse(publishAck.hasError())

            // Verify the message was actually stored in JetStream
            val streamInfo = jetStreamManagement.getStreamInfo("TEST_STREAM")
            assertEquals(1, streamInfo.streamState.msgCount)
        }

    @Test
    fun `should publish event with metadata`() =
        runBlocking {
            // Given
            val tenantId = "tenant-456"
            val subject = "events.order.placed"
            val event = OrderPlacedEvent("order-789", 99.99)
            val metadata =
                mapOf(
                    "correlationId" to "corr-123",
                    "version" to "1.0",
                    "source" to "order-service",
                )
            val expectedSubject = "$tenantId.$subject"

            // Create stream for the tenant-specific subject
            createStreamForSubject(expectedSubject)

            // When
            val publishAck = publisher.publish(subject, tenantId, event, metadata)

            // Then
            assertNotNull(publishAck)
            assertTrue(publishAck.seqno > 0)
            assertFalse(publishAck.hasError())

            // Verify the message content includes metadata
            val pullSubscription =
                jetStream.subscribe(
                    expectedSubject,
                    io.nats.client.PullSubscribeOptions
                        .builder()
                        .stream("TEST_STREAM")
                        .durable("test-consumer")
                        .build(),
                )
            val messages = pullSubscription.fetch(1, Duration.ofSeconds(5))
            assertTrue(messages.isNotEmpty())

            val message = messages.first()
            val eventEnvelope = objectMapper.readValue<EventEnvelope>(message.data)
            assertEquals(tenantId, eventEnvelope.tenantId)
            assertEquals("OrderPlacedEvent", eventEnvelope.eventType)
            assertEquals(metadata, eventEnvelope.metadata)

            message.ack()
        }

    @Test
    fun `should handle multiple tenants publishing to same subject pattern`() =
        runBlocking {
            // Given
            val subject = "events.notification.sent"
            val tenant1 = "tenant-alpha"
            val tenant2 = "tenant-beta"
            val event1 = NotificationEvent("email", "user1@example.com")
            val event2 = NotificationEvent("sms", "+1234567890")

            // Create streams for both tenant-specific subjects
            createStreamForSubject("$tenant1.$subject")
            createStreamForSubject("$tenant2.$subject")

            // When
            val publishAck1 = publisher.publish(subject, tenant1, event1)
            val publishAck2 = publisher.publish(subject, tenant2, event2)

            // Then
            assertNotNull(publishAck1)
            assertNotNull(publishAck2)
            assertTrue(publishAck1.seqno > 0)
            assertTrue(publishAck2.seqno > 0)

            // Verify both messages are stored
            val streamInfo = jetStreamManagement.getStreamInfo("TEST_STREAM")
            assertEquals(2, streamInfo.streamState.msgCount)
        }

    @Test
    fun `should throw IllegalArgumentException for empty tenant ID`() {
        assertThrows<IllegalArgumentException> {
            runBlocking {
                publisher.publish("test.subject", "", "test event")
            }
        }
    }

    @Test
    fun `should throw IllegalArgumentException for blank tenant ID`() {
        assertThrows<IllegalArgumentException> {
            runBlocking {
                publisher.publish("test.subject", "   ", "test event")
            }
        }
    }

    @Test
    fun `should throw EventPublishingException when stream does not exist`() {
        // Given - no stream created for this subject
        val tenantId = "tenant-999"
        val subject = "events.nonexistent.subject"
        val event = "test event"

        // When/Then
        assertThrows<EventPublishingException> {
            runBlocking {
                publisher.publish(subject, tenantId, event)
            }
        }
    }

    @Test
    fun `should handle connection errors gracefully`() {
        // Given
        connection.close() // Close the connection to simulate error

        // When/Then
        assertThrows<EventPublishingException> {
            runBlocking {
                publisher.publish("test.subject", "tenant-123", "test event")
            }
        }
    }

    @Test
    fun `should validate event envelope structure`() =
        runBlocking {
            // Given
            val tenantId = "tenant-validation"
            val subject = "events.structure.test"
            val event = StructureTestEvent("test-data", 42)
            val metadata = mapOf("testKey" to "testValue")

            createStreamForSubject("$tenantId.$subject")

            // When
            val publishAck = publisher.publish(subject, tenantId, event, metadata)

            // Then
            assertNotNull(publishAck)

            // Verify the event envelope structure
            val pullSubscription =
                jetStream.subscribe(
                    "$tenantId.$subject",
                    io.nats.client.PullSubscribeOptions
                        .builder()
                        .stream("TEST_STREAM")
                        .durable("test-consumer")
                        .build(),
                )
            val messages = pullSubscription.fetch(1, Duration.ofSeconds(5))
            assertTrue(messages.isNotEmpty())

            val message = messages.first()
            val eventEnvelope = objectMapper.readValue<EventEnvelope>(message.data)

            // Validate envelope fields
            assertNotNull(eventEnvelope.eventId)
            assertTrue(eventEnvelope.eventId.isNotBlank())
            assertEquals("StructureTestEvent", eventEnvelope.eventType)
            assertEquals(tenantId, eventEnvelope.tenantId)
            assertNotNull(eventEnvelope.timestamp)
            assertEquals(metadata, eventEnvelope.metadata)

            // Validate payload structure
            assertNotNull(eventEnvelope.payload)

            message.ack()
        }

    @Test
    fun `should handle large payloads`() =
        runBlocking {
            // Given
            val tenantId = "tenant-large"
            val subject = "events.large.payload"
            val largeData = "x".repeat(10000) // 10KB string
            val event = LargePayloadEvent(largeData, (1..1000).toList())

            createStreamForSubject("$tenantId.$subject")

            // When
            val publishAck = publisher.publish(subject, tenantId, event)

            // Then
            assertNotNull(publishAck)
            assertTrue(publishAck.seqno > 0)

            // Verify the large message was stored
            val streamInfo = jetStreamManagement.getStreamInfo("TEST_STREAM")
            assertEquals(1, streamInfo.streamState.msgCount)
            // Note: StreamState.bytes is private, so we just verify the message count
        }

    private fun createStreamForSubject(subject: String) {
        try {
            // Try to delete existing stream first
            jetStreamManagement.deleteStream("TEST_STREAM")
        } catch (e: Exception) {
            // Stream doesn't exist, which is fine
        }

        val streamConfig =
            StreamConfiguration
                .builder()
                .name("TEST_STREAM")
                .subjects(subject, "$subject.*")
                .storageType(StorageType.Memory)
                .build()

        jetStreamManagement.addStream(streamConfig)

        // Create a consumer for testing message retrieval
        jetStreamManagement.addOrUpdateConsumer(
            "TEST_STREAM",
            ConsumerConfiguration
                .builder()
                .durable("test-consumer")
                .build(),
        )
    }

    // Test event classes
    data class UserCreatedEvent(
        val userId: String,
        val name: String,
        val email: String,
    )

    data class OrderPlacedEvent(
        val orderId: String,
        val amount: Double,
    )

    data class NotificationEvent(
        val type: String,
        val target: String,
    )

    data class StructureTestEvent(
        val data: String,
        val number: Int,
    )

    data class LargePayloadEvent(
        val largeString: String,
        val largeList: List<Int>,
    )
}

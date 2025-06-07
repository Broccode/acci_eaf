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
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * Enhanced integration tests for NatsEventPublisher using the local docker-compose NATS infrastructure.
 *
 * These tests use the NATS server from infra/docker-compose/docker-compose.yml and verify:
 * - Successful publishing and PublishAck handling
 * - Correct use of tenant context for publishing
 * - Publishing to tenant-specific subjects
 * - Graceful handling of error scenarios
 * - Retry mechanisms and at-least-once delivery
 *
 * Prerequisites:
 * 1. Start NATS server: `cd infra/docker-compose && docker-compose up -d nats`
 * 2. Run tests with: `./gradlew test -Dnats.integration.enhanced=true`
 */
@EnabledIfSystemProperty(named = "nats.integration.enhanced", matches = "true")
class NatsEventPublisherIntegrationTest {
    companion object {
        private val streamCounter = AtomicInteger(0)
    }

    private lateinit var connection: Connection
    private lateinit var jetStream: JetStream
    private lateinit var jetStreamManagement: JetStreamManagement
    private lateinit var publisher: DefaultNatsEventPublisher
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        val natsUrl = System.getProperty("nats.url", "nats://localhost:4222")

        val options =
            Options
                .Builder()
                .server(natsUrl)
                .connectionTimeout(Duration.ofSeconds(5))
                .build()

        try {
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
        } catch (e: Exception) {
            throw AssertionError(
                "Failed to connect to NATS server at $natsUrl. " +
                    "Please ensure NATS server is running: cd infra/docker-compose && docker-compose up -d nats",
                e,
            )
        }
    }

    @AfterEach
    fun tearDown() {
        try {
            // Clean up any test streams
            val streams = jetStreamManagement.streamNames
            streams.forEach { streamName ->
                try {
                    if (streamName.startsWith("TEST_STREAM_")) {
                        jetStreamManagement.deleteStream(streamName)
                    }
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }

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
            val streamName = createStreamForSubject(expectedSubject)

            // When
            val publishAck = publisher.publish(subject, tenantId, event)

            // Then
            assertNotNull(publishAck)
            assertTrue(publishAck.seqno > 0)
            assertFalse(publishAck.hasError())

            // Verify the message was actually stored in JetStream
            val streamInfo = jetStreamManagement.getStreamInfo(streamName)
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
            val streamName = createStreamForSubject(expectedSubject)

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
                        .stream(streamName)
                        .durable("test-consumer-${streamCounter.get()}")
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

            // Create a single stream that can handle both tenant subjects
            val streamName = createStreamForSubject("*.events.notification.sent")

            // When
            val publishAck1 = publisher.publish(subject, tenant1, event1)
            val publishAck2 = publisher.publish(subject, tenant2, event2)

            // Then
            assertNotNull(publishAck1)
            assertNotNull(publishAck2)
            assertTrue(publishAck1.seqno > 0)
            assertTrue(publishAck2.seqno > 0)

            // Verify both messages are stored
            val streamInfo = jetStreamManagement.getStreamInfo(streamName)
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
    fun `should validate event envelope structure`() =
        runBlocking {
            // Given
            val tenantId = "tenant-validation"
            val subject = "events.structure.test"
            val event = StructureTestEvent("test-data", 42)
            val metadata = mapOf("testKey" to "testValue")

            val streamName = createStreamForSubject("$tenantId.$subject")

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
                        .stream(streamName)
                        .durable("test-consumer-validation")
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

            val streamName = createStreamForSubject("$tenantId.$subject")

            // When
            val publishAck = publisher.publish(subject, tenantId, event)

            // Then
            assertNotNull(publishAck)
            assertTrue(publishAck.seqno > 0)

            // Verify the large message was stored
            val streamInfo = jetStreamManagement.getStreamInfo(streamName)
            assertEquals(1, streamInfo.streamState.msgCount)
        }

    @Test
    fun `should work with docker-compose NATS configuration`() =
        runBlocking {
            // Given - Test that we can work with the configured NATS instance
            val tenantId = "TENANT_A" // Use the configured tenant from nats-server-simple.conf
            val subject = "events.docker.test"
            val event = DockerTestEvent("docker-compose", "working")

            val streamName = createStreamForSubject("$tenantId.$subject")

            // When
            val publishAck = publisher.publish(subject, tenantId, event)

            // Then
            assertNotNull(publishAck)
            assertTrue(publishAck.seqno > 0)

            // Verify JetStream information
            val streamInfo = jetStreamManagement.getStreamInfo(streamName)
            assertEquals(1, streamInfo.streamState.msgCount)
            assertNotNull(streamInfo)
        }

    private fun createStreamForSubject(subject: String): String {
        val streamName = "TEST_STREAM_${streamCounter.incrementAndGet()}"

        try {
            // Clean up any existing stream with same name
            jetStreamManagement.deleteStream(streamName)
        } catch (e: Exception) {
            // Stream doesn't exist, which is fine
        }

        val streamConfig =
            StreamConfiguration
                .builder()
                .name(streamName)
                .subjects(subject)
                .storageType(StorageType.File) // Use File storage like in docker-compose setup
                .maxMessages(1000)
                .maxBytes(10485760) // 10MB
                .maxAge(Duration.ofMinutes(10))
                .build()

        jetStreamManagement.addStream(streamConfig)

        // Create a consumer for testing message retrieval
        jetStreamManagement.addOrUpdateConsumer(
            streamName,
            ConsumerConfiguration
                .builder()
                .durable("test-consumer-${streamCounter.get()}")
                .build(),
        )

        return streamName
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

    data class DockerTestEvent(
        val source: String,
        val status: String,
    )
}

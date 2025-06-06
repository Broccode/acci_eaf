package com.axians.eaf.eventing.consumer

import com.axians.eaf.eventing.EventEnvelope
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Basic integration test for NATS JetStream functionality.
 *
 * This test requires a local NATS server with JetStream enabled.
 * To run this test, start a NATS server with:
 * ```
 * docker run --rm -p 4222:4222 -p 8222:8222 nats:2.10.22-alpine -js -m 8222
 * ```
 *
 * Then run the test with:
 * ```
 * ./gradlew test -Dnats.integration.enabled=true
 * ```
 */
@EnabledIfSystemProperty(named = "nats.integration.enabled", matches = "true")
class NatsJetStreamBasicIntegrationTest {
    private lateinit var connection: Connection
    private lateinit var jetStream: JetStream
    private lateinit var jetStreamManagement: JetStreamManagement
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

            objectMapper =
                ObjectMapper().apply {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                }
        } catch (e: Exception) {
            throw AssertionError(
                "Failed to connect to NATS server at $natsUrl. " +
                    "Please ensure NATS server is running with JetStream enabled. " +
                    "Start with: docker run --rm -p 4222:4222 -p 8222:8222 nats:2.10.22-alpine -js -m 8222",
                e,
            )
        }
    }

    @AfterEach
    fun tearDown() {
        try {
            // Clean up test streams
            val streams = jetStreamManagement.streamNames
            streams.forEach { streamName ->
                if (streamName.startsWith("TEST_")) {
                    try {
                        jetStreamManagement.deleteStream(streamName)
                    } catch (e: Exception) {
                        // Ignore cleanup errors
                    }
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
    fun `should create stream and consumer successfully`() {
        // Given
        val streamName = "TEST_BASIC_STREAM"
        val streamConfig =
            StreamConfiguration
                .builder()
                .name(streamName)
                .subjects("test.events.basic")
                .storageType(StorageType.Memory)
                .maxMessages(100)
                .build()

        // When
        val streamInfo = jetStreamManagement.addStream(streamConfig)
        val consumerInfo =
            jetStreamManagement.addOrUpdateConsumer(
                streamName,
                ConsumerConfiguration
                    .builder()
                    .durable("test-basic-consumer")
                    .build(),
            )

        // Then
        assertNotNull(streamInfo)
        assertNotNull(consumerInfo)
        assertEquals(streamName, streamInfo.configuration.name)
        assertEquals("test-basic-consumer", consumerInfo.name)
    }

    @Test
    fun `should publish and consume message from JetStream`() =
        runBlocking {
            // Given
            val streamName = "TEST_PUBSUB_STREAM"
            val subject = "test.events.pubsub"

            val streamConfig =
                StreamConfiguration
                    .builder()
                    .name(streamName)
                    .subjects(subject)
                    .storageType(StorageType.Memory)
                    .maxMessages(100)
                    .build()

            jetStreamManagement.addStream(streamConfig)
            jetStreamManagement.addOrUpdateConsumer(
                streamName,
                ConsumerConfiguration
                    .builder()
                    .durable("test-pubsub-consumer")
                    .build(),
            )

            // Publish a test message
            val eventEnvelope =
                EventEnvelope(
                    eventId = UUID.randomUUID().toString(),
                    eventType = "TestEvent",
                    timestamp = Instant.now(),
                    tenantId = "test-tenant",
                    payload = mapOf("data" to "test"),
                    metadata = mapOf("source" to "test"),
                )

            val messageData = objectMapper.writeValueAsBytes(eventEnvelope)
            val publishAck = jetStream.publish(subject, messageData)

            // When - Consume the message
            val pullSubscription =
                jetStream.subscribe(
                    subject,
                    io.nats.client.PullSubscribeOptions
                        .builder()
                        .stream(streamName)
                        .durable("test-pubsub-consumer")
                        .build(),
                )

            val messages = pullSubscription.fetch(1, Duration.ofSeconds(5))

            // Then
            assertNotNull(publishAck)
            assertEquals(1, messages.size)

            val message = messages.first()
            val receivedEnvelope = objectMapper.readValue(message.data, EventEnvelope::class.java)

            assertEquals(eventEnvelope.eventId, receivedEnvelope.eventId)
            assertEquals(eventEnvelope.eventType, receivedEnvelope.eventType)
            assertEquals(eventEnvelope.tenantId, receivedEnvelope.tenantId)

            message.ack()
        }

    @Test
    fun `should handle message acknowledgment correctly`() =
        runBlocking {
            // Given
            val streamName = "TEST_ACK_STREAM"
            val subject = "test.events.ack"

            val streamConfig =
                StreamConfiguration
                    .builder()
                    .name(streamName)
                    .subjects(subject)
                    .storageType(StorageType.Memory)
                    .build()

            jetStreamManagement.addStream(streamConfig)
            jetStreamManagement.addOrUpdateConsumer(
                streamName,
                ConsumerConfiguration
                    .builder()
                    .durable("test-ack-consumer")
                    .build(),
            )

            // Publish test messages
            jetStream.publish(subject, "message1".toByteArray())
            jetStream.publish(subject, "message2".toByteArray())

            // When
            val pullSubscription =
                jetStream.subscribe(
                    subject,
                    io.nats.client.PullSubscribeOptions
                        .builder()
                        .stream(streamName)
                        .durable("test-ack-consumer")
                        .build(),
                )

            val messages = pullSubscription.fetch(2, Duration.ofSeconds(5))

            // Then
            assertEquals(2, messages.size)

            // Acknowledge first message
            messages[0].ack()

            // NAK second message (for retry)
            messages[1].nak()

            // Verify stream state - both messages should still be in the stream
            // since NAK means the message will be redelivered
            val streamInfo = jetStreamManagement.getStreamInfo(streamName)
            assertEquals(2, streamInfo.streamState.msgCount)
        }

    @Test
    fun `should demonstrate MessageContext functionality`() {
        // Given
        val mockMessage =
            io.mockk.mockk<io.nats.client.Message> {
                io.mockk.every { subject } returns "test.subject"
                io.mockk.every { headers } returns null
                io.mockk.every { ack() } returns Unit
                io.mockk.every { nak() } returns Unit
                io.mockk.every { term() } returns Unit
            }

        // When
        val messageContext = DefaultMessageContext(mockMessage, "test-tenant")

        // Then
        assertEquals("test-tenant", messageContext.tenantId)
        assertEquals("test.subject", messageContext.subject)
        assertEquals(mockMessage, messageContext.message)

        // Test acknowledgment
        messageContext.ack()
        assertEquals(true, messageContext.isAcknowledged())

        // Test that subsequent calls are ignored
        messageContext.nak() // Should be ignored
        assertEquals(true, messageContext.isAcknowledged())
    }
}

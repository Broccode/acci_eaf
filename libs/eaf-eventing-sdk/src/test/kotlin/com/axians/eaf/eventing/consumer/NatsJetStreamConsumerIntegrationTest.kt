@file:Suppress("TooGenericExceptionThrown")

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
 * Enhanced integration test for NATS JetStream message consumption using docker-compose
 * infrastructure.
 *
 * These tests use the NATS server from infra/docker-compose/docker-compose.yml and verify:
 * - Stream and consumer creation
 * - Message publishing and consumption
 * - Message acknowledgment patterns (ack/nak)
 * - Integration with the configured NATS tenant setup
 *
 * Prerequisites:
 * 1. Start NATS server: `cd infra/docker-compose && docker-compose up -d nats`
 * 2. Run tests with: `nx run eaf-eventing-sdk:test --args="-Dnats.integration.enhanced=true"`
 */
@EnabledIfSystemProperty(named = "nats.integration.enhanced", matches = "true")
class NatsJetStreamConsumerIntegrationTest {
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
                    "Please ensure NATS server is running: 'nx run-many --target=up --projects=nats -d'",
                e,
            )
        }
    }

    @AfterEach
    fun tearDown() {
        try {
            jetStreamManagement.deleteStream("TEST_CONSUMER_STREAM")
        } catch (e: Exception) {
            // Ignore cleanup errors
        }

        if (::connection.isInitialized && connection.status == Connection.Status.CONNECTED) {
            connection.close()
        }
    }

    @Test
    fun `should create stream and consumer successfully with docker-compose NATS`() {
        // Given
        val streamConfig =
            StreamConfiguration
                .builder()
                .name("TEST_CONSUMER_STREAM")
                .subjects("TENANT_A.events.test") // Use TENANT_A from docker-compose config
                .storageType(StorageType.File) // Use File storage like docker-compose setup
                .maxMessages(100)
                .build()

        // When
        val createdStreamInfo = jetStreamManagement.addStream(streamConfig)
        val consumerInfo =
            jetStreamManagement.addOrUpdateConsumer(
                "TEST_CONSUMER_STREAM",
                ConsumerConfiguration.builder().durable("test-consumer").build(),
            )

        // Then
        assertNotNull(createdStreamInfo)
        assertNotNull(consumerInfo)
        assertEquals("TEST_CONSUMER_STREAM", createdStreamInfo.configuration.name)
        assertEquals("test-consumer", consumerInfo.name)

        // Verify JetStream is properly configured
        val streamInfo = jetStreamManagement.getStreamInfo("TEST_CONSUMER_STREAM")
        assertNotNull(streamInfo)
    }

    @Test
    fun `should publish and consume message from JetStream with docker-compose`() =
        runBlocking {
            // Given
            val streamConfig =
                StreamConfiguration
                    .builder()
                    .name("TEST_CONSUMER_STREAM")
                    .subjects("TENANT_A.events.test")
                    .storageType(StorageType.File)
                    .maxMessages(100)
                    .build()

            jetStreamManagement.addStream(streamConfig)
            jetStreamManagement.addOrUpdateConsumer(
                "TEST_CONSUMER_STREAM",
                ConsumerConfiguration.builder().durable("test-consumer").build(),
            )

            // Publish a test message
            val eventEnvelope =
                EventEnvelope(
                    eventId = UUID.randomUUID().toString(),
                    eventType = "TestEvent",
                    timestamp = Instant.now(),
                    tenantId = "TENANT_A",
                    payload = mapOf("data" to "docker-compose-test"),
                    metadata = mapOf("source" to "docker-integration-test"),
                )

            val messageData = objectMapper.writeValueAsBytes(eventEnvelope)
            val publishAck = jetStream.publish("TENANT_A.events.test", messageData)

            // When - Consume the message
            val pullSubscription =
                jetStream.subscribe(
                    "TENANT_A.events.test",
                    io.nats.client.PullSubscribeOptions
                        .builder()
                        .stream("TEST_CONSUMER_STREAM")
                        .durable("test-consumer")
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
            assertEquals("docker-compose-test", (receivedEnvelope.payload as Map<*, *>)["data"])

            message.ack()
        }

    @Test
    fun `should handle message acknowledgment correctly with persistent storage`() =
        runBlocking {
            // Given
            val streamConfig =
                StreamConfiguration
                    .builder()
                    .name("TEST_CONSUMER_STREAM")
                    .subjects("TENANT_A.events.ack")
                    .storageType(StorageType.File) // File storage persists across restarts
                    .build()

            jetStreamManagement.addStream(streamConfig)
            jetStreamManagement.addOrUpdateConsumer(
                "TEST_CONSUMER_STREAM",
                ConsumerConfiguration.builder().durable("test-ack-consumer").build(),
            )

            // Publish test messages
            jetStream.publish("TENANT_A.events.ack", "docker-message1".toByteArray())
            jetStream.publish("TENANT_A.events.ack", "docker-message2".toByteArray())

            // When
            val pullSubscription =
                jetStream.subscribe(
                    "TENANT_A.events.ack",
                    io.nats.client.PullSubscribeOptions
                        .builder()
                        .stream("TEST_CONSUMER_STREAM")
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
            val streamInfo = jetStreamManagement.getStreamInfo("TEST_CONSUMER_STREAM")
            assertEquals(2, streamInfo.streamState.msgCount)
        }

    @Test
    fun `should demonstrate MessageContext functionality with docker-compose setup`() {
        // Given
        val mockMessage =
            io.mockk.mockk<io.nats.client.Message> {
                io.mockk.every { subject } returns "TENANT_A.events.context.test"
                io.mockk.every { headers } returns null
                io.mockk.every { ack() } returns Unit
                io.mockk.every { nak() } returns Unit
                io.mockk.every { term() } returns Unit
            }

        // When
        val messageContext = DefaultMessageContext(mockMessage, "TENANT_A")

        // Then
        assertEquals("TENANT_A", messageContext.tenantId)
        assertEquals("TENANT_A.events.context.test", messageContext.subject)
        assertEquals(mockMessage, messageContext.message)

        // Test acknowledgment
        messageContext.ack()
        assertEquals(true, messageContext.isAcknowledged())

        // Test that subsequent calls are ignored
        messageContext.nak() // Should be ignored
        assertEquals(true, messageContext.isAcknowledged())
    }

    @Test
    fun `should work with tenant-specific subject patterns from docker-compose config`() =
        runBlocking {
            // Given - Create streams for different tenant patterns
            val tenantAStream =
                StreamConfiguration
                    .builder()
                    .name("TEST_CONSUMER_STREAM")
                    .subjects("TENANT_A.events.>") // Wildcard for all TENANT_A events
                    .storageType(StorageType.File)
                    .build()

            jetStreamManagement.addStream(tenantAStream)
            jetStreamManagement.addOrUpdateConsumer(
                "TEST_CONSUMER_STREAM",
                ConsumerConfiguration.builder().durable("tenant-a-consumer").build(),
            )

            // Publish different types of events for TENANT_A
            jetStream.publish("TENANT_A.events.user.created", "user-event".toByteArray())
            jetStream.publish("TENANT_A.events.order.placed", "order-event".toByteArray())
            jetStream.publish(
                "TENANT_A.events.notification.sent",
                "notification-event".toByteArray(),
            )

            // When - Consume all messages
            val pullSubscription =
                jetStream.subscribe(
                    "TENANT_A.events.>",
                    io.nats.client.PullSubscribeOptions
                        .builder()
                        .stream("TEST_CONSUMER_STREAM")
                        .durable("tenant-a-consumer")
                        .build(),
                )

            val messages = pullSubscription.fetch(3, Duration.ofSeconds(5))

            // Then
            assertEquals(3, messages.size)

            // Verify all messages are for TENANT_A
            messages.forEach { message ->
                assert(message.subject.startsWith("TENANT_A.events."))
                message.ack()
            }

            val streamInfo = jetStreamManagement.getStreamInfo("TEST_CONSUMER_STREAM")
            assertEquals(3, streamInfo.streamState.msgCount)
        }
}

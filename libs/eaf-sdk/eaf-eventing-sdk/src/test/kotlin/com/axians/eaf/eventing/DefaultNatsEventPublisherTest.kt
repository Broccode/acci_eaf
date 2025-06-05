package com.axians.eaf.eventing

import com.axians.eaf.eventing.config.NatsEventingProperties
import com.axians.eaf.eventing.config.RetryProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.nats.client.Connection
import io.nats.client.JetStream
import io.nats.client.api.PublishAck
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DefaultNatsEventPublisherTest {
    private lateinit var mockConnection: Connection
    private lateinit var mockJetStream: JetStream
    private lateinit var mockPublishAck: PublishAck
    private lateinit var properties: NatsEventingProperties
    private lateinit var publisher: DefaultNatsEventPublisher

    @BeforeEach
    fun setUp() {
        mockConnection = mockk()
        mockJetStream = mockk()
        mockPublishAck = mockk()

        // Setup default properties for testing
        properties =
            NatsEventingProperties(
                retry =
                    RetryProperties(
                        maxAttempts = 3,
                        initialDelayMs = 100, // Shorter delay for tests
                        backoffMultiplier = 2.0,
                        maxDelayMs = 1000,
                    ),
            )

        every { mockConnection.jetStream() } returns mockJetStream
        every { mockConnection.status } returns Connection.Status.CONNECTED

        publisher = DefaultNatsEventPublisher(mockConnection, properties)
    }

    @Test
    fun `should throw IllegalArgumentException when tenantId is empty`() {
        assertThrows<IllegalArgumentException> {
            runBlocking {
                publisher.publish("test.subject", "", "test event")
            }
        }
    }

    @Test
    fun `should throw IllegalArgumentException when tenantId is blank`() {
        assertThrows<IllegalArgumentException> {
            runBlocking {
                publisher.publish("test.subject", "   ", "test event")
            }
        }
    }

    @Test
    fun `should throw EventPublishingException when connection is not active`() {
        // Given
        every { mockConnection.status } returns Connection.Status.DISCONNECTED

        // When/Then
        assertThrows<EventPublishingException> {
            runBlocking {
                publisher.publish("test.subject", "tenant-123", "test event")
            }
        }
    }

    @Test
    fun `should publish event with tenant-specific subject`() =
        runBlocking {
            // Given
            val subject = "events.user.created"
            val tenantId = "tenant-123"
            val event = mapOf("userId" to "user-456", "name" to "John Doe")
            val expectedSubject = "$tenantId.$subject"

            every { mockPublishAck.stream } returns "test-stream"
            every { mockPublishAck.seqno } returns 1L
            every { mockPublishAck.isDuplicate } returns false
            every { mockPublishAck.hasError() } returns false

            every {
                mockJetStream.publish(eq(expectedSubject), any<ByteArray>())
            } returns mockPublishAck

            // When
            val result = publisher.publish(subject, tenantId, event)

            // Then
            assertEquals(mockPublishAck, result)
            verify { mockJetStream.publish(expectedSubject, any<ByteArray>()) }
        }

    @Test
    fun `should publish event with metadata`() =
        runBlocking {
            // Given
            val subject = "events.order.placed"
            val tenantId = "tenant-456"
            val event = mapOf("orderId" to "order-789")
            val metadata = mapOf("correlationId" to "corr-123", "version" to "1.0")
            val expectedSubject = "$tenantId.$subject"

            every { mockPublishAck.stream } returns "test-stream"
            every { mockPublishAck.seqno } returns 2L
            every { mockPublishAck.isDuplicate } returns false
            every { mockPublishAck.hasError() } returns false

            every {
                mockJetStream.publish(eq(expectedSubject), any<ByteArray>())
            } returns mockPublishAck

            // When
            val result = publisher.publish(subject, tenantId, event, metadata)

            // Then
            assertEquals(mockPublishAck, result)
            verify { mockJetStream.publish(expectedSubject, any<ByteArray>()) }
        }

    @Test
    fun `should retry on publish failure and succeed on second attempt`() =
        runBlocking {
            // Given
            val subject = "events.test"
            val tenantId = "tenant-123"
            val event = "test event"
            val expectedSubject = "$tenantId.$subject"

            every { mockPublishAck.stream } returns "test-stream"
            every { mockPublishAck.seqno } returns 1L
            every { mockPublishAck.isDuplicate } returns false
            every { mockPublishAck.hasError() } returns false

            every {
                mockJetStream.publish(eq(expectedSubject), any<ByteArray>())
            } throws RuntimeException("Temporary failure") andThen mockPublishAck

            // When
            val result = publisher.publish(subject, tenantId, event)

            // Then
            assertEquals(mockPublishAck, result)
            verify(exactly = 2) { mockJetStream.publish(expectedSubject, any<ByteArray>()) }
        }

    @Test
    fun `should fail after maximum retry attempts`() {
        // Given
        val subject = "events.test"
        val tenantId = "tenant-123"
        val event = "test event"
        val expectedSubject = "$tenantId.$subject"

        every {
            mockJetStream.publish(eq(expectedSubject), any<ByteArray>())
        } throws RuntimeException("Persistent failure")

        // When/Then
        assertThrows<EventPublishingException> {
            runBlocking {
                publisher.publish(subject, tenantId, event)
            }
        }

        verify(exactly = properties.retry.maxAttempts) {
            mockJetStream.publish(expectedSubject, any<ByteArray>())
        }
    }

    @Test
    fun `should throw EventPublishingException when PublishAck has error`() {
        // Given
        val subject = "events.test"
        val tenantId = "tenant-123"
        val event = "test event"

        every { mockPublishAck.hasError() } returns true
        every { mockPublishAck.error } returns "Stream not found"

        every {
            mockJetStream.publish(any<String>(), any<ByteArray>())
        } returns mockPublishAck

        // When/Then
        assertThrows<EventPublishingException> {
            runBlocking {
                publisher.publish(subject, tenantId, event)
            }
        }
    }

    @Test
    fun `should throw EventPublishingException when PublishAck has invalid sequence number`() {
        // Given
        val subject = "events.test"
        val tenantId = "tenant-123"
        val event = "test event"

        every { mockPublishAck.hasError() } returns false
        every { mockPublishAck.seqno } returns 0L

        every {
            mockJetStream.publish(any<String>(), any<ByteArray>())
        } returns mockPublishAck

        // When/Then
        assertThrows<EventPublishingException> {
            runBlocking {
                publisher.publish(subject, tenantId, event)
            }
        }
    }
}

package com.axians.eaf.eventing

import com.axians.eaf.eventing.config.NatsEventingProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.nats.client.Connection
import io.nats.client.JetStream
import io.nats.client.api.PublishAck
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import kotlin.math.min

/**
 * Default implementation of NatsEventPublisher that publishes events to NATS JetStream
 * with tenant-specific routing and at-least-once delivery guarantees.
 *
 * This implementation includes:
 * - Tenant-specific subject routing
 * - Configurable retry mechanisms with exponential backoff
 * - Connection lifecycle management
 * - Comprehensive error handling and logging
 */
@Service
class DefaultNatsEventPublisher(
    private val connection: Connection,
    private val properties: NatsEventingProperties,
) : NatsEventPublisher {
    private val logger = LoggerFactory.getLogger(DefaultNatsEventPublisher::class.java)
    private val jetStream: JetStream = connection.jetStream()

    private val objectMapper =
        ObjectMapper().apply {
            registerKotlinModule()
            registerModule(JavaTimeModule())
        }

    override suspend fun publish(
        subject: String,
        tenantId: String,
        event: Any,
    ): PublishAck = publish(subject, tenantId, event, emptyMap())

    override suspend fun publish(
        subject: String,
        tenantId: String,
        event: Any,
        metadata: Map<String, Any>,
    ): PublishAck {
        validateTenantId(tenantId)
        validateConnection()

        val eventEnvelope = createEventEnvelope(event, tenantId, metadata)
        val tenantSpecificSubject = "$tenantId.$subject"

        return publishWithRetry(tenantSpecificSubject, eventEnvelope, tenantId)
    }

    /**
     * Publishes an event with configurable retry logic and exponential backoff.
     */
    private suspend fun publishWithRetry(
        subject: String,
        eventEnvelope: EventEnvelope,
        tenantId: String,
    ): PublishAck {
        var lastException: Exception? = null
        var attempt = 1
        var delayMs = properties.retry.initialDelayMs

        repeat(properties.retry.maxAttempts) {
            try {
                logger.debug("Publishing event attempt {} to subject: {} for tenant: {}", attempt, subject, tenantId)

                val eventJson = objectMapper.writeValueAsBytes(eventEnvelope)
                val publishAck = jetStream.publish(subject, eventJson)

                // Validate PublishAck for successful delivery
                validatePublishAck(publishAck, subject, tenantId)

                logger.info(
                    "Successfully published event to stream: {}, sequence: {}, duplicate: {}, attempt: {}",
                    publishAck.stream,
                    publishAck.seqno,
                    publishAck.isDuplicate,
                    attempt,
                )

                return publishAck
            } catch (e: Exception) {
                lastException = e
                logger.warn(
                    "Failed to publish event attempt {} to subject: {} for tenant: {} - {}",
                    attempt,
                    subject,
                    tenantId,
                    e.message,
                )

                if (attempt < properties.retry.maxAttempts) {
                    logger.debug("Retrying in {}ms (attempt {}/{})", delayMs, attempt, properties.retry.maxAttempts)
                    delay(delayMs)

                    // Exponential backoff with maximum delay cap
                    delayMs =
                        min(
                            (delayMs * properties.retry.backoffMultiplier).toLong(),
                            properties.retry.maxDelayMs,
                        )
                }
                attempt++
            }
        }

        logger.error(
            "Failed to publish event after {} attempts to subject: {} for tenant: {}",
            properties.retry.maxAttempts,
            subject,
            tenantId,
            lastException,
        )
        throw EventPublishingException(
            "Failed to publish event after ${properties.retry.maxAttempts} attempts: ${lastException?.message}",
            lastException,
        )
    }

    /**
     * Validates the PublishAck to ensure successful delivery.
     */
    private fun validatePublishAck(
        publishAck: PublishAck,
        subject: String,
        tenantId: String,
    ) {
        if (publishAck.hasError()) {
            throw EventPublishingException(
                "NATS JetStream returned error for subject $subject, tenant $tenantId: ${publishAck.error}",
            )
        }

        if (publishAck.seqno <= 0) {
            throw EventPublishingException(
                "Invalid sequence number ${publishAck.seqno} for subject $subject, tenant $tenantId",
            )
        }
    }

    /**
     * Validates that the NATS connection is active and healthy.
     */
    private fun validateConnection() {
        if (connection.status != Connection.Status.CONNECTED) {
            throw EventPublishingException(
                "NATS connection is not active. Current status: ${connection.status}",
            )
        }
    }

    private fun validateTenantId(tenantId: String) {
        if (tenantId.isBlank()) {
            throw IllegalArgumentException("Tenant ID cannot be null or empty")
        }
    }

    private fun createEventEnvelope(
        event: Any,
        tenantId: String,
        metadata: Map<String, Any>,
    ): EventEnvelope =
        EventEnvelope(
            eventId = UUID.randomUUID().toString(),
            eventType = event::class.simpleName ?: "UnknownEvent",
            timestamp = Instant.now(),
            tenantId = tenantId,
            payload = event,
            metadata = metadata,
        )
}

/**
 * Standard event envelope structure for EAF events.
 */
data class EventEnvelope(
    val eventId: String,
    val eventType: String,
    val timestamp: Instant,
    val tenantId: String,
    val payload: Any,
    val metadata: Map<String, Any> = emptyMap(),
)

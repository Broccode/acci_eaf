package com.axians.eaf.eventing

import com.axians.eaf.eventing.config.NatsEventingProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.nats.client.Connection
import io.nats.client.JetStream
import io.nats.client.JetStreamApiException
import io.nats.client.api.PublishAck
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import kotlin.math.min

/**
 * Default implementation of NatsEventPublisher that publishes events to NATS JetStream with
 * tenant-specific routing and at-least-once delivery guarantees.
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
        validateConnectionAndTenant(tenantId)

        val eventEnvelope = createEventEnvelope(event, tenantId, metadata)
        val tenantSpecificSubject = "$tenantId.$subject"

        return publishWithRetry(tenantSpecificSubject, eventEnvelope, tenantId)
    }

    /** Publishes an event with configurable retry logic and exponential backoff. */
    private suspend fun publishWithRetry(
        subject: String,
        eventEnvelope: EventEnvelope,
        tenantId: String,
    ): PublishAck {
        var lastException: Exception? = null
        var attempt = 1
        var delayMs = properties.retry.initialDelayMs

        repeat(properties.retry.maxAttempts) {
            val result = attemptPublish(subject, eventEnvelope, tenantId, attempt)

            when (result) {
                is PublishResult.Success -> return result.publishAck
                is PublishResult.RetryableFailure -> {
                    lastException = result.exception
                    if (attempt < properties.retry.maxAttempts) {
                        delayMs = calculateRetryDelay(delayMs, attempt)
                    }
                }
                is PublishResult.NonRetryableFailure -> throw result.exception
            }
            attempt++
        }

        handleFinalFailure(subject, tenantId, lastException)
    }

    /** Attempts a single publish operation and categorizes the result. */
    private suspend fun attemptPublish(
        subject: String,
        eventEnvelope: EventEnvelope,
        tenantId: String,
        attempt: Int,
    ): PublishResult =
        try {
            logger.debug(
                "Publishing event attempt {} to subject: {} for tenant: {}",
                attempt,
                subject,
                tenantId,
            )

            val eventJson = objectMapper.writeValueAsBytes(eventEnvelope)
            val publishAck = jetStream.publish(subject, eventJson)

            validatePublishAck(publishAck, subject, tenantId)
            logSuccessfulPublish(publishAck, attempt)

            PublishResult.Success(publishAck)
        } catch (e: JetStreamApiException) {
            categorizeException(e, subject, tenantId, attempt)
        } catch (e: java.io.IOException) {
            categorizeException(e, subject, tenantId, attempt)
        } catch (e: com.fasterxml.jackson.core.JsonProcessingException) {
            categorizeException(e, subject, tenantId, attempt)
        } catch (e: java.lang.InterruptedException) {
            categorizeException(e, subject, tenantId, attempt)
        } catch (e: IllegalStateException) {
            logger.warn(
                "Illegal state during publish attempt {} to subject: {} for tenant: {} - {}",
                attempt,
                subject,
                tenantId,
                e.message,
                e,
            )
            PublishResult.RetryableFailure(e)
        } catch (e: IllegalArgumentException) {
            logger.error(
                "Invalid argument during publish attempt {} to subject: {} for tenant: {} - {}",
                attempt,
                subject,
                tenantId,
                e.message,
                e,
            )
            PublishResult.NonRetryableFailure(e)
        }

    /** Categorizes exceptions into retryable and non-retryable failures. */
    private fun categorizeException(
        exception: Exception,
        subject: String,
        tenantId: String,
        attempt: Int,
    ): PublishResult =
        when (exception) {
            is com.fasterxml.jackson.core.JsonProcessingException -> {
                logger.error(
                    "JSON serialization error for subject: {} tenant: {} - {}",
                    subject,
                    tenantId,
                    exception.message,
                )
                PublishResult.NonRetryableFailure(
                    EventPublishingException(
                        "Failed to serialize event envelope",
                        exception,
                    ),
                )
            }
            is java.lang.InterruptedException -> {
                Thread.currentThread().interrupt()
                logger.warn(
                    "Interrupted while publishing event to subject: {} for tenant: {}",
                    subject,
                    tenantId,
                )
                PublishResult.NonRetryableFailure(
                    EventPublishingException("Publishing was interrupted", exception),
                )
            }
            is JetStreamApiException -> {
                logger.warn(
                    "JetStream API error (attempt {}) for subject: {} tenant: {} - {}",
                    attempt,
                    subject,
                    tenantId,
                    exception.message,
                )
                PublishResult.RetryableFailure(exception)
            }
            is java.io.IOException -> {
                logger.warn(
                    "I/O error while publishing (attempt {}) to subject: {} tenant: {} - {}",
                    attempt,
                    subject,
                    tenantId,
                    exception.message,
                )
                PublishResult.RetryableFailure(exception)
            }
            else -> PublishResult.RetryableFailure(exception)
        }

    /** Calculates the retry delay with exponential backoff. */
    private suspend fun calculateRetryDelay(
        currentDelayMs: Long,
        attempt: Int,
    ): Long {
        logger.debug(
            "Retrying in {}ms (attempt {}/{})",
            currentDelayMs,
            attempt,
            properties.retry.maxAttempts,
        )
        delay(currentDelayMs)

        return min(
            (currentDelayMs * properties.retry.backoffMultiplier).toLong(),
            properties.retry.maxDelayMs,
        )
    }

    /** Logs successful publish operation. */
    private fun logSuccessfulPublish(
        publishAck: PublishAck,
        attempt: Int,
    ) {
        logger.info(
            "Successfully published event to stream: {}, sequence: {}, duplicate: {}, attempt: {}",
            publishAck.stream,
            publishAck.seqno,
            publishAck.isDuplicate,
            attempt,
        )
    }

    /** Handles final failure after all retry attempts exhausted. */
    private fun handleFinalFailure(
        subject: String,
        tenantId: String,
        lastException: Exception?,
    ): Nothing {
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

    /** Validates the PublishAck to ensure successful delivery. */
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

    /** Validates that the NATS connection is active and healthy, and validates tenant ID. */
    private fun validateConnectionAndTenant(tenantId: String) {
        if (connection.status != Connection.Status.CONNECTED) {
            throw EventPublishingException(
                "NATS connection is not active. Current status: ${connection.status}",
            )
        }
        require(tenantId.isNotBlank()) { "Tenant ID cannot be null or empty" }
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

/** Represents the result of a publish attempt. */
private sealed class PublishResult {
    data class Success(
        val publishAck: PublishAck,
    ) : PublishResult()

    data class RetryableFailure(
        val exception: Exception,
    ) : PublishResult()

    data class NonRetryableFailure(
        val exception: Exception,
    ) : PublishResult()
}

/** Standard event envelope structure for EAF events. */
data class EventEnvelope(
    val eventId: String,
    val eventType: String,
    val timestamp: Instant,
    val tenantId: String,
    val payload: Any,
    val metadata: Map<String, Any> = emptyMap(),
)

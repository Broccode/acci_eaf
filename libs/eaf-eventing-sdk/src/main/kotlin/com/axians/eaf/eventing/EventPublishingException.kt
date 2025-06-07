package com.axians.eaf.eventing

/**
 * Exception thrown when event publishing fails after all retry attempts.
 *
 * This exception indicates that the SDK was unable to successfully publish
 * an event to NATS JetStream despite retry mechanisms.
 */
class EventPublishingException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

package com.axians.eaf.eventing.consumer

/**
 * Base exception for event consumption failures.
 *
 * This exception hierarchy allows developers to control how messages
 * are acknowledged when listener methods fail.
 */
abstract class EventConsumptionException(
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Exception indicating a retryable failure during event processing.
 *
 * When this exception is thrown from a listener method, the message will be
 * negatively acknowledged (nak'd) allowing for retry by NATS JetStream.
 *
 * Example usage:
 * ```kotlin
 * @NatsJetStreamListener("events.user.created")
 * fun handleUserCreated(event: UserCreatedEvent) {
 *     try {
 *         userService.createUser(event.userId)
 *     } catch (e: DatabaseConnectionException) {
 *         throw RetryableEventException("Database temporarily unavailable", e)
 *     }
 * }
 * ```
 */
class RetryableEventException(
    message: String? = null,
    cause: Throwable? = null,
) : EventConsumptionException(message, cause)

/**
 * Exception indicating a non-retryable failure during event processing.
 *
 * When this exception is thrown from a listener method, the message will be
 * terminated (term'd) and will not be retried. Use this for poison pill
 * messages or unrecoverable errors.
 *
 * Example usage:
 * ```kotlin
 * @NatsJetStreamListener("events.user.created")
 * fun handleUserCreated(event: UserCreatedEvent) {
 *     if (event.userId == null) {
 *         throw PoisonPillEventException("Invalid event: missing userId")
 *     }
 *     userService.createUser(event.userId)
 * }
 * ```
 */
class PoisonPillEventException(
    message: String? = null,
    cause: Throwable? = null,
) : EventConsumptionException(message, cause)

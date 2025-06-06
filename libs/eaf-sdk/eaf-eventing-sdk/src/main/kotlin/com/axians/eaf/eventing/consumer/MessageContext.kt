package com.axians.eaf.eventing.consumer

import io.nats.client.Message
import java.time.Duration

/**
 * Provides access to the raw NATS message and acknowledgment controls.
 *
 * This interface allows listener methods to access message metadata,
 * headers, and manual acknowledgment controls when needed for advanced
 * processing scenarios.
 *
 * Example usage:
 * ```kotlin
 * @NatsJetStreamListener("events.user.>", autoAck = false)
 * fun handleUserEvent(event: UserEvent, context: MessageContext) {
 *     val correlationId = context.getHeader("correlationId")
 *
 *     try {
 *         processEvent(event, correlationId)
 *         context.ack() // Manual acknowledgment
 *     } catch (e: RetryableException) {
 *         context.nak(Duration.ofSeconds(30)) // Retry after 30 seconds
 *     } catch (e: PoisonPillException) {
 *         context.term() // Terminate message
 *     }
 * }
 * ```
 */
interface MessageContext {
    /**
     * The raw NATS message.
     *
     * Provides access to all message properties including headers,
     * reply subjects, and metadata.
     */
    val message: Message

    /**
     * The tenant ID extracted from the message subject or metadata.
     *
     * This is automatically extracted by the EAF SDK to support
     * multi-tenant event processing.
     */
    val tenantId: String

    /**
     * The original subject the message was published to.
     *
     * This may differ from [message].subject if the message
     * has been forwarded or transformed.
     */
    val subject: String

    /**
     * Gets a header value from the message.
     *
     * @param name the header name
     * @return the header value, or null if not present
     */
    fun getHeader(name: String): String?

    /**
     * Gets all headers from the message.
     *
     * @return a map of header names to values
     */
    fun getHeaders(): Map<String, String>

    /**
     * Acknowledges the message as successfully processed.
     *
     * This indicates that the message has been successfully processed
     * and should not be redelivered.
     */
    fun ack()

    /**
     * Negatively acknowledges the message.
     *
     * This indicates that the message could not be processed at this time
     * but should be retried. The message will be redelivered according
     * to the consumer's retry policy.
     */
    fun nak()

    /**
     * Negatively acknowledges the message with a delay.
     *
     * This indicates that the message could not be processed at this time
     * but should be retried after the specified delay.
     *
     * @param delay the delay before redelivery
     */
    fun nak(delay: Duration)

    /**
     * Terminates the message.
     *
     * This indicates that the message is a "poison pill" and should not
     * be retried. The message will be removed from the stream or moved
     * to a dead letter queue.
     */
    fun term()

    /**
     * Checks if the message has already been acknowledged.
     *
     * @return true if the message has been ack'd, nak'd, or term'd
     */
    fun isAcknowledged(): Boolean
}

package com.axians.eaf.eventing.consumer

import io.nats.client.Message
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Default implementation of MessageContext that wraps a NATS message.
 *
 * This implementation provides access to message metadata and acknowledgment
 * controls while tracking acknowledgment state to prevent double-acknowledgment.
 */
class DefaultMessageContext(
    override val message: Message,
    override val tenantId: String,
) : MessageContext {
    private val logger = LoggerFactory.getLogger(DefaultMessageContext::class.java)
    private val acknowledged = AtomicBoolean(false)

    override val subject: String = message.subject

    override fun getHeader(name: String): String? = message.headers?.getFirst(name)

    override fun getHeaders(): Map<String, String> {
        val headers = message.headers ?: return emptyMap()

        return headers
            .entrySet()
            .associate { entry ->
                entry.key to entry.value.firstOrNull()
            }.filterValues { it != null } as Map<String, String>
    }

    override fun ack() {
        if (acknowledged.getAndSet(true)) {
            logger.warn("Message already acknowledged: {}", message.subject)
            return
        }

        try {
            message.ack()
            logger.debug("Acknowledged message: {}", message.subject)
        } catch (e: Exception) {
            acknowledged.set(false) // Reset on failure
            logger.error("Failed to acknowledge message {}: {}", message.subject, e.message, e)
            throw RuntimeException("Failed to acknowledge message", e)
        }
    }

    override fun nak() {
        if (acknowledged.getAndSet(true)) {
            logger.warn("Message already acknowledged: {}", message.subject)
            return
        }

        try {
            message.nak()
            logger.debug("Negatively acknowledged message: {}", message.subject)
        } catch (e: Exception) {
            acknowledged.set(false) // Reset on failure
            logger.error("Failed to nak message {}: {}", message.subject, e.message, e)
            throw RuntimeException("Failed to nak message", e)
        }
    }

    override fun nak(delay: Duration) {
        if (acknowledged.getAndSet(true)) {
            logger.warn("Message already acknowledged: {}", message.subject)
            return
        }

        try {
            message.nakWithDelay(delay)
            logger.debug("Negatively acknowledged message with delay {}: {}", delay, message.subject)
        } catch (e: Exception) {
            acknowledged.set(false) // Reset on failure
            logger.error(
                "Failed to nak message {} with delay {}: {}",
                message.subject,
                delay,
                e.message,
                e,
            )
            throw RuntimeException("Failed to nak message with delay", e)
        }
    }

    override fun term() {
        if (acknowledged.getAndSet(true)) {
            logger.warn("Message already acknowledged: {}", message.subject)
            return
        }

        try {
            message.term()
            logger.debug("Terminated message: {}", message.subject)
        } catch (e: Exception) {
            acknowledged.set(false) // Reset on failure
            logger.error("Failed to terminate message {}: {}", message.subject, e.message, e)
            throw RuntimeException("Failed to terminate message", e)
        }
    }

    override fun isAcknowledged(): Boolean = acknowledged.get()
}

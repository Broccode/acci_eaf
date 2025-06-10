package com.axians.eaf.eventing.consumer

import io.nats.client.Message

/**
 * Enhanced MessageContext that provides access to EAF security context
 * information extracted from message headers.
 *
 * This context automatically extracts tenant ID, user ID, and correlation ID
 * from message metadata and makes them available to event handlers.
 */
interface EafMessageContext : MessageContext {
    /**
     * The user ID extracted from message headers, if available.
     * This represents the user who initiated the action that led to this event.
     */
    val userId: String?

    /**
     * The correlation ID for tracing requests across services.
     * This can be used to correlate events with the original request.
     */
    val correlationId: String?

    /**
     * Checks if the message contains security context information.
     * @return true if user ID is available in the message headers
     */
    fun hasSecurityContext(): Boolean
}

/**
 * Default implementation of EafMessageContext that extracts security context
 * from NATS message headers.
 */
class DefaultEafMessageContext(
    private val delegate: MessageContext,
) : EafMessageContext,
    MessageContext by delegate {
    companion object {
        private const val USER_ID_HEADER = "eaf.user.id"
        private const val TENANT_ID_HEADER = "eaf.tenant.id"
        private const val CORRELATION_ID_HEADER = "eaf.correlation.id"
    }

    override val userId: String? by lazy {
        getHeader(USER_ID_HEADER)
    }

    override val correlationId: String? by lazy {
        getHeader(CORRELATION_ID_HEADER)
    }

    override fun hasSecurityContext(): Boolean = userId != null
}

/**
 * Factory for creating EafMessageContext instances.
 */
object EafMessageContextFactory {
    /**
     * Creates an EafMessageContext from a NATS message.
     * Automatically extracts tenant ID and other context information.
     */
    fun create(message: Message): EafMessageContext {
        val tenantId = extractTenantId(message)
        val baseContext = DefaultMessageContext(message, tenantId)
        return DefaultEafMessageContext(baseContext)
    }

    /**
     * Extracts tenant ID from the message subject or headers.
     * The tenant ID is expected to be the first part of the subject.
     */
    private fun extractTenantId(message: Message): String {
        // First try to get from headers
        message.headers?.getFirst("eaf.tenant.id")?.let { return it }

        // Fallback: extract from subject (format: tenantId.subject.parts)
        val subjectParts = message.subject.split(".")
        if (subjectParts.isNotEmpty()) {
            return subjectParts[0]
        }

        throw IllegalArgumentException("Cannot extract tenant ID from message subject: ${message.subject}")
    }
}

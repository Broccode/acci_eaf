package com.axians.eaf.eventing.consumer

import com.axians.eaf.core.security.EafSecurityContextHolder
import com.axians.eaf.core.security.HasTenantId
import com.axians.eaf.core.security.HasUserId
import io.nats.client.Message
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.security.Principal

/**
 * Context-aware message processor that establishes security context
 * from message headers before invoking event handlers.
 *
 * This processor automatically:
 * - Extracts security context from message headers
 * - Establishes Spring Security context for the duration of event processing
 * - Clears security context after processing
 */
@Component
@ConditionalOnBean(EafSecurityContextHolder::class)
class ContextAwareMessageProcessor {
    companion object {
        private val logger = LoggerFactory.getLogger(ContextAwareMessageProcessor::class.java)
    }

    /**
     * Processes a message with security context established from message headers.
     */
    fun processWithContext(
        message: Message,
        processor: () -> Unit,
    ) {
        val eafContext = EafMessageContextFactory.create(message)

        // Establish security context if available
        if (eafContext.hasSecurityContext()) {
            val authentication = createAuthenticationFromContext(eafContext)
            val securityContext = SecurityContextHolder.createEmptyContext()
            securityContext.authentication = authentication
            SecurityContextHolder.setContext(securityContext)

            logger.debug(
                "Established security context for message processing - tenant: {}, user: {}",
                eafContext.tenantId,
                eafContext.userId,
            )
        }

        try {
            // Process the message with security context established
            processor()
        } finally {
            // Always clear security context after processing
            SecurityContextHolder.clearContext()
            logger.debug("Cleared security context after message processing")
        }
    }

    /**
     * Creates a Spring Security Authentication object from the message context.
     */
    private fun createAuthenticationFromContext(context: EafMessageContext): Authentication =
        MessageAuthentication(
            tenantId = context.tenantId,
            userId = context.userId,
            correlationId = context.correlationId,
        )
}

/**
 * Simple Authentication implementation for message processing context.
 * This provides the minimum required information for EAF security context.
 */
private class MessageAuthentication(
    private val tenantId: String,
    private val userId: String?,
    private val correlationId: String?,
) : Authentication,
    HasTenantId,
    HasUserId {
    private val principal = MessagePrincipal(tenantId, userId, correlationId)

    override fun getName(): String = userId ?: "message-processor"

    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()

    override fun getCredentials(): Any? = null

    override fun getDetails(): Any? = correlationId

    override fun getPrincipal(): Principal = principal

    override fun isAuthenticated(): Boolean = true

    override fun setAuthenticated(isAuthenticated: Boolean) {
        // No-op for message authentication
    }

    override fun getTenantId(): String = tenantId

    override fun getUserId(): String? = userId
}

/**
 * Simple Principal implementation for message processing context.
 */
private class MessagePrincipal(
    private val tenantId: String,
    private val userId: String?,
    private val correlationId: String?,
) : Principal,
    HasTenantId,
    HasUserId {
    override fun getName(): String = userId ?: "message-processor"

    override fun getTenantId(): String = tenantId

    override fun getUserId(): String? = userId

    fun getCorrelationId(): String? = correlationId
}

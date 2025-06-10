package com.axians.eaf.eventing

import com.axians.eaf.core.security.EafSecurityContextHolder
import io.nats.client.api.PublishAck
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Service

/**
 * Context-aware NATS event publisher that automatically includes security context
 * information (tenant ID, user ID) in published events.
 *
 * This publisher wraps the default implementation and automatically:
 * - Extracts tenant ID from security context if not provided
 * - Adds user ID to event metadata
 * - Includes correlation information for tracing
 */
@Service
@ConditionalOnBean(EafSecurityContextHolder::class)
class ContextAwareNatsEventPublisher(
    private val delegate: DefaultNatsEventPublisher,
    private val securityContextHolder: EafSecurityContextHolder,
) : NatsEventPublisher {
    companion object {
        private val logger = LoggerFactory.getLogger(ContextAwareNatsEventPublisher::class.java)
        private const val USER_ID_HEADER = "eaf.user.id"
        private const val TENANT_ID_HEADER = "eaf.tenant.id"
        private const val CORRELATION_ID_HEADER = "eaf.correlation.id"
    }

    /**
     * Publishes an event using the tenant ID from the security context.
     * If no security context is available, this method will fail.
     */
    suspend fun publish(
        subject: String,
        event: Any,
    ): PublishAck {
        val tenantId = securityContextHolder.getTenantId()
        return publish(subject, tenantId, event)
    }

    /**
     * Publishes an event with metadata using the tenant ID from the security context.
     */
    suspend fun publish(
        subject: String,
        event: Any,
        metadata: Map<String, Any>,
    ): PublishAck {
        val tenantId = securityContextHolder.getTenantId()
        return publish(subject, tenantId, event, metadata)
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
        val enrichedMetadata = enrichMetadataWithContext(metadata, tenantId)

        logger.debug(
            "Publishing event with context - tenant: {}, user: {}, subject: {}",
            tenantId,
            enrichedMetadata[USER_ID_HEADER],
            subject,
        )

        return delegate.publish(subject, tenantId, event, enrichedMetadata)
    }

    /**
     * Enriches the metadata with security context information.
     */
    private fun enrichMetadataWithContext(
        originalMetadata: Map<String, Any>,
        tenantId: String,
    ): Map<String, Any> {
        val enrichedMetadata = originalMetadata.toMutableMap()

        // Always include tenant ID in metadata
        enrichedMetadata[TENANT_ID_HEADER] = tenantId

        // Add user ID if available
        securityContextHolder.getUserId()?.let { userId ->
            enrichedMetadata[USER_ID_HEADER] = userId
        }

        // Add correlation ID for tracing (could be enhanced with actual correlation ID from request)
        if (!enrichedMetadata.containsKey(CORRELATION_ID_HEADER)) {
            enrichedMetadata[CORRELATION_ID_HEADER] = generateCorrelationId()
        }

        return enrichedMetadata
    }

    /**
     * Generates a simple correlation ID. In a real implementation, this might
     * extract an existing correlation ID from the request context.
     */
    private fun generateCorrelationId(): String =
        java.util.UUID
            .randomUUID()
            .toString()
}

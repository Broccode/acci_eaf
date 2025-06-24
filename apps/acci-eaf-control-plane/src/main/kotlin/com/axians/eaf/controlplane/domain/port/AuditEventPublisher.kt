package com.axians.eaf.controlplane.domain.port

import com.axians.eaf.controlplane.domain.model.audit.AuditEvent

/**
 * Domain port for publishing audit events. This interface defines the contract for audit event
 * publishing without depending on specific infrastructure.
 */
interface AuditEventPublisher {
    /**
     * Publishes an audit event for the specified tenant.
     *
     * @param tenantId The tenant identifier
     * @param event The audit event to publish
     */
    suspend fun publishAuditEvent(
        tenantId: String,
        event: AuditEvent,
    )

    /**
     * Publishes an audit event with additional metadata.
     *
     * @param tenantId The tenant identifier
     * @param event The audit event to publish
     * @param metadata Additional metadata to include with the event
     */
    suspend fun publishAuditEvent(
        tenantId: String,
        event: AuditEvent,
        metadata: Map<String, Any>,
    )

    /**
     * Publishes a batch of audit events for performance optimization.
     *
     * @param tenantId The tenant identifier
     * @param events The list of audit events to publish
     */
    suspend fun publishAuditEvents(
        tenantId: String,
        events: List<AuditEvent>,
    )
}

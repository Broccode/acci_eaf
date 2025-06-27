package com.axians.eaf.controlplane.infrastructure.adapter.outbound.event

import com.axians.eaf.controlplane.domain.model.audit.AuditEvent
import com.axians.eaf.controlplane.domain.port.AuditEventPublisher
import com.axians.eaf.eventing.NatsEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * NATS-based implementation of the AuditEventPublisher domain port.
 *
 * Publishes audit events to NATS JetStream for consumption by other services and audit processing
 * systems.
 */
@Component
class NatsAuditEventPublisher(
    private val natsEventPublisher: NatsEventPublisher,
) : AuditEventPublisher {
    private val logger = LoggerFactory.getLogger(NatsAuditEventPublisher::class.java)

    override suspend fun publishAuditEvent(
        tenantId: String,
        event: AuditEvent,
    ) {
        try {
            natsEventPublisher.publish(
                subject = "audit.events.${getEventSubject(event)}",
                tenantId = tenantId,
                event = event,
            )

            logger.debug(
                "Published audit event {} for tenant {} with ID {}",
                event::class.simpleName,
                tenantId,
                event.auditEntryId.value,
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to publish audit event {} for tenant {}: {}",
                event::class.simpleName,
                tenantId,
                e.message,
                e,
            )
            throw e
        }
    }

    override suspend fun publishAuditEvent(
        tenantId: String,
        event: AuditEvent,
        metadata: Map<String, Any>,
    ) {
        try {
            natsEventPublisher.publish(
                subject = "audit.events.${getEventSubject(event)}",
                tenantId = tenantId,
                event = event,
                metadata = metadata,
            )

            logger.debug(
                "Published audit event {} for tenant {} with ID {} and metadata",
                event::class.simpleName,
                tenantId,
                event.auditEntryId.value,
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to publish audit event {} for tenant {} with metadata: {}",
                event::class.simpleName,
                tenantId,
                e.message,
                e,
            )
            throw e
        }
    }

    override suspend fun publishAuditEvents(
        tenantId: String,
        events: List<AuditEvent>,
    ) {
        for (event in events) {
            publishAuditEvent(tenantId, event)
        }

        logger.debug("Published {} audit events for tenant {}", events.size, tenantId)
    }

    /** Determines the NATS subject suffix based on the audit event type. */
    private fun getEventSubject(event: AuditEvent): String =
        when (event) {
            is AuditEvent.AuditEntryCreated -> "created"
            is AuditEvent.SecurityViolationDetected -> "security.violation"
            is AuditEvent.HighRiskActionPerformed -> "high.risk.action"
        }
}

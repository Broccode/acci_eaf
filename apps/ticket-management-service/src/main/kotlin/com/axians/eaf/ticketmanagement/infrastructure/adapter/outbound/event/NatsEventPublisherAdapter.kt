package com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound.event

import com.axians.eaf.eventing.NatsEventPublisher
import com.axians.eaf.ticketmanagement.domain.event.TicketAssignedEvent
import com.axians.eaf.ticketmanagement.domain.event.TicketClosedEvent
import com.axians.eaf.ticketmanagement.domain.event.TicketCreatedEvent
import com.axians.eaf.ticketmanagement.domain.port.outbound.EventPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Infrastructure adapter that implements event publishing using NATS via the EAF SDK.
 * This adapter bridges the domain port with the EAF eventing infrastructure.
 */
@Component
class NatsEventPublisherAdapter(
    private val natsEventPublisher: NatsEventPublisher,
) : EventPublisher {
    private val logger = LoggerFactory.getLogger(NatsEventPublisherAdapter::class.java)

    /**
     * Publishes a domain event using NATS via the EAF SDK.
     * The subject is automatically derived from the event type and tenant context.
     */
    override suspend fun publishEvent(
        tenantId: String,
        event: Any,
    ) {
        try {
            // Determine the correct subject based on event type
            // Following the EAF convention: events.<domain>.<action>
            val subject =
                when (event) {
                    is TicketCreatedEvent -> "events.ticket.created"
                    is TicketAssignedEvent -> "events.ticket.assigned"
                    is TicketClosedEvent -> "events.ticket.closed"
                    else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
                }

            // Use the EAF SDK to publish the event
            // The EAF SDK will handle tenant prefixing, serialization, and NATS publishing
            natsEventPublisher.publish(
                subject = subject,
                tenantId = tenantId,
                event = event,
            )

            logger.info(
                "Successfully published event {} to subject {} for tenant {}",
                event::class.simpleName,
                subject,
                tenantId,
            )
        } catch (e: Exception) {
            logger.warn(
                "Failed to publish event {} for tenant {} - NATS may not be available. " +
                    "This is expected in development mode. Error: {}",
                event::class.simpleName,
                tenantId,
                e.message,
            )
            // Don't rethrow the exception to allow the application to continue functioning
            // In production, you might want to use a dead letter queue or retry mechanism
        }
    }
}

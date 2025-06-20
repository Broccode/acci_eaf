package com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound.event

import com.axians.eaf.eventing.NatsEventPublisher
import com.axians.eaf.eventsourcing.adapter.EventPublisher
import com.axians.eaf.eventsourcing.model.PersistedEvent
import com.axians.eaf.ticketmanagement.domain.event.TicketAssignedEvent
import com.axians.eaf.ticketmanagement.domain.event.TicketClosedEvent
import com.axians.eaf.ticketmanagement.domain.event.TicketCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Adapter that implements the EAF SDK EventPublisher interface and publishes events to NATS.
 * This bridges the event sourcing SDK with the eventing SDK for automatic event publishing.
 */
@Component
class EafEventPublisherAdapter(
    private val natsEventPublisher: NatsEventPublisher,
) : EventPublisher {
    private val logger = LoggerFactory.getLogger(EafEventPublisherAdapter::class.java)

    /**
     * Publishes domain events to NATS after they have been persisted to the event store.
     * This method is called automatically by the AbstractAggregateRepository after saving events.
     */
    override suspend fun publishEvents(
        persistedEvents: List<PersistedEvent>,
        domainEvents: List<Any>,
        tenantId: String,
    ) {
        logger.debug(
            "Publishing {} domain events to NATS for tenant {}",
            domainEvents.size,
            tenantId,
        )

        for ((index, domainEvent) in domainEvents.withIndex()) {
            try {
                val persistedEvent = persistedEvents[index]

                // Determine the correct subject based on event type
                val subject =
                    when (domainEvent) {
                        is TicketCreatedEvent -> "events.ticket.created"
                        is TicketAssignedEvent -> "events.ticket.assigned"
                        is TicketClosedEvent -> "events.ticket.closed"
                        else -> {
                            logger.warn(
                                "Unknown event type: {}, skipping NATS publishing",
                                domainEvent::class.simpleName,
                            )
                            continue
                        }
                    }

                // Publish the domain event to NATS
                val publishAck =
                    natsEventPublisher.publish(
                        subject = subject,
                        tenantId = tenantId,
                        event = domainEvent,
                        metadata =
                            mapOf(
                                "eventId" to persistedEvent.eventId.toString(),
                                "streamId" to persistedEvent.streamId,
                                "sequenceNumber" to persistedEvent.sequenceNumber,
                                "aggregateId" to persistedEvent.aggregateId,
                                "aggregateType" to persistedEvent.aggregateType,
                            ),
                    )

                logger.info(
                    "Successfully published event {} to NATS stream {} with sequence {} for tenant {}",
                    domainEvent::class.simpleName,
                    publishAck.stream,
                    publishAck.seqno,
                    tenantId,
                )
            } catch (e: Exception) {
                logger.error(
                    "Failed to publish event {} to NATS for tenant {}: {}",
                    domainEvent::class.simpleName,
                    tenantId,
                    e.message,
                    e,
                )
                // Rethrow the exception to fail the aggregate save operation
                // This ensures transactional consistency between event store and event publishing
                throw e
            }
        }

        logger.info(
            "Successfully published all {} events to NATS for tenant {}",
            domainEvents.size,
            tenantId,
        )
    }
}

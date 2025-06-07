package com.axians.eaf.eventsourcing.example.aggregate

import com.axians.eaf.eventsourcing.adapter.AbstractAggregateRepository
import com.axians.eaf.eventsourcing.adapter.EventPublisher
import com.axians.eaf.eventsourcing.example.event.TicketAssignedEvent
import com.axians.eaf.eventsourcing.example.event.TicketClosedEvent
import com.axians.eaf.eventsourcing.example.event.TicketCreatedEvent
import com.axians.eaf.eventsourcing.model.PersistedEvent
import com.axians.eaf.eventsourcing.port.EventStoreRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository implementation for the Ticket aggregate.
 *
 * This class demonstrates how to create a concrete repository by extending
 * AbstractAggregateRepository and providing event serialization/deserialization logic.
 */
@Repository
class TicketRepository(
    eventStoreRepository: EventStoreRepository,
    objectMapper: ObjectMapper,
    eventPublisher: EventPublisher? = null,
) : AbstractAggregateRepository<Ticket, UUID>(eventStoreRepository, objectMapper, eventPublisher) {
    override fun createNew(aggregateId: UUID): Ticket = Ticket(aggregateId)

    override fun deserializeEvent(persistedEvent: PersistedEvent): Any =
        when (persistedEvent.eventType) {
            "TicketCreatedEvent" -> objectMapper.readValue<TicketCreatedEvent>(persistedEvent.payload)
            "TicketAssignedEvent" -> objectMapper.readValue<TicketAssignedEvent>(persistedEvent.payload)
            "TicketClosedEvent" -> objectMapper.readValue<TicketClosedEvent>(persistedEvent.payload)
            else -> throw IllegalArgumentException("Unknown event type: ${persistedEvent.eventType}")
        }

    override fun deserializeSnapshot(snapshotPayload: String): TicketSnapshot = objectMapper.readValue(snapshotPayload)
}

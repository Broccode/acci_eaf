package com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound

import com.axians.eaf.eventsourcing.adapter.AbstractAggregateRepository
import com.axians.eaf.eventsourcing.adapter.EventPublisher
import com.axians.eaf.eventsourcing.model.PersistedEvent
import com.axians.eaf.eventsourcing.port.EventStoreRepository
import com.axians.eaf.ticketmanagement.domain.aggregate.Ticket
import com.axians.eaf.ticketmanagement.domain.event.TicketAssignedEvent
import com.axians.eaf.ticketmanagement.domain.event.TicketClosedEvent
import com.axians.eaf.ticketmanagement.domain.event.TicketCreatedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.util.UUID

/**
 * Repository implementation for Ticket aggregates using EAF Event Sourcing SDK.
 *
 * This repository extends the AbstractAggregateRepository to provide concrete
 * implementations for event serialization/deserialization and aggregate creation
 * specific to the Ticket domain.
 */
class TicketAggregateRepository(
    eventStoreRepository: EventStoreRepository,
    objectMapper: ObjectMapper,
    eventPublisher: EventPublisher? = null,
) : AbstractAggregateRepository<Ticket, UUID>(
        eventStoreRepository,
        objectMapper,
        eventPublisher,
    ) {
    /**
     * Secondary constructor without event publisher for simpler testing setup.
     */
    constructor(eventStoreRepository: EventStoreRepository) : this(
        eventStoreRepository,
        ObjectMapper().apply {
            registerKotlinModule()
            registerModule(JavaTimeModule())
        },
        null,
    )

    override fun createNew(aggregateId: UUID): Ticket = Ticket(aggregateId)

    override fun deserializeEvent(persistedEvent: PersistedEvent): Any =
        when (persistedEvent.eventType) {
            "TicketCreatedEvent" -> objectMapper.readValue<TicketCreatedEvent>(persistedEvent.payload)
            "TicketAssignedEvent" -> objectMapper.readValue<TicketAssignedEvent>(persistedEvent.payload)
            "TicketClosedEvent" -> objectMapper.readValue<TicketClosedEvent>(persistedEvent.payload)
            else -> throw IllegalArgumentException(
                "Unknown event type: ${persistedEvent.eventType} for ticket aggregate",
            )
        }

    override fun deserializeSnapshot(snapshotPayload: String): Any {
        // Deserialize the snapshot data specific to Ticket aggregate
        return objectMapper.readValue<TicketSnapshot>(snapshotPayload)
    }
}

/**
 * Snapshot data structure for Ticket aggregate.
 * This should match the structure returned by Ticket.createSnapshot().
 */
data class TicketSnapshot(
    val aggregateId: UUID,
    val title: String,
    val description: String,
    val priority: String, // Serialized as string for JSON compatibility
    val assigneeId: String?,
    val status: String,
    val resolution: String?,
    val createdAt: String?, // Serialized as ISO string
    val assignedAt: String?,
    val closedAt: String?,
)

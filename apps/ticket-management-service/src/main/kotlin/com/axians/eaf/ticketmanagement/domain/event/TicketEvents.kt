package com.axians.eaf.ticketmanagement.domain.event

import com.axians.eaf.ticketmanagement.domain.command.TicketPriority
import java.time.Instant
import java.util.UUID

/**
 * Domain event indicating a ticket was created.
 */
data class TicketCreatedEvent(
    val ticketId: UUID,
    val title: String,
    val description: String,
    val priority: TicketPriority,
    val assigneeId: String? = null,
    val createdAt: Instant = Instant.now(),
)

/**
 * Domain event indicating a ticket was assigned.
 */
data class TicketAssignedEvent(
    val ticketId: UUID,
    val assigneeId: String,
    val assignedAt: Instant = Instant.now(),
)

/**
 * Domain event indicating a ticket was closed.
 */
data class TicketClosedEvent(
    val ticketId: UUID,
    val resolution: String,
    val closedAt: Instant = Instant.now(),
)

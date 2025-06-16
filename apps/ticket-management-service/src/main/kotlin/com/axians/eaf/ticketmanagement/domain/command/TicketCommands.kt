package com.axians.eaf.ticketmanagement.domain.command

import java.util.UUID

/**
 * Command to create a new ticket.
 */
data class CreateTicketCommand(
    val ticketId: UUID,
    val title: String,
    val description: String,
    val priority: TicketPriority,
    val assigneeId: String? = null,
) {
    init {
        require(title.isNotBlank()) { "Ticket title cannot be blank" }
        require(description.isNotBlank()) { "Ticket description cannot be blank" }
    }
}

/**
 * Command to assign a ticket to someone.
 */
data class AssignTicketCommand(
    val ticketId: UUID,
    val assigneeId: String,
) {
    init {
        require(assigneeId.isNotBlank()) { "Assignee ID cannot be blank" }
    }
}

/**
 * Command to close a ticket.
 */
data class CloseTicketCommand(
    val ticketId: UUID,
    val resolution: String,
) {
    init {
        require(resolution.isNotBlank()) { "Resolution cannot be blank" }
    }
}

/**
 * Priority levels for tickets.
 */
enum class TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

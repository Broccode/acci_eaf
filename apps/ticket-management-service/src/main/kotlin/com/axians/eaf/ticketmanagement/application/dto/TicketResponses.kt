package com.axians.eaf.ticketmanagement.application.dto

import com.axians.eaf.ticketmanagement.domain.command.TicketPriority
import java.time.Instant
import java.util.UUID

/**
 * Response DTO for ticket creation operations.
 */
data class CreateTicketResponse(
    val ticketId: UUID,
    val title: String,
    val description: String,
    val priority: TicketPriority,
    val assigneeId: String? = null,
    val createdAt: Instant,
)

/**
 * Response DTO for ticket assignment operations.
 */
data class AssignTicketResponse(
    val ticketId: UUID,
    val assigneeId: String,
    val assignedAt: Instant,
)

/**
 * Response DTO for ticket closure operations.
 */
data class CloseTicketResponse(
    val ticketId: UUID,
    val resolution: String,
    val closedAt: Instant,
)

/**
 * Comprehensive ticket response DTO for queries.
 */
data class TicketQueryResponse(
    val ticketId: UUID,
    val title: String,
    val description: String,
    val priority: TicketPriority,
    val assigneeId: String? = null,
    val status: String,
    val resolution: String? = null,
    val createdAt: Instant?,
    val assignedAt: Instant? = null,
    val closedAt: Instant? = null,
)

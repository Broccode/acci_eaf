package com.axians.eaf.ticketmanagement.application.request

import com.axians.eaf.ticketmanagement.domain.command.TicketPriority
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * Request DTO for creating a new ticket.
 */
data class CreateTicketRequest(
    @field:NotBlank(message = "Ticket title cannot be blank")
    @field:Size(max = 200, message = "Ticket title cannot exceed 200 characters")
    val title: String,
    @field:NotBlank(message = "Ticket description cannot be blank")
    @field:Size(max = 2000, message = "Ticket description cannot exceed 2000 characters")
    val description: String,
    @field:NotNull(message = "Ticket priority is required")
    val priority: TicketPriority,
    @field:Size(max = 100, message = "Assignee ID cannot exceed 100 characters")
    val assigneeId: String? = null,
) {
    init {
        require(title.isNotBlank()) { "Ticket title cannot be blank" }
        require(description.isNotBlank()) { "Ticket description cannot be blank" }
    }
}

/**
 * Request DTO for assigning a ticket.
 */
data class AssignTicketRequest(
    @field:NotBlank(message = "Assignee ID cannot be blank")
    @field:Size(max = 100, message = "Assignee ID cannot exceed 100 characters")
    val assigneeId: String,
) {
    init {
        require(assigneeId.isNotBlank()) { "Assignee ID cannot be blank" }
    }
}

/**
 * Request DTO for closing a ticket.
 */
data class CloseTicketRequest(
    @field:NotBlank(message = "Resolution cannot be blank")
    @field:Size(max = 1000, message = "Resolution cannot exceed 1000 characters")
    val resolution: String,
) {
    init {
        require(resolution.isNotBlank()) { "Resolution cannot be blank" }
    }
}

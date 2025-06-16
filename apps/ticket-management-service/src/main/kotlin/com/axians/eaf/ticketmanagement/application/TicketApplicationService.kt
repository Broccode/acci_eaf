package com.axians.eaf.ticketmanagement.application

import com.axians.eaf.ticketmanagement.application.dto.AssignTicketResponse
import com.axians.eaf.ticketmanagement.application.dto.CloseTicketResponse
import com.axians.eaf.ticketmanagement.application.dto.CreateTicketResponse
import com.axians.eaf.ticketmanagement.application.dto.TicketQueryResponse
import com.axians.eaf.ticketmanagement.application.query.GetTicketByIdQuery
import com.axians.eaf.ticketmanagement.application.query.ListTicketsQuery
import com.axians.eaf.ticketmanagement.application.request.AssignTicketRequest
import com.axians.eaf.ticketmanagement.application.request.CloseTicketRequest
import com.axians.eaf.ticketmanagement.application.request.CreateTicketRequest
import com.axians.eaf.ticketmanagement.domain.command.AssignTicketCommand
import com.axians.eaf.ticketmanagement.domain.command.CloseTicketCommand
import com.axians.eaf.ticketmanagement.domain.command.CreateTicketCommand
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Application service for ticket operations.
 *
 * This service provides the main application interface for ticket operations,
 * coordinating between command and query handlers while providing input validation
 * and error handling.
 */
@Service
class TicketApplicationService(
    private val commandHandler: TicketCommandHandler,
    private val queryHandler: TicketQueryHandler,
) {
    /**
     * Creates a new ticket.
     */
    suspend fun createTicket(request: CreateTicketRequest): CreateTicketResponse {
        // Additional business validation can be added here
        validateCreateTicketRequest(request)

        val command =
            CreateTicketCommand(
                ticketId = UUID.randomUUID(),
                title = request.title,
                description = request.description,
                priority = request.priority,
                assigneeId = request.assigneeId,
            )

        return commandHandler.handle(command)
    }

    /**
     * Assigns a ticket to a user.
     */
    suspend fun assignTicket(
        ticketId: UUID,
        request: AssignTicketRequest,
    ): AssignTicketResponse {
        validateAssignTicketRequest(request)

        val command =
            AssignTicketCommand(
                ticketId = ticketId,
                assigneeId = request.assigneeId,
            )

        return commandHandler.handle(command)
    }

    /**
     * Closes a ticket with a resolution.
     */
    suspend fun closeTicket(
        ticketId: UUID,
        request: CloseTicketRequest,
    ): CloseTicketResponse {
        validateCloseTicketRequest(request)

        val command =
            CloseTicketCommand(
                ticketId = ticketId,
                resolution = request.resolution,
            )

        return commandHandler.handle(command)
    }

    /**
     * Retrieves a ticket by its ID.
     */
    suspend fun getTicketById(ticketId: UUID): TicketQueryResponse {
        val query = GetTicketByIdQuery(ticketId)
        return queryHandler.handle(query)
    }

    /**
     * Retrieves all tickets.
     */
    suspend fun listTickets(): List<TicketQueryResponse> {
        val query = ListTicketsQuery()
        return queryHandler.handle(query)
    }

    // Private validation methods

    private fun validateCreateTicketRequest(request: CreateTicketRequest) {
        // The request's init block already validates title and description
        // Additional business rules can be added here
        if (request.title.length > 200) {
            throw IllegalArgumentException("Ticket title cannot exceed 200 characters")
        }
        if (request.description.length > 2000) {
            throw IllegalArgumentException("Ticket description cannot exceed 2000 characters")
        }
    }

    private fun validateAssignTicketRequest(request: AssignTicketRequest) {
        // The request's init block already validates assigneeId
        // Additional business rules can be added here
        if (request.assigneeId.length > 100) {
            throw IllegalArgumentException("Assignee ID cannot exceed 100 characters")
        }
    }

    private fun validateCloseTicketRequest(request: CloseTicketRequest) {
        // The request's init block already validates resolution
        // Additional business rules can be added here
        if (request.resolution.length > 1000) {
            throw IllegalArgumentException("Resolution cannot exceed 1000 characters")
        }
    }
}

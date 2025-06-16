package com.axians.eaf.ticketmanagement.application

import com.axians.eaf.eventsourcing.port.AggregateRepository
import com.axians.eaf.ticketmanagement.application.dto.TicketQueryResponse
import com.axians.eaf.ticketmanagement.application.query.GetTicketByIdQuery
import com.axians.eaf.ticketmanagement.application.query.ListTicketsQuery
import com.axians.eaf.ticketmanagement.domain.aggregate.Ticket
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Query handler for ticket operations.
 *
 * This service handles all ticket-related queries following CQRS principles.
 * It provides read-only access to ticket data.
 */
@Service
class TicketQueryHandler(
    private val aggregateRepository: AggregateRepository<Ticket, UUID>,
) {
    // For simplicity in the pilot, we'll use a default tenant context
    // In a real application, this would come from the security context
    private val defaultTenantId = "default-tenant"

    /**
     * Handles retrieval of a single ticket by ID.
     */
    suspend fun handle(query: GetTicketByIdQuery): TicketQueryResponse {
        val ticket =
            aggregateRepository.load(defaultTenantId, query.ticketId)
                ?: throw IllegalArgumentException("Ticket not found: ${query.ticketId}")

        return mapTicketToResponse(ticket)
    }

    /**
     * Handles retrieval of all tickets.
     *
     * Note: The EAF AggregateRepository doesn't provide a findAll method as it's
     * designed for event sourcing patterns. In a real implementation, this would
     * typically be handled by:
     * 1. A separate read model/projection
     * 2. A query-side database
     * 3. An event-based projector that maintains ticket summaries
     *
     * For the pilot, we'll return an empty list with a TODO for proper implementation.
     */
    suspend fun handle(query: ListTicketsQuery): List<TicketQueryResponse> {
        // TODO: Implement proper read model for listing tickets
        // This would typically involve:
        // 1. Creating a TicketSummaryProjector (as mentioned in the story)
        // 2. Maintaining a read model table (ticket_summary)
        // 3. Querying the read model instead of the event store

        return emptyList()
    }

    /**
     * Maps a Ticket aggregate to a TicketQueryResponse DTO.
     */
    private fun mapTicketToResponse(ticket: Ticket): TicketQueryResponse =
        TicketQueryResponse(
            ticketId = ticket.aggregateId,
            title = ticket.getTitle(),
            description = ticket.getDescription(),
            priority = ticket.getPriority(),
            assigneeId = ticket.getAssigneeId(),
            status = ticket.getStatus().toString(),
            resolution = ticket.getResolution(),
            createdAt = ticket.getCreatedAt(),
            assignedAt = ticket.getAssignedAt(),
            closedAt = ticket.getClosedAt(),
        )
}

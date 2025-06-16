package com.axians.eaf.ticketmanagement.application

import com.axians.eaf.eventsourcing.port.AggregateRepository
import com.axians.eaf.ticketmanagement.application.dto.AssignTicketResponse
import com.axians.eaf.ticketmanagement.application.dto.CloseTicketResponse
import com.axians.eaf.ticketmanagement.application.dto.CreateTicketResponse
import com.axians.eaf.ticketmanagement.domain.aggregate.Ticket
import com.axians.eaf.ticketmanagement.domain.command.AssignTicketCommand
import com.axians.eaf.ticketmanagement.domain.command.CloseTicketCommand
import com.axians.eaf.ticketmanagement.domain.command.CreateTicketCommand
import com.axians.eaf.ticketmanagement.domain.event.TicketAssignedEvent
import com.axians.eaf.ticketmanagement.domain.event.TicketClosedEvent
import com.axians.eaf.ticketmanagement.domain.event.TicketCreatedEvent
import com.axians.eaf.ticketmanagement.domain.port.outbound.EventPublisher
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Command handler for ticket operations.
 *
 * This service handles all ticket-related commands following CQRS principles.
 * It coordinates between the domain model and infrastructure concerns.
 */
@Service
class TicketCommandHandler(
    private val aggregateRepository: AggregateRepository<Ticket, UUID>,
    private val eventPublisher: EventPublisher,
) {
    // For simplicity in the pilot, we'll use a default tenant context
    // In a real application, this would come from the security context
    private val defaultTenantId = "default-tenant"

    /**
     * Handles the creation of a new ticket.
     */
    suspend fun handle(command: CreateTicketCommand): CreateTicketResponse {
        // Create new ticket aggregate
        val ticket = aggregateRepository.createNew(command.ticketId)

        // Execute the command on the aggregate
        ticket.handle(command)

        // Save the aggregate to the event store
        aggregateRepository.save(defaultTenantId, ticket)

        // Publish domain events
        ticket.getUncommittedEvents().forEach { event ->
            when (event) {
                is TicketCreatedEvent ->
                    eventPublisher.publishEvent(
                        defaultTenantId,
                        event,
                    )
            }
        }

        // Return response DTO
        return CreateTicketResponse(
            ticketId = command.ticketId,
            title = command.title,
            description = command.description,
            priority = command.priority,
            assigneeId = command.assigneeId,
            createdAt = ticket.getCreatedAt()!!,
        )
    }

    /**
     * Handles the assignment of a ticket to a user.
     */
    suspend fun handle(command: AssignTicketCommand): AssignTicketResponse {
        // Load existing ticket aggregate
        val ticket =
            aggregateRepository.load(defaultTenantId, command.ticketId)
                ?: throw IllegalArgumentException("Ticket not found: ${command.ticketId}")

        // Execute the command on the aggregate
        ticket.handle(command)

        // Save the updated aggregate
        aggregateRepository.save(defaultTenantId, ticket)

        // Publish domain events
        ticket.getUncommittedEvents().forEach { event ->
            when (event) {
                is TicketAssignedEvent ->
                    eventPublisher.publishEvent(
                        defaultTenantId,
                        event,
                    )
            }
        }

        // Return response DTO
        return AssignTicketResponse(
            ticketId = command.ticketId,
            assigneeId = command.assigneeId,
            assignedAt = ticket.getAssignedAt()!!,
        )
    }

    /**
     * Handles the closure of a ticket.
     */
    suspend fun handle(command: CloseTicketCommand): CloseTicketResponse {
        // Load existing ticket aggregate
        val ticket =
            aggregateRepository.load(defaultTenantId, command.ticketId)
                ?: throw IllegalArgumentException("Ticket not found: ${command.ticketId}")

        // Execute the command on the aggregate
        ticket.handle(command)

        // Save the updated aggregate
        aggregateRepository.save(defaultTenantId, ticket)

        // Publish domain events
        ticket.getUncommittedEvents().forEach { event ->
            when (event) {
                is TicketClosedEvent ->
                    eventPublisher.publishEvent(
                        defaultTenantId,
                        event,
                    )
            }
        }

        // Return response DTO
        return CloseTicketResponse(
            ticketId = command.ticketId,
            resolution = command.resolution,
            closedAt = ticket.getClosedAt()!!,
        )
    }
}

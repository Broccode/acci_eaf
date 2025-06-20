package com.axians.eaf.ticketmanagement.infrastructure.adapter.input.messaging

import com.axians.eaf.eventing.consumer.EafProjectorEventHandler
import com.axians.eaf.ticketmanagement.domain.aggregate.TicketStatus
import com.axians.eaf.ticketmanagement.domain.event.TicketAssignedEvent
import com.axians.eaf.ticketmanagement.domain.event.TicketClosedEvent
import com.axians.eaf.ticketmanagement.domain.event.TicketCreatedEvent
import com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound.TicketSummaryRepository
import com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound.entity.TicketSummary
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Projector that maintains the TicketSummary read model.
 *
 * This projector listens to ticket domain events from NATS JetStream and updates
 * the denormalized ticket_summary table for optimized read operations. It ensures
 * idempotency through the EAF projector framework and maintains proper tenant isolation.
 *
 * The projector handles the following events:
 * - TicketCreatedEvent: Creates initial ticket summary
 * - TicketAssignedEvent: Updates assignee information
 * - TicketClosedEvent: Updates status and resolution
 */
@Component
@Transactional
class TicketSummaryProjector(
    private val ticketSummaryRepository: TicketSummaryRepository,
) {
    private val logger = LoggerFactory.getLogger(TicketSummaryProjector::class.java)

    init {
        logger.info("TicketSummaryProjector initialized - ready to process events!")
    }

    /**
     * Handles TicketCreatedEvent to create initial ticket summary.
     *
     * This method creates a new TicketSummary record when a ticket is created.
     * It extracts all relevant information from the event and stores it in the read model.
     */
    @EafProjectorEventHandler(
        projectorName = "ticket-created-projector",
        subject = "events.ticket.created",
    )
    fun handleTicketCreatedEvent(
        event: TicketCreatedEvent,
        eventId: UUID,
        tenantId: String,
    ) {
        logger.debug(
            "Processing TicketCreatedEvent for ticket {} in tenant {} (eventId: {})",
            event.ticketId,
            tenantId,
            eventId,
        )

        // Check if ticket summary already exists (defensive programming)
        val existingTicket = ticketSummaryRepository.findById(event.ticketId)
        if (existingTicket.isPresent) {
            logger.warn(
                "Ticket summary already exists for ticket {} in tenant {}, skipping create event",
                event.ticketId,
                tenantId,
            )
            return
        }

        // Create new ticket summary from creation event
        val ticketSummary =
            TicketSummary(
                id = event.ticketId,
                tenantId = tenantId,
                title = event.title,
                description = event.description,
                priority = event.priority.name,
                status = TicketStatus.OPEN.name,
                assigneeId = event.assigneeId,
                createdAt = event.createdAt,
                assignedAt = null, // Assignment timestamp is only set on explicit assignment events
                lastEventSequenceNumber = 1L,
            )

        ticketSummaryRepository.save(ticketSummary)

        logger.info(
            "Created ticket summary for ticket {} in tenant {} (eventId: {})",
            event.ticketId,
            tenantId,
            eventId,
        )
    }

    /**
     * Handles TicketAssignedEvent to update assignee information.
     *
     * This method updates the existing TicketSummary with assignee information
     * when a ticket is assigned to a user.
     */
    @EafProjectorEventHandler(
        projectorName = "ticket-assigned-projector",
        subject = "events.ticket.assigned",
    )
    fun handleTicketAssignedEvent(
        event: TicketAssignedEvent,
        eventId: UUID,
        tenantId: String,
    ) {
        logger.debug(
            "Processing TicketAssignedEvent for ticket {} in tenant {} (eventId: {})",
            event.ticketId,
            tenantId,
            eventId,
        )

        // Find existing ticket summary
        val existingTicket = ticketSummaryRepository.findById(event.ticketId)
        if (existingTicket.isEmpty) {
            logger.warn(
                "Ticket summary not found for ticket {} in tenant {}, skipping assignment event",
                event.ticketId,
                tenantId,
            )
            return
        }

        // Update with assignment information
        val currentSummary = existingTicket.get()
        val updatedSummary =
            currentSummary.updateWith(
                assigneeId = event.assigneeId,
                assignedAt = event.assignedAt,
                lastEventSequenceNumber = currentSummary.lastEventSequenceNumber + 1,
            )

        ticketSummaryRepository.save(updatedSummary)

        logger.info(
            "Updated ticket summary for assignment: ticket {} assigned to {} in tenant {} (eventId: {})",
            event.ticketId,
            event.assigneeId,
            tenantId,
            eventId,
        )
    }

    /**
     * Handles TicketClosedEvent to update status and resolution.
     *
     * This method updates the existing TicketSummary with closure information
     * when a ticket is closed with a resolution.
     */
    @EafProjectorEventHandler(
        projectorName = "ticket-closed-projector",
        subject = "events.ticket.closed",
    )
    fun handleTicketClosedEvent(
        event: TicketClosedEvent,
        eventId: UUID,
        tenantId: String,
    ) {
        logger.debug(
            "Processing TicketClosedEvent for ticket {} in tenant {} (eventId: {})",
            event.ticketId,
            tenantId,
            eventId,
        )

        // Find existing ticket summary
        val existingTicket = ticketSummaryRepository.findById(event.ticketId)
        if (existingTicket.isEmpty) {
            logger.warn(
                "Ticket summary not found for ticket {} in tenant {}, skipping closure event",
                event.ticketId,
                tenantId,
            )
            return
        }

        // Update with closure information
        val currentSummary = existingTicket.get()
        val updatedSummary =
            currentSummary.updateWith(
                status = TicketStatus.CLOSED.name,
                resolution = event.resolution,
                closedAt = event.closedAt,
                lastEventSequenceNumber = currentSummary.lastEventSequenceNumber + 1,
            )

        ticketSummaryRepository.save(updatedSummary)

        logger.info(
            "Updated ticket summary for closure: ticket {} closed in tenant {} (eventId: {})",
            event.ticketId,
            tenantId,
            eventId,
        )
    }
}

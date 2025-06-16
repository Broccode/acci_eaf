package com.axians.eaf.ticketmanagement.domain.aggregate

import com.axians.eaf.eventsourcing.annotation.AggregateIdentifier
import com.axians.eaf.eventsourcing.annotation.EafAggregate
import com.axians.eaf.eventsourcing.annotation.EafCommandHandler
import com.axians.eaf.eventsourcing.annotation.EafEventSourcingHandler
import com.axians.eaf.eventsourcing.model.AbstractAggregateRoot
import com.axians.eaf.ticketmanagement.domain.command.AssignTicketCommand
import com.axians.eaf.ticketmanagement.domain.command.CloseTicketCommand
import com.axians.eaf.ticketmanagement.domain.command.CreateTicketCommand
import com.axians.eaf.ticketmanagement.domain.command.TicketPriority
import com.axians.eaf.ticketmanagement.domain.event.TicketAssignedEvent
import com.axians.eaf.ticketmanagement.domain.event.TicketClosedEvent
import com.axians.eaf.ticketmanagement.domain.event.TicketCreatedEvent
import java.time.Instant
import java.util.UUID

/**
 * Ticket aggregate root for the ticket management pilot service.
 *
 * This aggregate demonstrates EAF event sourcing capabilities and follows
 * Domain-Driven Design principles. It handles commands related to ticket
 * lifecycle management and maintains state through event sourcing.
 */
@EafAggregate("Ticket")
class Ticket() : AbstractAggregateRoot<UUID>() {
    @AggregateIdentifier
    override lateinit var aggregateId: UUID
        private set

    private var title: String = ""
    private var description: String = ""
    private var priority: TicketPriority = TicketPriority.MEDIUM
    private var assigneeId: String? = null
    private var status: TicketStatus = TicketStatus.OPEN
    private var resolution: String? = null
    private var createdAt: Instant? = null
    private var assignedAt: Instant? = null
    private var closedAt: Instant? = null

    /**
     * Creates a ticket aggregate with a specific ID.
     * This constructor is used by the repository when loading aggregates.
     */
    constructor(ticketId: UUID) : this() {
        this.aggregateId = ticketId
    }

    // Command Handlers

    @EafCommandHandler
    fun handle(command: CreateTicketCommand) {
        // Validate business rules
        require(version == 0L) { "Ticket already exists" }

        // Apply the domain event
        apply(
            TicketCreatedEvent(
                ticketId = command.ticketId,
                title = command.title,
                description = command.description,
                priority = command.priority,
                assigneeId = command.assigneeId,
            ),
        )
    }

    @EafCommandHandler
    fun handle(command: AssignTicketCommand) {
        // Validate business rules
        require(::aggregateId.isInitialized) { "Ticket does not exist" }
        require(status == TicketStatus.OPEN) { "Cannot assign a closed ticket" }
        require(assigneeId != command.assigneeId) { "Ticket is already assigned to this person" }

        // Apply the domain event
        apply(
            TicketAssignedEvent(
                ticketId = command.ticketId,
                assigneeId = command.assigneeId,
            ),
        )
    }

    @EafCommandHandler
    fun handle(command: CloseTicketCommand) {
        // Validate business rules
        require(::aggregateId.isInitialized) { "Ticket does not exist" }
        require(status == TicketStatus.OPEN) { "Ticket is already closed" }

        // Apply the domain event
        apply(
            TicketClosedEvent(
                ticketId = command.ticketId,
                resolution = command.resolution,
            ),
        )
    }

    // Event Sourcing Handlers

    @EafEventSourcingHandler
    fun on(event: TicketCreatedEvent) {
        this.aggregateId = event.ticketId
        this.title = event.title
        this.description = event.description
        this.priority = event.priority
        this.assigneeId = event.assigneeId
        this.status = TicketStatus.OPEN
        this.createdAt = event.createdAt
    }

    @EafEventSourcingHandler
    fun on(event: TicketAssignedEvent) {
        this.assigneeId = event.assigneeId
        this.assignedAt = event.assignedAt
    }

    @EafEventSourcingHandler
    fun on(event: TicketClosedEvent) {
        this.status = TicketStatus.CLOSED
        this.resolution = event.resolution
        this.closedAt = event.closedAt
    }

    // Event Dispatcher

    override fun handleEvent(event: Any) {
        when (event) {
            is TicketCreatedEvent -> on(event)
            is TicketAssignedEvent -> on(event)
            is TicketClosedEvent -> on(event)
            else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
        }
    }

    // Snapshot Support

    override fun createSnapshot(): TicketSnapshot? =
        if (::aggregateId.isInitialized) {
            TicketSnapshot(
                aggregateId = aggregateId,
                title = title,
                description = description,
                priority = priority,
                assigneeId = assigneeId,
                status = status,
                resolution = resolution,
                createdAt = createdAt,
                assignedAt = assignedAt,
                closedAt = closedAt,
            )
        } else {
            null
        }

    override fun restoreFromSnapshot(snapshot: Any) {
        require(snapshot is TicketSnapshot) { "Invalid snapshot type: ${snapshot::class.simpleName}" }

        this.aggregateId = snapshot.aggregateId
        this.title = snapshot.title
        this.description = snapshot.description
        this.priority = snapshot.priority
        this.assigneeId = snapshot.assigneeId
        this.status = snapshot.status
        this.resolution = snapshot.resolution
        this.createdAt = snapshot.createdAt
        this.assignedAt = snapshot.assignedAt
        this.closedAt = snapshot.closedAt
    }

    // Getters for testing and query purposes

    fun getTitle(): String = title

    fun getDescription(): String = description

    fun getPriority(): TicketPriority = priority

    fun getAssigneeId(): String? = assigneeId

    fun getStatus(): TicketStatus = status

    fun getResolution(): String? = resolution

    fun getCreatedAt(): Instant? = createdAt

    fun getAssignedAt(): Instant? = assignedAt

    fun getClosedAt(): Instant? = closedAt

    fun isAssigned(): Boolean = assigneeId != null

    fun isClosed(): Boolean = status == TicketStatus.CLOSED
}

/**
 * Snapshot data class for the Ticket aggregate.
 */
data class TicketSnapshot(
    val aggregateId: UUID,
    val title: String,
    val description: String,
    val priority: TicketPriority,
    val assigneeId: String?,
    val status: TicketStatus,
    val resolution: String?,
    val createdAt: Instant?,
    val assignedAt: Instant?,
    val closedAt: Instant?,
)

/**
 * Status enumeration for tickets.
 */
enum class TicketStatus {
    OPEN,
    CLOSED,
}

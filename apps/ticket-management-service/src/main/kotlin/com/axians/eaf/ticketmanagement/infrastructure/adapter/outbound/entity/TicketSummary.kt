package com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * JPA entity representing the ticket summary read model.
 *
 * This entity is optimized for read operations and is populated by
 * the TicketSummaryProjector from domain events. It provides denormalized
 * data for efficient querying without reconstructing from event store.
 */
@Entity
@Table(
    name = "ticket_summary",
    indexes = [
        Index(name = "idx_ticket_summary_tenant_id", columnList = "tenant_id"),
        Index(name = "idx_ticket_summary_status", columnList = "status"),
        Index(name = "idx_ticket_summary_assignee", columnList = "assignee_id"),
        Index(name = "idx_ticket_summary_priority", columnList = "priority"),
        Index(name = "idx_ticket_summary_created_at", columnList = "created_at"),
        Index(name = "idx_ticket_summary_tenant_status", columnList = "tenant_id, status"),
    ],
)
data class TicketSummary(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID,
    @Column(name = "tenant_id", nullable = false, length = 100)
    val tenantId: String,
    @Column(name = "title", nullable = false, length = 200)
    val title: String,
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    val description: String,
    @Column(name = "priority", nullable = false, length = 50)
    val priority: String,
    @Column(name = "status", nullable = false, length = 50)
    val status: String,
    @Column(name = "assignee_id", length = 100)
    val assigneeId: String? = null,
    @Column(name = "resolution", columnDefinition = "TEXT")
    val resolution: String? = null,
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    val createdAt: Instant,
    @Column(name = "assigned_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    val assignedAt: Instant? = null,
    @Column(name = "closed_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    val closedAt: Instant? = null,
    @Column(name = "last_event_sequence_number", nullable = false)
    val lastEventSequenceNumber: Long,
    @Column(
        name = "updated_at",
        nullable = false,
        columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP",
    )
    val updatedAt: Instant = Instant.now(),
) {
    /**
     * No-argument constructor for JPA.
     * This is required by JPA but should not be used directly in application code.
     */
    @Suppress("unused")
    private constructor() : this(
        id = UUID.randomUUID(),
        tenantId = "",
        title = "",
        description = "",
        priority = "",
        status = "",
        createdAt = Instant.now(),
        lastEventSequenceNumber = 0L,
    )

    /**
     * Creates an updated copy of this ticket summary with new values.
     * This is useful for projector updates while maintaining immutability.
     */
    fun updateWith(
        title: String = this.title,
        description: String = this.description,
        priority: String = this.priority,
        status: String = this.status,
        assigneeId: String? = this.assigneeId,
        resolution: String? = this.resolution,
        assignedAt: Instant? = this.assignedAt,
        closedAt: Instant? = this.closedAt,
        lastEventSequenceNumber: Long = this.lastEventSequenceNumber,
    ): TicketSummary =
        copy(
            title = title,
            description = description,
            priority = priority,
            status = status,
            assigneeId = assigneeId,
            resolution = resolution,
            assignedAt = assignedAt,
            closedAt = closedAt,
            lastEventSequenceNumber = lastEventSequenceNumber,
            updatedAt = Instant.now(),
        )
}

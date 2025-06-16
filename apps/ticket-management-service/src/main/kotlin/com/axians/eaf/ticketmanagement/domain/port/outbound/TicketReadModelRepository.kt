package com.axians.eaf.ticketmanagement.domain.port.outbound

import java.time.Instant
import java.util.UUID

/**
 * Domain port for read model repository operations.
 * This interface defines the contract for reading ticket projections without depending on specific infrastructure.
 */
interface TicketReadModelRepository {
    /**
     * Finds a ticket read model by ID.
     */
    fun findById(ticketId: UUID): TicketReadModel?

    /**
     * Finds all tickets for a tenant ordered by creation date.
     */
    fun findByTenantIdOrderByCreatedAtDesc(tenantId: String): List<TicketReadModel>

    /**
     * Finds tickets by tenant and status.
     */
    fun findByTenantIdAndStatusOrderByCreatedAtDesc(
        tenantId: String,
        status: String,
    ): List<TicketReadModel>

    /**
     * Finds tickets by tenant and assignee.
     */
    fun findByTenantIdAndAssigneeIdOrderByCreatedAtDesc(
        tenantId: String,
        assigneeId: String,
    ): List<TicketReadModel>

    /**
     * Gets ticket statistics by tenant.
     */
    fun getTicketStatsByTenant(tenantId: String): List<Array<Any>>

    /**
     * Finds tickets with flexible filtering.
     */
    fun findByTenantIdWithFilters(
        tenantId: String,
        status: String? = null,
        assigneeId: String? = null,
        priority: String? = null,
    ): List<TicketReadModel>

    /**
     * Finds ticket by ID and tenant.
     */
    fun findByIdAndTenantId(
        ticketId: UUID,
        tenantId: String,
    ): TicketReadModel?

    /**
     * Counts tickets by tenant.
     */
    fun countByTenantId(tenantId: String): Long

    /**
     * Counts tickets by tenant and status.
     */
    fun countByTenantIdAndStatus(
        tenantId: String,
        status: String,
    ): Long
}

/**
 * Domain model for ticket read model data.
 * This represents the projected ticket data without infrastructure concerns.
 */
data class TicketReadModel(
    val id: UUID,
    val tenantId: String,
    val title: String,
    val description: String,
    val priority: String,
    val status: String,
    val assigneeId: String?,
    val resolution: String?,
    val createdAt: Instant,
    val assignedAt: Instant?,
    val closedAt: Instant?,
    val eventSequence: Long,
)

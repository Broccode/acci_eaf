package com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound

import com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound.entity.TicketSummary
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JPA repository for TicketSummary read model.
 *
 * This repository provides optimized query methods for the read side of CQRS,
 * ensuring proper tenant isolation and efficient data access patterns.
 */
@Repository
interface TicketSummaryRepository : JpaRepository<TicketSummary, UUID> {
    /**
     * Finds all tickets for a specific tenant, ordered by creation date (newest first).
     *
     * @param tenantId The tenant identifier
     * @return List of ticket summaries for the tenant
     */
    fun findByTenantIdOrderByCreatedAtDesc(tenantId: String): List<TicketSummary>

    /**
     * Finds a ticket by ID and tenant ID for tenant-isolated access.
     *
     * @param id The ticket ID
     * @param tenantId The tenant identifier
     * @return The ticket summary if found, null otherwise
     */
    fun findByIdAndTenantId(
        id: UUID,
        tenantId: String,
    ): TicketSummary?

    /**
     * Finds tickets for a specific tenant and status, ordered by creation date (newest first).
     *
     * @param tenantId The tenant identifier
     * @param status The ticket status
     * @return List of ticket summaries matching the criteria
     */
    fun findByTenantIdAndStatusOrderByCreatedAtDesc(
        tenantId: String,
        status: String,
    ): List<TicketSummary>

    /**
     * Finds tickets assigned to a specific user within a tenant, ordered by creation date (newest first).
     *
     * @param tenantId The tenant identifier
     * @param assigneeId The assignee's user ID
     * @return List of ticket summaries assigned to the user
     */
    fun findByTenantIdAndAssigneeIdOrderByCreatedAtDesc(
        tenantId: String,
        assigneeId: String,
    ): List<TicketSummary>

    /**
     * Finds tickets by priority within a tenant, ordered by creation date (newest first).
     *
     * @param tenantId The tenant identifier
     * @param priority The ticket priority
     * @return List of ticket summaries with the specified priority
     */
    fun findByTenantIdAndPriorityOrderByCreatedAtDesc(
        tenantId: String,
        priority: String,
    ): List<TicketSummary>

    /**
     * Counts tickets by status for a specific tenant.
     *
     * @param tenantId The tenant identifier
     * @param status The ticket status
     * @return Number of tickets with the specified status
     */
    fun countByTenantIdAndStatus(
        tenantId: String,
        status: String,
    ): Long

    /**
     * Counts total tickets for a specific tenant.
     *
     * @param tenantId The tenant identifier
     * @return Total number of tickets for the tenant
     */
    fun countByTenantId(tenantId: String): Long

    /**
     * Finds tickets matching multiple criteria with optional filters.
     * This method provides flexible querying for advanced search scenarios.
     *
     * @param tenantId The tenant identifier (required)
     * @param status Optional status filter
     * @param assigneeId Optional assignee filter
     * @param priority Optional priority filter
     * @return List of ticket summaries matching the criteria
     */
    @Query(
        """
        SELECT t FROM TicketSummary t
        WHERE t.tenantId = :tenantId
        AND (:status IS NULL OR t.status = :status)
        AND (:assigneeId IS NULL OR t.assigneeId = :assigneeId)
        AND (:priority IS NULL OR t.priority = :priority)
        ORDER BY t.createdAt DESC
        """,
    )
    fun findByTenantIdWithFilters(
        @Param("tenantId") tenantId: String,
        @Param("status") status: String? = null,
        @Param("assigneeId") assigneeId: String? = null,
        @Param("priority") priority: String? = null,
    ): List<TicketSummary>

    /**
     * Gets ticket statistics for a tenant (counts by status).
     *
     * @param tenantId The tenant identifier
     * @return Map of status to count
     */
    @Query(
        """
        SELECT t.status, COUNT(t) FROM TicketSummary t
        WHERE t.tenantId = :tenantId
        GROUP BY t.status
        """,
    )
    fun getTicketStatsByTenant(
        @Param("tenantId") tenantId: String,
    ): List<Array<Any>>

    /**
     * Finds the most recently updated tickets for a tenant.
     * Useful for dashboard views showing recent activity.
     *
     * @param tenantId The tenant identifier
     * @param limit Maximum number of tickets to return
     * @return List of recently updated ticket summaries
     */
    @Query(
        """
        SELECT t FROM TicketSummary t
        WHERE t.tenantId = :tenantId
        ORDER BY t.updatedAt DESC
        LIMIT :limit
        """,
    )
    fun findRecentlyUpdatedByTenant(
        @Param("tenantId") tenantId: String,
        @Param("limit") limit: Int = 10,
    ): List<TicketSummary>
}

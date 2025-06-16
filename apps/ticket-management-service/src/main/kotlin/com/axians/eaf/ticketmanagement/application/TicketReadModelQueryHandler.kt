package com.axians.eaf.ticketmanagement.application

import com.axians.eaf.ticketmanagement.application.dto.TicketQueryResponse
import com.axians.eaf.ticketmanagement.application.query.GetTicketByIdQuery
import com.axians.eaf.ticketmanagement.application.query.ListTicketsQuery
import com.axians.eaf.ticketmanagement.domain.command.TicketPriority
import com.axians.eaf.ticketmanagement.domain.port.outbound.TicketReadModel
import com.axians.eaf.ticketmanagement.domain.port.outbound.TicketReadModelRepository
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Query handler that uses the TicketSummary read model for optimized queries.
 *
 * This handler provides the query-side implementation of CQRS by reading from
 * denormalized TicketSummary projections rather than reconstructing state from
 * event sourcing. This provides:
 * - Much faster query performance
 * - Simpler query logic
 * - Optimized database indexes
 * - Reduced complexity for read operations
 *
 * The read model is maintained by the TicketSummaryProjector which processes
 * domain events and keeps the projections up-to-date.
 */
@Service
class TicketReadModelQueryHandler(
    private val ticketReadModelRepository: TicketReadModelRepository,
) {
    /**
     * Handles queries to get a single ticket by ID from the read model.
     *
     * @param query The get ticket by ID query
     * @return The ticket query response
     * @throws IllegalArgumentException if the ticket is not found
     */
    suspend fun handle(query: GetTicketByIdQuery): TicketQueryResponse {
        val ticketReadModel =
            ticketReadModelRepository.findById(query.ticketId)
                ?: throw IllegalArgumentException("Ticket not found: ${query.ticketId}")

        return mapToQueryResponse(ticketReadModel)
    }

    /**
     * Handles queries to list all tickets for a tenant from the read model.
     *
     * Note: In a full implementation, tenant context would be automatically injected.
     * For this pilot, we're using a hardcoded tenant ID for demonstration.
     *
     * @param query The list tickets query
     * @return List of ticket query responses
     */
    suspend fun handle(query: ListTicketsQuery): List<TicketQueryResponse> {
        // TODO: Replace with actual tenant context injection from security context
        val tenantId = getCurrentTenantId() ?: "default-tenant"

        val ticketReadModels = ticketReadModelRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
        return ticketReadModels.map { mapToQueryResponse(it) }
    }

    /**
     * Maps a TicketReadModel to a TicketQueryResponse DTO.
     *
     * @param ticketReadModel The ticket read model from the domain
     * @return The ticket query response DTO
     */
    private fun mapToQueryResponse(ticketReadModel: TicketReadModel): TicketQueryResponse =
        TicketQueryResponse(
            ticketId = ticketReadModel.id,
            title = ticketReadModel.title,
            description = ticketReadModel.description,
            priority = TicketPriority.valueOf(ticketReadModel.priority),
            status = ticketReadModel.status,
            assigneeId = ticketReadModel.assigneeId,
            resolution = ticketReadModel.resolution,
            createdAt = ticketReadModel.createdAt,
            assignedAt = ticketReadModel.assignedAt,
            closedAt = ticketReadModel.closedAt,
        )

    /**
     * Retrieves the current tenant ID from the security context.
     *
     * TODO: This should be implemented to extract tenant ID from Spring Security context
     * or IAM integration. For the pilot, we return a default value.
     *
     * @return The current tenant ID or null if not available
     */
    private fun getCurrentTenantId(): String? {
        // TODO: Implement tenant context extraction from security context
        // This would typically look like:
        // return SecurityContextHolder.getContext()
        //     .authentication
        //     ?.principal
        //     ?.let { it as? EafUserPrincipal }
        //     ?.tenantId

        return "default-tenant" // Placeholder for pilot
    }
}

/**
 * Additional query methods for advanced read model queries.
 * These methods demonstrate the power of having optimized read models.
 */
@Service
class TicketReadModelAdvancedQueryHandler(
    private val ticketReadModelRepository: TicketReadModelRepository,
) {
    /**
     * Gets tickets by status for the current tenant.
     */
    suspend fun getTicketsByStatus(status: String): List<TicketQueryResponse> {
        val tenantId = getCurrentTenantId() ?: "default-tenant"
        val ticketReadModels = ticketReadModelRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status)
        return ticketReadModels.map { mapToQueryResponse(it) }
    }

    /**
     * Gets tickets assigned to a specific user.
     */
    suspend fun getTicketsByAssignee(assigneeId: String): List<TicketQueryResponse> {
        val tenantId = getCurrentTenantId() ?: "default-tenant"
        val ticketReadModels =
            ticketReadModelRepository.findByTenantIdAndAssigneeIdOrderByCreatedAtDesc(
                tenantId,
                assigneeId,
            )
        return ticketReadModels.map { mapToQueryResponse(it) }
    }

    /**
     * Gets ticket statistics for the current tenant.
     */
    suspend fun getTicketStatistics(): Map<String, Long> {
        val tenantId = getCurrentTenantId() ?: "default-tenant"
        val stats = ticketReadModelRepository.getTicketStatsByTenant(tenantId)
        return stats.associate { it[0] as String to it[1] as Long }
    }

    /**
     * Gets tickets with flexible filtering.
     */
    suspend fun getTicketsWithFilters(
        status: String? = null,
        assigneeId: String? = null,
        priority: String? = null,
    ): List<TicketQueryResponse> {
        val tenantId = getCurrentTenantId() ?: "default-tenant"
        val ticketReadModels =
            ticketReadModelRepository.findByTenantIdWithFilters(
                tenantId = tenantId,
                status = status,
                assigneeId = assigneeId,
                priority = priority,
            )
        return ticketReadModels.map { mapToQueryResponse(it) }
    }

    private fun mapToQueryResponse(ticketReadModel: TicketReadModel): TicketQueryResponse =
        TicketQueryResponse(
            ticketId = ticketReadModel.id,
            title = ticketReadModel.title,
            description = ticketReadModel.description,
            priority = TicketPriority.valueOf(ticketReadModel.priority),
            status = ticketReadModel.status,
            assigneeId = ticketReadModel.assigneeId,
            resolution = ticketReadModel.resolution,
            createdAt = ticketReadModel.createdAt,
            assignedAt = ticketReadModel.assignedAt,
            closedAt = ticketReadModel.closedAt,
        )

    private fun getCurrentTenantId(): String? = "default-tenant" // Placeholder for pilot

    /**
     * Get all tickets for a tenant (for Hilla endpoint)
     */
    fun getAllTickets(tenantId: String): List<TicketSummaryResponse> =
        ticketReadModelRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).map { ticket ->
            TicketSummaryResponse(
                id = ticket.id.toString(),
                title = ticket.title,
                status = ticket.status,
                priority = ticket.priority,
                assigneeId = ticket.assigneeId,
                createdAt = ticket.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
                closedAt = ticket.closedAt?.atZone(java.time.ZoneId.systemDefault())?.toLocalDateTime(),
            )
        }

    /**
     * Get ticket by ID (for Hilla endpoint)
     */
    fun getTicketById(
        tenantId: String,
        ticketId: String,
    ): TicketDetailsResponse? =
        ticketReadModelRepository.findByIdAndTenantId(UUID.fromString(ticketId), tenantId)?.let { ticket ->
            TicketDetailsResponse(
                id = ticket.id.toString(),
                title = ticket.title,
                description = ticket.description,
                status = ticket.status,
                priority = ticket.priority,
                assigneeId = ticket.assigneeId,
                createdAt = ticket.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
                closedAt = ticket.closedAt?.atZone(java.time.ZoneId.systemDefault())?.toLocalDateTime(),
            )
        }

    /**
     * Get ticket statistics (for Hilla endpoint)
     */
    fun getTicketStatistics(tenantId: String): TicketStatistics {
        val totalCount = ticketReadModelRepository.countByTenantId(tenantId)
        val openCount = ticketReadModelRepository.countByTenantIdAndStatus(tenantId, "OPEN")
        val assignedCount = ticketReadModelRepository.countByTenantIdAndStatus(tenantId, "ASSIGNED")
        val closedCount = ticketReadModelRepository.countByTenantIdAndStatus(tenantId, "CLOSED")

        return TicketStatistics(
            total = totalCount.toInt(),
            open = openCount.toInt(),
            assigned = assignedCount.toInt(),
            closed = closedCount.toInt(),
        )
    }
}

/**
 * DTOs for Hilla endpoints
 */
data class TicketSummaryResponse(
    val id: String,
    val title: String,
    val status: String,
    val priority: String,
    val assigneeId: String?,
    val createdAt: java.time.LocalDateTime,
    val closedAt: java.time.LocalDateTime?,
)

data class TicketDetailsResponse(
    val id: String,
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val assigneeId: String?,
    val createdAt: java.time.LocalDateTime,
    val closedAt: java.time.LocalDateTime?,
)

data class TicketStatistics(
    val total: Int,
    val open: Int,
    val assigned: Int,
    val closed: Int,
)

package com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound

import com.axians.eaf.ticketmanagement.domain.port.outbound.TicketReadModel
import com.axians.eaf.ticketmanagement.domain.port.outbound.TicketReadModelRepository
import com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound.entity.TicketSummary
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Infrastructure adapter that implements the TicketReadModelRepository port using JPA.
 * This adapter bridges the domain port with the JPA repository infrastructure.
 */
@Component
class TicketReadModelRepositoryAdapter(
    private val ticketSummaryRepository: TicketSummaryRepository,
) : TicketReadModelRepository {
    override fun findById(ticketId: UUID): TicketReadModel? =
        ticketSummaryRepository
            .findById(ticketId)
            .map { mapToReadModel(it) }
            .orElse(null)

    override fun findByTenantIdOrderByCreatedAtDesc(tenantId: String): List<TicketReadModel> =
        ticketSummaryRepository
            .findByTenantIdOrderByCreatedAtDesc(tenantId)
            .map { mapToReadModel(it) }

    override fun findByTenantIdAndStatusOrderByCreatedAtDesc(
        tenantId: String,
        status: String,
    ): List<TicketReadModel> =
        ticketSummaryRepository
            .findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status)
            .map { mapToReadModel(it) }

    override fun findByTenantIdAndAssigneeIdOrderByCreatedAtDesc(
        tenantId: String,
        assigneeId: String,
    ): List<TicketReadModel> =
        ticketSummaryRepository
            .findByTenantIdAndAssigneeIdOrderByCreatedAtDesc(tenantId, assigneeId)
            .map { mapToReadModel(it) }

    override fun getTicketStatsByTenant(tenantId: String): List<Array<Any>> =
        ticketSummaryRepository.getTicketStatsByTenant(tenantId)

    override fun findByTenantIdWithFilters(
        tenantId: String,
        status: String?,
        assigneeId: String?,
        priority: String?,
    ): List<TicketReadModel> =
        ticketSummaryRepository
            .findByTenantIdWithFilters(tenantId, status, assigneeId, priority)
            .map { mapToReadModel(it) }

    override fun findByIdAndTenantId(
        ticketId: UUID,
        tenantId: String,
    ): TicketReadModel? =
        ticketSummaryRepository
            .findByIdAndTenantId(ticketId, tenantId)
            ?.let { mapToReadModel(it) }

    override fun countByTenantId(tenantId: String): Long = ticketSummaryRepository.countByTenantId(tenantId)

    override fun countByTenantIdAndStatus(
        tenantId: String,
        status: String,
    ): Long = ticketSummaryRepository.countByTenantIdAndStatus(tenantId, status)

    /**
     * Maps JPA entity to domain read model.
     */
    private fun mapToReadModel(entity: TicketSummary): TicketReadModel =
        TicketReadModel(
            id = entity.id,
            tenantId = entity.tenantId,
            title = entity.title,
            description = entity.description,
            priority = entity.priority,
            status = entity.status,
            assigneeId = entity.assigneeId,
            resolution = entity.resolution,
            createdAt = entity.createdAt,
            assignedAt = entity.assignedAt,
            closedAt = entity.closedAt,
            eventSequence = entity.lastEventSequenceNumber,
        )
}

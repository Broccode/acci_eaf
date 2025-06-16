package com.axians.eaf.ticketmanagement.infrastructure.adapter.input.web

import com.axians.eaf.ticketmanagement.application.TicketApplicationService
import com.axians.eaf.ticketmanagement.application.TicketDetailsResponse
import com.axians.eaf.ticketmanagement.application.TicketReadModelAdvancedQueryHandler
import com.axians.eaf.ticketmanagement.application.TicketStatistics
import com.axians.eaf.ticketmanagement.application.TicketSummaryResponse
import com.axians.eaf.ticketmanagement.application.request.AssignTicketRequest
import com.axians.eaf.ticketmanagement.application.request.CloseTicketRequest
import com.axians.eaf.ticketmanagement.application.request.CreateTicketRequest
import com.axians.eaf.ticketmanagement.domain.command.TicketPriority
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * REST endpoints for ticket management frontend.
 * Provides API for the frontend to interact with ticket management system.
 */
@RestController
@RequestMapping("/api/tickets")
@PreAuthorize("isAuthenticated()")
class TicketEndpoint(
    private val ticketApplicationService: TicketApplicationService,
    private val ticketReadModelQueryHandler: TicketReadModelAdvancedQueryHandler,
) {
    /**
     * Get all tickets for the current tenant
     */
    @GetMapping
    fun getAllTickets(): List<TicketSummaryResponse> {
        // For pilot: return empty list (will be populated after full integration)
        return emptyList()
    }

    /**
     * Get ticket statistics for dashboard
     */
    @GetMapping("/statistics")
    fun getTicketStatistics(): TicketStatistics {
        // For pilot: return zero statistics (will be populated after full integration)
        return TicketStatistics(total = 0, open = 0, assigned = 0, closed = 0)
    }

    /**
     * Get ticket by ID
     */
    @GetMapping("/{ticketId}")
    fun getTicketById(
        @PathVariable ticketId: String,
    ): TicketDetailsResponse? {
        // For pilot: return null (will be implemented after full integration)
        return null
    }

    /**
     * Create a new ticket
     */
    @PostMapping
    suspend fun createTicket(
        @RequestBody request: CreateTicketRequestDto,
    ): CreateTicketResponseDto {
        val userId = getCurrentUserId()

        val createRequest =
            CreateTicketRequest(
                title = request.title,
                description = request.description,
                priority = TicketPriority.valueOf(request.priority),
                assigneeId = userId, // For now, using current user as reporter
            )

        val response = ticketApplicationService.createTicket(createRequest)
        return CreateTicketResponseDto(response.ticketId.toString())
    }

    /**
     * Assign a ticket to a user
     */
    @PostMapping("/{ticketId}/assign")
    suspend fun assignTicket(
        @PathVariable ticketId: String,
        @RequestBody request: AssignTicketRequestDto,
    ) {
        val assignRequest =
            AssignTicketRequest(
                assigneeId = request.assigneeId,
            )

        ticketApplicationService.assignTicket(UUID.fromString(ticketId), assignRequest)
    }

    /**
     * Close a ticket
     */
    @PostMapping("/{ticketId}/close")
    suspend fun closeTicket(
        @PathVariable ticketId: String,
        @RequestBody request: CloseTicketRequestDto? = null,
    ) {
        val closeRequest =
            CloseTicketRequest(
                resolution = request?.closureReason ?: "Closed",
            )

        ticketApplicationService.closeTicket(UUID.fromString(ticketId), closeRequest)
    }

    private fun getCurrentTenantId(): String {
        // TODO: Extract from security context or user claims
        // For pilot, using default tenant
        return "default-tenant"
    }

    private fun getCurrentUserId(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.name ?: "anonymous"
    }
}

/**
 * Frontend DTOs for ticket management
 */
data class CreateTicketRequestDto(
    val title: String,
    val description: String,
    val priority: String,
)

data class CreateTicketResponseDto(
    val ticketId: String,
)

data class AssignTicketRequestDto(
    val assigneeId: String,
)

data class CloseTicketRequestDto(
    val closureReason: String?,
)

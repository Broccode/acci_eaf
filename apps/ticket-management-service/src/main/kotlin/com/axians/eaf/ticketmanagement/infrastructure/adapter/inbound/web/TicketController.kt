package com.axians.eaf.ticketmanagement.infrastructure.adapter.inbound.web

import com.axians.eaf.ticketmanagement.application.TicketApplicationService
import com.axians.eaf.ticketmanagement.application.dto.AssignTicketResponse
import com.axians.eaf.ticketmanagement.application.dto.CloseTicketResponse
import com.axians.eaf.ticketmanagement.application.dto.CreateTicketResponse
import com.axians.eaf.ticketmanagement.application.dto.TicketQueryResponse
import com.axians.eaf.ticketmanagement.application.request.AssignTicketRequest
import com.axians.eaf.ticketmanagement.application.request.CloseTicketRequest
import com.axians.eaf.ticketmanagement.application.request.CreateTicketRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * REST controller for ticket management operations.
 *
 * This controller provides HTTP endpoints for all ticket-related operations
 * following RESTful principles. It integrates with Spring Security for
 * authentication and authorization.
 */
@RestController
@RequestMapping("/api/tickets")
class TicketController(
    private val ticketApplicationService: TicketApplicationService,
) {
    /**
     * Creates a new ticket.
     *
     * @param request The ticket creation request
     * @return The created ticket response
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    suspend fun createTicket(
        @Valid @RequestBody request: CreateTicketRequest,
    ): ResponseEntity<CreateTicketResponse> {
        val response = ticketApplicationService.createTicket(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * Retrieves a ticket by its ID.
     *
     * @param ticketId The ticket ID
     * @return The ticket details
     */
    @GetMapping("/{ticketId}")
    @PreAuthorize("hasRole('USER')")
    suspend fun getTicketById(
        @PathVariable ticketId: UUID,
    ): ResponseEntity<TicketQueryResponse> {
        val response = ticketApplicationService.getTicketById(ticketId)
        return ResponseEntity.ok(response)
    }

    /**
     * Retrieves all tickets.
     *
     * @return List of all tickets
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    suspend fun listTickets(): ResponseEntity<List<TicketQueryResponse>> {
        val response = ticketApplicationService.listTickets()
        return ResponseEntity.ok(response)
    }

    /**
     * Assigns a ticket to a user.
     *
     * @param ticketId The ticket ID
     * @param request The assignment request
     * @return The assignment response
     */
    @PatchMapping("/{ticketId}/assign")
    @PreAuthorize("hasRole('USER')")
    suspend fun assignTicket(
        @PathVariable ticketId: UUID,
        @Valid @RequestBody request: AssignTicketRequest,
    ): ResponseEntity<AssignTicketResponse> {
        val response = ticketApplicationService.assignTicket(ticketId, request)
        return ResponseEntity.ok(response)
    }

    /**
     * Closes a ticket with a resolution.
     *
     * @param ticketId The ticket ID
     * @param request The closure request
     * @return The closure response
     */
    @PatchMapping("/{ticketId}/close")
    @PreAuthorize("hasRole('USER')")
    suspend fun closeTicket(
        @PathVariable ticketId: UUID,
        @Valid @RequestBody request: CloseTicketRequest,
    ): ResponseEntity<CloseTicketResponse> {
        val response = ticketApplicationService.closeTicket(ticketId, request)
        return ResponseEntity.ok(response)
    }

    /**
     * Exception handler for IllegalArgumentException (e.g., ticket not found).
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        val errorResponse =
            ErrorResponse(
                message = ex.message ?: "Invalid request",
                code = "INVALID_REQUEST",
            )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    /**
     * Exception handler for validation errors.
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: org.springframework.web.bind.MethodArgumentNotValidException,
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        val errorResponse =
            ErrorResponse(
                message = "Validation failed: ${errors.joinToString(", ")}",
                code = "VALIDATION_ERROR",
            )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    /**
     * Exception handler for JSON deserialization errors (e.g., constructor validation).
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        ex: org.springframework.http.converter.HttpMessageNotReadableException,
    ): ResponseEntity<ErrorResponse> {
        // Extract the root cause message if it's an IllegalArgumentException from our DTOs
        val message = ex.cause?.message ?: ex.message ?: "Invalid request format"
        val errorResponse =
            ErrorResponse(
                message = message,
                code = "INVALID_REQUEST",
            )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }
}

/**
 * Standard error response structure.
 */
data class ErrorResponse(
    val message: String,
    val code: String,
)

package com.axians.eaf.ticketmanagement.infrastructure.adapter.inbound.hilla

import com.axians.eaf.eventing.consumer.ProjectorRegistry
import com.axians.eaf.ticketmanagement.application.TicketApplicationService
import com.axians.eaf.ticketmanagement.application.dto.AssignTicketResponse
import com.axians.eaf.ticketmanagement.application.dto.CloseTicketResponse
import com.axians.eaf.ticketmanagement.application.dto.CreateTicketResponse
import com.axians.eaf.ticketmanagement.application.dto.TicketQueryResponse
import com.axians.eaf.ticketmanagement.application.request.AssignTicketRequest
import com.axians.eaf.ticketmanagement.application.request.CloseTicketRequest
import com.axians.eaf.ticketmanagement.application.request.CreateTicketRequest
import com.axians.eaf.ticketmanagement.domain.command.TicketPriority
import com.vaadin.flow.server.auth.AnonymousAllowed
import com.vaadin.hilla.Endpoint
import jakarta.annotation.security.RolesAllowed
import kotlinx.coroutines.runBlocking
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Hilla endpoint for ticket management operations.
 * Provides a bridge between the React frontend and the backend services.
 */
@Endpoint
@RestController
class TicketEndpoint(
    private val ticketApplicationService: TicketApplicationService,
) {
    /**
     * Creates a new ticket.
     */
    @RolesAllowed("USER")
    fun createTicket(request: CreateTicketRequestDto): CreateTicketResponse =
        runBlocking {
            val createRequest =
                CreateTicketRequest(
                    title = request.title,
                    description = request.description,
                    priority = TicketPriority.valueOf(request.priority),
                    assigneeId = request.assigneeId,
                )
            ticketApplicationService.createTicket(createRequest)
        }

    /**
     * Retrieves all tickets.
     */
    @RolesAllowed("USER")
    fun listTickets(): List<TicketQueryResponse> =
        runBlocking {
            ticketApplicationService.listTickets()
        }

    /**
     * Retrieves a ticket by its ID.
     */
    @RolesAllowed("USER")
    fun getTicketById(request: GetTicketByIdRequestDto): TicketQueryResponse =
        runBlocking {
            val uuid = UUID.fromString(request.ticketId)
            ticketApplicationService.getTicketById(uuid)
        }

    /**
     * Assigns a ticket to a user.
     */
    @RolesAllowed("USER")
    fun assignTicket(request: AssignTicketRequestDto): AssignTicketResponse =
        runBlocking {
            val uuid = UUID.fromString(request.ticketId)
            val assignRequest = AssignTicketRequest(request.assigneeId)
            ticketApplicationService.assignTicket(uuid, assignRequest)
        }

    /**
     * Closes a ticket with a resolution.
     */
    @RolesAllowed("USER")
    fun closeTicket(request: CloseTicketRequestDto): CloseTicketResponse =
        runBlocking {
            val uuid = UUID.fromString(request.ticketId)
            val closeRequest = CloseTicketRequest(request.resolution)
            ticketApplicationService.closeTicket(uuid, closeRequest)
        }

    /**
     * Simple health check method for authentication verification.
     */
    @RolesAllowed("USER")
    fun healthCheck(): String = "OK"

    /**
     * Test endpoint to check authentication status - allows anonymous access for debugging.
     */
    @AnonymousAllowed
    fun testAuth(): String =
        try {
            // Try to get current authentication
            val authentication = SecurityContextHolder.getContext().authentication
            val principal = authentication?.principal
            val authorities = authentication?.authorities?.map { it.authority } ?: emptyList()

            "Auth Status: ${authentication != null}, " +
                "Principal: ${principal?.javaClass?.simpleName}, " +
                "Authorities: $authorities, " +
                "Name: ${authentication?.name}"
        } catch (e: Exception) {
            "Error getting auth: ${e.message}"
        }

    /**
     * Test endpoint to check projector registry status.
     */
    @AnonymousAllowed
    fun testProjectors(): String =
        try {
            val projectors =
                com.axians.eaf.eventing.consumer.ProjectorRegistry
                    .getAllProjectors()
            "Found ${projectors.size} registered projectors: ${projectors.map {
                "${it.projectorName} -> ${it.subject}"
            }}"
        } catch (e: Exception) {
            "Error checking projectors: ${e.message}"
        }

    /**
     * Simple test to check if EAF SDK beans are available.
     */
    @AnonymousAllowed
    fun testEafSdk(): String =
        try {
            // Try to access the EAF SDK classes directly
            val registryClass = com.axians.eaf.eventing.consumer.ProjectorRegistry::class.java
            val processorClass = com.axians.eaf.eventing.consumer.EafProjectorEventHandlerProcessor::class.java
            "EAF SDK classes available: ProjectorRegistry=${registryClass.simpleName}, Processor=${processorClass.simpleName}"
        } catch (e: Exception) {
            "Error accessing EAF SDK: ${e.message}"
        }

    /**
     * Test method to check if suspend functions work - non-suspend version.
     */
    @AnonymousAllowed
    fun testCreateTicketNonSuspend(): String = "Non-suspend createTicket test - OK"

    /**
     * Test method to check if suspend functions work - suspend version.
     */
    @AnonymousAllowed
    suspend fun testCreateTicketSuspend(): String = "Suspend createTicket test - OK"

    /**
     * Test method to create a ticket without authentication for debugging.
     */
    @AnonymousAllowed
    fun testCreateTicketDebug(): String =
        runBlocking {
            try {
                val createRequest =
                    CreateTicketRequest(
                        title = "Debug Test Ticket",
                        description = "This is a test ticket created for debugging",
                        priority = TicketPriority.HIGH,
                        assigneeId = null,
                    )
                val response = ticketApplicationService.createTicket(createRequest)
                "Successfully created test ticket: ${response.ticketId}"
            } catch (e: Exception) {
                "Error creating test ticket: ${e.message}"
            }
        }

    /**
     * Simple HTTP GET endpoint to check projector status - for debugging.
     */
    @GetMapping("/debug/projectors")
    @AnonymousAllowed
    fun debugProjectors(): String =
        try {
            val projectors = ProjectorRegistry.getAllProjectors()
            val projectorInfo = projectors.map { "${it.projectorName} -> ${it.subject}" }
            "Registered projectors (${projectors.size}): $projectorInfo"
        } catch (e: Exception) {
            "Error checking projectors: ${e.message}"
        }
}

/**
 * DTO for ticket creation requests from the frontend.
 * This mirrors CreateTicketRequest but uses String for priority to simplify frontend usage.
 */
data class CreateTicketRequestDto(
    val title: String,
    val description: String,
    val priority: String,
    val assigneeId: String? = null,
)

/**
 * DTO for getting a ticket by ID from the frontend.
 */
data class GetTicketByIdRequestDto(
    val ticketId: String,
)

/**
 * DTO for assigning a ticket to a user.
 */
data class AssignTicketRequestDto(
    val ticketId: String,
    val assigneeId: String,
)

/**
 * DTO for closing a ticket with a resolution.
 */
data class CloseTicketRequestDto(
    val ticketId: String,
    val resolution: String,
)

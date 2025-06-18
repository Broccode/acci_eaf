package com.axians.eaf.ticketmanagement.infrastructure.adapter.inbound.hilla

import com.vaadin.hilla.Endpoint
import org.springframework.security.access.annotation.Secured

/**
 * Hilla endpoint for ticket operations.
 * This provides browser-callable methods that can be accessed from the frontend.
 */
@Endpoint
@Secured("ROLE_USER")
class TicketEndpoint {
    /**
     * Simple greeting method to test the endpoint.
     */
    fun hello(name: String): String = "Hello, $name! Welcome to the Ticket Management System."

    /**
     * Simple health check method for authentication verification.
     * This method requires authentication.
     */
    fun healthCheck(): String = "OK"

    /**
     * Get basic ticket information.
     */
    fun getTicketInfo(ticketId: String): TicketInfo =
        TicketInfo(
            id = ticketId,
            title = "Sample Ticket",
            status = "OPEN",
            createdBy = "system",
        )
}

/**
 * Data class for ticket information.
 */
data class TicketInfo(
    val id: String,
    val title: String,
    val status: String,
    val createdBy: String,
)

package com.axians.eaf.ticketmanagement.application.query

import java.util.UUID

/**
 * Query to retrieve a ticket by its ID.
 */
data class GetTicketByIdQuery(
    val ticketId: UUID,
)

/**
 * Query to retrieve all tickets.
 * This can be extended later with filtering and pagination parameters.
 */
data class ListTicketsQuery(
    val limit: Int = 100,
    val offset: Int = 0,
)

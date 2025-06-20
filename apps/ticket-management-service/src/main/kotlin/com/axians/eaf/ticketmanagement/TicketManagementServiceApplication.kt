@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package com.axians.eaf.ticketmanagement

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Main application class for the Ticket Management Service.
 *
 * This service serves as a pilot implementation to validate the ACCI EAF MVP capabilities,
 * demonstrating proper usage of:
 * - Event Sourcing with the eaf-eventsourcing-sdk
 * - Event-driven architecture with the eaf-eventing-sdk
 * - IAM integration with the eaf-iam-client
 * - Hexagonal Architecture principles
 * - Domain-Driven Design patterns
 */
@SpringBootApplication(
    scanBasePackages = [
        "com.axians.eaf.ticketmanagement.application",
        "com.axians.eaf.ticketmanagement.domain",
        "com.axians.eaf.ticketmanagement.infrastructure",
        "com.axians.eaf.ticketmanagement.web",
        // EAF SDK eventing components
        "com.axians.eaf.eventing.consumer",
    ],
)
class TicketManagementServiceApplication

fun main(args: Array<String>) {
    runApplication<TicketManagementServiceApplication>(*args)
}

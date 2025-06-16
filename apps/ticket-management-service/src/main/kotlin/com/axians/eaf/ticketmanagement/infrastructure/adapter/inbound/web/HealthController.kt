package com.axians.eaf.ticketmanagement.infrastructure.adapter.inbound.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Health check controller for the Ticket Management Service.
 * Provides basic endpoints to verify service is running and operational.
 */
@RestController
@RequestMapping("/api")
class HealthController {
    /**
     * Health check endpoint.
     * @return Health status information
     */
    @GetMapping("/health")
    fun health(): Map<String, String> =
        mapOf(
            "status" to "UP",
            "service" to "ticket-management-service",
        )

    /**
     * Service information endpoint.
     * @return Service metadata
     */
    @GetMapping("/info")
    fun info(): Map<String, String> =
        mapOf(
            "name" to "Ticket Management Service",
            "version" to "1.0.0",
            "description" to "EAF Pilot Service for validating framework capabilities",
        )
}

package com.axians.eaf.ticketmanagement.domain.port.outbound

/**
 * Domain port for publishing events.
 * This interface defines the contract for event publishing without depending on specific infrastructure.
 */
interface EventPublisher {
    /**
     * Publishes a domain event for the specified tenant.
     *
     * @param tenantId The tenant identifier
     * @param event The domain event to publish
     */
    suspend fun publishEvent(
        tenantId: String,
        event: Any,
    )
}

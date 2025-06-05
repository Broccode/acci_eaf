package com.axians.eaf.eventing

import io.nats.client.api.PublishAck

/**
 * Service interface for publishing domain events to NATS JetStream.
 *
 * This interface provides at-least-once delivery guarantees for event publishing
 * and handles tenant-specific routing and serialization.
 */
interface NatsEventPublisher {
    /**
     * Publishes a domain event to NATS JetStream with at-least-once delivery guarantee.
     *
     * @param subject The NATS subject pattern (without tenant prefix)
     * @param tenantId The tenant identifier for multi-tenancy support
     * @param event The event payload object to be serialized and published
     * @return PublishAck from JetStream confirming publication
     * @throws IllegalArgumentException if tenantId is null or empty
     * @throws EventPublishingException if publishing fails after retries
     */
    suspend fun publish(
        subject: String,
        tenantId: String,
        event: Any,
    ): PublishAck

    /**
     * Publishes a domain event with additional metadata.
     *
     * @param subject The NATS subject pattern (without tenant prefix)
     * @param tenantId The tenant identifier for multi-tenancy support
     * @param event The event payload object to be serialized and published
     * @param metadata Additional metadata to include in the event envelope
     * @return PublishAck from JetStream confirming publication
     * @throws IllegalArgumentException if tenantId is null or empty
     * @throws EventPublishingException if publishing fails after retries
     */
    suspend fun publish(
        subject: String,
        tenantId: String,
        event: Any,
        metadata: Map<String, Any>,
    ): PublishAck
}

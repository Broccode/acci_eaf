package com.axians.eaf.eventsourcing.model

import java.time.Instant
import java.util.UUID

/**
 * Represents a persisted domain event in the event store.
 *
 * This data class maps to the domain_events table and contains all the metadata
 * necessary for event sourcing, including tenant isolation and optimistic concurrency control.
 */
data class PersistedEvent(
    val globalSequenceId: Long? = null,
    val eventId: UUID,
    val streamId: String,
    val aggregateId: String,
    val aggregateType: String,
    val expectedVersion: Long?,
    val sequenceNumber: Long,
    val tenantId: String,
    val eventType: String,
    val payload: String, // JSON string representation
    val metadata: String? = null, // JSON string representation
    val timestampUtc: Instant = Instant.now(),
) {
    companion object {
        /**
         * Creates a stream ID from aggregate type and aggregate ID.
         * Format: "aggregateType-aggregateId"
         */
        fun createStreamId(
            aggregateType: String,
            aggregateId: String,
        ): String = "$aggregateType-$aggregateId"
    }

    /**
     * Returns true if this event represents the first event in an aggregate stream.
     */
    fun isFirstEvent(): Boolean = sequenceNumber == 1L

    /**
     * Returns the next expected sequence number for this aggregate.
     */
    fun nextSequenceNumber(): Long = sequenceNumber + 1
}

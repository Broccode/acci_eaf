package com.axians.eaf.eventsourcing.model

import java.time.Instant

/**
 * Represents a snapshot of an aggregate's state at a specific point in time.
 *
 * This data class maps to the aggregate_snapshots table and is used for performance
 * optimization to avoid replaying large numbers of events.
 */
data class AggregateSnapshot(
    val id: Long? = null,
    val aggregateId: String,
    val tenantId: String,
    val aggregateType: String,
    val lastSequenceNumber: Long,
    val snapshotPayloadJsonb: String, // JSON string representation of aggregate state
    val version: Int = 1,
    val timestampUtc: Instant = Instant.now(),
) {
    /**
     * Returns true if this snapshot is up-to-date with the given sequence number.
     */
    fun isUpToDateWith(sequenceNumber: Long): Boolean = lastSequenceNumber >= sequenceNumber

    /**
     * Returns true if this snapshot can serve as a starting point for the given sequence number.
     */
    fun canServeAsStartingPointFor(sequenceNumber: Long): Boolean = lastSequenceNumber < sequenceNumber
}

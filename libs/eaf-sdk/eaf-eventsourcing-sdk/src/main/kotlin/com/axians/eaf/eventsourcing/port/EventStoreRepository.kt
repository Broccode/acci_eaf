package com.axians.eaf.eventsourcing.port

import com.axians.eaf.eventsourcing.model.AggregateSnapshot
import com.axians.eaf.eventsourcing.model.PersistedEvent

/**
 * Port interface for event store repository operations.
 *
 * This interface defines the contract for persisting and retrieving events and snapshots
 * from the event store with strict tenant isolation.
 */
interface EventStoreRepository {
    /**
     * Atomically appends one or more events to the event store for a specific aggregate.
     *
     * This operation includes optimistic concurrency control by checking the expected version
     * against the current sequence number for the aggregate in the specified tenant.
     *
     * @param events The list of events to append, all must belong to the same aggregate and tenant
     * @param tenantId The tenant identifier for isolation
     * @param aggregateId The aggregate identifier
     * @param expectedVersion The expected current version (last sequence number) of the aggregate
     *
     * @throws OptimisticLockingFailureException if the expected version doesn't match the current version
     * @throws IllegalArgumentException if events are empty or belong to different aggregates/tenants
     */
    suspend fun appendEvents(
        events: List<PersistedEvent>,
        tenantId: String,
        aggregateId: String,
        expectedVersion: Long?,
    )

    /**
     * Retrieves all events for a specific aggregate in the correct order (by sequence number).
     *
     * @param tenantId The tenant identifier for isolation
     * @param aggregateId The aggregate identifier
     * @param fromSequenceNumber Optional starting sequence number (inclusive), defaults to 1
     *
     * @return List of events ordered by sequence number
     */
    suspend fun getEvents(
        tenantId: String,
        aggregateId: String,
        fromSequenceNumber: Long = 1,
    ): List<PersistedEvent>

    /**
     * Retrieves events for a specific aggregate starting from a given sequence number.
     *
     * This is useful when loading from a snapshot and only replaying events after the snapshot.
     *
     * @param tenantId The tenant identifier for isolation
     * @param aggregateId The aggregate identifier
     * @param fromSequenceNumber The starting sequence number (inclusive)
     * @param toSequenceNumber Optional ending sequence number (inclusive)
     *
     * @return List of events ordered by sequence number
     */
    suspend fun getEventsInRange(
        tenantId: String,
        aggregateId: String,
        fromSequenceNumber: Long,
        toSequenceNumber: Long? = null,
    ): List<PersistedEvent>

    /**
     * Saves or updates a snapshot for the specified aggregate.
     *
     * @param snapshot The snapshot to save
     * @param tenantId The tenant identifier for isolation
     * @param aggregateId The aggregate identifier
     */
    suspend fun saveSnapshot(
        snapshot: AggregateSnapshot,
        tenantId: String,
        aggregateId: String,
    )

    /**
     * Retrieves the latest snapshot for the specified aggregate, if any exists.
     *
     * @param tenantId The tenant identifier for isolation
     * @param aggregateId The aggregate identifier
     *
     * @return The latest snapshot or null if no snapshot exists
     */
    suspend fun getSnapshot(
        tenantId: String,
        aggregateId: String,
    ): AggregateSnapshot?

    /**
     * Gets the current version (highest sequence number) for the specified aggregate.
     *
     * @param tenantId The tenant identifier for isolation
     * @param aggregateId The aggregate identifier
     *
     * @return The current version or null if the aggregate doesn't exist
     */
    suspend fun getCurrentVersion(
        tenantId: String,
        aggregateId: String,
    ): Long?
}

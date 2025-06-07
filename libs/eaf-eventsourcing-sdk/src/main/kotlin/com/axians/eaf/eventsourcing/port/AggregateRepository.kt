package com.axians.eaf.eventsourcing.port

import com.axians.eaf.eventsourcing.model.AbstractAggregateRoot

/**
 * Port interface for aggregate repository operations.
 *
 * This interface defines the contract for loading and saving event-sourced aggregates
 * using the EAF Event Store SDK. It provides high-level operations that handle
 * event serialization, aggregate rehydration, and optimistic concurrency control.
 */
interface AggregateRepository<T : AbstractAggregateRoot<ID>, ID : Any> {
    /**
     * Loads an aggregate by its identifier within the specified tenant.
     *
     * This method:
     * 1. Attempts to load the latest snapshot (if available)
     * 2. Loads events from the event store (either from the beginning or after the snapshot)
     * 3. Rehydrates the aggregate by replaying the events
     *
     * @param tenantId The tenant identifier for isolation
     * @param aggregateId The aggregate identifier
     *
     * @return The rehydrated aggregate instance or null if not found
     */
    suspend fun load(
        tenantId: String,
        aggregateId: ID,
    ): T?

    /**
     * Saves an aggregate to the event store.
     *
     * This method:
     * 1. Retrieves uncommitted events from the aggregate
     * 2. Persists them to the event store using optimistic concurrency control
     * 3. Publishes the events to the event bus (if configured)
     * 4. Optionally creates and saves a snapshot
     *
     * @param tenantId The tenant identifier for isolation
     * @param aggregate The aggregate instance to save
     *
     * @throws OptimisticLockingFailureException if a concurrency conflict is detected
     */
    suspend fun save(
        tenantId: String,
        aggregate: T,
    )

    /**
     * Checks if an aggregate exists with the given identifier.
     *
     * @param tenantId The tenant identifier for isolation
     * @param aggregateId The aggregate identifier
     *
     * @return True if the aggregate exists, false otherwise
     */
    suspend fun exists(
        tenantId: String,
        aggregateId: ID,
    ): Boolean

    /**
     * Gets the current version of an aggregate without loading the full aggregate.
     *
     * @param tenantId The tenant identifier for isolation
     * @param aggregateId The aggregate identifier
     *
     * @return The current version or null if the aggregate doesn't exist
     */
    suspend fun getCurrentVersion(
        tenantId: String,
        aggregateId: ID,
    ): Long?

    /**
     * Creates a new aggregate instance.
     *
     * This is a factory method that subclasses should implement to create
     * new instances of their specific aggregate type.
     *
     * @param aggregateId The identifier for the new aggregate
     *
     * @return A new aggregate instance
     */
    fun createNew(aggregateId: ID): T
}

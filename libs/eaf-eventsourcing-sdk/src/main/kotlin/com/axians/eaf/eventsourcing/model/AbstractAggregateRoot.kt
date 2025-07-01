@file:Suppress("TooManyFunctions")

package com.axians.eaf.eventsourcing.model

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Abstract base class for EAF aggregate roots.
 *
 * This class provides the foundation for implementing event-sourced aggregates in the EAF
 * framework. It manages uncommitted domain events, versioning for optimistic concurrency control,
 * and basic aggregate identity.
 *
 * Subclasses should:
 * - Be annotated with @EafAggregate
 * - Contain command handler methods annotated with @EafCommandHandler
 * - Contain event sourcing handler methods annotated with @EafEventSourcingHandler
 * - Use the apply() method to apply new events
 * - Only modify state through event handlers
 */
abstract class AbstractAggregateRoot<ID : Any> {
    /** The unique identifier for this aggregate instance. */
    abstract val aggregateId: ID

    /**
     * The current version of the aggregate (sequence number of the last applied event). Used for
     * optimistic concurrency control.
     */
    var version: Long = 0L
        private set

    /**
     * List of domain events that have been applied but not yet persisted. These events will be
     * persisted when the aggregate is saved.
     */
    private val uncommittedEvents: MutableList<Any> = CopyOnWriteArrayList()

    /**
     * Applies a domain event to this aggregate.
     *
     * This method:
     * 1. Adds the event to the list of uncommitted events
     * 2. Increments the version
     * 3. Calls the appropriate event sourcing handler to update the aggregate's state
     *
     * @param event The domain event to apply
     */
    protected fun apply(event: Any) {
        uncommittedEvents.add(event)
        setVersion(version + 1)
        handleEvent(event)
    }

    /**
     * Rehydrates this aggregate from a sequence of historical events.
     *
     * This method is used by the repository when loading an aggregate from the event store. It
     * replays events in order to reconstruct the aggregate's current state.
     *
     * @param events The historical events to replay
     * @param fromVersion The starting version (typically from a snapshot)
     */
    fun rehydrateFromEvents(
        events: List<Any>,
        fromVersion: Long = 0L,
    ) {
        setVersion(fromVersion)
        events.forEach { event ->
            setVersion(version + 1)
            handleEvent(event)
        }
    }

    /**
     * Returns all uncommitted events and clears the internal list.
     *
     * This method is called by the repository when saving the aggregate to retrieve the events that
     * need to be persisted.
     *
     * @return List of uncommitted domain events
     */
    fun getUncommittedEvents(): List<Any> {
        val events = uncommittedEvents.toList()
        uncommittedEvents.clear()
        return events
    }

    /**
     * Returns the uncommitted events without clearing them.
     *
     * @return List of uncommitted domain events
     */
    fun peekUncommittedEvents(): List<Any> = uncommittedEvents.toList()

    /** Returns true if this aggregate has uncommitted events. */
    fun hasUncommittedEvents(): Boolean = uncommittedEvents.isNotEmpty()

    /**
     * Marks all uncommitted events as committed.
     *
     * This method is called by the repository after successfully persisting events.
     */
    fun markEventsAsCommitted() {
        uncommittedEvents.clear()
    }

    /**
     * Sets the version of the aggregate.
     *
     * This method is intended for internal use by the repository when restoring an aggregate from a
     * snapshot. Should not be used by application code.
     *
     * @param newVersion The version to set
     */
    fun setVersion(newVersion: Long) {
        version = newVersion
    }

    /**
     * Returns the aggregate type name, used for event store stream identification. Defaults to the
     * simple class name, but can be overridden for custom naming.
     */
    open fun getAggregateType(): String = this::class.simpleName ?: "UnknownAggregate"

    /**
     * Handles a domain event by dispatching it to the appropriate event sourcing handler.
     *
     * Subclasses should override this method to implement their event handling logic, typically
     * using when expressions to dispatch to specific handler methods based on event type.
     *
     * @param event The domain event to handle
     */
    protected abstract fun handleEvent(event: Any)

    /**
     * Creates a snapshot representation of this aggregate.
     *
     * Subclasses can override this method to provide custom snapshot serialization. The default
     * implementation returns null, indicating no snapshot support.
     *
     * @return Snapshot data or null if snapshots are not supported
     */
    open fun createSnapshot(): Any? = null

    /**
     * Restores the aggregate state from a snapshot.
     *
     * Subclasses should override this method if they support snapshots. This method is called
     * before replaying events that occurred after the snapshot.
     *
     * @param snapshot The snapshot data to restore from
     */
    open fun restoreFromSnapshot(snapshot: Any) {
        // Default implementation does nothing
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractAggregateRoot<*>) return false
        return aggregateId == other.aggregateId
    }

    override fun hashCode(): Int = aggregateId.hashCode()

    override fun toString(): String = "${getAggregateType()}(id=$aggregateId, version=$version)"
}

package com.axians.eaf.eventsourcing.adapter

import com.axians.eaf.eventsourcing.exception.OptimisticLockingFailureException
import com.axians.eaf.eventsourcing.model.AbstractAggregateRoot
import com.axians.eaf.eventsourcing.model.AggregateSnapshot
import com.axians.eaf.eventsourcing.model.PersistedEvent
import com.axians.eaf.eventsourcing.port.AggregateRepository
import com.axians.eaf.eventsourcing.port.EventStoreRepository
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import java.lang.IllegalStateException
import java.time.Instant
import java.util.UUID

/**
 * Abstract base implementation of AggregateRepository.
 *
 * This class provides the common logic for loading and saving aggregates using the Event Store SDK.
 * Concrete implementations need to provide event serialization/deserialization and aggregate
 * creation logic.
 */
abstract class AbstractAggregateRepository<T : AbstractAggregateRoot<ID>, ID : Any>(
    private val eventStoreRepository: EventStoreRepository,
    protected val objectMapper: ObjectMapper,
    private val eventPublisher: EventPublisher? = null,
) : AggregateRepository<T, ID> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        /** Default interval for creating snapshots (every N events) */
        private const val DEFAULT_SNAPSHOT_INTERVAL = 100L
    }

    override suspend fun load(
        tenantId: String,
        aggregateId: ID,
    ): T? {
        val aggregateIdString = aggregateId.toString()

        // Try to load the latest snapshot
        val snapshot = eventStoreRepository.getSnapshot(tenantId, aggregateIdString)
        var aggregate: T? = null
        var fromVersion = 0L

        if (snapshot != null) {
            try {
                aggregate = createNew(aggregateId)
                val snapshotData = deserializeSnapshot(snapshot.snapshotPayloadJsonb)
                aggregate.restoreFromSnapshot(snapshotData)
                fromVersion = snapshot.lastSequenceNumber
                aggregate.setVersion(fromVersion)
                logger.debug(
                    "Loaded aggregate {} from snapshot at version {} for tenant {}",
                    aggregateIdString,
                    fromVersion,
                    tenantId,
                )
            } catch (e: JsonProcessingException) {
                logger.warn(
                    "Failed to restore aggregate {} from snapshot for tenant {}, loading from events: {}",
                    aggregateIdString,
                    tenantId,
                    e.message,
                )
                aggregate = null
                fromVersion = 0L
            } catch (e: IllegalStateException) {
                logger.warn(
                    "Invalid snapshot state for aggregate {} in tenant {}, loading from events: {}",
                    aggregateIdString,
                    tenantId,
                    e.message,
                )
                aggregate = null
                fromVersion = 0L
            }
        }

        // Load events after the snapshot (or from the beginning)
        val events = eventStoreRepository.getEvents(tenantId, aggregateIdString, fromVersion + 1)

        if (events.isEmpty() && aggregate == null) {
            logger.debug(
                "No events found for aggregate {} in tenant {}",
                aggregateIdString,
                tenantId,
            )
            return null
        }

        // Create aggregate if not loaded from snapshot
        if (aggregate == null) {
            aggregate = createNew(aggregateId)
        }

        // Rehydrate from events
        if (events.isNotEmpty()) {
            val domainEvents = events.map { deserializeEvent(it) }
            aggregate.rehydrateFromEvents(domainEvents, fromVersion)
            logger.debug(
                "Rehydrated aggregate {} with {} events, final version {} for tenant {}",
                aggregateIdString,
                events.size,
                aggregate.version,
                tenantId,
            )
        }

        return aggregate
    }

    override suspend fun save(
        tenantId: String,
        aggregate: T,
    ) {
        val aggregateIdString = aggregate.aggregateId.toString()
        val uncommittedEvents = aggregate.getUncommittedEvents()

        if (uncommittedEvents.isEmpty()) {
            logger.debug(
                "No uncommitted events for aggregate {} in tenant {}",
                aggregateIdString,
                tenantId,
            )
            return
        }

        val aggregateType = aggregate.getAggregateType()
        val expectedVersion = aggregate.version - uncommittedEvents.size
        val streamId = PersistedEvent.createStreamId(aggregateType, aggregateIdString)

        // Convert domain events to persisted events
        val persistedEvents =
            uncommittedEvents.mapIndexed { index, event ->
                PersistedEvent(
                    eventId = UUID.randomUUID(),
                    streamId = streamId,
                    aggregateId = aggregateIdString,
                    aggregateType = aggregateType,
                    expectedVersion = expectedVersion,
                    sequenceNumber = expectedVersion + index + 1,
                    tenantId = tenantId,
                    eventType = event::class.simpleName ?: "UnknownEvent",
                    payload = serializeEvent(event),
                    metadata = createEventMetadata(event, tenantId),
                    timestampUtc = Instant.now(),
                )
            }

        try {
            // Persist events to event store with optimistic locking
            eventStoreRepository.appendEvents(
                events = persistedEvents,
                tenantId = tenantId,
                aggregateId = aggregateIdString,
                expectedVersion = if (expectedVersion == 0L) null else expectedVersion,
            )

            logger.info(
                "Persisted {} events for aggregate {} (version {} -> {}) in tenant {}",
                persistedEvents.size,
                aggregateIdString,
                expectedVersion,
                aggregate.version,
                tenantId,
            )

            // Publish events to event bus (if configured)
            if (eventPublisher != null) {
                publishEvents(persistedEvents, uncommittedEvents, tenantId)
            }

            // Create and save snapshot if needed
            createSnapshotIfNeeded(aggregate, tenantId)

            // Mark events as committed
            aggregate.markEventsAsCommitted()
        } catch (e: OptimisticLockingFailureException) {
            logger.warn(
                "Optimistic locking failure when saving aggregate {} in tenant {}: {}",
                aggregateIdString,
                tenantId,
                e.message,
            )
            throw e
        } catch (e: DataAccessException) {
            logger.error(
                "Failed to save aggregate {} in tenant {}: {}",
                aggregateIdString,
                tenantId,
                e.message,
                e,
            )
            throw e
        } catch (e: JsonProcessingException) {
            logger.error(
                "Failed to serialize event for aggregate {} in tenant {}: {}",
                aggregateIdString,
                tenantId,
                e.message,
                e,
            )
            throw e
        }
    }

    override suspend fun exists(
        tenantId: String,
        aggregateId: ID,
    ): Boolean {
        val version = eventStoreRepository.getCurrentVersion(tenantId, aggregateId.toString())
        return version != null && version > 0
    }

    override suspend fun getCurrentVersion(
        tenantId: String,
        aggregateId: ID,
    ): Long? = eventStoreRepository.getCurrentVersion(tenantId, aggregateId.toString())

    /** Serializes a domain event to JSON. */
    protected open fun serializeEvent(event: Any): String = objectMapper.writeValueAsString(event)

    /**
     * Deserializes a persisted event back to a domain event. Subclasses must implement this to
     * handle their specific event types.
     */
    protected abstract fun deserializeEvent(persistedEvent: PersistedEvent): Any

    /**
     * Creates event metadata for the given domain event. Subclasses can override this to add custom
     * metadata.
     */
    protected open fun createEventMetadata(
        event: Any,
        tenantId: String,
    ): String? {
        val metadata =
            mapOf(
                "tenantId" to tenantId,
                "eventClass" to event::class.qualifiedName,
                "timestamp" to Instant.now().toString(),
            )
        return objectMapper.writeValueAsString(metadata)
    }

    /** Serializes a snapshot to JSON. */
    protected open fun serializeSnapshot(snapshot: Any): String = objectMapper.writeValueAsString(snapshot)

    /** Deserializes a snapshot from JSON. */
    protected abstract fun deserializeSnapshot(snapshotPayload: String): Any

    /** Publishes events to the event bus. */
    private suspend fun publishEvents(
        persistedEvents: List<PersistedEvent>,
        domainEvents: List<Any>,
        tenantId: String,
    ) {
        try {
            eventPublisher?.publishEvents(persistedEvents, domainEvents, tenantId)
        } catch (e: IllegalStateException) {
            logger.error(
                "Failed to publish events due to illegal state for tenant {}: {}",
                tenantId,
                e.message,
                e,
            )
            throw e
        } catch (e: JsonProcessingException) {
            logger.error("Failed to serialize events for tenant {}: {}", tenantId, e.message, e)
            throw e
        }
    }

    /** Creates and saves a snapshot if the aggregate meets snapshotting criteria. */
    private suspend fun createSnapshotIfNeeded(
        aggregate: T,
        tenantId: String,
    ) {
        if (shouldCreateSnapshot(aggregate)) {
            val snapshotData = aggregate.createSnapshot()
            if (snapshotData != null) {
                try {
                    val snapshot =
                        AggregateSnapshot(
                            aggregateId = aggregate.aggregateId.toString(),
                            aggregateType = aggregate.getAggregateType(),
                            lastSequenceNumber = aggregate.version,
                            snapshotPayloadJsonb = serializeSnapshot(snapshotData),
                            tenantId = tenantId,
                            version = 1, // This could be incremented for
                            // snapshot versioning
                            timestampUtc = Instant.now(),
                        )

                    eventStoreRepository.saveSnapshot(
                        snapshot,
                        tenantId,
                        aggregate.aggregateId.toString(),
                    )
                    logger.debug(
                        "Created snapshot for aggregate {} at version {} in tenant {}",
                        aggregate.aggregateId,
                        aggregate.version,
                        tenantId,
                    )
                } catch (e: JsonProcessingException) {
                    logger.warn(
                        "Failed to create snapshot for aggregate {} in tenant {}: {}",
                        aggregate.aggregateId,
                        tenantId,
                        e.message,
                        e,
                    )
                    // Snapshot failures should not fail the overall save
                    // operation
                }
            }
        }
    }

    /**
     * Determines whether a snapshot should be created for the given aggregate. Default
     * implementation creates snapshots every 100 events. Subclasses can override this for custom
     * snapshotting strategies.
     */
    protected open fun shouldCreateSnapshot(aggregate: T): Boolean =
        aggregate.version % DEFAULT_SNAPSHOT_INTERVAL == 0L && aggregate.version > 0
}

/**
 * Interface for publishing events to the event bus. This will be implemented by the Eventing SDK
 * integration.
 */
interface EventPublisher {
    suspend fun publishEvents(
        persistedEvents: List<PersistedEvent>,
        domainEvents: List<Any>,
        tenantId: String,
    )
}

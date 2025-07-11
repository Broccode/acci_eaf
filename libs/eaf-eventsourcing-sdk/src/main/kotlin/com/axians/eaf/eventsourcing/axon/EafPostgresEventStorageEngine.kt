@file:Suppress("NestedBlockDepth")

package com.axians.eaf.eventsourcing.axon

import com.axians.eaf.core.tenancy.TenantContextHolder
import com.axians.eaf.eventsourcing.model.PersistedEvent
import com.axians.eaf.eventsourcing.port.EventStoreRepository
import kotlinx.coroutines.runBlocking
import org.axonframework.eventhandling.DomainEventMessage
import org.axonframework.eventhandling.EventMessage
import org.axonframework.eventhandling.GenericTrackedEventMessage
import org.axonframework.eventhandling.TrackedEventMessage
import org.axonframework.eventhandling.TrackingToken
import org.axonframework.eventsourcing.eventstore.DomainEventStream
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Optional
import java.util.stream.Stream

// Utility functions moved outside class to reduce function count
private fun calculateExpectedVersion(sequenceNumber: Long): Long? =
    if (sequenceNumber == 0L) null else sequenceNumber - 1

private fun extractStartSequence(trackingToken: TrackingToken?): Long =
    if (trackingToken is GlobalSequenceTrackingToken) {
        trackingToken.globalSequence + 1
    } else {
        0L
    }

private fun createTrackedEventStream(
    persistedEvents: List<PersistedEvent>,
    eventMessageMapper: AxonEventMessageMapper,
): Stream<TrackedEventMessage<*>> =
    persistedEvents
        .asSequence()
        .map { event ->
            val token = GlobalSequenceTrackingToken(event.globalSequenceId ?: 0L)
            GenericTrackedEventMessage(token, eventMessageMapper.mapToDomainEvent(event))
        }.asStream()

private fun <T> Sequence<T>.asStream(): Stream<T> = this.toList().stream()

/**
 * PostgreSQL implementation of Axon's EventStorageEngine interface for Axon Framework 4.11.2.
 *
 * This implementation bridges between Axon Framework and the EAF event store, providing event
 * storage and retrieval capabilities with multi-tenant support.
 */
@Component
@Suppress("TooManyFunctions") // Interface implementation requires these specific methods
class EafPostgresEventStorageEngine(
    private val eventStoreRepository: EventStoreRepository,
    private val eventMessageMapper: AxonEventMessageMapper,
    private val exceptionHandler: AxonExceptionHandler,
) : EventStorageEngine {
    private val logger = LoggerFactory.getLogger(EafPostgresEventStorageEngine::class.java)

    override fun appendEvents(events: MutableList<out EventMessage<*>>) {
        try {
            TenantContextHolder.validateTenantContext("appendEvents")
            val tenantId = TenantContextHolder.requireCurrentTenantId()

            events.forEach { event ->
                if (event is DomainEventMessage<*>) {
                    val persistedEvent = eventMessageMapper.mapToPersistedEvent(event, tenantId)
                    val expectedVersion = calculateExpectedVersion(event.sequenceNumber)

                    runBlocking {
                        eventStoreRepository.appendEvents(
                            listOf(persistedEvent),
                            tenantId,
                            event.aggregateIdentifier,
                            expectedVersion,
                        )
                    }
                }
            }
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            val tenantId = TenantContextHolder.getCurrentTenantId() ?: "unknown"
            throw exceptionHandler.handleAppendException(
                "appendEvents",
                tenantId,
                null,
                events.size,
                e,
            )
        }
    }

    override fun appendEvents(vararg events: EventMessage<*>) = appendEvents(events.toMutableList())

    override fun readEvents(
        aggregateIdentifier: String,
        firstSequenceNumber: Long,
    ): DomainEventStream =
        try {
            TenantContextHolder.validateTenantContext("readEvents")
            val tenantId = TenantContextHolder.requireCurrentTenantId()
            val persistedEvents =
                runBlocking {
                    eventStoreRepository.getEvents(
                        tenantId,
                        aggregateIdentifier,
                        firstSequenceNumber,
                    )
                }
            val domainEvents = persistedEvents.map { eventMessageMapper.mapToDomainEvent(it) }
            DomainEventStream.of(domainEvents)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            val tenantId = TenantContextHolder.getCurrentTenantId() ?: "unknown"
            throw exceptionHandler.handleReadException(
                "event loading",
                tenantId,
                aggregateIdentifier,
                e,
            )
        }

    override fun readEvents(
        trackingToken: TrackingToken?,
        mayBlock: Boolean,
    ): Stream<TrackedEventMessage<*>> =
        try {
            TenantContextHolder.validateTenantContext("readEventsTracking")
            val tenantId = TenantContextHolder.requireCurrentTenantId()
            val startSequence = extractStartSequence(trackingToken)
            val persistedEvents =
                runBlocking {
                    eventStoreRepository.readEventsFrom(tenantId, startSequence)
                }
            createTrackedEventStream(persistedEvents, eventMessageMapper)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            val tenantId = TenantContextHolder.getCurrentTenantId() ?: "unknown"
            throw exceptionHandler.handleReadException(
                "event loading",
                tenantId,
                null,
                e,
            )
        }

    override fun createHeadToken(): TrackingToken =
        try {
            TenantContextHolder.validateTenantContext("createHeadToken")
            val tenantId = TenantContextHolder.requireCurrentTenantId()
            val maxSequence =
                runBlocking {
                    eventStoreRepository.getMaxGlobalSequence(tenantId)
                }
            GlobalSequenceTrackingToken(maxSequence)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            val tenantId = TenantContextHolder.getCurrentTenantId() ?: "unknown"
            logger.warn(
                "Failed to create head token for tenant {}, using fallback",
                tenantId,
                e, // Pass exception as third parameter to log full stack trace
            )
            GlobalSequenceTrackingToken.initial()
        }

    override fun createTailToken(): TrackingToken = GlobalSequenceTrackingToken.initial()

    override fun createTokenAt(dateTime: Instant): TrackingToken = createTailToken()

    override fun storeSnapshot(snapshot: DomainEventMessage<*>) {
        try {
            TenantContextHolder.validateTenantContext("storeSnapshot")
            val tenantId = TenantContextHolder.requireCurrentTenantId()
            val aggregateSnapshot = eventMessageMapper.mapToAggregateSnapshot(snapshot, tenantId)
            runBlocking {
                eventStoreRepository.saveSnapshot(
                    aggregateSnapshot,
                    tenantId,
                    snapshot.aggregateIdentifier,
                )
            }
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            val tenantId = TenantContextHolder.getCurrentTenantId() ?: "unknown"
            throw exceptionHandler.handleAppendException(
                "storeSnapshot",
                tenantId,
                snapshot.aggregateIdentifier,
                1,
                e,
            )
        }
    }

    override fun readSnapshot(aggregateIdentifier: String): Optional<DomainEventMessage<*>> =
        try {
            TenantContextHolder.validateTenantContext("readSnapshot")
            val tenantId = TenantContextHolder.requireCurrentTenantId()
            val snapshot =
                runBlocking {
                    eventStoreRepository.getSnapshot(tenantId, aggregateIdentifier)
                }
            if (snapshot != null) {
                Optional.of(eventMessageMapper.mapSnapshotToDomainEvent(snapshot))
            } else {
                Optional.empty()
            }
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            val tenantId = TenantContextHolder.getCurrentTenantId() ?: "unknown"
            logger.warn(
                "Failed to read snapshot for tenant {} and aggregate {}",
                tenantId,
                aggregateIdentifier,
                e, // Pass exception as third parameter to log full stack trace
            )
            Optional.empty()
        }
}

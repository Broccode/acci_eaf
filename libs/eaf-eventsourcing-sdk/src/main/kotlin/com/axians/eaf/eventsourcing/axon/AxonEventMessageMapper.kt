package com.axians.eaf.eventsourcing.axon

import com.axians.eaf.eventsourcing.axon.exception.EventSerializationException
import com.axians.eaf.eventsourcing.model.AggregateSnapshot
import com.axians.eaf.eventsourcing.model.PersistedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.axonframework.eventhandling.DomainEventMessage
import org.axonframework.eventhandling.GenericDomainEventMessage
import org.axonframework.messaging.MetaData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Utility class for bidirectional conversion between Axon EventMessage types and EAF event store
 * models.
 *
 * This mapper handles:
 * - DomainEventMessage ↔ PersistedEvent conversion
 * - AggregateSnapshot ↔ DomainEventMessage conversion for snapshots
 * - Event metadata preservation and serialization
 * - Proper type resolution and error handling
 * - Axon Framework 5.0.0-M2 compatibility
 */
@Component
class AxonEventMessageMapper(
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(AxonEventMessageMapper::class.java)

    companion object {
        private const val TENANT_ID_META_KEY = "tenant_id"
        private const val AGGREGATE_VERSION_META_KEY = "aggregate_version"
        private const val CORRELATION_ID_META_KEY = "correlation_id"
        private const val CAUSATION_ID_META_KEY = "causation_id"
        private const val GLOBAL_SEQUENCE_META_KEY = "global_sequence_id"
        private const val EVENT_TIMESTAMP_META_KEY = "event_timestamp"
    }

    /**
     * Converts an Axon DomainEventMessage to a PersistedEvent for storage.
     *
     * @param event The Axon domain event message
     * @param tenantId The tenant identifier for multi-tenant isolation
     * @return PersistedEvent ready for persistence
     * @throws IllegalArgumentException if the event lacks required fields
     */
    fun mapToPersistedEvent(
        event: DomainEventMessage<*>,
        tenantId: String,
    ): PersistedEvent {
        val aggregateId =
            event.aggregateIdentifier
                ?: throw IllegalArgumentException(
                    "Event must have an aggregate identifier",
                )
        val aggregateType = event.type ?: "UnknownAggregate"

        return PersistedEvent(
            eventId = UUID.fromString(event.identifier),
            streamId = PersistedEvent.createStreamId(aggregateType, aggregateId),
            aggregateId = aggregateId,
            aggregateType = aggregateType,
            expectedVersion = null, // Will be set by repository
            sequenceNumber = event.sequenceNumber,
            tenantId = tenantId,
            eventType = event.payloadType.name,
            payload = serializePayload(event.payload),
            metadata = serializeMetadata(event.metaData, tenantId, event),
            timestampUtc = event.timestamp,
        )
    }

    /**
     * Converts a PersistedEvent back to an Axon DomainEventMessage.
     *
     * @param persistedEvent The persisted event from the event store
     * @return Reconstructed Axon DomainEventMessage
     */
    fun mapToDomainEvent(persistedEvent: PersistedEvent): DomainEventMessage<*> {
        val payload = deserializePayload(persistedEvent.payload, persistedEvent.eventType)
        val metadata =
            deserializeMetadata(persistedEvent.metadata)
                .and(
                    GLOBAL_SEQUENCE_META_KEY,
                    persistedEvent.globalSequenceId?.toString() ?: "0",
                ).and(
                    EVENT_TIMESTAMP_META_KEY,
                    persistedEvent.timestampUtc.toString(),
                )

        return GenericDomainEventMessage(
            persistedEvent.aggregateType,
            persistedEvent.aggregateId,
            persistedEvent.sequenceNumber,
            payload,
            metadata,
        )
    }

    /**
     * Converts an Axon DomainEventMessage to an AggregateSnapshot for storage.
     *
     * @param snapshot The snapshot event message
     * @param tenantId The tenant identifier
     * @return AggregateSnapshot ready for persistence
     */
    fun mapToAggregateSnapshot(
        snapshot: DomainEventMessage<*>,
        tenantId: String,
    ): AggregateSnapshot {
        val aggregateId =
            snapshot.aggregateIdentifier
                ?: throw IllegalArgumentException(
                    "Snapshot must have an aggregate identifier",
                )

        return AggregateSnapshot(
            aggregateId = aggregateId,
            tenantId = tenantId,
            aggregateType = snapshot.type ?: "UnknownAggregate",
            lastSequenceNumber = snapshot.sequenceNumber,
            snapshotPayloadJsonb = serializePayload(snapshot.payload),
            version = 1, // Could be incremented for snapshot versioning
            timestampUtc = snapshot.timestamp,
        )
    }

    /**
     * Converts an AggregateSnapshot back to an Axon DomainEventMessage for snapshot loading.
     *
     * @param snapshot The aggregate snapshot from storage
     * @return Reconstructed snapshot as DomainEventMessage
     */
    fun mapSnapshotToDomainEvent(snapshot: AggregateSnapshot): DomainEventMessage<*> {
        val payload = deserializePayload(snapshot.snapshotPayloadJsonb, "AggregateSnapshot")
        val metadata =
            MetaData.from(
                mapOf(
                    TENANT_ID_META_KEY to snapshot.tenantId,
                    AGGREGATE_VERSION_META_KEY to
                        snapshot.version
                            .toString(), // Use version field for
                    // snapshot
                    // metadata
                    EVENT_TIMESTAMP_META_KEY to
                        snapshot.timestampUtc.toString(),
                ),
            )

        return GenericDomainEventMessage(
            snapshot.aggregateType,
            snapshot.aggregateId,
            snapshot.lastSequenceNumber,
            payload,
            metadata,
        )
    }

    /**
     * Extracts the tenant ID from event metadata.
     *
     * @param event The Axon event message
     * @return Tenant ID or null if not found
     */
    fun extractTenantId(event: DomainEventMessage<*>): String? = event.metaData[TENANT_ID_META_KEY]?.toString()

    /**
     * Adds tenant context to event metadata.
     *
     * @param metadata Original metadata
     * @param tenantId Tenant identifier to add
     * @return Enhanced metadata with tenant context
     */
    fun addTenantContext(
        metadata: MetaData,
        tenantId: String,
    ): MetaData = metadata.and(TENANT_ID_META_KEY, tenantId)

    /** Serializes event payload to JSON string. */
    private fun serializePayload(payload: Any?): String =
        try {
            if (payload == null) {
                "null"
            } else {
                objectMapper.writeValueAsString(payload)
            }
        } catch (e: Exception) {
            logger.error(
                "Failed to serialize event payload of type {}: {}",
                payload?.javaClass?.name,
                e.message,
                e,
            )
            throw EventSerializationException("Failed to serialize event payload", e)
        }

    /** Deserializes event payload from JSON string. */
    private fun deserializePayload(
        payloadJson: String,
        eventType: String,
    ): Any? =
        try {
            if (payloadJson == "null") {
                null
            } else {
                // For now, deserialize as a generic object
                // In a real implementation, you'd use proper type resolution based
                // on eventType
                objectMapper.readValue(payloadJson, Any::class.java)
            }
        } catch (e: Exception) {
            logger.error(
                "Failed to deserialize event payload for type {}: {}",
                eventType,
                e.message,
                e,
            )
            throw EventSerializationException(
                "Failed to deserialize event payload for type $eventType",
                e,
            )
        }

    /** Serializes event metadata to JSON, enriching with event context. */
    private fun serializeMetadata(
        metadata: MetaData,
        tenantId: String,
        event: DomainEventMessage<*>,
    ): String? =
        try {
            if (metadata.isEmpty()) {
                // Create minimal metadata even if original is empty
                val minimalMetadata =
                    mapOf(
                        TENANT_ID_META_KEY to tenantId,
                        AGGREGATE_VERSION_META_KEY to
                            event.sequenceNumber.toString(),
                        EVENT_TIMESTAMP_META_KEY to
                            event.timestamp.toString(),
                    )
                objectMapper.writeValueAsString(minimalMetadata)
            } else {
                // Enhance existing metadata with tenant context - Axon 5 uses
                // string-based
                // metadata
                val enhancedMetadata =
                    metadata.asStringMap().apply {
                        put(TENANT_ID_META_KEY, tenantId)
                        put(
                            AGGREGATE_VERSION_META_KEY,
                            event.sequenceNumber.toString(),
                        )
                        put(
                            EVENT_TIMESTAMP_META_KEY,
                            event.timestamp.toString(),
                        )
                    }
                objectMapper.writeValueAsString(enhancedMetadata)
            }
        } catch (e: Exception) {
            logger.error("Failed to serialize event metadata: {}", e.message, e)
            throw EventSerializationException("Failed to serialize event metadata", e)
        }

    /** Deserializes event metadata from JSON. */
    private fun deserializeMetadata(metadataJson: String?): MetaData =
        try {
            if (metadataJson.isNullOrBlank()) {
                MetaData.emptyInstance()
            } else {
                @Suppress("UNCHECKED_CAST")
                val map =
                    objectMapper.readValue(metadataJson, Map::class.java) as
                        Map<String, Any>
                // Convert all values to strings for Axon 5 compatibility
                val stringMap = map.mapValues { it.value?.toString() ?: "" }
                MetaData.from(stringMap)
            }
        } catch (e: Exception) {
            logger.warn(
                "Failed to deserialize event metadata, returning empty metadata: {}",
                e.message,
            )
            MetaData.emptyInstance()
        }

    /** Extension function to convert MetaData to string map for Axon 5 compatibility */
    private fun MetaData.asStringMap(): MutableMap<String, String> =
        this.entries
            .associate { (key, value) -> key to (value?.toString() ?: "") }
            .toMutableMap()
}

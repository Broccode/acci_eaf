package com.axians.eaf.eventsourcing.axon.tenancy

import com.axians.eaf.core.tenancy.TenantContextHolder
import org.axonframework.messaging.MetaData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Utility class for managing tenant-aware event metadata in Axon Framework.
 *
 * This class provides functionality to:
 * - Ensure tenant ID propagates from commands to events
 * - Support tenant context in domain event publishing
 * - Handle tenant metadata in event upcasting scenarios
 * - Maintain tenant context through event transformation chains
 * - Add tenant validation for cross-tenant event processing
 */
@Component
class TenantAwareEventMetadata {
    companion object {
        private val logger = LoggerFactory.getLogger(TenantAwareEventMetadata::class.java)

        /** Length of UUID suffix in correlation IDs */
        private const val CORRELATION_UUID_SUFFIX_LENGTH = 8

        /** Standard metadata key for tenant ID */
        const val TENANT_ID_KEY = "tenant_id"

        /** Alternative metadata key for tenant ID (camelCase) */
        const val TENANT_ID_CAMEL_CASE_KEY = "tenantId"

        /** Source of the event metadata key */
        const val EVENT_SOURCE_KEY = "event_source"

        /** Timestamp when event was created */
        const val EVENT_TIMESTAMP_KEY = "event_timestamp"

        /** Original command that triggered this event */
        const val ORIGINATING_COMMAND_KEY = "originating_command"

        /** Event sequence number within tenant */
        const val TENANT_EVENT_SEQUENCE_KEY = "tenant_event_sequence"

        /** Indicates if this is a replay scenario */
        const val EVENT_REPLAY_KEY = "event_replay"

        /** Correlation ID for tracing across tenant boundaries */
        const val CORRELATION_ID_KEY = "correlation_id"

        /** Event transformation chain information */
        const val TRANSFORMATION_CHAIN_KEY = "transformation_chain"

        /** Possible sources of events */
        enum class EventSource {
            COMMAND_HANDLER,
            DOMAIN_EVENT,
            SAGA_EVENT,
            SYSTEM_EVENT,
            UPCASTED_EVENT,
            REPLAYED_EVENT,
        }
    }

    /**
     * Creates event metadata that inherits tenant context from command metadata.
     *
     * This method ensures tenant ID and context flow seamlessly from commands to events,
     * maintaining tenant isolation throughout the CQRS/ES processing chain.
     *
     * @param commandMetadata The originating command metadata
     * @param eventSource The source type of this event
     * @param additionalMetadata Additional metadata to include
     * @return MetaData with tenant context inherited from command
     */
    fun createEventMetadataFromCommand(
        commandMetadata: MetaData,
        eventSource: EventSource = EventSource.COMMAND_HANDLER,
        additionalMetadata: Map<String, Any> = emptyMap(),
    ): MetaData {
        val tenantId =
            extractTenantIdFromMetadata(commandMetadata)
                ?: TenantContextHolder.getCurrentTenantId()

        val metadataMap =
            mutableMapOf<String, Any>().apply {
                // Inherit tenant context from command
                if (tenantId != null) {
                    put(TENANT_ID_KEY, tenantId)
                    put(EVENT_TIMESTAMP_KEY, System.currentTimeMillis())
                    put(EVENT_SOURCE_KEY, eventSource.name)

                    // Inherit correlation ID if present
                    val correlationId = commandMetadata.get(CORRELATION_ID_KEY) as? String
                    if (!correlationId.isNullOrBlank()) {
                        put(CORRELATION_ID_KEY, correlationId)
                    }

                    // Track originating command type
                    val commandType = commandMetadata.get("command_type") as? String
                    if (!commandType.isNullOrBlank()) {
                        put(ORIGINATING_COMMAND_KEY, commandType)
                    }

                    logger.debug(
                        "Created event metadata for tenant: {} from command source: {}",
                        tenantId,
                        eventSource,
                    )
                } else {
                    logger.warn(
                        "Creating event without tenant context - this may indicate a system event",
                    )
                    put(EVENT_SOURCE_KEY, EventSource.SYSTEM_EVENT.name)
                    put(EVENT_TIMESTAMP_KEY, System.currentTimeMillis())
                }

                // Add any additional metadata
                putAll(additionalMetadata)
            }

        return MetaData.from(metadataMap)
    }

    /**
     * Creates metadata for domain events published directly (not from command handlers).
     *
     * @param tenantId The tenant ID for the domain event
     * @param domainEventType The type of domain event
     * @param additionalMetadata Additional metadata to include
     * @return MetaData configured for domain event publishing
     */
    fun createDomainEventMetadata(
        tenantId: String? = null,
        domainEventType: String,
        additionalMetadata: Map<String, Any> = emptyMap(),
    ): MetaData {
        val effectiveTenantId = tenantId ?: TenantContextHolder.getCurrentTenantId()

        val metadataMap =
            mutableMapOf<String, Any>().apply {
                if (effectiveTenantId != null) {
                    put(TENANT_ID_KEY, effectiveTenantId)
                    put(EVENT_SOURCE_KEY, EventSource.DOMAIN_EVENT.name)
                    put(EVENT_TIMESTAMP_KEY, System.currentTimeMillis())
                    put("domain_event_type", domainEventType)

                    // Generate correlation ID for domain events
                    val correlationId = generateCorrelationId()
                    put(CORRELATION_ID_KEY, correlationId)

                    logger.debug(
                        "Created domain event metadata for tenant: {} type: {}",
                        effectiveTenantId,
                        domainEventType,
                    )
                } else {
                    logger.warn(
                        "Creating domain event without tenant context: {}",
                        domainEventType,
                    )
                    put(EVENT_SOURCE_KEY, EventSource.SYSTEM_EVENT.name)
                    put(EVENT_TIMESTAMP_KEY, System.currentTimeMillis())
                }

                putAll(additionalMetadata)
            }

        return MetaData.from(metadataMap)
    }

    /**
     * Handles event metadata during upcasting scenarios.
     *
     * This ensures tenant context is preserved when events are transformed during event store
     * replay or migration processes.
     *
     * @param originalMetadata The original event metadata
     * @param targetEventType The target event type after upcasting
     * @param transformationInfo Information about the transformation
     * @return Enhanced metadata for upcasted event
     */
    fun createUpcastedEventMetadata(
        originalMetadata: MetaData,
        targetEventType: String,
        transformationInfo: Map<String, Any> = emptyMap(),
    ): MetaData {
        val tenantId = extractTenantIdFromMetadata(originalMetadata)

        // Preserve original metadata and enhance with upcasting information
        val enhancedMetadata =
            originalMetadata
                .and(EVENT_SOURCE_KEY, EventSource.UPCASTED_EVENT.name)
                .and("upcasted_to", targetEventType)
                .and("upcasting_timestamp", System.currentTimeMillis())

        // Add transformation chain information
        val existingChain = originalMetadata.get(TRANSFORMATION_CHAIN_KEY) as? List<*>
        val newChainEntry =
            mapOf(
                "timestamp" to System.currentTimeMillis(),
                "type" to "upcasting",
                "target" to targetEventType,
                "info" to transformationInfo,
            )

        val updatedChain = (existingChain ?: emptyList<Any>()) + newChainEntry
        val finalMetadata = enhancedMetadata.and(TRANSFORMATION_CHAIN_KEY, updatedChain)

        if (tenantId != null) {
            logger.debug(
                "Created upcasted event metadata for tenant: {} to type: {}",
                tenantId,
                targetEventType,
            )
        }

        return finalMetadata
    }

    /**
     * Creates metadata for event replay scenarios.
     *
     * @param originalMetadata The original event metadata
     * @param replayReason The reason for replaying events
     * @param replayBatch Optional batch identifier for grouped replays
     * @return Metadata configured for event replay
     */
    fun createReplayEventMetadata(
        originalMetadata: MetaData,
        replayReason: String,
        replayBatch: String? = null,
    ): MetaData {
        val tenantId = extractTenantIdFromMetadata(originalMetadata)

        val replayMetadata =
            originalMetadata
                .and(EVENT_SOURCE_KEY, EventSource.REPLAYED_EVENT.name)
                .and(EVENT_REPLAY_KEY, true)
                .and("replay_reason", replayReason)
                .and("replay_timestamp", System.currentTimeMillis())

        val finalMetadata =
            if (replayBatch != null) {
                replayMetadata.and("replay_batch", replayBatch)
            } else {
                replayMetadata
            }

        if (tenantId != null) {
            logger.debug(
                "Created replay event metadata for tenant: {} reason: {}",
                tenantId,
                replayReason,
            )
        }

        return finalMetadata
    }

    /**
     * Validates that event processing doesn't cross tenant boundaries inappropriately.
     *
     * @param eventMetadata The event metadata to validate
     * @param allowedTenantId The tenant ID that should be processing this event (null means any)
     * @throws TenantValidationException if validation fails
     */
    fun validateTenantBoundaries(
        eventMetadata: MetaData,
        allowedTenantId: String? = null,
    ) {
        val eventTenantId = extractTenantIdFromMetadata(eventMetadata)
        val currentTenantId = TenantContextHolder.getCurrentTenantId()

        // If event has no tenant context, it might be a system event
        if (eventTenantId.isNullOrBlank()) {
            val eventSource = eventMetadata.get(EVENT_SOURCE_KEY) as? String
            if (eventSource != EventSource.SYSTEM_EVENT.name) {
                logger.warn("Event without tenant context from non-system source: {}", eventSource)
            }
            return
        }

        // Validate against allowed tenant if specified
        if (allowedTenantId != null && eventTenantId != allowedTenantId) {
            throw TenantValidationException(
                "Event tenant '$eventTenantId' does not match allowed tenant '$allowedTenantId'",
            )
        }

        // Validate against current processing context
        if (currentTenantId != null && eventTenantId != currentTenantId) {
            throw TenantValidationException(
                "Event tenant '$eventTenantId' does not match current processing tenant '$currentTenantId'",
            )
        }

        logger.debug("Tenant boundary validation successful for tenant: {}", eventTenantId)
    }

    /**
     * Enhances existing event metadata with additional tenant context.
     *
     * @param existingMetadata The existing event metadata
     * @param tenantId Optional tenant ID to set
     * @param additionalMetadata Additional metadata to merge
     * @return Enhanced event metadata
     */
    fun enhanceEventMetadata(
        existingMetadata: MetaData,
        tenantId: String? = null,
        additionalMetadata: Map<String, Any> = emptyMap(),
    ): MetaData {
        val effectiveTenantId =
            tenantId
                ?: extractTenantIdFromMetadata(existingMetadata)
                ?: TenantContextHolder.getCurrentTenantId()

        var enhancedMetadata = existingMetadata

        // Add tenant ID if not present or if explicitly provided
        if (effectiveTenantId != null) {
            val existingTenantId = extractTenantIdFromMetadata(existingMetadata)
            if (existingTenantId != effectiveTenantId) {
                enhancedMetadata = enhancedMetadata.and(TENANT_ID_KEY, effectiveTenantId)
                logger.debug("Enhanced event metadata with tenant ID: {}", effectiveTenantId)
            }
        }

        // Add additional metadata
        for ((key, value) in additionalMetadata) {
            enhancedMetadata = enhancedMetadata.and(key, value)
        }

        return enhancedMetadata
    }

    /**
     * Extracts tenant ID from event metadata using standard keys.
     *
     * @param metaData The event metadata
     * @return The tenant ID if found, null otherwise
     */
    private fun extractTenantIdFromMetadata(metaData: MetaData): String? {
        // Try primary key first
        val tenantId = metaData.get(TENANT_ID_KEY) as? String
        if (!tenantId.isNullOrBlank()) {
            return tenantId.trim()
        }

        // Try alternative camelCase key
        val tenantIdCamelCase = metaData.get(TENANT_ID_CAMEL_CASE_KEY) as? String
        if (!tenantIdCamelCase.isNullOrBlank()) {
            return tenantIdCamelCase.trim()
        }

        return null
    }

    /**
     * Generates a unique correlation ID for tracing across tenant boundaries.
     *
     * @return A unique correlation ID
     */
    private fun generateCorrelationId(): String =
        "corr-${System.currentTimeMillis()}-${java.util.UUID.randomUUID().toString().take(
            CORRELATION_UUID_SUFFIX_LENGTH,
        )}"
}

/** Exception thrown when tenant validation fails during event processing. */
class TenantValidationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

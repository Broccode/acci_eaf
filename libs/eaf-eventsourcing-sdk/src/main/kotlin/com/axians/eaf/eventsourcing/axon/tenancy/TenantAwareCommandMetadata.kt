package com.axians.eaf.eventsourcing.axon.tenancy

import com.axians.eaf.core.tenancy.TenantContextHolder
import org.axonframework.messaging.MetaData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

// Note: Spring Web dependencies would be needed for HTTP request integration
// For now, we'll provide a simpler implementation without HTTP extraction

/**
 * Utility class for managing tenant-aware command metadata in Axon Framework.
 *
 * This class provides functionality to:
 * - Automatically inject tenant ID into command metadata
 * - Support tenant context inheritance from HTTP requests
 * - Validate tenant authorization for command execution
 * - Add tenant-aware command routing capabilities
 * - Handle tenant context for scheduled/background commands
 */
@Component
class TenantAwareCommandMetadata {
    companion object {
        private val logger = LoggerFactory.getLogger(TenantAwareCommandMetadata::class.java)

        /** Standard metadata key for tenant ID */
        const val TENANT_ID_KEY = "tenant_id"

        /** Source of tenant context metadata key */
        const val TENANT_SOURCE_KEY = "tenant_source"

        /** Timestamp when tenant context was set */
        const val TENANT_TIMESTAMP_KEY = "tenant_timestamp"

        /** Indicates if command requires tenant authorization */
        const val REQUIRES_TENANT_AUTH_KEY = "requires_tenant_auth"

        /** Command priority for tenant-aware routing */
        const val TENANT_COMMAND_PRIORITY_KEY = "tenant_command_priority"

        /** Possible sources of tenant context */
        enum class TenantSource {
            HTTP_REQUEST,
            THREAD_CONTEXT,
            EXPLICIT_PARAMETER,
            SCHEDULED_TASK,
            BACKGROUND_PROCESS,
            SYSTEM_COMMAND,
        }

        /** Command priorities for tenant-aware routing */
        enum class CommandPriority {
            LOW,
            NORMAL,
            HIGH,
            URGENT,
        }
    }

    /**
     * Creates command metadata with automatic tenant ID injection.
     *
     * This method attempts to resolve tenant context from multiple sources in order:
     * 1. Explicit tenant ID parameter
     * 2. Current thread context (TenantContextHolder)
     * 3. HTTP request context (if available)
     * 4. Falls back to system command if no tenant context available
     *
     * @param explicitTenantId Optional explicit tenant ID to use
     * @param requiresAuth Whether this command requires tenant authorization
     * @param priority Command priority for routing
     * @param additionalMetadata Additional metadata to include
     * @return MetaData with tenant context and routing information
     */
    fun createTenantAwareMetadata(
        explicitTenantId: String? = null,
        requiresAuth: Boolean = true,
        priority: CommandPriority = CommandPriority.NORMAL,
        additionalMetadata: Map<String, Any> = emptyMap(),
    ): MetaData {
        val tenantContext = resolveTenantContext(explicitTenantId)

        val metadataMap =
            mutableMapOf<String, Any>().apply {
                // Add tenant information
                if (tenantContext.tenantId != null) {
                    put(TENANT_ID_KEY, tenantContext.tenantId)
                    put(TENANT_SOURCE_KEY, tenantContext.source.name)
                    put(TENANT_TIMESTAMP_KEY, System.currentTimeMillis())

                    logger.debug(
                        "Created tenant-aware metadata for tenant: {} from source: {}",
                        tenantContext.tenantId,
                        tenantContext.source,
                    )
                } else {
                    put(TENANT_SOURCE_KEY, TenantSource.SYSTEM_COMMAND.name)
                    logger.debug("Created metadata for system command (no tenant context)")
                }

                // Add authorization requirement
                put(REQUIRES_TENANT_AUTH_KEY, requiresAuth)

                // Add command routing priority
                put(TENANT_COMMAND_PRIORITY_KEY, priority.name)

                // Add any additional metadata
                putAll(additionalMetadata)
            }

        return MetaData.from(metadataMap)
    }

    /**
     * Enhances existing metadata with tenant context if not already present.
     *
     * @param existingMetadata The existing command metadata
     * @param explicitTenantId Optional explicit tenant ID to use
     * @return Enhanced metadata with tenant context
     */
    fun enhanceWithTenantContext(
        existingMetadata: MetaData,
        explicitTenantId: String? = null,
    ): MetaData {
        // Check if tenant context is already present
        val existingTenantId = existingMetadata.get(TENANT_ID_KEY) as? String

        return if (existingTenantId.isNullOrBlank()) {
            val tenantContext = resolveTenantContext(explicitTenantId)

            if (tenantContext.tenantId != null) {
                existingMetadata
                    .and(TENANT_ID_KEY, tenantContext.tenantId)
                    .and(TENANT_SOURCE_KEY, tenantContext.source.name)
                    .and(TENANT_TIMESTAMP_KEY, System.currentTimeMillis())
            } else {
                existingMetadata.and(TENANT_SOURCE_KEY, TenantSource.SYSTEM_COMMAND.name)
            }
        } else {
            logger.debug("Metadata already contains tenant ID: {}", existingTenantId)
            existingMetadata
        }
    }

    /**
     * Validates tenant authorization for command execution.
     *
     * @param metadata The command metadata
     * @param requiredTenantId The tenant ID required for authorization (null means any tenant)
     * @throws TenantAuthorizationException if authorization fails
     */
    fun validateTenantAuthorization(
        metadata: MetaData,
        requiredTenantId: String? = null,
    ) {
        val requiresAuth = metadata.get(REQUIRES_TENANT_AUTH_KEY) as? Boolean ?: true

        if (!requiresAuth) {
            logger.debug("Command does not require tenant authorization")
            return
        }

        val commandTenantId = metadata.get(TENANT_ID_KEY) as? String

        if (commandTenantId.isNullOrBlank()) {
            throw TenantAuthorizationException(
                "Command requires tenant authorization but no tenant ID found in metadata",
            )
        }

        if (requiredTenantId != null && commandTenantId != requiredTenantId) {
            throw TenantAuthorizationException(
                "Tenant authorization failed: command tenant '$commandTenantId' does not match required tenant '$requiredTenantId'",
            )
        }

        logger.debug("Tenant authorization successful for tenant: {}", commandTenantId)
    }

    /**
     * Creates metadata for scheduled or background commands.
     *
     * @param tenantId The tenant ID for the scheduled command
     * @param scheduledBy Who/what scheduled this command
     * @param additionalMetadata Additional metadata to include
     * @return MetaData configured for scheduled execution
     */
    fun createScheduledCommandMetadata(
        tenantId: String,
        scheduledBy: String,
        additionalMetadata: Map<String, Any> = emptyMap(),
    ): MetaData {
        val metadataMap =
            mutableMapOf<String, Any>().apply {
                put(TENANT_ID_KEY, tenantId)
                put(TENANT_SOURCE_KEY, TenantSource.SCHEDULED_TASK.name)
                put(TENANT_TIMESTAMP_KEY, System.currentTimeMillis())
                put(REQUIRES_TENANT_AUTH_KEY, true)
                put(TENANT_COMMAND_PRIORITY_KEY, CommandPriority.NORMAL.name)
                put("scheduled_by", scheduledBy)
                put("execution_type", "scheduled")
                putAll(additionalMetadata)
            }

        logger.debug(
            "Created scheduled command metadata for tenant: {} by: {}",
            tenantId,
            scheduledBy,
        )
        return MetaData.from(metadataMap)
    }

    /**
     * Resolves tenant context from available sources.
     *
     * @param explicitTenantId Optional explicit tenant ID
     * @return TenantContextInfo with resolved tenant ID and source
     */
    private fun resolveTenantContext(explicitTenantId: String?): TenantContextInfo {
        // 1. Check explicit parameter first
        if (!explicitTenantId.isNullOrBlank()) {
            return TenantContextInfo(explicitTenantId.trim(), TenantSource.EXPLICIT_PARAMETER)
        }

        // 2. Check current thread context
        val threadTenantId = TenantContextHolder.getCurrentTenantId()
        if (!threadTenantId.isNullOrBlank()) {
            return TenantContextInfo(threadTenantId, TenantSource.THREAD_CONTEXT)
        }

        // 3. HTTP request extraction not available without Spring Web dependencies
        // Skip HTTP extraction for now

        // 4. No tenant context available
        return TenantContextInfo(null, TenantSource.SYSTEM_COMMAND)
    }

    /** Data class to hold tenant context resolution information. */
    private data class TenantContextInfo(
        val tenantId: String?,
        val source: TenantSource,
    )
}

/** Exception thrown when tenant authorization fails. */
class TenantAuthorizationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

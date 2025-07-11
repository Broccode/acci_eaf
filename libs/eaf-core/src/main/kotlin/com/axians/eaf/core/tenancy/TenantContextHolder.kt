package com.axians.eaf.core.tenancy

import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

/**
 * Thread-safe tenant context holder for multi-tenant applications.
 *
 * Provides static methods for managing tenant context across threads and coroutines, ensuring
 * strict data isolation within the EventStorageEngine and other components.
 *
 * This implementation uses ThreadLocal storage for thread safety and provides enhanced context
 * management including scoped execution and coroutine support.
 */
@Suppress("TooManyFunctions")
object TenantContextHolder {
    private val logger = LoggerFactory.getLogger(TenantContextHolder::class.java)

    /** Maximum allowed length for tenant IDs to prevent abuse. */
    private const val MAX_TENANT_ID_LENGTH = 64

    /** ThreadLocal storage for tenant ID with proper initialization and cleanup. */
    private val tenantContext: ThreadLocal<String> = ThreadLocal.withInitial { null }

    /** ThreadLocal storage for full tenant context metadata. */
    private val fullTenantContext: ThreadLocal<TenantContext> = ThreadLocal.withInitial { null }

    /**
     * Sets the current tenant ID for the current thread.
     *
     * @param tenantId The tenant identifier to set
     * @throws IllegalArgumentException if tenantId is blank
     */
    @JvmStatic
    fun setCurrentTenantId(tenantId: String) {
        require(tenantId.isNotBlank()) { "Tenant ID cannot be blank" }

        val sanitizedTenantId = sanitizeTenantId(tenantId)
        tenantContext.set(sanitizedTenantId)

        // Also set a minimal TenantContext
        fullTenantContext.set(TenantContext(sanitizedTenantId))

        logger.debug("Set tenant context: {}", sanitizedTenantId)
    }

    /**
     * Sets the current full tenant context for the current thread.
     *
     * @param context The complete tenant context to set
     * @throws IllegalArgumentException if context is invalid
     */
    @JvmStatic
    fun setCurrentTenantContext(context: TenantContext) {
        context.validate()

        tenantContext.set(context.tenantId)
        fullTenantContext.set(context)

        logger.debug("Set full tenant context: {} with metadata", context.tenantId)
    }

    /**
     * Gets the current tenant ID from the context.
     *
     * @return The current tenant ID or null if no tenant is set
     */
    @JvmStatic fun getCurrentTenantId(): String? = tenantContext.get()

    /**
     * Gets the current full tenant context.
     *
     * @return The current TenantContext or null if no tenant is set
     */
    @JvmStatic fun getCurrentTenantContext(): TenantContext? = fullTenantContext.get()

    /**
     * Gets the current tenant ID, throwing an exception if not set.
     *
     * @return The current tenant ID
     * @throws TenantContextException if no tenant context is available
     */
    @JvmStatic
    fun requireCurrentTenantId(): String =
        getCurrentTenantId()
            ?: throw TenantContextException(
                "No tenant context available. Ensure tenant context is set before " +
                    "accessing tenant-scoped resources.",
            )

    /**
     * Clears the tenant context for the current thread. Should be called at the end of request
     * processing to prevent memory leaks.
     */
    @JvmStatic
    fun clear() {
        val currentTenant = getCurrentTenantId()
        tenantContext.remove()
        fullTenantContext.remove()

        if (currentTenant != null) {
            logger.debug("Cleared tenant context: {}", currentTenant)
        }
    }

    /**
     * Executes a block of code within a specific tenant context. Automatically cleans up the
     * context after execution, even if an exception occurs.
     *
     * @param tenantId The tenant identifier for the execution scope
     * @param block The code block to execute
     * @return The result of the block execution
     */
    @JvmStatic
    fun <T> executeInTenantContext(
        tenantId: String,
        block: () -> T,
    ): T = executeInTenantContext(TenantContext(tenantId), block)

    /**
     * Executes a block of code within a specific full tenant context. Automatically cleans up the
     * context after execution, even if an exception occurs.
     *
     * @param context The complete tenant context for the execution scope
     * @param block The code block to execute
     * @return The result of the block execution
     */
    @JvmStatic
    fun <T> executeInTenantContext(
        context: TenantContext,
        block: () -> T,
    ): T {
        val previousContext = getCurrentTenantContext()

        try {
            setCurrentTenantContext(context)
            logger.debug("Executing in full tenant context: {}", context.tenantId)
            return block()
        } finally {
            if (previousContext != null) {
                setCurrentTenantContext(previousContext)
                logger.debug("Restored previous tenant context: {}", previousContext.tenantId)
            } else {
                clear()
                logger.debug("Cleared tenant context after execution")
            }
        }
    }

    /**
     * Inherits tenant context for child threads/coroutines. This method should be called when
     * spawning new threads that need to inherit tenant context.
     *
     * @return A function that can be called in the child thread to set up inherited context
     */
    @JvmStatic
    fun inheritTenantContext(): () -> Unit {
        val contextToInherit = getCurrentTenantContext()

        return {
            if (contextToInherit != null) {
                setCurrentTenantContext(contextToInherit)
                logger.debug("Inherited tenant context: {}", contextToInherit.tenantId)
            }
        }
    }

    /**
     * Sanitizes tenant ID to prevent injection attacks and ensure consistency.
     *
     * @param tenantId The raw tenant ID
     * @return Sanitized tenant ID
     */
    private fun sanitizeTenantId(tenantId: String): String {
        // Remove any potentially dangerous characters and normalize
        return tenantId
            .trim()
            .replace(Regex("[^a-zA-Z0-9_-]"), "")
            .take(MAX_TENANT_ID_LENGTH) // Limit length to reasonable maximum
    }

    // Utility helpers added for backward compatibility with existing tests and components

    /** Checks if tenant context is currently set. */
    @JvmStatic fun hasTenantContext(): Boolean = getCurrentTenantId() != null

    /**
     * Validates that tenant context is present. Throws [TenantContextException] if absent.
     *
     * @param operation descriptive name of the calling operation for error context
     * @throws TenantContextException when no tenant context is available
     */
    @JvmStatic
    fun validateTenantContext(operation: String = "operation") {
        if (!hasTenantContext()) {
            throw TenantContextException(
                "Tenant context is required during $operation but was not found. " +
                    "Ensure tenant context is set before calling this operation.",
            )
        }
    }

    /** Returns a metadata value for the current tenant context, or null if not present. */
    @JvmStatic
    fun getTenantMetadata(key: String): String? = fullTenantContext.get()?.metadata?.get(key)

    /**
     * Adds or updates a metadata entry in the current tenant context.
     *
     * @throws TenantContextException if no tenant context is set
     */
    @JvmStatic
    fun addTenantMetadata(
        key: String,
        value: String,
    ) {
        val current =
            fullTenantContext.get()
                ?: throw TenantContextException(
                    "Cannot add metadata: no tenant context available",
                )
        fullTenantContext.set(current.withMetadata(key, value))
    }
}

/**
 * Coroutine context element for tenant context propagation. Ensures tenant context is preserved
 * across coroutine suspension points.
 */
class TenantCoroutineContext(
    val tenantId: String,
) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<TenantCoroutineContext>

    override val key: CoroutineContext.Key<*> = Key

    /** Executes a suspend block with tenant context propagation. */
    suspend fun <T> withTenantContext(block: suspend () -> T): T =
        withContext(this) {
            val previousTenantId = TenantContextHolder.getCurrentTenantId()
            TenantContextHolder.setCurrentTenantId(tenantId)
            try {
                block()
            } finally {
                // Restore previous context instead of clearing
                if (previousTenantId != null) {
                    TenantContextHolder.setCurrentTenantId(previousTenantId)
                } else {
                    TenantContextHolder.clear()
                }
            }
        }
}

/** Extension function to execute suspend code with tenant context. */
suspend fun <T> withTenantContext(
    tenantId: String,
    block: suspend () -> T,
): T {
    val tenantContext = TenantCoroutineContext(tenantId)
    return withContext(tenantContext) {
        val previousTenantId = TenantContextHolder.getCurrentTenantId()
        TenantContextHolder.setCurrentTenantId(tenantId)
        try {
            block()
        } finally {
            // Restore previous context instead of clearing completely
            if (previousTenantId != null) {
                TenantContextHolder.setCurrentTenantId(previousTenantId)
            } else {
                TenantContextHolder.clear()
            }
        }
    }
}

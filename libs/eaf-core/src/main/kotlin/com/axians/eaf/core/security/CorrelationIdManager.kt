package com.axians.eaf.core.security

import org.slf4j.MDC
import java.util.UUID

/**
 * Manages correlation IDs for distributed tracing across EAF services.
 *
 * This utility provides a consistent way to:
 * - Generate new correlation IDs
 * - Store and retrieve correlation IDs from thread-local storage
 * - Integrate with logging frameworks (SLF4J MDC)
 * - Propagate correlation IDs across async boundaries
 */
object CorrelationIdManager {
    private const val MDC_CORRELATION_ID_KEY = "correlationId"

    private val correlationIdThreadLocal = InheritableThreadLocal<String>()

    /**
     * Gets the current correlation ID from thread-local storage. If no correlation ID exists,
     * generates a new one and stores it.
     *
     * @return the current correlation ID
     */
    fun getCurrentCorrelationId(): String = correlationIdThreadLocal.get() ?: generateAndSetCorrelationId()

    /**
     * Gets the current correlation ID without generating a new one if it doesn't exist.
     *
     * @return the current correlation ID, or null if none exists
     */
    fun getCurrentCorrelationIdOrNull(): String? = correlationIdThreadLocal.get()

    /**
     * Sets the correlation ID for the current thread. Also updates the SLF4J MDC for logging
     * integration.
     *
     * @param correlationId the correlation ID to set
     */
    fun setCorrelationId(correlationId: String) {
        correlationIdThreadLocal.set(correlationId)
        MDC.put(MDC_CORRELATION_ID_KEY, correlationId)
    }

    /**
     * Generates a new correlation ID and sets it for the current thread.
     *
     * @return the newly generated correlation ID
     */
    fun generateAndSetCorrelationId(): String {
        val correlationId = generateCorrelationId()
        setCorrelationId(correlationId)
        return correlationId
    }

    /** Clears the correlation ID from the current thread. Also removes it from the SLF4J MDC. */
    fun clearCorrelationId() {
        correlationIdThreadLocal.remove()
        MDC.remove(MDC_CORRELATION_ID_KEY)
    }

    /**
     * Executes a block of code with a specific correlation ID. The correlation ID is automatically
     * restored after execution.
     *
     * @param correlationId the correlation ID to use during execution
     * @param block the code block to execute
     * @return the result of the block execution
     */
    fun <T> withCorrelationId(
        correlationId: String,
        block: () -> T,
    ): T {
        val previousCorrelationId = getCurrentCorrelationIdOrNull()
        return try {
            setCorrelationId(correlationId)
            block()
        } finally {
            if (previousCorrelationId != null) {
                setCorrelationId(previousCorrelationId)
            } else {
                clearCorrelationId()
            }
        }
    }

    /**
     * Executes a block of code with a new correlation ID. The correlation ID is automatically
     * restored after execution.
     *
     * @param block the code block to execute
     * @return the result of the block execution
     */
    fun <T> withNewCorrelationId(block: () -> T): T {
        val newCorrelationId = generateCorrelationId()
        return withCorrelationId(newCorrelationId, block)
    }

    /**
     * Generates a new UUID-based correlation ID.
     *
     * @return a new correlation ID
     */
    private fun generateCorrelationId(): String = UUID.randomUUID().toString()

    /**
     * Returns a [CorrelationIdElement] containing the current (or newly generated) correlation ID.
     * This is useful for easily attaching the ID to the root of a coroutine scope, e.g.
     *
     * ```kotlin
     * // ensure ID exists
     * CorrelationIdManager.generateAndSetCorrelationId()
     * runBlocking(CorrelationIdManager.asContextElement()) {
     *     // correlation id available in every coroutine
     * }
     * ```
     */
    fun asContextElement(): CorrelationIdElement = CorrelationIdElement(getCurrentCorrelationId())
}

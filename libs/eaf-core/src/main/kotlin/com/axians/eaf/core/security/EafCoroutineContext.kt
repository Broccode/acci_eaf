package com.axians.eaf.core.security

import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * A coroutine context element that propagates Spring Security context. This ensures that the
 * security context is available in child coroutines.
 */
class SecurityContextElement(
    private val securityContext: SecurityContext = SecurityContextHolder.getContext(),
) : ThreadContextElement<SecurityContext?> {
    companion object Key : CoroutineContext.Key<SecurityContextElement>

    override val key: CoroutineContext.Key<SecurityContextElement> = Key

    override fun updateThreadContext(context: CoroutineContext): SecurityContext? {
        val previousContext = SecurityContextHolder.getContext()
        SecurityContextHolder.setContext(securityContext)
        return previousContext
    }

    override fun restoreThreadContext(
        context: CoroutineContext,
        oldState: SecurityContext?,
    ) {
        if (oldState != null) {
            SecurityContextHolder.setContext(oldState)
        } else {
            SecurityContextHolder.clearContext()
        }
    }
}

/**
 * A coroutine context element that propagates correlation IDs. This ensures that correlation IDs
 * are available in child coroutines for distributed tracing.
 */
class CorrelationIdElement(
    private val correlationId: String? = CorrelationIdManager.getCurrentCorrelationIdOrNull(),
) : ThreadContextElement<String?> {
    companion object Key : CoroutineContext.Key<CorrelationIdElement>

    override val key: CoroutineContext.Key<CorrelationIdElement> = Key

    override fun updateThreadContext(context: CoroutineContext): String? {
        val previousCorrelationId = CorrelationIdManager.getCurrentCorrelationIdOrNull()
        if (correlationId != null) {
            CorrelationIdManager.setCorrelationId(correlationId)
        }
        return previousCorrelationId
    }

    override fun restoreThreadContext(
        context: CoroutineContext,
        oldState: String?,
    ) {
        if (oldState != null) {
            CorrelationIdManager.setCorrelationId(oldState)
        } else {
            CorrelationIdManager.clearCorrelationId()
        }
    }
}

/**
 * A combined coroutine context element that propagates both security context and correlation ID.
 * This is the recommended way to propagate EAF context in coroutines.
 */
class EafContextElement(
    private val securityContext: SecurityContext = SecurityContextHolder.getContext(),
    private val correlationId: String = CorrelationIdManager.getCurrentCorrelationId(),
) : ThreadContextElement<EafContextElement.State?> {
    data class State(
        val securityContext: SecurityContext?,
        val correlationId: String?,
    )

    companion object Key : CoroutineContext.Key<EafContextElement>

    override val key: CoroutineContext.Key<EafContextElement> = Key

    override fun updateThreadContext(context: CoroutineContext): State? {
        val previousSecurityContext = SecurityContextHolder.getContext()
        val previousCorrelationId = CorrelationIdManager.getCurrentCorrelationIdOrNull()

        SecurityContextHolder.setContext(securityContext)
        if (correlationId != null) {
            CorrelationIdManager.setCorrelationId(correlationId)
        }

        return State(previousSecurityContext, previousCorrelationId)
    }

    override fun restoreThreadContext(
        context: CoroutineContext,
        oldState: State?,
    ) {
        if (oldState?.securityContext != null) {
            SecurityContextHolder.setContext(oldState.securityContext)
        } else {
            SecurityContextHolder.clearContext()
        }

        if (oldState?.correlationId != null) {
            CorrelationIdManager.setCorrelationId(oldState.correlationId)
        } else {
            CorrelationIdManager.clearCorrelationId()
        }
    }
}

/** Extension function to get the current security context element from the coroutine context. */
fun CoroutineContext.securityContext(): SecurityContextElement =
    this[SecurityContextElement] ?: SecurityContextElement()

/** Extension function to get the current correlation ID element from the coroutine context. */
fun CoroutineContext.correlationIdContext(): CorrelationIdElement = this[CorrelationIdElement] ?: CorrelationIdElement()

/** Extension function to get the current EAF context element from the coroutine context. */
fun CoroutineContext.eafContext(): EafContextElement = this[EafContextElement] ?: EafContextElement()

/**
 * Creates a new SecurityContextElement with the current Spring Security context. This can be used
 * to manually propagate security context to coroutines.
 */
fun currentSecurityContextElement(): SecurityContextElement = SecurityContextElement(SecurityContextHolder.getContext())

/**
 * Creates a new EafContextElement with the current security context and correlation ID. This is the
 * recommended way to propagate EAF context to coroutines.
 */
fun currentEafContextElement(): EafContextElement =
    EafContextElement(
        SecurityContextHolder.getContext(),
        CorrelationIdManager.getCurrentCorrelationId(),
    )

/**
 * Executes the given block with the current EAF context (security context + correlation ID)
 * propagated to the coroutine. This is a convenience function that automatically includes both
 * security context and correlation ID.
 *
 * Example usage:
 * ```kotlin
 * withEafContext {
 *     // Security context and correlation ID are automatically available here
 *     val tenantId = eafSecurityContextHolder.getTenantId()
 *     val correlationId = CorrelationIdManager.getCurrentCorrelationId()
 *     // ... async operations
 * }
 * ```
 */
suspend fun <T> withEafContext(block: suspend () -> T): T {
    val eafElement = currentEafContextElement()
    return withContext(coroutineContext + eafElement) { block() }
}

/**
 * Similar to withEafContext but allows additional context elements to be added.
 *
 * Example usage:
 * ```kotlin
 * withEafContext(Dispatchers.IO) {
 *     // Security context + correlation ID + IO dispatcher
 *     val tenantId = eafSecurityContextHolder.getTenantId()
 *     val correlationId = CorrelationIdManager.getCurrentCorrelationId()
 *     // ... IO operations
 * }
 * ```
 */
suspend fun <T> withEafContext(
    context: CoroutineContext,
    block: suspend () -> T,
): T {
    val eafElement = currentEafContextElement()
    return withContext(coroutineContext + context + eafElement) { block() }
}

/**
 * Extension function to easily add security context to any CoroutineContext.
 *
 * Example usage:
 * ```kotlin
 * launch(Dispatchers.IO + securityContext()) {
 *     // Security context is propagated
 * }
 * ```
 */
fun CoroutineContext.withSecurityContext(): CoroutineContext = this + currentSecurityContextElement()

/**
 * Extension function to easily add EAF context (security + correlation ID) to any CoroutineContext.
 *
 * Example usage:
 * ```kotlin
 * launch(Dispatchers.IO + eafContext()) {
 *     // Security context and correlation ID are propagated
 * }
 * ```
 */
fun CoroutineContext.withEafContext(): CoroutineContext = this + currentEafContextElement()

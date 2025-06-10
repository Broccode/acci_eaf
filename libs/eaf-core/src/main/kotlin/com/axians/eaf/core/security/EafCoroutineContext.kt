package com.axians.eaf.core.security

import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * A coroutine context element that propagates Spring Security context.
 * This ensures that the security context is available in child coroutines.
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
 * Extension function to get the current security context element from the coroutine context.
 */
fun CoroutineContext.securityContext(): SecurityContextElement =
    this[SecurityContextElement] ?: SecurityContextElement()

/**
 * Creates a new SecurityContextElement with the current Spring Security context.
 * This can be used to manually propagate security context to coroutines.
 */
fun currentSecurityContextElement(): SecurityContextElement = SecurityContextElement(SecurityContextHolder.getContext())

/**
 * Executes the given block with the current security context propagated to the coroutine.
 * This is a convenience function that automatically includes the security context.
 *
 * Example usage:
 * ```kotlin
 * withEafContext {
 *     // Security context is automatically available here
 *     val tenantId = eafSecurityContextHolder.getTenantId()
 *     // ... async operations
 * }
 * ```
 */
suspend fun <T> withEafContext(block: suspend () -> T): T {
    val securityElement = currentSecurityContextElement()
    return withContext(coroutineContext + securityElement) {
        block()
    }
}

/**
 * Similar to withEafContext but allows additional context elements to be added.
 *
 * Example usage:
 * ```kotlin
 * withEafContext(Dispatchers.IO) {
 *     // Security context + IO dispatcher
 *     val tenantId = eafSecurityContextHolder.getTenantId()
 *     // ... IO operations
 * }
 * ```
 */
suspend fun <T> withEafContext(
    context: CoroutineContext,
    block: suspend () -> T,
): T {
    val securityElement = currentSecurityContextElement()
    return withContext(coroutineContext + context + securityElement) {
        block()
    }
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

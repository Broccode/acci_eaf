package com.axians.eaf.core.tenancy.integration

import com.axians.eaf.core.security.EafSecurityContextHolder
import com.axians.eaf.core.tenancy.TenantContextException
import com.axians.eaf.core.tenancy.TenantContextHolder
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Component

/**
 * Bridge component that automatically synchronizes tenant context from Spring Security to
 * TenantContextHolder.
 *
 * This component reads tenant information from EafSecurityContextHolder and ensures it's properly
 * synchronized with the ThreadLocal-based TenantContextHolder for seamless multi-tenant operations.
 */
@Component
class SecurityTenantContextBridge(
    private val securityContextHolder: EafSecurityContextHolder,
) {
    private val logger = LoggerFactory.getLogger(SecurityTenantContextBridge::class.java)

    /**
     * Synchronizes tenant context from Spring Security to TenantContextHolder.
     *
     * @param respectExisting If true, won't override existing tenant context in TenantContextHolder
     * @return true if synchronization was successful, false if no tenant context available
     */
    @Suppress("TooGenericExceptionCaught") // Ensure bridge failures do not crash the calling thread
    fun synchronizeTenantContext(respectExisting: Boolean = true): Boolean =
        try {
            performSynchronization(respectExisting)
        } catch (e: TenantContextException) {
            logger.warn("Tenant context error during synchronization: {}", e.message)
            false
        } catch (e: AuthenticationException) {
            logger.warn("Authentication error during tenant synchronization: {}", e.message)
            false
        } catch (e: AccessDeniedException) {
            logger.warn("Access denied during tenant synchronization: {}", e.message)
            false
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid tenant ID during synchronization: {}", e.message)
            false
        } catch (e: IllegalStateException) {
            logger.warn("Invalid state during tenant context synchronization: {}", e.message)
            false
        } catch (e: Exception) {
            logger.warn("Unexpected error during tenant context synchronization: {}", e.message)
            false
        }

    private fun performSynchronization(respectExisting: Boolean): Boolean {
        // Check if we should respect existing tenant context
        if (respectExisting && TenantContextHolder.getCurrentTenantId() != null) {
            val existingTenantId = TenantContextHolder.getCurrentTenantId()
            logger.debug(
                "Tenant context already exists: {}, skipping synchronization",
                existingTenantId,
            )
            return true
        }

        // Get tenant ID from Spring Security context
        val tenantId = securityContextHolder.getTenantIdOrNull()

        return if (tenantId != null) {
            TenantContextHolder.setCurrentTenantId(tenantId)
            logger.debug("Synchronized tenant context: source=security, tenant={}", tenantId)
            true
        } else {
            logger.debug("No tenant ID available in security context")
            false
        }
    }

    /**
     * Forces synchronization of tenant context, overriding any existing context.
     *
     * @return true if synchronization was successful, false if no tenant context available
     */
    fun forceSynchronizeTenantContext(): Boolean = synchronizeTenantContext(respectExisting = false)

    /**
     * Synchronizes tenant context with fallback to header-based extraction.
     *
     * @param fallbackTenantId Optional fallback tenant ID if security context is unavailable
     * @param respectExisting If true, won't override existing tenant context
     * @return true if synchronization was successful
     */
    fun synchronizeWithFallback(
        fallbackTenantId: String? = null,
        respectExisting: Boolean = true,
    ): Boolean {
        // Try primary synchronization first
        if (synchronizeTenantContext(respectExisting)) {
            return true
        }

        // Try fallback if provided and appropriate
        return tryFallbackSynchronization(fallbackTenantId, respectExisting)
    }

    private fun tryFallbackSynchronization(
        fallbackTenantId: String?,
        respectExisting: Boolean,
    ): Boolean {
        if (fallbackTenantId.isNullOrBlank()) {
            return false
        }

        return try {
            if (!respectExisting || TenantContextHolder.getCurrentTenantId() == null) {
                TenantContextHolder.setCurrentTenantId(fallbackTenantId)
                logger.debug(
                    "Synchronized tenant context: source=fallback, tenant={}",
                    fallbackTenantId,
                )
                true
            } else {
                logger.debug("Existing tenant context found, not using fallback")
                true
            }
        } catch (e: TenantContextException) {
            logger.warn("Tenant context error with fallback: {}", e.message)
            false
        } catch (e: IllegalArgumentException) {
            logger.warn("Failed to set tenant context from fallback: {}", e.message)
            false
        }
    }

    /**
     * Validates that tenant context is properly synchronized between security and tenant context.
     *
     * @throws TenantContextException if contexts are out of sync
     */
    fun validateSynchronization() {
        val securityTenantId = securityContextHolder.getTenantIdOrNull()
        val contextTenantId = TenantContextHolder.getCurrentTenantId()

        when {
            securityTenantId == null && contextTenantId == null -> {
                // Both are null - valid state for unauthenticated requests
                logger.debug(
                    "Both security and tenant contexts are empty - valid for unauthenticated requests",
                )
            }
            securityTenantId != null && contextTenantId == null -> {
                throw TenantContextException(
                    "Security context has tenant ID '$securityTenantId' but TenantContextHolder is empty. " +
                        "Ensure SecurityTenantContextBridge.synchronizeTenantContext() is called.",
                )
            }
            securityTenantId == null && contextTenantId != null -> {
                logger.warn(
                    "TenantContextHolder has tenant ID '{}' but security context is empty. " +
                        "This might indicate manual tenant context setup.",
                    contextTenantId,
                )
            }
            securityTenantId != contextTenantId -> {
                throw TenantContextException(
                    "Tenant context mismatch: Security context has '$securityTenantId' " +
                        "but TenantContextHolder has '$contextTenantId'. Contexts must be synchronized.",
                )
            }
            else -> {
                // Both have the same tenant ID - perfect sync
                logger.debug("Tenant contexts are properly synchronized: {}", securityTenantId)
            }
        }
    }

    /**
     * Gets the effective tenant ID from either security context or tenant context holder.
     *
     * @return The tenant ID if available from either source, null otherwise
     */
    fun getEffectiveTenantId(): String? =
        securityContextHolder.getTenantIdOrNull() ?: TenantContextHolder.getCurrentTenantId()

    /**
     * Checks if tenant context is available from either security context or tenant context holder.
     *
     * @return true if tenant context is available from either source
     */
    fun hasTenantContext(): Boolean =
        securityContextHolder.getTenantIdOrNull() != null ||
            TenantContextHolder.getCurrentTenantId() != null

    /**
     * Clears tenant context from TenantContextHolder if it matches the security context. This is
     * useful for cleanup while preserving manual tenant context setups.
     */
    fun clearIfSynchronized() {
        val securityTenantId = securityContextHolder.getTenantIdOrNull()
        val contextTenantId = TenantContextHolder.getCurrentTenantId()

        if (securityTenantId != null && securityTenantId == contextTenantId) {
            TenantContextHolder.clear()
            logger.debug("Cleared synchronized tenant context: {}", securityTenantId)
        } else if (contextTenantId != null) {
            logger.debug(
                "Not clearing tenant context: security='{}', context='{}'",
                securityTenantId,
                contextTenantId,
            )
        }
    }
}

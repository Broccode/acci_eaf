package com.axians.eaf.core.tenancy.integration

import com.axians.eaf.core.tenancy.TenantContextException
import com.axians.eaf.core.tenancy.TenantContextHolder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Support for @Scheduled methods with tenant context propagation.
 *
 * This component provides utilities for scheduled tasks that need to operate within specific tenant
 * contexts, particularly useful for batch processing and maintenance tasks.
 */
@Component
class TenantContextScheduledExecutor {
    private val logger = LoggerFactory.getLogger(TenantContextScheduledExecutor::class.java)

    /**
     * Executes a scheduled task with a specific tenant context.
     *
     * @param tenantId The tenant ID to execute the task under
     * @param task The task to execute
     * @param description Optional description for logging
     */
    fun executeWithTenantContext(
        tenantId: String,
        task: Runnable,
        description: String = "scheduled task",
    ) {
        try {
            TenantContextHolder.executeInTenantContext(tenantId) {
                logger.debug("Executing {} with tenant context: {}", description, tenantId)
                task.run()
                logger.debug("Completed {} for tenant: {}", description, tenantId)
            }
        } catch (e: TenantContextException) {
            logger.error(
                "Tenant context error executing {} for tenant {}: {}",
                description,
                tenantId,
                e.message,
                e,
            )
        } catch (e: IllegalArgumentException) {
            logger.error(
                "Invalid argument executing {} for tenant {}: {}",
                description,
                tenantId,
                e.message,
                e,
            )
        } catch (e: IllegalStateException) {
            logger.error(
                "Invalid state executing {} for tenant {}: {}",
                description,
                tenantId,
                e.message,
                e,
            )
        }
    }

    /**
     * Executes a scheduled task for all known tenants. Note: This requires a tenant registry or
     * discovery mechanism.
     *
     * @param task The task to execute per tenant
     * @param tenantIds List of tenant IDs to process
     * @param description Optional description for logging
     */
    fun executeForAllTenants(
        task: (String) -> Unit,
        tenantIds: List<String>,
        description: String = "multi-tenant scheduled task",
    ) {
        logger.info("Starting {} for {} tenants", description, tenantIds.size)

        tenantIds.forEach { tenantId ->
            try {
                TenantContextHolder.executeInTenantContext(tenantId) {
                    logger.debug("Executing {} for tenant: {}", description, tenantId)
                    task(tenantId)
                }
            } catch (e: TenantContextException) {
                logger.error(
                    "Tenant context error executing {} for tenant {}: {}",
                    description,
                    tenantId,
                    e.message,
                    e,
                )
            } catch (e: IllegalArgumentException) {
                logger.error(
                    "Invalid argument executing {} for tenant {}: {}",
                    description,
                    tenantId,
                    e.message,
                    e,
                )
            } catch (e: IllegalStateException) {
                logger.error(
                    "Invalid state executing {} for tenant {}: {}",
                    description,
                    tenantId,
                    e.message,
                    e,
                )
            }
        }

        logger.info("Completed {} for {} tenants", description, tenantIds.size)
    }

    /**
     * Creates a tenant-aware Runnable wrapper for scheduled methods.
     *
     * @param tenantId The tenant ID to execute under
     * @param task The original task
     * @return Wrapped task with tenant context
     */
    fun wrapWithTenantContext(
        tenantId: String,
        task: Runnable,
    ): Runnable = Runnable { executeWithTenantContext(tenantId, task) }
}

/**
 * Utility functions for manual tenant context propagation in complex scenarios.
 *
 * These utilities are particularly useful for Vaadin + Hilla applications where you need to manage
 * tenant context across different execution contexts.
 */
object TenantContextManualPropagation {
    private val logger = LoggerFactory.getLogger(TenantContextManualPropagation::class.java)

    /**
     * Captures the current tenant context for manual propagation to other threads.
     *
     * @return A function that can be called in another thread to restore the context
     */
    @JvmStatic
    fun captureTenantContext(): () -> Unit {
        val currentTenantId = TenantContextHolder.getCurrentTenantId()
        val currentContext = TenantContextHolder.getCurrentTenantContext()

        return {
            when {
                currentContext != null -> {
                    TenantContextHolder.setCurrentTenantContext(currentContext)
                    logger.debug("Restored full tenant context: {}", currentContext.tenantId)
                }
                currentTenantId != null -> {
                    TenantContextHolder.setCurrentTenantId(currentTenantId)
                    logger.debug("Restored tenant ID: {}", currentTenantId)
                }
                else -> {
                    logger.debug("No tenant context to restore")
                }
            }
        }
    }

    /**
     * Creates a tenant-aware wrapper for any generic task.
     *
     * @param task The task to wrap
     * @param tenantId Optional tenant ID (uses current context if null)
     * @return Wrapped task with tenant context propagation
     */
    @JvmStatic
    fun <T> wrapWithTenantContext(
        task: () -> T,
        tenantId: String? = null,
    ): () -> T {
        val contextTenantId = tenantId ?: TenantContextHolder.getCurrentTenantId()

        return {
            if (contextTenantId != null) {
                TenantContextHolder.executeInTenantContext(contextTenantId) {
                    logger.debug("Executing wrapped task with tenant context: {}", contextTenantId)
                    task()
                }
            } else {
                logger.debug("Executing wrapped task without tenant context")
                task()
            }
        }
    }

    /**
     * Validates tenant context consistency across multiple components.
     *
     * @param componentName Name of the component for logging
     * @param expectedTenantId Expected tenant ID
     * @throws IllegalStateException if context is inconsistent
     */
    @JvmStatic
    fun validateTenantContext(
        componentName: String,
        expectedTenantId: String?,
    ) {
        val currentTenantId = TenantContextHolder.getCurrentTenantId()

        when {
            expectedTenantId == null && currentTenantId == null -> {
                logger.debug(
                    "Component {} validated: no tenant context expected or present",
                    componentName,
                )
            }
            expectedTenantId != null && currentTenantId == expectedTenantId -> {
                logger.debug(
                    "Component {} validated: tenant context matches {}",
                    componentName,
                    expectedTenantId,
                )
            }
            expectedTenantId != currentTenantId -> {
                val message =
                    "Component $componentName tenant context mismatch: " +
                        "expected '$expectedTenantId', current '$currentTenantId'"
                logger.error(message)
                throw IllegalStateException(message)
            }
        }
    }

    /**
     * Creates a tenant-aware Runnable for background processing. Useful for Vaadin UI operations
     * that need to run with tenant context.
     *
     * @param runnable The runnable to wrap
     * @param tenantId Optional tenant ID (uses current context if null)
     * @return Wrapped runnable with tenant context propagation
     */
    @JvmStatic
    fun wrapRunnable(
        runnable: Runnable,
        tenantId: String? = null,
    ): Runnable {
        val contextTenantId = tenantId ?: TenantContextHolder.getCurrentTenantId()

        return Runnable {
            if (contextTenantId != null) {
                TenantContextHolder.executeInTenantContext(contextTenantId) {
                    logger.debug(
                        "Executing wrapped runnable with tenant context: {}",
                        contextTenantId,
                    )
                    runnable.run()
                }
            } else {
                logger.debug("Executing wrapped runnable without tenant context")
                runnable.run()
            }
        }
    }
}

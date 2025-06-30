package com.axians.eaf.core.tenancy.integration

import com.axians.eaf.core.tenancy.TenantContextHolder
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Function
import java.util.function.Supplier

/**
 * Utilities for CompletableFuture operations with automatic tenant context inheritance.
 *
 * These utilities ensure that tenant context from the calling thread is properly propagated to
 * CompletableFuture operations and their continuations.
 */
object TenantContextCompletableFuture {
    private val logger = LoggerFactory.getLogger(TenantContextCompletableFuture::class.java)

    /**
     * Creates a CompletableFuture that runs with the current tenant context.
     *
     * @param supplier The supplier to execute with tenant context
     * @param executor Optional executor for async execution
     * @return CompletableFuture with tenant context propagation
     */
    @JvmStatic
    fun <T> supplyWithTenantContext(
        supplier: Supplier<T>,
        executor: Executor? = null,
    ): CompletableFuture<T> {
        val currentTenantId = TenantContextHolder.getCurrentTenantId()

        val tenantAwareSupplier =
            Supplier {
                if (currentTenantId != null) {
                    TenantContextHolder.executeInTenantContext(currentTenantId) {
                        logger.debug(
                            "Executing CompletableFuture supplier with tenant context: {}",
                            currentTenantId,
                        )
                        supplier.get()
                    }
                } else {
                    logger.debug("Executing CompletableFuture supplier without tenant context")
                    supplier.get()
                }
            }

        return if (executor != null) {
            CompletableFuture.supplyAsync(tenantAwareSupplier, executor)
        } else {
            CompletableFuture.supplyAsync(tenantAwareSupplier)
        }
    }

    /**
     * Creates a CompletableFuture that runs a Runnable with the current tenant context.
     *
     * @param runnable The runnable to execute with tenant context
     * @param executor Optional executor for async execution
     * @return CompletableFuture with tenant context propagation
     */
    @JvmStatic
    fun runWithTenantContext(
        runnable: Runnable,
        executor: Executor? = null,
    ): CompletableFuture<Void> {
        val currentTenantId = TenantContextHolder.getCurrentTenantId()

        val tenantAwareRunnable =
            Runnable {
                if (currentTenantId != null) {
                    TenantContextHolder.executeInTenantContext(currentTenantId) {
                        logger.debug(
                            "Executing CompletableFuture runnable with tenant context: {}",
                            currentTenantId,
                        )
                        runnable.run()
                    }
                } else {
                    logger.debug("Executing CompletableFuture runnable without tenant context")
                    runnable.run()
                }
            }

        return if (executor != null) {
            CompletableFuture.runAsync(tenantAwareRunnable, executor)
        } else {
            CompletableFuture.runAsync(tenantAwareRunnable)
        }
    }

    /**
     * Creates a tenant-aware function for CompletableFuture.thenApply() operations.
     *
     * @param function The function to wrap with tenant context
     * @return Function that propagates tenant context
     */
    @JvmStatic
    fun <T, R> withTenantContext(function: Function<T, R>): Function<T, R> {
        val currentTenantId = TenantContextHolder.getCurrentTenantId()

        return Function { input ->
            if (currentTenantId != null) {
                TenantContextHolder.executeInTenantContext(currentTenantId) {
                    logger.debug(
                        "Executing CompletableFuture function with tenant context: {}",
                        currentTenantId,
                    )
                    function.apply(input)
                }
            } else {
                logger.debug("Executing CompletableFuture function without tenant context")
                function.apply(input)
            }
        }
    }

    /**
     * Creates a tenant-aware runnable for CompletableFuture.thenRun() operations.
     *
     * @param runnable The runnable to wrap with tenant context
     * @return Runnable that propagates tenant context
     */
    @JvmStatic
    fun withTenantContext(runnable: Runnable): Runnable {
        val currentTenantId = TenantContextHolder.getCurrentTenantId()

        return Runnable {
            if (currentTenantId != null) {
                TenantContextHolder.executeInTenantContext(currentTenantId) {
                    logger.debug(
                        "Executing CompletableFuture continuation with tenant context: {}",
                        currentTenantId,
                    )
                    runnable.run()
                }
            } else {
                logger.debug("Executing CompletableFuture continuation without tenant context")
                runnable.run()
            }
        }
    }
}

// Extension functions for CompletableFuture to provide fluent tenant context propagation

/** Extension function for CompletableFuture.thenApply with automatic tenant context propagation. */
fun <T, U> CompletableFuture<T>.thenApplyWithTenantContext(function: Function<in T, out U>): CompletableFuture<U> =
    this.thenApply(TenantContextCompletableFuture.withTenantContext(function))

/** Extension function for CompletableFuture.thenRun with automatic tenant context propagation. */
fun <T> CompletableFuture<T>.thenRunWithTenantContext(runnable: Runnable): CompletableFuture<Void> =
    this.thenRun(TenantContextCompletableFuture.withTenantContext(runnable))

/**
 * Extension function for CompletableFuture.thenCompose with automatic tenant context propagation.
 */
fun <T, U> CompletableFuture<T>.thenComposeWithTenantContext(
    function: Function<in T, out CompletableFuture<U>>,
): CompletableFuture<U> = this.thenCompose(TenantContextCompletableFuture.withTenantContext(function))

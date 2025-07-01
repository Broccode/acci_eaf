package com.axians.eaf.core.tenancy.integration

import com.axians.eaf.core.tenancy.TenantContextHolder
import org.slf4j.LoggerFactory
import org.springframework.core.task.TaskDecorator
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.Future

/**
 * Security-aware tenant executor that automatically propagates tenant context from Spring Security
 * to async operations.
 *
 * This executor ensures that tenant context from the calling thread (established via
 * SecurityTenantContextBridge) is properly inherited by async method executions.
 */
class SecurityAwareTenantExecutor(
    private val bridge: SecurityTenantContextBridge,
    private val delegate: Executor = createDefaultExecutor(),
) : Executor {
    private val logger = LoggerFactory.getLogger(SecurityAwareTenantExecutor::class.java)

    companion object {
        // Default thread pool configuration
        private const val DEFAULT_CORE_POOL_SIZE = 2
        private const val DEFAULT_MAX_POOL_SIZE = 10
        private const val DEFAULT_QUEUE_CAPACITY = 100

        private fun createDefaultExecutor(): ThreadPoolTaskExecutor =
            ThreadPoolTaskExecutor().apply {
                corePoolSize = DEFAULT_CORE_POOL_SIZE
                maxPoolSize = DEFAULT_MAX_POOL_SIZE
                queueCapacity = DEFAULT_QUEUE_CAPACITY
                setThreadNamePrefix("tenant-async-")
                initialize()
            }
    }

    override fun execute(task: Runnable) {
        val tenantAwareTask = wrapWithTenantContext(task)
        delegate.execute(tenantAwareTask)
    }

    /** Executes a callable with tenant context propagation. */
    fun <T> submit(task: Callable<T>): Future<T> {
        val tenantAwareTask = wrapWithTenantContext(task)
        return if (delegate is ThreadPoolTaskExecutor) {
            delegate.submit(tenantAwareTask)
        } else {
            throw UnsupportedOperationException(
                "Callable submission requires ThreadPoolTaskExecutor",
            )
        }
    }

    /** Wraps a Runnable with tenant context propagation. */
    private fun wrapWithTenantContext(task: Runnable): Runnable {
        // Capture current tenant context from either security context or TenantContextHolder
        val currentTenantId = bridge.getEffectiveTenantId()

        return Runnable {
            if (currentTenantId != null) {
                TenantContextHolder.executeInTenantContext(currentTenantId) {
                    logger.debug("Executing async task with tenant context: {}", currentTenantId)
                    try {
                        task.run()
                    } finally {
                        logger.debug("Completed async task for tenant: {}", currentTenantId)
                    }
                }
            } else {
                logger.debug("Executing async task without tenant context")
                task.run()
            }
        }
    }

    /** Wraps a Callable with tenant context propagation. */
    private fun <T> wrapWithTenantContext(task: Callable<T>): Callable<T> {
        // Capture current tenant context from either security context or TenantContextHolder
        val currentTenantId = bridge.getEffectiveTenantId()

        return Callable {
            if (currentTenantId != null) {
                TenantContextHolder.executeInTenantContext(currentTenantId) {
                    logger.debug(
                        "Executing async callable with tenant context: {}",
                        currentTenantId,
                    )
                    try {
                        task.call()
                    } finally {
                        logger.debug("Completed async callable for tenant: {}", currentTenantId)
                    }
                }
            } else {
                logger.debug("Executing async callable without tenant context")
                task.call()
            }
        }
    }
}

/**
 * TaskDecorator that automatically propagates tenant context for Spring's @Async methods.
 *
 * This decorator can be used with any TaskExecutor to enable automatic tenant context propagation
 * for async method executions.
 */
class TenantContextTaskDecorator(
    private val bridge: SecurityTenantContextBridge,
) : TaskDecorator {
    private val logger = LoggerFactory.getLogger(TenantContextTaskDecorator::class.java)

    override fun decorate(runnable: Runnable): Runnable {
        // Capture tenant context from the calling thread
        val currentTenantId = bridge.getEffectiveTenantId()

        return Runnable {
            if (currentTenantId != null) {
                TenantContextHolder.executeInTenantContext(currentTenantId) {
                    logger.debug(
                        "Executing decorated task with tenant context: {}",
                        currentTenantId,
                    )
                    try {
                        runnable.run()
                    } finally {
                        logger.debug("Completed decorated task for tenant: {}", currentTenantId)
                    }
                }
            } else {
                logger.debug("Executing decorated task without tenant context")
                runnable.run()
            }
        }
    }
}

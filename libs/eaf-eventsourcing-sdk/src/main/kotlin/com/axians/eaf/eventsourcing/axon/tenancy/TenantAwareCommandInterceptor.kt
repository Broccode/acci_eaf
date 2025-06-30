package com.axians.eaf.eventsourcing.axon.tenancy

import com.axians.eaf.core.tenancy.TenantContextHolder
import org.axonframework.commandhandling.CommandMessage
import org.axonframework.messaging.InterceptorChain
import org.axonframework.messaging.MessageHandlerInterceptor
import org.axonframework.messaging.unitofwork.UnitOfWork
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Tenant-aware command interceptor for Axon Framework integration.
 *
 * This interceptor automatically manages tenant context during command processing by:
 * - Extracting tenant ID from command metadata
 * - Setting tenant context before command handler execution
 * - Cleaning up tenant context after processing (success/failure)
 * - Handling commands gracefully when tenant context is missing
 */
@Component
class TenantAwareCommandInterceptor : MessageHandlerInterceptor<CommandMessage<*>> {
    companion object {
        private val logger = LoggerFactory.getLogger(TenantAwareCommandInterceptor::class.java)
        const val TENANT_ID_KEY = "tenant_id"
        const val TENANT_SOURCE_KEY = "tenant_source"
    }

    override fun handle(
        unitOfWork: UnitOfWork<out CommandMessage<*>>,
        interceptorChain: InterceptorChain,
    ): Any? {
        val message = unitOfWork.getMessage()
        val tenantId = extractTenantIdFromMetadata(message.metaData)

        return if (tenantId != null) {
            logger.debug("Processing command with tenant context: {}", tenantId)

            // Set tenant context and process command
            TenantContextHolder.executeInTenantContext(tenantId) {
                try {
                    val result = interceptorChain.proceed()
                    logger.debug(
                        "Command processing completed successfully for tenant: {}",
                        tenantId,
                    )
                    result
                } catch (exception: Exception) {
                    logger.warn(
                        "Command processing failed for tenant: {} - {}",
                        tenantId,
                        exception.message,
                    )
                    throw exception
                }
            }
        } else {
            // Handle command without tenant context
            handleCommandWithoutTenantContext(message, interceptorChain)
        }
    }

    /** Extracts tenant ID from command metadata. */
    private fun extractTenantIdFromMetadata(metaData: org.axonframework.messaging.MetaData): String? =
        metaData.get(TENANT_ID_KEY) as? String

    /** Handles commands that don't have tenant context. */
    private fun handleCommandWithoutTenantContext(
        message: CommandMessage<*>,
        interceptorChain: InterceptorChain,
    ): Any? {
        val commandType = message.payloadType.simpleName

        logger.debug("Processing command without tenant context: {}", commandType)

        return try {
            val result = interceptorChain.proceed()
            logger.debug("System command processing completed: {}", commandType)
            result
        } catch (exception: Exception) {
            logger.warn("System command processing failed: {} - {}", commandType, exception.message)
            throw exception
        }
    }
}

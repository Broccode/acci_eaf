package com.axians.eaf.eventsourcing.axon.tenancy

import com.axians.eaf.core.tenancy.TenantContextHolder
import org.axonframework.eventhandling.EventMessage
import org.axonframework.messaging.InterceptorChain
import org.axonframework.messaging.MessageHandlerInterceptor
import org.axonframework.messaging.unitofwork.UnitOfWork
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Tenant-aware event interceptor for Axon Framework integration.
 *
 * This interceptor automatically manages tenant context during event processing by:
 * - Extracting tenant ID from event metadata
 * - Setting tenant context before event handler execution
 * - Cleaning up tenant context after processing (success/failure)
 * - Supporting tenant context in saga processing
 * - Handling event replay scenarios with proper tenant isolation
 */
@Component
class TenantAwareEventInterceptor : MessageHandlerInterceptor<EventMessage<*>> {
    companion object {
        private val logger = LoggerFactory.getLogger(TenantAwareEventInterceptor::class.java)
        const val TENANT_ID_KEY = "tenant_id"
        const val TENANT_SOURCE_KEY = "tenant_source"
    }

    override fun handle(
        unitOfWork: UnitOfWork<out EventMessage<*>>,
        interceptorChain: InterceptorChain,
    ): Any? {
        val message = unitOfWork.getMessage()
        val tenantId = extractTenantIdFromMetadata(message.metaData)
        val isReplay = isEventReplay(message.metaData)

        return if (tenantId != null) {
            logger.debug(
                "Processing event with tenant context: {} (replay: {})",
                tenantId,
                isReplay,
            )

            // Set tenant context and process event
            TenantContextHolder.executeInTenantContext(tenantId) {
                try {
                    val result = interceptorChain.proceed()
                    logger.debug("Event processing completed successfully for tenant: {}", tenantId)
                    result
                } catch (exception: Exception) {
                    logger.warn(
                        "Event processing failed for tenant: {} - {}",
                        tenantId,
                        exception.message,
                    )
                    throw exception
                }
            }
        } else {
            // Handle event without tenant context
            handleEventWithoutTenantContext(message, interceptorChain, isReplay)
        }
    }

    /** Extracts tenant ID from event metadata. */
    private fun extractTenantIdFromMetadata(metaData: org.axonframework.messaging.MetaData): String? =
        metaData.get(TENANT_ID_KEY) as? String

    /** Checks if this is an event replay scenario. */
    private fun isEventReplay(metaData: org.axonframework.messaging.MetaData): Boolean =
        metaData.get("_isReplay") as? Boolean ?: false

    /** Handles events that don't have tenant context. */
    private fun handleEventWithoutTenantContext(
        message: EventMessage<*>,
        interceptorChain: InterceptorChain,
        isReplay: Boolean,
    ): Any? {
        val eventType = message.payloadType.simpleName

        logger.debug(
            "Processing event without tenant context: {} (replay: {})",
            eventType,
            isReplay,
        )

        return try {
            val result = interceptorChain.proceed()
            logger.debug("System event processing completed: {}", eventType)
            result
        } catch (exception: Exception) {
            logger.warn("System event processing failed: {} - {}", eventType, exception.message)
            throw exception
        }
    }
}

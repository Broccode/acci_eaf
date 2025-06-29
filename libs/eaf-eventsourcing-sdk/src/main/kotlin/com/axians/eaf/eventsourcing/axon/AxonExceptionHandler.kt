package com.axians.eaf.eventsourcing.axon

import com.axians.eaf.core.tenancy.TenantContextException
import com.axians.eaf.eventsourcing.axon.exception.EventSerializationException
import com.axians.eaf.eventsourcing.exception.OptimisticLockingFailureException
import org.axonframework.eventsourcing.eventstore.EventStoreException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import java.sql.SQLException

/**
 * Handles translation of EAF event store exceptions to appropriate Axon exceptions.
 *
 * This handler provides:
 * - Exception type translation for Axon compatibility
 * - Comprehensive logging with context information
 * - Consistent error handling patterns
 * - Tenant context preservation in error messages
 */
@Component
class AxonExceptionHandler {
    private val logger = LoggerFactory.getLogger(AxonExceptionHandler::class.java)

    /**
     * Handles exceptions that occur during event append operations.
     *
     * @param operation Description of the operation being performed
     * @param tenantId The tenant context for logging
     * @param aggregateId The aggregate identifier (if available)
     * @param eventCount Number of events being processed
     * @param exception The original exception
     * @return Appropriate Axon exception
     */
    fun handleAppendException(
        operation: String,
        tenantId: String,
        aggregateId: String? = null,
        eventCount: Int = 0,
        exception: Exception,
    ): RuntimeException {
        val context = buildErrorContext(operation, tenantId, aggregateId, eventCount)

        return when (exception) {
            is OptimisticLockingFailureException -> {
                logger.warn(
                    "Optimistic locking failure during {}: {} - {}",
                    operation,
                    context,
                    exception.message,
                )

                // Include detailed concurrency information
                val message =
                    "Concurrency conflict detected for aggregate ${aggregateId ?: "unknown"} " +
                        "in tenant $tenantId. Expected version: ${exception.expectedVersion}, " +
                        "actual version: ${exception.actualVersion}"

                EventStoreException(message, exception)
            }
            is SQLException -> {
                logger.error(
                    "Database error during {}: {} - SQL State: {}, Error Code: {}, Message: {}",
                    operation,
                    context,
                    exception.sqlState,
                    exception.errorCode,
                    exception.message,
                    exception,
                )

                val eventInfo = if (eventCount > 0) " $eventCount events" else ""
                val message =
                    "Database error during $operation in tenant $tenantId$eventInfo"
                EventStoreException(message, exception)
            }
            is DataAccessException -> {
                logger.error(
                    "Data access error during {}: {} - {}",
                    operation,
                    context,
                    exception.message,
                    exception,
                )

                val eventInfo = if (eventCount > 0) " $eventCount events" else ""
                val message =
                    "Data access error during $operation in tenant $tenantId$eventInfo"
                EventStoreException(message, exception)
            }
            is DataIntegrityViolationException -> {
                logger.error(
                    "Data integrity violation during {}: {} - {}",
                    operation,
                    context,
                    exception.message,
                    exception,
                )

                val eventInfo = if (eventCount > 0) " $eventCount events" else ""
                val message =
                    "Data integrity violation during $operation in tenant $tenantId$eventInfo"
                EventStoreException(message, exception)
            }
            is TenantContextException -> {
                logger.error(
                    "Tenant context error during {}: {} - {}",
                    operation,
                    context,
                    exception.message,
                    exception,
                )

                val eventInfo = if (eventCount > 0) " $eventCount events" else ""
                val message =
                    "Tenant context error during $operation in tenant $tenantId$eventInfo"
                EventStoreException(message, exception)
            }
            is EventSerializationException -> {
                logger.error(
                    "Event serialization error during {}: {} - {}",
                    operation,
                    context,
                    exception.message,
                    exception,
                )

                val eventInfo = if (eventCount > 0) " $eventCount events" else ""
                val message =
                    "Event serialization error during $operation in tenant $tenantId$eventInfo"
                EventStoreException(message, exception)
            }
            is IllegalArgumentException -> {
                logger.error(
                    "Invalid operation parameters during {}: {} - {}",
                    operation,
                    context,
                    exception.message,
                    exception,
                )

                val message =
                    "Invalid parameters for $operation in tenant $tenantId: ${exception.message}"
                EventStoreException(message, exception)
            }
            is IllegalStateException -> {
                logger.error(
                    "Invalid state during {}: {} - {}",
                    operation,
                    context,
                    exception.message,
                    exception,
                )

                val message =
                    "Invalid state for $operation in tenant $tenantId: ${exception.message}"
                EventStoreException(message, exception)
            }
            else -> {
                logger.error(
                    "Unexpected error during {}: {} - {}",
                    operation,
                    context,
                    exception.message,
                    exception,
                )

                val eventInfo = if (eventCount > 0) " $eventCount events" else ""
                val message =
                    "Unexpected error during $operation in tenant $tenantId$eventInfo"
                EventStoreException(message, exception)
            }
        }
    }

    /**
     * Handles exceptions that occur during event read operations.
     *
     * @param operation Description of the operation being performed
     * @param tenantId The tenant context for logging
     * @param aggregateId The aggregate identifier (if available)
     * @param sequenceInfo Additional sequence information for context
     * @param exception The original exception
     * @return Appropriate Axon exception
     */
    fun handleReadException(
        operation: String,
        tenantId: String,
        aggregateId: String? = null,
        sequenceInfo: String? = null,
        exception: Exception,
    ): RuntimeException {
        val context =
            buildErrorContext(
                operation,
                tenantId,
                aggregateId,
                sequenceInfo = sequenceInfo,
            )

        return when (exception) {
            is SQLException -> {
                logger.error(
                    "Database error during {}: {} - SQL State: {}, Error Code: {}, Message: {}",
                    operation,
                    context,
                    exception.sqlState,
                    exception.errorCode,
                    exception.message,
                    exception,
                )

                val aggregateContext = aggregateId?.let { ", aggregate=$it" } ?: ""
                val sequenceContext = sequenceInfo?.let { " sequence $it" } ?: ""
                val message =
                    "Database error during $operation in tenant $tenantId$aggregateContext$sequenceContext"
                EventStoreException(message, exception)
            }
            is DataAccessException -> {
                logger.error(
                    "Data access error during {}: {} - {}",
                    operation,
                    context,
                    exception.message,
                    exception,
                )

                val sequenceContext = sequenceInfo?.let { " sequence $it" } ?: ""
                val message =
                    "Data access error during $operation in tenant $tenantId$sequenceContext"
                EventStoreException(message, exception)
            }
            is TenantContextException -> {
                logger.error(
                    "Tenant context error during {}: {} - {}",
                    operation,
                    context,
                    exception.message,
                    exception,
                )

                val sequenceContext = sequenceInfo?.let { " sequence $it" } ?: ""
                val message =
                    "Tenant context error during $operation in tenant $tenantId$sequenceContext"
                EventStoreException(message, exception)
            }
            is EventSerializationException -> {
                logger.error(
                    "Event deserialization error during {}: {} - {}",
                    operation,
                    context,
                    exception.message,
                    exception,
                )

                val sequenceContext = sequenceInfo?.let { " sequence $it" } ?: ""
                val message =
                    "Event deserialization error during $operation in tenant $tenantId$sequenceContext"
                EventStoreException(message, exception)
            }
            else -> {
                logger.error(
                    "Unexpected error during {}: {} - {}",
                    operation,
                    context,
                    exception.message,
                    exception,
                )

                val sequenceContext = sequenceInfo?.let { " sequence $it" } ?: ""
                val message =
                    "Unexpected error during $operation in tenant $tenantId$sequenceContext"
                EventStoreException(message, exception)
            }
        }
    }

    /**
     * Handles exceptions that occur during snapshot operations.
     *
     * @param operation Description of the operation being performed
     * @param tenantId The tenant context for logging
     * @param aggregateId The aggregate identifier
     * @param exception The original exception
     * @return Appropriate Axon exception
     */
    fun handleSnapshotException(
        operation: String,
        tenantId: String,
        aggregateId: String,
        exception: Exception,
    ): RuntimeException {
        val context = buildErrorContext(operation, tenantId, aggregateId)

        return when (exception) {
            is SQLException -> {
                logger.error(
                    "Database error during {}: {} - SQL State: {}, Error Code: {}, Message: {}",
                    operation,
                    context,
                    exception.sqlState,
                    exception.errorCode,
                    exception.message,
                    exception,
                )

                val message = "Database error during $operation in tenant $tenantId"
                EventStoreException(message, exception)
            }
            is DataAccessException -> {
                logger.error(
                    "Data access error during {}: {} - {}",
                    operation,
                    context,
                    exception.message,
                    exception,
                )

                val message =
                    "Data access error during $operation in tenant $tenantId"
                EventStoreException(message, exception)
            }
            is EventSerializationException -> {
                logger.error(
                    "Snapshot serialization error during {}: {} - {}",
                    operation,
                    context,
                    exception.message,
                    exception,
                )

                val message =
                    "Event serialization error during $operation in tenant $tenantId"
                EventStoreException(message, exception)
            }
            else -> {
                logger.error(
                    "Unexpected error during {}: {} - {}",
                    operation,
                    context,
                    exception.message,
                    exception,
                )

                val message =
                    "Unexpected error during $operation in tenant $tenantId"
                EventStoreException(message, exception)
            }
        }
    }

    /**
     * Validates tenant context and throws appropriate exception if missing.
     *
     * @param tenantId The tenant identifier to validate
     * @param operation The operation being performed for error context
     * @throws EventStoreException if tenant context is invalid
     */
    fun validateTenantContext(
        tenantId: String?,
        operation: String,
    ) {
        if (tenantId.isNullOrBlank()) {
            logger.error("Missing tenant context for operation: {}", operation)
            throw EventStoreException(
                "No tenant context available for $operation. " +
                    "Ensure proper tenant context is established before performing event store operations.",
            )
        }
    }

    /**
     * Logs successful operations for audit purposes.
     *
     * @param operation Description of the operation
     * @param tenantId The tenant context
     * @param details Additional operation details
     */
    fun logSuccessfulOperation(
        operation: String,
        tenantId: String,
        details: String,
    ) {
        logger.info(
            "Successfully completed {} for tenant {}: {}",
            operation,
            tenantId,
            details,
        )
    }

    /** Builds consistent error context information for logging. */
    private fun buildErrorContext(
        operation: String,
        tenantId: String,
        aggregateId: String? = null,
        eventCount: Int? = null,
        sequenceInfo: String? = null,
    ): String {
        val context = mutableListOf<String>()
        context.add("tenant=$tenantId")

        aggregateId?.let { context.add("aggregate=$it") }
        eventCount?.let { context.add("eventCount=$it") }
        sequenceInfo?.let { context.add("sequence=$it") }

        return context.joinToString(", ")
    }
}

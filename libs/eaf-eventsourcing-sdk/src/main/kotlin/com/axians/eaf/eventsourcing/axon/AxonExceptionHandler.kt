package com.axians.eaf.eventsourcing.axon

import com.axians.eaf.core.tenancy.TenantContextException
import com.axians.eaf.eventsourcing.axon.exception.EventSerializationException
import com.axians.eaf.eventsourcing.exception.OptimisticLockingFailureException
import com.fasterxml.jackson.core.JsonProcessingException
import org.axonframework.eventsourcing.eventstore.EventStoreException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import java.io.IOException
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
        val context = ExceptionContext(operation, tenantId, aggregateId, eventCount)
        return mapExceptionToEventStoreException(context, exception)
    }

    private fun mapExceptionToEventStoreException(
        context: ExceptionContext,
        exception: Exception,
    ): EventStoreException =
        when (exception) {
            is OptimisticLockingFailureException ->
                EventStoreException(
                    "Concurrent modification detected: ${context.summary}",
                    exception,
                )
            is TenantContextException ->
                EventStoreException(
                    "Tenant context error: ${context.summary}",
                    exception,
                )
            is EventSerializationException ->
                EventStoreException("Serialization error: ${context.summary}", exception)
            is SQLException ->
                EventStoreException("Database error: ${context.summary}", exception)
            is DataIntegrityViolationException ->
                EventStoreException(
                    "Data integrity violation: ${context.summary}",
                    exception,
                )
            is DataAccessException ->
                EventStoreException("Data access error: ${context.summary}", exception)
            is JsonProcessingException ->
                EventStoreException("Serialization error: ${context.summary}", exception)
            is IOException -> EventStoreException("I/O error: ${context.summary}", exception)
            is InterruptedException ->
                EventStoreException("Operation interrupted: ${context.summary}", exception)
            else ->
                EventStoreException(
                    "Unexpected error during ${context.operation}: ${context.summary}",
                    exception,
                )
        }

    private data class ExceptionContext(
        val operation: String,
        val tenantId: String,
        val aggregateId: String?,
        val eventCount: Int?,
    ) {
        val summary: String
            get() =
                buildString {
                    append("operation=$operation, tenant=$tenantId")
                    if (aggregateId != null) append(", aggregate=$aggregateId")
                    if (eventCount != null) append(", events=$eventCount")
                }
    }

    /**
     * Handles exceptions that occur during event read operations.
     *
     * @param operation Description of the operation being performed
     * @param tenantId The tenant context for logging
     * @param aggregateId The aggregate identifier (if available)
     * @param exception The original exception
     * @return Appropriate Axon exception
     */
    fun handleReadException(
        operation: String,
        tenantId: String,
        aggregateId: String? = null,
        exception: Exception,
    ): RuntimeException {
        val context = ExceptionContext(operation, tenantId, aggregateId, null)
        return mapExceptionToEventStoreException(context, exception)
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
        val context = ExceptionContext(operation, tenantId, aggregateId, null)
        return mapExceptionToEventStoreException(context, exception)
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
}

package com.axians.eaf.controlplane.infrastructure.adapter.input.common

import com.vaadin.hilla.exception.EndpointException
import org.slf4j.Logger
import java.util.concurrent.TimeoutException

/**
 * Standardized exception handling utility for Hilla endpoints. Addresses detekt issues:
 * TooGenericExceptionCaught and SwallowedException.
 */
object EndpointExceptionHandler {
    /**
     * Executes a block with standardized exception handling for endpoint operations. Preserves
     * original exception details and provides specific exception handling.
     */
    inline fun <T> handleEndpointExecution(
        operationName: String,
        logger: Logger,
        crossinline operation: () -> T,
    ): T =
        try {
            operation()
        } catch (e: EndpointException) {
            // Re-throw EndpointExceptions as-is to preserve original error context
            throw e
        } catch (e: IllegalArgumentException) {
            val message = "$operationName failed: Invalid argument - ${e.message}"
            logger.warn(message, e)
            throw EndpointException(message, e)
        } catch (e: IllegalStateException) {
            val message = "$operationName failed: Invalid state - ${e.message}"
            logger.error(message, e)
            throw EndpointException(message, e)
        } catch (e: SecurityException) {
            val message = "$operationName failed: Security violation"
            logger.error(message, e)
            throw EndpointException(message, e)
        } catch (e: TimeoutException) {
            val message = "$operationName failed: Operation timeout"
            logger.error(message, e)
            throw EndpointException(message, e)
        } catch (e: RuntimeException) {
            val message = "$operationName failed: Runtime error - ${e.message}"
            logger.error(message, e)
            throw EndpointException(message, e)
        }

    /**
     * Executes a suspend block with standardized exception handling for async endpoint operations.
     */
    suspend inline fun <T> handleSuspendEndpointExecution(
        operationName: String,
        logger: Logger,
        crossinline operation: suspend () -> T,
    ): T =
        try {
            operation()
        } catch (e: EndpointException) {
            throw e
        } catch (e: IllegalArgumentException) {
            val message = "$operationName failed: Invalid argument - ${e.message}"
            logger.warn(message, e)
            throw EndpointException(message, e)
        } catch (e: IllegalStateException) {
            val message = "$operationName failed: Invalid state - ${e.message}"
            logger.error(message, e)
            throw EndpointException(message, e)
        } catch (e: SecurityException) {
            val message = "$operationName failed: Security violation"
            logger.error(message, e)
            throw EndpointException(message, e)
        } catch (e: TimeoutException) {
            val message = "$operationName failed: Operation timeout"
            logger.error(message, e)
            throw EndpointException(message, e)
        } catch (e: RuntimeException) {
            val message = "$operationName failed: Runtime error - ${e.message}"
            logger.error(message, e)
            throw EndpointException(message, e)
        }

    /** Handles health check operations with specific error handling for monitoring scenarios. */
    inline fun <T> handleHealthCheckExecution(
        checkName: String,
        logger: Logger,
        crossinline healthCheck: () -> T,
    ): HealthCheckResult<T> =
        try {
            HealthCheckResult.Success(healthCheck())
        } catch (e: IllegalStateException) {
            logger.warn("Health check '$checkName' failed due to invalid state: ${e.message}")
            HealthCheckResult.Failure(
                status = "DOWN",
                error = "Service unavailable: ${e.message}",
                exception = e,
            )
        } catch (e: TimeoutException) {
            logger.warn("Health check '$checkName' timed out: ${e.message}")
            HealthCheckResult.Failure(
                status = "DOWN",
                error = "Health check timeout",
                exception = e,
            )
        } catch (e: SecurityException) {
            logger.error("Health check '$checkName' failed due to security issue", e)
            HealthCheckResult.Failure(
                status = "DOWN",
                error = "Security configuration error",
                exception = e,
            )
        } catch (e: RuntimeException) {
            logger.error("Health check '$checkName' failed with runtime error", e)
            HealthCheckResult.Failure(
                status = "DOWN",
                error = "Health check failed: ${e.message}",
                exception = e,
            )
        }

    /** Result wrapper for health check operations that preserves exception context. */
    sealed class HealthCheckResult<T> {
        data class Success<T>(
            val data: T,
        ) : HealthCheckResult<T>()

        data class Failure<T>(
            val status: String,
            val error: String,
            val exception: Throwable,
        ) : HealthCheckResult<T>()
    }

    /**
     * Executes a block with standardized exception handling for endpoint operations. Preserves
     * original exception details and provides specific exception handling.
     */
    inline fun <T> handleEndpointOperation(
        logger: Logger,
        operation: String,
        details: String,
        block: () -> T,
    ): T? {
        logger.info("Starting operation: {} with details: {}", operation, details)

        return try {
            val result = block()
            logger.debug("Operation completed successfully: {}", operation)
            result
        } catch (ex: IllegalArgumentException) {
            logger.warn("Invalid argument for operation {}: {}", operation, ex.message)
            throw EndpointException("Invalid request: ${ex.message}", ex)
        } catch (ex: IllegalStateException) {
            logger.warn("Invalid state for operation {}: {}", operation, ex.message)
            throw EndpointException("Operation not allowed: ${ex.message}", ex)
        } catch (ex: TimeoutException) {
            logger.error("Timeout during operation {}: {}", operation, ex.message)
            throw EndpointException("Operation timed out", ex)
        } catch (ex: SecurityException) {
            logger.error("Security violation during operation {}: {}", operation, ex.message)
            throw EndpointException("Access denied", ex)
        } catch (ex: EndpointException) {
            // Re-throw existing endpoint exceptions without wrapping
            logger.warn("Endpoint exception during operation {}: {}", operation, ex.message)
            throw ex
        } catch (ex: RuntimeException) {
            logger.error("Unexpected error during operation {}: {}", operation, ex.message, ex)
            throw EndpointException("Internal server error", ex)
        } catch (ex: Exception) {
            logger.error(
                "Unexpected checked exception during operation {}: {}",
                operation,
                ex.message,
                ex,
            )
            throw EndpointException("Internal server error", ex)
        }
    }

    /** Executes a suspending block with standardized exception handling. */
    inline fun <T> handleSuspendingOperation(
        logger: Logger,
        operation: String,
        details: String,
        crossinline block: suspend () -> T,
    ): T? {
        logger.info("Starting suspending operation: {} with details: {}", operation, details)

        return try {
            val result = kotlinx.coroutines.runBlocking { block() }
            logger.debug("Suspending operation completed successfully: {}", operation)
            result
        } catch (ex: IllegalArgumentException) {
            logger.warn("Invalid argument for operation {}: {}", operation, ex.message)
            throw EndpointException("Invalid request: ${ex.message}", ex)
        } catch (ex: IllegalStateException) {
            logger.warn("Invalid state for operation {}: {}", operation, ex.message)
            throw EndpointException("Operation not allowed: ${ex.message}", ex)
        } catch (ex: TimeoutException) {
            logger.error("Timeout during operation {}: {}", operation, ex.message)
            throw EndpointException("Operation timed out", ex)
        } catch (ex: SecurityException) {
            logger.error("Security violation during operation {}: {}", operation, ex.message)
            throw EndpointException("Access denied", ex)
        } catch (ex: EndpointException) {
            // Re-throw existing endpoint exceptions without wrapping
            logger.warn("Endpoint exception during operation {}: {}", operation, ex.message)
            throw ex
        } catch (ex: RuntimeException) {
            logger.error("Unexpected error during operation {}: {}", operation, ex.message, ex)
            throw EndpointException("Internal server error", ex)
        } catch (ex: Exception) {
            logger.error(
                "Unexpected checked exception during operation {}: {}",
                operation,
                ex.message,
                ex,
            )
            throw EndpointException("Internal server error", ex)
        }
    }

    /**
     * Executes a block that returns a list with standardized exception handling. Returns an empty
     * list on failure instead of null.
     */
    inline fun <T> handleListOperation(
        logger: Logger,
        operation: String,
        details: String,
        block: () -> List<T>,
    ): List<T> = handleEndpointOperation(logger, operation, details, block) ?: emptyList()
}

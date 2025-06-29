package com.axians.eaf.eventsourcing.axon

import com.axians.eaf.core.tenancy.TenantContextException
import com.axians.eaf.eventsourcing.axon.exception.EventSerializationException
import com.axians.eaf.eventsourcing.exception.OptimisticLockingFailureException
import org.axonframework.eventsourcing.eventstore.EventStoreException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import java.sql.SQLException
import org.springframework.dao.OptimisticLockingFailureException as SpringOptimisticLockingFailureException

/**
 * Comprehensive tests for AxonExceptionHandler covering exception translation scenarios, proper
 * error handling, message preservation, and context information.
 *
 * Test Coverage:
 * - Append operation exception handling
 * - Read operation exception handling
 * - Snapshot operation exception handling
 * - Exception type translation (EAF → Axon)
 * - Message and cause preservation
 * - Context information preservation
 * - Various exception scenarios
 */
class AxonExceptionHandlerTest {
    private lateinit var exceptionHandler: AxonExceptionHandler

    // Test data
    private val testTenantId = "tenant-123"
    private val testAggregateId = "aggregate-456"
    private val testOperation = "test-operation"

    @BeforeEach
    fun setUp() {
        exceptionHandler = AxonExceptionHandler()
    }

    @Nested
    inner class AppendExceptionHandlingTests {
        @Test
        fun `handleAppendException should translate OptimisticLockingFailureException`() {
            // Given
            val originalCause = RuntimeException("Version conflict")
            val optimisticException =
                OptimisticLockingFailureException(
                    "Optimistic locking failed",
                    testTenantId,
                    testAggregateId,
                    5L,
                    6L,
                    originalCause,
                )

            // When
            val result =
                exceptionHandler.handleAppendException(
                    testOperation,
                    testTenantId,
                    testAggregateId,
                    1,
                    optimisticException,
                )

            // Then
            assertTrue(result is EventStoreException)
            assertTrue(result.message!!.contains("Concurrency conflict"))
            assertTrue(result.message!!.contains(testTenantId))
            assertEquals(optimisticException, result.cause)
        }

        @Test
        fun `handleAppendException should translate Spring OptimisticLockingFailureException`() {
            // Given
            val springOptimisticException =
                SpringOptimisticLockingFailureException("Spring optimistic locking failed")

            // When
            val result =
                exceptionHandler.handleAppendException(
                    testOperation,
                    testTenantId,
                    testAggregateId,
                    1,
                    springOptimisticException,
                )

            // Then
            assertTrue(result is EventStoreException)
            assertTrue(result.message!!.contains("Data access"))
            assertTrue(result.message!!.contains(testTenantId))
            assertEquals(springOptimisticException, result.cause)
        }

        @Test
        fun `handleAppendException should translate DataIntegrityViolationException`() {
            // Given
            val dataIntegrityException = DataIntegrityViolationException("Duplicate key constraint")

            // When
            val result =
                exceptionHandler.handleAppendException(
                    testOperation,
                    testTenantId,
                    testAggregateId,
                    2,
                    dataIntegrityException,
                )

            // Then
            assertTrue(result is EventStoreException)
            assertTrue(result.message!!.contains("Data access"))
            assertTrue(result.message!!.contains(testTenantId))
            assertTrue(result.message!!.contains("2 events"))
            assertEquals(dataIntegrityException, result.cause)
        }

        @Test
        fun `handleAppendException should translate SQLException`() {
            // Given
            val sqlException = SQLException("Database connection failed", "08001", 1234)

            // When
            val result =
                exceptionHandler.handleAppendException(
                    testOperation,
                    testTenantId,
                    testAggregateId,
                    1,
                    sqlException,
                )

            // Then
            assertTrue(result is EventStoreException)
            assertTrue(result.message!!.contains("Database error"))
            assertTrue(result.message!!.contains(testTenantId))
            assertEquals(sqlException, result.cause)
        }

        @Test
        fun `handleAppendException should translate DataAccessException`() {
            // Given
            val dataAccessException = object : DataAccessException("Generic data access error") {}

            // When
            val result =
                exceptionHandler.handleAppendException(
                    testOperation,
                    testTenantId,
                    testAggregateId,
                    1,
                    dataAccessException,
                )

            // Then
            assertTrue(result is EventStoreException)
            assertTrue(result.message!!.contains("Data access"))
            assertTrue(result.message!!.contains(testTenantId))
            assertEquals(dataAccessException, result.cause)
        }

        @Test
        fun `handleAppendException should translate TenantContextException`() {
            // Given
            val tenantContextException = TenantContextException("No tenant context available")

            // When
            val result =
                exceptionHandler.handleAppendException(
                    testOperation,
                    testTenantId,
                    testAggregateId,
                    1,
                    tenantContextException,
                )

            // Then
            assertTrue(result is EventStoreException)
            assertTrue(result.message!!.contains("Tenant context"))
            assertTrue(result.message!!.contains(testTenantId))
            assertEquals(tenantContextException, result.cause)
        }

        @Test
        fun `handleAppendException should translate EventSerializationException`() {
            // Given
            val serializationException = EventSerializationException("JSON serialization failed")

            // When
            val result =
                exceptionHandler.handleAppendException(
                    testOperation,
                    testTenantId,
                    testAggregateId,
                    1,
                    serializationException,
                )

            // Then
            assertTrue(result is EventStoreException)
            assertTrue(result.message!!.contains("Event serialization"))
            assertTrue(result.message!!.contains(testTenantId))
            assertEquals(serializationException, result.cause)
        }

        @Test
        fun `handleAppendException should handle unknown exceptions`() {
            // Given
            val unknownException = RuntimeException("Unknown error occurred")

            // When
            val result =
                exceptionHandler.handleAppendException(
                    testOperation,
                    testTenantId,
                    testAggregateId,
                    1,
                    unknownException,
                )

            // Then
            assertTrue(result is EventStoreException)
            assertTrue(result.message!!.contains("Unexpected error"))
            assertTrue(result.message!!.contains(testTenantId))
            assertEquals(unknownException, result.cause)
        }

        @Test
        fun `handleAppendException should include event count in message`() {
            // Given
            val exception = RuntimeException("Generic error")
            val eventCount = 5

            // When
            val result =
                exceptionHandler.handleAppendException(
                    testOperation,
                    testTenantId,
                    testAggregateId,
                    eventCount,
                    exception,
                )

            // Then
            assertTrue(result.message!!.contains("5 events"))
        }

        @Test
        fun `handleAppendException should handle null aggregate ID`() {
            // Given
            val exception = RuntimeException("Generic error")

            // When
            val result =
                exceptionHandler.handleAppendException(
                    testOperation,
                    testTenantId,
                    null,
                    1,
                    exception,
                )

            // Then
            assertNotNull(result.message)
            assertTrue(result.message!!.contains(testTenantId))
        }
    }

    @Nested
    inner class ReadExceptionHandlingTests {
        @Test
        fun `handleReadException should translate SQLException`() {
            // Given
            val sqlException = SQLException("Select query failed", "42000", 5678)

            // When
            val result =
                exceptionHandler.handleReadException(
                    testOperation,
                    testTenantId,
                    testAggregateId,
                    "100",
                    sqlException,
                )

            // Then
            assertTrue(result is EventStoreException)
            assertTrue(result.message!!.contains("Database error"))
            assertTrue(result.message!!.contains(testTenantId))
            assertTrue(result.message!!.contains(testAggregateId))
            assertEquals(sqlException, result.cause)
        }

        @Test
        fun `handleReadException should translate DataAccessException`() {
            // Given
            val dataAccessException = object : DataAccessException("Connection timeout") {}

            // When
            val result =
                exceptionHandler.handleReadException(
                    testOperation,
                    testTenantId,
                    testAggregateId,
                    "50",
                    dataAccessException,
                )

            // Then
            assertTrue(result is EventStoreException)
            assertTrue(result.message!!.contains("Data access"))
            assertTrue(result.message!!.contains(testTenantId))
            assertEquals(dataAccessException, result.cause)
        }

        @Test
        fun `handleReadException should translate TenantContextException`() {
            // Given
            val tenantContextException = TenantContextException("Invalid tenant context")

            // When
            val result =
                exceptionHandler.handleReadException(
                    testOperation,
                    testTenantId,
                    testAggregateId,
                    "1",
                    tenantContextException,
                )

            // Then
            assertTrue(result is EventStoreException)
            assertTrue(result.message!!.contains("Tenant context"))
            assertTrue(result.message!!.contains(testTenantId))
            assertEquals(tenantContextException, result.cause)
        }

        @Test
        fun `handleReadException should translate EventSerializationException`() {
            // Given
            val serializationException = EventSerializationException("JSON deserialization failed")

            // When
            val result =
                exceptionHandler.handleReadException(
                    testOperation,
                    testTenantId,
                    testAggregateId,
                    "10",
                    serializationException,
                )

            // Then
            assertTrue(result is EventStoreException)
            assertTrue(result.message!!.contains("Event deserialization"))
            assertTrue(result.message!!.contains(testTenantId))
            assertEquals(serializationException, result.cause)
        }

        @Test
        fun `handleReadException should handle unknown exceptions`() {
            // Given
            val unknownException = IllegalStateException("Unexpected state")

            // When
            val result =
                exceptionHandler.handleReadException(
                    testOperation,
                    testTenantId,
                    testAggregateId,
                    "1",
                    unknownException,
                )

            // Then
            assertTrue(result is EventStoreException)
            assertTrue(result.message!!.contains("Unexpected error"))
            assertTrue(result.message!!.contains(testTenantId))
            assertEquals(unknownException, result.cause)
        }

        @Test
        fun `handleReadException should handle null parameters`() {
            // Given
            val exception = RuntimeException("Generic error")

            // When
            val result =
                exceptionHandler.handleReadException(
                    testOperation,
                    testTenantId,
                    null,
                    null,
                    exception,
                )

            // Then
            assertNotNull(result.message)
            assertTrue(result.message!!.contains(testTenantId))
        }

        @Test
        fun `handleReadException should include sequence number in context`() {
            // Given
            val exception = RuntimeException("Generic error")
            val sequenceNumber = 42L

            // When
            val result =
                exceptionHandler.handleReadException(
                    testOperation,
                    testTenantId,
                    testAggregateId,
                    sequenceNumber.toString(),
                    exception,
                )

            // Then
            assertTrue(result.message!!.contains("sequence 42"))
        }
    }

    @Nested
    inner class SnapshotExceptionHandlingTests {
        @Test
        fun `handleSnapshotException should translate SQLException`() {
            // Given
            val sqlException = SQLException("Snapshot query failed")

            // When
            val result =
                exceptionHandler.handleSnapshotException(
                    "storeSnapshot",
                    testTenantId,
                    testAggregateId,
                    sqlException,
                )

            // Then
            assertTrue(result is EventStoreException)
            assertTrue(result.message!!.contains("Database error"))
            assertTrue(result.message!!.contains("storeSnapshot"))
            assertTrue(result.message!!.contains(testTenantId))
            assertEquals(sqlException, result.cause)
        }

        @Test
        fun `handleSnapshotException should translate DataAccessException`() {
            // Given
            val dataAccessException = object : DataAccessException("Snapshot save failed") {}

            // When
            val result =
                exceptionHandler.handleSnapshotException(
                    "readSnapshot",
                    testTenantId,
                    testAggregateId,
                    dataAccessException,
                )

            // Then
            assertTrue(result is EventStoreException)
            assertTrue(result.message!!.contains("Data access"))
            assertTrue(result.message!!.contains("readSnapshot"))
            assertEquals(dataAccessException, result.cause)
        }

        @Test
        fun `handleSnapshotException should translate EventSerializationException`() {
            // Given
            val serializationException =
                EventSerializationException("Snapshot serialization failed")

            // When
            val result =
                exceptionHandler.handleSnapshotException(
                    "storeSnapshot",
                    testTenantId,
                    testAggregateId,
                    serializationException,
                )

            // Then
            assertTrue(result is EventStoreException)
            assertTrue(result.message!!.contains("Event serialization"))
            assertEquals(serializationException, result.cause)
        }

        @Test
        fun `handleSnapshotException should handle unknown exceptions`() {
            // Given
            val unknownException = IllegalArgumentException("Invalid snapshot data")

            // When
            val result =
                exceptionHandler.handleSnapshotException(
                    "storeSnapshot",
                    testTenantId,
                    testAggregateId,
                    unknownException,
                )

            // Then
            assertTrue(result is EventStoreException)
            assertTrue(result.message!!.contains("Unexpected error"))
            assertEquals(unknownException, result.cause)
        }
    }

    @Nested
    inner class MessageAndCausePreservationTests {
        @Test
        fun `should preserve original exception cause chain`() {
            // Given
            val rootCause = SQLException("Root database error")
            val middlewareCause = object : DataAccessException("Middleware error", rootCause) {}
            val topException = RuntimeException("Top level error", middlewareCause)

            // When
            val result =
                exceptionHandler.handleAppendException(
                    testOperation,
                    testTenantId,
                    testAggregateId,
                    1,
                    topException,
                )

            // Then
            assertEquals(topException, result.cause)
            assertEquals(middlewareCause, result.cause?.cause)
            assertEquals(rootCause, result.cause?.cause?.cause)
        }

        @Test
        fun `should include operation context in error messages`() {
            // Given
            val operation = "appendEventsToStream"
            val exception = RuntimeException("Test error")

            // When
            val result =
                exceptionHandler.handleAppendException(
                    operation,
                    testTenantId,
                    testAggregateId,
                    1,
                    exception,
                )

            // Then
            assertTrue(result.message!!.contains(operation))
        }

        @Test
        fun `should include tenant context in all error messages`() {
            // Given
            val exception = RuntimeException("Test error")

            // When
            val appendResult =
                exceptionHandler.handleAppendException(
                    testOperation,
                    testTenantId,
                    testAggregateId,
                    1,
                    exception,
                )
            val readResult =
                exceptionHandler.handleReadException(
                    testOperation,
                    testTenantId,
                    testAggregateId,
                    "1",
                    exception,
                )
            val snapshotResult =
                exceptionHandler.handleSnapshotException(
                    testOperation,
                    testTenantId,
                    testAggregateId,
                    exception,
                )

            // Then
            assertTrue(appendResult.message!!.contains(testTenantId))
            assertTrue(readResult.message!!.contains(testTenantId))
            assertTrue(snapshotResult.message!!.contains(testTenantId))
        }

        @Test
        fun `should handle exceptions with null messages gracefully`() {
            // Given
            val exceptionWithNullMessage =
                object : RuntimeException() {
                    override val message: String? = null
                }

            // When
            val result =
                exceptionHandler.handleAppendException(
                    testOperation,
                    testTenantId,
                    testAggregateId,
                    1,
                    exceptionWithNullMessage,
                )

            // Then
            assertNotNull(result.message)
            assertTrue(result.message!!.contains("Unexpected error"))
        }
    }

    @Nested
    inner class ExceptionTypeValidationTests {
        @Test
        fun `should always return EventStoreException or its subclasses`() {
            // Given
            val exceptions =
                listOf(
                    RuntimeException("runtime"),
                    SQLException("sql"),
                    object : DataAccessException("data") {},
                    TenantContextException("tenant"),
                    EventSerializationException("serialization"),
                    OptimisticLockingFailureException(
                        "optimistic",
                        testTenantId,
                        testAggregateId,
                        1L,
                        2L,
                    ),
                )

            // When/Then
            exceptions.forEach { exception ->
                val appendResult =
                    exceptionHandler.handleAppendException(
                        testOperation,
                        testTenantId,
                        testAggregateId,
                        1,
                        exception,
                    )
                val readResult =
                    exceptionHandler.handleReadException(
                        testOperation,
                        testTenantId,
                        testAggregateId,
                        "1",
                        exception,
                    )
                val snapshotResult =
                    exceptionHandler.handleSnapshotException(
                        testOperation,
                        testTenantId,
                        testAggregateId,
                        exception,
                    )

                assertTrue(appendResult is EventStoreException)
                assertTrue(readResult is EventStoreException)
                assertTrue(snapshotResult is EventStoreException)
            }
        }
    }
}

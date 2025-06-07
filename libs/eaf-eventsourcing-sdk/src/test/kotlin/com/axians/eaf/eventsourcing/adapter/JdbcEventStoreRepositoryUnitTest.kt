package com.axians.eaf.eventsourcing.adapter

import com.axians.eaf.eventsourcing.exception.OptimisticLockingFailureException
import com.axians.eaf.eventsourcing.model.AggregateSnapshot
import com.axians.eaf.eventsourcing.model.PersistedEvent
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.Instant
import java.util.UUID

class JdbcEventStoreRepositoryUnitTest {
    private val jdbcTemplate = mockk<NamedParameterJdbcTemplate>()
    private lateinit var repository: JdbcEventStoreRepository

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        repository = JdbcEventStoreRepository(jdbcTemplate)
    }

    @Test
    fun `should validate events belong to same aggregate and tenant before append`() =
        runTest {
            // Given
            val tenantId = "tenant-1"
            val aggregateId = "aggregate-1"
            val differentAggregateEvent =
                createTestEvent(
                    aggregateId = "different-aggregate",
                    tenantId = tenantId,
                    sequenceNumber = 1,
                )
            val differentTenantEvent =
                createTestEvent(
                    aggregateId = aggregateId,
                    tenantId = "different-tenant",
                    sequenceNumber = 1,
                )

            // Mock getCurrentVersionInternal call
            every {
                jdbcTemplate.queryForObject(any<String>(), any<MapSqlParameterSource>(), Long::class.java)
            } returns null

            // When & Then - different aggregate
            val aggregateException =
                assertThrows(IllegalArgumentException::class.java) {
                    runBlocking {
                        repository.appendEvents(
                            events = listOf(differentAggregateEvent),
                            tenantId = tenantId,
                            aggregateId = aggregateId,
                            expectedVersion = null,
                        )
                    }
                }
            assertTrue(aggregateException.message!!.contains("same aggregate"))

            // When & Then - different tenant
            val tenantException =
                assertThrows(IllegalArgumentException::class.java) {
                    runBlocking {
                        repository.appendEvents(
                            events = listOf(differentTenantEvent),
                            tenantId = tenantId,
                            aggregateId = aggregateId,
                            expectedVersion = null,
                        )
                    }
                }
            assertTrue(tenantException.message!!.contains("same tenant"))
        }

    @Test
    fun `should throw exception when events list is empty`() =
        runTest {
            // When & Then
            val exception =
                assertThrows(IllegalArgumentException::class.java) {
                    runBlocking {
                        repository.appendEvents(
                            events = emptyList(),
                            tenantId = "tenant-1",
                            aggregateId = "aggregate-1",
                            expectedVersion = null,
                        )
                    }
                }
            assertTrue(exception.message!!.contains("cannot be empty"))
        }

    @Test
    fun `should handle optimistic locking failure from duplicate key exception`() =
        runTest {
            // Given
            val tenantId = "tenant-1"
            val aggregateId = "aggregate-1"
            val event = createTestEvent(aggregateId, tenantId, 1)

            // Mock successful version check initially
            every {
                jdbcTemplate.queryForObject(any<String>(), any<MapSqlParameterSource>(), Long::class.java)
            } returns null andThen 1L // First call returns null, second call (after exception) returns 1L

            // Mock DuplicateKeyException on insert
            every { jdbcTemplate.update(any<String>(), any<MapSqlParameterSource>()) } throws
                DuplicateKeyException("Duplicate key")

            // When & Then
            val exception =
                assertThrows(OptimisticLockingFailureException::class.java) {
                    runBlocking {
                        repository.appendEvents(
                            events = listOf(event),
                            tenantId = tenantId,
                            aggregateId = aggregateId,
                            expectedVersion = null,
                        )
                    }
                }

            assertEquals(tenantId, exception.tenantId)
            assertEquals(aggregateId, exception.aggregateId)
            assertEquals(null, exception.expectedVersion)
            assertEquals(1L, exception.actualVersion)
        }

    @Test
    fun `should validate snapshot belongs to correct tenant and aggregate`() =
        runTest {
            // Given
            val tenantId = "tenant-1"
            val aggregateId = "aggregate-1"
            val wrongTenantSnapshot =
                AggregateSnapshot(
                    aggregateId = aggregateId,
                    tenantId = "wrong-tenant",
                    aggregateType = "TestAggregate",
                    lastSequenceNumber = 5L,
                    snapshotPayloadJsonb = """{"state": "test"}""",
                    version = 1,
                )
            val wrongAggregateSnapshot =
                AggregateSnapshot(
                    aggregateId = "wrong-aggregate",
                    tenantId = tenantId,
                    aggregateType = "TestAggregate",
                    lastSequenceNumber = 5L,
                    snapshotPayloadJsonb = """{"state": "test"}""",
                    version = 1,
                )

            // When & Then - wrong tenant
            val tenantException =
                assertThrows(IllegalArgumentException::class.java) {
                    runBlocking {
                        repository.saveSnapshot(wrongTenantSnapshot, tenantId, aggregateId)
                    }
                }
            assertTrue(tenantException.message!!.contains("tenant ID must match"))

            // When & Then - wrong aggregate
            val aggregateException =
                assertThrows(IllegalArgumentException::class.java) {
                    runBlocking {
                        repository.saveSnapshot(wrongAggregateSnapshot, tenantId, aggregateId)
                    }
                }
            assertTrue(aggregateException.message!!.contains("aggregate ID must match"))
        }

    @Test
    fun `should return null when no snapshot exists`() =
        runTest {
            // Given
            val tenantId = "tenant-1"
            val aggregateId = "aggregate-1"

            every {
                jdbcTemplate.query(
                    any<String>(),
                    any<MapSqlParameterSource>(),
                    any<RowMapper<AggregateSnapshot>>(),
                )
            } returns emptyList()

            // When
            val result = repository.getSnapshot(tenantId, aggregateId)

            // Then
            assertNull(result)
            verify {
                jdbcTemplate.query(
                    any<String>(),
                    any<MapSqlParameterSource>(),
                    any<RowMapper<AggregateSnapshot>>(),
                )
            }
        }

    @Test
    fun `should return null when no events exist for getCurrentVersion`() =
        runTest {
            // Given
            val tenantId = "tenant-1"
            val aggregateId = "aggregate-1"

            every {
                jdbcTemplate.queryForObject(any<String>(), any<MapSqlParameterSource>(), Long::class.java)
            } returns null

            // When
            val result = repository.getCurrentVersion(tenantId, aggregateId)

            // Then
            assertNull(result)
            verify {
                jdbcTemplate.queryForObject(any<String>(), any<MapSqlParameterSource>(), Long::class.java)
            }
        }

    @Test
    fun `should delegate to getEvents when toSequenceNumber is null in getEventsInRange`() =
        runTest {
            // Given
            val tenantId = "tenant-1"
            val aggregateId = "aggregate-1"
            val fromSequenceNumber = 5L
            val mockEvents = listOf(createTestEvent(aggregateId, tenantId, fromSequenceNumber))

            every {
                jdbcTemplate.query(any<String>(), any<MapSqlParameterSource>(), any<RowMapper<PersistedEvent>>())
            } returns mockEvents

            // When
            val result =
                repository.getEventsInRange(
                    tenantId = tenantId,
                    aggregateId = aggregateId,
                    fromSequenceNumber = fromSequenceNumber,
                    toSequenceNumber = null,
                )

            // Then
            assertEquals(mockEvents, result)
            verify(exactly = 1) {
                jdbcTemplate.query(any<String>(), any<MapSqlParameterSource>(), any<RowMapper<PersistedEvent>>())
            }
        }

    private fun createTestEvent(
        aggregateId: String,
        tenantId: String,
        sequenceNumber: Long,
        aggregateType: String = "TestAggregate",
        eventType: String = "TestEvent",
    ): PersistedEvent =
        PersistedEvent(
            eventId = UUID.randomUUID(),
            streamId = PersistedEvent.createStreamId(aggregateType, aggregateId),
            aggregateId = aggregateId,
            aggregateType = aggregateType,
            expectedVersion = if (sequenceNumber == 1L) null else sequenceNumber - 1,
            sequenceNumber = sequenceNumber,
            tenantId = tenantId,
            eventType = eventType,
            payload = """{"data": "test-data-$sequenceNumber"}""",
            metadata = """{"timestamp": "${Instant.now()}"}""",
            timestampUtc = Instant.now(),
        )
}

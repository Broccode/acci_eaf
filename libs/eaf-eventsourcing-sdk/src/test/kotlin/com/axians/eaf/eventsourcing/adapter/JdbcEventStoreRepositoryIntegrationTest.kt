package com.axians.eaf.eventsourcing.adapter

import com.axians.eaf.eventsourcing.TestConfiguration
import com.axians.eaf.eventsourcing.exception.OptimisticLockingFailureException
import com.axians.eaf.eventsourcing.model.AggregateSnapshot
import com.axians.eaf.eventsourcing.model.PersistedEvent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.util.UUID

@Testcontainers
@SpringBootTest(classes = [com.axians.eaf.eventsourcing.TestApplication::class])
@Import(TestConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JdbcEventStoreRepositoryIntegrationTest {
    companion object {
        @Container
        @JvmStatic
        val postgresContainer: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:15-alpine")
                .withDatabaseName("eventstore_test")
                .withUsername("test")
                .withPassword("test")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("spring.flyway.locations") { "classpath:db/migration" }
        }
    }

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    private lateinit var repository: JdbcEventStoreRepository

    @BeforeEach
    fun setUp() {
        repository = JdbcEventStoreRepository(jdbcTemplate)
        // Clean up tables before each test
        jdbcTemplate.update("DELETE FROM domain_events", emptyMap<String, Any>())
        jdbcTemplate.update("DELETE FROM aggregate_snapshots", emptyMap<String, Any>())
    }

    @Test
    fun `should append single event successfully`() =
        runTest {
            // Given
            val tenantId = "tenant-1"
            val aggregateId = "aggregate-single-${System.nanoTime()}"
            val aggregateType = "TestAggregate"
            val event =
                createTestEvent(
                    aggregateId = aggregateId,
                    aggregateType = aggregateType,
                    tenantId = tenantId,
                    sequenceNumber = 1,
                    expectedVersion = null,
                )

            // When
            repository.appendEvents(
                events = listOf(event),
                tenantId = tenantId,
                aggregateId = aggregateId,
                expectedVersion = null,
            )

            // Then
            val retrievedEvents = repository.getEvents(tenantId, aggregateId)
            assertEquals(1, retrievedEvents.size)

            val retrievedEvent = retrievedEvents.first()
            assertEquals(event.eventId, retrievedEvent.eventId)
            assertEquals(event.aggregateId, retrievedEvent.aggregateId)
            assertEquals(event.tenantId, retrievedEvent.tenantId)
            assertEquals(event.sequenceNumber, retrievedEvent.sequenceNumber)
            assertEquals(event.eventType, retrievedEvent.eventType)
            assertEquals(event.payload, retrievedEvent.payload)
        }

    @Test
    fun `should append multiple events atomically`() =
        runTest {
            // Given
            val tenantId = "tenant-1"
            val aggregateId = "aggregate-multiple-${System.nanoTime()}"
            val aggregateType = "TestAggregate"
            val events =
                listOf(
                    createTestEvent(aggregateId, aggregateType, tenantId, 1, null),
                    createTestEvent(aggregateId, aggregateType, tenantId, 2, null),
                    createTestEvent(aggregateId, aggregateType, tenantId, 3, null),
                )

            // When
            repository.appendEvents(
                events = events,
                tenantId = tenantId,
                aggregateId = aggregateId,
                expectedVersion = null,
            )

            // Then
            val retrievedEvents = repository.getEvents(tenantId, aggregateId)
            assertEquals(3, retrievedEvents.size)

            // Verify events are in correct order
            retrievedEvents.forEachIndexed { index, event ->
                assertEquals((index + 1).toLong(), event.sequenceNumber)
            }
        }

    @Test
    fun `should enforce optimistic concurrency control`() =
        runTest {
            // Given
            val tenantId = "tenant-1"
            val aggregateId = "aggregate-concurrency-${System.nanoTime()}"
            val aggregateType = "TestAggregate"

            // First, append an initial event
            val initialEvent = createTestEvent(aggregateId, aggregateType, tenantId, 1, null)
            repository.appendEvents(
                events = listOf(initialEvent),
                tenantId = tenantId,
                aggregateId = aggregateId,
                expectedVersion = null,
            )

            // When & Then - try to append with wrong expected version
            val nextEvent = createTestEvent(aggregateId, aggregateType, tenantId, 2, 0L)

            val exception =
                assertThrows(OptimisticLockingFailureException::class.java) {
                    runBlocking {
                        repository.appendEvents(
                            events = listOf(nextEvent),
                            tenantId = tenantId,
                            aggregateId = aggregateId,
                            expectedVersion = 0L, // Wrong expected version, should be 1
                        )
                    }
                }

            assertEquals(tenantId, exception.tenantId)
            assertEquals(aggregateId, exception.aggregateId)
            assertEquals(0L, exception.expectedVersion)
            assertEquals(1L, exception.actualVersion)
        }

    @Test
    fun `should enforce tenant isolation for events`() =
        runTest {
            // Given
            val tenant1 = "tenant-1"
            val tenant2 = "tenant-2"
            val aggregateId = "aggregate-1"
            val aggregateType = "TestAggregate"

            val event1 = createTestEvent(aggregateId, aggregateType, tenant1, 1, null)
            val event2 = createTestEvent(aggregateId, aggregateType, tenant2, 1, null)

            // When
            repository.appendEvents(listOf(event1), tenant1, aggregateId, null)
            repository.appendEvents(listOf(event2), tenant2, aggregateId, null)

            // Then - each tenant should only see their own events
            val tenant1Events = repository.getEvents(tenant1, aggregateId)
            val tenant2Events = repository.getEvents(tenant2, aggregateId)

            assertEquals(1, tenant1Events.size)
            assertEquals(1, tenant2Events.size)
            assertEquals(tenant1, tenant1Events.first().tenantId)
            assertEquals(tenant2, tenant2Events.first().tenantId)
        }

    @Test
    fun `should retrieve events in correct order`() =
        runTest {
            // Given
            val tenantId = "tenant-1"
            val aggregateId = "aggregate-order-${System.nanoTime()}"
            val aggregateType = "TestAggregate"

            // Append events one by one
            val event1 = createTestEvent(aggregateId, aggregateType, tenantId, 1, null)
            val event2 = createTestEvent(aggregateId, aggregateType, tenantId, 2, null)
            val event3 = createTestEvent(aggregateId, aggregateType, tenantId, 3, null)

            repository.appendEvents(listOf(event1), tenantId, aggregateId, null)
            repository.appendEvents(listOf(event2), tenantId, aggregateId, 1L)
            repository.appendEvents(listOf(event3), tenantId, aggregateId, 2L)

            // When
            val retrievedEvents = repository.getEvents(tenantId, aggregateId)

            // Then - events should be ordered by sequence number
            assertEquals(3, retrievedEvents.size)
            assertEquals(1L, retrievedEvents[0].sequenceNumber)
            assertEquals(2L, retrievedEvents[1].sequenceNumber)
            assertEquals(3L, retrievedEvents[2].sequenceNumber)
        }

    @Test
    fun `should retrieve events from specific sequence number`() =
        runTest {
            // Given
            val tenantId = "tenant-1"
            val aggregateId = "aggregate-sequence-${System.nanoTime()}"
            val aggregateType = "TestAggregate"

            val events =
                (1..5).map { i ->
                    createTestEvent(
                        aggregateId,
                        aggregateType,
                        tenantId,
                        i.toLong(),
                        (i - 1).toLong().takeIf { it >= 0 },
                    )
                }

            events.forEachIndexed { index, event ->
                val expectedVersion = if (index == 0) null else (index).toLong()
                repository.appendEvents(listOf(event), tenantId, aggregateId, expectedVersion)
            }

            // When
            val retrievedEvents = repository.getEvents(tenantId, aggregateId, fromSequenceNumber = 3L)

            // Then
            assertEquals(3, retrievedEvents.size)
            assertEquals(3L, retrievedEvents[0].sequenceNumber)
            assertEquals(4L, retrievedEvents[1].sequenceNumber)
            assertEquals(5L, retrievedEvents[2].sequenceNumber)
        }

    @Test
    fun `should retrieve events in range`() =
        runTest {
            // Given
            val tenantId = "tenant-1"
            val aggregateId = "aggregate-range-${System.nanoTime()}"
            val aggregateType = "TestAggregate"

            val events =
                (1..5).map { i ->
                    createTestEvent(
                        aggregateId,
                        aggregateType,
                        tenantId,
                        i.toLong(),
                        (i - 1).toLong().takeIf { it >= 0 },
                    )
                }

            events.forEachIndexed { index, event ->
                val expectedVersion = if (index == 0) null else (index).toLong()
                repository.appendEvents(listOf(event), tenantId, aggregateId, expectedVersion)
            }

            // When
            val retrievedEvents =
                repository.getEventsInRange(
                    tenantId = tenantId,
                    aggregateId = aggregateId,
                    fromSequenceNumber = 2L,
                    toSequenceNumber = 4L,
                )

            // Then
            assertEquals(3, retrievedEvents.size)
            assertEquals(2L, retrievedEvents[0].sequenceNumber)
            assertEquals(3L, retrievedEvents[1].sequenceNumber)
            assertEquals(4L, retrievedEvents[2].sequenceNumber)
        }

    @Test
    fun `should get current version of aggregate`() =
        runTest {
            // Given
            val tenantId = "tenant-1"
            val aggregateId = "aggregate-version-${System.nanoTime()}"
            val aggregateType = "TestAggregate"

            // When - no events exist
            var currentVersion = repository.getCurrentVersion(tenantId, aggregateId)

            // Then
            assertNull(currentVersion)

            // When - append some events
            val events =
                (1..3).map { i ->
                    createTestEvent(
                        aggregateId,
                        aggregateType,
                        tenantId,
                        i.toLong(),
                        (i - 1).toLong().takeIf { it >= 0 },
                    )
                }

            events.forEachIndexed { index, event ->
                val expectedVersion = if (index == 0) null else (index).toLong()
                repository.appendEvents(listOf(event), tenantId, aggregateId, expectedVersion)
            }

            currentVersion = repository.getCurrentVersion(tenantId, aggregateId)

            // Then
            assertEquals(3L, currentVersion)
        }

    @Test
    fun `should save and retrieve snapshot`() =
        runTest {
            // Given
            val tenantId = "tenant-1"
            val aggregateId = "aggregate-snapshot-${System.nanoTime()}"
            val aggregateType = "TestAggregate"
            val snapshot =
                AggregateSnapshot(
                    aggregateId = aggregateId,
                    tenantId = tenantId,
                    aggregateType = aggregateType,
                    lastSequenceNumber = 5L,
                    snapshotPayloadJsonb = """{"state": "test-state", "version": 5}""",
                    version = 1,
                )

            // When
            repository.saveSnapshot(snapshot, tenantId, aggregateId)

            // Then
            val retrievedSnapshot = repository.getSnapshot(tenantId, aggregateId)
            assertNotNull(retrievedSnapshot)
            assertEquals(snapshot.aggregateId, retrievedSnapshot!!.aggregateId)
            assertEquals(snapshot.tenantId, retrievedSnapshot.tenantId)
            assertEquals(snapshot.lastSequenceNumber, retrievedSnapshot.lastSequenceNumber)
            assertEquals(snapshot.snapshotPayloadJsonb, retrievedSnapshot.snapshotPayloadJsonb)
        }

    @Test
    fun `should update existing snapshot`() =
        runTest {
            // Given
            val tenantId = "tenant-1"
            val aggregateId = "aggregate-update-snapshot-${System.nanoTime()}"
            val aggregateType = "TestAggregate"

            val initialSnapshot =
                AggregateSnapshot(
                    aggregateId = aggregateId,
                    tenantId = tenantId,
                    aggregateType = aggregateType,
                    lastSequenceNumber = 5L,
                    snapshotPayloadJsonb = """{"state": "initial", "version": 5}""",
                    version = 1,
                )

            val updatedSnapshot =
                AggregateSnapshot(
                    aggregateId = aggregateId,
                    tenantId = tenantId,
                    aggregateType = aggregateType,
                    lastSequenceNumber = 10L,
                    snapshotPayloadJsonb = """{"state": "updated", "version": 10}""",
                    version = 2,
                )

            // When
            repository.saveSnapshot(initialSnapshot, tenantId, aggregateId)
            repository.saveSnapshot(updatedSnapshot, tenantId, aggregateId)

            // Then
            val retrievedSnapshot = repository.getSnapshot(tenantId, aggregateId)
            assertNotNull(retrievedSnapshot)
            assertEquals(10L, retrievedSnapshot!!.lastSequenceNumber)
            assertEquals("""{"state": "updated", "version": 10}""", retrievedSnapshot.snapshotPayloadJsonb)
            assertEquals(2, retrievedSnapshot.version)
        }

    @Test
    fun `should enforce tenant isolation for snapshots`() =
        runTest {
            // Given
            val tenant1 = "tenant-1"
            val tenant2 = "tenant-2"
            val aggregateId = "aggregate-snapshot-isolation-${System.nanoTime()}"
            val aggregateType = "TestAggregate"

            val snapshot1 =
                AggregateSnapshot(
                    aggregateId = aggregateId,
                    tenantId = tenant1,
                    aggregateType = aggregateType,
                    lastSequenceNumber = 5L,
                    snapshotPayloadJsonb = """{"tenant": "1"}""",
                    version = 1,
                )

            val snapshot2 =
                AggregateSnapshot(
                    aggregateId = aggregateId,
                    tenantId = tenant2,
                    aggregateType = aggregateType,
                    lastSequenceNumber = 3L,
                    snapshotPayloadJsonb = """{"tenant": "2"}""",
                    version = 1,
                )

            // When
            repository.saveSnapshot(snapshot1, tenant1, aggregateId)
            repository.saveSnapshot(snapshot2, tenant2, aggregateId)

            // Then
            val retrievedSnapshot1 = repository.getSnapshot(tenant1, aggregateId)
            val retrievedSnapshot2 = repository.getSnapshot(tenant2, aggregateId)

            assertNotNull(retrievedSnapshot1)
            assertNotNull(retrievedSnapshot2)
            assertEquals(tenant1, retrievedSnapshot1!!.tenantId)
            assertEquals(tenant2, retrievedSnapshot2!!.tenantId)
            assertEquals("""{"tenant": "1"}""", retrievedSnapshot1.snapshotPayloadJsonb)
            assertEquals("""{"tenant": "2"}""", retrievedSnapshot2.snapshotPayloadJsonb)
        }

    private fun createTestEvent(
        aggregateId: String,
        aggregateType: String,
        tenantId: String,
        sequenceNumber: Long,
        expectedVersion: Long?,
        eventType: String = "TestEvent",
        payload: String = """{"data": "test-data-$sequenceNumber"}""",
    ): PersistedEvent =
        PersistedEvent(
            eventId = UUID.randomUUID(),
            streamId = PersistedEvent.createStreamId(aggregateType, aggregateId),
            aggregateId = aggregateId,
            aggregateType = aggregateType,
            expectedVersion = expectedVersion,
            sequenceNumber = sequenceNumber,
            tenantId = tenantId,
            eventType = eventType,
            payload = payload,
            metadata = """{"timestamp": "${Instant.now()}"}""",
            timestampUtc = Instant.now(),
        )
}

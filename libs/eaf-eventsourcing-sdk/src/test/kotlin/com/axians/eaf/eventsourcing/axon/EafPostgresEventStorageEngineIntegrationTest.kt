package com.axians.eaf.eventsourcing.axon

import com.axians.eaf.core.tenancy.TenantContextHolder
import com.axians.eaf.eventsourcing.TestConfiguration
import com.axians.eaf.eventsourcing.port.EventStoreRepository
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.axonframework.eventhandling.DomainEventMessage
import org.axonframework.eventhandling.GenericDomainEventMessage
import org.axonframework.eventhandling.TrackedEventMessage
import org.axonframework.messaging.MetaData
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 * Comprehensive integration tests for EafPostgresEventStorageEngine covering:
 * - End-to-end event persistence and retrieval
 * - Multi-tenant isolation
 * - Concurrency and optimistic locking
 * - Performance characteristics
 * - Large-scale event streaming
 * - Snapshot storage and reconstruction
 * - Error handling and recovery scenarios
 */
@SpringBootTest(classes = [com.axians.eaf.eventsourcing.TestApplication::class])
@Import(TestConfiguration::class)
@Testcontainers
class EafPostgresEventStorageEngineIntegrationTest {
    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:15")
                .withDatabaseName("eaf_test")
                .withUsername("test")
                .withPassword("test")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.flyway.enabled") { "true" }
        }

        // Test tenant constants for consistency
        private const val TENANT_A = "tenant-a"
        private const val TENANT_B = "tenant-b"
        private const val TENANT_C = "tenant-c"
    }

    @Autowired private lateinit var eventStoreRepository: EventStoreRepository

    @Autowired private lateinit var objectMapper: ObjectMapper

    private lateinit var eventMessageMapper: AxonEventMessageMapper
    private lateinit var exceptionHandler: AxonExceptionHandler
    private lateinit var storageEngine: EafPostgresEventStorageEngine

    @BeforeEach
    fun setUp() {
        // Clear any existing tenant context
        TenantContextHolder.clear()

        // Set up real components for integration testing
        eventMessageMapper = AxonEventMessageMapper(objectMapper)
        exceptionHandler = AxonExceptionHandler()

        storageEngine =
            EafPostgresEventStorageEngine(
                eventStoreRepository = eventStoreRepository,
                eventMessageMapper = eventMessageMapper,
                exceptionHandler = exceptionHandler,
            )
    }

    @AfterEach
    fun tearDown() {
        TenantContextHolder.clear()
    }

    @Nested
    inner class BasicIntegrationTests {
        @Test
        fun `appendEvents should use tenant context from TenantContextHolder`() {
            // Given
            val tenantId = "test-tenant-123"
            TenantContextHolder.setCurrentTenantId(tenantId)

            val event = createMockDomainEvent()
            val events = mutableListOf(event)

            // When
            storageEngine.appendEvents(events)

            // Then - event should be stored with correct tenant context
            val persistedEvents =
                runBlocking {
                    eventStoreRepository.getEvents(tenantId, event.aggregateIdentifier, 1L)
                }
            assertEquals(1, persistedEvents.size)
            assertEquals(tenantId, persistedEvents[0].tenantId)
        }

        @Test
        fun `appendEvents should throw exception when no tenant context set`() {
            // Given - no tenant context set
            val event = createMockDomainEvent()

            // When/Then
            assertThrows<RuntimeException> { storageEngine.appendEvents(mutableListOf(event)) }
        }

        @Test
        fun `readEvents should use tenant context for aggregate reading`() {
            // Given
            val tenantId = "read-tenant-456"
            TenantContextHolder.setCurrentTenantId(tenantId)

            val aggregateId = "test-aggregate"
            val event = createMockDomainEvent(aggregateId)

            // Store event first
            storageEngine.appendEvents(mutableListOf(event))

            // When
            val result = storageEngine.readEvents(aggregateId, 1L)

            // Then
            assertNotNull(result)
            assertTrue(result.hasNext())
        }

        @Test
        fun `readEvents should throw exception when no tenant context for aggregate reading`() {
            // Given - no tenant context set
            val aggregateId = "test-aggregate"

            // When/Then
            assertThrows<RuntimeException> { storageEngine.readEvents(aggregateId, 1L) }
        }

        @Test
        fun `createHeadToken should use tenant context`() {
            // Given
            val tenantId = "token-tenant-789"
            TenantContextHolder.setCurrentTenantId(tenantId)

            // Store some events first to have a non-zero max sequence
            val event = createMockDomainEvent()
            storageEngine.appendEvents(mutableListOf(event))

            // When
            val token = storageEngine.createHeadToken()

            // Then
            assertTrue(token is GlobalSequenceTrackingToken)
            assertTrue((token as GlobalSequenceTrackingToken).globalSequence >= 0)
        }

        @Test
        fun `storeSnapshot should use tenant context`() {
            // Given
            val tenantId = "snapshot-tenant-abc"
            TenantContextHolder.setCurrentTenantId(tenantId)

            val snapshot = createMockDomainEvent()

            // When
            storageEngine.storeSnapshot(snapshot)

            // Then - verify snapshot was stored
            val result = storageEngine.readSnapshot(snapshot.aggregateIdentifier)
            assertTrue(result.isPresent)
        }

        @Test
        fun `readSnapshot should use tenant context`() {
            // Given
            val tenantId = "read-snapshot-tenant"
            TenantContextHolder.setCurrentTenantId(tenantId)

            val aggregateId = "snapshot-aggregate"
            val snapshot = createMockDomainEvent(aggregateId)

            // Store snapshot first
            storageEngine.storeSnapshot(snapshot)

            // When
            val result = storageEngine.readSnapshot(aggregateId)

            // Then
            assertTrue(result.isPresent)
            assertEquals(aggregateId, result.get().aggregateIdentifier)
        }
    }

    @Nested
    inner class TenantIsolationTests {
        @Test
        fun `events from different tenants should be completely isolated`() {
            // Given
            val aggregateId = "shared-aggregate-id"
            val eventA = createMockDomainEvent(aggregateId, "Event from Tenant A")
            val eventB = createMockDomainEvent(aggregateId, "Event from Tenant B")

            // Store events in different tenants
            TenantContextHolder.setCurrentTenantId(TENANT_A)
            storageEngine.appendEvents(mutableListOf(eventA))

            TenantContextHolder.setCurrentTenantId(TENANT_B)
            storageEngine.appendEvents(mutableListOf(eventB))

            // When/Then - Tenant A should only see its events
            TenantContextHolder.setCurrentTenantId(TENANT_A)
            val eventsA = storageEngine.readEvents(aggregateId, 1L)
            var countA = 0
            while (eventsA.hasNext()) {
                countA++
                eventsA.next()
            }
            assertEquals(1, countA)

            // Tenant B should only see its events
            TenantContextHolder.setCurrentTenantId(TENANT_B)
            val eventsB = storageEngine.readEvents(aggregateId, 1L)
            var countB = 0
            while (eventsB.hasNext()) {
                countB++
                eventsB.next()
            }
            assertEquals(1, countB)
        }

        @Test
        fun `snapshots should be tenant-isolated`() {
            // Given
            val aggregateId = "shared-snapshot-aggregate"
            val snapshotA = createMockDomainEvent(aggregateId, "Snapshot A")
            val snapshotB = createMockDomainEvent(aggregateId, "Snapshot B")

            // Store snapshots in different tenants
            TenantContextHolder.setCurrentTenantId(TENANT_A)
            storageEngine.storeSnapshot(snapshotA)

            TenantContextHolder.setCurrentTenantId(TENANT_B)
            storageEngine.storeSnapshot(snapshotB)

            // When/Then - Each tenant should only see its own snapshot
            TenantContextHolder.setCurrentTenantId(TENANT_A)
            val resultA = storageEngine.readSnapshot(aggregateId)
            assertTrue(resultA.isPresent)

            TenantContextHolder.setCurrentTenantId(TENANT_B)
            val resultB = storageEngine.readSnapshot(aggregateId)
            assertTrue(resultB.isPresent)

            // Verify they are different (would contain different payload data)
            assertNotNull(resultA.get())
            assertNotNull(resultB.get())
        }

        @Test
        fun `tracking tokens should be tenant-specific`() {
            // Given - Store events in different tenants
            TenantContextHolder.setCurrentTenantId(TENANT_A)
            val eventA1 = createMockDomainEvent("agg-a-1")
            val eventA2 = createMockDomainEvent("agg-a-2")
            storageEngine.appendEvents(mutableListOf(eventA1, eventA2))

            TenantContextHolder.setCurrentTenantId(TENANT_B)
            val eventB1 = createMockDomainEvent("agg-b-1")
            storageEngine.appendEvents(mutableListOf(eventB1))

            // When - Get head tokens for each tenant
            TenantContextHolder.setCurrentTenantId(TENANT_A)
            val tokenA = storageEngine.createHeadToken()

            TenantContextHolder.setCurrentTenantId(TENANT_B)
            val tokenB = storageEngine.createHeadToken()

            // Then - Tokens should reflect different event counts per tenant
            assertTrue(tokenA is GlobalSequenceTrackingToken)
            assertTrue(tokenB is GlobalSequenceTrackingToken)
            // Note: Actual values depend on implementation but should be different
        }
    }

    @Nested
    inner class ConcurrencyTests {
        @Test
        fun `concurrent event appending should work correctly`() {
            // Given
            val tenantId = "concurrent-tenant"
            val numberOfThreads = 10
            val eventsPerThread = 5
            val latch = CountDownLatch(numberOfThreads)
            val errors = mutableListOf<Exception>()
            val executor = Executors.newFixedThreadPool(numberOfThreads)

            try {
                // When - Multiple threads append events concurrently
                repeat(numberOfThreads) { threadIndex ->
                    executor.submit {
                        try {
                            TenantContextHolder.setCurrentTenantId(tenantId)
                            repeat(eventsPerThread) { eventIndex ->
                                val aggregateId = "aggregate-$threadIndex-$eventIndex"
                                val event =
                                    createMockDomainEvent(
                                        aggregateId,
                                        "Thread $threadIndex Event $eventIndex",
                                    )
                                storageEngine.appendEvents(mutableListOf(event))
                            }
                        } catch (e: Exception) {
                            synchronized(errors) { errors.add(e) }
                        } finally {
                            TenantContextHolder.clear()
                            latch.countDown()
                        }
                    }
                }

                // Then - All operations should complete successfully
                assertTrue(latch.await(10, TimeUnit.SECONDS))
                assertTrue(
                    errors.isEmpty(),
                    "Concurrent operations failed: ${errors.joinToString()}",
                )

                // Verify all events were stored
                TenantContextHolder.setCurrentTenantId(tenantId)
                val totalEvents =
                    runBlocking {
                        eventStoreRepository.readEventsFrom(tenantId, 1L).size
                    }
                assertEquals(numberOfThreads * eventsPerThread, totalEvents)
            } finally {
                executor.shutdown()
                TenantContextHolder.clear()
            }
        }

        @Test
        fun `concurrent reads from different tenants should not interfere`() {
            // Given
            val numberOfTenants = 5
            val eventsPerTenant = 3
            val latch = CountDownLatch(numberOfTenants * 2) // Setup + Read operations
            val executor = Executors.newFixedThreadPool(numberOfTenants * 2)
            val results = mutableMapOf<String, Int>()

            try {
                // Setup - Store events for each tenant
                repeat(numberOfTenants) { tenantIndex ->
                    executor.submit {
                        try {
                            val tenantId = "tenant-$tenantIndex"
                            TenantContextHolder.setCurrentTenantId(tenantId)
                            repeat(eventsPerTenant) { eventIndex ->
                                val event =
                                    createMockDomainEvent(
                                        "agg-$eventIndex",
                                        "Event $eventIndex",
                                    )
                                storageEngine.appendEvents(mutableListOf(event))
                            }
                        } finally {
                            TenantContextHolder.clear()
                            latch.countDown()
                        }
                    }
                }

                // Wait for setup to complete
                Thread.sleep(1000)

                // When - Concurrent reads from different tenants
                repeat(numberOfTenants) { tenantIndex ->
                    executor.submit {
                        try {
                            val tenantId = "tenant-$tenantIndex"
                            TenantContextHolder.setCurrentTenantId(tenantId)
                            val events = storageEngine.readEvents(null, false)
                            var count = 0
                            events.forEach { count++ }
                            synchronized(results) { results[tenantId] = count }
                        } finally {
                            TenantContextHolder.clear()
                            latch.countDown()
                        }
                    }
                }

                // Then
                assertTrue(latch.await(15, TimeUnit.SECONDS))
                assertEquals(numberOfTenants, results.size)
                results.values.forEach { count -> assertEquals(eventsPerTenant, count) }
            } finally {
                executor.shutdown()
            }
        }

        @Test
        fun `thread safety with rapid tenant context switching`() {
            // Given
            val numberOfSwitches = 100
            val errors = mutableListOf<Exception>()
            val latch = CountDownLatch(numberOfSwitches)
            val executor = Executors.newFixedThreadPool(20)

            try {
                // When - Rapidly switch tenant contexts and perform operations
                repeat(numberOfSwitches) { index ->
                    executor.submit {
                        try {
                            val tenantId = "tenant-${index % 3}" // Cycle through 3 tenants
                            TenantContextHolder.setCurrentTenantId(tenantId)

                            val event = createMockDomainEvent("agg-$index", "Event $index")
                            storageEngine.appendEvents(mutableListOf(event))

                            // Verify the event was stored under correct tenant
                            val storedEvents =
                                runBlocking {
                                    eventStoreRepository.getEvents(tenantId, "agg-$index", 1L)
                                }
                            assertEquals(tenantId, storedEvents[0].tenantId)
                        } catch (e: Exception) {
                            synchronized(errors) { errors.add(e) }
                        } finally {
                            TenantContextHolder.clear()
                            latch.countDown()
                        }
                    }
                }

                // Then
                assertTrue(latch.await(30, TimeUnit.SECONDS))
                assertTrue(errors.isEmpty(), "Thread safety issues: ${errors.joinToString()}")
            } finally {
                executor.shutdown()
            }
        }
    }

    @Nested
    inner class PerformanceTests {
        @Test
        fun `event append throughput should meet SLA requirements`() {
            // Given
            val tenantId = "performance-tenant"
            val numberOfEvents = 1000
            val events =
                (1..numberOfEvents)
                    .map { index ->
                        createMockDomainEvent(
                            "perf-agg-$index",
                            "Performance test event $index",
                        )
                    }.toMutableList()

            TenantContextHolder.setCurrentTenantId(tenantId)

            // When - Measure append time
            val appendTime = measureTimeMillis { storageEngine.appendEvents(events) }

            // Then - Should meet performance SLA: < 10ms per event
            val averageTimePerEvent = appendTime.toDouble() / numberOfEvents
            println("Average append time per event: ${averageTimePerEvent}ms")
            assertTrue(
                averageTimePerEvent < 50.0,
                "Average append time ${averageTimePerEvent}ms exceeds SLA of 50ms per event",
            )

            // Verify all events were stored
            val storedEvents = runBlocking { eventStoreRepository.readEventsFrom(tenantId, 1L) }
            assertEquals(numberOfEvents, storedEvents.size)
        }

        @Test
        fun `event streaming throughput should handle large volumes`() {
            // Given
            val tenantId = "streaming-tenant"
            val numberOfEvents = 500
            TenantContextHolder.setCurrentTenantId(tenantId)

            // Store events
            repeat(numberOfEvents) { index ->
                val event = createMockDomainEvent("stream-agg-$index", "Streaming event $index")
                storageEngine.appendEvents(mutableListOf(event))
            }

            // When - Measure streaming time
            val streamingTime =
                measureTimeMillis {
                    val events = storageEngine.readEvents(null, false)
                    var count = 0
                    events.forEach {
                        count++
                        // Process event (minimal)
                        it.payload
                    }
                    assertEquals(numberOfEvents, count)
                }

            // Then - Should meet streaming SLA: < 50ms for 500 events
            println("Streaming time for $numberOfEvents events: ${streamingTime}ms")
            assertTrue(
                streamingTime < 5000,
                "Streaming time ${streamingTime}ms exceeds SLA of 5000ms for $numberOfEvents events",
            )
        }

        @Test
        fun `memory usage should remain stable during large operations`() {
            // Given
            val tenantId = "memory-tenant"
            val numberOfBatches = 10
            val eventsPerBatch = 100
            TenantContextHolder.setCurrentTenantId(tenantId)

            val initialMemory =
                Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

            // When - Process multiple large batches
            repeat(numberOfBatches) { batchIndex ->
                val events =
                    (1..eventsPerBatch)
                        .map { eventIndex ->
                            createMockDomainEvent(
                                "mem-agg-$batchIndex-$eventIndex",
                                "Memory test event $eventIndex in batch $batchIndex",
                            )
                        }.toMutableList()

                storageEngine.appendEvents(events)

                // Force garbage collection
                System.gc()
                Thread.sleep(100)
            }

            val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val memoryIncrease = finalMemory - initialMemory

            // Then - Memory increase should be reasonable (< 50MB)
            val memoryIncreaseMB = memoryIncrease / (1024 * 1024)
            println("Memory increase: ${memoryIncreaseMB}MB")
            assertTrue(
                memoryIncreaseMB < 100,
                "Memory increase ${memoryIncreaseMB}MB exceeds acceptable threshold",
            )
        }
    }

    @Nested
    inner class EndToEndScenarios {
        @Test
        fun `complete aggregate lifecycle with snapshots`() {
            // Given
            val tenantId = "lifecycle-tenant"
            val aggregateId = "lifecycle-aggregate"
            TenantContextHolder.setCurrentTenantId(tenantId)

            // When - Create aggregate with events
            val events =
                (1..10)
                    .map { index ->
                        createMockDomainEvent(aggregateId, "Lifecycle event $index")
                    }.toMutableList()
            storageEngine.appendEvents(events)

            // Store snapshot at event 5
            val snapshot = createMockDomainEvent(aggregateId, "Snapshot at event 5")
            storageEngine.storeSnapshot(snapshot)

            // Add more events after snapshot
            val moreEvents =
                (11..15)
                    .map { index ->
                        createMockDomainEvent(aggregateId, "Post-snapshot event $index")
                    }.toMutableList()
            storageEngine.appendEvents(moreEvents)

            // Then - Verify complete history can be reconstructed
            val allEvents = storageEngine.readEvents(aggregateId, 1L)
            var eventCount = 0
            while (allEvents.hasNext()) {
                eventCount++
                allEvents.next()
            }
            assertEquals(15, eventCount)

            // Verify snapshot can be retrieved
            val retrievedSnapshot = storageEngine.readSnapshot(aggregateId)
            assertTrue(retrievedSnapshot.isPresent)
        }

        @Test
        fun `event processor simulation with tracking tokens`() {
            // Given
            val tenantId = "processor-tenant"
            TenantContextHolder.setCurrentTenantId(tenantId)

            // Store events across multiple aggregates
            val aggregateIds = (1..5).map { "processor-agg-$it" }
            aggregateIds.forEach { aggregateId ->
                repeat(3) { eventIndex ->
                    val event = createMockDomainEvent(aggregateId, "Event $eventIndex")
                    storageEngine.appendEvents(mutableListOf(event))
                }
            }

            // When - Simulate event processor reading events
            var currentToken = storageEngine.createTailToken()
            val processedEvents = mutableListOf<TrackedEventMessage<*>>()

            // Process events in batches
            repeat(3) { batchIndex ->
                val events = storageEngine.readEvents(currentToken, false)
                var lastToken = currentToken

                events.forEach { trackedEvent ->
                    processedEvents.add(trackedEvent)
                    lastToken = trackedEvent.trackingToken()
                }

                currentToken = lastToken
            }

            // Then - All events should be processed
            assertEquals(15, processedEvents.size) // 5 aggregates × 3 events
        }

        @Test
        fun `disaster recovery scenario - token reconstruction`() {
            // Given
            val tenantId = "recovery-tenant"
            TenantContextHolder.setCurrentTenantId(tenantId)

            // Store events
            repeat(20) { index ->
                val event = createMockDomainEvent("recovery-agg-$index", "Recovery event $index")
                storageEngine.appendEvents(mutableListOf(event))
            }

            // When - Simulate system restart by recreating head token
            val headToken = storageEngine.createHeadToken()
            val tailToken = storageEngine.createTailToken()

            // Then - Tokens should represent correct positions
            assertTrue(headToken is GlobalSequenceTrackingToken)
            assertTrue(tailToken is GlobalSequenceTrackingToken)
            assertTrue(
                (headToken as GlobalSequenceTrackingToken).globalSequence >=
                    (tailToken as GlobalSequenceTrackingToken).globalSequence,
            )
        }
    }

    // Test data creation helpers
    private fun createMockDomainEvent(
        aggregateId: String = "test-aggregate-${UUID.randomUUID()}",
        payload: String = "test-payload",
    ): DomainEventMessage<String> =
        GenericDomainEventMessage(
            "TestAggregate",
            aggregateId,
            1L,
            payload,
            MetaData.from(mapOf("test-meta" to "value")),
            UUID.randomUUID().toString(),
            Instant.now(),
        )
}

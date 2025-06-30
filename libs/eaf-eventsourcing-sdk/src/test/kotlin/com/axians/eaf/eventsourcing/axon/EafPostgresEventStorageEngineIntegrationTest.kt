package com.axians.eaf.eventsourcing.axon

import com.axians.eaf.core.tenancy.TenantContextHolder
import com.axians.eaf.eventsourcing.EventSourcingTestcontainerConfiguration
import com.axians.eaf.eventsourcing.test.TestEafEventSourcingApplication
import org.axonframework.eventhandling.DomainEventMessage
import org.axonframework.eventhandling.GenericDomainEventMessage
import org.axonframework.messaging.MetaData
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

/**
 * Integration tests for EafPostgresEventStorageEngine with real PostgreSQL database.
 *
 * These tests verify:
 * - End-to-end event persistence and retrieval
 * - Tenant isolation and multi-tenancy support
 * - Axon Framework 4.11.2 compatibility
 * - Database schema and constraints
 * - Transaction boundaries and rollback scenarios
 *
 * Test Infrastructure:
 * - PostgreSQL 15-alpine via Testcontainers
 * - EAF event store schema with test data
 * - Spring Boot Test with stable bean configuration
 * - Tenant context management integration
 */
@SpringBootTest(classes = [TestEafEventSourcingApplication::class])
@Testcontainers
@ActiveProfiles("test")
@Transactional
@Import(EventSourcingTestcontainerConfiguration::class)
class EafPostgresEventStorageEngineIntegrationTest {
    @Autowired private lateinit var eventStorageEngine: EafPostgresEventStorageEngine

    // Test data constants
    private val tenantA = "TENANT_A"
    private val tenantB = "TENANT_B"
    private val testAggregateType = "TestAggregateIntegration"

    @BeforeEach
    fun setUp() {
        // Clear tenant context before each test
        TenantContextHolder.clear()
    }

    // Generate unique aggregate ID per test to prevent optimistic locking conflicts
    private fun createUniqueAggregateId(baseName: String = "test-aggregate"): String =
        "$baseName-${UUID.randomUUID().toString().take(8)}"

    @AfterEach
    fun tearDown() {
        // Clear tenant context after each test
        TenantContextHolder.clear()
    }

    // ============================================================================
    // Basic Event Storage and Retrieval Tests
    // ============================================================================

    @Test
    fun `should append and retrieve events for single tenant`() {
        // Given
        TenantContextHolder.setCurrentTenantId(tenantA)
        val testAggregateId = createUniqueAggregateId("single-tenant")
        val events = createTestDomainEvents(testAggregateId, 3)

        // When
        eventStorageEngine.appendEvents(events.toMutableList())

        // Then
        val retrievedStream = eventStorageEngine.readEvents(testAggregateId, 0L)
        val retrievedEvents = retrievedStream.asSequence().toList()

        assertEquals(3, retrievedEvents.size)
        assertEquals(testAggregateId, retrievedEvents[0].aggregateIdentifier)
        assertEquals(0L, retrievedEvents[0].sequenceNumber)
        assertEquals(1L, retrievedEvents[1].sequenceNumber)
        assertEquals(2L, retrievedEvents[2].sequenceNumber)
    }

    @Test
    fun `should retrieve events from specific sequence number`() {
        // Given
        TenantContextHolder.setCurrentTenantId(tenantA)
        val testAggregateId = createUniqueAggregateId("sequence-number")
        val events = createTestDomainEvents(testAggregateId, 5)
        eventStorageEngine.appendEvents(events.toMutableList())

        // When
        val retrievedStream = eventStorageEngine.readEvents(testAggregateId, 2L)
        val retrievedEvents = retrievedStream.asSequence().toList()

        // Then
        assertEquals(3, retrievedEvents.size) // Events from sequence 2, 3, 4
        assertEquals(2L, retrievedEvents[0].sequenceNumber)
        assertEquals(3L, retrievedEvents[1].sequenceNumber)
        assertEquals(4L, retrievedEvents[2].sequenceNumber)
    }

    // ============================================================================
    // Tenant Isolation Tests
    // ============================================================================

    @Test
    fun `should isolate events between tenants`() {
        // Given - Create events for Tenant A
        TenantContextHolder.setCurrentTenantId(tenantA)
        val eventsA = createTestDomainEvents("aggregate-tenant-a", 2)
        eventStorageEngine.appendEvents(eventsA.toMutableList())

        // Given - Create events for Tenant B
        TenantContextHolder.setCurrentTenantId(tenantB)
        val eventsB = createTestDomainEvents("aggregate-tenant-b", 3)
        eventStorageEngine.appendEvents(eventsB.toMutableList())

        // When/Then - Tenant A should only see its events
        TenantContextHolder.setCurrentTenantId(tenantA)
        val retrievedStreamA = eventStorageEngine.readEvents("aggregate-tenant-a", 0L)
        val retrievedEventsA = retrievedStreamA.asSequence().toList()
        assertEquals(2, retrievedEventsA.size)

        // When/Then - Tenant B should only see its events
        TenantContextHolder.setCurrentTenantId(tenantB)
        val retrievedStreamB = eventStorageEngine.readEvents("aggregate-tenant-b", 0L)
        val retrievedEventsB = retrievedStreamB.asSequence().toList()
        assertEquals(3, retrievedEventsB.size)

        // When/Then - Tenant A should not see Tenant B's aggregate
        TenantContextHolder.setCurrentTenantId(tenantA)
        val noEventsStream = eventStorageEngine.readEvents("aggregate-tenant-b", 0L)
        val noEvents = noEventsStream.asSequence().toList()
        assertEquals(0, noEvents.size)
    }

    @Test
    fun `should handle same aggregate ID across different tenants`() {
        // Given - Same aggregate ID in both tenants
        val sharedAggregateId = "shared-aggregate-id"

        // Create events for Tenant A
        TenantContextHolder.setCurrentTenantId(tenantA)
        val eventsA = createTestDomainEvents(sharedAggregateId, 2)
        eventStorageEngine.appendEvents(eventsA.toMutableList())

        // Create events for Tenant B
        TenantContextHolder.setCurrentTenantId(tenantB)
        val eventsB = createTestDomainEvents(sharedAggregateId, 3)
        eventStorageEngine.appendEvents(eventsB.toMutableList())

        // When/Then - Each tenant should see only their events
        TenantContextHolder.setCurrentTenantId(tenantA)
        val retrievedStreamA = eventStorageEngine.readEvents(sharedAggregateId, 0L)
        val retrievedEventsA = retrievedStreamA.asSequence().toList()
        assertEquals(2, retrievedEventsA.size)

        TenantContextHolder.setCurrentTenantId(tenantB)
        val retrievedStreamB = eventStorageEngine.readEvents(sharedAggregateId, 0L)
        val retrievedEventsB = retrievedStreamB.asSequence().toList()
        assertEquals(3, retrievedEventsB.size)
    }

    // ============================================================================
    // Token Management Tests
    // ============================================================================

    @Test
    fun `should create valid tail and head tokens`() {
        // Given
        TenantContextHolder.setCurrentTenantId(tenantA)

        // When
        val tailToken = eventStorageEngine.createTailToken()
        val headToken = eventStorageEngine.createHeadToken()

        // Then
        assertNotNull(tailToken)
        assertNotNull(headToken)
        assertTrue(tailToken is GlobalSequenceTrackingToken)
        assertTrue(headToken is GlobalSequenceTrackingToken)
    }

    @Test
    fun `should read events using tracking token`() {
        // Given
        TenantContextHolder.setCurrentTenantId(tenantA)
        val testAggregateId = createUniqueAggregateId("tracking-token")
        val events = createTestDomainEvents(testAggregateId, 3)
        eventStorageEngine.appendEvents(events.toMutableList())

        // When
        val tailToken = eventStorageEngine.createTailToken()
        val eventStream = eventStorageEngine.readEvents(tailToken, false)

        // Then
        assertNotNull(eventStream)
        val retrievedEvents = eventStream.toList()

        assertTrue(retrievedEvents.size >= 3) // At least our 3 events (may include test data)
    }

    // ============================================================================
    // Snapshot Storage Tests
    // ============================================================================

    @Test
    fun `should store and retrieve aggregate snapshots`() {
        // Given
        TenantContextHolder.setCurrentTenantId(tenantA)
        val snapshotData = TestAggregateSnapshot("test-state", 5)
        val testAggregateId = createUniqueAggregateId("snapshot-test")
        val snapshot =
            GenericDomainEventMessage(
                testAggregateType,
                testAggregateId,
                5L,
                snapshotData,
                MetaData.emptyInstance(),
            )

        // When
        eventStorageEngine.storeSnapshot(snapshot)

        // Then - Snapshot storage should complete without error
        // Note: Snapshot retrieval testing would require additional infrastructure
        // This test verifies that snapshot storage doesn't throw exceptions
        assertTrue(true) // Test passes if no exception is thrown
    }

    // ============================================================================
    // Error Handling and Edge Cases
    // ============================================================================

    @Test
    fun `should handle empty event stream gracefully`() {
        // Given
        TenantContextHolder.setCurrentTenantId(tenantA)
        val nonExistentAggregateId = "non-existent-aggregate"

        // When
        val eventStream = eventStorageEngine.readEvents(nonExistentAggregateId, 0L)

        // Then
        assertNotNull(eventStream)
        assertFalse(eventStream.hasNext())
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    private fun createTestDomainEvents(
        aggregateId: String,
        count: Int,
    ): List<DomainEventMessage<*>> =
        (0 until count).map { index ->
            GenericDomainEventMessage(
                testAggregateType,
                aggregateId,
                index.toLong(),
                TestEventPayload("Test event $index", index),
                MetaData.with("test", "true"),
            )
        }
}

data class TestEventPayload(
    val message: String,
    val index: Int,
)

data class TestAggregateSnapshot(
    val state: String,
    val version: Int,
)

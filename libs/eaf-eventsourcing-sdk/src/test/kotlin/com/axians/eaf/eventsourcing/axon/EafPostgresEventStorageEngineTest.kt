@file:Suppress("UnusedPrivateProperty")

package com.axians.eaf.eventsourcing.axon

import com.axians.eaf.core.tenancy.TenantContextException
import com.axians.eaf.core.tenancy.TenantContextHolder
import com.axians.eaf.eventsourcing.model.AggregateSnapshot
import com.axians.eaf.eventsourcing.model.PersistedEvent
import com.axians.eaf.eventsourcing.port.EventStoreRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.axonframework.eventhandling.DomainEventMessage
import org.axonframework.eventhandling.EventMessage
import org.axonframework.eventhandling.GenericDomainEventMessage
import org.axonframework.eventhandling.GenericEventMessage
import org.axonframework.eventsourcing.eventstore.EventStoreException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * Comprehensive unit tests for EafPostgresEventStorageEngine using MockK for all dependencies.
 * Tests focus on delegation logic, exception handling, tenant context validation, and proper
 * interaction with all dependencies.
 *
 * Coverage includes:
 * - All public methods (appendEvents, readEvents, storeSnapshot, readSnapshot, createTokens)
 * - Exception handling and translation
 * - Tenant context validation
 * - Proper delegation to dependencies
 * - Event message mapping verification
 */
class EafPostgresEventStorageEngineTest {
    // Dependencies - all mocked
    private lateinit var eventStoreRepository: EventStoreRepository
    private lateinit var eventMessageMapper: AxonEventMessageMapper
    private lateinit var exceptionHandler: AxonExceptionHandler

    // System under test
    private lateinit var storageEngine: EafPostgresEventStorageEngine

    // Test data
    private val testTenantId = "test-tenant-123"
    private val testAggregateId = "test-aggregate-456"
    private val testEventId = UUID.randomUUID()
    private val testSequenceNumber = 1L
    private val testGlobalSequence = 100L

    @BeforeEach
    fun setUp() {
        // Clear any existing tenant context
        TenantContextHolder.clear()

        // Create mocks with relaxed configuration to avoid MockK DSL matcher issues
        eventStoreRepository = mockk(relaxed = true)
        eventMessageMapper = mockk(relaxed = true)
        exceptionHandler = mockk(relaxed = true)

        // Create system under test
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

    @Test
    fun `should initialize successfully with valid dependencies`() {
        // When/Then - constructor should not throw
        assertNotNull(storageEngine)
    }

    @Nested
    inner class AppendEventsTests {
        @Test
        fun `appendEvents should delegate to repository with correct parameters`() {
            // Given
            TenantContextHolder.setCurrentTenantId(testTenantId)
            val event = createTestDomainEvent()
            val persistedEvent = createTestPersistedEvent()
            val events = mutableListOf<EventMessage<*>>(event)

            every { eventMessageMapper.mapToPersistedEvent(event, testTenantId) } returns
                persistedEvent
            coEvery {
                eventStoreRepository.appendEvents(
                    listOf(persistedEvent),
                    testTenantId,
                    testAggregateId,
                    null, // Expected version for sequence 0 should be null (new aggregate)
                )
            } returns Unit

            // When
            storageEngine.appendEvents(events)

            // Then
            verify { eventMessageMapper.mapToPersistedEvent(event, testTenantId) }
            coVerify {
                eventStoreRepository.appendEvents(
                    listOf(persistedEvent),
                    testTenantId,
                    testAggregateId,
                    null, // Expected version for sequence 0 should be null (new aggregate)
                )
            }
        }

        @Test
        fun `appendEvents should handle vararg parameter correctly`() {
            // Given
            TenantContextHolder.setCurrentTenantId(testTenantId)
            val event1 = createTestDomainEvent("event1")
            val event2 = createTestDomainEvent("event2")
            val persistedEvent1 = createTestPersistedEvent()
            val persistedEvent2 = createTestPersistedEvent()

            every { eventMessageMapper.mapToPersistedEvent(event1, testTenantId) } returns
                persistedEvent1
            every { eventMessageMapper.mapToPersistedEvent(event2, testTenantId) } returns
                persistedEvent2
            coEvery { eventStoreRepository.appendEvents(any(), any(), any(), any()) } returns Unit

            // When
            storageEngine.appendEvents(event1, event2)

            // Then
            verify { eventMessageMapper.mapToPersistedEvent(event1, testTenantId) }
            verify { eventMessageMapper.mapToPersistedEvent(event2, testTenantId) }
        }

        @Test
        fun `appendEvents should skip non-domain events`() {
            // Given
            TenantContextHolder.setCurrentTenantId(testTenantId)
            val domainEvent = createTestDomainEvent()
            val genericEvent = GenericEventMessage.asEventMessage<String>("non-domain-event")
            val events = mutableListOf<EventMessage<*>>(domainEvent, genericEvent)
            val persistedEvent = createTestPersistedEvent()

            every { eventMessageMapper.mapToPersistedEvent(domainEvent, testTenantId) } returns
                persistedEvent
            coEvery { eventStoreRepository.appendEvents(any(), any(), any(), any()) } returns Unit

            // When
            storageEngine.appendEvents(events)

            // Then
            verify(exactly = 1) { eventMessageMapper.mapToPersistedEvent(any(), any()) }
            verify { eventMessageMapper.mapToPersistedEvent(domainEvent, testTenantId) }
        }

        @Test
        fun `appendEvents should throw exception when no tenant context`() {
            // Given - no tenant context set
            val event = createTestDomainEvent()
            val events = mutableListOf<EventMessage<*>>(event)
            val expectedException = RuntimeException("No tenant context")

            every {
                exceptionHandler.handleAppendException("appendEvents", "unknown", null, 1, any())
            } returns expectedException

            // When/Then
            assertThrows(RuntimeException::class.java) { storageEngine.appendEvents(events) }

            verify {
                exceptionHandler.handleAppendException(
                    "appendEvents",
                    "unknown",
                    null,
                    1,
                    any<TenantContextException>(),
                )
            }
        }

        @Test
        fun `appendEvents should handle repository exceptions`() {
            // Given
            TenantContextHolder.setCurrentTenantId(testTenantId)
            val event = createTestDomainEvent()
            val persistedEvent = createTestPersistedEvent()
            val events = mutableListOf<EventMessage<*>>(event)
            val repositoryException = RuntimeException("Database error")
            val handledException = EventStoreException("Handled exception")

            every { eventMessageMapper.mapToPersistedEvent(event, testTenantId) } returns
                persistedEvent
            coEvery { eventStoreRepository.appendEvents(any(), any(), any(), any()) } throws
                repositoryException
            every {
                exceptionHandler.handleAppendException(
                    any(),
                    any(),
                    any(),
                    any(),
                    repositoryException,
                )
            } returns handledException

            // When/Then
            assertThrows(EventStoreException::class.java) { storageEngine.appendEvents(events) }

            verify {
                exceptionHandler.handleAppendException(
                    "appendEvents",
                    testTenantId,
                    null,
                    1,
                    repositoryException,
                )
            }
        }
    }

    @Nested
    inner class ReadEventsTests {
        @Test
        fun `readEvents with aggregate identifier should delegate correctly`() {
            // Given
            TenantContextHolder.setCurrentTenantId(testTenantId)
            val firstSequenceNumber = 1L
            val persistedEvents = listOf(createTestPersistedEvent())
            val domainEvent = createTestDomainEvent()

            coEvery {
                eventStoreRepository.getEvents(testTenantId, testAggregateId, firstSequenceNumber)
            } returns persistedEvents
            every { eventMessageMapper.mapToDomainEvent(persistedEvents[0]) } returns domainEvent

            // When
            val result = storageEngine.readEvents(testAggregateId, firstSequenceNumber)

            // Then
            assertNotNull(result)
            coVerify {
                eventStoreRepository.getEvents(testTenantId, testAggregateId, firstSequenceNumber)
            }
            verify { eventMessageMapper.mapToDomainEvent(persistedEvents[0]) }
        }

        @Test
        fun `readEvents with tracking token should handle null token`() {
            // Given
            TenantContextHolder.setCurrentTenantId(testTenantId)
            val persistedEvents = listOf(createTestPersistedEvent())
            val domainEvent = createTestDomainEvent()

            coEvery { eventStoreRepository.readEventsFrom(testTenantId, 0L) } returns
                persistedEvents
            every { eventMessageMapper.mapToDomainEvent(persistedEvents[0]) } returns domainEvent

            // When
            val result = storageEngine.readEvents(null, false)

            // Then
            assertNotNull(result)
            coVerify { eventStoreRepository.readEventsFrom(testTenantId, 0L) }
        }

        @Test
        fun `readEvents with tracking token should handle GlobalSequenceTrackingToken`() {
            // Given
            TenantContextHolder.setCurrentTenantId(testTenantId)
            val globalSequence = 100L
            val token = GlobalSequenceTrackingToken(globalSequence)
            val persistedEvents = listOf(createTestPersistedEvent())
            val domainEvent = createTestDomainEvent()

            coEvery {
                eventStoreRepository.readEventsFrom(testTenantId, globalSequence + 1)
            } returns persistedEvents
            every { eventMessageMapper.mapToDomainEvent(persistedEvents[0]) } returns domainEvent

            // When
            val result = storageEngine.readEvents(token, false)

            // Then
            assertNotNull(result)
            coVerify { eventStoreRepository.readEventsFrom(testTenantId, globalSequence + 1) }
        }

        @Test
        fun `readEvents should throw exception when no tenant context`() {
            // Given - no tenant context set
            val expectedException = RuntimeException("No tenant context")

            every {
                exceptionHandler.handleReadException(
                    "readEvents",
                    "unknown",
                    testAggregateId,
                    null,
                    any(),
                )
            } returns expectedException

            // When/Then
            assertThrows(RuntimeException::class.java) {
                storageEngine.readEvents(testAggregateId, 1L)
            }
        }

        @Test
        fun `readEvents tracking should throw exception when no tenant context`() {
            // Given - no tenant context set
            val expectedException = RuntimeException("No tenant context")

            every {
                exceptionHandler.handleReadException(
                    "readEventsTracking",
                    "unknown",
                    null,
                    null,
                    any(),
                )
            } returns expectedException

            // When/Then
            assertThrows(RuntimeException::class.java) { storageEngine.readEvents(null, false) }
        }
    }

    @Nested
    inner class SnapshotTests {
        @Test
        fun `storeSnapshot should delegate correctly`() {
            // Given
            TenantContextHolder.setCurrentTenantId(testTenantId)
            val snapshot = createTestDomainEvent()
            val aggregateSnapshot = createTestAggregateSnapshot()

            every { eventMessageMapper.mapToAggregateSnapshot(snapshot, testTenantId) } returns
                aggregateSnapshot
            coEvery {
                eventStoreRepository.saveSnapshot(aggregateSnapshot, testTenantId, testAggregateId)
            } returns Unit

            // When
            storageEngine.storeSnapshot(snapshot)

            // Then
            verify { eventMessageMapper.mapToAggregateSnapshot(snapshot, testTenantId) }
            coVerify {
                eventStoreRepository.saveSnapshot(aggregateSnapshot, testTenantId, testAggregateId)
            }
        }

        @Test
        fun `storeSnapshot should handle exceptions`() {
            // Given
            TenantContextHolder.setCurrentTenantId(testTenantId)
            val snapshot = createTestDomainEvent()
            val repositoryException = RuntimeException("Snapshot save failed")
            val handledException = EventStoreException("Handled exception")

            every { eventMessageMapper.mapToAggregateSnapshot(any(), any()) } throws
                repositoryException
            every {
                exceptionHandler.handleAppendException(
                    "storeSnapshot",
                    testTenantId,
                    testAggregateId,
                    1,
                    repositoryException,
                )
            } returns handledException

            // When/Then
            assertThrows(EventStoreException::class.java) { storageEngine.storeSnapshot(snapshot) }
        }

        @Test
        fun `readSnapshot should return snapshot when found`() {
            // Given
            TenantContextHolder.setCurrentTenantId(testTenantId)
            val aggregateSnapshot = createTestAggregateSnapshot()
            val domainEvent = createTestDomainEvent()

            coEvery { eventStoreRepository.getSnapshot(testTenantId, testAggregateId) } returns
                aggregateSnapshot
            every { eventMessageMapper.mapSnapshotToDomainEvent(aggregateSnapshot) } returns
                domainEvent

            // When
            val result = storageEngine.readSnapshot(testAggregateId)

            // Then
            assertTrue(result.isPresent)
            assertEquals(domainEvent, result.get())
            coVerify { eventStoreRepository.getSnapshot(testTenantId, testAggregateId) }
            verify { eventMessageMapper.mapSnapshotToDomainEvent(aggregateSnapshot) }
        }

        @Test
        fun `readSnapshot should return empty when not found`() {
            // Given
            TenantContextHolder.setCurrentTenantId(testTenantId)

            coEvery { eventStoreRepository.getSnapshot(testTenantId, testAggregateId) } returns null

            // When
            val result = storageEngine.readSnapshot(testAggregateId)

            // Then
            assertTrue(result.isEmpty)
            coVerify { eventStoreRepository.getSnapshot(testTenantId, testAggregateId) }
        }

        @Test
        fun `readSnapshot should handle exceptions gracefully`() {
            // Given
            TenantContextHolder.setCurrentTenantId(testTenantId)
            val repositoryException = RuntimeException("Snapshot read failed")

            coEvery { eventStoreRepository.getSnapshot(testTenantId, testAggregateId) } throws
                repositoryException

            // When
            val result = storageEngine.readSnapshot(testAggregateId)

            // Then
            assertTrue(result.isEmpty)
        }
    }

    @Nested
    inner class TokenCreationTests {
        @Test
        fun `createHeadToken should delegate to repository`() {
            // Given
            TenantContextHolder.setCurrentTenantId(testTenantId)
            val maxSequence = 150L

            coEvery { eventStoreRepository.getMaxGlobalSequence(testTenantId) } returns maxSequence

            // When
            val token = storageEngine.createHeadToken()

            // Then
            assertNotNull(token)
            assertTrue(token is GlobalSequenceTrackingToken)
            assertEquals(maxSequence, (token as GlobalSequenceTrackingToken).globalSequence)
            coVerify { eventStoreRepository.getMaxGlobalSequence(testTenantId) }
        }

        @Test
        fun `createHeadToken should handle exceptions and return initial token`() {
            // Given
            TenantContextHolder.setCurrentTenantId(testTenantId)
            val repositoryException = RuntimeException("Max sequence read failed")

            coEvery { eventStoreRepository.getMaxGlobalSequence(testTenantId) } throws
                repositoryException

            // When
            val token = storageEngine.createHeadToken()

            // Then
            assertNotNull(token)
            assertTrue(token is GlobalSequenceTrackingToken)
            assertEquals(
                GlobalSequenceTrackingToken.initial().globalSequence,
                (token as GlobalSequenceTrackingToken).globalSequence,
            )
        }

        @Test
        fun `createTailToken should return initial token`() {
            // When
            val token = storageEngine.createTailToken()

            // Then
            assertNotNull(token)
            assertTrue(token is GlobalSequenceTrackingToken)
            assertEquals(
                GlobalSequenceTrackingToken.initial().globalSequence,
                (token as GlobalSequenceTrackingToken).globalSequence,
            )
        }

        @Test
        fun `createTokenAt should return initial token`() {
            // When
            val token = storageEngine.createTokenAt(Instant.now())

            // Then
            assertNotNull(token)
            assertTrue(token is GlobalSequenceTrackingToken)
            assertEquals(
                GlobalSequenceTrackingToken.initial().globalSequence,
                (token as GlobalSequenceTrackingToken).globalSequence,
            )
        }
    }

    @Nested
    inner class TenantContextValidationTests {
        @Test
        fun `all methods should validate tenant context before execution`() {
            // Given - no tenant context set
            val event = createTestDomainEvent()

            // When/Then - methods that throw exceptions due to missing tenant context
            assertThrows(Exception::class.java) { storageEngine.appendEvents(mutableListOf(event)) }
            assertThrows(Exception::class.java) { storageEngine.readEvents(testAggregateId, 1L) }
            assertThrows(Exception::class.java) { storageEngine.readEvents(null, false) }
            assertThrows(Exception::class.java) { storageEngine.storeSnapshot(event) }

            // These methods handle tenant context validation gracefully and don't throw:
            // createHeadToken returns initial token on error
            val headToken = storageEngine.createHeadToken()
            assertNotNull(headToken)
            assertTrue(headToken is GlobalSequenceTrackingToken)

            // readSnapshot returns empty optional on error
            val snapshot = storageEngine.readSnapshot(testAggregateId)
            assertTrue(snapshot.isEmpty)

            // Verify exception handler was called for the methods that do throw
            verify(atLeast = 1) {
                exceptionHandler.handleAppendException(any(), any(), any(), any(), any())
            }
            verify(atLeast = 1) {
                exceptionHandler.handleReadException(any(), any(), any(), any(), any())
            }
        }
    }

    // Test data creation helpers
    private fun createTestDomainEvent(payload: String = "test-payload"): DomainEventMessage<String> =
        GenericDomainEventMessage(
            "TestAggregate",
            testAggregateId,
            0L, // Use sequence 0 for first event to match null expected version logic
            payload,
            mapOf("test-meta" to "value"),
        )

    private fun createTestPersistedEvent(): PersistedEvent =
        PersistedEvent(
            globalSequenceId = 1L,
            eventId = testEventId,
            streamId = "TestAggregate-$testAggregateId",
            aggregateId = testAggregateId,
            aggregateType = "TestAggregate",
            expectedVersion = null, // null for first event (sequence 0)
            sequenceNumber = 0L, // Changed to 0 to be consistent with domain event
            tenantId = testTenantId,
            eventType = "TestEvent",
            payload = """{"data":"test"}""",
            metadata = """{"tenant":"$testTenantId"}""",
            timestampUtc = Instant.now(),
        )

    private fun createTestAggregateSnapshot(): AggregateSnapshot =
        AggregateSnapshot(
            aggregateId = testAggregateId,
            tenantId = testTenantId,
            aggregateType = "TestAggregate",
            lastSequenceNumber = 5L,
            snapshotPayloadJsonb = """{"state":"snapshot"}""",
            version = 1,
            timestampUtc = Instant.now(),
        )
}

package com.axians.eaf.eventsourcing.axon

import com.axians.eaf.eventsourcing.axon.exception.EventSerializationException
import com.axians.eaf.eventsourcing.model.AggregateSnapshot
import com.axians.eaf.eventsourcing.model.PersistedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.axonframework.eventhandling.GenericDomainEventMessage
import org.axonframework.messaging.MetaData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * Comprehensive tests for AxonEventMessageMapper covering bidirectional event message conversion,
 * metadata preservation, serialization/deserialization, and error handling scenarios.
 *
 * Test Coverage:
 * - DomainEventMessage ↔ PersistedEvent conversion
 * - AggregateSnapshot ↔ DomainEventMessage conversion
 * - Metadata preservation and enrichment
 * - Payload serialization/deserialization
 * - Error scenarios and exception handling
 * - Various event payload types
 * - Tenant context handling
 */
class AxonEventMessageMapperTest {
    private lateinit var objectMapper: ObjectMapper
    private lateinit var mapper: AxonEventMessageMapper

    // Test data
    private val testTenantId = "tenant-123"
    private val testAggregateId = "aggregate-456"
    private val testAggregateType = "TestAggregate"
    private val testSequenceNumber = 5L
    private val testEventId = UUID.randomUUID().toString()
    private val testTimestamp = Instant.now()

    @BeforeEach
    fun setUp() {
        objectMapper =
            ObjectMapper().apply {
                registerKotlinModule()
                registerModule(JavaTimeModule())
            }
        mapper = AxonEventMessageMapper(objectMapper)
    }

    @Nested
    inner class DomainEventToPersistedEventTests {
        @Test
        fun `mapToPersistedEvent should convert simple domain event correctly`() {
            // Given
            val payload = TestEventPayload("test-data", 42)
            val metadata = MetaData.from(mapOf("correlation-id" to "test-123"))
            val domainEvent =
                GenericDomainEventMessage(
                    testAggregateType,
                    testAggregateId,
                    testSequenceNumber,
                    payload,
                    metadata,
                    testEventId,
                    testTimestamp,
                )

            // When
            val persistedEvent = mapper.mapToPersistedEvent(domainEvent, testTenantId)

            // Then
            assertEquals(testAggregateId, persistedEvent.aggregateId)
            assertEquals(testAggregateType, persistedEvent.aggregateType)
            assertEquals(testSequenceNumber, persistedEvent.sequenceNumber)
            assertEquals(testTenantId, persistedEvent.tenantId)
            assertEquals(
                "com.axians.eaf.eventsourcing.axon.AxonEventMessageMapperTest\$TestEventPayload",
                persistedEvent.eventType,
            )
            assertEquals(testTimestamp, persistedEvent.timestampUtc)

            // Verify payload serialization
            assertEquals("""{"data":"test-data","number":42}""", persistedEvent.payload)

            // Verify metadata enrichment
            assertNotNull(persistedEvent.metadata)
            assertTrue(persistedEvent.metadata!!.contains(testTenantId))
            assertTrue(
                persistedEvent.metadata!!.contains("test-123"),
            ) // Correct correlation-id value
        }

        @Test
        fun `mapToPersistedEvent should handle null payload`() {
            // Given
            val domainEvent =
                GenericDomainEventMessage(
                    testAggregateType,
                    testAggregateId,
                    testSequenceNumber,
                    null,
                    MetaData.emptyInstance(),
                )

            // When
            val persistedEvent = mapper.mapToPersistedEvent(domainEvent, testTenantId)

            // Then
            assertEquals("null", persistedEvent.payload)
            assertNotNull(persistedEvent.metadata)
        }

        @Test
        fun `mapToPersistedEvent should handle empty metadata`() {
            // Given
            val payload = "simple-string"
            val domainEvent =
                GenericDomainEventMessage(
                    testAggregateType,
                    testAggregateId,
                    testSequenceNumber,
                    payload,
                    MetaData.emptyInstance(),
                )

            // When
            val persistedEvent = mapper.mapToPersistedEvent(domainEvent, testTenantId)

            // Then
            assertNotNull(persistedEvent.metadata)
            assertTrue(persistedEvent.metadata!!.contains(testTenantId))
            assertTrue(
                persistedEvent.metadata!!.contains(testSequenceNumber.toString()),
            )
        }

        @Test
        fun `mapToPersistedEvent should throw exception for missing aggregate identifier`() {
            // Given
            val domainEvent =
                GenericDomainEventMessage(
                    testAggregateType,
                    null, // Missing aggregate identifier
                    testSequenceNumber,
                    "payload",
                )

            // When/Then
            assertThrows(IllegalArgumentException::class.java) {
                mapper.mapToPersistedEvent(domainEvent, testTenantId)
            }
        }

        @Test
        fun `mapToPersistedEvent should handle complex nested payload`() {
            // Given
            val complexPayload =
                ComplexEventPayload(
                    id = UUID.randomUUID().toString(),
                    data = NestedData("nested", 123),
                    tags = listOf("tag1", "tag2"),
                    metadata = mapOf("key1" to "value1", "key2" to "value2"),
                )
            val domainEvent =
                GenericDomainEventMessage(
                    testAggregateType,
                    testAggregateId,
                    testSequenceNumber,
                    complexPayload,
                )

            // When
            val persistedEvent = mapper.mapToPersistedEvent(domainEvent, testTenantId)

            // Then
            assertNotNull(persistedEvent.payload)
            // Verify the JSON contains expected nested structure
            assertEquals(
                """{"id":"${complexPayload.id}","data":{"value":"nested","count":123},"tags":["tag1","tag2"],"metadata":{"key1":"value1","key2":"value2"}}""",
                persistedEvent.payload,
            )
        }
    }

    @Nested
    inner class PersistedEventToDomainEventTests {
        @Test
        fun `mapToDomainEvent should reconstruct domain event correctly`() {
            // Given
            val originalPayload = TestEventPayload("original-data", 789)
            val originalMetadata =
                mapOf(
                    "tenant_id" to testTenantId,
                    "correlation-id" to "test-456",
                    "aggregate_version" to "3",
                )
            val persistedEvent =
                PersistedEvent(
                    globalSequenceId = 100L,
                    eventId = UUID.fromString(testEventId),
                    streamId = "TestAggregate-$testAggregateId",
                    aggregateId = testAggregateId,
                    aggregateType = testAggregateType,
                    expectedVersion = 2L,
                    sequenceNumber = 3L,
                    tenantId = testTenantId,
                    eventType = "TestEventPayload",
                    payload = """{"data":"original-data","number":789}""",
                    metadata =
                        """{"tenant_id":"$testTenantId","correlation-id":"test-456","aggregate_version":"3"}""",
                    timestampUtc = testTimestamp,
                )

            // When
            val domainEvent = mapper.mapToDomainEvent(persistedEvent)

            // Then
            assertEquals(testAggregateType, domainEvent.type)
            assertEquals(testAggregateId, domainEvent.aggregateIdentifier)
            assertEquals(3L, domainEvent.sequenceNumber)

            // Verify metadata reconstruction
            assertEquals(testTenantId, domainEvent.metaData["tenant_id"])
            assertEquals("test-456", domainEvent.metaData["correlation-id"])
            assertEquals("100", domainEvent.metaData["global_sequence_id"])
            assertEquals(
                testTimestamp.toString(),
                domainEvent.metaData["event_timestamp"],
            )
        }

        @Test
        fun `mapToDomainEvent should handle null metadata`() {
            // Given
            val persistedEvent =
                PersistedEvent(
                    eventId = UUID.fromString(testEventId),
                    streamId = "TestAggregate-$testAggregateId",
                    aggregateId = testAggregateId,
                    aggregateType = testAggregateType,
                    expectedVersion = 0L,
                    sequenceNumber = 1L,
                    tenantId = testTenantId,
                    eventType = "TestEvent",
                    payload = """{"data":"simple-payload"}""",
                    metadata = null,
                    timestampUtc = testTimestamp,
                )

            // When
            val domainEvent = mapper.mapToDomainEvent(persistedEvent)

            // Then
            assertNotNull(domainEvent.metaData)
            assertEquals("0", domainEvent.metaData["global_sequence_id"])
        }

        @Test
        fun `mapToDomainEvent should handle empty metadata`() {
            // Given
            val persistedEvent =
                PersistedEvent(
                    eventId = UUID.fromString(testEventId),
                    streamId = "TestAggregate-$testAggregateId",
                    aggregateId = testAggregateId,
                    aggregateType = testAggregateType,
                    expectedVersion = 0L,
                    sequenceNumber = 1L,
                    tenantId = testTenantId,
                    eventType = "TestEvent",
                    payload = """{"data":"simple-payload"}""",
                    metadata = "",
                    timestampUtc = testTimestamp,
                )

            // When
            val domainEvent = mapper.mapToDomainEvent(persistedEvent)

            // Then
            assertNotNull(domainEvent.metaData)
        }

        @Test
        fun `mapToDomainEvent should handle invalid metadata gracefully`() {
            // Given
            val persistedEvent =
                PersistedEvent(
                    eventId = UUID.fromString(testEventId),
                    streamId = "TestAggregate-$testAggregateId",
                    aggregateId = testAggregateId,
                    aggregateType = testAggregateType,
                    expectedVersion = 0L,
                    sequenceNumber = 1L,
                    tenantId = testTenantId,
                    eventType = "TestEvent",
                    payload = """{"data":"valid-payload"}""",
                    metadata = "invalid-json-{{{",
                    timestampUtc = testTimestamp,
                )

            // When
            val domainEvent = mapper.mapToDomainEvent(persistedEvent)

            // Then
            assertNotNull(domainEvent.metaData)
            // Should return empty metadata instead of throwing
            assertEquals(
                2,
                domainEvent.metaData.size,
            ) // Only global_sequence_id and event_timestamp
        }
    }

    @Nested
    inner class SnapshotMappingTests {
        @Test
        fun `mapToAggregateSnapshot should convert domain event correctly`() {
            // Given
            val snapshotPayload =
                TestSnapshotPayload("snapshot-state", listOf("item1", "item2"))
            val snapshotEvent =
                GenericDomainEventMessage(
                    testAggregateType,
                    testAggregateId,
                    testSequenceNumber,
                    snapshotPayload,
                    MetaData.from(mapOf("snapshot-version" to "1.0")),
                    testEventId,
                    testTimestamp,
                )

            // When
            val aggregateSnapshot =
                mapper.mapToAggregateSnapshot(snapshotEvent, testTenantId)

            // Then
            assertEquals(testAggregateId, aggregateSnapshot.aggregateId)
            assertEquals(testTenantId, aggregateSnapshot.tenantId)
            assertEquals(testAggregateType, aggregateSnapshot.aggregateType)
            assertEquals(testSequenceNumber, aggregateSnapshot.lastSequenceNumber)
            assertEquals(1, aggregateSnapshot.version)
            assertEquals(testTimestamp, aggregateSnapshot.timestampUtc)

            // Verify snapshot payload
            assertEquals(
                """{"state":"snapshot-state","items":["item1","item2"]}""",
                aggregateSnapshot.snapshotPayloadJsonb,
            )
        }

        @Test
        fun `mapSnapshotToDomainEvent should reconstruct snapshot event correctly`() {
            // Given
            val originalData = TestSnapshotPayload("restored-state", listOf("a", "b"))
            val aggregateSnapshot =
                AggregateSnapshot(
                    aggregateId = testAggregateId,
                    tenantId = testTenantId,
                    aggregateType = testAggregateType,
                    lastSequenceNumber = 15L,
                    snapshotPayloadJsonb =
                        """{"state":"restored-state","items":["a","b"]}""",
                    version = 2,
                    timestampUtc = testTimestamp,
                )

            // When
            val domainEvent = mapper.mapSnapshotToDomainEvent(aggregateSnapshot)

            // Then
            assertEquals(testAggregateType, domainEvent.type)
            assertEquals(testAggregateId, domainEvent.aggregateIdentifier)
            assertEquals(15L, domainEvent.sequenceNumber)

            // Verify metadata
            assertEquals(testTenantId, domainEvent.metaData["tenant_id"])
            assertEquals("2", domainEvent.metaData["aggregate_version"])
            assertEquals(
                testTimestamp.toString(),
                domainEvent.metaData["event_timestamp"],
            )
        }

        @Test
        fun `mapToAggregateSnapshot should throw exception for missing aggregate identifier`() {
            // Given
            val snapshotEvent =
                GenericDomainEventMessage(
                    testAggregateType,
                    null, // Missing aggregate identifier
                    testSequenceNumber,
                    "snapshot-data",
                )

            // When/Then
            assertThrows(IllegalArgumentException::class.java) {
                mapper.mapToAggregateSnapshot(snapshotEvent, testTenantId)
            }
        }
    }

    @Nested
    inner class TenantContextTests {
        @Test
        fun `extractTenantId should return tenant ID from metadata`() {
            // Given
            val metadata =
                MetaData.from(
                    mapOf("tenant_id" to testTenantId, "other" to "value"),
                )
            val domainEvent =
                GenericDomainEventMessage(
                    testAggregateType,
                    testAggregateId,
                    testSequenceNumber,
                    "payload",
                    metadata,
                )

            // When
            val extractedTenantId = mapper.extractTenantId(domainEvent)

            // Then
            assertEquals(testTenantId, extractedTenantId)
        }

        @Test
        fun `extractTenantId should return null when not present`() {
            // Given
            val metadata = MetaData.from(mapOf("other" to "value"))
            val domainEvent =
                GenericDomainEventMessage(
                    testAggregateType,
                    testAggregateId,
                    testSequenceNumber,
                    "payload",
                    metadata,
                )

            // When
            val extractedTenantId = mapper.extractTenantId(domainEvent)

            // Then
            assertNull(extractedTenantId)
        }

        @Test
        fun `addTenantContext should add tenant ID to metadata`() {
            // Given
            val originalMetadata = MetaData.from(mapOf("existing" to "value"))

            // When
            val enhancedMetadata =
                mapper.addTenantContext(originalMetadata, testTenantId)

            // Then
            assertEquals("value", enhancedMetadata["existing"])
            assertEquals(testTenantId, enhancedMetadata["tenant_id"])
        }
    }

    @Nested
    inner class SerializationErrorTests {
        @Test
        fun `should handle payload serialization errors`() {
            // Given
            val problematicPayload = ProblematicPayload()
            val domainEvent =
                GenericDomainEventMessage(
                    testAggregateType,
                    testAggregateId,
                    testSequenceNumber,
                    problematicPayload,
                )

            // When/Then
            assertThrows(EventSerializationException::class.java) {
                mapper.mapToPersistedEvent(domainEvent, testTenantId)
            }
        }

        @Test
        fun `should handle payload deserialization errors gracefully`() {
            // Given
            val persistedEvent =
                PersistedEvent(
                    eventId = UUID.fromString(testEventId),
                    streamId = "TestAggregate-$testAggregateId",
                    aggregateId = testAggregateId,
                    aggregateType = testAggregateType,
                    expectedVersion = 0L,
                    sequenceNumber = 1L,
                    tenantId = testTenantId,
                    eventType = "UnknownEventType",
                    payload = "invalid-json-{{{",
                    metadata = null,
                    timestampUtc = testTimestamp,
                )

            // When/Then
            assertThrows(EventSerializationException::class.java) {
                mapper.mapToDomainEvent(persistedEvent)
            }
        }
    }

    @Nested
    inner class BidirectionalConversionTests {
        @Test
        fun `round trip conversion should preserve event data`() {
            // Given
            val originalPayload = TestEventPayload("round-trip-test", 999)
            val originalMetadata =
                MetaData.from(
                    mapOf(
                        "correlation-id" to "test-round-trip",
                        "user-id" to "user-123",
                    ),
                )
            val originalEvent =
                GenericDomainEventMessage(
                    testAggregateType,
                    testAggregateId,
                    testSequenceNumber,
                    originalPayload,
                    originalMetadata,
                    testEventId,
                    testTimestamp,
                )

            // When - Convert to persisted event and back
            val persistedEvent = mapper.mapToPersistedEvent(originalEvent, testTenantId)
            val reconstructedEvent = mapper.mapToDomainEvent(persistedEvent)

            // Then - Verify key properties are preserved
            assertEquals(originalEvent.type, reconstructedEvent.type)
            assertEquals(
                originalEvent.aggregateIdentifier,
                reconstructedEvent.aggregateIdentifier,
            )
            assertEquals(
                originalEvent.sequenceNumber,
                reconstructedEvent.sequenceNumber,
            )

            // Verify metadata preservation (some keys may be added during the process)
            assertEquals(
                "test-round-trip",
                reconstructedEvent.metaData["correlation-id"],
            )
            assertEquals("user-123", reconstructedEvent.metaData["user-id"])
            assertEquals(testTenantId, reconstructedEvent.metaData["tenant_id"])
        }

        @Test
        fun `snapshot round trip should preserve data`() {
            // Given
            val snapshotPayload =
                TestSnapshotPayload("snapshot-test", listOf("x", "y", "z"))
            val originalSnapshot =
                GenericDomainEventMessage(
                    testAggregateType,
                    testAggregateId,
                    testSequenceNumber,
                    snapshotPayload,
                    MetaData.emptyInstance(),
                    testEventId,
                    testTimestamp,
                )

            // When - Convert to aggregate snapshot and back
            val aggregateSnapshot =
                mapper.mapToAggregateSnapshot(originalSnapshot, testTenantId)
            val reconstructedSnapshot =
                mapper.mapSnapshotToDomainEvent(aggregateSnapshot)

            // Then
            assertEquals(originalSnapshot.type, reconstructedSnapshot.type)
            assertEquals(
                originalSnapshot.aggregateIdentifier,
                reconstructedSnapshot.aggregateIdentifier,
            )
            assertEquals(
                originalSnapshot.sequenceNumber,
                reconstructedSnapshot.sequenceNumber,
            )
            assertEquals(testTenantId, reconstructedSnapshot.metaData["tenant_id"])
        }
    }

    // Test data classes
    data class TestEventPayload(
        val data: String,
        val number: Int,
    )

    data class ComplexEventPayload(
        val id: String,
        val data: NestedData,
        val tags: List<String>,
        val metadata: Map<String, String>,
    )

    data class TestSnapshotPayload(
        val state: String,
        val items: List<String>,
    )

    data class NestedData(
        val value: String,
        val count: Int,
    )

    // Problematic payload that will cause serialization errors
    class ProblematicPayload {
        fun getProblematicField(): String = throw RuntimeException("Serialization error!")
    }
}

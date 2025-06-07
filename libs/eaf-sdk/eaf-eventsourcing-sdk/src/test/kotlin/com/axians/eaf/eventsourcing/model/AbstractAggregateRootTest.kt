package com.axians.eaf.eventsourcing.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class AbstractAggregateRootTest {
    private lateinit var testAggregate: TestAggregate

    @BeforeEach
    fun setUp() {
        testAggregate = TestAggregate(UUID.randomUUID())
    }

    @Test
    fun `should start with version 0 and no uncommitted events`() {
        assertEquals(0L, testAggregate.version)
        assertFalse(testAggregate.hasUncommittedEvents())
        assertTrue(testAggregate.peekUncommittedEvents().isEmpty())
    }

    @Test
    fun `should increment version when applying events`() {
        val event1 = TestEvent("event1")
        val event2 = TestEvent("event2")

        testAggregate.applyEvent(event1)
        assertEquals(1L, testAggregate.version)
        assertTrue(testAggregate.hasUncommittedEvents())

        testAggregate.applyEvent(event2)
        assertEquals(2L, testAggregate.version)
        assertEquals(2, testAggregate.peekUncommittedEvents().size)
    }

    @Test
    fun `should track uncommitted events correctly`() {
        val event1 = TestEvent("event1")
        val event2 = TestEvent("event2")

        testAggregate.applyEvent(event1)
        testAggregate.applyEvent(event2)

        val uncommittedEvents = testAggregate.peekUncommittedEvents()
        assertEquals(2, uncommittedEvents.size)
        assertEquals(event1, uncommittedEvents[0])
        assertEquals(event2, uncommittedEvents[1])
    }

    @Test
    fun `should clear uncommitted events when getting them`() {
        val event = TestEvent("event")
        testAggregate.applyEvent(event)

        assertTrue(testAggregate.hasUncommittedEvents())

        val events = testAggregate.getUncommittedEvents()
        assertEquals(1, events.size)
        assertEquals(event, events[0])

        // Events should be cleared after getting them
        assertFalse(testAggregate.hasUncommittedEvents())
        assertTrue(testAggregate.peekUncommittedEvents().isEmpty())
    }

    @Test
    fun `should mark events as committed`() {
        val event = TestEvent("event")
        testAggregate.applyEvent(event)

        assertTrue(testAggregate.hasUncommittedEvents())

        testAggregate.markEventsAsCommitted()

        assertFalse(testAggregate.hasUncommittedEvents())
        assertTrue(testAggregate.peekUncommittedEvents().isEmpty())
    }

    @Test
    fun `should rehydrate from events correctly`() {
        val events =
            listOf(
                TestEvent("event1"),
                TestEvent("event2"),
                TestEvent("event3"),
            )

        testAggregate.rehydrateFromEvents(events)

        assertEquals(3L, testAggregate.version)
        assertEquals(3, testAggregate.appliedEvents.size)
        assertEquals("event1", testAggregate.appliedEvents[0].data)
        assertEquals("event2", testAggregate.appliedEvents[1].data)
        assertEquals("event3", testAggregate.appliedEvents[2].data)

        // Rehydration should not create uncommitted events
        assertFalse(testAggregate.hasUncommittedEvents())
    }

    @Test
    fun `should rehydrate from events with starting version`() {
        val events =
            listOf(
                TestEvent("event1"),
                TestEvent("event2"),
            )

        testAggregate.rehydrateFromEvents(events, fromVersion = 5L)

        assertEquals(7L, testAggregate.version) // 5 + 2 events
        assertEquals(2, testAggregate.appliedEvents.size)
        assertFalse(testAggregate.hasUncommittedEvents())
    }

    @Test
    fun `should handle event dispatching correctly`() {
        val event = TestEvent("test")
        testAggregate.applyEvent(event)

        assertEquals(1, testAggregate.appliedEvents.size)
        assertEquals("test", testAggregate.appliedEvents[0].data)
    }

    @Test
    fun `should provide correct aggregate type`() {
        assertEquals("TestAggregate", testAggregate.getAggregateType())
    }

    @Test
    fun `should support equality based on aggregate ID`() {
        val id = UUID.randomUUID()
        val aggregate1 = TestAggregate(id)
        val aggregate2 = TestAggregate(id)
        val aggregate3 = TestAggregate(UUID.randomUUID())

        assertEquals(aggregate1, aggregate2)
        assertEquals(aggregate1.hashCode(), aggregate2.hashCode())
        assertTrue(aggregate1 != aggregate3)
    }

    @Test
    fun `should provide meaningful toString representation`() {
        val id = UUID.randomUUID()
        val aggregate = TestAggregate(id)
        val toString = aggregate.toString()

        assertTrue(toString.contains("TestAggregate"))
        assertTrue(toString.contains(id.toString()))
        assertTrue(toString.contains("version=0"))
    }

    // Test aggregate implementation for testing purposes
    private class TestAggregate(
        override val aggregateId: UUID,
    ) : AbstractAggregateRoot<UUID>() {
        val appliedEvents = mutableListOf<TestEvent>()

        fun applyEvent(event: TestEvent) {
            apply(event)
        }

        override fun handleEvent(event: Any) {
            when (event) {
                is TestEvent -> appliedEvents.add(event)
                else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
            }
        }
    }

    private data class TestEvent(
        val data: String,
    )
}

package com.axians.eaf.eventsourcing.example.aggregate

import com.axians.eaf.eventsourcing.example.command.AssignTicketCommand
import com.axians.eaf.eventsourcing.example.command.CloseTicketCommand
import com.axians.eaf.eventsourcing.example.command.CreateTicketCommand
import com.axians.eaf.eventsourcing.example.command.TicketPriority
import com.axians.eaf.eventsourcing.example.event.TicketAssignedEvent
import com.axians.eaf.eventsourcing.example.event.TicketClosedEvent
import com.axians.eaf.eventsourcing.example.event.TicketCreatedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class TicketTest {
    private lateinit var ticketId: UUID
    private lateinit var ticket: Ticket

    @BeforeEach
    fun setUp() {
        ticketId = UUID.randomUUID()
        ticket = Ticket()
    }

    @Test
    fun `should create ticket when handling CreateTicketCommand`() {
        // Given
        val command =
            CreateTicketCommand(
                ticketId = ticketId,
                title = "Test Ticket",
                description = "Test Description",
                priority = TicketPriority.HIGH,
                assigneeId = "user123",
            )

        // When
        ticket.handle(command)

        // Then
        assertEquals(1L, ticket.version)
        assertTrue(ticket.hasUncommittedEvents())

        val events = ticket.getUncommittedEvents()
        assertEquals(1, events.size)

        val event = events[0] as TicketCreatedEvent
        assertEquals(ticketId, event.ticketId)
        assertEquals("Test Ticket", event.title)
        assertEquals("Test Description", event.description)
        assertEquals(TicketPriority.HIGH, event.priority)
        assertEquals("user123", event.assigneeId)

        // Verify aggregate state
        assertEquals(ticketId, ticket.aggregateId)
        assertEquals("Test Ticket", ticket.getTitle())
        assertEquals("Test Description", ticket.getDescription())
        assertEquals(TicketPriority.HIGH, ticket.getPriority())
        assertEquals("user123", ticket.getAssigneeId())
        assertEquals(TicketStatus.OPEN, ticket.getStatus())
        assertTrue(ticket.isAssigned())
        assertFalse(ticket.isClosed())
    }

    @Test
    fun `should not create ticket when already exists`() {
        // Given
        val command =
            CreateTicketCommand(
                ticketId = ticketId,
                title = "Test Ticket",
                description = "Test Description",
                priority = TicketPriority.MEDIUM,
            )

        ticket.handle(command)
        ticket.markEventsAsCommitted()

        // When/Then
        assertThrows(IllegalArgumentException::class.java) {
            ticket.handle(command)
        }
    }

    @Test
    fun `should assign ticket when handling AssignTicketCommand`() {
        // Given
        createTicket()
        val command = AssignTicketCommand(ticketId, "newAssignee")

        // When
        ticket.handle(command)

        // Then
        assertEquals(2L, ticket.version)
        assertTrue(ticket.hasUncommittedEvents())

        val events = ticket.peekUncommittedEvents()
        assertEquals(1, events.size)

        val event = events[0] as TicketAssignedEvent
        assertEquals(ticketId, event.ticketId)
        assertEquals("newAssignee", event.assigneeId)

        // Verify aggregate state
        assertEquals("newAssignee", ticket.getAssigneeId())
        assertNotNull(ticket.getAssignedAt())
    }

    @Test
    fun `should not assign ticket when already assigned to same person`() {
        // Given
        createTicket(assigneeId = "user123")
        val command = AssignTicketCommand(ticketId, "user123")

        // When/Then
        assertThrows(IllegalArgumentException::class.java) {
            ticket.handle(command)
        }
    }

    @Test
    fun `should not assign closed ticket`() {
        // Given
        createTicket()
        closeTicket()
        val command = AssignTicketCommand(ticketId, "newAssignee")

        // When/Then
        assertThrows(IllegalArgumentException::class.java) {
            ticket.handle(command)
        }
    }

    @Test
    fun `should close ticket when handling CloseTicketCommand`() {
        // Given
        createTicket()
        val command = CloseTicketCommand(ticketId, "Fixed the issue")

        // When
        ticket.handle(command)

        // Then
        assertEquals(2L, ticket.version)
        assertTrue(ticket.hasUncommittedEvents())

        val events = ticket.peekUncommittedEvents()
        assertEquals(1, events.size)

        val event = events[0] as TicketClosedEvent
        assertEquals(ticketId, event.ticketId)
        assertEquals("Fixed the issue", event.resolution)

        // Verify aggregate state
        assertEquals(TicketStatus.CLOSED, ticket.getStatus())
        assertEquals("Fixed the issue", ticket.getResolution())
        assertNotNull(ticket.getClosedAt())
        assertTrue(ticket.isClosed())
    }

    @Test
    fun `should not close already closed ticket`() {
        // Given
        createTicket()
        closeTicket()
        val command = CloseTicketCommand(ticketId, "Another resolution")

        // When/Then
        assertThrows(IllegalArgumentException::class.java) {
            ticket.handle(command)
        }
    }

    @Test
    fun `should rehydrate from events correctly`() {
        // Given
        val events =
            listOf(
                TicketCreatedEvent(
                    ticketId = ticketId,
                    title = "Rehydrated Ticket",
                    description = "Rehydrated Description",
                    priority = TicketPriority.CRITICAL,
                ),
                TicketAssignedEvent(
                    ticketId = ticketId,
                    assigneeId = "assignee123",
                ),
                TicketClosedEvent(
                    ticketId = ticketId,
                    resolution = "Resolved",
                ),
            )

        // When
        ticket.rehydrateFromEvents(events)

        // Then
        assertEquals(3L, ticket.version)
        assertFalse(ticket.hasUncommittedEvents())

        assertEquals(ticketId, ticket.aggregateId)
        assertEquals("Rehydrated Ticket", ticket.getTitle())
        assertEquals("Rehydrated Description", ticket.getDescription())
        assertEquals(TicketPriority.CRITICAL, ticket.getPriority())
        assertEquals("assignee123", ticket.getAssigneeId())
        assertEquals(TicketStatus.CLOSED, ticket.getStatus())
        assertEquals("Resolved", ticket.getResolution())
        assertTrue(ticket.isAssigned())
        assertTrue(ticket.isClosed())
    }

    @Test
    fun `should create and restore from snapshot`() {
        // Given
        createTicket()
        ticket.handle(AssignTicketCommand(ticketId, "assignee123"))
        ticket.markEventsAsCommitted()

        // When
        val snapshot = ticket.createSnapshot()
        assertNotNull(snapshot)

        val newTicket = Ticket()
        newTicket.restoreFromSnapshot(snapshot!!)

        // Then
        assertEquals(ticket.aggregateId, newTicket.aggregateId)
        assertEquals(ticket.getTitle(), newTicket.getTitle())
        assertEquals(ticket.getDescription(), newTicket.getDescription())
        assertEquals(ticket.getPriority(), newTicket.getPriority())
        assertEquals(ticket.getAssigneeId(), newTicket.getAssigneeId())
        assertEquals(ticket.getStatus(), newTicket.getStatus())
    }

    @Test
    fun `should not create snapshot for uninitialized aggregate`() {
        // Given
        val emptyTicket = Ticket()

        // When
        val snapshot = emptyTicket.createSnapshot()

        // Then
        assertNull(snapshot)
    }

    @Test
    fun `should validate command parameters`() {
        // Test CreateTicketCommand validation
        assertThrows(IllegalArgumentException::class.java) {
            CreateTicketCommand(
                ticketId = ticketId,
                title = "",
                description = "Valid description",
                priority = TicketPriority.MEDIUM,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            CreateTicketCommand(
                ticketId = ticketId,
                title = "Valid title",
                description = "",
                priority = TicketPriority.MEDIUM,
            )
        }

        // Test AssignTicketCommand validation
        assertThrows(IllegalArgumentException::class.java) {
            AssignTicketCommand(ticketId, "")
        }

        // Test CloseTicketCommand validation
        assertThrows(IllegalArgumentException::class.java) {
            CloseTicketCommand(ticketId, "")
        }
    }

    private fun createTicket(assigneeId: String? = null) {
        val command =
            CreateTicketCommand(
                ticketId = ticketId,
                title = "Test Ticket",
                description = "Test Description",
                priority = TicketPriority.MEDIUM,
                assigneeId = assigneeId,
            )
        ticket.handle(command)
        ticket.markEventsAsCommitted()
    }

    private fun closeTicket() {
        val command = CloseTicketCommand(ticketId, "Resolved")
        ticket.handle(command)
        ticket.markEventsAsCommitted()
    }
}

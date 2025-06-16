package com.axians.eaf.ticketmanagement.domain.aggregate

import com.axians.eaf.ticketmanagement.domain.command.AssignTicketCommand
import com.axians.eaf.ticketmanagement.domain.command.CloseTicketCommand
import com.axians.eaf.ticketmanagement.domain.command.CreateTicketCommand
import com.axians.eaf.ticketmanagement.domain.command.TicketPriority
import com.axians.eaf.ticketmanagement.domain.event.TicketAssignedEvent
import com.axians.eaf.ticketmanagement.domain.event.TicketClosedEvent
import com.axians.eaf.ticketmanagement.domain.event.TicketCreatedEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD tests for the Ticket aggregate.
 *
 * These tests drive the implementation of the domain model following
 * Domain-Driven Design and Event Sourcing principles.
 */
class TicketTest {
    @Test
    fun `should create ticket when valid create command is handled`() {
        // Given
        val ticketId = UUID.randomUUID()
        val command =
            CreateTicketCommand(
                ticketId = ticketId,
                title = "Fix login bug",
                description = "Users cannot log in with special characters in password",
                priority = TicketPriority.HIGH,
            )
        val ticket = Ticket()

        // When
        ticket.handle(command)

        // Then
        val events = ticket.getUncommittedEvents()
        assertEquals(1, events.size)

        val event = events.first() as TicketCreatedEvent
        assertEquals(ticketId, event.ticketId)
        assertEquals("Fix login bug", event.title)
        assertEquals("Users cannot log in with special characters in password", event.description)
        assertEquals(TicketPriority.HIGH, event.priority)
        assertNull(event.assigneeId)

        // Verify aggregate state
        assertEquals(ticketId, ticket.aggregateId)
        assertEquals("Fix login bug", ticket.getTitle())
        assertEquals("Users cannot log in with special characters in password", ticket.getDescription())
        assertEquals(TicketPriority.HIGH, ticket.getPriority())
        assertEquals(TicketStatus.OPEN, ticket.getStatus())
        assertFalse(ticket.isAssigned())
        assertFalse(ticket.isClosed())
    }

    @Test
    fun `should create ticket with assignee when assignee is provided`() {
        // Given
        val ticketId = UUID.randomUUID()
        val command =
            CreateTicketCommand(
                ticketId = ticketId,
                title = "Update documentation",
                description = "API documentation needs updating",
                priority = TicketPriority.MEDIUM,
                assigneeId = "john.doe@example.com",
            )
        val ticket = Ticket()

        // When
        ticket.handle(command)

        // Then
        val events = ticket.getUncommittedEvents()
        val event = events.first() as TicketCreatedEvent
        assertEquals("john.doe@example.com", event.assigneeId)

        // Verify aggregate state
        assertEquals("john.doe@example.com", ticket.getAssigneeId())
        assertTrue(ticket.isAssigned())
    }

    @Test
    fun `should reject create command when ticket already exists`() {
        // Given
        val ticketId = UUID.randomUUID()
        val ticket = Ticket(ticketId)
        ticket.setVersion(1L) // Simulate existing ticket

        val command =
            CreateTicketCommand(
                ticketId = ticketId,
                title = "Test ticket",
                description = "Test description",
                priority = TicketPriority.LOW,
            )

        // When & Then
        val exception =
            assertThrows<IllegalArgumentException> {
                ticket.handle(command)
            }
        assertEquals("Ticket already exists", exception.message)
    }

    @Test
    fun `should assign ticket when valid assign command is handled`() {
        // Given
        val ticketId = UUID.randomUUID()
        val ticket = createOpenTicket(ticketId)

        val command =
            AssignTicketCommand(
                ticketId = ticketId,
                assigneeId = "jane.smith@example.com",
            )

        // When
        ticket.handle(command)

        // Then
        val events = ticket.getUncommittedEvents()
        assertEquals(1, events.size)

        val event = events.first() as TicketAssignedEvent
        assertEquals(ticketId, event.ticketId)
        assertEquals("jane.smith@example.com", event.assigneeId)

        // Verify aggregate state
        assertEquals("jane.smith@example.com", ticket.getAssigneeId())
        assertTrue(ticket.isAssigned())
    }

    @Test
    fun `should reject assign command when ticket does not exist`() {
        // Given
        val ticketId = UUID.randomUUID()
        val ticket = Ticket()
        val command = AssignTicketCommand(ticketId, "assignee@example.com")

        // When & Then
        val exception =
            assertThrows<IllegalArgumentException> {
                ticket.handle(command)
            }
        assertEquals("Ticket does not exist", exception.message)
    }

    @Test
    fun `should reject assign command when ticket is already closed`() {
        // Given
        val ticketId = UUID.randomUUID()
        val ticket = createClosedTicket(ticketId)

        val command = AssignTicketCommand(ticketId, "assignee@example.com")

        // When & Then
        val exception =
            assertThrows<IllegalArgumentException> {
                ticket.handle(command)
            }
        assertEquals("Cannot assign a closed ticket", exception.message)
    }

    @Test
    fun `should close ticket when valid close command is handled`() {
        // Given
        val ticketId = UUID.randomUUID()
        val ticket = createOpenTicket(ticketId)

        val command =
            CloseTicketCommand(
                ticketId = ticketId,
                resolution = "Fixed by updating authentication logic",
            )

        // When
        ticket.handle(command)

        // Then
        val events = ticket.getUncommittedEvents()
        assertEquals(1, events.size)

        val event = events.first() as TicketClosedEvent
        assertEquals(ticketId, event.ticketId)
        assertEquals("Fixed by updating authentication logic", event.resolution)

        // Verify aggregate state
        assertEquals(TicketStatus.CLOSED, ticket.getStatus())
        assertEquals("Fixed by updating authentication logic", ticket.getResolution())
        assertTrue(ticket.isClosed())
    }

    @Test
    fun `should reject close command when ticket does not exist`() {
        // Given
        val ticketId = UUID.randomUUID()
        val ticket = Ticket()
        val command = CloseTicketCommand(ticketId, "Resolution")

        // When & Then
        val exception =
            assertThrows<IllegalArgumentException> {
                ticket.handle(command)
            }
        assertEquals("Ticket does not exist", exception.message)
    }

    @Test
    fun `should reject close command when ticket is already closed`() {
        // Given
        val ticketId = UUID.randomUUID()
        val ticket = createClosedTicket(ticketId)

        val command = CloseTicketCommand(ticketId, "Another resolution")

        // When & Then
        val exception =
            assertThrows<IllegalArgumentException> {
                ticket.handle(command)
            }
        assertEquals("Ticket is already closed", exception.message)
    }

    @Test
    fun `should rehydrate ticket from events correctly`() {
        // Given
        val ticketId = UUID.randomUUID()
        val ticket = Ticket(ticketId)

        val events =
            listOf(
                TicketCreatedEvent(
                    ticketId = ticketId,
                    title = "Test ticket",
                    description = "Test description",
                    priority = TicketPriority.HIGH,
                    assigneeId = "initial@example.com",
                ),
                TicketAssignedEvent(
                    ticketId = ticketId,
                    assigneeId = "reassigned@example.com",
                ),
                TicketClosedEvent(
                    ticketId = ticketId,
                    resolution = "Test resolution",
                ),
            )

        // When
        ticket.rehydrateFromEvents(events)

        // Then
        assertEquals(ticketId, ticket.aggregateId)
        assertEquals("Test ticket", ticket.getTitle())
        assertEquals("Test description", ticket.getDescription())
        assertEquals(TicketPriority.HIGH, ticket.getPriority())
        assertEquals("reassigned@example.com", ticket.getAssigneeId())
        assertEquals(TicketStatus.CLOSED, ticket.getStatus())
        assertEquals("Test resolution", ticket.getResolution())
        assertEquals(3L, ticket.version)
        assertTrue(ticket.isAssigned())
        assertTrue(ticket.isClosed())
    }

    // Helper methods for test setup

    private fun createOpenTicket(ticketId: UUID): Ticket {
        val ticket = Ticket()
        val createCommand =
            CreateTicketCommand(
                ticketId = ticketId,
                title = "Test ticket",
                description = "Test description",
                priority = TicketPriority.MEDIUM,
            )
        ticket.handle(createCommand)
        ticket.markEventsAsCommitted() // Clear uncommitted events
        return ticket
    }

    private fun createClosedTicket(ticketId: UUID): Ticket {
        val ticket = createOpenTicket(ticketId)
        val closeCommand = CloseTicketCommand(ticketId, "Test resolution")
        ticket.handle(closeCommand)
        ticket.markEventsAsCommitted() // Clear uncommitted events
        return ticket
    }
}

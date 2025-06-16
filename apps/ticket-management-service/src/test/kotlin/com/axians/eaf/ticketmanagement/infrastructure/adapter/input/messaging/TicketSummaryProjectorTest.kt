package com.axians.eaf.ticketmanagement.infrastructure.adapter.input.messaging

import com.axians.eaf.ticketmanagement.domain.command.TicketPriority
import com.axians.eaf.ticketmanagement.domain.event.TicketAssignedEvent
import com.axians.eaf.ticketmanagement.domain.event.TicketClosedEvent
import com.axians.eaf.ticketmanagement.domain.event.TicketCreatedEvent
import com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound.TicketSummaryRepository
import com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound.entity.TicketSummary
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Optional
import java.util.UUID

class TicketSummaryProjectorTest {
    private lateinit var repository: TicketSummaryRepository
    private lateinit var projector: TicketSummaryProjector

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        projector = TicketSummaryProjector(repository)
    }

    @Test
    fun `should create ticket summary when TicketCreatedEvent is received`() {
        // Arrange
        val eventId = UUID.randomUUID()
        val tenantId = "test-tenant"
        val ticketId = UUID.randomUUID()
        val now = Instant.now()

        val event =
            TicketCreatedEvent(
                ticketId = ticketId,
                title = "Test Ticket",
                description = "Test Description",
                priority = TicketPriority.HIGH,
                assigneeId = "user-123",
                createdAt = now,
            )

        every { repository.findById(ticketId) } returns Optional.empty()

        val savedSummarySlot = slot<TicketSummary>()
        every { repository.save(capture(savedSummarySlot)) } returnsArgument 0

        // Act
        projector.handleTicketCreatedEvent(event, eventId, tenantId)

        // Assert
        verify { repository.findById(ticketId) }
        verify { repository.save(any()) }

        val savedSummary = savedSummarySlot.captured
        assertEquals(ticketId, savedSummary.id)
        assertEquals(tenantId, savedSummary.tenantId)
        assertEquals("Test Ticket", savedSummary.title)
        assertEquals("Test Description", savedSummary.description)
        assertEquals("HIGH", savedSummary.priority)
        assertEquals("OPEN", savedSummary.status)
        assertEquals("user-123", savedSummary.assigneeId)
        assertEquals(now, savedSummary.createdAt)
        assertNull(savedSummary.assignedAt)
        assertNull(savedSummary.closedAt)
        assertNull(savedSummary.resolution)
        assertEquals(1L, savedSummary.lastEventSequenceNumber)
    }

    @Test
    fun `should create ticket summary with null assignee when TicketCreatedEvent has no assignee`() {
        // Arrange
        val eventId = UUID.randomUUID()
        val tenantId = "test-tenant"
        val ticketId = UUID.randomUUID()
        val now = Instant.now()

        val event =
            TicketCreatedEvent(
                ticketId = ticketId,
                title = "Unassigned Ticket",
                description = "Unassigned Description",
                priority = TicketPriority.MEDIUM,
                assigneeId = null,
                createdAt = now,
            )

        every { repository.findById(ticketId) } returns Optional.empty()

        val savedSummarySlot = slot<TicketSummary>()
        every { repository.save(capture(savedSummarySlot)) } returnsArgument 0

        // Act
        projector.handleTicketCreatedEvent(event, eventId, tenantId)

        // Assert
        verify { repository.save(any()) }

        val savedSummary = savedSummarySlot.captured
        assertEquals("Unassigned Ticket", savedSummary.title)
        assertEquals("MEDIUM", savedSummary.priority)
        assertNull(savedSummary.assigneeId)
        assertNull(savedSummary.assignedAt)
    }

    @Test
    fun `should update ticket summary when TicketAssignedEvent is received`() {
        // Arrange
        val eventId = UUID.randomUUID()
        val tenantId = "test-tenant"
        val ticketId = UUID.randomUUID()
        val assignedAt = Instant.now()

        val event =
            TicketAssignedEvent(
                ticketId = ticketId,
                assigneeId = "user-456",
                assignedAt = assignedAt,
            )

        val existingSummary =
            TicketSummary(
                id = ticketId,
                tenantId = tenantId,
                title = "Existing Ticket",
                description = "Existing Description",
                priority = "HIGH",
                status = "OPEN",
                assigneeId = null,
                createdAt = Instant.now().minusSeconds(3600),
                lastEventSequenceNumber = 1L,
            )

        every { repository.findById(ticketId) } returns Optional.of(existingSummary)

        val savedSummarySlot = slot<TicketSummary>()
        every { repository.save(capture(savedSummarySlot)) } returnsArgument 0

        // Act
        projector.handleTicketAssignedEvent(event, eventId, tenantId)

        // Assert
        verify { repository.findById(ticketId) }
        verify { repository.save(any()) }

        val savedSummary = savedSummarySlot.captured
        assertEquals(ticketId, savedSummary.id)
        assertEquals("user-456", savedSummary.assigneeId)
        assertEquals(assignedAt, savedSummary.assignedAt)
        assertEquals("OPEN", savedSummary.status) // Status should remain the same
        assertEquals(2L, savedSummary.lastEventSequenceNumber)
    }

    @Test
    fun `should update ticket summary when TicketClosedEvent is received`() {
        // Arrange
        val eventId = UUID.randomUUID()
        val tenantId = "test-tenant"
        val ticketId = UUID.randomUUID()
        val closedAt = Instant.now()

        val event =
            TicketClosedEvent(
                ticketId = ticketId,
                resolution = "Issue resolved successfully",
                closedAt = closedAt,
            )

        val existingSummary =
            TicketSummary(
                id = ticketId,
                tenantId = tenantId,
                title = "Assigned Ticket",
                description = "Assigned Description",
                priority = "HIGH",
                status = "OPEN",
                assigneeId = "user-456",
                createdAt = Instant.now().minusSeconds(7200),
                assignedAt = Instant.now().minusSeconds(3600),
                lastEventSequenceNumber = 2L,
            )

        every { repository.findById(ticketId) } returns Optional.of(existingSummary)

        val savedSummarySlot = slot<TicketSummary>()
        every { repository.save(capture(savedSummarySlot)) } returnsArgument 0

        // Act
        projector.handleTicketClosedEvent(event, eventId, tenantId)

        // Assert
        verify { repository.findById(ticketId) }
        verify { repository.save(any()) }

        val savedSummary = savedSummarySlot.captured
        assertEquals(ticketId, savedSummary.id)
        assertEquals("CLOSED", savedSummary.status)
        assertEquals("Issue resolved successfully", savedSummary.resolution)
        assertEquals(closedAt, savedSummary.closedAt)
        assertEquals("user-456", savedSummary.assigneeId) // Should preserve assignee
        assertEquals(3L, savedSummary.lastEventSequenceNumber)
    }

    @Test
    fun `should skip event if ticket summary not found for assignment`() {
        // Arrange
        val eventId = UUID.randomUUID()
        val tenantId = "test-tenant"
        val ticketId = UUID.randomUUID()

        val event =
            TicketAssignedEvent(
                ticketId = ticketId,
                assigneeId = "user-456",
                assignedAt = Instant.now(),
            )

        every { repository.findById(ticketId) } returns Optional.empty()

        // Act
        projector.handleTicketAssignedEvent(event, eventId, tenantId)

        // Assert
        verify { repository.findById(ticketId) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `should skip event if ticket summary not found for closure`() {
        // Arrange
        val eventId = UUID.randomUUID()
        val tenantId = "test-tenant"
        val ticketId = UUID.randomUUID()

        val event =
            TicketClosedEvent(
                ticketId = ticketId,
                resolution = "Fixed",
                closedAt = Instant.now(),
            )

        every { repository.findById(ticketId) } returns Optional.empty()

        // Act
        projector.handleTicketClosedEvent(event, eventId, tenantId)

        // Assert
        verify { repository.findById(ticketId) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `should handle event sequence properly for multiple events`() {
        // Arrange
        val tenantId = "test-tenant"
        val ticketId = UUID.randomUUID()

        // First event - Create
        val createEvent =
            TicketCreatedEvent(
                ticketId = ticketId,
                title = "Test Ticket",
                description = "Test Description",
                priority = TicketPriority.HIGH,
                assigneeId = null,
                createdAt = Instant.now(),
            )

        every { repository.findById(ticketId) } returns Optional.empty()
        val firstSaveSlot = slot<TicketSummary>()
        every { repository.save(capture(firstSaveSlot)) } returnsArgument 0

        // Act - Create
        projector.handleTicketCreatedEvent(createEvent, UUID.randomUUID(), tenantId)

        // Assert - Create
        val createdSummary = firstSaveSlot.captured
        assertEquals(1L, createdSummary.lastEventSequenceNumber)

        // Arrange - Assign
        val assignEvent =
            TicketAssignedEvent(
                ticketId = ticketId,
                assigneeId = "user-123",
                assignedAt = Instant.now(),
            )

        every { repository.findById(ticketId) } returns Optional.of(createdSummary)
        val secondSaveSlot = slot<TicketSummary>()
        every { repository.save(capture(secondSaveSlot)) } returnsArgument 0

        // Act - Assign
        projector.handleTicketAssignedEvent(assignEvent, UUID.randomUUID(), tenantId)

        // Assert - Assign
        val assignedSummary = secondSaveSlot.captured
        assertEquals(2L, assignedSummary.lastEventSequenceNumber)
        assertEquals("user-123", assignedSummary.assigneeId)
        assertNotNull(assignedSummary.assignedAt)
    }
}

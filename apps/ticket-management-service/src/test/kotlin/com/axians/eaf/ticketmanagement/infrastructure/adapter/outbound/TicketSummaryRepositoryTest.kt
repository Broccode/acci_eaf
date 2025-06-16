package com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound

import com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound.entity.TicketSummary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.UUID

@DataJpaTest
@ActiveProfiles("test")
class TicketSummaryRepositoryTest {
    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var repository: TicketSummaryRepository

    @Test
    fun `should save and find ticket summary by id`() {
        // Arrange
        val ticketId = UUID.randomUUID()
        val ticketSummary =
            TicketSummary(
                id = ticketId,
                tenantId = "test-tenant",
                title = "Test Ticket",
                description = "Test Description",
                priority = "HIGH",
                status = "OPEN",
                assigneeId = "user-123",
                createdAt = Instant.now(),
                lastEventSequenceNumber = 1L,
            )

        // Act
        val saved = repository.save(ticketSummary)
        entityManager.flush()
        entityManager.clear()

        val found = repository.findById(ticketId)

        // Assert
        assertNotNull(saved)
        assertEquals(ticketId, saved.id)
        assertTrue(found.isPresent)
        assertEquals("Test Ticket", found.get().title)
        assertEquals("test-tenant", found.get().tenantId)
    }

    @Test
    fun `should find all tickets by tenant id`() {
        // Arrange
        val tenantId = "test-tenant"
        val ticket1 =
            TicketSummary(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                title = "First Ticket",
                description = "First Description",
                priority = "HIGH",
                status = "OPEN",
                createdAt = Instant.now(),
                lastEventSequenceNumber = 1L,
            )
        val ticket2 =
            TicketSummary(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                title = "Second Ticket",
                description = "Second Description",
                priority = "MEDIUM",
                status = "CLOSED",
                assigneeId = "user-456",
                resolution = "Fixed",
                createdAt = Instant.now(),
                closedAt = Instant.now(),
                lastEventSequenceNumber = 3L,
            )
        val otherTenantTicket =
            TicketSummary(
                id = UUID.randomUUID(),
                tenantId = "other-tenant",
                title = "Other Tenant Ticket",
                description = "Other Description",
                priority = "LOW",
                status = "OPEN",
                createdAt = Instant.now(),
                lastEventSequenceNumber = 1L,
            )

        repository.saveAll(listOf(ticket1, ticket2, otherTenantTicket))
        entityManager.flush()

        // Act
        val results = repository.findByTenantIdOrderByCreatedAtDesc(tenantId)

        // Assert
        assertEquals(2, results.size)
        assertEquals("Second Ticket", results[0].title) // More recent due to ordering
        assertEquals("First Ticket", results[1].title)
        assertTrue(results.all { it.tenantId == tenantId })
    }

    @Test
    fun `should find tickets by tenant and status`() {
        // Arrange
        val tenantId = "test-tenant"
        val openTicket =
            TicketSummary(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                title = "Open Ticket",
                description = "Open Description",
                priority = "HIGH",
                status = "OPEN",
                createdAt = Instant.now(),
                lastEventSequenceNumber = 1L,
            )
        val closedTicket =
            TicketSummary(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                title = "Closed Ticket",
                description = "Closed Description",
                priority = "MEDIUM",
                status = "CLOSED",
                resolution = "Fixed",
                createdAt = Instant.now(),
                closedAt = Instant.now(),
                lastEventSequenceNumber = 2L,
            )

        repository.saveAll(listOf(openTicket, closedTicket))
        entityManager.flush()

        // Act
        val openTickets = repository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, "OPEN")
        val closedTickets = repository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, "CLOSED")

        // Assert
        assertEquals(1, openTickets.size)
        assertEquals("Open Ticket", openTickets[0].title)
        assertEquals("OPEN", openTickets[0].status)

        assertEquals(1, closedTickets.size)
        assertEquals("Closed Ticket", closedTickets[0].title)
        assertEquals("CLOSED", closedTickets[0].status)
    }

    @Test
    fun `should find tickets by assignee`() {
        // Arrange
        val tenantId = "test-tenant"
        val assigneeId = "user-123"
        val assignedTicket =
            TicketSummary(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                title = "Assigned Ticket",
                description = "Assigned Description",
                priority = "HIGH",
                status = "OPEN",
                assigneeId = assigneeId,
                assignedAt = Instant.now(),
                createdAt = Instant.now(),
                lastEventSequenceNumber = 2L,
            )
        val unassignedTicket =
            TicketSummary(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                title = "Unassigned Ticket",
                description = "Unassigned Description",
                priority = "MEDIUM",
                status = "OPEN",
                createdAt = Instant.now(),
                lastEventSequenceNumber = 1L,
            )

        repository.saveAll(listOf(assignedTicket, unassignedTicket))
        entityManager.flush()

        // Act
        val assignedTickets = repository.findByTenantIdAndAssigneeIdOrderByCreatedAtDesc(tenantId, assigneeId)

        // Assert
        assertEquals(1, assignedTickets.size)
        assertEquals("Assigned Ticket", assignedTickets[0].title)
        assertEquals(assigneeId, assignedTickets[0].assigneeId)
    }

    @Test
    fun `should count tickets by status for tenant`() {
        // Arrange
        val tenantId = "test-tenant"
        val tickets =
            listOf(
                TicketSummary(
                    id = UUID.randomUUID(),
                    tenantId = tenantId,
                    title = "Open Ticket 1",
                    description = "Description 1",
                    priority = "HIGH",
                    status = "OPEN",
                    createdAt = Instant.now(),
                    lastEventSequenceNumber = 1L,
                ),
                TicketSummary(
                    id = UUID.randomUUID(),
                    tenantId = tenantId,
                    title = "Open Ticket 2",
                    description = "Description 2",
                    priority = "MEDIUM",
                    status = "OPEN",
                    createdAt = Instant.now(),
                    lastEventSequenceNumber = 1L,
                ),
                TicketSummary(
                    id = UUID.randomUUID(),
                    tenantId = tenantId,
                    title = "Closed Ticket",
                    description = "Description 3",
                    priority = "LOW",
                    status = "CLOSED",
                    resolution = "Fixed",
                    createdAt = Instant.now(),
                    closedAt = Instant.now(),
                    lastEventSequenceNumber = 2L,
                ),
            )

        repository.saveAll(tickets)
        entityManager.flush()

        // Act
        val openCount = repository.countByTenantIdAndStatus(tenantId, "OPEN")
        val closedCount = repository.countByTenantIdAndStatus(tenantId, "CLOSED")

        // Assert
        assertEquals(2, openCount)
        assertEquals(1, closedCount)
    }
}

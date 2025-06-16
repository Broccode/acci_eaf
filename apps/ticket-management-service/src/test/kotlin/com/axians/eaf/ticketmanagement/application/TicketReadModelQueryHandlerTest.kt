package com.axians.eaf.ticketmanagement.application

import com.axians.eaf.ticketmanagement.application.query.GetTicketByIdQuery
import com.axians.eaf.ticketmanagement.application.query.ListTicketsQuery
import com.axians.eaf.ticketmanagement.domain.command.TicketPriority
import com.axians.eaf.ticketmanagement.domain.port.outbound.TicketReadModel
import com.axians.eaf.ticketmanagement.domain.port.outbound.TicketReadModelRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID

class TicketReadModelQueryHandlerTest {
    private lateinit var ticketReadModelRepository: TicketReadModelRepository
    private lateinit var queryHandler: TicketReadModelQueryHandler

    @BeforeEach
    fun setUp() {
        ticketReadModelRepository = mockk()
        queryHandler = TicketReadModelQueryHandler(ticketReadModelRepository)
    }

    @Test
    fun `should handle GetTicketByIdQuery successfully`() =
        runTest {
            // Arrange
            val ticketId = UUID.randomUUID()
            val query = GetTicketByIdQuery(ticketId)
            val ticketReadModel =
                TicketReadModel(
                    id = ticketId,
                    tenantId = "test-tenant",
                    title = "Test Ticket",
                    description = "Test Description",
                    priority = "HIGH",
                    status = "OPEN",
                    assigneeId = "user-123",
                    resolution = null,
                    createdAt = Instant.now(),
                    assignedAt = Instant.now(),
                    closedAt = null,
                    eventSequence = 1L,
                )

            every { ticketReadModelRepository.findById(ticketId) } returns ticketReadModel

            // Act
            val result = queryHandler.handle(query)

            // Assert
            assertNotNull(result)
            assertEquals(ticketId, result.ticketId)
            assertEquals("Test Ticket", result.title)
            assertEquals(TicketPriority.HIGH, result.priority)
            assertEquals("OPEN", result.status)
            verify { ticketReadModelRepository.findById(ticketId) }
        }

    @Test
    fun `should throw exception when ticket not found`() =
        runTest {
            // Arrange
            val ticketId = UUID.randomUUID()
            val query = GetTicketByIdQuery(ticketId)
            every { ticketReadModelRepository.findById(ticketId) } returns null

            // Act & Assert
            assertThrows<IllegalArgumentException> {
                queryHandler.handle(query)
            }
            verify { ticketReadModelRepository.findById(ticketId) }
        }

    @Test
    fun `should handle ListTicketsQuery successfully`() =
        runTest {
            // Arrange
            val query = ListTicketsQuery()
            val tickets =
                listOf(
                    TicketReadModel(
                        id = UUID.randomUUID(),
                        tenantId = "default-tenant",
                        title = "First Ticket",
                        description = "Description",
                        priority = "HIGH",
                        status = "OPEN",
                        assigneeId = null,
                        resolution = null,
                        createdAt = Instant.now(),
                        assignedAt = null,
                        closedAt = null,
                        eventSequence = 1L,
                    ),
                )

            every { ticketReadModelRepository.findByTenantIdOrderByCreatedAtDesc("default-tenant") } returns tickets

            // Act
            val result = queryHandler.handle(query)

            // Assert
            assertEquals(1, result.size)
            assertEquals("First Ticket", result[0].title)
            verify { ticketReadModelRepository.findByTenantIdOrderByCreatedAtDesc("default-tenant") }
        }
}

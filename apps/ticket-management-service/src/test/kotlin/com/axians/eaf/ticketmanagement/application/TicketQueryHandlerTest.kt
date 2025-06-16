package com.axians.eaf.ticketmanagement.application

import com.axians.eaf.eventsourcing.port.AggregateRepository
import com.axians.eaf.ticketmanagement.application.query.GetTicketByIdQuery
import com.axians.eaf.ticketmanagement.application.query.ListTicketsQuery
import com.axians.eaf.ticketmanagement.domain.aggregate.Ticket
import com.axians.eaf.ticketmanagement.domain.command.CreateTicketCommand
import com.axians.eaf.ticketmanagement.domain.command.TicketPriority
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class TicketQueryHandlerTest {
    private lateinit var aggregateRepository: AggregateRepository<Ticket, UUID>
    private lateinit var queryHandler: TicketQueryHandler

    @BeforeEach
    fun setUp() {
        aggregateRepository = mockk()
        queryHandler = TicketQueryHandler(aggregateRepository)
    }

    @Test
    fun `should handle GetTicketByIdQuery successfully`() =
        runTest {
            // Arrange
            val ticketId = UUID.randomUUID()
            val ticket =
                Ticket(ticketId).apply {
                    handle(
                        CreateTicketCommand(
                            ticketId = ticketId,
                            title = "Test Ticket",
                            description = "Test Description",
                            priority = TicketPriority.HIGH,
                            assigneeId = "user-123",
                        ),
                    )
                    markEventsAsCommitted()
                }

            val query = GetTicketByIdQuery(ticketId)
            coEvery { aggregateRepository.load("default-tenant", ticketId) } returns ticket

            // Act
            val result = queryHandler.handle(query)

            // Assert
            assertNotNull(result)
            assertEquals(ticketId, result.ticketId)
            assertEquals("Test Ticket", result.title)
            assertEquals("Test Description", result.description)
            assertEquals(TicketPriority.HIGH, result.priority)
            assertEquals("user-123", result.assigneeId)
            assertEquals("OPEN", result.status)
        }

    @Test
    fun `should throw exception when ticket not found for GetTicketByIdQuery`() =
        runTest {
            // Arrange
            val ticketId = UUID.randomUUID()
            val query = GetTicketByIdQuery(ticketId)
            coEvery { aggregateRepository.load("default-tenant", ticketId) } returns null

            // Act & Assert
            assertThrows<IllegalArgumentException> {
                queryHandler.handle(query)
            }
        }

    @Test
    fun `should handle ListTicketsQuery successfully`() =
        runTest {
            // Arrange
            val query = ListTicketsQuery()

            // Act
            val result = queryHandler.handle(query)

            // Assert
            // Note: The current implementation returns an empty list because
            // EAF AggregateRepository doesn't provide findAll method.
            // This would be implemented with a proper read model/projector.
            assertNotNull(result)
            assertTrue(result.isEmpty())
        }

    @Test
    fun `should return empty list when no tickets exist for ListTicketsQuery`() =
        runTest {
            // Arrange
            val query = ListTicketsQuery()
            // No setup needed since the implementation returns empty list

            // Act
            val result = queryHandler.handle(query)

            // Assert
            assertNotNull(result)
            assertTrue(result.isEmpty())
        }
}

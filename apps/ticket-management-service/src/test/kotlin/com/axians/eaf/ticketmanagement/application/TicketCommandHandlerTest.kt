package com.axians.eaf.ticketmanagement.application

import com.axians.eaf.eventsourcing.port.AggregateRepository
import com.axians.eaf.ticketmanagement.domain.aggregate.Ticket
import com.axians.eaf.ticketmanagement.domain.command.AssignTicketCommand
import com.axians.eaf.ticketmanagement.domain.command.CloseTicketCommand
import com.axians.eaf.ticketmanagement.domain.command.CreateTicketCommand
import com.axians.eaf.ticketmanagement.domain.command.TicketPriority
import com.axians.eaf.ticketmanagement.domain.port.outbound.EventPublisher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class TicketCommandHandlerTest {
    private lateinit var aggregateRepository: AggregateRepository<Ticket, UUID>
    private lateinit var eventPublisher: EventPublisher
    private lateinit var commandHandler: TicketCommandHandler

    @BeforeEach
    fun setUp() {
        aggregateRepository = mockk()
        eventPublisher = mockk(relaxed = true)
        commandHandler = TicketCommandHandler(aggregateRepository, eventPublisher)
    }

    @Test
    fun `should handle CreateTicketCommand successfully`() =
        runTest {
            // Arrange
            val ticketId = UUID.randomUUID()
            val command =
                CreateTicketCommand(
                    ticketId = ticketId,
                    title = "Test Ticket",
                    description = "Test Description",
                    priority = TicketPriority.HIGH,
                    assigneeId = "user-123",
                )

            val mockTicket = Ticket(ticketId)

            coEvery { aggregateRepository.createNew(ticketId) } returns mockTicket
            coEvery { aggregateRepository.save(any(), any()) } returns Unit
            coEvery { eventPublisher.publishEvent(any(), any()) } returns Unit

            // Act
            val result = commandHandler.handle(command)

            // Assert
            assertNotNull(result)
            assertEquals(ticketId, result.ticketId)
            assertEquals("Test Ticket", result.title)
            assertEquals("Test Description", result.description)
            assertEquals(TicketPriority.HIGH, result.priority)
            assertEquals("user-123", result.assigneeId)

            coVerify { aggregateRepository.createNew(ticketId) }
            coVerify { aggregateRepository.save("default-tenant", any()) }
            coVerify { eventPublisher.publishEvent("default-tenant", any()) }
        }

    @Test
    fun `should handle AssignTicketCommand successfully`() =
        runTest {
            // Arrange
            val ticketId = UUID.randomUUID()
            val existingTicket =
                Ticket(ticketId).apply {
                    handle(
                        CreateTicketCommand(
                            ticketId = ticketId,
                            title = "Existing Ticket",
                            description = "Description",
                            priority = TicketPriority.MEDIUM,
                        ),
                    )
                    markEventsAsCommitted() // Clear uncommitted events
                }

            val command =
                AssignTicketCommand(
                    ticketId = ticketId,
                    assigneeId = "new-assignee",
                )

            coEvery { aggregateRepository.load("default-tenant", ticketId) } returns existingTicket
            coEvery { aggregateRepository.save(any(), any()) } returns Unit
            coEvery { eventPublisher.publishEvent(any(), any()) } returns Unit

            // Act
            val result = commandHandler.handle(command)

            // Assert
            assertNotNull(result)
            assertEquals(ticketId, result.ticketId)
            assertEquals("new-assignee", result.assigneeId)

            coVerify { aggregateRepository.load("default-tenant", ticketId) }
            coVerify { aggregateRepository.save("default-tenant", any()) }
            coVerify { eventPublisher.publishEvent("default-tenant", any()) }
        }

    @Test
    fun `should handle CloseTicketCommand successfully`() =
        runTest {
            // Arrange
            val ticketId = UUID.randomUUID()
            val existingTicket =
                Ticket(ticketId).apply {
                    handle(
                        CreateTicketCommand(
                            ticketId = ticketId,
                            title = "Existing Ticket",
                            description = "Description",
                            priority = TicketPriority.MEDIUM,
                        ),
                    )
                    markEventsAsCommitted() // Clear uncommitted events
                }

            val command =
                CloseTicketCommand(
                    ticketId = ticketId,
                    resolution = "Fixed the issue",
                )

            coEvery { aggregateRepository.load("default-tenant", ticketId) } returns existingTicket
            coEvery { aggregateRepository.save(any(), any()) } returns Unit
            coEvery { eventPublisher.publishEvent(any(), any()) } returns Unit

            // Act
            val result = commandHandler.handle(command)

            // Assert
            assertNotNull(result)
            assertEquals(ticketId, result.ticketId)
            assertEquals("Fixed the issue", result.resolution)

            coVerify { aggregateRepository.load("default-tenant", ticketId) }
            coVerify { aggregateRepository.save("default-tenant", any()) }
            coVerify { eventPublisher.publishEvent("default-tenant", any()) }
        }

    @Test
    fun `should throw exception when ticket not found for assign command`() =
        runTest {
            // Arrange
            val ticketId = UUID.randomUUID()
            val command =
                AssignTicketCommand(
                    ticketId = ticketId,
                    assigneeId = "assignee-123",
                )

            coEvery { aggregateRepository.load("default-tenant", ticketId) } returns null

            // Act & Assert
            assertThrows<IllegalArgumentException> {
                commandHandler.handle(command)
            }
        }

    @Test
    fun `should throw exception when ticket not found for close command`() =
        runTest {
            // Arrange
            val ticketId = UUID.randomUUID()
            val command =
                CloseTicketCommand(
                    ticketId = ticketId,
                    resolution = "Cannot close non-existent ticket",
                )

            coEvery { aggregateRepository.load("default-tenant", ticketId) } returns null

            // Act & Assert
            assertThrows<IllegalArgumentException> {
                commandHandler.handle(command)
            }
        }
}

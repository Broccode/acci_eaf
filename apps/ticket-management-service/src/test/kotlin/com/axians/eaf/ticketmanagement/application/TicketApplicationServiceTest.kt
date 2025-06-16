package com.axians.eaf.ticketmanagement.application

import com.axians.eaf.ticketmanagement.application.dto.AssignTicketResponse
import com.axians.eaf.ticketmanagement.application.dto.CloseTicketResponse
import com.axians.eaf.ticketmanagement.application.dto.CreateTicketResponse
import com.axians.eaf.ticketmanagement.application.dto.TicketQueryResponse
import com.axians.eaf.ticketmanagement.application.query.GetTicketByIdQuery
import com.axians.eaf.ticketmanagement.application.query.ListTicketsQuery
import com.axians.eaf.ticketmanagement.application.request.AssignTicketRequest
import com.axians.eaf.ticketmanagement.application.request.CloseTicketRequest
import com.axians.eaf.ticketmanagement.application.request.CreateTicketRequest
import com.axians.eaf.ticketmanagement.domain.command.AssignTicketCommand
import com.axians.eaf.ticketmanagement.domain.command.CloseTicketCommand
import com.axians.eaf.ticketmanagement.domain.command.CreateTicketCommand
import com.axians.eaf.ticketmanagement.domain.command.TicketPriority
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID

class TicketApplicationServiceTest {
    private lateinit var commandHandler: TicketCommandHandler
    private lateinit var queryHandler: TicketQueryHandler
    private lateinit var applicationService: TicketApplicationService

    @BeforeEach
    fun setUp() {
        commandHandler = mockk()
        queryHandler = mockk()
        applicationService = TicketApplicationService(commandHandler, queryHandler)
    }

    @Test
    fun `should create ticket successfully`() =
        runTest {
            // Arrange
            val ticketId = UUID.randomUUID()
            val request =
                CreateTicketRequest(
                    title = "Test Ticket",
                    description = "Test Description",
                    priority = TicketPriority.HIGH,
                    assigneeId = "user-123",
                )

            val expectedResponse =
                CreateTicketResponse(
                    ticketId = ticketId,
                    title = request.title,
                    description = request.description,
                    priority = request.priority,
                    assigneeId = request.assigneeId,
                    createdAt = Instant.now(),
                )

            coEvery { commandHandler.handle(any<CreateTicketCommand>()) } returns expectedResponse

            // Act
            val result = applicationService.createTicket(request)

            // Assert
            assertNotNull(result)
            assertEquals(expectedResponse.title, result.title)
            assertEquals(expectedResponse.description, result.description)
            assertEquals(expectedResponse.priority, result.priority)
            assertEquals(expectedResponse.assigneeId, result.assigneeId)

            coVerify { commandHandler.handle(any<CreateTicketCommand>()) }
        }

    @Test
    fun `should validate required fields for createTicket`() =
        runTest {
            // Act & Assert - expect the exception during object construction
            assertThrows<IllegalArgumentException> {
                CreateTicketRequest(
                    title = "",
                    description = "Description",
                    priority = TicketPriority.HIGH,
                )
            }
        }

    @Test
    fun `should assign ticket successfully`() =
        runTest {
            // Arrange
            val ticketId = UUID.randomUUID()
            val request =
                AssignTicketRequest(
                    assigneeId = "user-456",
                )

            val expectedResponse =
                AssignTicketResponse(
                    ticketId = ticketId,
                    assigneeId = request.assigneeId,
                    assignedAt = Instant.now(),
                )

            coEvery { commandHandler.handle(any<AssignTicketCommand>()) } returns expectedResponse

            // Act
            val result = applicationService.assignTicket(ticketId, request)

            // Assert
            assertNotNull(result)
            assertEquals(expectedResponse.ticketId, result.ticketId)
            assertEquals(expectedResponse.assigneeId, result.assigneeId)

            coVerify { commandHandler.handle(any<AssignTicketCommand>()) }
        }

    @Test
    fun `should close ticket successfully`() =
        runTest {
            // Arrange
            val ticketId = UUID.randomUUID()
            val request =
                CloseTicketRequest(
                    resolution = "Issue resolved",
                )

            val expectedResponse =
                CloseTicketResponse(
                    ticketId = ticketId,
                    resolution = request.resolution,
                    closedAt = Instant.now(),
                )

            coEvery { commandHandler.handle(any<CloseTicketCommand>()) } returns expectedResponse

            // Act
            val result = applicationService.closeTicket(ticketId, request)

            // Assert
            assertNotNull(result)
            assertEquals(expectedResponse.ticketId, result.ticketId)
            assertEquals(expectedResponse.resolution, result.resolution)

            coVerify { commandHandler.handle(any<CloseTicketCommand>()) }
        }

    @Test
    fun `should get ticket by id successfully`() =
        runTest {
            // Arrange
            val ticketId = UUID.randomUUID()
            val expectedResponse =
                TicketQueryResponse(
                    ticketId = ticketId,
                    title = "Test Ticket",
                    description = "Test Description",
                    priority = TicketPriority.HIGH,
                    assigneeId = "user-123",
                    status = "OPEN",
                    createdAt = Instant.now(),
                )

            coEvery { queryHandler.handle(any<GetTicketByIdQuery>()) } returns expectedResponse

            // Act
            val result = applicationService.getTicketById(ticketId)

            // Assert
            assertNotNull(result)
            assertEquals(expectedResponse.ticketId, result.ticketId)
            assertEquals(expectedResponse.title, result.title)

            coVerify { queryHandler.handle(any<GetTicketByIdQuery>()) }
        }

    @Test
    fun `should list all tickets successfully`() =
        runTest {
            // Arrange
            val ticketId1 = UUID.randomUUID()
            val ticketId2 = UUID.randomUUID()
            val expectedResponse =
                listOf(
                    TicketQueryResponse(
                        ticketId = ticketId1,
                        title = "First Ticket",
                        description = "First Description",
                        priority = TicketPriority.HIGH,
                        status = "OPEN",
                        createdAt = Instant.now(),
                    ),
                    TicketQueryResponse(
                        ticketId = ticketId2,
                        title = "Second Ticket",
                        description = "Second Description",
                        priority = TicketPriority.MEDIUM,
                        status = "OPEN",
                        createdAt = Instant.now(),
                    ),
                )

            coEvery { queryHandler.handle(any<ListTicketsQuery>()) } returns expectedResponse

            // Act
            val result = applicationService.listTickets()

            // Assert
            assertNotNull(result)
            assertEquals(2, result.size)
            assertEquals(expectedResponse[0].title, result[0].title)
            assertEquals(expectedResponse[1].title, result[1].title)

            coVerify { queryHandler.handle(any<ListTicketsQuery>()) }
        }
}

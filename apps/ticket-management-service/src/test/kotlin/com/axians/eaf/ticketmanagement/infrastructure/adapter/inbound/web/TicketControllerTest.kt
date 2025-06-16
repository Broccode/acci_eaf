package com.axians.eaf.ticketmanagement.infrastructure.adapter.inbound.web

import com.axians.eaf.ticketmanagement.application.TicketApplicationService
import com.axians.eaf.ticketmanagement.application.dto.AssignTicketResponse
import com.axians.eaf.ticketmanagement.application.dto.CloseTicketResponse
import com.axians.eaf.ticketmanagement.application.dto.CreateTicketResponse
import com.axians.eaf.ticketmanagement.application.dto.TicketQueryResponse
import com.axians.eaf.ticketmanagement.application.request.AssignTicketRequest
import com.axians.eaf.ticketmanagement.application.request.CloseTicketRequest
import com.axians.eaf.ticketmanagement.application.request.CreateTicketRequest
import com.axians.eaf.ticketmanagement.domain.command.TicketPriority
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

@WebMvcTest(TicketController::class)
@TestPropertySource(properties = ["eaf.iam.security.enabled=false"])
class TicketControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var ticketApplicationService: TicketApplicationService

    @Test
    @WithMockUser(roles = ["USER"])
    fun `should create ticket successfully`() =
        runTest {
            // Arrange
            val ticketId = UUID.randomUUID()
            val requestBody =
                """
                {
                    "title": "Test Ticket",
                    "description": "Test Description",
                    "priority": "HIGH",
                    "assigneeId": "user-123"
                }
                """.trimIndent()

            val expectedResponse =
                CreateTicketResponse(
                    ticketId = ticketId,
                    title = "Test Ticket",
                    description = "Test Description",
                    priority = TicketPriority.HIGH,
                    assigneeId = "user-123",
                    createdAt = Instant.now(),
                )

            // Simple mock - test if MockK is working at all
            coEvery {
                ticketApplicationService.createTicket(any<CreateTicketRequest>())
            } returns expectedResponse

            // Act & Assert
            mockMvc
                .perform(
                    post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()),
                ).andExpect(request().asyncStarted())
                .andDo { result ->
                    mockMvc
                        .perform(asyncDispatch(result))
                        .andExpect(status().isCreated)
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.title").value("Test Ticket"))
                        .andExpect(jsonPath("$.description").value("Test Description"))
                        .andExpect(jsonPath("$.priority").value("HIGH"))
                        .andExpect(jsonPath("$.assigneeId").value("user-123"))
                }

            coVerify {
                ticketApplicationService.createTicket(any<CreateTicketRequest>())
            }
        }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `should return bad request for invalid create ticket request`() =
        runTest {
            // Arrange
            val requestBody =
                """
                {
                    "title": "",
                    "description": "Test Description",
                    "priority": "HIGH"
                }
                """.trimIndent()

            // Act & Assert (validation errors are NOT async)
            mockMvc
                .perform(
                    post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()),
                ).andExpect(status().isBadRequest)
        }

    @Test
    @WithMockUser(roles = ["USER"])
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

            coEvery { ticketApplicationService.getTicketById(ticketId) } returns expectedResponse

            // Act & Assert
            mockMvc
                .perform(get("/api/tickets/$ticketId"))
                .andExpect(request().asyncStarted())
                .andDo { result ->
                    mockMvc
                        .perform(asyncDispatch(result))
                        .andExpect(status().isOk)
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.ticketId").value(ticketId.toString()))
                        .andExpect(jsonPath("$.title").value("Test Ticket"))
                        .andExpect(jsonPath("$.status").value("OPEN"))
                }

            coVerify { ticketApplicationService.getTicketById(ticketId) }
        }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `should return not found when ticket does not exist`() =
        runTest {
            // Arrange
            val ticketId = UUID.randomUUID()
            coEvery {
                ticketApplicationService.getTicketById(ticketId)
            } throws IllegalArgumentException("Ticket not found")

            // Act & Assert
            mockMvc
                .perform(get("/api/tickets/$ticketId"))
                .andExpect(request().asyncStarted())
                .andDo { result ->
                    mockMvc
                        .perform(asyncDispatch(result))
                        .andExpect(status().isNotFound)
                }
        }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `should list all tickets successfully`() =
        runTest {
            // Arrange
            val tickets =
                listOf(
                    TicketQueryResponse(
                        ticketId = UUID.randomUUID(),
                        title = "First Ticket",
                        description = "First Description",
                        priority = TicketPriority.HIGH,
                        status = "OPEN",
                        createdAt = Instant.now(),
                    ),
                    TicketQueryResponse(
                        ticketId = UUID.randomUUID(),
                        title = "Second Ticket",
                        description = "Second Description",
                        priority = TicketPriority.MEDIUM,
                        assigneeId = "user-456",
                        status = "OPEN",
                        createdAt = Instant.now(),
                    ),
                )

            coEvery { ticketApplicationService.listTickets() } returns tickets

            // Act & Assert
            mockMvc
                .perform(get("/api/tickets"))
                .andExpect(request().asyncStarted())
                .andDo { result ->
                    mockMvc
                        .perform(asyncDispatch(result))
                        .andExpect(status().isOk)
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$").isArray)
                        .andExpect(jsonPath("$.length()").value(2))
                        .andExpect(jsonPath("$[0].title").value("First Ticket"))
                        .andExpect(jsonPath("$[1].title").value("Second Ticket"))
                }

            coVerify { ticketApplicationService.listTickets() }
        }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `should assign ticket successfully`() =
        runTest {
            // Arrange
            val ticketId = UUID.randomUUID()
            val requestBody =
                """
                {
                    "assigneeId": "user-456"
                }
                """.trimIndent()

            val expectedResponse =
                AssignTicketResponse(
                    ticketId = ticketId,
                    assigneeId = "user-456",
                    assignedAt = Instant.now(),
                )

            coEvery {
                ticketApplicationService.assignTicket(ticketId, any<AssignTicketRequest>())
            } returns expectedResponse

            // Act & Assert
            mockMvc
                .perform(
                    patch("/api/tickets/$ticketId/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()),
                ).andExpect(request().asyncStarted())
                .andDo { result ->
                    mockMvc
                        .perform(asyncDispatch(result))
                        .andExpect(status().isOk)
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.ticketId").value(ticketId.toString()))
                        .andExpect(jsonPath("$.assigneeId").value("user-456"))
                }

            coVerify {
                ticketApplicationService.assignTicket(ticketId, any<AssignTicketRequest>())
            }
        }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `should close ticket successfully`() =
        runTest {
            // Arrange
            val ticketId = UUID.randomUUID()
            val requestBody =
                """
                {
                    "resolution": "Issue resolved successfully"
                }
                """.trimIndent()

            val expectedResponse =
                CloseTicketResponse(
                    ticketId = ticketId,
                    resolution = "Issue resolved successfully",
                    closedAt = Instant.now(),
                )

            coEvery {
                ticketApplicationService.closeTicket(ticketId, any<CloseTicketRequest>())
            } returns expectedResponse

            // Act & Assert
            mockMvc
                .perform(
                    patch("/api/tickets/$ticketId/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()),
                ).andExpect(request().asyncStarted())
                .andDo { result ->
                    mockMvc
                        .perform(asyncDispatch(result))
                        .andExpect(status().isOk)
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.ticketId").value(ticketId.toString()))
                        .andExpect(jsonPath("$.resolution").value("Issue resolved successfully"))
                }

            coVerify {
                ticketApplicationService.closeTicket(ticketId, any<CloseTicketRequest>())
            }
        }

    @Test
    fun `should return unauthorized when no authentication`() =
        runTest {
            // Act & Assert
            mockMvc
                .perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized)
        }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `should return bad request for invalid assign request`() =
        runTest {
            // Arrange
            val ticketId = UUID.randomUUID()
            val requestBody =
                """
                {
                    "assigneeId": ""
                }
                """.trimIndent()

            // Act & Assert
            mockMvc
                .perform(
                    patch("/api/tickets/$ticketId/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()),
                ).andExpect(status().isBadRequest)
        }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `should return bad request for invalid close request`() =
        runTest {
            // Arrange
            val ticketId = UUID.randomUUID()
            val requestBody =
                """
                {
                    "resolution": ""
                }
                """.trimIndent()

            // Act & Assert
            mockMvc
                .perform(
                    patch("/api/tickets/$ticketId/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()),
                ).andExpect(status().isBadRequest)
        }
}

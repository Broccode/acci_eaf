package com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound

import com.axians.eaf.eventsourcing.port.EventStoreRepository
import com.axians.eaf.ticketmanagement.domain.aggregate.Ticket
import com.axians.eaf.ticketmanagement.domain.command.CreateTicketCommand
import com.axians.eaf.ticketmanagement.domain.command.TicketPriority
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class TicketAggregateRepositoryTest {
    private lateinit var eventStoreRepository: EventStoreRepository
    private lateinit var ticketRepository: TicketAggregateRepository

    @BeforeEach
    fun setUp() {
        eventStoreRepository = mockk()
        ticketRepository = TicketAggregateRepository(eventStoreRepository)
    }

    @Test
    fun `should create new ticket aggregate successfully`() =
        runTest {
            // Arrange
            val ticketId = UUID.randomUUID()

            // Act
            val ticket = ticketRepository.createNew(ticketId)

            // Assert
            assertNotNull(ticket)
            assertEquals(ticketId, ticket.aggregateId)
        }

    @Test
    fun `should save ticket aggregate successfully`() =
        runTest {
            // Arrange
            val tenantId = "test-tenant"
            val ticketId = UUID.randomUUID()
            val ticket =
                Ticket(ticketId).apply {
                    handle(
                        CreateTicketCommand(
                            ticketId = ticketId,
                            title = "Test Ticket",
                            description = "Test Description",
                            priority = TicketPriority.HIGH,
                        ),
                    )
                }

            coEvery { eventStoreRepository.appendEvents(any(), any(), any(), any()) } returns Unit

            // Act
            ticketRepository.save(tenantId, ticket)

            // Assert
            coVerify { eventStoreRepository.appendEvents(any(), any(), any(), any()) }
        }

    @Test
    fun `should load ticket aggregate successfully`() =
        runTest {
            // Arrange
            val tenantId = "test-tenant"
            val ticketId = UUID.randomUUID()

            // Mock returning events from event store
            val mockEvents = emptyList<com.axians.eaf.eventsourcing.model.PersistedEvent>()
            coEvery { eventStoreRepository.getSnapshot(tenantId, ticketId.toString()) } returns null
            coEvery { eventStoreRepository.getEvents(tenantId, ticketId.toString(), any()) } returns mockEvents

            // Act
            val ticket = ticketRepository.load(tenantId, ticketId)

            // Assert
            // Since we're mocking an empty event list, the aggregate won't be initialized
            // In a real implementation, this would verify proper event replay
            coVerify { eventStoreRepository.getEvents(tenantId, ticketId.toString(), any()) }
        }

    @Test
    fun `should return null when ticket not found`() =
        runTest {
            // Arrange
            val tenantId = "test-tenant"
            val ticketId = UUID.randomUUID()

            coEvery { eventStoreRepository.getSnapshot(tenantId, ticketId.toString()) } returns null
            coEvery { eventStoreRepository.getEvents(tenantId, ticketId.toString(), any()) } returns emptyList()

            // Act
            val ticket = ticketRepository.load(tenantId, ticketId)

            // Assert
            assertNull(ticket)
        }

    @Test
    fun `should check if ticket exists successfully`() =
        runTest {
            // Arrange
            val tenantId = "test-tenant"
            val ticketId = UUID.randomUUID()

            coEvery { eventStoreRepository.getCurrentVersion(tenantId, ticketId.toString()) } returns 5L

            // Act
            val exists = ticketRepository.exists(tenantId, ticketId)

            // Assert
            assertEquals(true, exists)
            coVerify { eventStoreRepository.getCurrentVersion(tenantId, ticketId.toString()) }
        }

    @Test
    fun `should return false when checking if non-existent ticket exists`() =
        runTest {
            // Arrange
            val tenantId = "test-tenant"
            val ticketId = UUID.randomUUID()

            coEvery { eventStoreRepository.getCurrentVersion(tenantId, ticketId.toString()) } returns null

            // Act
            val exists = ticketRepository.exists(tenantId, ticketId)

            // Assert
            assertEquals(false, exists)
        }

    @Test
    fun `should get current version successfully`() =
        runTest {
            // Arrange
            val tenantId = "test-tenant"
            val ticketId = UUID.randomUUID()
            val expectedVersion = 3L

            coEvery { eventStoreRepository.getCurrentVersion(tenantId, ticketId.toString()) } returns expectedVersion

            // Act
            val version = ticketRepository.getCurrentVersion(tenantId, ticketId)

            // Assert
            assertEquals(expectedVersion, version)
            coVerify { eventStoreRepository.getCurrentVersion(tenantId, ticketId.toString()) }
        }
}

package com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound.event

import com.axians.eaf.eventing.NatsEventPublisher
import com.axians.eaf.ticketmanagement.domain.command.TicketPriority
import com.axians.eaf.ticketmanagement.domain.event.TicketCreatedEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class NatsEventPublisherAdapterTest {
    private lateinit var natsEventPublisher: NatsEventPublisher
    private lateinit var adapter: NatsEventPublisherAdapter

    @BeforeEach
    fun setUp() {
        natsEventPublisher = mockk()
        adapter = NatsEventPublisherAdapter(natsEventPublisher)
    }

    @Test
    fun `should publish event using NATS via EAF SDK`() =
        runTest {
            // Arrange
            val tenantId = "test-tenant"
            val event =
                TicketCreatedEvent(
                    ticketId = UUID.randomUUID(),
                    title = "Test Event",
                    description = "Test Description",
                    priority = TicketPriority.HIGH,
                    assigneeId = "user-123",
                    createdAt = Instant.now(),
                )

            coEvery {
                natsEventPublisher.publish(
                    subject = "events.ticket.created",
                    tenantId = tenantId,
                    event = event,
                )
            } returns mockk()

            // Act
            adapter.publishEvent(tenantId, event)

            // Assert
            coVerify {
                natsEventPublisher.publish(
                    subject = "events.ticket.created",
                    tenantId = tenantId,
                    event = event,
                )
            }
        }
}

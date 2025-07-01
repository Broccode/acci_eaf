@file:Suppress("UnusedPrivateProperty")

package com.axians.eaf.eventsourcing.adapter

import com.axians.eaf.eventsourcing.TestConfiguration
import com.axians.eaf.eventsourcing.example.aggregate.Ticket
import com.axians.eaf.eventsourcing.example.aggregate.TicketRepository
import com.axians.eaf.eventsourcing.example.command.AssignTicketCommand
import com.axians.eaf.eventsourcing.example.command.CloseTicketCommand
import com.axians.eaf.eventsourcing.example.command.CreateTicketCommand
import com.axians.eaf.eventsourcing.example.command.TicketPriority
import com.axians.eaf.eventsourcing.exception.OptimisticLockingFailureException
import com.axians.eaf.eventsourcing.port.EventStoreRepository
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootTest
@Import(TestConfiguration::class)
@Testcontainers
class AggregateRepositoryIntegrationTest {
    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:15")
                .withDatabaseName("eaf_test")
                .withUsername("test")
                .withPassword("test")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.flyway.enabled") { "true" }
        }
    }

    @Autowired private lateinit var eventStoreRepository: EventStoreRepository

    @Autowired private lateinit var objectMapper: ObjectMapper

    private lateinit var ticketRepository: TicketRepository

    @BeforeEach
    fun setUp() {
        ticketRepository = TicketRepository(eventStoreRepository, objectMapper)
    }

    @Test
    fun `should save and load aggregate successfully`() =
        runBlocking {
            // Given
            val tenantId = "tenant-123"
            val ticketId = UUID.randomUUID()
            val ticket = Ticket()

            // Create ticket
            ticket.handle(
                CreateTicketCommand(
                    ticketId = ticketId,
                    title = "Integration Test Ticket",
                    description = "Testing repository integration",
                    priority = TicketPriority.HIGH,
                    assigneeId = "user123",
                ),
            )

            // When - Save the aggregate
            ticketRepository.save(tenantId, ticket)

            // Then - Load and verify
            val loadedTicket = ticketRepository.load(tenantId, ticketId)
            assertNotNull(loadedTicket)
            assertEquals(ticketId, loadedTicket!!.aggregateId)
            assertEquals("Integration Test Ticket", loadedTicket.getTitle())
            assertEquals("Testing repository integration", loadedTicket.getDescription())
            assertEquals(TicketPriority.HIGH, loadedTicket.getPriority())
            assertEquals("user123", loadedTicket.getAssigneeId())
            assertEquals(1L, loadedTicket.version)
            assertFalse(loadedTicket.hasUncommittedEvents())
        }

    @Test
    fun `should handle full lifecycle - create, modify, save, load`() =
        runBlocking {
            // Given
            val tenantId = "tenant-456"
            val ticketId = UUID.randomUUID()
            val ticket = Ticket()

            // Create ticket
            ticket.handle(
                CreateTicketCommand(
                    ticketId = ticketId,
                    title = "Lifecycle Test",
                    description = "Testing full lifecycle",
                    priority = TicketPriority.MEDIUM,
                ),
            )

            // Save initial version
            ticketRepository.save(tenantId, ticket)

            // Load and modify
            val loadedTicket = ticketRepository.load(tenantId, ticketId)!!
            loadedTicket.handle(AssignTicketCommand(ticketId, "assignee123"))
            loadedTicket.handle(CloseTicketCommand(ticketId, "Completed successfully"))

            // Save modifications
            ticketRepository.save(tenantId, loadedTicket)

            // Load final version and verify
            val finalTicket = ticketRepository.load(tenantId, ticketId)!!
            assertEquals(3L, finalTicket.version)
            assertEquals("assignee123", finalTicket.getAssigneeId())
            assertTrue(finalTicket.isClosed())
            assertEquals("Completed successfully", finalTicket.getResolution())
        }

    @Test
    fun `should enforce optimistic locking on concurrent modifications`() =
        runBlocking {
            // Given
            val tenantId = "tenant-789"
            val ticketId = UUID.randomUUID()
            val ticket = Ticket()

            ticket.handle(
                CreateTicketCommand(
                    ticketId = ticketId,
                    title = "Concurrency Test",
                    description = "Testing optimistic locking",
                    priority = TicketPriority.LOW,
                ),
            )

            ticketRepository.save(tenantId, ticket)

            // Load the same aggregate in two different instances
            val ticket1 = ticketRepository.load(tenantId, ticketId)!!
            val ticket2 = ticketRepository.load(tenantId, ticketId)!!

            // Modify both instances
            ticket1.handle(AssignTicketCommand(ticketId, "user1"))
            ticket2.handle(AssignTicketCommand(ticketId, "user2"))

            // Save first instance - should succeed
            ticketRepository.save(tenantId, ticket1)

            // Save second instance - should fail with optimistic locking exception
            assertThrows(OptimisticLockingFailureException::class.java) {
                runBlocking { ticketRepository.save(tenantId, ticket2) }
            }
        }

    @Test
    fun `should handle concurrent modifications with proper exception details`() =
        runBlocking {
            // Given
            val tenantId = "tenant-concurrent"
            val ticketId = UUID.randomUUID()
            val ticket = Ticket()

            ticket.handle(
                CreateTicketCommand(
                    ticketId = ticketId,
                    title = "Concurrent Test",
                    description = "Testing concurrent access",
                    priority = TicketPriority.CRITICAL,
                ),
            )

            ticketRepository.save(tenantId, ticket)

            // Simulate concurrent access
            val latch = CountDownLatch(2)
            val results = mutableListOf<Result<Unit>>()

            val future1 =
                CompletableFuture.runAsync {
                    try {
                        runBlocking {
                            val t1 = ticketRepository.load(tenantId, ticketId)!!
                            t1.handle(AssignTicketCommand(ticketId, "user1"))
                            // Add small delay to increase chance of conflict
                            Thread.sleep(50)
                            ticketRepository.save(tenantId, t1)
                            results.add(Result.success(Unit))
                        }
                    } catch (e: Exception) {
                        results.add(Result.failure(e))
                    } finally {
                        latch.countDown()
                    }
                }

            val future2 =
                CompletableFuture.runAsync {
                    try {
                        runBlocking {
                            val t2 = ticketRepository.load(tenantId, ticketId)!!
                            t2.handle(AssignTicketCommand(ticketId, "user2"))
                            // Add small delay to increase chance of conflict
                            Thread.sleep(50)
                            ticketRepository.save(tenantId, t2)
                            results.add(Result.success(Unit))
                        }
                    } catch (e: Exception) {
                        results.add(Result.failure(e))
                    } finally {
                        latch.countDown()
                    }
                }

            // Wait for both operations to complete
            assertTrue(latch.await(5, TimeUnit.SECONDS))

            // One should succeed, one should fail
            val successes = results.count { it.isSuccess }
            val failures = results.count { it.isFailure }

            assertEquals(1, successes, "Exactly one operation should succeed")
            assertEquals(1, failures, "Exactly one operation should fail")

            // The failure should be an OptimisticLockingFailureException
            val failure = results.first { it.isFailure }
            assertTrue(failure.exceptionOrNull() is OptimisticLockingFailureException)

            val exception = failure.exceptionOrNull() as OptimisticLockingFailureException
            assertEquals(tenantId, exception.tenantId)
            assertEquals(ticketId.toString(), exception.aggregateId)
        }

    @Test
    fun `should return null when loading non-existent aggregate`() =
        runBlocking {
            // Given
            val tenantId = "tenant-nonexistent"
            val nonExistentId = UUID.randomUUID()

            // When
            val result = ticketRepository.load(tenantId, nonExistentId)

            // Then
            assertNull(result)
        }

    @Test
    fun `should check aggregate existence correctly`() =
        runBlocking {
            // Given
            val tenantId = "tenant-exists"
            val ticketId = UUID.randomUUID()
            val ticket = Ticket()

            // Initially should not exist
            assertFalse(ticketRepository.exists(tenantId, ticketId))

            // Create and save ticket
            ticket.handle(
                CreateTicketCommand(
                    ticketId = ticketId,
                    title = "Existence Test",
                    description = "Testing existence check",
                    priority = TicketPriority.MEDIUM,
                ),
            )

            ticketRepository.save(tenantId, ticket)

            // Now should exist
            assertTrue(ticketRepository.exists(tenantId, ticketId))
        }

    @Test
    fun `should get current version correctly`() =
        runBlocking {
            // Given
            val tenantId = "tenant-version"
            val ticketId = UUID.randomUUID()
            val ticket = Ticket()

            // Initially should return null
            assertNull(ticketRepository.getCurrentVersion(tenantId, ticketId))

            // Create ticket
            ticket.handle(
                CreateTicketCommand(
                    ticketId = ticketId,
                    title = "Version Test",
                    description = "Testing version tracking",
                    priority = TicketPriority.HIGH,
                ),
            )

            ticketRepository.save(tenantId, ticket)

            // Should return version 1
            assertEquals(1L, ticketRepository.getCurrentVersion(tenantId, ticketId))

            // Modify and save again
            val loadedTicket = ticketRepository.load(tenantId, ticketId)!!
            loadedTicket.handle(AssignTicketCommand(ticketId, "assignee"))
            ticketRepository.save(tenantId, loadedTicket)

            // Should return version 2
            assertEquals(2L, ticketRepository.getCurrentVersion(tenantId, ticketId))
        }

    @Test
    fun `should enforce tenant isolation`() =
        runBlocking {
            // Given
            val tenant1 = "tenant-1"
            val tenant2 = "tenant-2"
            val ticketId = UUID.randomUUID()

            // Create ticket in tenant1
            val ticket1 = Ticket()
            ticket1.handle(
                CreateTicketCommand(
                    ticketId = ticketId,
                    title = "Tenant 1 Ticket",
                    description = "Only visible to tenant 1",
                    priority = TicketPriority.MEDIUM,
                ),
            )
            ticketRepository.save(tenant1, ticket1)

            // Try to load from tenant2 - should not find it
            val resultFromTenant2 = ticketRepository.load(tenant2, ticketId)
            assertNull(resultFromTenant2)

            // Should exist in tenant1
            assertTrue(ticketRepository.exists(tenant1, ticketId))

            // Should not exist in tenant2
            assertFalse(ticketRepository.exists(tenant2, ticketId))
        }
}

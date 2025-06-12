# EAF Event Sourcing SDK Guide

## Dependency Coordinates

```kotlin
// build.gradle.kts
implementation("com.axians.eaf:eaf-eventsourcing-sdk")
implementation("com.axians.eaf:eaf-eventing-sdk") // for auto publishing
```

The EAF Event Sourcing SDK provides powerful abstractions for building event-sourced aggregates in
Kotlin/Spring, seamlessly integrating with the EAF Event Store and Eventing Bus.

## Core Abstractions

### @EafAggregate

Marks a class as an event-sourced aggregate root:

```kotlin
@EafAggregate
class Ticket private constructor() : AbstractAggregateRoot<String>() {
    // Aggregate implementation
}
```

### @AggregateIdentifier

Identifies the unique identifier property of an aggregate:

```kotlin
@EafAggregate
class Ticket private constructor() : AbstractAggregateRoot<String>() {
    @AggregateIdentifier
    private lateinit var ticketId: String

    // ... rest of implementation
}
```

### @EafCommandHandler

Marks methods that handle commands and produce events:

```kotlin
@EafCommandHandler
constructor(command: CreateTicketCommand) : this() {
    val event = TicketCreatedEvent(
        ticketId = command.ticketId,
        title = command.title,
        description = command.description,
        priority = command.priority,
        tenantId = command.tenantId
    )
    apply(event)
}
```

### @EafEventSourcingHandler

Marks methods that apply events to rebuild aggregate state:

```kotlin
@EafEventSourcingHandler
private fun on(event: TicketCreatedEvent) {
    this.ticketId = event.ticketId
    this.title = event.title
    this.description = event.description
    this.priority = event.priority
    this.status = TicketStatus.OPEN
    this.tenantId = event.tenantId
}
```

## Step-by-Step Guide: Building a Ticket Aggregate

### 1. Define Commands

Commands represent intentions to change aggregate state:

```kotlin
data class CreateTicketCommand(
    val ticketId: String,
    val title: String,
    val description: String,
    val priority: TicketPriority,
    val tenantId: String,
)

data class AssignTicketCommand(
    val ticketId: String,
    val assigneeId: String,
    val tenantId: String,
)

data class CloseTicketCommand(
    val ticketId: String,
    val resolution: String,
    val tenantId: String,
)
```

### 2. Define Events

Events represent what actually happened:

```kotlin
data class TicketCreatedEvent(
    val ticketId: String,
    val title: String,
    val description: String,
    val priority: TicketPriority,
    val tenantId: String,
) : DomainEvent

data class TicketAssignedEvent(
    val ticketId: String,
    val assigneeId: String,
    val tenantId: String,
) : DomainEvent

data class TicketClosedEvent(
    val ticketId: String,
    val resolution: String,
    val tenantId: String,
) : DomainEvent
```

### 3. Implement the Aggregate

```kotlin
@EafAggregate
class Ticket private constructor() : AbstractAggregateRoot<String>() {

    @AggregateIdentifier
    private lateinit var ticketId: String
    private lateinit var title: String
    private lateinit var description: String
    private lateinit var priority: TicketPriority
    private lateinit var status: TicketStatus
    private var assigneeId: String? = null
    private lateinit var tenantId: String

    // Creation command handler
    @EafCommandHandler
    constructor(command: CreateTicketCommand) : this() {
        val event = TicketCreatedEvent(
            ticketId = command.ticketId,
            title = command.title,
            description = command.description,
            priority = command.priority,
            tenantId = command.tenantId
        )
        apply(event)
    }

    // Assignment command handler
    @EafCommandHandler
    fun handle(command: AssignTicketCommand) {
        require(this.tenantId == command.tenantId) {
            "Ticket belongs to different tenant"
        }
        require(status == TicketStatus.OPEN) {
            "Can only assign open tickets"
        }

        val event = TicketAssignedEvent(
            ticketId = command.ticketId,
            assigneeId = command.assigneeId,
            tenantId = command.tenantId
        )
        apply(event)
    }

    // Closing command handler
    @EafCommandHandler
    fun handle(command: CloseTicketCommand) {
        require(this.tenantId == command.tenantId) {
            "Ticket belongs to different tenant"
        }
        require(status != TicketStatus.CLOSED) {
            "Ticket is already closed"
        }

        val event = TicketClosedEvent(
            ticketId = command.ticketId,
            resolution = command.resolution,
            tenantId = command.tenantId
        )
        apply(event)
    }

    // Event sourcing handlers
    @EafEventSourcingHandler
    private fun on(event: TicketCreatedEvent) {
        this.ticketId = event.ticketId
        this.title = event.title
        this.description = event.description
        this.priority = event.priority
        this.status = TicketStatus.OPEN
        this.tenantId = event.tenantId
    }

    @EafEventSourcingHandler
    private fun on(event: TicketAssignedEvent) {
        this.assigneeId = event.assigneeId
    }

    @EafEventSourcingHandler
    private fun on(event: TicketClosedEvent) {
        this.status = TicketStatus.CLOSED
    }

    // Public getters for testing and querying
    fun getTicketId(): String = ticketId
    fun getTitle(): String = title
    fun getStatus(): TicketStatus = status
    fun getAssigneeId(): String? = assigneeId
    fun getTenantId(): String = tenantId
}

enum class TicketStatus { OPEN, ASSIGNED, CLOSED }
enum class TicketPriority { LOW, MEDIUM, HIGH, CRITICAL }
```

### 4. Create Repository

```kotlin
@Repository
class TicketRepository(
    eventStoreRepository: EventStoreRepository,
    eventPublisher: EventPublisher,
) : AbstractAggregateRepository<Ticket, String>(
    aggregateType = Ticket::class.java,
    eventStoreRepository = eventStoreRepository,
    eventPublisher = eventPublisher,
)
```

### 5. Use in Application Service

```kotlin
@Service
@Transactional
class TicketApplicationService(
    private val ticketRepository: TicketRepository,
) {

    fun createTicket(command: CreateTicketCommand) {
        val ticket = Ticket(command)
        ticketRepository.save(ticket)
    }

    fun assignTicket(command: AssignTicketCommand) {
        val ticket = ticketRepository.load(command.ticketId, command.tenantId)
        ticket.handle(command)
        ticketRepository.save(ticket)
    }

    fun closeTicket(command: CloseTicketCommand) {
        val ticket = ticketRepository.load(command.ticketId, command.tenantId)
        ticket.handle(command)
        ticketRepository.save(ticket)
    }
}
```

## Testing Event-Sourced Aggregates

### Unit Testing with Given-When-Then

```kotlin
class TicketTest {

    @Test
    fun `should create ticket when valid command is provided`() {
        // Given
        val command = CreateTicketCommand(
            ticketId = "ticket-123",
            title = "Fix bug",
            description = "Critical bug in payment system",
            priority = TicketPriority.CRITICAL,
            tenantId = "tenant-1"
        )

        // When
        val ticket = Ticket(command)

        // Then
        assertEquals("ticket-123", ticket.getTicketId())
        assertEquals("Fix bug", ticket.getTitle())
        assertEquals(TicketStatus.OPEN, ticket.getStatus())
        assertEquals("tenant-1", ticket.getTenantId())

        // Verify events
        val events = ticket.getUncommittedChanges()
        assertEquals(1, events.size)
        assertTrue(events[0] is TicketCreatedEvent)
    }

    @Test
    fun `should assign ticket when ticket is open`() {
        // Given
        val createCommand = CreateTicketCommand(
            ticketId = "ticket-123",
            title = "Fix bug",
            description = "Critical bug",
            priority = TicketPriority.HIGH,
            tenantId = "tenant-1"
        )
        val ticket = Ticket(createCommand)
        ticket.markChangesAsCommitted()

        val assignCommand = AssignTicketCommand(
            ticketId = "ticket-123",
            assigneeId = "user-456",
            tenantId = "tenant-1"
        )

        // When
        ticket.handle(assignCommand)

        // Then
        assertEquals("user-456", ticket.getAssigneeId())

        val events = ticket.getUncommittedChanges()
        assertEquals(1, events.size)
        assertTrue(events[0] is TicketAssignedEvent)
    }

    @Test
    fun `should throw exception when assigning closed ticket`() {
        // Given
        val ticket = createAndCloseTicket()
        val assignCommand = AssignTicketCommand(
            ticketId = "ticket-123",
            assigneeId = "user-456",
            tenantId = "tenant-1"
        )

        // When & Then
        assertThrows<IllegalArgumentException> {
            ticket.handle(assignCommand)
        }
    }

    private fun createAndCloseTicket(): Ticket {
        val createCommand = CreateTicketCommand(
            ticketId = "ticket-123",
            title = "Fix bug",
            description = "Critical bug",
            priority = TicketPriority.HIGH,
            tenantId = "tenant-1"
        )
        val ticket = Ticket(createCommand)
        ticket.markChangesAsCommitted()

        val closeCommand = CloseTicketCommand(
            ticketId = "ticket-123",
            resolution = "Fixed in v1.2.3",
            tenantId = "tenant-1"
        )
        ticket.handle(closeCommand)
        ticket.markChangesAsCommitted()

        return ticket
    }
}
```

### Integration Testing

```kotlin
@SpringBootTest
@Testcontainers
class TicketRepositoryIntegrationTest {

    @Container
    static val postgres = PostgreSQLContainer("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test")

    @Autowired
    private lateinit var ticketRepository: TicketRepository

    @Test
    @Transactional
    fun `should save and load ticket aggregate`() {
        // Given
        val createCommand = CreateTicketCommand(
            ticketId = "ticket-123",
            title = "Test ticket",
            description = "Test description",
            priority = TicketPriority.MEDIUM,
            tenantId = "tenant-1"
        )
        val originalTicket = Ticket(createCommand)

        // When
        ticketRepository.save(originalTicket)

        // Then
        val loadedTicket = ticketRepository.load("ticket-123", "tenant-1")
        assertEquals(originalTicket.getTicketId(), loadedTicket.getTicketId())
        assertEquals(originalTicket.getTitle(), loadedTicket.getTitle())
        assertEquals(originalTicket.getStatus(), loadedTicket.getStatus())
        assertEquals(originalTicket.getTenantId(), loadedTicket.getTenantId())
    }

    @Test
    fun `should handle optimistic locking exception`() {
        // Given
        val createCommand = CreateTicketCommand(
            ticketId = "ticket-456",
            title = "Test ticket",
            description = "Test description",
            priority = TicketPriority.LOW,
            tenantId = "tenant-1"
        )
        val ticket1 = Ticket(createCommand)
        ticketRepository.save(ticket1)

        // Load same aggregate in two different instances
        val ticket2 = ticketRepository.load("ticket-456", "tenant-1")
        val ticket3 = ticketRepository.load("ticket-456", "tenant-1")

        // When
        val assignCommand1 = AssignTicketCommand("ticket-456", "user-1", "tenant-1")
        val assignCommand2 = AssignTicketCommand("ticket-456", "user-2", "tenant-1")

        ticket2.handle(assignCommand1)
        ticket3.handle(assignCommand2)

        ticketRepository.save(ticket2)

        // Then
        assertThrows<OptimisticLockingFailureException> {
            ticketRepository.save(ticket3)
        }
    }
}
```

## Best Practices

### 1. Keep Aggregates Small

Focus on single business concepts and avoid large, complex aggregates.

### 2. Design for Idempotency

Ensure command handlers can be safely retried:

```kotlin
@EafCommandHandler
fun handle(command: AssignTicketCommand) {
    // Check if already assigned to same user
    if (assigneeId == command.assigneeId) {
        return // Idempotent - no event needed
    }

    // Apply change
    apply(TicketAssignedEvent(ticketId, command.assigneeId, tenantId))
}
```

### 3. Use Business-Meaningful Events

Events should capture business intent, not technical details:

```kotlin
// Good
data class TicketPriorityEscalatedEvent(
    val ticketId: String,
    val newPriority: TicketPriority,
    val reason: String
)

// Avoid
data class TicketUpdatedEvent(
    val ticketId: String,
    val fieldName: String,
    val newValue: Any
)
```

### 4. Handle Event Evolution

Use versioning strategies for long-lived event streams:

```kotlin
// V1 Event
data class TicketCreatedEventV1(
    val ticketId: String,
    val title: String
)

// V2 Event with additional field
data class TicketCreatedEventV2(
    val ticketId: String,
    val title: String,
    val priority: TicketPriority = TicketPriority.MEDIUM
)
```

### 5. Test Event Sourcing Scenarios

Always test aggregate reconstruction from events:

```kotlin
@Test
fun `should reconstruct aggregate from events`() {
    // Given
    val events = listOf(
        TicketCreatedEvent("ticket-1", "Bug", "Description", TicketPriority.HIGH, "tenant-1"),
        TicketAssignedEvent("ticket-1", "user-1", "tenant-1"),
        TicketClosedEvent("ticket-1", "Fixed", "tenant-1")
    )

    // When
    val ticket = recreateFromEvents(events)

    // Then
    assertEquals(TicketStatus.CLOSED, ticket.getStatus())
    assertEquals("user-1", ticket.getAssigneeId())
}
```

## Integration with EAF Services

The SDK automatically handles:

- **Tenant Context**: All operations respect the current tenant context
- **Event Publishing**: Events are automatically published to NATS after successful persistence
- **Optimistic Locking**: Version conflicts are detected and appropriate exceptions thrown
- **Snapshots**: Large aggregates can benefit from automatic snapshot creation

## Further Reading

- [CQRS/Event Sourcing Principles](./cqrs-es.md)
- [Hexagonal Architecture Guide](./hexagonal.md)
- [Test-Driven Development](./tdd.md)

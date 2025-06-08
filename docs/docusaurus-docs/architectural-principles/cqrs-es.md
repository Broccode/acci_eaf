# CQRS/Event Sourcing in ACCI EAF

Command Query Responsibility Segregation (CQRS) and Event Sourcing (ES) are core architectural
patterns in ACCI EAF that enable scalable, auditable, and resilient applications.

## CQRS Overview

CQRS separates read and write operations:

### Command Side (Write)

Handles business operations that change state:

```kotlin
// Command
data class CreateTenantCommand(
    val tenantName: String,
    val adminEmail: String
)

// Command Handler
@Component
class CreateTenantCommandHandler(
    private val tenantRepository: TenantRepository,
    private val eventPublisher: EventPublisher
) {
    fun handle(command: CreateTenantCommand) {
        val tenant = Tenant.create(command.tenantName, command.adminEmail)
        tenantRepository.save(tenant)
        eventPublisher.publish(tenant.domainEvents)
    }
}
```

### Query Side (Read)

Optimized for data retrieval:

```kotlin
// Query
data class GetTenantQuery(val tenantId: TenantId)

// Query Handler
@Component
class GetTenantQueryHandler(
    private val tenantReadModel: TenantReadModelRepository
) {
    fun handle(query: GetTenantQuery): TenantView? {
        return tenantReadModel.findById(query.tenantId)
    }
}
```

## Event Sourcing

Instead of storing current state, Event Sourcing persists the sequence of events that led to the
current state.

### Event Store Structure

Events are stored in PostgreSQL with the EAF Event Store SDK:

```kotlin
data class PersistedEvent(
    val eventId: UUID,
    val streamId: String,
    val aggregateId: String,
    val sequenceNumber: Long,
    val tenantId: String,
    val eventType: String,
    val eventPayload: JsonNode,
    val timestamp: Instant
)
```

### Aggregate Reconstruction

Aggregates are rebuilt by replaying events:

```kotlin
class Tenant private constructor() {
    private val domainEvents = mutableListOf<DomainEvent>()

    companion object {
        fun fromEvents(events: List<DomainEvent>): Tenant {
            val tenant = Tenant()
            events.forEach { tenant.apply(it) }
            return tenant
        }
    }

    private fun apply(event: DomainEvent) {
        when (event) {
            is TenantCreatedEvent -> {
                // Apply state changes
            }
        }
    }
}
```

## EAF Integration

### Event Store SDK

The EAF Event Store SDK provides:

- Atomic event appends with optimistic concurrency
- Event stream retrieval
- Snapshot support
- Tenant isolation

### Eventing SDK

The EAF Eventing SDK handles:

- Publishing events to NATS/JetStream
- Event consumption with idempotency
- Dead letter queue management
- Tenant-scoped subjects

### Axon Framework Integration

ACCI EAF leverages Axon Framework for:

- Command and query buses
- Event handlers and projectors
- Saga orchestration
- Testing utilities

## Projectors and Read Models

Projectors consume events and update read models:

```kotlin
@EventHandler
class TenantProjector(
    private val tenantViewRepository: TenantViewRepository
) {
    fun on(event: TenantCreatedEvent) {
        val tenantView = TenantView(
            id = event.tenantId,
            name = event.tenantName,
            adminEmail = event.adminEmail,
            createdAt = event.timestamp
        )
        tenantViewRepository.save(tenantView)
    }
}
```

## Benefits

- **Scalability**: Read and write sides can be scaled independently
- **Auditability**: Complete history of all changes
- **Flexibility**: Multiple read models from the same events
- **Resilience**: Events are immutable and provide recovery capabilities
- **Temporal Queries**: Query state at any point in time

## Best Practices

1. **Keep events immutable**: Never modify published events
2. **Version events**: Handle schema evolution gracefully
3. **Idempotent projectors**: Handle duplicate events safely
4. **Separate concerns**: Don't mix command and query logic
5. **Monitor event streams**: Track processing lag and failures

## Learn More

For hands-on guidance on implementing event-sourced aggregates with the EAF SDK, see the
comprehensive [EAF Event Sourcing SDK Guide](./eaf-eventsourcing-sdk.md).

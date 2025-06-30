---
sidebar_position: 15
title: Frequently Asked Questions
---

# Frequently Asked Questions

This FAQ addresses common questions, challenges, and solutions when working with Axon Framework in
the EAF platform.

## 🔍 General Concepts

### Q: When should I use Event Sourcing vs traditional CRUD?

**A:** Use Event Sourcing when you need:

- **Complete audit trails** (financial, medical, compliance systems)
- **Complex business logic** with many invariants
- **Temporal queries** ("What was the state on date X?")
- **Integration with multiple systems** that react to changes
- **Debugging capabilities** to understand how you reached current state

Avoid Event Sourcing for:

- Simple CRUD operations with minimal business logic
- Performance-critical applications requiring sub-millisecond latency
- Read-heavy systems with few writes
- Teams without event-driven architecture expertise

### Q: What's the difference between Commands and Events?

**A:**

- **Commands** represent **intent** - what you want to happen

  - Can be rejected (validation fails)
  - Present/future tense: `CreateUser`, `UpdateEmail`
  - Mutable (can be modified before processing)

- **Events** represent **facts** - what actually happened
  - Cannot be rejected (already occurred)
  - Past tense: `UserCreated`, `EmailUpdated`
  - Immutable (cannot be changed once persisted)

```kotlin
// Command (intent)
data class CreateUserCommand(val email: String, val name: String)

// Event (fact)
data class UserCreatedEvent(val userId: String, val email: String, val name: String)
```

### Q: How does CQRS relate to Event Sourcing?

**A:** They're complementary but independent patterns:

- **CQRS** separates read and write operations
- **Event Sourcing** stores events instead of current state
- You can use CQRS without Event Sourcing
- Event Sourcing naturally leads to CQRS (events for writes, projections for reads)

In EAF:

- Commands update aggregates (write side)
- Events build projections (read side)
- Query handlers read from projections, not aggregates

## 🏗️ Architecture Questions

### Q: How big should my aggregates be?

**A:** Follow these guidelines:

✅ **Good aggregate size:**

- Represents one business concept
- Maintains clear invariants
- Usually handles 5-15 business operations
- Can be loaded and processed in memory efficiently

❌ **Aggregate too large signs:**

- Handles unrelated business concepts
- Has hundreds of events
- Takes long time to load
- Complex internal state management

❌ **Aggregate too small signs:**

- Just simple CRUD operations
- No business invariants
- Always used together with other aggregates

**Example:** User aggregate should handle user profile, roles, and status - but not orders or
licenses.

### Q: Should I put business logic in Command Handlers or Aggregates?

**A:** **Aggregates** should contain business logic:

```kotlin
// ✅ Business logic in aggregate
@Aggregate
class User {
    @CommandHandler
    fun handle(command: ChangeEmailCommand) {
        require(isActive) { "Cannot change email of inactive user" }
        require(email != command.newEmail) { "Email unchanged" }

        AggregateLifecycle.apply(EmailChangedEvent(...))
    }
}

// ✅ External validation in command handler
@Component
class UserCommandHandler {
    @CommandHandler
    fun handle(command: ChangeEmailCommand) {
        emailService.validateEmailAvailable(command.newEmail) // External check

        val user = repository.load(command.userId)
        user.handle(command) // Delegate to aggregate
        repository.save(user)
    }
}
```

**Rule:** Aggregates = business invariants, Command Handlers = external coordination

### Q: How do I handle cross-aggregate validation?

**A:** Use **eventual consistency** patterns:

1. **Policy/Saga Pattern** (recommended):

```kotlin
@ProcessingGroup("email-uniqueness-policy")
class EmailUniquenessPolicy {
    @SagaOrchestrationStart
    @EventHandler
    fun on(event: UserEmailChangeRequestedEvent) {
        // Check if email is available
        if (emailService.isEmailTaken(event.newEmail)) {
            commandGateway.send(RejectEmailChangeCommand(event.userId, "Email taken"))
        } else {
            commandGateway.send(ApproveEmailChangeCommand(event.userId))
        }
    }
}
```

2. **Read Model Validation**:

```kotlin
@CommandHandler
fun handle(command: CreateUserCommand) {
    // Check projection (eventually consistent)
    val existingUser = userProjectionRepository.findByEmail(command.email)
    if (existingUser != null) {
        throw EmailAlreadyExistsException()
    }
    // Proceed with creation...
}
```

## 🛠️ Implementation Questions

### Q: How do I handle concurrency conflicts?

**A:** Axon provides optimistic concurrency control automatically:

```kotlin
// This will throw ConcurrencyException if aggregate was modified
@CommandHandler
fun handle(command: UpdateUserCommand) {
    val user = repository.load(command.userId) // Loads with version
    user.update(command)
    repository.save(user) // Fails if version changed
}

// Handle conflicts in calling code
try {
    commandGateway.sendAndWait(command)
} catch (e: ConcurrencyException) {
    // Retry with exponential backoff
    retryService.retryWithBackoff { commandGateway.sendAndWait(command) }
}
```

### Q: How do I migrate event schemas?

**A:** Use **Upcasting** for event evolution:

```kotlin
// V1 Event
data class UserCreatedEvent(
    val userId: String,
    val email: String,
    val name: String // Single name field
)

// V2 Event
data class UserCreatedEvent(
    val userId: String,
    val email: String,
    val firstName: String, // Split name
    val lastName: String
)

// Upcaster
@Component
class UserCreatedEventUpcaster : SingleEventUpcaster() {

    override fun canUpcast(intermediateRepresentation: IntermediateEventRepresentation): Boolean {
        return "UserCreatedEvent" == intermediateRepresentation.type.name &&
               intermediateRepresentation.type.revision == null
    }

    override fun doUpcast(intermediateRepresentation: IntermediateEventRepresentation): IntermediateEventRepresentation {
        val data = intermediateRepresentation.data as MutableMap<String, Any>
        val fullName = data.remove("name") as String
        val nameParts = fullName.split(" ", limit = 2)

        data["firstName"] = nameParts[0]
        data["lastName"] = nameParts.getOrElse(1) { "" }

        return intermediateRepresentation.upcastPayload(
            SerializedType.forName("UserCreatedEvent", "1"),
            JsonNode(data)
        )
    }
}
```

### Q: How do I test aggregates effectively?

**A:** Use Axon's test fixtures:

```kotlin
class UserTest {
    private lateinit var fixture: AggregateTestFixture<User>

    @BeforeEach
    fun setUp() {
        fixture = AggregateTestFixture(User::class.java)
    }

    @Test
    fun `should create user when command is valid`() {
        fixture.givenNoPriorActivity()
            .when(CreateUserCommand("john@example.com", "John", "Doe"))
            .expectEvents(UserCreatedEvent("generated-id", "john@example.com", "John", "Doe"))
    }

    @Test
    fun `should reject duplicate email`() {
        fixture.given(UserCreatedEvent("user-1", "john@example.com", "John", "Doe"))
            .when(CreateUserCommand("john@example.com", "Jane", "Smith"))
            .expectException(EmailAlreadyExistsException::class.java)
    }
}
```

## 🚀 Performance Questions

### Q: How do I optimize aggregate loading performance?

**A:** Use several strategies:

1. **Snapshots** for aggregates with many events:

```kotlin
@Aggregate(snapshotTriggerDefinition = "userSnapshotTrigger")
class User {
    // Aggregate implementation
}

@Bean
fun userSnapshotTrigger(): SnapshotTriggerDefinition {
    return EventCountSnapshotTriggerDefinition(
        snapshotter = snapshotter,
        threshold = 50 // Snapshot every 50 events
    )
}
```

2. **Caching** frequently accessed aggregates:

```kotlin
@Configuration
class CachingConfiguration {
    @Bean
    fun cache(): Cache {
        return CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build()
    }
}
```

3. **Batch processing** for multiple operations:

```kotlin
// Process commands in batches
val commands = listOf(cmd1, cmd2, cmd3)
commands.forEach { command ->
    commandGateway.send(command) // Async
}
```

### Q: How do I handle projection performance issues?

**A:** Optimize projections with these techniques:

1. **Batch updates**:

```kotlin
@EventHandler
@BatchSize(100)
fun on(events: List<UserCreatedEvent>) {
    val projections = events.map { event ->
        UserProjection(event.userId, event.email, event.firstName, event.lastName)
    }
    userProjectionRepository.saveAll(projections)
}
```

2. **Separate processing groups**:

```kotlin
@ProcessingGroup("user-projections") // Fast, critical projections
class UserProjectionHandler { }

@ProcessingGroup("user-analytics") // Slower, non-critical analytics
class UserAnalyticsHandler { }
```

3. **Database optimization**:

```sql
-- Add indexes for common queries
CREATE INDEX idx_user_email ON user_projections(tenant_id, email);
CREATE INDEX idx_user_department ON user_projections(tenant_id, department);
```

## 🔧 EAF-Specific Questions

### Q: How does multi-tenancy work with Axon?

**A:** EAF provides automatic tenant isolation:

```kotlin
// Tenant context is automatically propagated
@CommandHandler
fun handle(command: CreateUserCommand, @MetaData("tenant_id") tenantId: String) {
    // tenantId is automatically available from security context
    require(command.tenantId == tenantId) { "Tenant mismatch" }
}

// Events include tenant metadata
@EventHandler
fun on(event: UserCreatedEvent, @MetaData("tenant_id") tenantId: String) {
    // Build tenant-specific projections
    val projection = UserProjection(
        userId = event.userId,
        tenantId = tenantId, // Ensures isolation
        email = event.email
    )
}
```

### Q: How do I integrate with NATS?

**A:** Use EAF's automatic NATS integration:

```kotlin
@Component
@ProcessingGroup("nats-publisher")
class NatsEventPublisher {

    @EventHandler
    suspend fun on(event: UserCreatedEvent, @MetaData("tenant_id") tenantId: String) {
        natsEventPublisher.publish(
            subject = "users.created",
            tenantId = tenantId,
            event = event
        )
    }
}
```

### Q: How do I handle security context in commands?

**A:** Security context is automatically propagated:

```kotlin
@CommandHandler
@PreAuthorize("hasRole('USER_MANAGEMENT')")
fun handle(
    command: CreateUserCommand,
    @MetaData("user_id") currentUserId: String,
    @MetaData("tenant_id") tenantId: String
) {
    // Security context is automatically available
    logger.info("User {} creating new user in tenant {}", currentUserId, tenantId)
}
```

## 🐛 Troubleshooting

### Q: Why am I getting "No suitable constructor found" errors?

**A:** Common causes and solutions:

1. **Missing no-arg constructor**:

```kotlin
@Aggregate
class User {
    constructor() // ✅ Required no-arg constructor

    @CommandHandler
    constructor(command: CreateUserCommand) { } // ✅ Command constructor
}
```

2. **Wrong annotation placement**:

```kotlin
// ❌ Wrong
@CommandHandler
class User(command: CreateUserCommand) { }

// ✅ Correct
@Aggregate
class User {
    @CommandHandler
    constructor(command: CreateUserCommand) { }
}
```

### Q: Why aren't my events being published to NATS?

**A:** Check these common issues:

1. **Processing group configuration**:

```kotlin
@Component
@ProcessingGroup("nats-publisher") // ✅ Must specify processing group
class NatsEventPublisher { }
```

2. **Async handling**:

```kotlin
@EventHandler
suspend fun on(event: UserCreatedEvent) { // ✅ Use suspend for async
    natsEventPublisher.publish(...)
}
```

3. **Error handling**:

```kotlin
@EventHandler
suspend fun on(event: UserCreatedEvent) {
    try {
        natsEventPublisher.publish(...)
    } catch (e: Exception) {
        logger.error("Failed to publish to NATS", e)
        // Don't rethrow - let event processing continue
    }
}
```

### Q: Why are my projections not updating?

**A:** Common causes:

1. **Missing @ProcessingGroup**:

```kotlin
@Component
@ProcessingGroup("user-projections") // ✅ Required
class UserProjectionHandler { }
```

2. **Transaction configuration**:

```kotlin
@Component
@ProcessingGroup("user-projections")
@Transactional // ✅ Ensure transactions work properly
class UserProjectionHandler { }
```

3. **Event processor not started**:

```bash
# Check processor status
curl http://localhost:8080/actuator/eventprocessors
```

### Q: Why am I getting concurrency exceptions frequently?

**A:** Solutions:

1. **Implement retry logic**:

```kotlin
@Retryable(value = [ConcurrencyException::class], maxAttempts = 3)
fun handleCommand(command: UpdateUserCommand) {
    commandGateway.sendAndWait(command)
}
```

2. **Use smaller aggregates** to reduce contention

3. **Implement optimistic UI** patterns for better user experience

## 📚 Best Practices Summary

### ✅ Do's

- Keep aggregates focused on single business concepts
- Use events to communicate between aggregates
- Implement comprehensive testing with Axon Test
- Handle errors gracefully with proper exception hierarchy
- Use snapshots for aggregates with many events
- Implement proper logging and monitoring
- Follow EAF multi-tenant patterns

### ❌ Don'ts

- Don't put unrelated business logic in same aggregate
- Don't call other aggregates directly from aggregates
- Don't ignore concurrency exceptions
- Don't skip testing of business invariants
- Don't use synchronous calls between aggregates
- Don't forget to handle event ordering
- Don't bypass tenant isolation checks

## 🔗 Additional Resources

- [Axon Framework Reference Guide](https://docs.axoniq.io/axon-framework-reference/)
- [EAF Architecture Documentation](../../architecture/domain-driven-design.md)
- [Event Sourcing Patterns](../../architecture/cqrs-event-sourcing.md)
- [Multi-tenancy Guide](../context-propagation.md)
- [NATS Integration](../nats-event-publishing.md)

---

💡 **Still have questions?** Check the [Troubleshooting Guide](./troubleshooting.md) or ask in the
`#eaf-architecture` Slack channel!

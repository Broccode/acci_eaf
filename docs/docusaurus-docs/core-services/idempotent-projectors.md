# Building Idempotent Projectors with EAF Eventing SDK

## Overview

Projectors are a crucial component in event-driven architectures that consume domain events and
build read models (queryable views) of your data. The EAF Eventing SDK provides powerful
abstractions to build **idempotent projectors** that ensure events are processed exactly once, even
in the face of message redeliveries or system failures.

## Key Features

- **Automatic Idempotency**: Events are processed exactly once per projector using a database-backed
  tracking mechanism
- **Transactional Safety**: All database operations are wrapped in transactions to ensure
  consistency
- **Tenant Isolation**: Strict enforcement of tenant data isolation in multi-tenant environments
- **Error Handling**: Robust error handling with automatic retries and dead-letter queue support
- **Simple API**: Clean annotation-based API that integrates seamlessly with Spring Boot

## Quick Start

### 1. Add Dependencies

Ensure your project includes the EAF Eventing SDK:

```kotlin
dependencies {
    implementation("com.axians.eaf:eaf-eventing-sdk")
    implementation("com.axians.eaf:eaf-eventsourcing-sdk") // For database migrations
}
```

### 2. Database Setup

The SDK requires a `processed_events` table to track processed events. This is automatically created
by the migration in `eaf-eventsourcing-sdk`:

```sql
CREATE TABLE processed_events (
    projector_name VARCHAR(255) NOT NULL,
    event_id UUID NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    PRIMARY KEY (projector_name, event_id, tenant_id)
);
```

### 3. Create Your First Projector

```kotlin
@Component
class UserProjector(
    private val userReadModelRepository: UserReadModelRepository,
) {
    private val logger = LoggerFactory.getLogger(UserProjector::class.java)

    @EafProjectorEventHandler(
        subject = "user.events.created",
        projectorName = "user-projector",
        durableName = "user-projector-consumer",
        maxDeliver = 3,
        ackWait = 30_000L,
    )
    fun handleUserCreatedEvent(
        event: UserCreatedEvent,
        eventId: UUID,
        tenantId: String,
    ) {
        logger.info("Processing UserCreatedEvent for user {} in tenant {}", event.userId, tenantId)

        // This code only executes if the event hasn't been processed before
        val readModel = UserReadModel(
            id = event.userId,
            email = event.email,
            name = event.name,
            tenantId = tenantId,
            createdAt = event.createdAt,
        )

        userReadModelRepository.save(readModel)

        logger.info("Successfully created read model for user {}", event.userId)
    }
}
```

## Annotation Reference

### @EafProjectorEventHandler

The `@EafProjectorEventHandler` annotation provides a high-level abstraction for building idempotent
projectors.

#### Parameters

| Parameter       | Type          | Required | Default                    | Description                            |
| --------------- | ------------- | -------- | -------------------------- | -------------------------------------- |
| `subject`       | String        | Yes      | -                          | NATS subject to listen to              |
| `projectorName` | String        | No       | `{ClassName}-{MethodName}` | Unique name for the projector          |
| `durableName`   | String        | No       | `{projectorName}-consumer` | NATS durable consumer name             |
| `deliverPolicy` | DeliverPolicy | No       | `All`                      | NATS delivery policy                   |
| `maxDeliver`    | Int           | No       | 3                          | Maximum delivery attempts              |
| `ackWait`       | Long          | No       | 30000                      | Acknowledgment timeout in milliseconds |
| `maxAckPending` | Int           | No       | 1000                       | Maximum pending acknowledgments        |
| `eventType`     | KClass        | No       | Auto-detected              | Event type for deserialization         |

#### Method Signature

Projector methods must have exactly this signature:

```kotlin
fun handleEvent(event: EventType, eventId: UUID, tenantId: String)
```

- **event**: The deserialized event object
- **eventId**: Unique identifier for the event (used for idempotency)
- **tenantId**: Tenant identifier (used for data isolation)

## Idempotency Strategy

### How It Works

1. **Event Reception**: When a message arrives, the SDK extracts the `eventId` and `tenantId`
2. **Idempotency Check**: The SDK queries the `processed_events` table to check if this event has
   been processed by this projector
3. **Processing**: If not processed, the projector method is invoked within a database transaction
4. **Tracking**: After successful processing, an entry is inserted into `processed_events`
5. **Acknowledgment**: The NATS message is acknowledged

### Transaction Boundaries

All operations within a projector method execution are wrapped in a single database transaction:

```
BEGIN TRANSACTION
  1. Check processed_events table
  2. Execute projector method (your business logic)
  3. Insert into processed_events table
COMMIT TRANSACTION
```

If any step fails, the entire transaction is rolled back and the message is negatively acknowledged
for retry.

## Error Handling

### Automatic Retries

When a projector method throws an exception:

1. The transaction is rolled back
2. The NATS message is negatively acknowledged (`nak()`)
3. NATS will redeliver the message according to the `maxDeliver` setting
4. After `maxDeliver` attempts, the message goes to the dead-letter queue

### Dead Letter Queue

Configure a dead-letter queue in your NATS JetStream configuration to handle poison pill messages:

```yaml
nats:
  jetstream:
    consumers:
      user-projector-consumer:
        max_deliver: 3
        ack_wait: 30s
        deliver_policy: all
        # Messages that fail max_deliver times go to DLQ
```

### Custom Error Handling

For custom error handling within your projector:

```kotlin
@EafProjectorEventHandler(subject = "user.events.created")
fun handleUserCreatedEvent(event: UserCreatedEvent, eventId: UUID, tenantId: String) {
    try {
        // Your business logic here
        processUserCreation(event, tenantId)
    } catch (ValidationException e) {
        // Log validation errors but don't retry
        logger.warn("Validation failed for event {}: {}", eventId, e.message)
        // Don't rethrow - this will ack the message and not retry
    } catch (Exception e) {
        // Log unexpected errors and rethrow for retry
        logger.error("Unexpected error processing event {}: {}", eventId, e.message, e)
        throw e
    }
}
```

## Tenant Isolation

### Automatic Enforcement

The SDK automatically enforces tenant isolation by:

1. Extracting `tenantId` from the event message
2. Including `tenantId` in all `processed_events` queries
3. Passing `tenantId` to your projector method

### Best Practices

Always include `tenantId` in your read model operations:

```kotlin
// ✅ Good - includes tenantId
userRepository.findByIdAndTenantId(userId, tenantId)
userRepository.saveWithTenantId(readModel, tenantId)

// ❌ Bad - missing tenantId (data leak risk)
userRepository.findById(userId)
userRepository.save(readModel)
```

## Read Model Management

### Schema Design

Design your read models with tenant isolation in mind:

```sql
CREATE TABLE user_read_models (
    id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id, tenant_id)
);

-- Index for efficient tenant-scoped queries
CREATE INDEX idx_user_read_models_tenant ON user_read_models (tenant_id);
```

### Repository Pattern

Use the repository pattern with tenant-aware methods:

```kotlin
interface UserReadModelRepository {
    fun save(userReadModel: UserReadModel, tenantId: String)
    fun findByIdAndTenantId(id: String, tenantId: String): UserReadModel?
    fun findAllByTenantId(tenantId: String): List<UserReadModel>
    fun deleteByIdAndTenantId(id: String, tenantId: String)
}
```

## Advanced Patterns

### Multiple Event Types

Handle multiple event types in a single projector:

```kotlin
@Component
class UserProjector(private val repository: UserReadModelRepository) {

    @EafProjectorEventHandler(subject = "user.events.created")
    fun handleUserCreated(event: UserCreatedEvent, eventId: UUID, tenantId: String) {
        val readModel = UserReadModel(
            id = event.userId,
            email = event.email,
            name = event.name,
            tenantId = tenantId,
        )
        repository.save(readModel, tenantId)
    }

    @EafProjectorEventHandler(subject = "user.events.updated")
    fun handleUserUpdated(event: UserUpdatedEvent, eventId: UUID, tenantId: String) {
        val existing = repository.findByIdAndTenantId(event.userId, tenantId)
            ?: throw IllegalStateException("User not found: ${event.userId}")

        val updated = existing.copy(
            email = event.email,
            name = event.name,
            updatedAt = event.updatedAt,
        )
        repository.save(updated, tenantId)
    }
}
```

### Conditional Processing

Skip processing based on business logic:

```kotlin
@EafProjectorEventHandler(subject = "user.events.created")
fun handleUserCreated(event: UserCreatedEvent, eventId: UUID, tenantId: String) {
    // Skip processing for test users
    if (event.email.endsWith("@test.com")) {
        logger.debug("Skipping test user: {}", event.email)
        return
    }

    // Process normal users
    val readModel = UserReadModel(...)
    repository.save(readModel, tenantId)
}
```

### Upsert Operations

For projectors that might receive events out of order:

```kotlin
@EafProjectorEventHandler(subject = "user.events.updated")
fun handleUserUpdated(event: UserUpdatedEvent, eventId: UUID, tenantId: String) {
    val existing = repository.findByIdAndTenantId(event.userId, tenantId)

    if (existing == null) {
        // Create if doesn't exist
        val readModel = UserReadModel(
            id = event.userId,
            email = event.email,
            name = event.name,
            tenantId = tenantId,
        )
        repository.save(readModel, tenantId)
    } else {
        // Update if exists and newer
        if (event.version > existing.version) {
            val updated = existing.copy(
                email = event.email,
                name = event.name,
                version = event.version,
            )
            repository.save(updated, tenantId)
        }
    }
}
```

## Monitoring and Observability

### Logging

The SDK provides structured logging at key points:

- Event reception and processing start
- Idempotency check results
- Processing success/failure
- Transaction commit/rollback

### Metrics

Monitor these key metrics:

- **Processing Rate**: Events processed per second
- **Error Rate**: Percentage of failed processing attempts
- **Retry Rate**: Percentage of messages that require retries
- **DLQ Rate**: Messages sent to dead-letter queue

### Health Checks

Implement health checks for your projectors:

```kotlin
@Component
class ProjectorHealthIndicator(
    private val processedEventRepository: ProcessedEventRepository,
) : HealthIndicator {

    override fun health(): Health {
        return try {
            // Check if we can query the processed_events table
            processedEventRepository.isEventProcessed("health-check", UUID.randomUUID(), "health")
            Health.up().build()
        } catch (e: Exception) {
            Health.down().withException(e).build()
        }
    }
}
```

## Troubleshooting

### Common Issues

#### 1. Events Not Being Processed

**Symptoms**: Projector method never gets called

**Possible Causes**:

- NATS connection issues
- Incorrect subject configuration
- Missing `@Component` annotation
- Database connection issues

**Solutions**:

- Check NATS connectivity and configuration
- Verify subject names match between publisher and consumer
- Ensure projector class is a Spring component
- Check database connectivity and migrations

#### 2. Duplicate Processing

**Symptoms**: Same event processed multiple times

**Possible Causes**:

- Transaction not properly configured
- Database isolation level issues
- Race conditions in concurrent processing

**Solutions**:

- Ensure `@Transactional` is properly configured
- Check database isolation level (READ_COMMITTED recommended)
- Review concurrent processing configuration

#### 3. Memory Leaks

**Symptoms**: Increasing memory usage over time

**Possible Causes**:

- Large event payloads
- Inefficient read model queries
- Connection leaks

**Solutions**:

- Optimize event payload size
- Add proper database indexes
- Monitor connection pool usage

### Debug Mode

Enable debug logging for detailed processing information:

```yaml
logging:
  level:
    com.axians.eaf.eventing.consumer: DEBUG
```

This will log:

- Event reception details
- Idempotency check results
- Transaction boundaries
- Processing timing information

## Best Practices

### 1. Projector Design

- **Single Responsibility**: Each projector should handle one type of read model
- **Idempotent Operations**: Design operations to be naturally idempotent when possible
- **Error Handling**: Distinguish between retryable and non-retryable errors

### 2. Performance

- **Batch Processing**: Consider batching for high-volume scenarios
- **Database Indexes**: Add appropriate indexes for tenant-scoped queries
- **Connection Pooling**: Configure appropriate database connection pools

### 3. Testing

- **Unit Tests**: Test projector logic in isolation
- **Integration Tests**: Test end-to-end with real NATS and database
- **Chaos Testing**: Test behavior under failure conditions

### 4. Deployment

- **Rolling Updates**: Deploy projectors with zero downtime
- **Monitoring**: Set up comprehensive monitoring and alerting
- **Backup Strategy**: Ensure read models can be rebuilt from events

## Migration Guide

### From Manual NATS Listeners

If you're migrating from manual `@NatsJetStreamListener` usage:

```kotlin
// Before
@NatsJetStreamListener(subject = "user.events.created")
fun handleUserCreated(event: UserCreatedEvent, context: MessageContext) {
    // Manual idempotency and transaction management
    val eventId = extractEventId(context)
    val tenantId = context.tenantId

    if (!isProcessed(eventId, tenantId)) {
        processEvent(event, tenantId)
        markAsProcessed(eventId, tenantId)
    }
    context.ack()
}

// After
@EafProjectorEventHandler(subject = "user.events.created")
fun handleUserCreated(event: UserCreatedEvent, eventId: UUID, tenantId: String) {
    // Just business logic - idempotency handled automatically
    processEvent(event, tenantId)
}
```

### Database Schema Updates

If you have existing processed event tracking, migrate to the standard schema:

```sql
-- Migration script
INSERT INTO processed_events (projector_name, event_id, tenant_id, processed_at)
SELECT 'legacy-projector', event_id, tenant_id, processed_at
FROM legacy_processed_events;
```

## Conclusion

The EAF Eventing SDK's idempotent projector support provides a robust foundation for building
reliable, scalable event-driven applications. By handling the complex aspects of idempotency,
transactions, and tenant isolation automatically, it allows you to focus on your business logic
while ensuring data consistency and reliability.

For more advanced use cases or questions, consult the API documentation or reach out to the EAF
team.

---
sidebar_position: 1
---

# NATS JetStream Event Publishing

The EAF NATS SDK provides a robust, tenant-aware `NatsEventPublisher` service to reliably publish domain events to NATS JetStream.

## Overview

The `NatsEventPublisher` abstracts away the complexities of NATS communication, offering a simple interface for publishing events while handling:

- **Automatic JSON Serialization**: Kotlin objects are automatically wrapped in a standardized `EventEnvelope` and serialized to JSON.
- **Tenant-Aware Subject Mapping**: Events are automatically published to tenant-specific subjects (e.g., `TENANT_A.events.user.created`).
- **At-Least-Once Delivery**: Utilizes JetStream's `PublishAck` mechanism to ensure messages are successfully persisted.
- **Configurable Retries**: Automatic retry logic with exponential backoff for transient publishing failures.
- **Centralized Configuration**: All connection and publishing settings are managed via Spring Boot properties.

## Basic Usage

### 1. Configure the Publisher

Add the necessary NATS configuration to your `application.yml`:

```yaml
eaf:
  eventing:
    nats:
      servers:
        - "nats://localhost:4222" # Your NATS server URL
      # Optional: Credentials for a specific tenant user
      # username: "tenant_a_user"
      # password: "tenant_a_password_456!"
      publisher:
        retry:
          max-attempts: 3
          initial-delay-ms: 100
          backoff-multiplier: 2.0
```

### 2. Inject and Use the Publisher

Inject the `NatsEventPublisher` service into any Spring-managed bean and use it to publish events.

```kotlin
@Service
class UserService(
    private val eventPublisher: NatsEventPublisher,
    private val tenantContext: TenantContext // Assume a way to get current tenant
) {

    fun createUser(name: String, email: String) {
        val tenantId = tenantContext.getCurrentTenantId()
        
        // Define the domain event payload
        val userCreatedEvent = UserCreatedPayload(
            userId = UUID.randomUUID(),
            name = name,
            email = email
        )
        
        // Publish the event
        eventPublisher.publish(
            subject = "events.user.created",
            tenantId = tenantId,
            event = userCreatedEvent,
            metadata = mapOf("source" to "iam-service")
        )
    }
}

data class UserCreatedPayload(
    val userId: UUID,
    val name: String,
    val email: String
)
```

## Event Envelope

The SDK automatically wraps your event payload in a standardized `EventEnvelope` before publishing. This ensures all events have a consistent structure.

```kotlin
data class EventEnvelope(
    val eventId: String,          // Unique ID for the event
    val eventType: String,        // The class name of your event payload
    val timestamp: Instant,       // UTC timestamp of when the event was created
    val tenantId: String,         // The tenant this event belongs to
    val payload: Any,             // Your actual event data class
    val metadata: Map<String, Any>  // Optional metadata
)
```

For the example above, the JSON payload sent to NATS would look like this:

```json
{
  "eventId": "a1b2c3d4-e5f6-a7b8-c9d0-e1f2a3b4c5d6",
  "eventType": "UserCreatedPayload",
  "timestamp": "2025-01-17T10:00:00Z",
  "tenantId": "TENANT_A",
  "payload": {
    "userId": "f1g2h3i4-j5k6-l7m8-n9o0-p1q2r3s4t5u6",
    "name": "John Doe",
    "email": "john.doe@axians.com"
  },
  "metadata": {
    "source": "iam-service"
  }
}
```

## Advanced Usage

### Publishing with Custom Metadata

You can add custom metadata to your events, which is useful for tracing, routing, or providing additional context without polluting your domain event payload.

```kotlin
fun updateUser(userId: UUID, newEmail: String, correlationId: String) {
    val event = UserEmailUpdatedPayload(userId, newEmail)
    
    eventPublisher.publish(
        subject = "events.user.email.updated",
        tenantId = tenantContext.getCurrentTenantId(),
        event = event,
        metadata = mapOf(
            "correlationId" to correlationId,
            "updatedBy" to "system-process"
        )
    )
}
```

### Error Handling

If the publisher fails to send an event after all retry attempts, it will throw an `EventPublishingException`. Your application code should be prepared to handle this exception.

```kotlin
try {
    eventPublisher.publish(...)
} catch (e: EventPublishingException) {
    // Handle the failure, e.g., log a critical error,
    // trigger a fallback mechanism, or re-throw as a business exception.
    logger.error("Failed to publish critical event after retries", e)
    throw UserCreationFailedException("Could not notify other services.", e)
}
```

## Configuration Reference

All publisher settings are configured under the `eaf.eventing.nats.publisher` key in `application.yml`.

| Property                   | Description                                  | Default |
| -------------------------- | -------------------------------------------- | ------- |
| `retry.max-attempts`       | Maximum number of publish attempts.          | `3`     |
| `retry.initial-delay-ms`   | Initial delay before the first retry.        | `100`   |
| `retry.backoff-multiplier` | Multiplier for the delay between retries.    | `2.0`   |
| `retry.max-delay-ms`       | Maximum delay between retries.               | `10000` |

## Best Practices

1. **Keep Payloads Small**: NATS is optimized for small, frequent messages. Avoid large event payloads.
2. **Use Specific Subjects**: Use a clear and hierarchical subject naming strategy (e.g., `<domain>.<entity>.<action>`).
3. **Handle Publish Failures**: While the SDK retries, persistent failures can occur. Ensure your business logic can handle cases where an event cannot be published.
4. **Leverage Metadata**: Use metadata for operational concerns like tracing (`correlationId`) and diagnostics, keeping your domain `payload` clean.
5. **Immutable Events**: Design your event data classes to be immutable (`val` properties in Kotlin data classes).

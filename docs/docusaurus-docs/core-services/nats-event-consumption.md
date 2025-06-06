---
sidebar_position: 2
---

# NATS JetStream Event Consumption

The EAF NATS SDK provides powerful abstractions for consuming events from NATS JetStream streams with full support for multi-tenancy, ordered processing, and reliable acknowledgment.

## Overview

The `@NatsJetStreamListener` annotation enables declarative event consumption that feels familiar to any Spring developer. It handles the complexity of:

- JetStream consumer management
- Automatic event deserialization
- Tenant-aware subscriptions
- Message acknowledgment control
- Error handling and retries

## Basic Usage

### Simple Event Consumer

```kotlin
@Service
class UserEventHandler {

    @NatsJetStreamListener("events.user.created")
    fun handleUserCreated(event: UserCreatedEvent) {
        println("User created: ${event.userId}")
        // Process the event...
    }
}

data class UserCreatedEvent(
    val userId: String,
    val email: String,
    val timestamp: Instant
)
```

### Consumer with Configuration

```kotlin
@Service
class OrderEventHandler {

    @NatsJetStreamListener(
        subject = "events.order.>",
        durableName = "order-processor",
        deliverPolicy = DeliverPolicy.All,
        ackPolicy = AckPolicy.Explicit,
        maxDeliver = 5,
        ackWait = 60000L,
        autoAck = true
    )
    fun handleOrderEvents(event: OrderEvent) {
        when (event.type) {
            "ORDER_CREATED" -> handleOrderCreated(event)
            "ORDER_UPDATED" -> handleOrderUpdated(event)
            "ORDER_CANCELLED" -> handleOrderCancelled(event)
        }
    }
}
```

## Advanced Usage

### Manual Message Control

For advanced scenarios where you need full control over message acknowledgment:

```kotlin
@Service
class PaymentEventHandler {

    @NatsJetStreamListener(
        subject = "events.payment.>",
        autoAck = false  // Disable automatic acknowledgment
    )
    fun handlePaymentEvent(event: PaymentEvent, context: MessageContext) {
        val correlationId = context.getHeader("correlation-id")
        
        try {
            processPayment(event, correlationId)
            context.ack() // Manually acknowledge success
            
        } catch (e: TemporaryPaymentException) {
            // Retry after 30 seconds
            context.nak(Duration.ofSeconds(30))
            
        } catch (e: InvalidPaymentException) {
            // Terminate - this is a poison pill
            context.term()
        }
    }
}
```

## Error Handling

The EAF SDK provides a sophisticated error handling mechanism using exception types to control message acknowledgment:

### Exception Types

```kotlin
// Retryable errors - message will be nak'd and retried
class RetryableEventException(message: String, cause: Throwable? = null) 
    : EventConsumptionException(message, cause)

// Non-retryable errors - message will be terminated
class PoisonPillEventException(message: String, cause: Throwable? = null) 
    : EventConsumptionException(message, cause)
```

### Error Handling Example

```kotlin
@Service
class RobustEventHandler {

    @NatsJetStreamListener("events.data.sync")
    fun handleDataSync(event: DataSyncEvent) {
        try {
            // Validate event structure
            if (event.payload.isEmpty()) {
                throw PoisonPillEventException("Empty payload - invalid event")
            }
            
            // Attempt to sync data
            dataService.syncData(event.payload)
            
        } catch (e: DatabaseConnectionException) {
            // Temporary issue - retry
            throw RetryableEventException("Database temporarily unavailable", e)
            
        } catch (e: ValidationException) {
            // Data issue - don't retry
            throw PoisonPillEventException("Invalid data format", e)
        }
    }
}
```

## Idempotency Patterns

Building idempotent event consumers is crucial for reliable event processing. Here are proven patterns:

### 1. Natural Idempotency

Design your operations to be naturally idempotent:

```kotlin
@Service
class UserProfileHandler {

    @NatsJetStreamListener("events.user.profile.updated")
    fun handleProfileUpdate(event: UserProfileUpdatedEvent) {
        // This is naturally idempotent - setting the same values multiple times
        // produces the same result
        userRepository.updateProfile(
            userId = event.userId,
            email = event.email,
            name = event.name,
            updatedAt = event.timestamp
        )
    }
}
```

### 2. Event ID Tracking

Track processed event IDs to prevent duplicate processing:

```kotlin
@Service
class OrderHandler {

    @NatsJetStreamListener("events.order.payment.completed")
    fun handlePaymentCompleted(event: PaymentCompletedEvent) {
        // Check if already processed
        if (processedEventRepository.exists(event.eventId)) {
            logger.debug("Event {} already processed, skipping", event.eventId)
            return
        }

        try {
            // Process the event
            orderService.markAsPaid(event.orderId, event.paymentId)
            
            // Mark as processed
            processedEventRepository.save(
                ProcessedEvent(
                    eventId = event.eventId,
                    eventType = "PaymentCompleted",
                    processedAt = Instant.now()
                )
            )
        } catch (e: Exception) {
            // Don't save as processed if there was an error
            throw RetryableEventException("Failed to process payment completion", e)
        }
    }
}
```

## Configuration

### Application Properties

Configure consumer defaults in your `application.yml`:

```yaml
eaf:
  eventing:
    nats:
      servers:
        - "nats://localhost:4222"
      defaultTenantId: "TENANT_A"
      consumer:
        defaultAckWait: 30000
        defaultMaxDeliver: 3
        defaultMaxAckPending: 1000
        autoStartup: true
```

### Annotation Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `subject` | NATS subject pattern to subscribe to | Required |
| `durableName` | Durable consumer name | `{ClassName}-{methodName}-consumer` |
| `deliverPolicy` | Message delivery policy | `DeliverPolicy.All` |
| `ackPolicy` | Acknowledgment policy | `AckPolicy.Explicit` |
| `maxDeliver` | Maximum delivery attempts | `3` |
| `ackWait` | Acknowledgment timeout (ms) | `30000` |
| `maxAckPending` | Max outstanding messages | `1000` |
| `autoAck` | Automatic acknowledgment | `true` |

## Best Practices

### 1. Consumer Design

- **Keep consumers focused**: Each consumer should handle a specific type of event
- **Make operations idempotent**: Always design for potential duplicate event delivery
- **Use meaningful durable names**: This helps with monitoring and debugging

### 2. Error Handling

- **Be specific with exceptions**: Use `RetryableEventException` for temporary issues, `PoisonPillEventException` for data problems
- **Log comprehensively**: Include correlation IDs and event details
- **Monitor poison pills**: Set up alerts for events that are repeatedly terminated

### 3. Multi-Tenancy

The EAF SDK automatically handles tenant isolation:

```kotlin
@Service
class TenantAwareHandler {

    @NatsJetStreamListener("events.user.>")
    fun handleUserEvent(event: UserEvent, context: MessageContext) {
        val tenantId = context.tenantId
        
        // All operations are automatically scoped to the tenant
        userService.processUser(event, tenantId)
    }
}
```

Subject routing is automatically tenant-prefixed:

- Your subject: `"events.user.created"`
- Actual subscription: `"TENANT_A.events.user.created"`

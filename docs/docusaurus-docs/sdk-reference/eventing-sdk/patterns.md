---
sidebar_position: 4
title: Usage Patterns
---

# Eventing SDK Usage Patterns

Common patterns and best practices for using the EAF Eventing SDK.

## 🎯 Domain Event Publishing

```kotlin
@Component
class OrderService(
    private val eventPublisher: NatsEventPublisher
) {
    suspend fun createOrder(command: CreateOrderCommand) {
        val order = Order.create(command)

        // Publish domain event
        val event = OrderCreatedEvent(
            orderId = order.id,
            customerId = order.customerId
        )

        eventPublisher.publish("orders.created", event)
    }
}
```

## 📥 Event Handling

```kotlin
@Component
class OrderEventHandler {
    @EventHandler("orders.created")
    suspend fun handleOrderCreated(event: OrderCreatedEvent) {
        // Process the event
        logger.info("Processing order created: ${event.orderId}")
    }
}
```

## 🔄 Error Handling

```kotlin
@EventHandler("orders.created")
suspend fun handleOrderCreated(event: OrderCreatedEvent) {
    try {
        processOrder(event)
    } catch (e: BusinessException) {
        // Handle business errors
        logger.warn("Business error processing order", e)
    } catch (e: Exception) {
        // Handle technical errors
        logger.error("Technical error processing order", e)
        throw e // Will trigger retry
    }
}
```

## 🏷️ Event Versioning

Handle event evolution gracefully:

```kotlin
@EventHandler("orders.created")
suspend fun handleOrderCreated(event: Any) {
    when (event) {
        is OrderCreatedEventV2 -> handleV2(event)
        is OrderCreatedEventV1 -> handleV1(event)
        else -> logger.warn("Unknown event version")
    }
}
```

---

_Best practices for using the EAF Eventing SDK effectively._

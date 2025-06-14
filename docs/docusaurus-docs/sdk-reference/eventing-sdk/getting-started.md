---
sidebar_position: 1
title: Getting Started
---

# Getting Started with EAF Eventing SDK

Quick start guide for integrating the EAF Eventing SDK into your service.

## 📦 Installation

Add the dependency to your project:

```kotlin
dependencies {
    implementation("com.axians.eaf:eaf-eventing-sdk:${eafVersion}")
}
```

## ⚙️ Configuration

Configure NATS connection in your `application.yml`:

```yaml
eaf:
  eventing:
    nats-url: 'nats://localhost:4222'
    cluster-id: 'eaf-cluster'
    client-id: 'my-service'
```

## 🚀 Basic Usage

### Publishing Events

```kotlin
@Component
class OrderEventPublisher(
    private val eventPublisher: NatsEventPublisher
) {
    suspend fun publishOrderCreated(order: Order) {
        val event = OrderCreatedEvent(order.id, order.customerId)
        eventPublisher.publish("orders.created", event)
    }
}
```

### Subscribing to Events

```kotlin
@Component
class OrderEventHandler {
    @EventHandler("orders.created")
    suspend fun handle(event: OrderCreatedEvent) {
        // Handle the event
        println("Order created: ${event.orderId}")
    }
}
```

## 🔧 Next Steps

- [Configuration Guide](./configuration.md) - Detailed configuration options
- [Usage Patterns](./patterns.md) - Common usage patterns
- [API Reference](./api-reference.md) - Complete API documentation

---

_Get started with event-driven architecture using the EAF Eventing SDK._

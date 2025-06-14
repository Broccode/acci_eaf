---
sidebar_position: 1
title: Getting Started
---

# Getting Started with EAF Event Sourcing SDK

Quick start guide for implementing event sourcing with the EAF Event Sourcing SDK.

## 📦 Installation

Add the dependency to your project:

```kotlin
dependencies {
    implementation("com.axians.eaf:eaf-eventsourcing-sdk:${eafVersion}")
}
```

## ⚙️ Configuration

Configure the event store in your `application.yml`:

```yaml
eaf:
  eventsourcing:
    datasource-url: 'jdbc:postgresql://localhost:5432/eventstore'
    username: 'eventstore_user'
    password: '${DATABASE_PASSWORD}'
```

## 🚀 Basic Usage

### Define an Aggregate

```kotlin
@AggregateRoot
class Order : EventSourcedAggregate() {
    private lateinit var orderId: OrderId
    private lateinit var customerId: CustomerId
    private var status: OrderStatus = OrderStatus.DRAFT

    companion object {
        fun create(customerId: CustomerId): Order {
            val order = Order()
            order.apply(OrderCreatedEvent(OrderId.generate(), customerId))
            return order
        }
    }

    fun confirm() {
        require(status == OrderStatus.DRAFT) { "Order already confirmed" }
        apply(OrderConfirmedEvent(orderId))
    }

    override fun applyEvent(event: DomainEvent) {
        when (event) {
            is OrderCreatedEvent -> {
                orderId = event.orderId
                customerId = event.customerId
                status = OrderStatus.DRAFT
            }
            is OrderConfirmedEvent -> {
                status = OrderStatus.CONFIRMED
            }
        }
    }
}
```

### Use the Repository

```kotlin
@Service
class OrderService(
    private val orderRepository: EventSourcedRepository&lt;Order, OrderId&gt;
) {
    suspend fun confirmOrder(orderId: OrderId) {
        val order = orderRepository.load(orderId)
        order.confirm()
        orderRepository.save(order)
    }
}
```

---

_Get started with event sourcing using the EAF Event Sourcing SDK._

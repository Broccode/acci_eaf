---
sidebar_position: 4
title: Usage Patterns
---

# Event Sourcing SDK Usage Patterns

Common patterns and best practices for using the EAF Event Sourcing SDK.

## 🏗️ Aggregate Design

### State Management

```kotlin
@AggregateRoot
class BankAccount : EventSourcedAggregate() {
    private lateinit var accountId: AccountId
    private var balance: Money = Money.ZERO
    private var status: AccountStatus = AccountStatus.ACTIVE

    fun withdraw(amount: Money) {
        require(status == AccountStatus.ACTIVE) { "Account is not active" }
        require(balance >= amount) { "Insufficient funds" }

        apply(MoneyWithdrawnEvent(accountId, amount))
    }

    override fun applyEvent(event: DomainEvent) {
        when (event) {
            is AccountOpenedEvent -> {
                accountId = event.accountId
                balance = event.initialDeposit
                status = AccountStatus.ACTIVE
            }
            is MoneyWithdrawnEvent -> {
                balance = balance.subtract(event.amount)
            }
        }
    }
}
```

## 📝 Event Design

### Event Versioning

```kotlin
// Version 1
data class CustomerCreatedEventV1(
    val customerId: CustomerId,
    val name: String,
    val email: String
) : DomainEvent

// Version 2 - Added address
data class CustomerCreatedEventV2(
    val customerId: CustomerId,
    val name: String,
    val email: String,
    val address: Address
) : DomainEvent
```

### Event Upcasting

```kotlin
@Component
class CustomerEventUpcaster : EventUpcaster {
    override fun upcast(event: DomainEvent): DomainEvent {
        return when (event) {
            is CustomerCreatedEventV1 -> CustomerCreatedEventV2(
                customerId = event.customerId,
                name = event.name,
                email = event.email,
                address = Address.UNKNOWN
            )
            else -> event
        }
    }
}
```

## 📸 Snapshot Patterns

### Custom Snapshot Logic

```kotlin
@AggregateRoot
class Order : EventSourcedAggregate(), Snapshottable {
    override fun shouldCreateSnapshot(): Boolean {
        return version % 50 == 0L || items.size > 100
    }

    override fun createSnapshot(): AggregateSnapshot {
        return OrderSnapshot(
            aggregateId = orderId,
            version = version,
            status = status,
            items = items.toList(),
            totalAmount = totalAmount
        )
    }

    override fun restoreFromSnapshot(snapshot: AggregateSnapshot) {
        val orderSnapshot = snapshot as OrderSnapshot
        this.orderId = orderSnapshot.aggregateId
        this.version = snapshot.version
        this.status = orderSnapshot.status
        this.items = orderSnapshot.items.toMutableList()
        this.totalAmount = orderSnapshot.totalAmount
    }
}
```

## 🔄 Event Migration

### Schema Evolution

```kotlin
@Component
class OrderEventMigrator : EventMigrator {
    override fun migrate(event: StoredEvent): StoredEvent {
        return when (event.eventType) {
            "OrderCreatedEventV1" -> migrateOrderCreatedEvent(event)
            else -> event
        }
    }

    private fun migrateOrderCreatedEvent(event: StoredEvent): StoredEvent {
        val v1Data = json.parseToJsonElement(event.eventData).jsonObject
        val v2Data = buildJsonObject {
            put("orderId", v1Data["orderId"]!!)
            put("customerId", v1Data["customerId"]!!)
            put("currency", "EUR") // Default value for new field
            putJsonArray("items") { } // New field
        }

        return event.copy(
            eventType = "OrderCreatedEventV2",
            eventData = v2Data.toString()
        )
    }
}
```

## 🧪 Testing Patterns

### Aggregate Testing

```kotlin
class BankAccountTest {
    @Test
    fun `should withdraw money when sufficient balance`() {
        // Given
        val account = BankAccount()
        account.apply(AccountOpenedEvent(
            AccountId.generate(),
            Money(BigDecimal("1000"))
        ))

        // When
        account.withdraw(Money(BigDecimal("100")))

        // Then
        val events = account.getUncommittedEvents()
        assertThat(events).hasSize(1)
        assertThat(events.first()).isInstanceOf(MoneyWithdrawnEvent::class.java)
    }
}
```

### Event Store Testing

```kotlin
@SpringBootTest
@Testcontainers
class EventStoreIntegrationTest {
    @Test
    fun `should store and replay events correctly`() = runTest {
        // Given
        val aggregateId = AccountId.generate()
        val events = listOf(
            AccountOpenedEvent(aggregateId, Money(BigDecimal("1000"))),
            MoneyWithdrawnEvent(aggregateId, Money(BigDecimal("100")))
        )

        // When
        eventStore.saveEvents(aggregateId, 0, events)
        val replayedEvents = eventStore.loadEvents(aggregateId)

        // Then
        assertThat(replayedEvents).hasSize(2)
        assertThat(replayedEvents).containsExactlyElementsOf(events)
    }
}
```

---

_Best practices for effective event sourcing with the EAF Event Sourcing SDK._

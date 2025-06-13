---
sidebar_position: 4
title: Context Propagation Guide
---

# Context Propagation in Distributed EAF Applications

The ACCI EAF provides robust context propagation mechanisms that ensure security context,
correlation IDs, and other essential information flows seamlessly across asynchronous operations,
coroutines, and distributed message processing.

## Overview

Context propagation is critical in distributed, asynchronous systems where traditional `ThreadLocal`
variables fail to maintain context across thread boundaries. The EAF solves this with:

- **Correlation ID Management**: Distributed tracing support with automatic ID generation
- **Coroutine Context Propagation**: Seamless context flow across Kotlin Coroutines
- **NATS Message Context**: Automatic context enrichment and establishment in event messaging
- **Spring Security Integration**: Transparent integration with existing security patterns

## Core Components

### CorrelationIdManager

The `CorrelationIdManager` provides thread-safe correlation ID management with SLF4J MDC integration
for distributed tracing:

```kotlin
import com.axians.eaf.core.security.CorrelationIdManager

class OrderService {
    private val logger = LoggerFactory.getLogger(OrderService::class.java)

    fun processOrder(order: Order) {
        // Generate a correlation ID for this operation
        CorrelationIdManager.withNewCorrelationId {
            logger.info("Processing order ${order.id}") // Correlation ID in logs

            // All subsequent operations share the same correlation ID
            validateOrder(order)
            saveOrder(order)
            publishOrderEvent(order)
        }
    }
}
```

#### API Reference

```kotlin
// Get current correlation ID (generates one if none exists)
val correlationId = CorrelationIdManager.getCurrentCorrelationId()

// Get correlation ID without generating (returns null if none)
val correlationId = CorrelationIdManager.getCurrentCorrelationIdOrNull()

// Set a specific correlation ID
CorrelationIdManager.setCorrelationId("my-correlation-id")

// Generate and set a new correlation ID
val newId = CorrelationIdManager.generateAndSetCorrelationId()

// Execute code with a specific correlation ID
CorrelationIdManager.withCorrelationId("specific-id") {
    // Code here has access to the correlation ID
    // It's automatically available in SLF4J MDC as "correlationId"
}

// Execute code with a new generated correlation ID
CorrelationIdManager.withNewCorrelationId {
    // Code here has a fresh correlation ID
}

// Clear the correlation ID
CorrelationIdManager.clearCorrelationId()
```

### Coroutine Context Propagation

The EAF provides `ThreadContextElement` implementations that automatically propagate context across
coroutine boundaries:

```kotlin
import com.axians.eaf.core.security.withEafContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

@Service
class ProductService(
    private val securityContextHolder: EafSecurityContextHolder
) {

    suspend fun processProductBatch(products: List<Product>) {
        // Propagate both security context and correlation ID
        withEafContext {
            // Launch parallel coroutines - all have access to context
            val results = products.map { product ->
                async {
                    // Security context and correlation ID available here
                    val tenantId = securityContextHolder.getTenantId()
                    val correlationId = CorrelationIdManager.getCurrentCorrelationId()

                    processProduct(product, tenantId)
                }
            }

            results.awaitAll()
        }
    }
}
```

#### Context Propagation Functions

```kotlin
// Propagate both security context and correlation ID
withEafContext {
    launch {
        // Full context available
    }
}

// Propagate only correlation ID
withCorrelationIdContext {
    launch {
        // Only correlation ID available
    }
}

// Propagate only security context
withSecurityContext {
    launch {
        // Only security context available
    }
}
```

#### Advanced Context Elements

For fine-grained control, use context elements directly:

```kotlin
import com.axians.eaf.core.security.CorrelationIdElement
import com.axians.eaf.core.security.EafContextElement
import kotlinx.coroutines.withContext

// Use correlation ID element only
withContext(CorrelationIdElement()) {
    // Correlation ID is propagated
}

// Use combined EAF context element
withContext(EafContextElement()) {
    // Both security context and correlation ID are propagated
}

// Combine with other context elements
withContext(Dispatchers.IO + EafContextElement()) {
    // IO dispatcher + EAF context
}
```

## NATS Message Context Propagation

The EAF Eventing SDK automatically handles context propagation for NATS messages:

### Outgoing Messages (Publishing)

Context is automatically added to message headers when publishing:

```kotlin
@Service
class OrderEventPublisher(
    private val eventPublisher: ContextAwareNatsEventPublisher
) {

    suspend fun publishOrderCreated(order: Order) {
        // Context automatically added to message headers:
        // - eaf.tenant.id
        // - eaf.user.id
        // - eaf.correlation.id
        eventPublisher.publish(
            subject = "orders.${order.tenantId}.created",
            tenantId = order.tenantId,
            event = OrderCreatedEvent(order.id, order.customerId),
            metadata = mapOf("orderType" to order.type)
        )
    }
}
```

### Incoming Messages (Consuming)

Context is automatically established from message headers:

```kotlin
@Component
class OrderEventHandler {

    @EventHandler
    fun handleOrderCreated(event: OrderCreatedEvent) {
        // Context automatically established from message headers
        val tenantId = SecurityContextHolder.getContext().authentication
            ?.let { (it as? HasTenantId)?.getTenantId() }

        val correlationId = CorrelationIdManager.getCurrentCorrelationIdOrNull()

        // Process event with full context
        processOrderCreated(event, tenantId, correlationId)
    }
}
```

### Manual Message Processing

For custom message processing, use `ContextAwareMessageProcessor`:

```kotlin
@Component
class CustomMessageProcessor(
    private val messageProcessor: ContextAwareMessageProcessor
) {

    fun processMessage(message: Message) {
        messageProcessor.processWithContext(message) {
            // Context automatically established from message headers
            val tenantId = SecurityContextHolder.getContext().authentication
                ?.let { (it as? HasTenantId)?.getTenantId() }

            val correlationId = CorrelationIdManager.getCurrentCorrelationIdOrNull()

            // Your message processing logic here
            handleBusinessLogic(message, tenantId, correlationId)
        }
        // Context automatically cleaned up after processing
    }
}
```

## Best Practices

### 1. Establish Context Early

Establish correlation IDs at service boundaries (controllers, message handlers):

```kotlin
@RestController
class OrderController {

    @PostMapping("/orders")
    fun createOrder(@RequestBody request: CreateOrderRequest): ResponseEntity<Order> {
        return CorrelationIdManager.withNewCorrelationId {
            // All subsequent operations share this correlation ID
            val order = orderService.createOrder(request)
            ResponseEntity.ok(order)
        }
    }
}
```

### 2. Always Use Context Propagation in Async Code

```kotlin
// ✅ Good - Context is propagated
withEafContext {
    launch {
        // Security context and correlation ID available
        processAsyncOperation()
    }
}

// ❌ Bad - Context is lost
launch {
    // No security context or correlation ID
    processAsyncOperation()
}
```

### 3. Leverage Structured Logging

The correlation ID is automatically included in SLF4J MDC:

```kotlin
class OrderService {
    private val logger = LoggerFactory.getLogger(OrderService::class.java)

    fun processOrder(order: Order) {
        CorrelationIdManager.withNewCorrelationId {
            logger.info("Starting order processing") // Includes correlation ID

            try {
                validateOrder(order)
                logger.info("Order validation successful")

                saveOrder(order)
                logger.info("Order saved successfully")

            } catch (e: Exception) {
                logger.error("Order processing failed", e) // Correlation ID in error logs
                throw e
            }
        }
    }
}
```

### 4. Handle Partial Context Gracefully

Not all operations require full context:

```kotlin
fun processMessage(message: Message) {
    messageProcessor.processWithContext(message) {
        val tenantId = SecurityContextHolder.getContext().authentication
            ?.let { (it as? HasTenantId)?.getTenantId() }

        if (tenantId != null) {
            // Process with tenant context
            processTenantSpecificLogic(message, tenantId)
        } else {
            // Process without tenant context (e.g., system events)
            processSystemLogic(message)
        }
    }
}
```

### 5. Test Context Propagation

Always test that context propagates correctly:

```kotlin
@Test
fun `should propagate context across coroutines`() = runBlocking {
    // Given
    val testTenantId = "test-tenant"
    val testCorrelationId = "test-correlation"

    // Set up context
    setupSecurityContext(testTenantId)
    CorrelationIdManager.setCorrelationId(testCorrelationId)

    // When
    withEafContext {
        launch {
            // Then
            assertEquals(testTenantId, securityContextHolder.getTenantId())
            assertEquals(testCorrelationId, CorrelationIdManager.getCurrentCorrelationId())
        }
    }
}
```

## Standard NATS Headers

The EAF uses standardized headers for context propagation:

| Header               | Description                            | Example                                |
| -------------------- | -------------------------------------- | -------------------------------------- |
| `eaf.tenant.id`      | Current tenant identifier              | `tenant-123`                           |
| `eaf.user.id`        | Current user identifier                | `user-456`                             |
| `eaf.correlation.id` | Correlation ID for distributed tracing | `550e8400-e29b-41d4-a716-446655440000` |

These headers are automatically managed by the EAF Eventing SDK.

## Troubleshooting

### Context Not Available in Coroutines

**Problem**: Security context or correlation ID is null in coroutines.

**Solution**: Ensure you're using context propagation functions:

```kotlin
// ❌ Wrong
launch {
    val tenantId = securityContextHolder.getTenantId() // May be null
}

// ✅ Correct
withEafContext {
    launch {
        val tenantId = securityContextHolder.getTenantId() // Available
    }
}
```

### Correlation ID Not in Logs

**Problem**: Correlation ID doesn't appear in log messages.

**Solution**: Ensure your logging configuration includes MDC:

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId}] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
</configuration>
```

### Message Context Not Established

**Problem**: Context is not available when processing NATS messages.

**Solution**: Ensure you're using `ContextAwareMessageProcessor` or the context-aware event
handlers:

```kotlin
// ✅ Correct - Context automatically established
@EventHandler
fun handleEvent(event: MyEvent) {
    val tenantId = securityContextHolder.getTenantId() // Available
}

// Or for manual processing
messageProcessor.processWithContext(message) {
    // Context available here
}
```

## Integration Examples

### With Spring Boot Controllers

```kotlin
@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService
) {

    @PostMapping
    fun createOrder(@RequestBody request: CreateOrderRequest): ResponseEntity<Order> {
        // Context automatically available from JWT token
        // Establish correlation ID for this request
        return CorrelationIdManager.withNewCorrelationId {
            val order = orderService.createOrder(request)
            ResponseEntity.ok(order)
        }
    }
}
```

### With Event-Driven Architecture

```kotlin
@Service
class OrderService(
    private val eventPublisher: ContextAwareNatsEventPublisher,
    private val securityContextHolder: EafSecurityContextHolder
) {

    suspend fun createOrder(request: CreateOrderRequest): Order {
        val tenantId = securityContextHolder.getTenantId()

        // Create order
        val order = Order(
            id = UUID.randomUUID().toString(),
            tenantId = tenantId,
            customerId = request.customerId,
            items = request.items
        )

        // Save order (with context)
        orderRepository.save(order)

        // Publish event (context automatically added to headers)
        eventPublisher.publish(
            subject = "orders.$tenantId.created",
            tenantId = tenantId,
            event = OrderCreatedEvent(order.id, order.customerId)
        )

        return order
    }
}
```

### With Background Processing

```kotlin
@Service
class OrderProcessingService {

    @Async
    fun processOrderAsync(orderId: String) {
        // For @Async methods, manually establish context if needed
        CorrelationIdManager.withNewCorrelationId {
            logger.info("Starting async order processing for order: $orderId")

            // Process order
            processOrder(orderId)

            logger.info("Completed async order processing for order: $orderId")
        }
    }

    suspend fun processOrderWithCoroutines(orderId: String) {
        // For coroutines, use context propagation
        withEafContext {
            launch {
                logger.info("Starting coroutine order processing for order: $orderId")
                processOrder(orderId)
                logger.info("Completed coroutine order processing for order: $orderId")
            }
        }
    }
}
```

## See Also

- [Security Context Access](./security-context-access.md) - Basic security context usage
- [NATS Event Publishing](./nats-event-publishing.md) - Event publishing patterns
- [NATS Event Consumption](./nats-event-consumption.md) - Event consumption patterns
- [Spring Boot Integration Testing](./spring-boot-integration-testing.md) - Testing with context

---
sidebar_position: 8
title: Security Context Access
---

# Secure EAF Context Access for Services

The ACCI EAF provides a comprehensive security context system that enables services to securely
access essential operational context such as tenant ID, user ID, roles, and permissions. This system
integrates seamlessly with Spring Security and automatically propagates context across asynchronous
operations.

## Overview

The security context system consists of three main libraries:

- **`eaf-iam-client`**: Handles JWT validation and Spring Security integration
- **`eaf-core`**: Provides the `EafSecurityContextHolder` for easy context access
- **`eaf-eventing-sdk`**: Ensures context propagation in NATS event messaging

## Quick Start

### 1. Add Dependencies

Add the necessary EAF SDK dependencies to your service's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.axians.eaf:eaf-core")
    implementation("com.axians.eaf:eaf-iam-client")
    implementation("com.axians.eaf:eaf-eventing-sdk") // If using events

    // Spring Security is automatically included
}
```

### 2. Enable Security Configuration

The `eaf-iam-client` automatically configures Spring Security with JWT validation. Simply ensure
your application is annotated properly:

```kotlin
@SpringBootApplication
class MyEafServiceApplication

fun main(args: Array<String>) {
    runApplication<MyEafServiceApplication>(*args)
}
```

### 3. Inject and Use Context Holder

```kotlin
@Service
class MyBusinessService(
    private val securityContextHolder: EafSecurityContextHolder
) {

    fun processBusinessLogic() {
        // Get current tenant and user context
        val tenantId = securityContextHolder.getTenantIdOrNull()
            ?: throw IllegalStateException("No tenant context")
        val userId = securityContextHolder.getUserId()

        // Check permissions
        if (securityContextHolder.hasRole("ADMIN")) {
            // Admin-specific logic
        }

        // Your business logic here...
    }
}
```

## Core Components

### EafSecurityContextHolder

The `EafSecurityContextHolder` is your primary interface for accessing security context:

```kotlin
interface EafSecurityContextHolder {
    // Tenant access
    fun getTenantId(): String                 // Throws if no tenant
    fun getTenantIdOrNull(): String?          // Returns null if no tenant

    // User access
    fun getUserId(): String?                  // Current user ID (nullable)
    fun getPrincipal(): Principal?            // Full principal object

    // Authorization
    fun hasRole(role: String): Boolean        // Check single role
    fun hasPermission(permission: String): Boolean  // Check permission
    fun hasAnyRole(vararg roles: String): Boolean   // Check multiple roles

    // Authentication status
    fun isAuthenticated(): Boolean            // Is user authenticated?
    fun getAuthentication(): Authentication?  // Raw Spring Security auth
}
```

### Usage Examples

#### Basic Context Access

```kotlin
@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService,
    private val securityContextHolder: EafSecurityContextHolder
) {

    @PostMapping
    fun createProduct(@RequestBody request: CreateProductRequest): ResponseEntity<Product> {
        // Automatically uses current tenant/user context
        val product = productService.createProduct(request)
        return ResponseEntity.ok(product)
    }

    @GetMapping
    fun listProducts(): ResponseEntity<List<Product>> {
        val tenantId = securityContextHolder.getTenantId()

        // Service automatically filters by tenant
        val products = productService.listProducts()
        return ResponseEntity.ok(products)
    }
}
```

#### Role-Based Access Control

```kotlin
@Service
class AdminService(
    private val securityContextHolder: EafSecurityContextHolder
) {

    fun performAdminAction() {
        // Method-level authorization check
        if (!securityContextHolder.hasRole("ADMIN")) {
            throw AccessDeniedException("Admin role required")
        }

        // Or check multiple roles
        if (!securityContextHolder.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            throw AccessDeniedException("Administrative privileges required")
        }

        // Proceed with admin logic...
    }

    fun checkUserPermissions(action: String) {
        val canPerform = when (action) {
            "DELETE_USER" -> securityContextHolder.hasPermission("user:delete")
            "MODIFY_TENANT" -> securityContextHolder.hasRole("TENANT_ADMIN")
            else -> false
        }

        if (!canPerform) {
            throw AccessDeniedException("Insufficient permissions for action: $action")
        }
    }
}
```

## Automatic Context Propagation

The EAF security context system automatically propagates context across different asynchronous
boundaries.

### Kotlin Coroutines

Use `withEafSecurityContext` to ensure context is available in coroutines:

```kotlin
@Service
class AsyncBusinessService(
    private val securityContextHolder: EafSecurityContextHolder
) {

    suspend fun processAsyncOperation() {
        // Context is automatically propagated to coroutines
        withEafSecurityContext {
            val tenantId = securityContextHolder.getTenantIdOrNull()

            // Launch parallel coroutines - context available in all
            val deferredResults = listOf(
                async { processStep1() },
                async { processStep2() },
                async { processStep3() }
            )

            // Each coroutine has access to the security context
            deferredResults.awaitAll()
        }
    }

    private suspend fun processStep1() {
        // Context is available here too
        val userId = securityContextHolder.getUserId()
        delay(100) // Simulate async work
        // Process with context...
    }
}
```

### NATS Event Publishing

Security context is automatically added to NATS message headers:

```kotlin
@Service
class OrderService(
    private val eventPublisher: ContextAwareNatsEventPublisher
) {

    fun processOrder(order: Order) {
        // Current tenant/user context automatically added to event
        val event = OrderProcessedEvent(
            orderId = order.id,
            amount = order.amount,
            timestamp = Instant.now()
        )

        // No need to manually add tenant/user info - it's automatic!
        eventPublisher.publish("order.processed", event)
    }
}
```

### NATS Event Consumption

Context is automatically established when processing events:

```kotlin
@Component
class OrderProjector(
    private val securityContextHolder: EafSecurityContextHolder,
    private val orderViewRepository: OrderViewRepository
) {

    @NatsJetStreamListener(
        destination = "order.processed",
        durable = "order-projector"
    )
    fun handleOrderProcessed(event: OrderProcessedEvent) {
        // Use the context-aware processor for automatic context setup
        contextAwareMessageProcessor.processWithContext(event) {
            // Security context from original event is available here
            val tenantId = securityContextHolder.getTenantIdOrNull()
                ?: throw IllegalStateException("No tenant context in event")

            // Update read model with tenant isolation
            val orderView = OrderView(
                id = event.orderId,
                tenantId = tenantId,
                amount = event.amount,
                processedAt = event.timestamp
            )

            orderViewRepository.save(orderView)
        }
    }
}
```

## Best Practices

### 1. Always Check Context Availability

```kotlin
// ✅ Good - Handle missing context gracefully
val tenantId = securityContextHolder.getTenantIdOrNull()
    ?: throw BadRequestException("Operation requires tenant context")

// ❌ Avoid - May throw unexpected exceptions
val tenantId = securityContextHolder.getTenantId()
```

### 2. Use Appropriate Authorization Methods

```kotlin
// ✅ Good - Use specific role checks
if (securityContextHolder.hasRole("ADMIN")) {
    // Admin logic
}

// ✅ Good - Use permission-based checks when appropriate
if (securityContextHolder.hasPermission("user:delete")) {
    // Delete user logic
}

// ❌ Avoid - Overly broad or vague checks
if (securityContextHolder.isAuthenticated()) {
    // This doesn't check specific permissions!
}
```

### 3. Propagate Context in Async Operations

```kotlin
// ✅ Good - Use withEafSecurityContext for coroutines
suspend fun processAsync() {
    withEafSecurityContext {
        // Context available in all child coroutines
        launch { step1() }
        launch { step2() }
    }
}

// ❌ Avoid - Context may not be available
suspend fun processAsync() {
    launch { step1() } // Context may be missing!
}
```

### 4. Tenant Isolation in Queries

```kotlin
// ✅ Good - Always include tenant ID in queries
@Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId")
fun findByTenantId(@Param("tenantId") tenantId: String): List<Product>

// ❌ Avoid - Cross-tenant data leakage risk
@Query("SELECT p FROM Product p") // Missing tenant isolation!
fun findAll(): List<Product>
```

## Testing

### Unit Testing with Mock Context

```kotlin
class MyServiceTest {

    @MockK
    private lateinit var securityContextHolder: EafSecurityContextHolder

    private lateinit var myService: MyService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        myService = MyService(securityContextHolder)
    }

    @Test
    fun `should process business logic with valid context`() {
        // Given
        every { securityContextHolder.getTenantIdOrNull() } returns "tenant-123"
        every { securityContextHolder.getUserId() } returns "user-456"
        every { securityContextHolder.hasRole("USER") } returns true

        // When
        val result = myService.processBusinessLogic()

        // Then
        assertThat(result).isNotNull()
        verify { securityContextHolder.getTenantIdOrNull() }
    }
}
```

### Integration Testing

```kotlin
@SpringBootTest
class SecurityContextIntegrationTest {

    @Autowired
    private lateinit var securityContextHolder: EafSecurityContextHolder

    @Test
    fun `should provide security context in authenticated scenario`() {
        // Set up Spring Security context for test
        val principal = object : HasTenantId, HasUserId {
            override fun getTenantId(): String = "test-tenant"
            override fun getUserId(): String = "test-user"
        }

        val authentication = TestingAuthenticationToken(
            principal,
            "credentials",
            SimpleGrantedAuthority("ROLE_USER")
        )
        authentication.isAuthenticated = true

        SecurityContextHolder.setContext(SecurityContextImpl(authentication))

        // Test context access
        assertThat(securityContextHolder.getTenantIdOrNull()).isEqualTo("test-tenant")
        assertThat(securityContextHolder.getUserId()).isEqualTo("test-user")
        assertThat(securityContextHolder.hasRole("USER")).isTrue()
    }
}
```

## Troubleshooting

### Common Issues

1. **"No tenant context available"**

   - Ensure JWT token is included in Authorization header
   - Verify IAM service is accessible and responding
   - Check token validity and claims

2. **"Context not propagated to coroutines"**

   - Use `withEafSecurityContext` wrapper
   - Ensure coroutines are launched within the wrapper

3. **"Missing context in event handlers"**
   - Use `ContextAwareMessageProcessor.processWithContext`
   - Verify event contains security context headers

### Debugging

Enable debug logging to troubleshoot context issues:

```yaml
logging:
  level:
    com.axians.eaf.core.security: DEBUG
    com.axians.eaf.iam.client: DEBUG
    org.springframework.security: DEBUG
```

## Related Documentation

- [NATS Event Publishing](./nats-event-publishing.md)
- [NATS Event Consumption](./nats-event-consumption.md)
- [Idempotent Projectors](./idempotent-projectors.md)

---
sidebar_position: 5
title: Usage Patterns
---

# IAM Client SDK Usage Patterns

Common patterns and best practices for authentication and authorization.

## 🔑 Authentication Patterns

### JWT Token Validation

```kotlin
@Component
class JwtAuthenticationFilter(
    private val authenticationService: AuthenticationService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractToken(request)

        if (token != null) {
            try {
                val authentication = authenticationService.authenticate(token)
                SecurityContextHolder.getContext().authentication = authentication
            } catch (e: AuthenticationException) {
                response.status = HttpStatus.UNAUTHORIZED.value()
                return
            }
        }

        filterChain.doFilter(request, response)
    }
}
```

### Service-to-Service Authentication

```kotlin
@Component
class ServiceAuthenticatedWebClient(
    private val iamClient: IAMClient,
    private val webClientBuilder: WebClient.Builder
) {
    private val webClient = webClientBuilder
        .filter(addServiceTokenFilter())
        .build()

    private fun addServiceTokenFilter(): ExchangeFilterFunction {
        return ExchangeFilterFunction.ofRequestProcessor { request ->
            iamClient.getServiceToken()
                .map { token ->
                    ClientRequest.from(request)
                        .header("Authorization", "Bearer $token")
                        .build()
                }
        }
    }
}
```

## 🛡️ Authorization Patterns

### Method-Level Security

```kotlin
@RestController
class OrderController {

    @PreAuthorize("hasPermission('orders', 'CREATE')")
    @PostMapping("/orders")
    fun createOrder(@RequestBody order: CreateOrderRequest): OrderResponse {
        return orderService.createOrder(order)
    }

    @PreAuthorize("hasPermission(#orderId, 'Order', 'READ')")
    @GetMapping("/orders/{orderId}")
    fun getOrder(@PathVariable orderId: String): OrderResponse {
        return orderService.getOrder(OrderId(orderId))
    }
}
```

### Programmatic Authorization

```kotlin
@Service
class OrderService(
    private val authorizationService: AuthorizationService
) {
    fun getOrdersForCustomer(customerId: CustomerId): List&lt;Order&gt; {
        if (!authorizationService.hasPermission("orders:read:all")) {
            // User can only see their own orders
            val currentUserId = authorizationService.getCurrentUserId()
            require(customerId.userId == currentUserId) { "Access denied" }
        }

        return orderRepository.findByCustomerId(customerId)
    }
}
```

## 🏢 Multi-Tenant Patterns

### Tenant-Aware Repository

```kotlin
@Repository
class TenantAwareOrderRepository(
    private val tenantContext: TenantContext,
    private val jpaRepository: OrderJpaRepository
) : OrderRepository {

    override fun findById(id: OrderId): Order? {
        val tenantId = tenantContext.getCurrentTenant()
        return jpaRepository.findByIdAndTenantId(id.value, tenantId.value)
            ?.let { orderMapper.toDomain(it) }
    }

    override fun save(order: Order): Order {
        val tenantId = tenantContext.getCurrentTenant()
        val entity = orderMapper.toEntity(order).copy(tenantId = tenantId.value)
        val savedEntity = jpaRepository.save(entity)
        return orderMapper.toDomain(savedEntity)
    }
}
```

### Tenant Context Propagation

```kotlin
@Component
class TenantContextPropagator(
    private val tenantContext: TenantContext
) {
    @EventListener
    fun propagateTenantContext(event: DomainEvent) {
        val currentTenant = tenantContext.getCurrentTenant()

        // Add tenant information to event metadata
        event.metadata["tenantId"] = currentTenant.value
    }

    @Async
    fun processAsync(task: Runnable) {
        val tenant = tenantContext.getCurrentTenant()

        CompletableFuture.runAsync {
            try {
                tenantContext.setTenant(tenant)
                task.run()
            } finally {
                tenantContext.clear()
            }
        }
    }
}
```

## 🔄 Error Handling

### Authentication Error Handling

```kotlin
@ControllerAdvice
class AuthenticationExceptionHandler {

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationError(ex: AuthenticationException): ResponseEntity&lt;ErrorResponse&gt; {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse("AUTHENTICATION_FAILED", ex.message))
    }

    @ExceptionHandler(AuthorizationException::class)
    fun handleAuthorizationError(ex: AuthorizationException): ResponseEntity&lt;ErrorResponse&gt; {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse("ACCESS_DENIED", "Insufficient permissions"))
    }
}
```

### Token Refresh Pattern

```kotlin
@Component
class TokenManager(
    private val iamClient: IAMClient
) {
    private var currentToken: String? = null
    private var refreshToken: String? = null
    private var tokenExpiry: Instant? = null

    suspend fun getValidToken(): String {
        if (isTokenExpired()) {
            refreshToken()
        }
        return currentToken ?: throw IllegalStateException("No valid token available")
    }

    private suspend fun refreshToken() {
        val refreshToken = this.refreshToken ?: throw IllegalStateException("No refresh token")

        try {
            val response = iamClient.refreshToken(refreshToken)
            this.currentToken = response.accessToken
            this.refreshToken = response.refreshToken
            this.tokenExpiry = response.expiresAt
        } catch (e: Exception) {
            // Handle refresh failure, might need to re-authenticate
            handleTokenRefreshFailure(e)
        }
    }
}
```

---

_Best practices for secure authentication and authorization with the EAF IAM Client SDK._

---
sidebar_position: 2
title: Getting Started
---

# Getting Started with EAF IAM Client SDK

Quick start guide for integrating authentication and authorization into your service.

## 📦 Installation

Add the dependency to your project:

```kotlin
dependencies {
    implementation("com.axians.eaf:eaf-iam-client-sdk:${eafVersion}")
}
```

## ⚙️ Configuration

Configure IAM integration in your `application.yml`:

```yaml
eaf:
  iam:
    base-url: 'https://iam.acci.com'
    client-id: 'my-service'
    client-secret: '${IAM_CLIENT_SECRET}'
    tenant-header: 'X-Tenant-ID'
```

## 🔑 Basic Authentication

### JWT Token Validation

```kotlin
@RestController
class UserController(
    private val authenticationService: AuthenticationService
) {
    @GetMapping("/profile")
    fun getUserProfile(@RequestHeader("Authorization") token: String): UserProfile {
        val authentication = authenticationService.authenticate(token)
        return userService.getProfile(authentication.userId)
    }
}
```

### Service Authentication

```kotlin
@Component
class ExternalServiceClient(
    private val iamClient: IAMClient
) {
    suspend fun callExternalService(): String {
        val token = iamClient.getServiceToken()
        return webClient
            .get()
            .uri("/api/data")
            .header("Authorization", "Bearer $token")
            .retrieve()
            .bodyToMono&lt;String&gt;()
            .awaitSingle()
    }
}
```

## 🛡️ Authorization

### Permission Checking

```kotlin
@RestController
class OrderController(
    private val authorizationService: AuthorizationService
) {
    @PostMapping("/orders")
    fun createOrder(@RequestBody order: CreateOrderRequest): OrderResponse {
        authorizationService.requirePermission("orders:create")
        return orderService.createOrder(order)
    }
}
```

### Role-Based Access

```kotlin
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/users")
fun getUsers(): List&lt;User&gt; {
    return userService.getAllUsers()
}
```

## 🏢 Multi-Tenant Operations

### Tenant Context

```kotlin
@Component
class TenantAwareService(
    private val tenantContext: TenantContext
) {
    fun processData(): String {
        val currentTenant = tenantContext.getCurrentTenant()
        return "Processing data for tenant: $currentTenant"
    }
}
```

---

_Get started with secure, multi-tenant applications using the EAF IAM Client SDK._

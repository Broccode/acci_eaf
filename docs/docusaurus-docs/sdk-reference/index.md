---
sidebar_position: 3
title: SDK Reference
---

# EAF SDK Reference

Welcome to the EAF SDK Reference! This section provides comprehensive documentation for all ACCI EAF
Software Development Kits (SDKs) that enable rapid development of enterprise-grade services.

## 🚀 SDK Overview

The EAF SDK ecosystem consists of three core libraries designed to implement the architectural
principles documented in our [Core Architecture](../architecture/index.md) section:

- **Eventing SDK** - NATS-based event publishing and consumption
- **Event Sourcing SDK** - PostgreSQL-based event store and aggregate persistence
- **IAM Client SDK** - Identity and Access Management integration

Each SDK is built with hexagonal architecture principles, providing clean abstractions while
maintaining high performance and reliability.

## 📦 Available SDKs

### [Eventing SDK](./eventing-sdk/index.md)

**NATS Event Publishing & Consumption**

The Eventing SDK provides high-level abstractions for event-driven communication using NATS as the
underlying messaging infrastructure.

**Key Features:**

- Type-safe event publishing with retry mechanisms
- Resilient event subscription with error handling
- Tenant-aware event routing and filtering
- Built-in serialization and deserialization
- Comprehensive monitoring and observability

### [Event Sourcing SDK](./eventsourcing-sdk/index.md)

**PostgreSQL Event Store & Aggregate Persistence**

The Event Sourcing SDK implements event sourcing patterns with PostgreSQL as the event store,
providing ACID guarantees and high performance.

**Key Features:**

- Aggregate root persistence and retrieval
- Event stream processing and replay
- Snapshot creation and restoration
- Schema evolution and migration support
- Optimistic concurrency control

### [IAM Client SDK](./iam-client-sdk/index.md)

**Identity & Access Management Integration**

The IAM Client SDK provides seamless integration with the ACCI IAM service for authentication,
authorization, and tenant management.

**Key Features:**

- OAuth 2.0 / OpenID Connect integration
- Role-based access control (RBAC)
- Tenant-aware security context
- Token lifecycle management
- Frontend and backend integration patterns

## 🎯 Common Patterns

All EAF SDKs follow consistent design patterns:

### Configuration

```kotlin
@Configuration
class SdkConfiguration {
    @Bean
    fun sdkProperties(): SdkProperties = SdkProperties(
        // Environment-specific configuration
        baseUrl = "\${app.sdk.base-url}",
        timeoutMs = "\${app.sdk.timeout-ms:5000}",
        retryAttempts = "\${app.sdk.retry-attempts:3}"
    )
}
```

### Error Handling

```kotlin
try {
    val result = sdkClient.performOperation(request)
    logger.info("Operation completed successfully: {}", result.id)
    return result
} catch (e: SdkException) {
    logger.error("SDK operation failed: {}", e.message, e)
    throw ServiceException("Operation failed", e)
}
```

### Async Operations

```kotlin
suspend fun performAsyncOperation(): Result&lt;T&gt; = withContext(Dispatchers.IO) {
    sdkClient.performOperationAsync(request)
        .also { logger.debug("Async operation completed") }
}
```

## 🧪 Testing Integration

Each SDK provides comprehensive testing support:

### Test Configuration

```kotlin
@TestConfiguration
class SdkTestConfiguration {
    @Bean
    @Primary
    fun testSdkClient(): SdkClient = mockk&lt;SdkClient&gt;()
}
```

### Integration Testing

```kotlin
@SpringBootTest
@Testcontainers
@Import(SdkTestConfiguration::class)
class SdkIntegrationTest {
    // Test implementation using TestContainers
}
```

## 📊 Performance Considerations

All SDKs are designed for high-performance production environments:

- **Connection Pooling**: Efficient resource utilization
- **Async Processing**: Non-blocking operations where applicable
- **Caching**: Intelligent caching strategies for frequently accessed data
- **Monitoring**: Built-in metrics and health checks
- **Resilience**: Circuit breaker patterns and graceful degradation

## 🔧 Configuration Management

SDKs integrate seamlessly with Spring Boot configuration:

```yaml
# application.yml
app:
  eaf:
    eventing:
      nats-url: 'nats://localhost:4222'
      retry-attempts: 3
      timeout-ms: 5000
    eventsourcing:
      datasource-url: 'jdbc:postgresql://localhost:5432/eventstore'
      snapshot-frequency: 100
    iam:
      base-url: 'https://iam.acci.com'
      client-id: 'service-client'
      tenant-header: 'X-Tenant-ID'
```

## 🔗 Quick Navigation

**Getting Started:**

- [Eventing SDK Quick Start](./eventing-sdk/getting-started.md)
- [Event Sourcing SDK Quick Start](./eventsourcing-sdk/getting-started.md)
- [IAM Client SDK Quick Start](./iam-client-sdk/getting-started.md)

**API Reference:**

- [Eventing SDK API](./eventing-sdk/api-reference.md)
- [Event Sourcing SDK API](./eventsourcing-sdk/api-reference.md)
- [IAM Client SDK API](./iam-client-sdk/api-reference.md)

**Configuration:**

- [Eventing SDK Configuration](./eventing-sdk/configuration.md)
- [Event Sourcing SDK Configuration](./eventsourcing-sdk/configuration.md)
- [IAM Client SDK Configuration](./iam-client-sdk/configuration.md)

## 🆘 Support & Troubleshooting

For SDK-specific issues:

- Check the individual SDK troubleshooting guides
- Review common error patterns and solutions
- Consult the API reference for method signatures and parameters
- Refer to the configuration documentation for setup issues

---

_All EAF SDKs are actively maintained and follow semantic versioning. Breaking changes are
communicated through architectural decision records and migration guides._

# Core Services Overview

ACCI EAF provides a suite of core services that handle common enterprise application concerns,
allowing you to focus on your business logic while leveraging robust, tested functionality.

## Currently Available Services

### 🔐 IAM Service & Security Context

**Identity and Access Management**

The IAM Service handles authentication, authorization, and multi-tenancy:

- **Local User Management**: Create and manage users within each tenant
- **Federation Support**: Integrate with SAML, OIDC, and LDAP/AD identity providers
- **RBAC/ABAC**: Role-based and attribute-based access control
- **Multi-tenant Isolation**: Strict data separation between tenants
- **Token Management**: JWT-based authentication with secure token lifecycle
- **Security Context**: Automatic context propagation across services and async operations

**Key APIs:**

- `POST /api/v1/tenants` - Create new tenant
- `POST /api/v1/tenants/{tenantId}/users` - Manage tenant users
- `POST /api/v1/auth/token` - Authenticate and get token

📖 **[Security Context Access Guide](./security-context-access.md)** - Learn how to access tenant,
user, and permission context in your services

📖 **[Context Propagation Guide](./context-propagation.md)** - Comprehensive guide for maintaining
context across asynchronous operations, coroutines, and distributed messaging

📖 **[EAF IAM Client SDK](./eaf-iam-client-sdk.md)** - Complete guide for integrating JWT
authentication, RBAC, and multi-tenant context in your Spring Boot applications

### ⚡ Axon Framework Integration

**Event Sourcing & CQRS Platform**

Complete Axon Framework v5 integration with EAF-specific patterns and training:

- **Event Storage**: Custom `EafPostgresEventStorageEngine` with multi-tenancy support
- **CQRS Architecture**: Command/Query separation with optimized read models
- **Event Sourcing**: Immutable event streams with powerful replay capabilities
- **Multi-tenant Events**: Tenant-aware event storage and projection handling
- **NATS Integration**: Seamless event publishing via NATS/JetStream
- **Testing Support**: Comprehensive testing strategies and fixtures

**Key Features:**

- Custom tracking tokens for global event ordering
- Security context propagation across command/event handlers
- Performance-optimized event storage with PostgreSQL
- Saga patterns for cross-aggregate coordination
- Event upcasting for schema evolution

📖 **[Axon Framework Training Hub](./axon-framework/index.md)** - Complete training materials
covering core concepts, EAF integration, and hands-on labs

📖 **[Hands-on Labs](./axon-framework/lab-01-order-management.md)** - Practical exercises for
building event-sourced systems

### 📡 NATS Event Infrastructure

**Distributed Event Messaging**

High-performance event streaming and messaging with NATS/JetStream:

- **Event Publishing**: Reliable event publishing with automatic retries
- **Event Consumption**: Durable subscriptions with consumer groups
- **Stream Processing**: Real-time event stream processing
- **Multi-tenant Events**: Tenant-aware event routing and filtering
- **Dead Letter Queues**: Automatic handling of failed message processing

📖 **[NATS Event Publishing](./nats-event-publishing.md)** - Patterns for publishing domain events

📖 **[NATS Event Consumption](./nats-event-consumption.md)** - Building robust event consumers

📖 **[NATS Integration Testing](./nats-integration-testing.md)** - Testing event-driven
architectures

### 🔄 Idempotent Projectors

**Reliable Event Processing**

Framework for building idempotent event projectors and read models:

- **Exactly-Once Processing**: Guaranteed idempotent event handling
- **Automatic Checkpointing**: Reliable progress tracking
- **Error Recovery**: Automatic retry and dead letter handling
- **Multi-tenant Projections**: Tenant-aware projection building
- **Performance Optimization**: Batched processing and caching strategies

📖 **[Idempotent Projectors Guide](./idempotent-projectors.md)** - Building reliable event
projectors

## Planned Services

The following services are in development or planned for future releases:

### 🎫 License Management Service _(Planned)_

**Software Licensing and Activation**

- License creation and entitlement management
- Hardware binding and activation workflows
- Real-time validation APIs
- Complete audit trails

### 🚩 Feature Flag Service _(Planned)_

**Dynamic Feature Management**

- Runtime feature toggles
- User segment targeting
- Environment-specific configurations
- Usage analytics

### ⚙️ Configuration Service _(Planned)_

**Centralized Application Configuration**

- Environment-specific settings
- Dynamic configuration updates
- Secure secrets management
- Configuration validation

## Service Architecture

All core services follow the same architectural patterns:

- **Event-Driven**: Communicate via NATS/JetStream events
- **CQRS/Event Sourcing**: Scalable read/write separation
- **Hexagonal Architecture**: Clean separation of concerns
- **Multi-tenant**: Strict tenant data isolation
- **Resilient**: Circuit breakers, retries, and graceful degradation

## Integration Patterns

### SDK-Based Integration

Each service provides a client SDK for easy integration:

```kotlin
// IAM Client SDK
@Autowired
lateinit var iamClient: IamClient

val user = iamClient.validateToken(token)
val hasPermission = iamClient.hasPermission(user, "product:create")
```

### Event-Based Integration

Services communicate through domain events:

```kotlin
// Listen for tenant creation events
@EventHandler
fun on(event: TenantCreatedEvent) {
    // Initialize tenant-specific resources
}
```

### REST API Integration

Direct HTTP API access for external systems:

```bash
curl -X POST /api/v1/licenses \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"productId": "product-1", "type": "enterprise"}'
```

## Deployment

Core services can be deployed:

- **Independently**: Each service as a separate container
- **Composed**: Using Docker Compose for development
- **Orchestrated**: With Kubernetes for production scaling

## Monitoring and Observability

All services include:

- **Health Checks**: Readiness and liveness endpoints
- **Metrics**: Prometheus-compatible metrics
- **Logging**: Structured JSON logging with correlation IDs
- **Tracing**: Distributed tracing support

## Development Guides

### Integration Testing

📖 **[Spring Boot Integration Testing Setup](./spring-boot-integration-testing.md)** - Complete
guide for setting up robust integration tests with Testcontainers, JPA, and PostgreSQL

📖 **[NATS Integration Testing](./nats-integration-testing.md)** - Patterns for testing event-driven
architectures

### Performance & Quality

📖 **[Performance Testing Baseline](./performance-testing-baseline.md)** - Establishing performance
benchmarks and testing strategies

## Getting Started

1. **Development Setup**: Use the provided Docker Compose configuration to start PostgreSQL and NATS
2. **Authentication**: Integrate with IAM service for multi-tenant authentication
3. **Event Architecture**: Choose between direct API calls or event-driven patterns
4. **SDK Integration**: Add the appropriate client SDKs to your project
5. **Training**: Start with the [Axon Framework Training Hub](./axon-framework/index.md) for event
   sourcing

For new services, consider starting with the
[First Service Guide](../getting-started/first-service.md) to understand EAF patterns and
architecture.

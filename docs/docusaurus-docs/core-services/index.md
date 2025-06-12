# Core Services Overview

ACCI EAF provides a suite of core services that handle common enterprise application concerns,
allowing you to focus on your business logic while leveraging robust, tested functionality.

## Available Services

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

### 🎫 License Management Service

**Software Licensing and Activation**

Manage software licenses and activations:

- **License Creation**: Define license types and entitlements
- **Hardware Binding**: Tie licenses to specific hardware configurations
- **Activation Workflows**: Streamlined license activation process
- **Validation APIs**: Real-time license validation for applications
- **Audit Trails**: Complete license usage tracking

### 🚩 Feature Flag Service

**Dynamic Feature Management**

Control feature rollouts and toggle functionality:

- **Dynamic Flags**: Enable/disable features without deployments
- **Targeting Rules**: Show features to specific user segments
- **Environment Management**: Different flag states per environment
- **SDK Integration**: Easy integration with EAF applications
- **Analytics**: Track feature usage and adoption

### ⚙️ Configuration Service

**Centralized Application Configuration**

Manage application settings and operational parameters:

- **Environment-specific Configs**: Different settings per environment
- **Dynamic Updates**: Change configuration without restarts
- **Secure Secrets**: Encrypted storage of sensitive configuration
- **Version Control**: Track configuration changes over time
- **Validation**: Ensure configuration correctness

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

📖 **[Spring Boot Integration Testing Setup](./spring-boot-integration-testing.md)** - Complete guide for setting up robust integration tests with Testcontainers, JPA, and PostgreSQL

### Event Integration

📖 **[NATS Integration Testing](./nats-integration-testing.md)** - Patterns for testing event-driven architectures

## Getting Started

1. **Development Setup**: Use the provided Docker Compose configuration
2. **Service Discovery**: Services register with the EAF service registry
3. **Authentication**: All services require IAM authentication
4. **SDK Integration**: Add the appropriate client SDKs to your project

_This is a placeholder document. Detailed service documentation will be added as each service is
implemented and stabilized._

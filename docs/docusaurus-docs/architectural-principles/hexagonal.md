# Hexagonal Architecture in ACCI EAF

Hexagonal Architecture (also known as Ports and Adapters) ensures that your application core remains
independent of external concerns like databases, messaging systems, and user interfaces.

## Core Concepts

### The Hexagon

The hexagon represents your application core, containing:

- **Domain Layer**: Entities, value objects, domain services
- **Application Layer**: Use cases, application services, ports

### Ports

Ports define interfaces for interacting with the external world:

```kotlin
// Inbound Port (driven by external actors)
interface CreateTenantUseCase {
    fun execute(command: CreateTenantCommand): TenantCreatedEvent
}

// Outbound Port (drives external systems)
interface TenantRepository {
    fun save(tenant: Tenant)
    fun findById(id: TenantId): Tenant?
}
```

### Adapters

Adapters implement ports and handle external communication:

```kotlin
// Inbound Adapter (REST Controller)
@RestController
class TenantController(
    private val createTenantUseCase: CreateTenantUseCase
) {
    @PostMapping("/tenants")
    fun createTenant(@RequestBody request: CreateTenantRequest) {
        // Adapter logic
    }
}

// Outbound Adapter (Database Repository)
@Repository
class JpaTenantRepository : TenantRepository {
    // Database interaction logic
}
```

## EAF Project Structure

The ACCI EAF CLI generates services with this structure:

```
src/main/kotlin/com/axians/eaf/service/
├── domain/
│   ├── model/           # Entities, Aggregates, Value Objects
│   ├── event/           # Domain Events
│   └── service/         # Domain Services
├── application/
│   ├── port/
│   │   ├── in/          # Inbound Ports (Use Cases)
│   │   └── out/         # Outbound Ports
│   └── service/         # Application Services
└── infrastructure/
    ├── adapter/
    │   ├── in/
    │   │   ├── web/     # REST Controllers, Hilla Endpoints
    │   │   └── messaging/ # Event Handlers
    │   └── out/
    │       ├── persistence/ # Repositories
    │       └── messaging/   # Event Publishers
    └── config/          # Spring Configuration
```

## Benefits in ACCI EAF

### Testability

- Domain logic can be tested without infrastructure
- Adapters can be tested independently
- Easy to mock external dependencies

### Technology Independence

- Switch from JPA to JDBC without affecting domain
- Change from REST to GraphQL without domain changes
- Migrate from PostgreSQL to another database

### EAF SDK Integration

- Use EAF Event Store SDK in persistence adapters
- Integrate EAF Eventing SDK in messaging adapters
- Apply EAF IAM SDK in security adapters

## Best Practices

1. **Keep the domain pure**: No framework dependencies in domain layer
2. **Dependency direction**: Always point inward toward the domain
3. **Interface segregation**: Create focused, single-purpose ports
4. **Adapter responsibility**: Handle serialization, validation, error translation

## Learn More

For comprehensive implementation guidance with step-by-step examples and code samples, see the
[Hexagonal Architecture Implementation Guide](./hexagonal-architecture-guide.md).

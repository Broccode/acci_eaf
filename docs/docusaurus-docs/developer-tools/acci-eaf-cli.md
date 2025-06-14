---
sidebar_position: 1
title: ACCI EAF CLI - Service & Component Generator
---

# ACCI EAF CLI - Service & Component Generator

The ACCI EAF CLI provides powerful generators for creating fully structured Kotlin/Spring backend
services and CQRS/ES components following hexagonal architecture principles, complete with testing
setup and EAF SDK integration.

## Quick Start

Generate a new service in the `apps` directory:

```bash
nx run acci-eaf-cli:run -- --args="generate service user-management"
```

Generate a service in the `libs` directory:

```bash
nx run acci-eaf-cli:run -- --args="generate service shared-utils --path=libs"
```

## Command Reference

### Service Generation

```bash
nx run acci-eaf-cli:run -- --args="generate service <service-name> [options]"
```

### CQRS/ES Component Generation

```bash
# Generate aggregate with creation command, event, and test
nx run acci-eaf-cli:run -- --args="generate aggregate <AggregateName> --service=<service-name>"

# Add command to existing aggregate
nx run acci-eaf-cli:run -- --args="generate command <CommandName> --aggregate=<AggregateName>"

# Add event to existing aggregate
nx run acci-eaf-cli:run -- --args="generate event <EventName> --aggregate=<AggregateName>"

# Generate projector for event handling
nx run acci-eaf-cli:run -- --args="generate projector <ProjectorName> --service=<service-name> --event=<EventName>"
```

### Parameters

- **`<service-name>`** (required) - The name of the service to generate
  - Must be lowercase
  - Can contain letters, numbers, and hyphens
  - Must start with a letter and end with a letter or number
  - Example: `user-management`, `payment-service`, `notification-hub`

### Options

- **`--path=<targetPath>`** (optional) - Target directory for the service
  - **`apps`** (default) - For application services
  - **`libs`** - For library/shared services

### Examples

```bash
# Generate a user management service
nx run acci-eaf-cli:run -- --args="generate service user-management"

# Generate a shared library
nx run acci-eaf-cli:run -- --args="generate service common-utils --path=libs"

# Generate an analytics service
nx run acci-eaf-cli:run -- --args="generate service analytics-engine"
```

## Generated Structure

The CLI generates a complete service structure following hexagonal architecture principles:

### Directory Layout

```
your-service/
├── build.gradle.kts              # Gradle build configuration
├── project.json                  # Nx project configuration
└── src/
    ├── main/
    │   ├── kotlin/com/axians/eaf/yourservice/
    │   │   ├── YourServiceApplication.kt                    # Spring Boot main class
    │   │   ├── application/                                 # Application Layer
    │   │   │   ├── port/
    │   │   │   │   ├── input/                              # Input ports (use cases)
    │   │   │   │   │   └── SampleYourServiceUseCase.kt
    │   │   │   │   └── output/                             # Output ports
    │   │   │   └── service/                                # Application services
    │   │   │       └── SampleYourServiceService.kt
    │   │   ├── domain/                                     # Domain Layer
    │   │   │   ├── model/                                  # Domain models
    │   │   │   │   └── SampleYourService.kt
    │   │   │   └── port/                                   # Domain ports
    │   │   └── infrastructure/                             # Infrastructure Layer
    │   │       ├── adapter/
    │   │       │   ├── input/web/                         # Web controllers
    │   │       │   │   └── SampleYourServiceController.kt
    │   │       │   └── output/persistence/                 # Repository implementations
    │   │       └── config/                                 # Configuration classes
    │   └── resources/
    │       └── application.yml                             # Application configuration
    └── test/
        └── kotlin/com/axians/eaf/yourservice/
            ├── application/service/
            │   └── SampleYourServiceServiceTest.kt         # Unit tests
            └── architecture/
                └── ArchitectureTest.kt                     # ArchUnit tests
```

### Generated Files

#### 1. Build Configuration

**`build.gradle.kts`** - Complete Gradle build setup with:

- Kotlin, Spring Boot, and JPA plugins
- All EAF SDK dependencies (`eaf-core`, `eaf-eventing-sdk`, `eaf-eventsourcing-sdk`)
- Testing dependencies (JUnit 5, MockK, ArchUnit, Testcontainers)
- Spotless formatting configuration

#### 2. Application Class

**`YourServiceApplication.kt`** - Spring Boot main application class:

```kotlin
@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])
@ComponentScan(basePackages = ["com.axians.eaf.yourservice"])
class YourServiceApplication

fun main(args: Array<String>) {
    runApplication<YourServiceApplication>(*args)
}
```

#### 3. Domain Layer

**Sample Domain Model** - Example domain entity with proper structure:

```kotlin
data class SampleYourService(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
```

#### 4. Application Layer

**Use Case Interface** - Input port following hexagonal architecture:

```kotlin
interface SampleYourServiceUseCase {
    fun findById(id: String): SampleYourService
    fun create(name: String, description: String? = null): SampleYourService
}
```

**Application Service** - Use case implementation:

```kotlin
@Service
class SampleYourServiceService : SampleYourServiceUseCase {
    override fun findById(id: String): SampleYourService { /* ... */ }
    override fun create(name: String, description: String?): SampleYourService { /* ... */ }
}
```

#### 5. Infrastructure Layer

**REST Controller** - Web adapter with health endpoint:

```kotlin
@RestController
@RequestMapping("/api/v1/sample-yourservice")
class SampleYourServiceController(
    private val sampleUseCase: SampleYourServiceUseCase,
) {
    @GetMapping("/{id}")
    fun getSample(@PathVariable id: String): ResponseEntity<SampleYourService> { /* ... */ }

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> { /* ... */ }
}
```

#### 6. Configuration

**`application.yml`** - Complete configuration with EAF integration:

```yaml
spring:
  application:
    name: your-service

# EAF Local Development Configuration
eaf:
  eventing:
    nats:
      url: 'nats://localhost:4222'
# PostgreSQL Connection (commented template)
# spring:
#   datasource:
#     url: jdbc:postgresql://localhost:5432/eaf_db
#     username: postgres
#     password: password
```

#### 7. Testing Setup

**Unit Tests** - TDD-ready test structure:

```kotlin
class SampleYourServiceServiceTest {
    private val service = SampleYourServiceService()

    @Test
    fun `should find sample by id`() { /* ... */ }

    @Test
    fun `should create new sample`() { /* ... */ }
}
```

**ArchUnit Tests** - Architectural validation:

```kotlin
class ArchitectureTest {
    @Test
    fun `domain layer should not depend on infrastructure layer`() { /* ... */ }

    @Test
    fun `application layer should not depend on infrastructure layer`() { /* ... */ }
}
```

## Integration with Monorepo

The generated service is automatically integrated with the existing monorepo:

### Gradle Integration

- **`settings.gradle.kts`** is updated with the new module
- **Centralized version management** for all dependencies
- **Build targets** are automatically configured

### Nx Integration

- **`project.json`** is generated with proper Nx configuration
- **Build, test, and run targets** are available immediately
- **Dependency graph** includes the new service

### Available Commands

After generation, you can immediately use:

```bash
# Build the service
nx build your-service

# Run tests
nx test your-service

# Start the service
nx run your-service

# Clean build artifacts
nx clean your-service
```

## Customization Guide

### Modifying Templates

The CLI uses a template engine that can be customized by modifying:

- **`TemplateEngine.kt`** - Core template generation logic
- **Method templates** - Individual file templates for each generated file type

### Adding New File Types

To extend the generator with additional files:

1. Add a new template method to `TemplateEngine`
2. Call the template method from `ServiceGenerator.generateSourceFiles()`
3. Update the directory structure creation if needed

### Custom Package Structure

The generated package structure follows the pattern:

```
com.axians.eaf.{service-name-without-hyphens}
```

Hyphens in service names are automatically removed for package naming.

## Best Practices

### Service Naming

- Use **kebab-case** for service names (`user-management`, not `UserManagement`)
- Be **descriptive** but **concise** (`payment-processor`, not `pay`)
- Use **domain-driven** naming that reflects business capabilities

### Development Workflow

1. **Generate the service** using the CLI
2. **Review generated files** and understand the structure
3. **Replace sample code** with your actual domain logic
4. **Add database entities** and repository implementations
5. **Implement business logic** in the application service
6. **Add integration tests** for your specific use cases
7. **Configure database connections** in `application.yml`

### Testing Strategy

The generated tests provide a foundation for:

- **Unit testing** application services
- **Integration testing** with TestContainers
- **Architecture testing** with ArchUnit
- **Contract testing** for REST APIs

## Troubleshooting

### Common Issues

**Service already exists**

```
Error: Service directory already exists: /path/to/service
```

**Solution**: Choose a different service name or remove the existing directory.

**Invalid service name**

```
Error: Service name must be lowercase, start with a letter, and contain only letters, numbers, and hyphens
```

**Solution**: Use a valid service name following the naming conventions.

**Build failures**

```
Error: Could not find settings.gradle.kts in project hierarchy
```

**Solution**: Run the CLI from the project root directory.

### Getting Help

```bash
# General help
nx run acci-eaf-cli:run -- --args="--help"

# Service generation help
nx run acci-eaf-cli:run -- --args="generate service --help"

# CQRS/ES component generation help
nx run acci-eaf-cli:run -- --args="generate aggregate --help"
nx run acci-eaf-cli:run -- --args="generate command --help"
nx run acci-eaf-cli:run -- --args="generate event --help"
nx run acci-eaf-cli:run -- --args="generate projector --help"
```

---

## CQRS/ES Component Generation

The CLI also provides specialized generators for creating CQRS/ES components within existing
services, following Domain-Driven Design principles and EAF SDK patterns.

### Generate Aggregate

Create a new Domain Aggregate with creation command, event, and test files:

```bash
nx run acci-eaf-cli:run -- --args="generate aggregate User --service=user-management"
```

**What it generates:**

- `CreateUserCommand.kt` - Creation command data class in `domain/command` package
- `UserCreatedEvent.kt` - Creation event data class in `domain/event` package
- `User.kt` - Aggregate root class in `domain/model` with `@EafAggregate` annotation
- `UserTest.kt` - TDD-style unit test in test directory

**Usage:**

```bash
nx run acci-eaf-cli:run -- --args="generate aggregate <AggregateName> --service=<service-name>"
```

**Parameters:**

- `<AggregateName>` - PascalCase name (e.g., `User`, `OrderItem`)
- `--service` - Target service name (e.g., `user-management`)

### Generate Command

Add a new command to an existing aggregate:

```bash
nx run acci-eaf-cli:run -- --args="generate command UpdateUserProfile --aggregate=User"
```

**What it generates:**

- `UpdateUserProfileCommand.kt` - New command data class
- Adds `@EafCommandHandler` method stub to the existing `User` aggregate

**Usage:**

```bash
nx run acci-eaf-cli:run -- --args="generate command <CommandName> --aggregate=<AggregateName>"
```

**Parameters:**

- `<CommandName>` - PascalCase command name (e.g., `UpdateUserProfile`, `DeactivateUser`)
- `--aggregate` - Target aggregate name (e.g., `User`)

### Generate Event

Add a new domain event to an existing aggregate:

```bash
nx run acci-eaf-cli:run -- --args="generate event UserProfileUpdated --aggregate=User"
```

**What it generates:**

- `UserProfileUpdatedEvent.kt` - New event data class
- Adds `@EafEventSourcingHandler` method stub to the existing `User` aggregate

**Usage:**

```bash
nx run acci-eaf-cli:run -- --args="generate event <EventName> --aggregate=<AggregateName>"
```

**Parameters:**

- `<EventName>` - PascalCase event name (e.g., `UserProfileUpdated`, `UserDeactivated`)
- `--aggregate` - Target aggregate name (e.g., `User`)

### Generate Projector

Create a new projector to handle domain events:

```bash
nx run acci-eaf-cli:run -- --args="generate projector UserProfileProjector --service=user-management --event=UserProfileUpdated"
```

**What it generates:**

- `UserProfileProjector.kt` - Projector class in `infrastructure/adapter/input/messaging` with
  `@NatsJetStreamListener` annotation

**Usage:**

```bash
nx run acci-eaf-cli:run -- --args="generate projector <ProjectorName> --service=<service-name> --event=<EventName>"
```

**Parameters:**

- `<ProjectorName>` - PascalCase projector name (e.g., `UserProfileProjector`)
- `--service` - Target service name (e.g., `user-management`)
- `--event` - Event name to listen for (e.g., `UserProfileUpdated`)

### CQRS/ES Command Examples

```bash
# Generate a complete Order aggregate
nx run acci-eaf-cli:run -- --args="generate aggregate Order --service=order-management"

# Add a new command to cancel orders
nx run acci-eaf-cli:run -- --args="generate command CancelOrder --aggregate=Order"

# Add a corresponding event
nx run acci-eaf-cli:run -- --args="generate event OrderCancelled --aggregate=Order"

# Create a projector to handle order cancellation
nx run acci-eaf-cli:run -- --args="generate projector OrderCancellationProjector --service=order-management --event=OrderCancelled"
```

### Generated Code Structure

**Aggregate Example:**

```kotlin
@EafAggregate
data class User(
    @AggregateIdentifier
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    // ... other fields
) {
    @EafCommandHandler
    constructor(command: CreateUserCommand) : this(
        name = command.name,
        email = command.email
    ) {
        // Apply creation event
        AggregateLifecycle.apply(UserCreatedEvent(id, command.name, command.email))
    }

    @EafEventSourcingHandler
    fun on(event: UserCreatedEvent) {
        // Handle state reconstruction from event
    }
}
```

**Projector Example:**

```kotlin
@Component
class UserProfileProjector {

    @NatsJetStreamListener(subject = "eaf.user_management.user_profile_updated")
    fun handle(event: UserProfileUpdatedEvent) {
        // TODO: Implement projector logic
        // Handle the UserProfileUpdatedEvent
    }
}
```

### EAF SDK Integration

All generated components use proper EAF SDK annotations:

- **`@EafAggregate`** - Marks aggregate root classes
- **`@AggregateIdentifier`** - Identifies the aggregate ID field
- **`@EafCommandHandler`** - Marks command handler methods
- **`@EafEventSourcingHandler`** - Marks event sourcing handlers
- **`@NatsJetStreamListener`** - Configures NATS JetStream event listeners

---

## What's Next?

### After generating a service

1. **Explore the code** - Understand the hexagonal architecture structure
2. **Run the tests** - Verify everything works out of the box
3. **Start customizing** - Replace sample code with your domain logic
4. **Add persistence** - Configure database connections and repositories
5. **Implement business logic** - Build your actual use cases
6. **Add more tests** - Expand the test coverage for your specific needs

### After generating CQRS/ES components

1. **Review generated files** - Understand the CQRS/ES structure and EAF SDK integration
2. **Implement command handlers** - Add your business logic to command handlers
3. **Implement event handlers** - Add state changes to event sourcing handlers
4. **Complete projectors** - Implement read model updates in projector handlers
5. **Add comprehensive tests** - Test your aggregates, commands, and events
6. **Configure event routing** - Set up proper NATS subjects and event routing

For more guidance, see:

- [Hexagonal Architecture Guide](/architectural-principles/hexagonal-architecture-guide)
- [Testing Strategy](/architectural-principles/testing-strategy)
- [TDD Principles](/architectural-principles/tdd)
- [CQRS/ES Architecture](/architectural-principles/cqrs-es)

---
sidebar_position: 1
title: ACCI EAF CLI - Service Generator
---

# ACCI EAF CLI - Service Generator

The ACCI EAF CLI provides a powerful service generator that creates fully structured Kotlin/Spring
backend services following hexagonal architecture principles, complete with testing setup and EAF
SDK integration.

## Quick Start

Generate a new service in the `apps` directory:

```bash
./gradlew :tools:acci-eaf-cli:run --args="generate service user-management"
```

Generate a service in the `libs` directory:

```bash
./gradlew :tools:acci-eaf-cli:run --args="generate service shared-utils --path=libs"
```

## Command Reference

### Basic Usage

```bash
./gradlew :tools:acci-eaf-cli:run --args="generate service <service-name> [options]"
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
./gradlew :tools:acci-eaf-cli:run --args="generate service user-management"

# Generate a shared library
./gradlew :tools:acci-eaf-cli:run --args="generate service common-utils --path=libs"

# Generate an analytics service
./gradlew :tools:acci-eaf-cli:run --args="generate service analytics-engine"
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
./gradlew :tools:acci-eaf-cli:run --args="--help"

# Service generation help
./gradlew :tools:acci-eaf-cli:run --args="generate service --help"
```

## What's Next?

After generating your service:

1. **Explore the code** - Understand the hexagonal architecture structure
2. **Run the tests** - Verify everything works out of the box
3. **Start customizing** - Replace sample code with your domain logic
4. **Add persistence** - Configure database connections and repositories
5. **Implement business logic** - Build your actual use cases
6. **Add more tests** - Expand the test coverage for your specific needs

For more guidance, see:

- [Hexagonal Architecture Guide](/architectural-principles/hexagonal-architecture-guide)
- [Testing Strategy](/architectural-principles/testing-strategy)
- [TDD Principles](/architectural-principles/tdd)

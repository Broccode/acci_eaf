---
sidebar_position: 4
title: Your First Service
---

# Your First Service

Now let's create your first EAF service using the ACCI EAF CLI. You'll learn how to generate a
properly structured service following hexagonal architecture principles and understand the generated
code.

## 🎯 What You'll Learn

- How to use the ACCI EAF CLI for service generation
- Understanding hexagonal architecture structure
- Domain, application, and infrastructure layer separation
- TDD setup and failing tests approach
- EAF SDK integrations

## 🔧 Using the ACCI EAF CLI

The ACCI EAF CLI is a powerful code generation tool that creates fully structured services following
our architectural patterns.

### Generate Your First Service

Let's create a user profile service:

```bash
# Generate a new service in the apps directory
nx run acci-eaf-cli:run -- --args="generate service user-profile"
```

You should see output similar to:

```
✅ Creating service directory: apps/user-profile
✅ Generating build.gradle.kts
✅ Generating project.json for Nx
✅ Creating source directory structure
✅ Generating domain layer files
✅ Generating application layer files
✅ Generating infrastructure layer files
✅ Generating test files
✅ Updating settings.gradle.kts
✅ Service 'user-profile' generated successfully!
```

### Alternative: Generate in libs Directory

For shared services, you can generate in the `libs` directory:

```bash
# Generate a shared service in libs
nx run acci-eaf-cli:run -- --args="generate service notification-client --path=libs"
```

## 📁 Understanding the Generated Structure

Let's explore what the CLI generated for your `user-profile` service:

```
apps/user-profile/
├── build.gradle.kts                 # Gradle build configuration
├── project.json                     # Nx project configuration
└── src/
    ├── main/
    │   ├── kotlin/com/axians/eaf/userprofile/
    │   │   ├── UserProfileApplication.kt         # Spring Boot application
    │   │   ├── domain/                           # Domain Layer
    │   │   │   ├── model/
    │   │   │   │   └── SampleUserProfile.kt     # Domain model
    │   │   │   └── port/                        # Domain ports
    │   │   ├── application/                     # Application Layer
    │   │   │   ├── port/
    │   │   │   │   ├── input/                   # Use cases (input ports)
    │   │   │   │   │   └── SampleUserProfileUseCase.kt
    │   │   │   │   └── output/                  # Output ports
    │   │   │   └── service/                     # Application services
    │   │   │       └── SampleUserProfileService.kt
    │   │   └── infrastructure/                  # Infrastructure Layer
    │   │       ├── adapter/
    │   │       │   ├── input/web/              # REST controllers
    │   │       │   │   └── SampleUserProfileController.kt
    │   │       │   └── output/persistence/      # Repository implementations
    │   │       └── config/                      # Configuration classes
    │   └── resources/
    │       └── application.yml                  # Application configuration
    └── test/
        └── kotlin/com/axians/eaf/userprofile/
            ├── application/service/
            │   └── SampleUserProfileServiceTest.kt  # Unit tests
            └── architecture/
                └── ArchitectureTest.kt             # ArchUnit tests
```

## 🏗️ Hexagonal Architecture Explained

The generated structure follows hexagonal architecture (ports and adapters) principles:

### Domain Layer (Center)

```kotlin
// Domain model - pure business logic, no external dependencies
data class SampleUserProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    // Business rules and invariants go here
    fun updateProfile(newName: String, newEmail: String): SampleUserProfile {
        require(newName.isNotBlank()) { "Name cannot be blank" }
        require(newEmail.contains("@")) { "Email must be valid" }

        return copy(
            name = newName,
            email = newEmail,
            updatedAt = Instant.now()
        )
    }
}
```

### Application Layer (Orchestration)

```kotlin
// Input port - defines what the application can do
interface SampleUserProfileUseCase {
    fun findById(id: String): SampleUserProfile?
    fun create(name: String, email: String): SampleUserProfile
    fun update(id: String, name: String, email: String): SampleUserProfile
}

// Application service - implements use cases
@Service
class SampleUserProfileService : SampleUserProfileUseCase {

    override fun findById(id: String): SampleUserProfile? {
        // Implementation will use output ports (repositories)
        return SampleUserProfile(
            id = id,
            name = "Sample User",
            email = "user@example.com"
        )
    }

    override fun create(name: String, email: String): SampleUserProfile {
        // Validation and business logic
        val profile = SampleUserProfile(name = name, email = email)
        // Would save via repository port
        return profile
    }

    override fun update(id: String, name: String, email: String): SampleUserProfile {
        // Find existing, update, and save
        val existing = findById(id) ?: throw IllegalArgumentException("Profile not found")
        return existing.updateProfile(name, email)
    }
}
```

### Infrastructure Layer (Adapters)

```kotlin
// Input adapter - REST API
@RestController
@RequestMapping("/api/v1/user-profiles")
class SampleUserProfileController(
    private val userProfileUseCase: SampleUserProfileUseCase,
) {

    @GetMapping("/{id}")
    fun getUserProfile(@PathVariable id: String): ResponseEntity<SampleUserProfile> {
        val profile = userProfileUseCase.findById(id)
        return if (profile != null) {
            ResponseEntity.ok(profile)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    fun createUserProfile(@RequestBody request: CreateUserProfileRequest): ResponseEntity<SampleUserProfile> {
        val profile = userProfileUseCase.create(request.name, request.email)
        return ResponseEntity.status(HttpStatus.CREATED).body(profile)
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "UP", "service" to "user-profile"))
    }
}

data class CreateUserProfileRequest(
    val name: String,
    val email: String,
)
```

## 🧪 Test-Driven Development Setup

The generated service includes TDD-ready tests:

### Unit Test Example

```kotlin
class SampleUserProfileServiceTest {

    private val service = SampleUserProfileService()

    @Test
    fun `should create user profile with valid data`() {
        // Given
        val name = "John Doe"
        val email = "john.doe@example.com"

        // When
        val result = service.create(name, email)

        // Then
        assertThat(result.name).isEqualTo(name)
        assertThat(result.email).isEqualTo(email)
        assertThat(result.id).isNotBlank()
        assertThat(result.createdAt).isNotNull()
    }

    @Test
    fun `should find user profile by id`() {
        // Given
        val id = "test-id"

        // When
        val result = service.findById(id)

        // Then
        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo(id)
    }

    @Test
    fun `should return null for non-existent profile`() {
        // This test would fail initially (TDD red phase)
        // Implement proper repository integration to make it pass

        // Given
        val nonExistentId = "non-existent-id"

        // When
        val result = service.findById(nonExistentId)

        // Then
        // This assertion will help guide proper implementation
        assertThat(result).isNull()
    }
}
```

### Architecture Tests

```kotlin
class ArchitectureTest {

    private val importedClasses = ClassFileImporter()
        .importPackages("com.axians.eaf.userprofile")

    @Test
    fun `domain layer should not depend on infrastructure layer`() {
        val rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .because("Domain layer must be independent of infrastructure concerns")

        rule.check(importedClasses)
    }

    @Test
    fun `application layer should not depend on infrastructure layer`() {
        val rule = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .because("Application layer should only depend on domain layer")

        rule.check(importedClasses)
    }

    @Test
    fun `infrastructure layer can depend on application and domain layers`() {
        val rule = classes()
            .that().resideInAPackage("..infrastructure..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..infrastructure..",
                "..application..",
                "..domain..",
                "java..",
                "kotlin..",
                "org.springframework..",
                "com.axians.eaf.."
            )

        rule.check(importedClasses)
    }
}
```

## ⚙️ Configuration and Dependencies

### Build Configuration

The generated `build.gradle.kts` includes:

```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.jlleitschuh.gradle.ktlint")
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // EAF SDKs
    implementation(project(":libs:eaf-core"))
    implementation(project(":libs:eaf-eventing-sdk"))
    implementation(project(":libs:eaf-eventsourcing-sdk"))

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk")
    testImplementation("com.tngtech.archunit:archunit-junit5")
}
```

### Application Configuration

The generated `application.yml` includes EAF integrations:

```yaml
spring:
  application:
    name: user-profile

# EAF Local Development Configuration
eaf:
  eventing:
    nats:
      url: 'nats://localhost:4222'
# PostgreSQL Connection (template)
# spring:
#   datasource:
#     url: jdbc:postgresql://localhost:5432/eaf_db
#     username: postgres
#     password: password
```

## 🚀 Build and Run Your Service

### Build the Service

```bash
# Build your new service
nx build user-profile

# Run tests
nx test user-profile

# Check code formatting
nx run user-profile:ktlintCheck
```

### Run the Service

```bash
# Start the service
nx run user-profile:run

# Alternative: Use bootRun for Spring Boot features
nx run user-profile:bootRun
```

### Test the Service

```bash
# Test health endpoint
curl -s http://localhost:8080/actuator/health

# Test your API endpoints
curl -s http://localhost:8080/api/v1/user-profiles/health

# Create a user profile (this will use the sample implementation)
curl -X POST http://localhost:8080/api/v1/user-profiles \
    -H "Content-Type: application/json" \
    -d '{"name": "John Doe", "email": "john.doe@example.com"}'
```

## 🎯 Key Concepts Recap

### Hexagonal Architecture Benefits

- **Testability**: Domain logic isolated from external concerns
- **Flexibility**: Easy to swap implementations (databases, message brokers)
- **Maintainability**: Clear separation of concerns
- **Independent Development**: Teams can work on different layers independently

### Domain-Driven Design Elements

- **Domain Model**: `SampleUserProfile` represents the business entity
- **Use Cases**: Clear definition of what the application can do
- **Business Rules**: Validation and invariants in the domain model
- **Ubiquitous Language**: Method and variable names reflect business terminology

### Test-Driven Development

- **Failing Tests**: Architecture tests and unit tests guide implementation
- **Red-Green-Refactor**: Start with red tests, make them green, then refactor
- **Living Documentation**: Tests describe expected behavior
- **Confidence**: Comprehensive tests enable safe refactoring

## 🔧 Customization Points

Before moving to the next step, consider these customization opportunities:

1. **Domain Model**: Replace the sample with your actual business entity
2. **Use Cases**: Define the specific operations your service needs
3. **Database Integration**: Add JPA entities and repositories
4. **Event Publishing**: Integrate NATS for event-driven communication
5. **Validation**: Add proper input validation and error handling

## ✅ Success Criteria

You've successfully created your first EAF service if:

- ✅ Service generates without errors
- ✅ All tests pass (including architecture tests)
- ✅ Service starts successfully
- ✅ Health endpoint responds correctly
- ✅ You understand the hexagonal architecture structure

## 🚀 Next Steps

Now that you have a working service structure, let's implement a complete feature! Continue to
[Hello World Example](./hello-world-example.md) to build a real user profile management system using
TDD and DDD principles.

---

**Great progress!** You've created your first EAF service following hexagonal architecture. Next,
we'll bring it to life with actual functionality! 🎉

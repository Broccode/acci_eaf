---
sidebar_position: 2
title: Hexagonal Architecture Implementation Guide
---

# Hexagonal Architecture Implementation Guide

The EAF provides comprehensive support for implementing Hexagonal Architecture (also known as Ports
and Adapters) in your Kotlin/Spring services. This guide demonstrates how to structure your services
according to hexagonal principles using the EAF SDK.

## Overview

Hexagonal Architecture promotes the separation of concerns by organizing code into distinct layers:

- **Domain Layer**: Contains business logic and domain entities
- **Application Layer**: Contains use cases and orchestrates domain operations
- **Infrastructure Layer**: Contains adapters that connect to external systems

The EAF provides port interfaces and patterns to help you implement this architecture correctly.

## Core Concepts

### The Hexagon

The hexagon represents your application core, containing:

- **Domain Layer**: Entities, value objects, domain services
- **Application Layer**: Use cases, application services, ports

### Ports

Ports define interfaces for interacting with the external world:

- **Inbound Ports**: Define what your application can do (use cases)
- **Outbound Ports**: Define what your application needs from external systems

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

Adapters implement the ports and handle the technical details:

- **Inbound Adapters**: Translate external requests into domain operations (e.g., REST controllers,
  message listeners)
- **Outbound Adapters**: Implement infrastructure concerns (e.g., database repositories, external
  API clients)

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

## EAF Port Interfaces

The EAF provides two core port interfaces in the `eaf-core` module:

### InboundPort&lt;C, R&gt;

```kotlin
interface InboundPort<C, R> {
    fun handle(command: C): R
}
```

Use this interface for defining use cases that accept commands and return results.

### OutboundPort

```kotlin
interface OutboundPort
```

This is a marker interface for outbound dependencies. Extend it to define your specific outbound
contracts.

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

## Step-by-Step Implementation

### 1. Define Your Domain Model

Start by creating your domain entities in the `domain/model` package:

```kotlin
package com.yourcompany.yourservice.domain.model

data class User(
    val id: String,
    val username: String,
    val email: String,
    val tenantId: String,
)
```

### 2. Define Outbound Ports

Create interfaces for external dependencies in `domain/port/out`:

```kotlin
package com.yourcompany.yourservice.domain.port.out

import com.axians.eaf.core.hexagonal.port.OutboundPort
import com.yourcompany.yourservice.domain.model.User

interface UserRepository : OutboundPort {
    fun save(user: User): User
    fun findById(userId: String, tenantId: String): User?
    fun findByUsername(username: String, tenantId: String): User?
}
```

### 3. Define Commands and Results

Create data classes for your use case inputs and outputs:

```kotlin
package com.yourcompany.yourservice.application.port.`in`

data class CreateUserCommand(
    val username: String,
    val email: String,
    val tenantId: String,
)

data class CreateUserResult(
    val userId: String,
    val username: String,
    val email: String,
    val tenantId: String,
)
```

### 4. Define Inbound Ports (Use Cases)

Create interfaces for your use cases in `application/port/in`:

```kotlin
package com.yourcompany.yourservice.application.port.`in`

import com.axians.eaf.core.hexagonal.port.InboundPort

interface CreateUserUseCase : InboundPort<CreateUserCommand, CreateUserResult>
```

### 5. Implement Application Services

Create services that implement your use cases in `application/service`:

```kotlin
package com.yourcompany.yourservice.application.service

import com.yourcompany.yourservice.application.port.`in`.CreateUserUseCase
import com.yourcompany.yourservice.application.port.`in`.CreateUserCommand
import com.yourcompany.yourservice.application.port.`in`.CreateUserResult
import com.yourcompany.yourservice.domain.port.out.UserRepository
import com.yourcompany.yourservice.domain.model.User
import java.util.UUID

class CreateUserService(
    private val userRepository: UserRepository,
) : CreateUserUseCase {

    override fun handle(command: CreateUserCommand): CreateUserResult {
        // Validate business rules
        validateCommand(command)

        // Check if user already exists
        val existingUser = userRepository.findByUsername(command.username, command.tenantId)
        if (existingUser != null) {
            throw IllegalArgumentException("User already exists")
        }

        // Create and save user
        val user = User(
            id = UUID.randomUUID().toString(),
            username = command.username,
            email = command.email,
            tenantId = command.tenantId
        )

        val savedUser = userRepository.save(user)

        return CreateUserResult(
            userId = savedUser.id,
            username = savedUser.username,
            email = savedUser.email,
            tenantId = savedUser.tenantId
        )
    }

    private fun validateCommand(command: CreateUserCommand) {
        require(command.username.isNotBlank()) { "Username cannot be blank" }
        require(command.email.isNotBlank()) { "Email cannot be blank" }
        require(command.tenantId.isNotBlank()) { "Tenant ID cannot be blank" }
        require(command.email.contains("@")) { "Email must be valid" }
    }
}
```

### 6. Implement Inbound Adapters

#### REST Controller (Spring WebMVC)

```kotlin
package com.yourcompany.yourservice.infrastructure.adapter.`in`.web

import com.yourcompany.yourservice.application.port.`in`.CreateUserUseCase
import com.yourcompany.yourservice.application.port.`in`.CreateUserCommand
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpStatus

@RestController
@RequestMapping("/api/v1/users")
class UserRestController(
    private val createUserUseCase: CreateUserUseCase,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(@RequestBody request: CreateUserRequest): CreateUserResponse {
        val command = CreateUserCommand(
            username = request.username,
            email = request.email,
            tenantId = extractTenantId() // Extract from security context
        )

        val result = createUserUseCase.handle(command)

        return CreateUserResponse(
            userId = result.userId,
            username = result.username,
            email = result.email
        )
    }

    private fun extractTenantId(): String {
        // Extract tenant ID from JWT token or security context
        // Implementation depends on your authentication setup
        return "tenant-id"
    }
}

data class CreateUserRequest(
    val username: String,
    val email: String,
)

data class CreateUserResponse(
    val userId: String,
    val username: String,
    val email: String,
)
```

#### Hilla Endpoint

```kotlin
package com.yourcompany.yourservice.infrastructure.adapter.`in`.web

import com.vaadin.hilla.Endpoint
import com.yourcompany.yourservice.application.port.`in`.CreateUserUseCase
import com.yourcompany.yourservice.application.port.`in`.CreateUserCommand

@Endpoint
class UserEndpoint(
    private val createUserUseCase: CreateUserUseCase,
) {

    fun createUser(request: CreateUserRequest): CreateUserResponse {
        val command = CreateUserCommand(
            username = request.username,
            email = request.email,
            tenantId = extractTenantId()
        )

        val result = createUserUseCase.handle(command)

        return CreateUserResponse(
            userId = result.userId,
            username = result.username,
            email = result.email
        )
    }
}
```

#### NATS Event Listener

```kotlin
package com.yourcompany.yourservice.infrastructure.adapter.`in`.messaging

import com.yourcompany.yourservice.application.port.`in`.CreateUserUseCase
import com.yourcompany.yourservice.application.port.`in`.CreateUserCommand
// Note: Actual NATS annotations will be provided by eaf-eventing-sdk

class UserEventListener(
    private val createUserUseCase: CreateUserUseCase,
) {

    // @NatsJetStreamListener("user.create") // Example annotation
    fun handleUserCreationEvent(event: UserCreationEvent) {
        val command = CreateUserCommand(
            username = event.username,
            email = event.email,
            tenantId = event.tenantId
        )

        createUserUseCase.handle(command)
    }
}
```

### 7. Implement Outbound Adapters

#### JPA Repository

```kotlin
package com.yourcompany.yourservice.infrastructure.adapter.out.persistence

import com.yourcompany.yourservice.domain.port.out.UserRepository
import com.yourcompany.yourservice.domain.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
class JpaUserRepository(
    private val jpaRepository: UserJpaRepository,
) : UserRepository {

    override fun save(user: User): User {
        val entity = UserEntity.fromDomain(user)
        val savedEntity = jpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findById(userId: String, tenantId: String): User? {
        return jpaRepository.findByIdAndTenantId(userId, tenantId)?.toDomain()
    }

    override fun findByUsername(username: String, tenantId: String): User? {
        return jpaRepository.findByUsernameAndTenantId(username, tenantId)?.toDomain()
    }
}

interface UserJpaRepository : JpaRepository<UserEntity, String> {
    fun findByIdAndTenantId(id: String, tenantId: String): UserEntity?
    fun findByUsernameAndTenantId(username: String, tenantId: String): UserEntity?
}
```

#### External API Client

```kotlin
package com.yourcompany.yourservice.infrastructure.adapter.out.messaging

import com.yourcompany.yourservice.domain.port.out.NotificationService
import org.springframework.web.client.RestTemplate

class EmailNotificationAdapter(
    private val restTemplate: RestTemplate,
) : NotificationService {

    override fun sendWelcomeEmail(email: String, username: String) {
        val request = EmailRequest(
            to = email,
            subject = "Welcome!",
            body = "Welcome $username!"
        )

        restTemplate.postForObject(
            "/api/emails",
            request,
            EmailResponse::class.java
        )
    }
}
```

## Configuration

Wire everything together using Spring configuration:

```kotlin
package com.yourcompany.yourservice.infrastructure.config

import com.yourcompany.yourservice.application.service.CreateUserService
import com.yourcompany.yourservice.domain.port.out.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApplicationConfig {

    @Bean
    fun createUserService(userRepository: UserRepository): CreateUserService {
        return CreateUserService(userRepository)
    }
}
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

## Testing

### Unit Testing Application Services

```kotlin
class CreateUserServiceTest {

    @Test
    fun `should create user when valid command provided`() {
        // Given
        val userRepository = mockk<UserRepository>()
        val service = CreateUserService(userRepository)

        every { userRepository.findByUsername(any(), any()) } returns null
        every { userRepository.save(any()) } returnsArgument 0

        val command = CreateUserCommand("john", "john@example.com", "tenant-1")

        // When
        val result = service.handle(command)

        // Then
        assertEquals("john", result.username)
        verify { userRepository.save(any()) }
    }
}
```

### Integration Testing Adapters

```kotlin
@SpringBootTest
@Testcontainers
class UserRepositoryIntegrationTest {

    @Container
    val postgres = PostgreSQLContainer("postgres:15")

    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `should save and retrieve user`() {
        // Given
        val user = User("id", "john", "john@example.com", "tenant-1")

        // When
        val saved = userRepository.save(user)
        val retrieved = userRepository.findById(saved.id, "tenant-1")

        // Then
        assertEquals(user.username, retrieved?.username)
    }
}
```

## Best Practices

1. **Keep the domain pure**: No framework dependencies in domain layer
2. **Dependency direction**: Always point inward toward the domain
3. **Interface segregation**: Create focused, single-purpose ports
4. **Adapter responsibility**: Handle serialization, validation, error translation
5. **Use Dependency Injection**: Inject ports into application services via constructor
6. **Validate at Boundaries**: Validate inputs in adapters and application services
7. **Handle Errors Gracefully**: Use proper exception handling and error responses
8. **Test Each Layer**: Unit test application services, integration test adapters
9. **Follow Naming Conventions**: Use clear, descriptive names for ports and adapters
10. **Maintain Tenant Isolation**: Always include tenant context in operations

## Learn More

- [Domain-Driven Design Guide](./ddd.md)
- [CQRS/Event Sourcing Guide](./cqrs-es.md)
- [EAF Event Sourcing SDK Guide](../sdk-reference/eventsourcing-sdk/index.md)
- See the dedicated [Testing Strategy guide](./testing-strategy.md) for end-to-end test practices.

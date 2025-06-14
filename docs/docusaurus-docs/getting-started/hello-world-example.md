---
sidebar_position: 5
title: Hello World Example
---

# Hello World Example

Now let's implement a complete user profile management feature following TDD, DDD, and CQRS/ES
principles. This hands-on example will transform your generated service into a fully functional
system.

## 🎯 What You'll Build

A **User Profile Management** system with:

- **Domain**: `UserProfile` aggregate with creation and update operations
- **Commands**: `CreateUserProfileCommand`, `UpdateUserProfileCommand`
- **Events**: `UserProfileCreatedEvent`, `UserProfileUpdatedEvent`
- **REST API**: CRUD operations with proper error handling
- **Event Publishing**: NATS integration for event-driven communication
- **Testing**: Comprehensive TDD approach with unit and integration tests

## 📋 Prerequisites

Ensure you have:

- ✅ Completed [Your First Service](./first-service.md)
- ✅ Infrastructure services running (NATS, PostgreSQL)
- ✅ `user-profile` service generated and building

## 🔴 Phase 1: TDD - Write Failing Tests

Let's start with the TDD approach - **Red** phase first!

### Domain Model Tests

Replace the generated test file with a comprehensive domain test:

```kotlin
// src/test/kotlin/com/axians/eaf/userprofile/domain/model/UserProfileTest.kt
package com.axians.eaf.userprofile.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant

class UserProfileTest {

    @Test
    fun `should create user profile with valid data`() {
        // Given
        val name = "John Doe"
        val email = "john.doe@example.com"
        val bio = "Software Developer"

        // When
        val profile = UserProfile(
            name = name,
            email = email,
            bio = bio
        )

        // Then
        assertThat(profile.id).isNotBlank()
        assertThat(profile.name).isEqualTo(name)
        assertThat(profile.email).isEqualTo(email)
        assertThat(profile.bio).isEqualTo(bio)
        assertThat(profile.createdAt).isNotNull()
        assertThat(profile.updatedAt).isNotNull()
        assertThat(profile.isActive).isTrue()
    }

    @Test
    fun `should reject blank name`() {
        assertThatThrownBy {
            UserProfile(name = "", email = "test@example.com", bio = "Bio")
        }.isInstanceOf(IllegalArgumentException::class.java)
         .hasMessageContaining("Name cannot be blank")
    }

    @Test
    fun `should reject invalid email`() {
        assertThatThrownBy {
            UserProfile(name = "John", email = "invalid-email", bio = "Bio")
        }.isInstanceOf(IllegalArgumentException::class.java)
         .hasMessageContaining("Email must be valid")
    }

    @Test
    fun `should update profile successfully`() {
        // Given
        val profile = UserProfile(
            name = "John Doe",
            email = "john@example.com",
            bio = "Developer"
        )
        val originalUpdatedAt = profile.updatedAt

        // Wait to ensure timestamp difference
        Thread.sleep(10)

        // When
        val updatedProfile = profile.updateProfile(
            name = "Jane Doe",
            email = "jane@example.com",
            bio = "Senior Developer"
        )

        // Then
        assertThat(updatedProfile.id).isEqualTo(profile.id)
        assertThat(updatedProfile.name).isEqualTo("Jane Doe")
        assertThat(updatedProfile.email).isEqualTo("jane@example.com")
        assertThat(updatedProfile.bio).isEqualTo("Senior Developer")
        assertThat(updatedProfile.updatedAt).isAfter(originalUpdatedAt)
        assertThat(updatedProfile.createdAt).isEqualTo(profile.createdAt)
    }

    @Test
    fun `should deactivate profile`() {
        // Given
        val profile = UserProfile(
            name = "John Doe",
            email = "john@example.com",
            bio = "Developer"
        )

        // When
        val deactivatedProfile = profile.deactivate()

        // Then
        assertThat(deactivatedProfile.isActive).isFalse()
        assertThat(deactivatedProfile.updatedAt).isAfter(profile.updatedAt)
    }
}
```

### Command and Event Tests

Create tests for CQRS components:

```kotlin
// src/test/kotlin/com/axians/eaf/userprofile/domain/command/UserProfileCommandTest.kt
package com.axians.eaf.userprofile.domain.command

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class UserProfileCommandTest {

    @Test
    fun `should create valid CreateUserProfileCommand`() {
        // Given
        val name = "John Doe"
        val email = "john@example.com"
        val bio = "Developer"

        // When
        val command = CreateUserProfileCommand(
            name = name,
            email = email,
            bio = bio
        )

        // Then
        assertThat(command.name).isEqualTo(name)
        assertThat(command.email).isEqualTo(email)
        assertThat(command.bio).isEqualTo(bio)
    }

    @Test
    fun `should create valid UpdateUserProfileCommand`() {
        // Given
        val id = "test-id"
        val name = "Jane Doe"
        val email = "jane@example.com"
        val bio = "Senior Developer"

        // When
        val command = UpdateUserProfileCommand(
            id = id,
            name = name,
            email = email,
            bio = bio
        )

        // Then
        assertThat(command.id).isEqualTo(id)
        assertThat(command.name).isEqualTo(name)
        assertThat(command.email).isEqualTo(email)
        assertThat(command.bio).isEqualTo(bio)
    }
}
```

### Application Service Tests

Update the application service test:

```kotlin
// src/test/kotlin/com/axians/eaf/userprofile/application/service/UserProfileServiceTest.kt
package com.axians.eaf.userprofile.application.service

import com.axians.eaf.userprofile.application.port.output.UserProfileRepository
import com.axians.eaf.userprofile.application.port.output.UserProfileEventPublisher
import com.axians.eaf.userprofile.domain.command.CreateUserProfileCommand
import com.axians.eaf.userprofile.domain.command.UpdateUserProfileCommand
import com.axians.eaf.userprofile.domain.model.UserProfile
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserProfileServiceTest {

    private val repository = mockk<UserProfileRepository>()
    private val eventPublisher = mockk<UserProfileEventPublisher>()
    private val service = UserProfileService(repository, eventPublisher)

    @BeforeEach
    fun setUp() {
        every { eventPublisher.publishUserProfileCreated(any()) } returns Unit
        every { eventPublisher.publishUserProfileUpdated(any()) } returns Unit
    }

    @Test
    fun `should create user profile successfully`() {
        // Given
        val command = CreateUserProfileCommand(
            name = "John Doe",
            email = "john@example.com",
            bio = "Developer"
        )
        every { repository.save(any()) } returnsArgument 0

        // When
        val result = service.createUserProfile(command)

        // Then
        assertThat(result.name).isEqualTo(command.name)
        assertThat(result.email).isEqualTo(command.email)
        assertThat(result.bio).isEqualTo(command.bio)
        verify { repository.save(any()) }
        verify { eventPublisher.publishUserProfileCreated(any()) }
    }

    @Test
    fun `should find user profile by id`() {
        // Given
        val id = "test-id"
        val profile = UserProfile(
            id = id,
            name = "John Doe",
            email = "john@example.com",
            bio = "Developer"
        )
        every { repository.findById(id) } returns profile

        // When
        val result = service.findUserProfileById(id)

        // Then
        assertThat(result).isEqualTo(profile)
        verify { repository.findById(id) }
    }

    @Test
    fun `should throw exception when profile not found`() {
        // Given
        val id = "non-existent-id"
        every { repository.findById(id) } returns null

        // When & Then
        assertThatThrownBy { service.findUserProfileById(id) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("User profile not found")
    }

    @Test
    fun `should update user profile successfully`() {
        // Given
        val existingProfile = UserProfile(
            id = "test-id",
            name = "John Doe",
            email = "john@example.com",
            bio = "Developer"
        )
        val command = UpdateUserProfileCommand(
            id = "test-id",
            name = "Jane Doe",
            email = "jane@example.com",
            bio = "Senior Developer"
        )

        every { repository.findById("test-id") } returns existingProfile
        every { repository.save(any()) } returnsArgument 0

        // When
        val result = service.updateUserProfile(command)

        // Then
        assertThat(result.name).isEqualTo(command.name)
        assertThat(result.email).isEqualTo(command.email)
        assertThat(result.bio).isEqualTo(command.bio)
        verify { repository.save(any()) }
        verify { eventPublisher.publishUserProfileUpdated(any()) }
    }
}
```

## 🟢 Phase 2: TDD - Make Tests Pass (Green)

Now let's implement the domain model and make our tests pass:

### Domain Model Implementation

```kotlin
// src/main/kotlin/com/axians/eaf/userprofile/domain/model/UserProfile.kt
package com.axians.eaf.userprofile.domain.model

import java.time.Instant
import java.util.*

data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val bio: String,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    init {
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(email.contains("@") && email.contains(".")) { "Email must be valid" }
        require(bio.isNotBlank()) { "Bio cannot be blank" }
    }

    fun updateProfile(name: String, email: String, bio: String): UserProfile {
        return copy(
            name = name,
            email = email,
            bio = bio,
            updatedAt = Instant.now()
        )
    }

    fun deactivate(): UserProfile {
        return copy(
            isActive = false,
            updatedAt = Instant.now()
        )
    }

    fun activate(): UserProfile {
        return copy(
            isActive = true,
            updatedAt = Instant.now()
        )
    }
}
```

### Commands and Events

```kotlin
// src/main/kotlin/com/axians/eaf/userprofile/domain/command/UserProfileCommands.kt
package com.axians.eaf.userprofile.domain.command

data class CreateUserProfileCommand(
    val name: String,
    val email: String,
    val bio: String,
)

data class UpdateUserProfileCommand(
    val id: String,
    val name: String,
    val email: String,
    val bio: String,
)

data class DeactivateUserProfileCommand(
    val id: String,
)
```

```kotlin
// src/main/kotlin/com/axians/eaf/userprofile/domain/event/UserProfileEvents.kt
package com.axians.eaf.userprofile.domain.event

import java.time.Instant

data class UserProfileCreatedEvent(
    val id: String,
    val name: String,
    val email: String,
    val bio: String,
    val createdAt: Instant,
)

data class UserProfileUpdatedEvent(
    val id: String,
    val name: String,
    val email: String,
    val bio: String,
    val updatedAt: Instant,
)

data class UserProfileDeactivatedEvent(
    val id: String,
    val deactivatedAt: Instant,
)
```

### Output Ports

```kotlin
// src/main/kotlin/com/axians/eaf/userprofile/application/port/output/UserProfileRepository.kt
package com.axians.eaf.userprofile.application.port.output

import com.axians.eaf.userprofile.domain.model.UserProfile

interface UserProfileRepository {
    fun save(userProfile: UserProfile): UserProfile
    fun findById(id: String): UserProfile?
    fun findByEmail(email: String): UserProfile?
    fun findAll(): List<UserProfile>
    fun delete(id: String)
}
```

```kotlin
// src/main/kotlin/com/axians/eaf/userprofile/application/port/output/UserProfileEventPublisher.kt
package com.axians.eaf.userprofile.application.port.output

import com.axians.eaf.userprofile.domain.event.UserProfileCreatedEvent
import com.axians.eaf.userprofile.domain.event.UserProfileUpdatedEvent
import com.axians.eaf.userprofile.domain.event.UserProfileDeactivatedEvent

interface UserProfileEventPublisher {
    fun publishUserProfileCreated(event: UserProfileCreatedEvent)
    fun publishUserProfileUpdated(event: UserProfileUpdatedEvent)
    fun publishUserProfileDeactivated(event: UserProfileDeactivatedEvent)
}
```

### Input Port (Use Case)

```kotlin
// src/main/kotlin/com/axians/eaf/userprofile/application/port/input/UserProfileUseCase.kt
package com.axians.eaf.userprofile.application.port.input

import com.axians.eaf.userprofile.domain.command.CreateUserProfileCommand
import com.axians.eaf.userprofile.domain.command.UpdateUserProfileCommand
import com.axians.eaf.userprofile.domain.command.DeactivateUserProfileCommand
import com.axians.eaf.userprofile.domain.model.UserProfile

interface UserProfileUseCase {
    fun createUserProfile(command: CreateUserProfileCommand): UserProfile
    fun findUserProfileById(id: String): UserProfile
    fun findUserProfileByEmail(email: String): UserProfile
    fun updateUserProfile(command: UpdateUserProfileCommand): UserProfile
    fun deactivateUserProfile(command: DeactivateUserProfileCommand): UserProfile
    fun getAllUserProfiles(): List<UserProfile>
}
```

### Application Service Implementation

```kotlin
// src/main/kotlin/com/axians/eaf/userprofile/application/service/UserProfileService.kt
package com.axians.eaf.userprofile.application.service

import com.axians.eaf.userprofile.application.port.input.UserProfileUseCase
import com.axians.eaf.userprofile.application.port.output.UserProfileRepository
import com.axians.eaf.userprofile.application.port.output.UserProfileEventPublisher
import com.axians.eaf.userprofile.domain.command.CreateUserProfileCommand
import com.axians.eaf.userprofile.domain.command.UpdateUserProfileCommand
import com.axians.eaf.userprofile.domain.command.DeactivateUserProfileCommand
import com.axians.eaf.userprofile.domain.event.UserProfileCreatedEvent
import com.axians.eaf.userprofile.domain.event.UserProfileUpdatedEvent
import com.axians.eaf.userprofile.domain.event.UserProfileDeactivatedEvent
import com.axians.eaf.userprofile.domain.model.UserProfile
import org.springframework.stereotype.Service

@Service
class UserProfileService(
    private val repository: UserProfileRepository,
    private val eventPublisher: UserProfileEventPublisher,
) : UserProfileUseCase {

    override fun createUserProfile(command: CreateUserProfileCommand): UserProfile {
        // Create domain object
        val userProfile = UserProfile(
            name = command.name,
            email = command.email,
            bio = command.bio
        )

        // Save to repository
        val savedProfile = repository.save(userProfile)

        // Publish event
        eventPublisher.publishUserProfileCreated(
            UserProfileCreatedEvent(
                id = savedProfile.id,
                name = savedProfile.name,
                email = savedProfile.email,
                bio = savedProfile.bio,
                createdAt = savedProfile.createdAt
            )
        )

        return savedProfile
    }

    override fun findUserProfileById(id: String): UserProfile {
        return repository.findById(id)
            ?: throw IllegalArgumentException("User profile not found with id: $id")
    }

    override fun findUserProfileByEmail(email: String): UserProfile {
        return repository.findByEmail(email)
            ?: throw IllegalArgumentException("User profile not found with email: $email")
    }

    override fun updateUserProfile(command: UpdateUserProfileCommand): UserProfile {
        val existingProfile = findUserProfileById(command.id)

        val updatedProfile = existingProfile.updateProfile(
            name = command.name,
            email = command.email,
            bio = command.bio
        )

        val savedProfile = repository.save(updatedProfile)

        // Publish event
        eventPublisher.publishUserProfileUpdated(
            UserProfileUpdatedEvent(
                id = savedProfile.id,
                name = savedProfile.name,
                email = savedProfile.email,
                bio = savedProfile.bio,
                updatedAt = savedProfile.updatedAt
            )
        )

        return savedProfile
    }

    override fun deactivateUserProfile(command: DeactivateUserProfileCommand): UserProfile {
        val existingProfile = findUserProfileById(command.id)
        val deactivatedProfile = existingProfile.deactivate()
        val savedProfile = repository.save(deactivatedProfile)

        // Publish event
        eventPublisher.publishUserProfileDeactivated(
            UserProfileDeactivatedEvent(
                id = savedProfile.id,
                deactivatedAt = savedProfile.updatedAt
            )
        )

        return savedProfile
    }

    override fun getAllUserProfiles(): List<UserProfile> {
        return repository.findAll()
    }
}
```

## 🔧 Phase 3: Infrastructure Implementation

### In-Memory Repository (for simplicity)

```kotlin
// src/main/kotlin/com/axians/eaf/userprofile/infrastructure/adapter/output/persistence/InMemoryUserProfileRepository.kt
package com.axians.eaf.userprofile.infrastructure.adapter.output.persistence

import com.axians.eaf.userprofile.application.port.output.UserProfileRepository
import com.axians.eaf.userprofile.domain.model.UserProfile
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryUserProfileRepository : UserProfileRepository {

    private val profiles = ConcurrentHashMap<String, UserProfile>()

    override fun save(userProfile: UserProfile): UserProfile {
        profiles[userProfile.id] = userProfile
        return userProfile
    }

    override fun findById(id: String): UserProfile? {
        return profiles[id]
    }

    override fun findByEmail(email: String): UserProfile? {
        return profiles.values.find { it.email == email }
    }

    override fun findAll(): List<UserProfile> {
        return profiles.values.toList()
    }

    override fun delete(id: String) {
        profiles.remove(id)
    }
}
```

### NATS Event Publisher

```kotlin
// src/main/kotlin/com/axians/eaf/userprofile/infrastructure/adapter/output/messaging/NatsUserProfileEventPublisher.kt
package com.axians.eaf.userprofile.infrastructure.adapter.output.messaging

import com.axians.eaf.eventing.NatsEventPublisher
import com.axians.eaf.userprofile.application.port.output.UserProfileEventPublisher
import com.axians.eaf.userprofile.domain.event.UserProfileCreatedEvent
import com.axians.eaf.userprofile.domain.event.UserProfileUpdatedEvent
import com.axians.eaf.userprofile.domain.event.UserProfileDeactivatedEvent
import org.springframework.stereotype.Component

@Component
class NatsUserProfileEventPublisher(
    private val natsEventPublisher: NatsEventPublisher,
) : UserProfileEventPublisher {

    override fun publishUserProfileCreated(event: UserProfileCreatedEvent) {
        natsEventPublisher.publish(
            subject = "eaf.user_profile.created",
            tenantId = "default", // In real app, get from security context
            event = event
        )
    }

    override fun publishUserProfileUpdated(event: UserProfileUpdatedEvent) {
        natsEventPublisher.publish(
            subject = "eaf.user_profile.updated",
            tenantId = "default",
            event = event
        )
    }

    override fun publishUserProfileDeactivated(event: UserProfileDeactivatedEvent) {
        natsEventPublisher.publish(
            subject = "eaf.user_profile.deactivated",
            tenantId = "default",
            event = event
        )
    }
}
```

### REST Controller

```kotlin
// src/main/kotlin/com/axians/eaf/userprofile/infrastructure/adapter/input/web/UserProfileController.kt
package com.axians.eaf.userprofile.infrastructure.adapter.input.web

import com.axians.eaf.userprofile.application.port.input.UserProfileUseCase
import com.axians.eaf.userprofile.domain.command.CreateUserProfileCommand
import com.axians.eaf.userprofile.domain.command.UpdateUserProfileCommand
import com.axians.eaf.userprofile.domain.command.DeactivateUserProfileCommand
import com.axians.eaf.userprofile.domain.model.UserProfile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/user-profiles")
class UserProfileController(
    private val userProfileUseCase: UserProfileUseCase,
) {

    @PostMapping
    fun createUserProfile(@RequestBody request: CreateUserProfileRequest): ResponseEntity<UserProfile> {
        val command = CreateUserProfileCommand(
            name = request.name,
            email = request.email,
            bio = request.bio
        )
        val profile = userProfileUseCase.createUserProfile(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(profile)
    }

    @GetMapping("/{id}")
    fun getUserProfile(@PathVariable id: String): ResponseEntity<UserProfile> {
        return try {
            val profile = userProfileUseCase.findUserProfileById(id)
            ResponseEntity.ok(profile)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping
    fun getAllUserProfiles(): ResponseEntity<List<UserProfile>> {
        val profiles = userProfileUseCase.getAllUserProfiles()
        return ResponseEntity.ok(profiles)
    }

    @PutMapping("/{id}")
    fun updateUserProfile(
        @PathVariable id: String,
        @RequestBody request: UpdateUserProfileRequest
    ): ResponseEntity<UserProfile> {
        return try {
            val command = UpdateUserProfileCommand(
                id = id,
                name = request.name,
                email = request.email,
                bio = request.bio
            )
            val profile = userProfileUseCase.updateUserProfile(command)
            ResponseEntity.ok(profile)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deactivateUserProfile(@PathVariable id: String): ResponseEntity<UserProfile> {
        return try {
            val command = DeactivateUserProfileCommand(id = id)
            val profile = userProfileUseCase.deactivateUserProfile(command)
            ResponseEntity.ok(profile)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "UP", "service" to "user-profile"))
    }
}

data class CreateUserProfileRequest(
    val name: String,
    val email: String,
    val bio: String,
)

data class UpdateUserProfileRequest(
    val name: String,
    val email: String,
    val bio: String,
)
```

## 🧪 Phase 4: Integration Testing

Create a comprehensive integration test:

```kotlin
// src/test/kotlin/com/axians/eaf/userprofile/integration/UserProfileIntegrationTest.kt
package com.axians.eaf.userprofile.integration

import com.axians.eaf.userprofile.infrastructure.adapter.input.web.CreateUserProfileRequest
import com.axians.eaf.userprofile.infrastructure.adapter.input.web.UpdateUserProfileRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureWebMvc
class UserProfileIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should create and retrieve user profile`() {
        // Create user profile
        val createRequest = CreateUserProfileRequest(
            name = "John Doe",
            email = "john.doe@example.com",
            bio = "Software Developer"
        )

        val createResult = mockMvc.perform(
            post("/api/v1/user-profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("John Doe"))
            .andExpect(jsonPath("$.email").value("john.doe@example.com"))
            .andExpect(jsonPath("$.bio").value("Software Developer"))
            .andExpect(jsonPath("$.isActive").value(true))
            .andReturn()

        // Extract ID from response
        val responseContent = createResult.response.contentAsString
        val createdProfile = objectMapper.readTree(responseContent)
        val profileId = createdProfile.get("id").asText()

        // Retrieve user profile
        mockMvc.perform(get("/api/v1/user-profiles/$profileId"))
            .andExpect(status().isOk)
            .andExpected(jsonPath("$.id").value(profileId))
            .andExpected(jsonPath("$.name").value("John Doe"))
    }

    @Test
    fun `should update user profile`() {
        // First create a profile
        val createRequest = CreateUserProfileRequest(
            name = "Jane Doe",
            email = "jane.doe@example.com",
            bio = "Designer"
        )

        val createResult = mockMvc.perform(
            post("/api/v1/user-profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn()

        val profileId = objectMapper.readTree(createResult.response.contentAsString)
            .get("id").asText()

        // Update the profile
        val updateRequest = UpdateUserProfileRequest(
            name = "Jane Smith",
            email = "jane.smith@example.com",
            bio = "Senior Designer"
        )

        mockMvc.perform(
            put("/api/v1/user-profiles/$profileId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpected(status().isOk)
            .andExpected(jsonPath("$.name").value("Jane Smith"))
            .andExpected(jsonPath("$.email").value("jane.smith@example.com"))
            .andExpected(jsonPath("$.bio").value("Senior Designer"))
    }

    @Test
    fun `should return 404 for non-existent profile`() {
        mockMvc.perform(get("/api/v1/user-profiles/non-existent-id"))
            .andExpected(status().isNotFound)
    }
}
```

## 🚀 Phase 5: Test Your Implementation

### Run Tests

```bash
# Run all tests
nx test user-profile

# Run specific test classes
nx test user-profile --tests "*UserProfileTest"
nx test user-profile --tests "*IntegrationTest"
```

### Start the Service

```bash
# Start infrastructure
cd infra/docker-compose && docker compose up -d && cd ../..

# Start your service
nx run user-profile:run
```

### Test with curl

```bash
# Create a user profile
curl -X POST http://localhost:8080/api/v1/user-profiles \
    -H "Content-Type: application/json" \
    -d '{
        "name": "John Doe",
        "email": "john.doe@example.com"
    }'

# Get the profile (replace {id} with actual ID from create response)
curl http://localhost:8080/api/v1/user-profiles/{id}

# Update the profile
curl -X PUT http://localhost:8080/api/v1/user-profiles/{id} \
    -H "Content-Type: application/json" \
    -d '{
        "name": "John Smith",
        "email": "john.smith@example.com"
    }'

# Get all profiles
curl http://localhost:8080/api/v1/user-profiles

# Deactivate profile
curl -X DELETE http://localhost:8080/api/v1/user-profiles/{id}
```

### Verify Event Publishing

Check NATS monitoring UI

```bash
# Open NATS monitoring UI
open http://localhost:8222
```

## 🎯 What You've Accomplished

Congratulations! You've successfully implemented:

### ✅ **Domain-Driven Design**

- Rich domain model with business rules
- Clear command and event definitions
- Proper aggregate boundaries

### ✅ **Hexagonal Architecture**

- Domain isolated from infrastructure
- Input and output ports clearly defined
- Dependency inversion principle applied

### ✅ **Test-Driven Development**

- Comprehensive test coverage
- Tests written before implementation
- Both unit and integration tests

### ✅ **CQRS/ES Patterns**

- Command-based operations
- Event publishing for state changes
- Proper separation of concerns

### ✅ **EAF Integration**

- NATS event publishing
- Spring Boot configuration
- Proper error handling

## 🚀 Next Steps

Now that you have a working backend service with event publishing, let's create a frontend to
interact with it! Continue to [Frontend Integration](./frontend-integration.md) to build a React
component that consumes your API.

---

**Excellent work!** You've built a complete, production-ready service following EAF best practices.
The foundation is solid - now let's add the user interface! 🎉

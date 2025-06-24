# TDD Workflow Implementation Guide for Control Plane

## Overview

This guide implements the **Test-Driven Development (TDD) workflow** as specified in Story 2.9.1,
Task 8. It follows the **domain-first approach** with comprehensive code examples and best practices
for the EAF Control Plane service.

## TDD Cycle Implementation

### Phase 1: Domain-First TDD Approach

The Control Plane follows a **domain-first TDD cycle**:

1. **Red**: Write failing domain test
2. **Green**: Implement minimal domain logic to pass test
3. **Refactor**: Improve design while keeping tests green
4. **Repeat**: Build incrementally with fast feedback

### Example: Tenant Creation Domain Logic

#### Step 1: Write Failing Domain Test

```kotlin
package com.axians.eaf.controlplane.domain.tenant

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TenantServiceTest {

    private val tenantService = TenantService()

    @Test
    fun `should create tenant with unique identifier and initial admin`() {
        // Given
        val command = CreateTenantCommand(
            name = "Acme Corporation",
            initialAdminEmail = "admin@acme.com",
            requestingUserId = "superadmin-123"
        )

        // When
        val result = tenantService.createTenant(command)

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.tenantId).isNotNull()
        assertThat(result.tenantId).startsWith("tenant-")
        assertThat(result.initialAdminUserId).isNotNull()
    }

    @Test
    fun `should reject tenant creation with invalid name`() {
        // Given
        val command = CreateTenantCommand(
            name = "", // Invalid empty name
            initialAdminEmail = "admin@acme.com",
            requestingUserId = "superadmin-123"
        )

        // When & Then
        assertThrows<IllegalArgumentException> {
            tenantService.createTenant(command)
        }
    }

    @Test
    fun `should reject tenant creation with invalid email`() {
        // Given
        val command = CreateTenantCommand(
            name = "Valid Corp",
            initialAdminEmail = "invalid-email", // Invalid email format
            requestingUserId = "superadmin-123"
        )

        // When & Then
        assertThrows<IllegalArgumentException> {
            tenantService.createTenant(command)
        }
    }
}
```

#### Step 2: Implement Minimal Domain Logic

```kotlin
package com.axians.eaf.controlplane.domain.tenant

import java.util.UUID

class TenantService {

    fun createTenant(command: CreateTenantCommand): CreateTenantResult {
        // Validate input
        require(command.name.isNotBlank()) { "Tenant name cannot be blank" }
        require(command.initialAdminEmail.contains("@")) { "Invalid email format" }

        // Generate unique identifiers
        val tenantId = "tenant-${UUID.randomUUID()}"
        val adminUserId = "user-${UUID.randomUUID()}"

        // Return successful result
        return CreateTenantResult.success(tenantId, adminUserId)
    }
}

data class CreateTenantCommand(
    val name: String,
    val initialAdminEmail: String,
    val requestingUserId: String
)

sealed class CreateTenantResult {
    data class Success(val tenantId: String, val initialAdminUserId: String) : CreateTenantResult() {
        val isSuccess = true
    }

    data class Failure(val errorMessage: String) : CreateTenantResult() {
        val isSuccess = false
    }

    companion object {
        fun success(tenantId: String, adminUserId: String) = Success(tenantId, adminUserId)
        fun failure(message: String) = Failure(message)
    }
}
```

#### Step 3: Refactor and Enhance

```kotlin
// Enhanced domain model with proper validation
class TenantService(
    private val tenantRepository: TenantRepository,
    private val emailValidator: EmailValidator,
    private val tenantNameValidator: TenantNameValidator
) {

    fun createTenant(command: CreateTenantCommand): CreateTenantResult {
        // Domain validation
        tenantNameValidator.validate(command.name)
            .onFailure { return CreateTenantResult.failure(it.message) }

        emailValidator.validate(command.initialAdminEmail)
            .onFailure { return CreateTenantResult.failure(it.message) }

        // Check for duplicate tenant name
        if (tenantRepository.existsByName(command.name)) {
            return CreateTenantResult.failure("Tenant name already exists")
        }

        // Create tenant aggregate
        val tenant = Tenant.create(
            name = TenantName(command.name),
            initialAdminEmail = EmailAddress(command.initialAdminEmail),
            createdBy = UserId(command.requestingUserId)
        )

        // Persist tenant
        tenantRepository.save(tenant)

        return CreateTenantResult.success(tenant.id.value, tenant.initialAdminUserId.value)
    }
}
```

### Phase 2: Application Layer TDD

#### Step 1: Write Application Service Test

```kotlin
package com.axians.eaf.controlplane.application.tenant

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TenantApplicationServiceTest {

    private val tenantService = mockk<TenantService>()
    private val eventPublisher = mockk<EventPublisher>()
    private val tenantApplicationService = TenantApplicationService(tenantService, eventPublisher)

    @Test
    fun `should publish tenant created event after successful creation`() {
        // Given
        val command = CreateTenantCommand(
            name = "Test Corp",
            initialAdminEmail = "admin@test.com",
            requestingUserId = "admin-123"
        )

        val result = CreateTenantResult.success("tenant-456", "user-789")
        every { tenantService.createTenant(command) } returns result
        every { eventPublisher.publish(any<TenantCreatedEvent>()) } returns Unit

        // When
        tenantApplicationService.createTenant(command)

        // Then
        verify {
            eventPublisher.publish(
                withArg<TenantCreatedEvent> { event ->
                    assertThat(event.tenantId).isEqualTo("tenant-456")
                    assertThat(event.tenantName).isEqualTo("Test Corp")
                    assertThat(event.adminEmail).isEqualTo("admin@test.com")
                }
            )
        }
    }

    @Test
    fun `should not publish event when tenant creation fails`() {
        // Given
        val command = CreateTenantCommand("", "invalid", "admin-123")
        val result = CreateTenantResult.failure("Invalid tenant name")
        every { tenantService.createTenant(command) } returns result

        // When
        tenantApplicationService.createTenant(command)

        // Then
        verify(exactly = 0) { eventPublisher.publish(any<TenantCreatedEvent>()) }
    }
}
```

#### Step 2: Implement Application Service

```kotlin
package com.axians.eaf.controlplane.application.tenant

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class TenantApplicationService(
    private val tenantService: TenantService,
    private val eventPublisher: EventPublisher
) {

    @Transactional
    fun createTenant(command: CreateTenantCommand): CreateTenantResult {
        val result = tenantService.createTenant(command)

        if (result.isSuccess) {
            val event = TenantCreatedEvent(
                tenantId = result.tenantId,
                tenantName = command.name,
                adminEmail = command.initialAdminEmail,
                createdBy = command.requestingUserId,
                createdAt = Instant.now()
            )
            eventPublisher.publish(event)
        }

        return result
    }
}

data class TenantCreatedEvent(
    val tenantId: String,
    val tenantName: String,
    val adminEmail: String,
    val createdBy: String,
    val createdAt: Instant
)
```

### Phase 3: Infrastructure TDD

#### Step 1: Write Hilla Endpoint Test

```kotlin
package com.axians.eaf.controlplane.infrastructure.adapter.input

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TenantEndpointTest {

    private val tenantApplicationService = mockk<TenantApplicationService>()
    private val securityContextHolder = mockk<EafSecurityContextHolder>()
    private val tenantEndpoint = TenantEndpoint(tenantApplicationService, securityContextHolder)

    @Test
    fun `should accept valid tenant creation request via Hilla endpoint`() {
        // Given
        val request = CreateTenantRequest(
            name = "Enterprise Client",
            adminEmail = "admin@enterprise.com"
        )

        every { securityContextHolder.getUserId() } returns "superadmin-123"
        every { tenantApplicationService.createTenant(any()) } returns
            CreateTenantResult.success("tenant-789", "user-456")

        // When
        val response = tenantEndpoint.createTenant(request)

        // Then
        assertThat(response.success).isTrue()
        assertThat(response.tenantId).isEqualTo("tenant-789")
        assertThat(response.message).isEqualTo("Tenant created successfully")
    }

    @Test
    fun `should handle tenant creation failure gracefully`() {
        // Given
        val request = CreateTenantRequest(name = "", adminEmail = "invalid")
        every { securityContextHolder.getUserId() } returns "superadmin-123"
        every { tenantApplicationService.createTenant(any()) } returns
            CreateTenantResult.failure("Invalid tenant name")

        // When
        val response = tenantEndpoint.createTenant(request)

        // Then
        assertThat(response.success).isFalse()
        assertThat(response.errorMessage).isEqualTo("Invalid tenant name")
    }
}
```

#### Step 2: Implement Hilla Endpoint

```kotlin
package com.axians.eaf.controlplane.infrastructure.adapter.input

import com.vaadin.hilla.BrowserCallable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@BrowserCallable
@Service
class TenantEndpoint(
    private val tenantApplicationService: TenantApplicationService,
    private val securityContextHolder: EafSecurityContextHolder
) {

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun createTenant(request: CreateTenantRequest): CreateTenantResponse {
        val command = CreateTenantCommand(
            name = request.name,
            initialAdminEmail = request.adminEmail,
            requestingUserId = securityContextHolder.getUserId()
        )

        val result = tenantApplicationService.createTenant(command)

        return if (result.isSuccess) {
            CreateTenantResponse.success(result.tenantId, "Tenant created successfully")
        } else {
            CreateTenantResponse.failure(result.errorMessage)
        }
    }
}

data class CreateTenantRequest(
    val name: String,
    val adminEmail: String
)

data class CreateTenantResponse(
    val success: Boolean,
    val tenantId: String? = null,
    val message: String? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun success(tenantId: String, message: String) =
            CreateTenantResponse(true, tenantId, message)

        fun failure(errorMessage: String) =
            CreateTenantResponse(false, errorMessage = errorMessage)
    }
}
```

## TDD Best Practices for Control Plane

### 1. Test Naming Convention

Use descriptive test names that explain the scenario:

```kotlin
@Test
fun `should create tenant with valid data and publish creation event`() { }

@Test
fun `should reject tenant creation when name already exists`() { }

@Test
fun `should handle IAM service failure during tenant creation gracefully`() { }
```

### 2. Arrange-Act-Assert Pattern

Structure all tests with clear Given-When-Then sections:

```kotlin
@Test
fun `should authenticate user and propagate tenant context`() {
    // Given (Arrange)
    val username = "admin@tenant.com"
    val password = "secure123"
    val expectedTenantId = "tenant-123"

    // When (Act)
    val result = authenticationService.authenticate(username, password)

    // Then (Assert)
    assertThat(result.isSuccess).isTrue()
    assertThat(result.tenantId).isEqualTo(expectedTenantId)
}
```

### 3. Mock External Dependencies

Use MockK to isolate units under test:

```kotlin
class TenantServiceTest {
    private val tenantRepository = mockk<TenantRepository>()
    private val emailValidator = mockk<EmailValidator>()
    private val tenantService = TenantService(tenantRepository, emailValidator)

    @Test
    fun `should validate email before creating tenant`() {
        // Given
        every { emailValidator.validate(any()) } returns ValidationResult.success()
        every { tenantRepository.save(any()) } returns Unit

        // When & Then...
    }
}
```

### 4. Test One Behavior Per Test

Each test should verify a single piece of functionality:

```kotlin
// Good - tests one behavior
@Test
fun `should generate unique tenant ID`() { }

@Test
fun `should validate tenant name format`() { }

@Test
fun `should send welcome email to admin`() { }

// Bad - tests multiple behaviors
@Test
fun `should create tenant and send email and validate everything`() { }
```

### 5. Integration Test Patterns

Use the 3-layer testing framework:

```kotlin
// Layer 1: Unit tests with mocks
class TenantServiceUnitTest { }

// Layer 2: Integration tests with Testcontainers
@SpringBootTest
@Testcontainers
class TenantServiceIntegrationTest { }

// Layer 3: End-to-end tests
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TenantManagementE2ETest { }
```

## TDD Workflow Checklist

### Before Writing Code

- [ ] Write failing test that describes desired behavior
- [ ] Ensure test fails for the right reason
- [ ] Write minimal code to make test pass
- [ ] Refactor while keeping tests green

### Domain Layer TDD

- [ ] Start with pure domain logic (no frameworks)
- [ ] Test business rules and invariants
- [ ] Use value objects for strong typing
- [ ] Test domain event generation

### Application Layer TDD

- [ ] Test use case orchestration
- [ ] Mock infrastructure dependencies
- [ ] Test transaction boundaries
- [ ] Test event publishing

### Infrastructure Layer TDD

- [ ] Test external service integration
- [ ] Test data persistence
- [ ] Test web endpoints
- [ ] Test error handling

### Quality Gates

- [ ] All tests pass
- [ ] Code coverage >90% for new code
- [ ] No architectural violations (ArchUnit)
- [ ] Integration tests cover all 7 categories
- [ ] Performance tests pass

## Example Test Execution Flow

```bash
# Run unit tests only
./gradlew test --tests "*.unit.*"

# Run integration tests
./gradlew test --tests "*IntegrationTest"

# Run all tests with coverage
./gradlew test jacocoTestReport

# Run architectural compliance tests
./gradlew test --tests "*ArchitectureComplianceTest"

# Run specific test category
./gradlew test --tests "*SecurityIntegrationTest"
```

## Continuous TDD Workflow

1. **Red**: Write failing test
2. **Green**: Make test pass with minimal code
3. **Refactor**: Improve design
4. **Commit**: Save progress
5. **Repeat**: Next requirement

This TDD workflow ensures high-quality, well-tested code that meets all Control Plane requirements
while maintaining architectural integrity and following EAF best practices.

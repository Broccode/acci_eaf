# Test-Driven Development (TDD) in ACCI EAF

Test-Driven Development is **mandatory** in ACCI EAF. It ensures high-quality, well-designed code through the red-green-refactor cycle.

## The TDD Cycle

### 🔴 Red: Write a Failing Test

Start by writing the smallest possible test that fails:

```kotlin
class TenantServiceTest {
    @Test
    fun `should create tenant with valid name and admin email`() {
        // Given
        val command = CreateTenantCommand("Acme Corp", "admin@acme.com")
        val tenantService = TenantService()
        
        // When & Then
        assertThrows<NotImplementedError> {
            tenantService.createTenant(command)
        }
    }
}
```

### 🟢 Green: Make It Pass

Write just enough code to make the test pass:

```kotlin
class TenantService {
    fun createTenant(command: CreateTenantCommand): TenantCreatedEvent {
        return TenantCreatedEvent(
            tenantId = TenantId.generate(),
            tenantName = command.tenantName,
            adminEmail = command.adminEmail,
            timestamp = Instant.now()
        )
    }
}
```

### 🔵 Refactor: Improve the Design

Clean up the code while keeping tests green:

```kotlin
class TenantService(
    private val tenantRepository: TenantRepository,
    private val eventPublisher: EventPublisher
) {
    fun createTenant(command: CreateTenantCommand): TenantCreatedEvent {
        validateCommand(command)
        
        val tenant = Tenant.create(command.tenantName, command.adminEmail)
        tenantRepository.save(tenant)
        
        val event = tenant.getUncommittedEvents().first()
        eventPublisher.publish(event)
        
        return event as TenantCreatedEvent
    }
    
    private fun validateCommand(command: CreateTenantCommand) {
        require(command.tenantName.isNotBlank()) { "Tenant name cannot be blank" }
        require(command.adminEmail.contains("@")) { "Invalid email format" }
    }
}
```

## EAF Testing Stack

### Backend (Kotlin/Spring)

- **JUnit 5**: Test framework
- **MockK**: Mocking library for Kotlin
- **AssertJ**: Fluent assertions
- **Spring Boot Test**: Integration testing
- **Testcontainers**: Database and NATS testing
- **ArchUnit**: Architecture rule testing

### Frontend (React/TypeScript)

- **Vitest**: Fast test runner
- **React Testing Library**: Component testing
- **Mock Service Worker**: API mocking

## Testing Patterns

### Unit Tests

Test individual classes in isolation:

```kotlin
class TenantTest {
    @Test
    fun `should emit TenantCreatedEvent when created`() {
        // Given
        val tenantName = "Acme Corp"
        val adminEmail = "admin@acme.com"
        
        // When
        val tenant = Tenant.create(tenantName, adminEmail)
        
        // Then
        val events = tenant.getUncommittedEvents()
        assertThat(events).hasSize(1)
        assertThat(events.first()).isInstanceOf<TenantCreatedEvent>()
    }
}
```

### Integration Tests

Test component interactions:

```kotlin
@SpringBootTest
@Testcontainers
class TenantServiceIntegrationTest {
    @Container
    static val postgres = PostgreSQLContainer("postgres:15")
    
    @Autowired
    lateinit var tenantService: TenantService
    
    @Test
    fun `should persist tenant and publish event`() {
        // Test with real database and event bus
    }
}
```

### Architecture Tests

Enforce architectural rules:

```kotlin
@Test
fun `domain layer should not depend on infrastructure`() {
    ArchRuleDefinition.noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
        .check(importedClasses)
}
```

## Best Practices

### Test Naming

Use descriptive names that explain the scenario:

```kotlin
// Good
fun `should throw exception when tenant name is blank`()

// Bad
fun testTenantCreation()
```

### Test Structure

Follow Given-When-Then pattern:

```kotlin
@Test
fun `should calculate correct total when applying discount`() {
    // Given
    val order = Order(items = listOf(item1, item2))
    val discount = Discount.percentage(10)
    
    // When
    val total = order.calculateTotal(discount)
    
    // Then
    assertThat(total).isEqualTo(Money.of(90.0))
}
```

### Mock Usage

Mock external dependencies, not the system under test:

```kotlin
@Test
fun `should send email when tenant is created`() {
    // Given
    val emailService = mockk<EmailService>()
    val tenantService = TenantService(tenantRepository, emailService)
    
    // When
    tenantService.createTenant(command)
    
    // Then
    verify { emailService.sendWelcomeEmail(any()) }
}
```

## CI/CD Integration

All tests run automatically in the CI pipeline:

```bash
# Backend tests
nx run-many -t test

# Frontend tests
npm test

# Architecture tests are included in the test suite
```

## TDD Benefits in ACCI EAF

1. **Design Feedback**: Tests guide better API design
2. **Documentation**: Tests serve as living documentation
3. **Confidence**: Refactoring is safe with comprehensive tests
4. **Quality**: Bugs are caught early in development
5. **Architecture**: Forces thinking about dependencies and interfaces

*This is a placeholder document. Detailed TDD practices and EAF-specific testing patterns will be documented as the framework evolves.*

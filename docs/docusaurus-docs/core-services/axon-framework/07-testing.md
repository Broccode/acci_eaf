---
sidebar_position: 8
title: Testing Strategies
---

# Testing Strategies

This module covers comprehensive testing approaches for Axon Framework components within the EAF
architecture, from unit tests to full integration scenarios.

## 📚 Learning Objectives

By the end of this module, you will be able to:

- Write effective unit tests for aggregates using Axon Test
- Create integration tests with EAF infrastructure
- Design acceptance tests for complete workflows
- Test multi-tenant scenarios and security
- Mock external dependencies properly
- Measure and optimize test performance

## 🧪 Testing Pyramid for Event-Sourced Systems

```mermaid
pyramid
    title Testing Pyramid for Axon/EAF

    level 1 "Unit Tests (70%)"
        description "Aggregate logic, Command handlers, Event handlers"

    level 2 "Integration Tests (20%)"
        description "Event store, Projections, NATS, Database"

    level 3 "Acceptance Tests (10%)"
        description "End-to-end business scenarios"
```

### Testing Focus Areas

1. **Aggregate Logic**: Business rules and invariants
2. **Event Processing**: Handlers and projections
3. **Infrastructure**: Event store, NATS, multi-tenancy
4. **Security**: Authorization and tenant isolation
5. **Performance**: Load handling and scalability

## 🎯 Unit Testing with Axon Test

### Aggregate Testing Framework

Axon provides excellent testing support through `AggregateTestFixture`:

```kotlin
// Test class setup
class UserAggregateTest {
    private lateinit var fixture: AggregateTestFixture<User>

    @BeforeEach
    fun setUp() {
        fixture = AggregateTestFixture(User::class.java)
    }
}
```

### Testing Aggregate Creation

```kotlin
@Test
fun `should create user when valid command is given`() {
    val command = CreateUserCommand(
        userId = "user-123",
        tenantId = "tenant-abc",
        email = "john.doe@example.com",
        firstName = "John",
        lastName = "Doe",
        roles = setOf("USER"),
        department = "Engineering"
    )

    fixture.givenNoPriorActivity()
        .`when`(command)
        .expectEvents(
            UserCreatedEvent(
                userId = "user-123",
                tenantId = "tenant-abc",
                email = "john.doe@example.com",
                firstName = "John",
                lastName = "Doe",
                roles = setOf("USER"),
                department = "Engineering"
            )
        )
}

@Test
fun `should reject user creation with invalid email`() {
    val command = CreateUserCommand(
        userId = "user-123",
        tenantId = "tenant-abc",
        email = "invalid-email", // Invalid format
        firstName = "John",
        lastName = "Doe",
        roles = setOf("USER"),
        department = "Engineering"
    )

    fixture.givenNoPriorActivity()
        .`when`(command)
        .expectException(IllegalArgumentException::class.java)
        .expectExceptionMessage("Invalid email format")
}
```

### Testing State Transitions

```kotlin
@Test
fun `should update user email when user is active`() {
    val createEvent = UserCreatedEvent(
        userId = "user-123",
        tenantId = "tenant-abc",
        email = "john.doe@example.com",
        firstName = "John",
        lastName = "Doe",
        roles = setOf("USER"),
        department = "Engineering"
    )

    val updateCommand = ChangeUserEmailCommand(
        userId = "user-123",
        newEmail = "john.new@example.com",
        reason = "Personal preference"
    )

    fixture.given(createEvent)
        .`when`(updateCommand)
        .expectEvents(
            UserEmailChangedEvent(
                userId = "user-123",
                previousEmail = "john.doe@example.com",
                newEmail = "john.new@example.com",
                reason = "Personal preference"
            )
        )
}

@Test
fun `should reject email change when user is suspended`() {
    val createEvent = UserCreatedEvent(
        userId = "user-123",
        tenantId = "tenant-abc",
        email = "john.doe@example.com",
        firstName = "John",
        lastName = "Doe",
        roles = setOf("USER"),
        department = "Engineering"
    )

    val suspendEvent = UserSuspendedEvent(
        userId = "user-123",
        reason = "Policy violation",
        suspendedBy = "admin-456"
    )

    val updateCommand = ChangeUserEmailCommand(
        userId = "user-123",
        newEmail = "john.new@example.com",
        reason = "Personal preference"
    )

    fixture.given(createEvent, suspendEvent)
        .`when`(updateCommand)
        .expectException(IllegalStateException::class.java)
        .expectExceptionMessage("Cannot change email of inactive user")
}
```

### Testing Business Rules

```kotlin
@Test
fun `should enforce minimum role requirement`() {
    val createEvent = UserCreatedEvent(
        userId = "user-123",
        tenantId = "tenant-abc",
        email = "john.doe@example.com",
        firstName = "John",
        lastName = "Doe",
        roles = setOf("USER"), // Only one role
        department = "Engineering"
    )

    val removeRoleCommand = RemoveUserRoleCommand(
        userId = "user-123",
        role = "USER", // Trying to remove the only role
        removedBy = "admin-456",
        reason = "Role cleanup"
    )

    fixture.given(createEvent)
        .`when`(removeRoleCommand)
        .expectException(IllegalStateException::class.java)
        .expectExceptionMessage("Cannot remove last role from user")
}
```

### Parameterized Testing

```kotlin
@ParameterizedTest
@ValueSource(strings = ["", " ", "invalid-email", "@example.com", "user@"])
fun `should reject invalid email formats`(invalidEmail: String) {
    val command = CreateUserCommand(
        userId = "user-123",
        tenantId = "tenant-abc",
        email = invalidEmail,
        firstName = "John",
        lastName = "Doe",
        roles = setOf("USER"),
        department = "Engineering"
    )

    fixture.givenNoPriorActivity()
        .`when`(command)
        .expectException(IllegalArgumentException::class.java)
}
```

## 🏗️ Integration Testing with EAF Infrastructure

### Test Configuration

```kotlin
// Test application configuration
@TestConfiguration
class AxonTestConfiguration {

    @Bean
    @Primary
    fun testEventStorageEngine(): EventStorageEngine {
        return InMemoryEventStorageEngine()
    }

    @Bean
    @Primary
    fun testTokenStore(): TokenStore {
        return InMemoryTokenStore()
    }

    @Bean
    @Primary
    fun testEventBus(): EventBus {
        return SimpleEventBus.builder().build()
    }
}
```

### Event Store Integration Tests

```kotlin
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(AxonTestConfiguration::class)
class UserEventStoreIntegrationTest {

    @Autowired
    private lateinit var commandGateway: CommandGateway

    @Autowired
    private lateinit var eventStore: EventStore

    @Container
    companion object {
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test")
    }

    @Test
    fun `should persist and retrieve user events`() {
        val userId = "user-${UUID.randomUUID()}"
        val tenantId = "tenant-test"

        // Send command
        val command = CreateUserCommand(
            userId = userId,
            tenantId = tenantId,
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            roles = setOf("USER"),
            department = "Testing"
        )

        commandGateway.sendAndWait<Void>(command)

        // Verify events were stored
        val events = eventStore.readEvents(userId).asSequence().toList()

        assertThat(events).hasSize(1)
        assertThat(events[0].payload).isInstanceOf(UserCreatedEvent::class.java)

        val event = events[0].payload as UserCreatedEvent
        assertThat(event.userId).isEqualTo(userId)
        assertThat(event.tenantId).isEqualTo(tenantId)
        assertThat(event.email).isEqualTo("test@example.com")
    }
}
```

### Projection Integration Tests

```kotlin
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class UserProjectionIntegrationTest {

    @Autowired
    private lateinit var commandGateway: CommandGateway

    @Autowired
    private lateinit var userProjectionRepository: UserProjectionRepository

    @Autowired
    private lateinit var eventProcessor: TrackingEventProcessor

    @Test
    fun `should create projection when user is created`() {
        val userId = "user-${UUID.randomUUID()}"
        val tenantId = "tenant-test"

        // Send command
        val command = CreateUserCommand(
            userId = userId,
            tenantId = tenantId,
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            roles = setOf("USER"),
            department = "Testing"
        )

        commandGateway.sendAndWait<Void>(command)

        // Wait for projection to be updated
        await().atMost(Duration.ofSeconds(5))
            .until { userProjectionRepository.findById(userId).isPresent }

        // Verify projection
        val projection = userProjectionRepository.findById(userId).get()
        assertThat(projection.userId).isEqualTo(userId)
        assertThat(projection.tenantId).isEqualTo(tenantId)
        assertThat(projection.email).isEqualTo("test@example.com")
        assertThat(projection.status).isEqualTo(UserStatus.ACTIVE)
    }

    @Test
    fun `should update projection when user email changes`() {
        val userId = "user-${UUID.randomUUID()}"
        val tenantId = "tenant-test"

        // Create user
        val createCommand = CreateUserCommand(
            userId = userId,
            tenantId = tenantId,
            email = "old@example.com",
            firstName = "Test",
            lastName = "User",
            roles = setOf("USER"),
            department = "Testing"
        )

        commandGateway.sendAndWait<Void>(createCommand)

        // Wait for initial projection
        await().atMost(Duration.ofSeconds(5))
            .until { userProjectionRepository.findById(userId).isPresent }

        // Update email
        val updateCommand = ChangeUserEmailCommand(
            userId = userId,
            newEmail = "new@example.com",
            reason = "Testing"
        )

        commandGateway.sendAndWait<Void>(updateCommand)

        // Wait for projection update
        await().atMost(Duration.ofSeconds(5))
            .until {
                userProjectionRepository.findById(userId)
                    .map { it.email }
                    .orElse("") == "new@example.com"
            }

        // Verify updated projection
        val projection = userProjectionRepository.findById(userId).get()
        assertThat(projection.email).isEqualTo("new@example.com")
    }
}
```

### Multi-Tenant Testing

```kotlin
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class MultiTenantIntegrationTest {

    @Autowired
    private lateinit var commandGateway: CommandGateway

    @Autowired
    private lateinit var userProjectionRepository: UserProjectionRepository

    @Autowired
    private lateinit var tenantContextHolder: TenantContextHolder

    @Test
    fun `should isolate users by tenant`() {
        val userId = "user-123"
        val tenant1 = "tenant-1"
        val tenant2 = "tenant-2"

        // Create user in tenant 1
        tenantContextHolder.setCurrentTenantId(tenant1)
        val command1 = CreateUserCommand(
            userId = userId,
            tenantId = tenant1,
            email = "user@tenant1.com",
            firstName = "Tenant1",
            lastName = "User",
            roles = setOf("USER"),
            department = "Testing"
        )
        commandGateway.sendAndWait<Void>(command1)

        // Create user with same ID in tenant 2
        tenantContextHolder.setCurrentTenantId(tenant2)
        val command2 = CreateUserCommand(
            userId = userId, // Same user ID, different tenant
            tenantId = tenant2,
            email = "user@tenant2.com",
            firstName = "Tenant2",
            lastName = "User",
            roles = setOf("USER"),
            department = "Testing"
        )
        commandGateway.sendAndWait<Void>(command2)

        // Wait for projections
        await().atMost(Duration.ofSeconds(5))
            .until {
                userProjectionRepository.findByTenantId(tenant1).size == 1 &&
                userProjectionRepository.findByTenantId(tenant2).size == 1
            }

        // Verify tenant isolation
        val tenant1Users = userProjectionRepository.findByTenantId(tenant1)
        val tenant2Users = userProjectionRepository.findByTenantId(tenant2)

        assertThat(tenant1Users).hasSize(1)
        assertThat(tenant2Users).hasSize(1)
        assertThat(tenant1Users[0].email).isEqualTo("user@tenant1.com")
        assertThat(tenant2Users[0].email).isEqualTo("user@tenant2.com")
    }
}
```

## 🔒 Security Testing

### Authorization Testing

```kotlin
@SpringBootTest
@TestMethodOrder(OrderAnnotation::class)
class UserSecurityIntegrationTest {

    @Autowired
    private lateinit var commandGateway: CommandGateway

    @Test
    @WithMockUser(roles = ["USER_MANAGEMENT"])
    fun `should allow user creation with proper role`() {
        val command = CreateUserCommand(
            userId = "user-123",
            tenantId = "tenant-abc",
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            roles = setOf("USER"),
            department = "Testing"
        )

        assertDoesNotThrow {
            commandGateway.sendAndWait<Void>(command)
        }
    }

    @Test
    @WithMockUser(roles = ["READ_ONLY"])
    fun `should reject user creation without proper role`() {
        val command = CreateUserCommand(
            userId = "user-124",
            tenantId = "tenant-abc",
            email = "test2@example.com",
            firstName = "Test2",
            lastName = "User",
            roles = setOf("USER"),
            department = "Testing"
        )

        assertThrows<AccessDeniedException> {
            commandGateway.sendAndWait<Void>(command)
        }
    }
}
```

### Tenant Isolation Testing

```kotlin
@Test
fun `should prevent cross-tenant access`() {
    val userId = "user-123"
    val authorizedTenant = "tenant-authorized"
    val unauthorizedTenant = "tenant-unauthorized"

    // Create user in authorized tenant
    tenantContextHolder.setCurrentTenantId(authorizedTenant)
    val createCommand = CreateUserCommand(
        userId = userId,
        tenantId = authorizedTenant,
        email = "test@example.com",
        firstName = "Test",
        lastName = "User",
        roles = setOf("USER"),
        department = "Testing"
    )
    commandGateway.sendAndWait<Void>(createCommand)

    // Try to access from unauthorized tenant
    tenantContextHolder.setCurrentTenantId(unauthorizedTenant)
    val updateCommand = ChangeUserEmailCommand(
        userId = userId,
        newEmail = "hacker@example.com",
        reason = "Malicious attempt"
    )

    assertThrows<IllegalStateException> {
        commandGateway.sendAndWait<Void>(updateCommand)
    }
}
```

## 📊 Performance Testing

### Load Testing Setup

```kotlin
@SpringBootTest
@Testcontainers
@ActiveProfiles("performance-test")
class UserPerformanceTest {

    @Autowired
    private lateinit var commandGateway: CommandGateway

    @Test
    fun `should handle concurrent user creation`() {
        val numUsers = 100
        val tenantId = "tenant-load-test"
        val latch = CountDownLatch(numUsers)
        val errors = CopyOnWriteArrayList<Exception>()

        val startTime = System.currentTimeMillis()

        // Create users concurrently
        repeat(numUsers) { index ->
            CompletableFuture.runAsync {
                try {
                    val command = CreateUserCommand(
                        userId = "user-$index",
                        tenantId = tenantId,
                        email = "user$index@example.com",
                        firstName = "User",
                        lastName = "$index",
                        roles = setOf("USER"),
                        department = "Load Testing"
                    )
                    commandGateway.sendAndWait<Void>(command)
                } catch (e: Exception) {
                    errors.add(e)
                } finally {
                    latch.countDown()
                }
            }
        }

        // Wait for completion
        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue()

        val duration = System.currentTimeMillis() - startTime

        // Verify results
        assertThat(errors).isEmpty()
        assertThat(duration).isLessThan(10000) // Less than 10 seconds

        println("Created $numUsers users in ${duration}ms")
        println("Throughput: ${numUsers * 1000.0 / duration} users/second")
    }
}
```

### Memory and Resource Testing

```kotlin
@Test
fun `should not leak memory during event processing`() {
    val runtime = Runtime.getRuntime()
    val initialMemory = runtime.totalMemory() - runtime.freeMemory()

    // Process many events
    repeat(1000) { index ->
        val command = CreateUserCommand(
            userId = "user-$index",
            tenantId = "tenant-memory-test",
            email = "user$index@example.com",
            firstName = "User",
            lastName = "$index",
            roles = setOf("USER"),
            department = "Memory Testing"
        )
        commandGateway.sendAndWait<Void>(command)
    }

    // Force garbage collection
    System.gc()
    Thread.sleep(1000)

    val finalMemory = runtime.totalMemory() - runtime.freeMemory()
    val memoryIncrease = finalMemory - initialMemory

    // Memory increase should be reasonable (less than 100MB)
    assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024)

    println("Memory increase: ${memoryIncrease / (1024 * 1024)}MB")
}
```

## 🎭 Acceptance Testing

### End-to-End Workflow Testing

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("acceptance-test")
class UserWorkflowAcceptanceTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `complete user lifecycle workflow`() {
        val userId = "user-${UUID.randomUUID()}"
        val tenantId = "tenant-acceptance"

        // Step 1: Create user
        val createRequest = CreateUserRequest(
            userId = userId,
            email = "workflow@example.com",
            firstName = "Workflow",
            lastName = "User",
            roles = setOf("USER"),
            department = "Acceptance Testing"
        )

        val createResponse = restTemplate.exchange(
            "/api/users",
            HttpMethod.POST,
            HttpEntity(createRequest, createHeaders(tenantId)),
            String::class.java
        )

        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        // Step 2: Verify user exists
        val getResponse = restTemplate.exchange(
            "/api/users/$userId",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            UserResponse::class.java
        )

        assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(getResponse.body?.email).isEqualTo("workflow@example.com")

        // Step 3: Update user profile
        val updateRequest = UpdateUserProfileRequest(
            firstName = "Updated",
            department = "New Department"
        )

        val updateResponse = restTemplate.exchange(
            "/api/users/$userId/profile",
            HttpMethod.PUT,
            HttpEntity(updateRequest, createHeaders(tenantId)),
            String::class.java
        )

        assertThat(updateResponse.statusCode).isEqualTo(HttpStatus.OK)

        // Step 4: Assign additional role
        val roleRequest = AssignRoleRequest(role = "MANAGER")

        val roleResponse = restTemplate.exchange(
            "/api/users/$userId/roles",
            HttpMethod.POST,
            HttpEntity(roleRequest, createHeaders(tenantId)),
            String::class.java
        )

        assertThat(roleResponse.statusCode).isEqualTo(HttpStatus.OK)

        // Step 5: Verify final state
        val finalGetResponse = restTemplate.exchange(
            "/api/users/$userId",
            HttpMethod.GET,
            HttpEntity(null, createHeaders(tenantId)),
            UserResponse::class.java
        )

        val finalUser = finalGetResponse.body!!
        assertThat(finalUser.firstName).isEqualTo("Updated")
        assertThat(finalUser.department).isEqualTo("New Department")
        assertThat(finalUser.roles).contains("USER", "MANAGER")
    }

    private fun createHeaders(tenantId: String): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("X-Tenant-ID", tenantId)
        return headers
    }
}
```

## 🎯 Testing Best Practices

### 1. Test Data Management

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserTestDataManager {

    companion object {
        fun createValidUser(
            userId: String = "user-${UUID.randomUUID()}",
            tenantId: String = "tenant-test",
            email: String = "test@example.com"
        ) = CreateUserCommand(
            userId = userId,
            tenantId = tenantId,
            email = email,
            firstName = "Test",
            lastName = "User",
            roles = setOf("USER"),
            department = "Testing"
        )

        fun createUserCreatedEvent(
            userId: String = "user-${UUID.randomUUID()}",
            tenantId: String = "tenant-test"
        ) = UserCreatedEvent(
            userId = userId,
            tenantId = tenantId,
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            roles = setOf("USER"),
            department = "Testing"
        )
    }
}
```

### 2. Custom Matchers

```kotlin
// Custom assertion for events
fun assertThatEvent(event: Any): EventAssert {
    return EventAssert(event)
}

class EventAssert(private val event: Any) {
    fun hasEventType(expectedType: Class<*>): EventAssert {
        assertThat(event).isInstanceOf(expectedType)
        return this
    }

    fun hasUserId(expectedUserId: String): EventAssert {
        when (event) {
            is UserCreatedEvent -> assertThat(event.userId).isEqualTo(expectedUserId)
            is UserEmailChangedEvent -> assertThat(event.userId).isEqualTo(expectedUserId)
            else -> fail("Event type does not have userId field")
        }
        return this
    }
}
```

### 3. Test Utilities

```kotlin
object TestFixtures {

    fun waitForProjectionUpdate(
        repository: UserProjectionRepository,
        userId: String,
        condition: (UserProjection) -> Boolean,
        timeout: Duration = Duration.ofSeconds(5)
    ) {
        await().atMost(timeout)
            .until {
                repository.findById(userId)
                    .map(condition)
                    .orElse(false)
            }
    }

    fun withTenantContext(tenantId: String, action: () -> Unit) {
        val holder = ApplicationContextProvider.getBean(TenantContextHolder::class.java)
        try {
            holder.setCurrentTenantId(tenantId)
            action()
        } finally {
            holder.clearContext()
        }
    }
}
```

## 📈 Test Metrics and Reporting

### Coverage Configuration

```kotlin
// build.gradle.kts
tasks.jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }

    executionData.setFrom(
        fileTree(layout.buildDirectory.dir("jacoco")).include("**/*.exec")
    )
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal() // 80% coverage minimum
            }
        }

        rule {
            element = "CLASS"
            excludes = listOf("*.configuration.*", "*.dto.*")

            limit {
                counter = "BRANCH"
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}
```

## 🎯 Testing Checklist

Before deploying your Axon components, ensure:

- [ ] ✅ All aggregate business rules have unit tests
- [ ] ✅ Command validation scenarios are covered
- [ ] ✅ Event sourcing handlers work correctly
- [ ] ✅ Projections update properly with events
- [ ] ✅ Multi-tenant isolation is enforced
- [ ] ✅ Security authorization works as expected
- [ ] ✅ Error handling behaves correctly
- [ ] ✅ Performance meets requirements
- [ ] ✅ Integration with EAF infrastructure works
- [ ] ✅ End-to-end workflows complete successfully

## 🚀 Next Steps

You've mastered testing strategies for Axon Framework. Next, let's explore advanced operational
topics:

**Next Module:** [Performance & Operations](./08-performance-operations.md) →

**Topics covered next:**

- Monitoring and observability
- Performance tuning and optimization
- Deployment strategies
- Troubleshooting common issues

---

💡 **Remember:** Good tests are the foundation of reliable event-sourced systems. Invest in
comprehensive testing to ensure confidence in your implementations!

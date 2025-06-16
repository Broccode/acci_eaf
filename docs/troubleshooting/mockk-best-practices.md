# MockK Best Practices for EAF Project

## Overview

This project uses MockK as the primary mocking framework for Kotlin-based testing. MockK provides
better Kotlin integration, coroutine support, and more idiomatic syntax compared to Mockito.

## Core Patterns

### 1. Basic Mocking

```kotlin
// Create mock
val mockService = mockk<UserService>()

// Relaxed mock (returns default values for unstubbed calls)
val relaxedMock = mockk<UserService>(relaxed = true)
```

### 2. Stubbing

```kotlin
// Basic stubbing
every { mockService.findUser(any()) } returns user

// Coroutine stubbing
coEvery { mockService.findUserAsync(any()) } returns user

// Multiple return values
every { mockService.getUsers() } returnsMany listOf(users1, users2)

// Exception throwing
every { mockService.deleteUser(any()) } throws IllegalArgumentException("User not found")
```

### 3. Verification

```kotlin
// Basic verification
verify { mockService.saveUser(any()) }

// Exact count verification
verify(exactly = 1) { mockService.saveUser(any()) }
verify(exactly = 0) { mockService.deleteUser(any()) }

// Coroutine verification
coVerify { mockService.saveUserAsync(any()) }

// Order verification
verifyOrder {
    mockService.findUser(any())
    mockService.updateUser(any())
}
```

### 4. Argument Matching

```kotlin
// Any matcher
verify { mockService.saveUser(any()) }

// Specific value
verify { mockService.saveUser(user) }

// Custom matcher
verify { mockService.saveUser(match { it.email.contains("@") }) }

// Capture arguments
val slot = slot<User>()
verify { mockService.saveUser(capture(slot)) }
assertEquals("test@example.com", slot.captured.email)
```

## Spring Integration

### 1. Spring Boot Test Annotations

```kotlin
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class UserServiceIntegrationTest {

    @MockkBean
    lateinit var userRepository: UserRepository

    @SpykBean
    lateinit var emailService: EmailService

    @Autowired
    lateinit var userService: UserService
}
```

### 2. Configuration Classes

```kotlin
@TestConfiguration
class TestConfig {
    @Bean
    @Primary
    fun mockUserRepository(): UserRepository = mockk()
}
```

## Advanced Patterns

### 1. Static Mocking

```kotlin
@Test
fun testStaticMethod() {
    mockkStatic(SecurityContextHolder::class)
    every { SecurityContextHolder.getContext() } returns mockSecurityContext

    // Test code here

    unmockkStatic(SecurityContextHolder::class)
}
```

### 2. Object Mocking

```kotlin
@Test
fun testSingleton() {
    mockkObject(MySingleton)
    every { MySingleton.doSomething() } returns "mocked"

    // Test code here

    unmockkObject(MySingleton)
}
```

### 3. Constructor Mocking

```kotlin
mockkConstructor(User::class)
every { anyConstructed<User>().getName() } returns "Mock User"
```

## Coroutine Testing

### 1. Suspend Function Mocking

```kotlin
@Test
fun testCoroutines() = runTest {
    val mockRepo = mockk<UserRepository>()

    coEvery { mockRepo.findByIdAsync(any()) } returns user

    val result = userService.getUserAsync(1L)

    coVerify { mockRepo.findByIdAsync(1L) }
    assertEquals(user, result)
}
```

### 2. Flow Mocking

```kotlin
coEvery { mockRepo.getAllUsersFlow() } returns flowOf(user1, user2)
```

## Common Pitfalls & Solutions

### 1. Strict vs Relaxed Mocks

```kotlin
// Problem: "no answer found" exceptions
val strictMock = mockk<UserService>() // Throws on unstubbed calls

// Solution: Use relaxed mocks for complex objects
val relaxedMock = mockk<UserService>(relaxed = true)

// Or stub all required methods
every { strictMock.findUser(any()) } returns null
every { strictMock.getAllUsers() } returns emptyList()
```

### 2. Final Classes

```kotlin
// MockK handles final classes automatically (unlike Mockito)
val mockFinalClass = mockk<FinalUserService>() // Works!
```

### 3. Verification Order

```kotlin
// Use verifySequence for strict ordering
verifySequence {
    mockService.step1()
    mockService.step2()
    mockService.step3()
}
```

## Migration from Mockito

### Common Conversions

| Mockito                                 | MockK                                   |
| --------------------------------------- | --------------------------------------- |
| `@Mock`                                 | `mockk<Type>()`                         |
| `@MockBean`                             | `@MockkBean`                            |
| `@SpyBean`                              | `@SpykBean`                             |
| `when(mock.method()).thenReturn(value)` | `every { mock.method() } returns value` |
| `verify(mock).method()`                 | `verify { mock.method() }`              |
| `verify(mock, times(2)).method()`       | `verify(exactly = 2) { mock.method() }` |
| `ArgumentCaptor`                        | `slot<Type>()` or `match { }`           |

## Testing Guidelines

### 1. Test Structure

```kotlin
@Test
fun `should create user when valid data provided`() {
    // Given
    val userData = CreateUserRequest("test@example.com", "Test User")
    every { userRepository.save(any()) } returns savedUser

    // When
    val result = userService.createUser(userData)

    // Then
    verify { userRepository.save(match { it.email == "test@example.com" }) }
    assertEquals(savedUser.id, result.id)
}
```

### 2. Setup and Cleanup

```kotlin
@BeforeEach
fun setup() {
    clearAllMocks() // Reset all mocks between tests
}

@AfterEach
fun cleanup() {
    unmockkAll() // Clean up static/object mocks
}
```

## Performance Tips

1. **Reuse mocks** when possible within test classes
2. **Use `@MockkBean`** for Spring integration tests instead of manual mocking
3. **Prefer `relaxed = true`** for complex dependency graphs
4. **Clear mocks** between tests to avoid state leakage

## IDE Integration

### IntelliJ IDEA Live Templates

Create live templates for common MockK patterns:

- `mockk` → `mockk<$TYPE$>()`
- `every` → `every { $MOCK$.$METHOD$($PARAMS$) } returns $RESULT$`
- `verify` → `verify { $MOCK$.$METHOD$($PARAMS$) }`
- `coevery` → `coEvery { $MOCK$.$METHOD$($PARAMS$) } returns $RESULT$`

## Resources

- [MockK Documentation](https://mockk.io/)
- [Spring MockK Integration](https://github.com/Ninja-Squad/springmockk)
- [Kotlin Coroutines Testing](https://kotlinlang.org/docs/coroutines-testing.html)

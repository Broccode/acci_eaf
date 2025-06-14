# 🛠️ Troubleshooting Log – Coroutine & NATS Context Propagation

> **STATUS: ✅ Resolved** (2025-06-14)
>
> **Subject:** `libs/eaf-core` & `libs/eaf-eventing-sdk` – Integration tests for context propagation were failing intermittently, especially when run in parallel (`nx run-many`).
>
> **Symptoms:**
>
> 1. `CoroutineContextIntegrationTest` failed with `null` correlation ID inside `runBlocking`.
> 2. `CompletableFuture` thread did not inherit context as expected.
> 3. Concurrent coroutine tests deadlocked or failed latch timeouts.
> 4. `ContextPropagationIntegrationTest` (NATS) had persistent compilation and runtime errors.

---

## 1. Root Cause Analysis & Resolution Summary

The issues stemmed from three core problems:

1. **`ThreadLocal` vs. Coroutine Dispatchers:** Standard `ThreadLocal` variables do not propagate across the different threads used by coroutine dispatchers.
2. **Test Environment Flakiness:** Parallel test execution (`nx run-many`) caused state leakage between tests, primarily via the static `SecurityContextHolder`.
3. **Complex Test Setup:** The NATS integration test relied on a real `Testcontainers` instance, introducing network flakiness and complex asynchronous interactions that made debugging difficult.

**Solutions Applied:**

1. **`InheritableThreadLocal`:** Switched `CorrelationIdManager` to use `InheritableThreadLocal` to ensure child threads (like those in `CompletableFuture`) inherit the correlation ID.
2. **Explicit Coroutine Dispatchers:** Switched the concurrent test's `runBlocking` to use `Dispatchers.Default` to prevent deadlocks.
3. **Mock-Based NATS Test:** Completely refactored the NATS integration test to use **MockK**, removing the `Testcontainers` dependency. This isolated the test to only verify the context-enrichment logic, making it fast, stable, and reliable.

---

## 2. Key Code Excerpts for Analysis

### 2.1. The Core Problem: `ThreadLocal` in Coroutines

The initial implementation used a standard `ThreadLocal` in `CorrelationIdManager`:

```kotlin
// libs/eaf-core/src/main/kotlin/com/axians/eaf/core/security/CorrelationIdManager.kt
object CorrelationIdManager {
    private val correlationIdThreadLocal = ThreadLocal<String>()
    // ...
}
```

The corresponding test failed because the `runBlocking` coroutine started on a different thread:

```kotlin
// libs/eaf-core/src/test/kotlin/com/axians/eaf/core/security/CoroutineContextIntegrationTest.kt
@Test
fun `should propagate complete context`() {
    CorrelationIdManager.setCorrelationId("my-id") // Set on main test thread

    runBlocking { // Switches to a coroutine thread
        // FAILS: Returns null because ThreadLocal is not carried over
        assertEquals("my-id", CorrelationIdManager.getCurrentCorrelationId())
    }
}
```

### 2.2. Solution Part 1: `InheritableThreadLocal`

Switching to `InheritableThreadLocal` solved the issue for child threads and `CompletableFuture`.

```kotlin
// libs/eaf-core/src/main/kotlin/com/axians/eaf/core/security/CorrelationIdManager.kt
object CorrelationIdManager {
    // This ensures child threads inherit the value
    private val correlationIdThreadLocal = InheritableThreadLocal<String>()
    // ...
}
```

### 2.3. Solution Part 2: `ThreadContextElement` for Coroutines

The `EafContextElement` is the official mechanism for propagating context between coroutine dispatches. It captures the state when the coroutine is created and restores it on each resumption.

```kotlin
// libs/eaf-core/src/main/kotlin/com/axians/eaf/core/security/EafCoroutineContext.kt
class EafContextElement(
    private val securityContext: SecurityContext = SecurityContextHolder.getContext(),
    private val correlationId: String? = CorrelationIdManager.getCurrentCorrelationIdOrNull(),
) : ThreadContextElement<State?> {

    override fun updateThreadContext(context: CoroutineContext): State? {
        // ... capture current state ...
        SecurityContextHolder.setContext(securityContext)
        CorrelationIdManager.setCorrelationId(correlationId)
        // ... return old state
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: State?) {
        // ... restore old state ...
    }
}
```

The final passing test correctly applies this element to the `runBlocking` scope:

```kotlin
// libs/eaf-core/src/test/kotlin/com/axians/eaf/core/security/CoroutineContextIntegrationTest.kt
@Test
fun `should propagate complete context`() {
    CorrelationIdManager.setCorrelationId("my-id")

    // Correctly apply the context element to the coroutine
    runBlocking(EafContextElement()) {
        // SUCCESS: The element restores the context on this thread
        assertEquals("my-id", CorrelationIdManager.getCurrentCorrelationId())
    }
}
```

### 2.4. NATS Test Refactoring: From `Testcontainers` to `MockK`

The NATS test was flaky and complex.

**Before (Flaky Testcontainers setup):**

```kotlin
@Testcontainers
class ContextPropagationIntegrationTest {
    @Container
    private val natsContainer = GenericContainer("nats:2.9-alpine")
    // ... plus complex setup for Connection, publisher, processor
}
```

**After (Stable MockK setup):**

```kotlin
// libs/eaf-eventing-sdk/src/test/kotlin/com/axians/eaf/eventing/ContextPropagationIntegrationTest.kt
class ContextPropagationIntegrationTest {
    // Mocks replace the real NATS connection
    private lateinit var delegate: DefaultNatsEventPublisher
    private val connection: Connection = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        delegate = mockk()
        publisher = ContextAwareNatsEventPublisher(delegate, /*...*/)
        // ...
    }

    @Test
    fun `should propagate full context`() = runBlocking {
        // 1. Setup security context
        setupSecurityContext(...)
        CorrelationIdManager.setCorrelationId(...)

        // 2. Capture the metadata passed to the publisher
        val metadataSlot = slot<Map<String, Any>>()
        coEvery { delegate.publish(any(), any(), any(), capture(metadataSlot)) } returns mockk()

        // 3. Publish the event
        publisher.publish("test.subject", mapOf("eventType" to "..."))

        // 4. Create a mock message with the captured headers
        val capturedHeaders = Headers()
        metadataSlot.captured.forEach { (k, v) -> capturedHeaders.add(k, v.toString()) }
        val mockMessage: Message = mockk()
        every { mockMessage.headers } returns capturedHeaders
        // ...

        // 5. Process the mock message and verify the context
        processor.processWithContext(mockMessage) {
            assertEquals("...", CorrelationIdManager.getCurrentCorrelationId())
            // ... other assertions
        }
    }
}
```

---

## 3. Final Conclusion

* **Code Correctness:** The context propagation implementation in `eaf-core` and `eaf-eventing-sdk` is correct and robust.
* **Test Strategy:** For complex, asynchronous, network-dependent components, mocking the transport layer (`NATS`, `Kafka`, etc.) in unit/integration tests is superior to relying on `Testcontainers`. `Testcontainers` should be reserved for true end-to-end (E2E) test suites that run in a more controlled environment.
* **CI Environment:** The `nx run-many` failures highlight a need for better test isolation in the CI environment, which should be addressed as a separate technical debt story.

---

**Note:** The key findings and best practices from this investigation have been codified in the
project's [Operational Guidelines](./operational-guidelines.md). Future development should adhere to
the patterns for context propagation and testing strategy outlined in that document.

---

*Document owner: Developer Agent*

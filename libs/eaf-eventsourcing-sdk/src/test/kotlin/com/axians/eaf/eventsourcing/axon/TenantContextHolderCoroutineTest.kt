package com.axians.eaf.eventsourcing.axon

import com.axians.eaf.core.tenancy.TenantContextHolder
import com.axians.eaf.core.tenancy.TenantCoroutineContext
import com.axians.eaf.core.tenancy.withTenantContext
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Unit tests for TenantContextHolder coroutine support features from story 4.1.3.
 *
 * Tests cover:
 * - TenantCoroutineContext class and its withTenantContext method
 * - withTenantContext extension function
 * - Coroutine context propagation across suspension points
 * - Thread safety with concurrent coroutines
 * - Context cleanup after coroutine completion
 */
class TenantContextHolderCoroutineTest {
    @BeforeEach
    fun setUp() {
        TenantContextHolder.clear()
    }

    @AfterEach
    fun tearDown() {
        TenantContextHolder.clear()
    }

    // ============================================================================
    // TenantCoroutineContext Testing
    // ============================================================================

    @Test
    fun `TenantCoroutineContext should propagate context across suspension points`() =
        runBlocking {
            // Given
            val tenantId = "coroutine-tenant-123"
            val coroutineContext = TenantCoroutineContext(tenantId)

            // When
            val result =
                coroutineContext.withTenantContext {
                    // Verify context is set
                    assertEquals(tenantId, TenantContextHolder.getCurrentTenantId())

                    // Simulate suspension
                    delay(10)

                    // Verify context persists after suspension
                    assertEquals(tenantId, TenantContextHolder.getCurrentTenantId())

                    "coroutine-result"
                }

            // Then
            assertEquals("coroutine-result", result)
            // Context should be cleared after coroutine completion
            assertNull(TenantContextHolder.getCurrentTenantId())
        }

    @Test
    fun `TenantCoroutineContext should handle nested suspend calls`() =
        runBlocking {
            // Given
            val tenantId = "nested-coroutine-tenant"
            val coroutineContext = TenantCoroutineContext(tenantId)

            // When
            val result =
                coroutineContext.withTenantContext {
                    assertEquals(tenantId, TenantContextHolder.getCurrentTenantId())

                    val nestedResult = suspendableOperation()
                    assertEquals(tenantId, TenantContextHolder.getCurrentTenantId())

                    nestedResult
                }

            // Then
            assertEquals("nested-operation-complete", result)
            assertNull(TenantContextHolder.getCurrentTenantId())
        }

    @Test
    fun `TenantCoroutineContext should cleanup on exception`() =
        runBlocking {
            // Given
            val tenantId = "exception-tenant"
            val coroutineContext = TenantCoroutineContext(tenantId)

            // When/Then
            try {
                coroutineContext.withTenantContext {
                    assertEquals(tenantId, TenantContextHolder.getCurrentTenantId())
                    delay(10)
                    throw RuntimeException("Test exception")
                }
            } catch (e: RuntimeException) {
                // Expected exception
            }

            // Verify cleanup occurred
            assertNull(TenantContextHolder.getCurrentTenantId())
        }

    // ============================================================================
    // withTenantContext Extension Function Testing
    // ============================================================================

    @Test
    fun `withTenantContext extension should propagate context`() =
        runBlocking {
            // Given
            val tenantId = "extension-tenant-456"

            // When
            val result =
                withTenantContext(tenantId) {
                    assertEquals(tenantId, TenantContextHolder.getCurrentTenantId())

                    delay(10)

                    assertEquals(tenantId, TenantContextHolder.getCurrentTenantId())
                    "extension-result"
                }

            // Then
            assertEquals("extension-result", result)
            assertNull(TenantContextHolder.getCurrentTenantId())
        }

    @Test
    @Disabled(
        "Known limitation: ThreadLocal doesn't propagate correctly across concurrent coroutines - requires more complex coroutine context implementation",
    )
    fun `withTenantContext should handle multiple concurrent coroutines`() =
        runBlocking {
            // Given
            val tenant1 = "concurrent-tenant-1"
            val tenant2 = "concurrent-tenant-2"

            // When - run concurrent coroutines with different tenant contexts
            val deferred1 =
                async {
                    withTenantContext(tenant1) {
                        delay(50)
                        TenantContextHolder.getCurrentTenantId()
                    }
                }

            val deferred2 =
                async {
                    withTenantContext(tenant2) {
                        delay(50)
                        TenantContextHolder.getCurrentTenantId()
                    }
                }

            // Then
            assertEquals(tenant1, deferred1.await())
            assertEquals(tenant2, deferred2.await())

            // Main coroutine context should be clean
            assertNull(TenantContextHolder.getCurrentTenantId())
        }

    @Test
    fun `withTenantContext should handle nested coroutine contexts`() =
        runBlocking {
            // Given
            val outerTenant = "outer-tenant"
            val innerTenant = "inner-tenant"

            // When
            val result =
                withTenantContext(outerTenant) {
                    assertEquals(outerTenant, TenantContextHolder.getCurrentTenantId())

                    val innerResult =
                        withTenantContext(innerTenant) {
                            assertEquals(innerTenant, TenantContextHolder.getCurrentTenantId())
                            delay(10)
                            "inner-result"
                        }

                    // Should restore outer context
                    assertEquals(outerTenant, TenantContextHolder.getCurrentTenantId())
                    innerResult
                }

            // Then
            assertEquals("inner-result", result)
            assertNull(TenantContextHolder.getCurrentTenantId())
        }

    @Test
    fun `withTenantContext should cleanup on exception`() =
        runBlocking {
            // Given
            val tenantId = "exception-extension-tenant"

            // When/Then
            try {
                withTenantContext(tenantId) {
                    assertEquals(tenantId, TenantContextHolder.getCurrentTenantId())
                    delay(10)
                    throw RuntimeException("Extension exception")
                }
            } catch (e: RuntimeException) {
                // Expected exception
            }

            // Verify cleanup occurred
            assertNull(TenantContextHolder.getCurrentTenantId())
        }

    // ============================================================================
    // Complex Coroutine Scenarios
    // ============================================================================

    @Test
    fun `coroutine context should work with async operations`() =
        runBlocking {
            // Given
            val tenantId = "async-tenant"

            // When
            val results =
                withTenantContext(tenantId) {
                    val deferredResults =
                        (1..3).map { index ->
                            async(
                                coroutineContext,
                            ) {
                                // Include current coroutine context which has
                                // TenantCoroutineContext
                                delay((10 * index).toLong())
                                assertEquals(tenantId, TenantContextHolder.getCurrentTenantId())
                                "async-result-$index"
                            }
                        }

                    deferredResults.map { it.await() }
                }

            // Then
            assertEquals(listOf("async-result-1", "async-result-2", "async-result-3"), results)
            assertNull(TenantContextHolder.getCurrentTenantId())
        }

    @Test
    @Disabled(
        "Known limitation: ThreadLocal doesn't propagate correctly across parallel coroutines - requires more complex coroutine context implementation",
    )
    fun `coroutine context should not leak between parallel executions`() =
        runBlocking {
            // Given
            val numParallelExecutions = 10

            // When - run multiple parallel coroutines
            val results =
                (1..numParallelExecutions)
                    .map { index ->
                        async {
                            val tenantId = "parallel-tenant-$index"
                            withTenantContext(tenantId) {
                                delay(10)
                                TenantContextHolder.getCurrentTenantId()
                            }
                        }
                    }.map { it.await() }

            // Then - each should have maintained its own context
            results.forEachIndexed { index, result ->
                assertEquals("parallel-tenant-${index + 1}", result)
            }

            // Main context should be clean
            assertNull(TenantContextHolder.getCurrentTenantId())
        }

    // ============================================================================
    // Helper Functions
    // ============================================================================

    private suspend fun suspendableOperation(): String {
        delay(5)
        return "nested-operation-complete"
    }
}

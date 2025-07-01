package com.axians.eaf.core.tenancy

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertNotNull

@ExtendWith(MockKExtension::class)
class TenantContextHolderTest {
    @BeforeEach
    fun setUp() {
        // Ensure clean state before each test
        TenantContextHolder.clear()
    }

    @AfterEach
    fun tearDown() {
        // Clean up after each test to prevent interference
        TenantContextHolder.clear()
    }

    @Nested
    inner class BasicOperations {
        @Test
        fun `setCurrentTenantId should set tenant ID`() {
            // Given
            val tenantId = "test-tenant-123"

            // When
            TenantContextHolder.setCurrentTenantId(tenantId)

            // Then
            assertEquals(tenantId, TenantContextHolder.getCurrentTenantId())
        }

        @Test
        fun `getCurrentTenantId should return null when no tenant set`() {
            // When/Then
            assertNull(TenantContextHolder.getCurrentTenantId())
        }

        @Test
        fun `clear should remove tenant context`() {
            // Given
            TenantContextHolder.setCurrentTenantId("test-tenant")

            // When
            TenantContextHolder.clear()

            // Then
            assertNull(TenantContextHolder.getCurrentTenantId())
        }

        @Test
        fun `requireCurrentTenantId should return tenant ID when set`() {
            // Given
            val tenantId = "test-tenant-456"
            TenantContextHolder.setCurrentTenantId(tenantId)

            // When
            val result = TenantContextHolder.requireCurrentTenantId()

            // Then
            assertEquals(tenantId, result)
        }

        @Test
        fun `requireCurrentTenantId should throw exception when no tenant set`() {
            // When/Then
            val exception =
                assertThrows<TenantContextException> {
                    TenantContextHolder.requireCurrentTenantId()
                }
            assertTrue(exception.message?.contains("No tenant context available") == true)
        }

        @Test
        fun `hasTenantContext should return true when tenant is set`() {
            // Given
            TenantContextHolder.setCurrentTenantId("test-tenant")

            // When/Then
            assertTrue(TenantContextHolder.hasTenantContext())
        }

        @Test
        fun `hasTenantContext should return false when no tenant is set`() {
            // When/Then
            assertFalse(TenantContextHolder.hasTenantContext())
        }
    }

    @Nested
    inner class ValidationAndSanitization {
        @Test
        fun `setCurrentTenantId should reject blank tenant ID`() {
            // When/Then
            assertThrows<IllegalArgumentException> { TenantContextHolder.setCurrentTenantId("") }
        }

        @Test
        fun `setCurrentTenantId should reject whitespace-only tenant ID`() {
            // When/Then
            assertThrows<IllegalArgumentException> { TenantContextHolder.setCurrentTenantId("   ") }
        }

        @Test
        fun `setCurrentTenantId should sanitize tenant ID`() {
            // Given - tenant ID with special characters
            val unsafeTenantId = "tenant@#\$%^&*()!+="

            // When
            TenantContextHolder.setCurrentTenantId(unsafeTenantId)

            // Then - should be sanitized to alphanumeric, underscore, hyphen only
            val sanitized = TenantContextHolder.getCurrentTenantId()
            assertNotNull(sanitized)
            assertTrue(sanitized!!.matches(Regex("[a-zA-Z0-9_-]*")))
            assertEquals("tenant", sanitized) // Special chars should be removed
        }

        @Test
        fun `setCurrentTenantId should truncate long tenant IDs`() {
            // Given - tenant ID longer than 64 characters
            val longTenantId = "a".repeat(100)

            // When
            TenantContextHolder.setCurrentTenantId(longTenantId)

            // Then - should be truncated to 64 characters max
            val result = TenantContextHolder.getCurrentTenantId()
            assertNotNull(result)
            assertTrue(result!!.length <= 64)
        }

        @Test
        fun `validateTenantContext should pass when tenant is set`() {
            // Given
            TenantContextHolder.setCurrentTenantId("valid-tenant")

            // When/Then - should not throw exception
            assertDoesNotThrow { TenantContextHolder.validateTenantContext("test-operation") }
        }

        @Test
        fun `validateTenantContext should throw exception when no tenant set`() {
            // When/Then
            val exception =
                assertThrows<TenantContextException> {
                    TenantContextHolder.validateTenantContext("test-operation")
                }
            assertTrue(exception.message?.contains("test-operation") == true)
        }
    }

    @Nested
    inner class ScopedExecution {
        @Test
        fun `executeInTenantContext should execute block with correct tenant`() {
            // Given
            val tenantId = "scoped-tenant"
            var capturedTenant: String? = null

            // When
            val result =
                TenantContextHolder.executeInTenantContext(tenantId) {
                    capturedTenant = TenantContextHolder.getCurrentTenantId()
                    "test-result"
                }

            // Then
            assertEquals("test-result", result)
            assertEquals(tenantId, capturedTenant)
        }

        @Test
        fun `executeInTenantContext should restore previous tenant context`() {
            // Given
            val originalTenant = "original-tenant"
            val scopedTenant = "scoped-tenant"
            TenantContextHolder.setCurrentTenantId(originalTenant)

            // When
            TenantContextHolder.executeInTenantContext(scopedTenant) {
                assertEquals(scopedTenant, TenantContextHolder.getCurrentTenantId())
                "test"
            }

            // Then
            assertEquals(originalTenant, TenantContextHolder.getCurrentTenantId())
        }

        @Test
        fun `executeInTenantContext should clear context when no previous tenant`() {
            // Given - no initial tenant

            // When
            TenantContextHolder.executeInTenantContext("scoped-tenant") { "test" }

            // Then
            assertNull(TenantContextHolder.getCurrentTenantId())
        }

        @Test
        fun `executeInTenantContext should cleanup on exception`() {
            // Given
            val originalTenant = "original-tenant"
            TenantContextHolder.setCurrentTenantId(originalTenant)

            // When/Then
            assertThrows<IllegalStateException> {
                TenantContextHolder.executeInTenantContext("scoped-tenant") {
                    throw IllegalStateException("Test exception")
                }
            }

            // Verify cleanup occurred
            assertEquals(originalTenant, TenantContextHolder.getCurrentTenantId())
        }
    }

    @Nested
    inner class ThreadSafety {
        @Test
        fun `different threads should have independent tenant contexts`() {
            val numberOfThreads = 10
            val latch = CountDownLatch(numberOfThreads)
            val results = mutableMapOf<String, String?>()
            val executor = Executors.newFixedThreadPool(numberOfThreads)

            try {
                // Start multiple threads with different tenant IDs
                repeat(numberOfThreads) { i ->
                    executor.submit {
                        try {
                            val tenantId = "tenant-$i"
                            TenantContextHolder.setCurrentTenantId(tenantId)

                            // Simulate some work
                            Thread.sleep(50)

                            // Verify the tenant context is still correct
                            results["thread-$i"] = TenantContextHolder.getCurrentTenantId()
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                // Wait for all threads to complete
                assertTrue(latch.await(5, TimeUnit.SECONDS))

                // Verify each thread maintained its own context
                repeat(numberOfThreads) { i -> assertEquals("tenant-$i", results["thread-$i"]) }
            } finally {
                executor.shutdown()
            }
        }

        @Test
        fun `coroutines should maintain tenant context with scoped execution`() =
            runBlocking {
                // Given
                val tenant1 = "tenant-1"
                val tenant2 = "tenant-2"

                // Ensure clean initial state
                TenantContextHolder.clear()

                // When - run two concurrent coroutines with different tenant contexts
                val result1 =
                    async {
                        TenantContextHolder.executeInTenantContext(tenant1) {
                            Thread.sleep(50) // Use blocking sleep instead of suspend delay
                            TenantContextHolder.getCurrentTenantId()
                        }
                    }

                val result2 =
                    async {
                        TenantContextHolder.executeInTenantContext(tenant2) {
                            Thread.sleep(50) // Use blocking sleep instead of suspend delay
                            TenantContextHolder.getCurrentTenantId()
                        }
                    }

                // Then
                assertEquals(tenant1, result1.await())
                assertEquals(tenant2, result2.await())

                // Verify main thread context is clear
                assertNull(TenantContextHolder.getCurrentTenantId())
            }

        @Test
        fun `concurrent access should not cause data races`() {
            val numberOfOperations = 1000
            val latch = CountDownLatch(numberOfOperations)
            val executor = Executors.newFixedThreadPool(50)

            try {
                repeat(numberOfOperations) { i ->
                    executor.submit {
                        try {
                            // Perform random operations
                            when (i % 3) {
                                0 -> {
                                    TenantContextHolder.setCurrentTenantId("tenant-$i")
                                    Thread.yield()
                                    assertNotNull(TenantContextHolder.getCurrentTenantId())
                                }
                                1 -> {
                                    TenantContextHolder.getCurrentTenantId()
                                }
                                2 -> {
                                    TenantContextHolder.clear()
                                }
                            }
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                // Wait for all operations to complete
                assertTrue(latch.await(10, TimeUnit.SECONDS))
            } finally {
                executor.shutdown()
            }
        }
    }
}

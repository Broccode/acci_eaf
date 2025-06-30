package com.axians.eaf.core.tenancy.integration

import com.axians.eaf.core.tenancy.TenantContextHolder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Function
import java.util.function.Supplier
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TenantContextCompletableFutureTest {
    @BeforeEach
    fun setUp() {
        TenantContextHolder.clear()
    }

    @AfterEach
    fun tearDown() {
        TenantContextHolder.clear()
    }

    @Test
    fun `supplyWithTenantContext should propagate tenant context to supplier`() {
        // Arrange
        val testTenantId = "test-tenant"
        TenantContextHolder.setCurrentTenantId(testTenantId)

        var supplierTenantId: String? = null
        val supplier =
            Supplier {
                supplierTenantId = TenantContextHolder.getCurrentTenantId()
                "result"
            }

        // Act
        val future = TenantContextCompletableFuture.supplyWithTenantContext(supplier)
        val result = future.get(1, TimeUnit.SECONDS)

        // Assert
        assertEquals("result", result)
        assertEquals(testTenantId, supplierTenantId)
    }

    @Test
    fun `supplyWithTenantContext should handle null tenant context`() {
        // Arrange
        var supplierTenantId: String? = "initial"
        val supplier =
            Supplier {
                supplierTenantId = TenantContextHolder.getCurrentTenantId()
                "result"
            }

        // Act
        val future = TenantContextCompletableFuture.supplyWithTenantContext(supplier)
        val result = future.get(1, TimeUnit.SECONDS)

        // Assert
        assertEquals("result", result)
        assertNull(supplierTenantId)
    }

    @Test
    fun `thenApplyWithTenantContext extension should propagate context`() {
        // Arrange
        val testTenantId = "test-tenant"
        TenantContextHolder.setCurrentTenantId(testTenantId)

        var functionTenantId: String? = null
        val function =
            Function<String, String> { input ->
                functionTenantId = TenantContextHolder.getCurrentTenantId()
                "processed-$input"
            }

        // Create the future chain while tenant context is set
        val futurePipeline =
            CompletableFuture.supplyAsync { "input" }.thenApplyWithTenantContext(function)

        TenantContextHolder.clear() // Clear context to test propagation

        // Act
        val result = futurePipeline.get(1, TimeUnit.SECONDS)

        // Assert
        assertEquals("processed-input", result)
        assertEquals(testTenantId, functionTenantId)
    }
}

package com.axians.eaf.core.tenancy.integration

import com.axians.eaf.core.tenancy.TenantContextHolder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.Ordered

class TenantContextSynchronizationFilterTest {
    private lateinit var bridge: SecurityTenantContextBridge
    private lateinit var filter: TenantContextSynchronizationFilter
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var filterChain: FilterChain

    @BeforeEach
    fun setUp() {
        bridge = mockk<SecurityTenantContextBridge>(relaxed = true)
        filter = TenantContextSynchronizationFilter(bridge)
        request = mockk<HttpServletRequest>(relaxed = true)
        response = mockk<HttpServletResponse>(relaxed = true)
        filterChain = mockk<FilterChain>(relaxed = true)

        // Clean up any existing tenant context
        TenantContextHolder.clear()

        // Default request setup
        every { request.requestURI } returns "/api/test"
    }

    @AfterEach
    fun tearDown() {
        TenantContextHolder.clear()
    }

    @Test
    fun `filter should have correct order`() {
        // Then
        assertEquals(TenantContextSynchronizationFilter.FILTER_ORDER, filter.order)
        assertTrue(filter is Ordered)
    }

    @Test
    fun `doFilterInternal should synchronize tenant context successfully`() {
        // Given
        val tenantId = "tenant-123"
        every { bridge.synchronizeWithFallback(null, true) } answers
            {
                // Simulate bridge setting tenant context
                TenantContextHolder.setCurrentTenantId(tenantId)
                true
            }

        // When
        filter.doFilterInternal(request, response, filterChain)

        // Then
        verify { bridge.synchronizeWithFallback(null, true) }
        verify { response.setHeader("X-Resolved-Tenant-ID", tenantId) }
        verify { filterChain.doFilter(request, response) }
        verify { bridge.clearIfSynchronized() }
    }

    @Test
    fun `doFilterInternal should use header fallback when available`() {
        // Given
        val fallbackTenantId = "header-tenant"
        every { request.getHeader("X-Tenant-ID") } returns fallbackTenantId
        every { bridge.synchronizeWithFallback(fallbackTenantId, true) } answers
            {
                TenantContextHolder.setCurrentTenantId(fallbackTenantId)
                true
            }

        // When
        filter.doFilterInternal(request, response, filterChain)

        // Then
        verify { bridge.synchronizeWithFallback(fallbackTenantId, true) }
        verify { response.setHeader("X-Resolved-Tenant-ID", fallbackTenantId) }
        verify { filterChain.doFilter(request, response) }
    }

    @Test
    fun `doFilterInternal should handle multiple tenant headers and use first available`() {
        // Given
        val tenantId = "tenant-from-header"
        every { request.getHeader("X-Tenant-ID") } returns null
        every { request.getHeader("Tenant-ID") } returns tenantId
        every { bridge.synchronizeWithFallback(tenantId, true) } answers
            {
                TenantContextHolder.setCurrentTenantId(tenantId)
                true
            }

        // When
        filter.doFilterInternal(request, response, filterChain)

        // Then
        verify { bridge.synchronizeWithFallback(tenantId, true) }
        verify { response.setHeader("X-Resolved-Tenant-ID", tenantId) }
    }

    @Test
    fun `doFilterInternal should sanitize header tenant ID`() {
        // Given
        val unsafeTenantId = "  tenant-123!@#$%^&*()+={}[]|\\:;\"'<>,.?/  "
        val sanitizedTenantId = "tenant-123" // After removing special chars and trimming
        every { request.getHeader("X-Tenant-ID") } returns unsafeTenantId
        every { bridge.synchronizeWithFallback(sanitizedTenantId, true) } answers
            {
                TenantContextHolder.setCurrentTenantId(sanitizedTenantId)
                true
            }

        // When
        filter.doFilterInternal(request, response, filterChain)

        // Then
        verify { bridge.synchronizeWithFallback(sanitizedTenantId, true) }
    }

    @Test
    fun `doFilterInternal should continue processing when synchronization fails`() {
        // Given
        every { bridge.synchronizeWithFallback(any(), any()) } returns false

        // When
        filter.doFilterInternal(request, response, filterChain)

        // Then
        verify { bridge.synchronizeWithFallback(null, true) }
        verify { filterChain.doFilter(request, response) }
        verify { bridge.clearIfSynchronized() }
        verify(exactly = 0) { response.setHeader("X-Resolved-Tenant-ID", any()) }
    }

    @Test
    fun `doFilterInternal should handle exceptions gracefully and continue processing`() {
        // Given
        every { bridge.synchronizeWithFallback(any(), any()) } throws
            RuntimeException("Bridge error")

        // When
        filter.doFilterInternal(request, response, filterChain)

        // Then
        verify { filterChain.doFilter(request, response) }
        verify { bridge.clearIfSynchronized() }
    }

    @Test
    fun `doFilterInternal should handle cleanup exceptions gracefully`() {
        // Given
        every { bridge.synchronizeWithFallback(any(), any()) } returns false
        every { bridge.clearIfSynchronized() } throws RuntimeException("Cleanup error")

        // When
        filter.doFilterInternal(request, response, filterChain)

        // Then
        verify { filterChain.doFilter(request, response) }
        verify { bridge.clearIfSynchronized() }
    }

    @Test
    fun `shouldNotFilter should skip actuator endpoints`() {
        // Given
        val actuatorPaths =
            listOf(
                "/actuator/health",
                "/actuator/metrics",
                "/actuator/info",
                "/health",
                "/metrics",
            )

        actuatorPaths.forEach { path ->
            every { request.requestURI } returns path

            // When
            val shouldSkip = filter.shouldNotFilter(request)

            // Then
            assertTrue(shouldSkip, "Should skip filtering for path: $path")
        }
    }

    @Test
    fun `shouldNotFilter should skip static resources`() {
        // Given
        val staticPaths =
            listOf(
                "/static/css/app.css",
                "/webjars/bootstrap/css/bootstrap.css",
                "/css/styles.css",
                "/js/app.js",
                "/images/logo.png",
                "/favicon.ico",
                "/logo.png",
                "/app.css",
                "/script.js",
            )

        staticPaths.forEach { path ->
            every { request.requestURI } returns path

            // When
            val shouldSkip = filter.shouldNotFilter(request)

            // Then
            assertTrue(shouldSkip, "Should skip filtering for path: $path")
        }
    }

    @Test
    fun `shouldNotFilter should not skip API endpoints`() {
        // Given
        val apiPaths =
            listOf(
                "/api/users",
                "/api/v1/tenants",
                "/admin/dashboard",
                "/app/main",
                "/service/data",
            )

        apiPaths.forEach { path ->
            every { request.requestURI } returns path

            // When
            val shouldSkip = filter.shouldNotFilter(request)

            // Then
            assertFalse(shouldSkip, "Should not skip filtering for path: $path")
        }
    }

    @Test
    fun `filter should handle blank header values`() {
        // Given
        every { request.getHeader("X-Tenant-ID") } returns "   "
        every { request.getHeader("Tenant-ID") } returns ""
        every { bridge.synchronizeWithFallback(null, true) } returns false

        // When
        filter.doFilterInternal(request, response, filterChain)

        // Then
        verify { bridge.synchronizeWithFallback(null, true) }
        verify { filterChain.doFilter(request, response) }
    }

    @Test
    fun `filter should limit tenant ID length from headers`() {
        // Given
        val longTenantId = "a".repeat(100) // 100 characters
        val expectedTenantId = "a".repeat(64) // Should be limited to 64
        every { request.getHeader("X-Tenant-ID") } returns longTenantId
        every { bridge.synchronizeWithFallback(expectedTenantId, true) } answers
            {
                TenantContextHolder.setCurrentTenantId(expectedTenantId)
                true
            }

        // When
        filter.doFilterInternal(request, response, filterChain)

        // Then
        verify { bridge.synchronizeWithFallback(expectedTenantId, true) }
    }

    @Test
    fun `filter should prefer X-Tenant-ID over Tenant-ID header`() {
        // Given
        val xTenantId = "x-tenant"
        val tenantId = "tenant-fallback"
        every { request.getHeader("X-Tenant-ID") } returns xTenantId
        every { request.getHeader("Tenant-ID") } returns tenantId
        every { bridge.synchronizeWithFallback(xTenantId, true) } answers
            {
                TenantContextHolder.setCurrentTenantId(xTenantId)
                true
            }

        // When
        filter.doFilterInternal(request, response, filterChain)

        // Then
        verify { bridge.synchronizeWithFallback(xTenantId, true) }
        verify(exactly = 0) { bridge.synchronizeWithFallback(tenantId, true) }
    }
}

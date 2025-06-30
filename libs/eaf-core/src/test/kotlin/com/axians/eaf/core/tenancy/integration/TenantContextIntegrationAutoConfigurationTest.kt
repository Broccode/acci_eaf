package com.axians.eaf.core.tenancy.integration

import com.axians.eaf.core.security.DefaultEafSecurityContextHolder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TenantContextIntegrationAutoConfigurationTest {
    @Test
    fun `should create default EafSecurityContextHolder`() {
        // Given
        val config = TenantContextIntegrationAutoConfiguration()

        // When
        val result = config.defaultEafSecurityContextHolder()

        // Then
        assertNotNull(result)
        assertTrue(result is DefaultEafSecurityContextHolder)
    }

    @Test
    fun `should create SecurityTenantContextBridge`() {
        // Given
        val config = TenantContextIntegrationAutoConfiguration()
        val securityContextHolder = DefaultEafSecurityContextHolder()

        // When
        val result = config.securityTenantContextBridge(securityContextHolder)

        // Then
        assertNotNull(result)
        assertTrue(result is SecurityTenantContextBridge)
    }

    @Test
    fun `should create TenantContextSynchronizationFilter`() {
        // Given
        val config = TenantContextIntegrationAutoConfiguration()
        val bridge = SecurityTenantContextBridge(DefaultEafSecurityContextHolder())

        // When
        val result = config.tenantContextSynchronizationFilter(bridge)

        // Then
        assertNotNull(result)
        assertTrue(result is TenantContextSynchronizationFilter)
    }

    @Test
    fun `should create properties with default values`() {
        // Given/When
        val properties = TenantContextIntegrationProperties()

        // Then
        assertTrue(properties.enabled)
        assertEquals(TenantContextSynchronizationFilter.FILTER_ORDER, properties.filterOrder)
        assertTrue(properties.urlPatterns.isEmpty())
        assertTrue(properties.respectExistingContext)
        assertTrue(properties.enableHeaderFallback)
        assertEquals(listOf("X-Tenant-ID", "Tenant-ID"), properties.tenantHeaders)
        assertTrue(properties.addResponseHeaders)
        assertEquals(64, properties.maxTenantIdLength)
        assertEquals(false, properties.enableDetailedLogging)
    }

    @Test
    fun `should create properties with custom values`() {
        // Given/When
        val properties =
            TenantContextIntegrationProperties(
                enabled = false,
                filterOrder = -25,
                urlPatterns = listOf("/api/*"),
                respectExistingContext = false,
                enableHeaderFallback = false,
                tenantHeaders = listOf("Custom-Tenant"),
                addResponseHeaders = false,
                maxTenantIdLength = 32,
                enableDetailedLogging = true,
            )

        // Then
        assertEquals(false, properties.enabled)
        assertEquals(-25, properties.filterOrder)
        assertEquals(listOf("/api/*"), properties.urlPatterns)
        assertEquals(false, properties.respectExistingContext)
        assertEquals(false, properties.enableHeaderFallback)
        assertEquals(listOf("Custom-Tenant"), properties.tenantHeaders)
        assertEquals(false, properties.addResponseHeaders)
        assertEquals(32, properties.maxTenantIdLength)
        assertEquals(true, properties.enableDetailedLogging)
    }

    @Test
    fun `should validate configuration constraints`() {
        // Given/When/Then - Testing edge cases for validation

        // Valid minimum values
        val minValidProperties =
            TenantContextIntegrationProperties(
                maxTenantIdLength = 8, // Minimum allowed
                tenantHeaders = listOf("Custom-Header"), // At least one header
            )
        assertEquals(8, minValidProperties.maxTenantIdLength)
        assertEquals(listOf("Custom-Header"), minValidProperties.tenantHeaders)

        // Valid maximum values
        val maxValidProperties =
            TenantContextIntegrationProperties(
                maxTenantIdLength = 256, // Maximum allowed
            )
        assertEquals(256, maxValidProperties.maxTenantIdLength)
    }
}

package com.axians.eaf.core.tenancy.integration

import com.axians.eaf.core.security.EafSecurityContextHolder
import com.axians.eaf.core.tenancy.TenantContextException
import com.axians.eaf.core.tenancy.TenantContextHolder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SecurityTenantContextBridgeTest {
    private lateinit var securityContextHolder: EafSecurityContextHolder
    private lateinit var bridge: SecurityTenantContextBridge

    @BeforeEach
    fun setUp() {
        securityContextHolder = mockk<EafSecurityContextHolder>()
        bridge = SecurityTenantContextBridge(securityContextHolder)
        // Clean up any existing tenant context
        TenantContextHolder.clear()
    }

    @AfterEach
    fun tearDown() {
        // Clean up tenant context after each test
        TenantContextHolder.clear()
    }

    @Test
    fun `synchronizeTenantContext should sync tenant from security context when available`() {
        // Given
        val tenantId = "tenant-123"
        every { securityContextHolder.getTenantIdOrNull() } returns tenantId

        // When
        val result = bridge.synchronizeTenantContext()

        // Then
        assertTrue(result)
        assertEquals(tenantId, TenantContextHolder.getCurrentTenantId())
        verify { securityContextHolder.getTenantIdOrNull() }
    }

    @Test
    fun `synchronizeTenantContext should return false when no tenant in security context`() {
        // Given
        every { securityContextHolder.getTenantIdOrNull() } returns null

        // When
        val result = bridge.synchronizeTenantContext()

        // Then
        assertFalse(result)
        assertNull(TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `synchronizeTenantContext should respect existing tenant context by default`() {
        // Given
        val existingTenantId = "existing-tenant"
        val securityTenantId = "security-tenant"
        TenantContextHolder.setCurrentTenantId(existingTenantId)
        every { securityContextHolder.getTenantIdOrNull() } returns securityTenantId

        // When
        val result = bridge.synchronizeTenantContext()

        // Then
        assertTrue(result)
        assertEquals(existingTenantId, TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `synchronizeTenantContext should override existing context when respectExisting is false`() {
        // Given
        val existingTenantId = "existing-tenant"
        val securityTenantId = "security-tenant"
        TenantContextHolder.setCurrentTenantId(existingTenantId)
        every { securityContextHolder.getTenantIdOrNull() } returns securityTenantId

        // When
        val result = bridge.synchronizeTenantContext(respectExisting = false)

        // Then
        assertTrue(result)
        assertEquals(securityTenantId, TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `forceSynchronizeTenantContext should override existing context`() {
        // Given
        val existingTenantId = "existing-tenant"
        val securityTenantId = "security-tenant"
        TenantContextHolder.setCurrentTenantId(existingTenantId)
        every { securityContextHolder.getTenantIdOrNull() } returns securityTenantId

        // When
        val result = bridge.forceSynchronizeTenantContext()

        // Then
        assertTrue(result)
        assertEquals(securityTenantId, TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `synchronizeWithFallback should use security context first`() {
        // Given
        val securityTenantId = "security-tenant"
        val fallbackTenantId = "fallback-tenant"
        every { securityContextHolder.getTenantIdOrNull() } returns securityTenantId

        // When
        val result = bridge.synchronizeWithFallback(fallbackTenantId)

        // Then
        assertTrue(result)
        assertEquals(securityTenantId, TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `synchronizeWithFallback should use fallback when security context empty`() {
        // Given
        val fallbackTenantId = "fallback-tenant"
        every { securityContextHolder.getTenantIdOrNull() } returns null

        // When
        val result = bridge.synchronizeWithFallback(fallbackTenantId)

        // Then
        assertTrue(result)
        assertEquals(fallbackTenantId, TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `synchronizeWithFallback should return false when both security and fallback are empty`() {
        // Given
        every { securityContextHolder.getTenantIdOrNull() } returns null

        // When
        val result = bridge.synchronizeWithFallback(null)

        // Then
        assertFalse(result)
        assertNull(TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `validateSynchronization should pass when both contexts are null`() {
        // Given
        every { securityContextHolder.getTenantIdOrNull() } returns null

        // When/Then - should not throw
        bridge.validateSynchronization()
    }

    @Test
    fun `validateSynchronization should pass when both contexts have same tenant`() {
        // Given
        val tenantId = "tenant-123"
        TenantContextHolder.setCurrentTenantId(tenantId)
        every { securityContextHolder.getTenantIdOrNull() } returns tenantId

        // When/Then - should not throw
        bridge.validateSynchronization()
    }

    @Test
    fun `validateSynchronization should throw when security has tenant but context is empty`() {
        // Given
        val tenantId = "tenant-123"
        every { securityContextHolder.getTenantIdOrNull() } returns tenantId

        // When/Then
        val exception = assertThrows<TenantContextException> { bridge.validateSynchronization() }
        assertTrue(exception.message!!.contains("Security context has tenant ID"))
    }

    @Test
    fun `validateSynchronization should throw when contexts have different tenants`() {
        // Given
        val securityTenantId = "security-tenant"
        val contextTenantId = "context-tenant"
        TenantContextHolder.setCurrentTenantId(contextTenantId)
        every { securityContextHolder.getTenantIdOrNull() } returns securityTenantId

        // When/Then
        val exception = assertThrows<TenantContextException> { bridge.validateSynchronization() }
        assertTrue(exception.message!!.contains("Tenant context mismatch"))
    }

    @Test
    fun `getEffectiveTenantId should return security context tenant when available`() {
        // Given
        val securityTenantId = "security-tenant"
        val contextTenantId = "context-tenant"
        TenantContextHolder.setCurrentTenantId(contextTenantId)
        every { securityContextHolder.getTenantIdOrNull() } returns securityTenantId

        // When
        val result = bridge.getEffectiveTenantId()

        // Then
        assertEquals(securityTenantId, result)
    }

    @Test
    fun `getEffectiveTenantId should return context tenant when security context empty`() {
        // Given
        val contextTenantId = "context-tenant"
        TenantContextHolder.setCurrentTenantId(contextTenantId)
        every { securityContextHolder.getTenantIdOrNull() } returns null

        // When
        val result = bridge.getEffectiveTenantId()

        // Then
        assertEquals(contextTenantId, result)
    }

    @Test
    fun `getEffectiveTenantId should return null when both contexts empty`() {
        // Given
        every { securityContextHolder.getTenantIdOrNull() } returns null

        // When
        val result = bridge.getEffectiveTenantId()

        // Then
        assertNull(result)
    }

    @Test
    fun `hasTenantContext should return true when security context has tenant`() {
        // Given
        every { securityContextHolder.getTenantIdOrNull() } returns "tenant-123"

        // When
        val result = bridge.hasTenantContext()

        // Then
        assertTrue(result)
    }

    @Test
    fun `hasTenantContext should return true when tenant context holder has tenant`() {
        // Given
        TenantContextHolder.setCurrentTenantId("tenant-123")
        every { securityContextHolder.getTenantIdOrNull() } returns null

        // When
        val result = bridge.hasTenantContext()

        // Then
        assertTrue(result)
    }

    @Test
    fun `hasTenantContext should return false when both contexts empty`() {
        // Given
        every { securityContextHolder.getTenantIdOrNull() } returns null

        // When
        val result = bridge.hasTenantContext()

        // Then
        assertFalse(result)
    }

    @Test
    fun `clearIfSynchronized should clear context when contexts match`() {
        // Given
        val tenantId = "tenant-123"
        TenantContextHolder.setCurrentTenantId(tenantId)
        every { securityContextHolder.getTenantIdOrNull() } returns tenantId

        // When
        bridge.clearIfSynchronized()

        // Then
        assertNull(TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `clearIfSynchronized should not clear context when contexts different`() {
        // Given
        val contextTenantId = "context-tenant"
        val securityTenantId = "security-tenant"
        TenantContextHolder.setCurrentTenantId(contextTenantId)
        every { securityContextHolder.getTenantIdOrNull() } returns securityTenantId

        // When
        bridge.clearIfSynchronized()

        // Then
        assertEquals(contextTenantId, TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `clearIfSynchronized should not clear when security context empty`() {
        // Given
        val contextTenantId = "context-tenant"
        TenantContextHolder.setCurrentTenantId(contextTenantId)
        every { securityContextHolder.getTenantIdOrNull() } returns null

        // When
        bridge.clearIfSynchronized()

        // Then
        assertEquals(contextTenantId, TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `synchronizeTenantContext should handle exceptions gracefully`() {
        // Given
        every { securityContextHolder.getTenantIdOrNull() } throws
            RuntimeException("Security error")

        // When
        val result = bridge.synchronizeTenantContext()

        // Then
        assertFalse(result)
        assertNull(TenantContextHolder.getCurrentTenantId())
    }
}

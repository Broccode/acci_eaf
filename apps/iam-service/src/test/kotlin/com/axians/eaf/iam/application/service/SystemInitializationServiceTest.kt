package com.axians.eaf.iam.application.service

import com.axians.eaf.iam.application.port.outbound.SaveTenantPort
import com.axians.eaf.iam.application.port.outbound.SaveTenantResult
import com.axians.eaf.iam.domain.model.Tenant
import com.axians.eaf.iam.domain.model.User
import com.axians.eaf.iam.domain.model.UserRole
import com.axians.eaf.iam.infrastructure.config.SystemInitializationProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SystemInitializationServiceTest {
    private val saveTenantPort = mockk<SaveTenantPort>()
    private val properties =
        SystemInitializationProperties(
            initializeDefaultTenant = true,
            defaultTenantName = "TestTenant",
            defaultSuperAdminEmail = "admin@test.com",
        )

    private val systemInitializationService =
        SystemInitializationService(
            saveTenantPort = saveTenantPort,
            properties = properties,
        )

    @Test
    fun `should create default tenant and superadmin when system is not initialized`() {
        // Given
        val tenantSlot = slot<Tenant>()
        val userSlot = slot<User>()
        val savedTenant = Tenant.create("TestTenant")
        val savedUser = User.createTenantAdmin(savedTenant.tenantId, "admin@test.com")

        every { saveTenantPort.existsByTenantName("TestTenant") } returns false
        every { saveTenantPort.existsByEmail("admin@test.com") } returns false
        every { saveTenantPort.saveTenantWithAdmin(capture(tenantSlot), capture(userSlot)) } returns
            SaveTenantResult(savedTenant, savedUser)

        // When
        val result = systemInitializationService.initializeDefaultTenantIfRequired()

        // Then
        assertTrue(result.wasInitialized)
        assertEquals("TestTenant", result.message)

        // Verify the captured tenant
        assertEquals("TestTenant", tenantSlot.captured.name)

        // Verify the captured user
        assertEquals("admin@test.com", userSlot.captured.email)
        assertEquals(UserRole.SUPER_ADMIN, userSlot.captured.role)
        assertEquals(tenantSlot.captured.tenantId, userSlot.captured.tenantId)

        verify { saveTenantPort.existsByTenantName("TestTenant") }
        verify { saveTenantPort.existsByEmail("admin@test.com") }
        verify { saveTenantPort.saveTenantWithAdmin(any(), any()) }
    }

    @Test
    fun `should not initialize when default tenant already exists`() {
        // Given
        every { saveTenantPort.existsByTenantName("TestTenant") } returns true
        every { saveTenantPort.existsByEmail("admin@test.com") } returns false

        // When
        val result = systemInitializationService.initializeDefaultTenantIfRequired()

        // Then
        assertFalse(result.wasInitialized)
        assertEquals(
            "System already initialized - default tenant 'TestTenant' exists",
            result.message,
        )

        verify { saveTenantPort.existsByTenantName("TestTenant") }
        verify { saveTenantPort.existsByEmail("admin@test.com") }
        verify(exactly = 0) { saveTenantPort.saveTenantWithAdmin(any(), any()) }
    }

    @Test
    fun `should not initialize when superadmin email already exists`() {
        // Given
        every { saveTenantPort.existsByTenantName("TestTenant") } returns false
        every { saveTenantPort.existsByEmail("admin@test.com") } returns true

        // When
        val result = systemInitializationService.initializeDefaultTenantIfRequired()

        // Then
        assertFalse(result.wasInitialized)
        assertEquals(
            "System already initialized - superadmin email 'admin@test.com' exists",
            result.message,
        )

        verify { saveTenantPort.existsByTenantName("TestTenant") }
        verify { saveTenantPort.existsByEmail("admin@test.com") }
        verify(exactly = 0) { saveTenantPort.saveTenantWithAdmin(any(), any()) }
    }

    @Test
    fun `should throw exception when tenant creation fails`() {
        // Given
        every { saveTenantPort.existsByTenantName("TestTenant") } returns false
        every { saveTenantPort.existsByEmail("admin@test.com") } returns false
        every { saveTenantPort.saveTenantWithAdmin(any(), any()) } throws
            RuntimeException("Database error")

        // When & Then
        assertThrows<RuntimeException> {
            systemInitializationService.initializeDefaultTenantIfRequired()
        }

        verify { saveTenantPort.existsByTenantName("TestTenant") }
        verify { saveTenantPort.existsByEmail("admin@test.com") }
        verify { saveTenantPort.saveTenantWithAdmin(any(), any()) }
    }

    @Test
    fun `should create superadmin user with correct role and tenant association`() {
        // Given
        val tenantSlot = slot<Tenant>()
        val userSlot = slot<User>()
        val savedTenant = Tenant.create("TestTenant")
        val savedUser = User.createTenantAdmin(savedTenant.tenantId, "admin@test.com")

        every { saveTenantPort.existsByTenantName("TestTenant") } returns false
        every { saveTenantPort.existsByEmail("admin@test.com") } returns false
        every { saveTenantPort.saveTenantWithAdmin(capture(tenantSlot), capture(userSlot)) } returns
            SaveTenantResult(savedTenant, savedUser)

        // When
        systemInitializationService.initializeDefaultTenantIfRequired()

        // Then
        val capturedUser = userSlot.captured
        assertEquals(UserRole.SUPER_ADMIN, capturedUser.role)
        assertEquals(tenantSlot.captured.tenantId, capturedUser.tenantId)
        assertEquals("admin@test.com", capturedUser.email)
    }
}

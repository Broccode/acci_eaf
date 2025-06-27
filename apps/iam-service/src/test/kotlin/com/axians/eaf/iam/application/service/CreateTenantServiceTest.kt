package com.axians.eaf.iam.application.service

import com.axians.eaf.iam.application.port.inbound.CreateTenantCommand
import com.axians.eaf.iam.application.port.outbound.SaveTenantPort
import com.axians.eaf.iam.application.port.outbound.SaveTenantResult
import com.axians.eaf.iam.domain.model.Tenant
import com.axians.eaf.iam.domain.model.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CreateTenantServiceTest {
    private val saveTenantPort = mockk<SaveTenantPort>()
    private val createTenantService = CreateTenantService(saveTenantPort)

    @Test
    fun `should orchestrate tenant creation and save entities when valid data is provided`() {
        // Given
        val command =
            CreateTenantCommand(
                tenantName = "Test Company",
                initialAdminEmail = "admin@testcompany.com",
            )

        val mockTenant = Tenant.create("Test Company")
        val mockUser = User.createTenantAdmin(mockTenant.tenantId, "admin@testcompany.com")
        val mockResult = SaveTenantResult(mockTenant, mockUser)

        every { saveTenantPort.existsByTenantName("Test Company") } returns false
        every { saveTenantPort.existsByEmail("admin@testcompany.com") } returns false
        every { saveTenantPort.saveTenantWithAdmin(any(), any()) } returns mockResult

        // When
        val result = createTenantService.handle(command)

        // Then
        assertNotNull(result)
        assertEquals(mockTenant.tenantId, result.tenantId)
        assertEquals("Test Company", result.tenantName)
        assertEquals(mockUser.userId, result.tenantAdminUserId)
        assertEquals("admin@testcompany.com", result.tenantAdminEmail)
        assertEquals(
            "Invitation link sent to admin@testcompany.com for tenant Test Company",
            result.invitationDetails,
        )

        verify(exactly = 1) { saveTenantPort.existsByTenantName("Test Company") }
        verify(exactly = 1) { saveTenantPort.existsByEmail("admin@testcompany.com") }
        verify(exactly = 1) { saveTenantPort.saveTenantWithAdmin(any(), any()) }
    }

    @Test
    fun `should throw exception when tenant name already exists`() {
        // Given
        val command =
            CreateTenantCommand(
                tenantName = "Existing Company",
                initialAdminEmail = "admin@existing.com",
            )

        every { saveTenantPort.existsByTenantName("Existing Company") } returns true

        // When & Then
        val exception = assertThrows<IllegalArgumentException> { createTenantService.handle(command) }

        assertEquals("Tenant with name 'Existing Company' already exists", exception.message)
        verify(exactly = 1) { saveTenantPort.existsByTenantName("Existing Company") }
        verify(exactly = 0) { saveTenantPort.existsByEmail(any()) }
        verify(exactly = 0) { saveTenantPort.saveTenantWithAdmin(any(), any()) }
    }

    @Test
    fun `should throw exception when admin email already exists`() {
        // Given
        val command =
            CreateTenantCommand(
                tenantName = "New Company",
                initialAdminEmail = "existing@admin.com",
            )

        every { saveTenantPort.existsByTenantName("New Company") } returns false
        every { saveTenantPort.existsByEmail("existing@admin.com") } returns true

        // When & Then
        val exception = assertThrows<IllegalArgumentException> { createTenantService.handle(command) }

        assertEquals("User with email 'existing@admin.com' already exists", exception.message)
        verify(exactly = 1) { saveTenantPort.existsByTenantName("New Company") }
        verify(exactly = 1) { saveTenantPort.existsByEmail("existing@admin.com") }
        verify(exactly = 0) { saveTenantPort.saveTenantWithAdmin(any(), any()) }
    }

    @Test
    fun `should throw exception when tenant name is blank`() {
        // Given
        val command =
            CreateTenantCommand(
                tenantName = "   ",
                initialAdminEmail = "admin@test.com",
            )

        // When & Then
        val exception = assertThrows<IllegalArgumentException> { createTenantService.handle(command) }

        assertEquals("Tenant name cannot be blank", exception.message)
        verify(exactly = 0) { saveTenantPort.existsByTenantName(any()) }
        verify(exactly = 0) { saveTenantPort.existsByEmail(any()) }
        verify(exactly = 0) { saveTenantPort.saveTenantWithAdmin(any(), any()) }
    }

    @Test
    fun `should throw exception when admin email is invalid`() {
        // Given
        val command =
            CreateTenantCommand(
                tenantName = "Valid Company",
                initialAdminEmail = "invalid-email",
            )

        // When & Then
        val exception = assertThrows<IllegalArgumentException> { createTenantService.handle(command) }

        assertEquals("Email must be valid", exception.message)
        verify(exactly = 0) { saveTenantPort.existsByTenantName(any()) }
        verify(exactly = 0) { saveTenantPort.existsByEmail(any()) }
        verify(exactly = 0) { saveTenantPort.saveTenantWithAdmin(any(), any()) }
    }

    @Test
    fun `should handle tenant name with leading and trailing spaces`() {
        // Given
        val command =
            CreateTenantCommand(
                tenantName = "  Trimmed Company  ",
                initialAdminEmail = "admin@trimmed.com",
            )

        val mockTenant = Tenant.create("Trimmed Company")
        val mockUser = User.createTenantAdmin(mockTenant.tenantId, "admin@trimmed.com")
        val mockResult = SaveTenantResult(mockTenant, mockUser)

        every { saveTenantPort.existsByTenantName("Trimmed Company") } returns false
        every { saveTenantPort.existsByEmail("admin@trimmed.com") } returns false
        every { saveTenantPort.saveTenantWithAdmin(any(), any()) } returns mockResult

        // When
        val result = createTenantService.handle(command)

        // Then
        assertEquals("Trimmed Company", result.tenantName)
        verify(exactly = 1) { saveTenantPort.existsByTenantName("Trimmed Company") }
    }
}

package com.axians.eaf.iam.web

import com.axians.eaf.iam.application.port.inbound.CreateTenantCommand
import com.axians.eaf.iam.application.port.inbound.CreateTenantResult
import com.axians.eaf.iam.application.port.inbound.CreateTenantUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class TenantControllerTest {
    private lateinit var createTenantUseCase: CreateTenantUseCase
    private lateinit var tenantController: TenantController

    @BeforeEach
    fun setUp() {
        createTenantUseCase = mockk()
        tenantController = TenantController(createTenantUseCase)
    }

    @Test
    fun `should create tenant when valid request provided`() {
        // Given
        val request =
            CreateTenantRequest(
                tenantName = "Test Company",
                initialAdminEmail = "admin@testcompany.com",
            )

        val expectedResult =
            CreateTenantResult(
                tenantId = "tenant-123",
                tenantName = "Test Company",
                tenantAdminUserId = "user-456",
                tenantAdminEmail = "admin@testcompany.com",
                invitationDetails = "Invitation link sent to admin@testcompany.com for tenant Test Company",
            )

        every {
            createTenantUseCase.handle(
                CreateTenantCommand("Test Company", "admin@testcompany.com"),
            )
        } returns expectedResult

        // When
        val response = tenantController.createTenant(request)

        // Then
        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(expectedResult.tenantId, response.body?.tenantId)
        assertEquals(expectedResult.tenantName, response.body?.tenantName)
        assertEquals(expectedResult.tenantAdminUserId, response.body?.tenantAdminUserId)
        assertEquals(expectedResult.tenantAdminEmail, response.body?.tenantAdminEmail)
        assertEquals(expectedResult.invitationDetails, response.body?.invitationDetails)

        verify { createTenantUseCase.handle(any()) }
    }

    @Test
    fun `should handle business logic validation errors`() {
        // Given
        val request =
            CreateTenantRequest(
                tenantName = "Invalid Company",
                initialAdminEmail = "test@company.com",
            )

        every {
            createTenantUseCase.handle(any())
        } throws IllegalArgumentException("Tenant name contains invalid characters")

        // When
        try {
            tenantController.createTenant(request)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (ex: IllegalArgumentException) {
            // Then
            assertEquals("Tenant name contains invalid characters", ex.message)
        }

        verify { createTenantUseCase.handle(any()) }
    }

    @Test
    fun `should handle tenant name already exists`() {
        // Given
        val request =
            CreateTenantRequest(
                tenantName = "Existing Company",
                initialAdminEmail = "admin@existing.com",
            )

        every {
            createTenantUseCase.handle(any())
        } throws IllegalArgumentException("Tenant with name 'Existing Company' already exists")

        // When
        try {
            tenantController.createTenant(request)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (ex: IllegalArgumentException) {
            // Then
            assertEquals("Tenant with name 'Existing Company' already exists", ex.message)
        }

        verify { createTenantUseCase.handle(any()) }
    }
}

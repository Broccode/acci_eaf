package com.axians.eaf.iam.application.service

import com.axians.eaf.iam.application.port.inbound.UpdateUserStatusCommand
import com.axians.eaf.iam.application.port.outbound.FindUsersByTenantIdPort
import com.axians.eaf.iam.domain.model.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class UpdateUserStatusServiceTest {
    private lateinit var findUsersByTenantIdPort: FindUsersByTenantIdPort
    private lateinit var updateUserStatusService: UpdateUserStatusService

    @BeforeEach
    fun setUp() {
        findUsersByTenantIdPort = mockk()
        updateUserStatusService = UpdateUserStatusService(findUsersByTenantIdPort)
    }

    @Test
    fun `should activate user successfully when valid command provided`() {
        // Given
        val command =
            UpdateUserStatusCommand(
                tenantId = "tenant-123",
                userId = "user-123",
                newStatus = "ACTIVE",
            )
        val existingUser =
            User.createUser(
                tenantId = "tenant-123",
                email = "user@example.com",
                username = "testuser",
            )
        val activatedUser = existingUser.activate()

        every { findUsersByTenantIdPort.findUserByIdAndTenantId("user-123", "tenant-123") } returns existingUser
        every { findUsersByTenantIdPort.saveUser(any()) } returns activatedUser

        // When
        val result = updateUserStatusService.updateUserStatus(command)

        // Then
        assertEquals("user-123", result.userId)
        assertEquals("tenant-123", result.tenantId)
        assertEquals("user@example.com", result.email)
        assertEquals("testuser", result.username)
        assertEquals("PENDING_ACTIVATION", result.previousStatus)
        assertEquals("ACTIVE", result.newStatus)

        verify { findUsersByTenantIdPort.findUserByIdAndTenantId("user-123", "tenant-123") }
        verify { findUsersByTenantIdPort.saveUser(any()) }
    }

    @Test
    fun `should deactivate user successfully when valid command provided`() {
        // Given
        val command =
            UpdateUserStatusCommand(
                tenantId = "tenant-123",
                userId = "user-123",
                newStatus = "INACTIVE",
            )
        val existingUser =
            User
                .createUser(
                    tenantId = "tenant-123",
                    email = "user@example.com",
                    username = "testuser",
                ).activate()
        val deactivatedUser = existingUser.deactivate()

        every { findUsersByTenantIdPort.findUserByIdAndTenantId("user-123", "tenant-123") } returns existingUser
        every { findUsersByTenantIdPort.saveUser(any()) } returns deactivatedUser

        // When
        val result = updateUserStatusService.updateUserStatus(command)

        // Then
        assertEquals("user-123", result.userId)
        assertEquals("ACTIVE", result.previousStatus)
        assertEquals("INACTIVE", result.newStatus)

        verify { findUsersByTenantIdPort.findUserByIdAndTenantId("user-123", "tenant-123") }
        verify { findUsersByTenantIdPort.saveUser(any()) }
    }

    @Test
    fun `should throw exception when user not found`() {
        // Given
        val command =
            UpdateUserStatusCommand(
                tenantId = "tenant-123",
                userId = "nonexistent-user",
                newStatus = "ACTIVE",
            )

        every { findUsersByTenantIdPort.findUserByIdAndTenantId("nonexistent-user", "tenant-123") } returns null

        // When & Then
        val exception =
            assertThrows<IllegalArgumentException> {
                updateUserStatusService.updateUserStatus(command)
            }

        assertEquals("User with ID nonexistent-user not found in tenant tenant-123", exception.message)
        verify { findUsersByTenantIdPort.findUserByIdAndTenantId("nonexistent-user", "tenant-123") }
        verify(exactly = 0) { findUsersByTenantIdPort.saveUser(any()) }
    }

    @Test
    fun `should throw exception when invalid status provided`() {
        // Given
        val command =
            UpdateUserStatusCommand(
                tenantId = "tenant-123",
                userId = "user-123",
                newStatus = "INVALID_STATUS",
            )
        val existingUser =
            User.createUser(
                tenantId = "tenant-123",
                email = "user@example.com",
                username = "testuser",
            )

        every { findUsersByTenantIdPort.findUserByIdAndTenantId("user-123", "tenant-123") } returns existingUser

        // When & Then
        val exception =
            assertThrows<IllegalArgumentException> {
                updateUserStatusService.updateUserStatus(command)
            }

        assertEquals(
            "Invalid status: INVALID_STATUS. Valid statuses are: PENDING_ACTIVATION, ACTIVE, INACTIVE, SUSPENDED",
            exception.message,
        )
        verify { findUsersByTenantIdPort.findUserByIdAndTenantId("user-123", "tenant-123") }
        verify(exactly = 0) { findUsersByTenantIdPort.saveUser(any()) }
    }

    @Test
    fun `should throw exception when tenant id is blank`() {
        // Given
        val command =
            UpdateUserStatusCommand(
                tenantId = "",
                userId = "user-123",
                newStatus = "ACTIVE",
            )

        // When & Then
        val exception =
            assertThrows<IllegalArgumentException> {
                updateUserStatusService.updateUserStatus(command)
            }

        assertEquals("Tenant ID cannot be blank", exception.message)
    }

    @Test
    fun `should throw exception when user id is blank`() {
        // Given
        val command =
            UpdateUserStatusCommand(
                tenantId = "tenant-123",
                userId = "",
                newStatus = "ACTIVE",
            )

        // When & Then
        val exception =
            assertThrows<IllegalArgumentException> {
                updateUserStatusService.updateUserStatus(command)
            }

        assertEquals("User ID cannot be blank", exception.message)
    }
}

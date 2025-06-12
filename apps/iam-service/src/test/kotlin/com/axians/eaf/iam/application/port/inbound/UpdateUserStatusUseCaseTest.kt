package com.axians.eaf.iam.application.port.inbound

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class UpdateUserStatusUseCaseTest {
    @Test
    fun `should define update user status command with required fields`() {
        // Given
        val command =
            UpdateUserStatusCommand(
                tenantId = "tenant-123",
                userId = "user-123",
                newStatus = "ACTIVE",
            )

        // Then
        assertEquals("tenant-123", command.tenantId)
        assertEquals("user-123", command.userId)
        assertEquals("ACTIVE", command.newStatus)
    }

    @Test
    fun `should define update user status result with updated user details`() {
        // Given
        val result =
            UpdateUserStatusResult(
                userId = "user-123",
                tenantId = "tenant-123",
                email = "user@example.com",
                username = "testuser",
                previousStatus = "PENDING_ACTIVATION",
                newStatus = "ACTIVE",
            )

        // Then
        assertEquals("user-123", result.userId)
        assertEquals("tenant-123", result.tenantId)
        assertEquals("user@example.com", result.email)
        assertEquals("testuser", result.username)
        assertEquals("PENDING_ACTIVATION", result.previousStatus)
        assertEquals("ACTIVE", result.newStatus)
    }
}

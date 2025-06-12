package com.axians.eaf.iam.application.port.inbound

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CreateUserUseCaseTest {
    @Test
    fun `should define create user command with required fields`() {
        // Given
        val command =
            CreateUserCommand(
                tenantId = "tenant-123",
                email = "user@example.com",
                username = "testuser",
            )

        // Then
        assertEquals("tenant-123", command.tenantId)
        assertEquals("user@example.com", command.email)
        assertEquals("testuser", command.username)
    }

    @Test
    fun `should define create user result with user details`() {
        // Given
        val result =
            CreateUserResult(
                userId = "user-123",
                tenantId = "tenant-123",
                email = "user@example.com",
                username = "testuser",
                status = "PENDING_ACTIVATION",
            )

        // Then
        assertNotNull(result.userId)
        assertEquals("tenant-123", result.tenantId)
        assertEquals("user@example.com", result.email)
        assertEquals("testuser", result.username)
        assertEquals("PENDING_ACTIVATION", result.status)
    }
}

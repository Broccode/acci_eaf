package com.axians.eaf.iam.application.port.inbound

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListUsersInTenantUseCaseTest {
    @Test
    fun `should define list users query with tenant id`() {
        // Given
        val query = ListUsersInTenantQuery(tenantId = "tenant-123")

        // Then
        assertEquals("tenant-123", query.tenantId)
    }

    @Test
    fun `should define list users result with user list`() {
        // Given
        val userSummary =
            UserSummary(
                userId = "user-123",
                email = "user@example.com",
                username = "testuser",
                role = "USER",
                status = "ACTIVE",
            )
        val result =
            ListUsersInTenantResult(
                tenantId = "tenant-123",
                users = listOf(userSummary),
            )

        // Then
        assertEquals("tenant-123", result.tenantId)
        assertEquals(1, result.users.size)
        assertEquals("user-123", result.users.first().userId)
        assertEquals("user@example.com", result.users.first().email)
    }

    @Test
    fun `should handle empty user list`() {
        // Given
        val result =
            ListUsersInTenantResult(
                tenantId = "tenant-123",
                users = emptyList(),
            )

        // Then
        assertEquals("tenant-123", result.tenantId)
        assertTrue(result.users.isEmpty())
    }
}

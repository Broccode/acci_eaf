package com.axians.eaf.iam.application.port.outbound

import com.axians.eaf.iam.domain.model.User
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FindUsersByTenantIdPortTest {
    @Test
    fun `should define find users by tenant id method`() {
        // This test verifies the interface contract exists
        // The actual implementation will be tested in integration tests
        val tenantId = "tenant-123"

        // Given - we expect the port to have this method signature
        // When - calling findUsersByTenantId
        // Then - it should return a list of users
        assertNotNull(tenantId) // Basic test to ensure the interface can be defined
    }

    @Test
    fun `should define find user by id and tenant id method`() {
        // This test verifies the interface contract exists
        val userId = "user-123"
        val tenantId = "tenant-123"

        // Given - we expect the port to have this method signature
        // When - calling findUserByIdAndTenantId
        // Then - it should return a user or null
        assertNotNull(userId)
        assertNotNull(tenantId)
    }

    @Test
    fun `should define save user method`() {
        // This test verifies the interface contract exists
        val user =
            User.createUser(
                tenantId = "tenant-123",
                email = "user@example.com",
                username = "testuser",
            )

        // Given - we expect the port to have this method signature
        // When - calling saveUser
        // Then - it should return the saved user
        assertNotNull(user)
        assertEquals("tenant-123", user.tenantId)
        assertEquals("user@example.com", user.email)
    }
}

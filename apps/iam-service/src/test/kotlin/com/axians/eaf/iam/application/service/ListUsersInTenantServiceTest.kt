package com.axians.eaf.iam.application.service

import com.axians.eaf.iam.application.port.inbound.ListUsersInTenantQuery
import com.axians.eaf.iam.application.port.outbound.FindUsersByTenantIdPort
import com.axians.eaf.iam.domain.model.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListUsersInTenantServiceTest {
    private lateinit var findUsersByTenantIdPort: FindUsersByTenantIdPort
    private lateinit var listUsersInTenantService: ListUsersInTenantService

    @BeforeEach
    fun setUp() {
        findUsersByTenantIdPort = mockk()
        listUsersInTenantService = ListUsersInTenantService(findUsersByTenantIdPort)
    }

    @Test
    fun `should list users successfully when valid tenant id provided`() {
        // Given
        val query = ListUsersInTenantQuery(tenantId = "tenant-123")
        val users =
            listOf(
                User
                    .createUser(
                        tenantId = "tenant-123",
                        email = "user1@example.com",
                        username = "user1",
                    ).activate(),
                User.createTenantAdmin(
                    tenantId = "tenant-123",
                    email = "admin@example.com",
                ),
            )

        every { findUsersByTenantIdPort.findUsersByTenantId("tenant-123") } returns users

        // When
        val result = listUsersInTenantService.listUsers(query)

        // Then
        assertEquals("tenant-123", result.tenantId)
        assertEquals(2, result.users.size)

        val user1 = result.users.find { it.email == "user1@example.com" }!!
        assertEquals("user1@example.com", user1.email)
        assertEquals("user1", user1.username)
        assertEquals("USER", user1.role)
        assertEquals("ACTIVE", user1.status)

        val admin = result.users.find { it.email == "admin@example.com" }!!
        assertEquals("admin@example.com", admin.email)
        assertEquals("TENANT_ADMIN", admin.role)
        assertEquals("PENDING_ACTIVATION", admin.status)

        verify { findUsersByTenantIdPort.findUsersByTenantId("tenant-123") }
    }

    @Test
    fun `should return empty list when no users found in tenant`() {
        // Given
        val query = ListUsersInTenantQuery(tenantId = "empty-tenant")

        every { findUsersByTenantIdPort.findUsersByTenantId("empty-tenant") } returns emptyList()

        // When
        val result = listUsersInTenantService.listUsers(query)

        // Then
        assertEquals("empty-tenant", result.tenantId)
        assertTrue(result.users.isEmpty())

        verify { findUsersByTenantIdPort.findUsersByTenantId("empty-tenant") }
    }

    @Test
    fun `should throw exception when tenant id is blank`() {
        // Given
        val query = ListUsersInTenantQuery(tenantId = "")

        // When & Then
        val exception =
            assertThrows<IllegalArgumentException> {
                listUsersInTenantService.listUsers(query)
            }

        assertEquals("Tenant ID cannot be blank", exception.message)
    }
}

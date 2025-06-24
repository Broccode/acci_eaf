package com.axians.eaf.controlplane.domain.model.user

import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserTest {
    private val testTenantId = TenantId.fromString("550e8400-e29b-41d4-a716-446655440000")
    private val testEmail = "test@example.com"
    private val testFirstName = "John"
    private val testLastName = "Doe"
    private val now = Instant.now()

    @Test
    fun `should create pending user with valid data`() {
        // When
        val user =
            User.createPending(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
                now = now,
            )

        // Then
        assertEquals(testTenantId, user.tenantId)
        assertEquals(testEmail, user.email)
        assertEquals(testFirstName, user.firstName)
        assertEquals(testLastName, user.lastName)
        assertEquals(UserStatus.PENDING, user.status)
        assertTrue(user.roles.isEmpty())
        assertNull(user.lastLogin)
        assertNull(user.activatedAt)
        assertNull(user.deactivatedAt)
        assertEquals(now, user.createdAt)
        assertEquals(now, user.lastModified)
        assertEquals("John Doe", user.getFullName())
    }

    @Test
    fun `should create active user with valid data`() {
        // When
        val user =
            User.createActive(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
                now = now,
            )

        // Then
        assertEquals(UserStatus.ACTIVE, user.status)
        assertEquals(now, user.activatedAt)
        assertTrue(user.canAccess())
    }

    @Test
    fun `should normalize email to lowercase`() {
        // When
        val user =
            User.createPending(
                tenantId = testTenantId,
                email = "TEST@EXAMPLE.COM",
                firstName = testFirstName,
                lastName = testLastName,
            )

        // Then
        assertEquals("test@example.com", user.email)
    }

    @Test
    fun `should trim whitespace from name fields`() {
        // When
        val user =
            User.createPending(
                tenantId = testTenantId,
                email = testEmail,
                firstName = "  John  ",
                lastName = "  Doe  ",
            )

        // Then
        assertEquals("John", user.firstName)
        assertEquals("Doe", user.lastName)
    }

    @Test
    fun `should reject blank email`() {
        // When & Then
        assertThrows<IllegalArgumentException> {
            User.createPending(
                tenantId = testTenantId,
                email = "",
                firstName = testFirstName,
                lastName = testLastName,
            )
        }
    }

    @Test
    fun `should reject invalid email format`() {
        // When & Then
        assertThrows<IllegalArgumentException> {
            User.createPending(
                tenantId = testTenantId,
                email = "invalid-email",
                firstName = testFirstName,
                lastName = testLastName,
            )
        }
    }

    @Test
    fun `should reject blank first name`() {
        // When & Then
        assertThrows<IllegalArgumentException> {
            User.createPending(
                tenantId = testTenantId,
                email = testEmail,
                firstName = "",
                lastName = testLastName,
            )
        }
    }

    @Test
    fun `should reject blank last name`() {
        // When & Then
        assertThrows<IllegalArgumentException> {
            User.createPending(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = "",
            )
        }
    }

    @Test
    fun `should reject first name longer than 50 characters`() {
        // When & Then
        assertThrows<IllegalArgumentException> {
            User.createPending(
                tenantId = testTenantId,
                email = testEmail,
                firstName = "a".repeat(51),
                lastName = testLastName,
            )
        }
    }

    @Test
    fun `should reject last name longer than 50 characters`() {
        // When & Then
        assertThrows<IllegalArgumentException> {
            User.createPending(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = "a".repeat(51),
            )
        }
    }

    @Test
    fun `should activate pending user`() {
        // Given
        val user =
            User.createPending(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )

        // When
        val activatedUser = user.activate(now)

        // Then
        assertEquals(UserStatus.ACTIVE, activatedUser.status)
        assertEquals(now, activatedUser.activatedAt)
        assertEquals(now, activatedUser.lastModified)
        assertTrue(activatedUser.canAccess())
    }

    @Test
    fun `should activate suspended user`() {
        // Given
        val user =
            User
                .createActive(
                    tenantId = testTenantId,
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                ).suspend()

        // When
        val reactivatedUser = user.activate(now)

        // Then
        assertEquals(UserStatus.ACTIVE, reactivatedUser.status)
        assertEquals(now, reactivatedUser.lastModified)
    }

    @Test
    fun `should not activate already active user`() {
        // Given
        val user =
            User.createActive(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )

        // When & Then
        assertThrows<IllegalArgumentException> { user.activate() }
    }

    @Test
    fun `should not activate deactivated user`() {
        // Given
        val user =
            User
                .createActive(
                    tenantId = testTenantId,
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                ).deactivate()

        // When & Then
        assertThrows<IllegalArgumentException> { user.activate() }
    }

    @Test
    fun `should suspend active user`() {
        // Given
        val user =
            User.createActive(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )

        // When
        val suspendedUser = user.suspend(now)

        // Then
        assertEquals(UserStatus.SUSPENDED, suspendedUser.status)
        assertEquals(now, suspendedUser.lastModified)
        assertFalse(suspendedUser.canAccess())
    }

    @Test
    fun `should not suspend pending user`() {
        // Given
        val user =
            User.createPending(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )

        // When & Then
        assertThrows<IllegalArgumentException> { user.suspend() }
    }

    @Test
    fun `should reactivate suspended user`() {
        // Given
        val user =
            User
                .createActive(
                    tenantId = testTenantId,
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                ).suspend()

        // When
        val reactivatedUser = user.reactivate(now)

        // Then
        assertEquals(UserStatus.ACTIVE, reactivatedUser.status)
        assertEquals(now, reactivatedUser.lastModified)
        assertTrue(reactivatedUser.canAccess())
    }

    @Test
    fun `should not reactivate active user`() {
        // Given
        val user =
            User.createActive(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )

        // When & Then
        assertThrows<IllegalArgumentException> { user.reactivate() }
    }

    @Test
    fun `should deactivate any user except already deactivated`() {
        // Given
        val activeUser =
            User.createActive(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )

        // When
        val deactivatedUser = activeUser.deactivate(now)

        // Then
        assertEquals(UserStatus.DEACTIVATED, deactivatedUser.status)
        assertEquals(now, deactivatedUser.deactivatedAt)
        assertEquals(now, deactivatedUser.lastModified)
        assertFalse(deactivatedUser.canAccess())
    }

    @Test
    fun `should not deactivate already deactivated user`() {
        // Given
        val user =
            User
                .createActive(
                    tenantId = testTenantId,
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                ).deactivate()

        // When & Then
        assertThrows<IllegalArgumentException> { user.deactivate() }
    }

    @Test
    fun `should update user profile`() {
        // Given
        val user =
            User.createActive(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )

        // When
        val updatedUser = user.updateProfile("Jane", "Smith", now)

        // Then
        assertEquals("Jane", updatedUser.firstName)
        assertEquals("Smith", updatedUser.lastName)
        assertEquals("Jane Smith", updatedUser.getFullName())
        assertEquals(now, updatedUser.lastModified)
    }

    @Test
    fun `should not update profile of deactivated user`() {
        // Given
        val user =
            User
                .createActive(
                    tenantId = testTenantId,
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                ).deactivate()

        // When & Then
        assertThrows<IllegalArgumentException> { user.updateProfile("Jane", "Smith") }
    }

    @Test
    fun `should record user login`() {
        // Given
        val user =
            User.createActive(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )

        // When
        val userWithLogin = user.recordLogin(now)

        // Then
        assertEquals(now, userWithLogin.lastLogin)
        assertEquals(now, userWithLogin.lastModified)
    }

    @Test
    fun `should not record login for suspended user`() {
        // Given
        val user =
            User
                .createActive(
                    tenantId = testTenantId,
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                ).suspend()

        // When & Then
        assertThrows<IllegalArgumentException> { user.recordLogin() }
    }

    @Test
    fun `should check if user belongs to tenant`() {
        // Given
        val user =
            User.createActive(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )
        val otherTenantId = TenantId.fromString("550e8400-e29b-41d4-a716-446655440001")

        // Then
        assertTrue(user.belongsToTenant(testTenantId))
        assertFalse(user.belongsToTenant(otherTenantId))
    }

    @Test
    fun `should check user operational status`() {
        // Given
        val pendingUser = User.createPending(testTenantId, testEmail, testFirstName, testLastName)
        val activeUser = User.createActive(testTenantId, testEmail, testFirstName, testLastName)
        val suspendedUser = activeUser.suspend()
        val deactivatedUser = activeUser.deactivate()

        // Then
        assertFalse(pendingUser.isOperational())
        assertTrue(activeUser.isOperational())
        assertTrue(suspendedUser.isOperational())
        assertFalse(deactivatedUser.isOperational())
    }
}

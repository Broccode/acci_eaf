package com.axians.eaf.controlplane.domain.model.user

import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test utility for creating User instances without Axon event sourcing. This allows testing domain
 * logic in isolation by using the event sourcing handlers directly.
 */
data class UserParams(
    val tenantId: TenantId,
    val email: String,
    val firstName: String,
    val lastName: String,
    val initialRoles: Set<Role> = emptySet(),
    val now: Instant = Instant.now(),
)

object UserTestFactory {
    fun createPendingUser(params: UserParams): User {
        require(params.email.isNotBlank()) { "User email cannot be blank" }
        require(params.firstName.isNotBlank()) { "User first name cannot be blank" }
        require(params.lastName.isNotBlank()) { "User last name cannot be blank" }
        val user = User()
        val userId = UserId.generate()
        val event =
            UserCreatedEvent(
                userId = userId,
                tenantId = params.tenantId,
                email = params.email.trim().lowercase(),
                firstName = params.firstName.trim(),
                lastName = params.lastName.trim(),
                status = UserStatus.PENDING,
                initialRoles = params.initialRoles.map { it.name }.toSet(),
                timestamp = params.now,
            )
        user.on(event)
        return user
    }

    fun createActiveUser(params: UserParams): User {
        require(params.email.isNotBlank()) { "User email cannot be blank" }
        require(params.firstName.isNotBlank()) { "User first name cannot be blank" }
        require(params.lastName.isNotBlank()) { "User last name cannot be blank" }
        val user = User()
        val userId = UserId.generate()
        val createdEvent =
            UserCreatedEvent(
                userId = userId,
                tenantId = params.tenantId,
                email = params.email.trim().lowercase(),
                firstName = params.firstName.trim(),
                lastName = params.lastName.trim(),
                status = UserStatus.ACTIVE,
                initialRoles = params.initialRoles.map { it.name }.toSet(),
                timestamp = params.now,
            )
        user.on(createdEvent)
        val activatedEvent =
            UserActivatedEvent(
                userId = userId,
                tenantId = params.tenantId,
                email = params.email.trim().lowercase(),
                fullName = "${params.firstName.trim()} ${params.lastName.trim()}",
                timestamp = params.now,
            )
        user.on(activatedEvent)
        return user
    }

    /** Test helper to activate a user without using Axon events */
    fun activateUser(
        user: User,
        now: Instant = Instant.now(),
    ) {
        val event =
            UserActivatedEvent(
                userId = user.getId(),
                tenantId = user.getTenantId(),
                email = user.getEmail(),
                fullName = user.getFullName(),
                timestamp = now,
            )
        user.on(event)
    }

    /** Test helper to suspend a user without using Axon events */
    fun suspendUser(
        user: User,
        reason: String? = null,
        now: Instant = Instant.now(),
    ) {
        val event =
            UserSuspendedEvent(
                userId = user.getId(),
                tenantId = user.getTenantId(),
                email = user.getEmail(),
                fullName = user.getFullName(),
                reason = reason,
                timestamp = now,
            )
        user.on(event)
    }

    /** Test helper to reactivate a user without using Axon events */
    fun reactivateUser(
        user: User,
        now: Instant = Instant.now(),
    ) {
        val event =
            UserReactivatedEvent(
                userId = user.getId(),
                tenantId = user.getTenantId(),
                email = user.getEmail(),
                fullName = user.getFullName(),
                timestamp = now,
            )
        user.on(event)
    }

    /** Test helper to deactivate a user without using Axon events */
    fun deactivateUser(
        user: User,
        reason: String? = null,
        now: Instant = Instant.now(),
    ) {
        val event =
            UserDeactivatedEvent(
                userId = user.getId(),
                tenantId = user.getTenantId(),
                email = user.getEmail(),
                fullName = user.getFullName(),
                reason = reason,
                timestamp = now,
            )
        user.on(event)
    }

    /** Test helper to update user profile without using Axon events */
    fun updateUserProfile(
        user: User,
        newFirstName: String,
        newLastName: String,
        now: Instant = Instant.now(),
    ) {
        val event =
            UserProfileUpdatedEvent(
                userId = user.getId(),
                tenantId = user.getTenantId(),
                email = user.getEmail(),
                oldFirstName = user.getFirstName(),
                newFirstName = newFirstName.trim(),
                oldLastName = user.getLastName(),
                newLastName = newLastName.trim(),
                timestamp = now,
            )
        user.on(event)
    }

    /** Test helper to record user login without using Axon events */
    fun recordUserLogin(
        user: User,
        now: Instant = Instant.now(),
    ) {
        val event =
            UserLoginEvent(
                userId = user.getId(),
                tenantId = user.getTenantId(),
                email = user.getEmail(),
                fullName = user.getFullName(),
                timestamp = now,
            )
        user.on(event)
    }
}

class UserTest {
    private val testTenantId = TenantId.fromString("550e8400-e29b-41d4-a716-446655440000")
    private val testEmail = "test@example.com"
    private val testFirstName = "John"
    private val testLastName = "Doe"
    private val now = Instant.now()

    @Test
    fun `should create pending user with valid data`() {
        val params =
            UserParams(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
                now = now,
            )
        val user = UserTestFactory.createPendingUser(params)

        // Then
        assertEquals(testTenantId, user.getTenantId())
        assertEquals(testEmail, user.getEmail())
        assertEquals(testFirstName, user.getFirstName())
        assertEquals(testLastName, user.getLastName())
        assertEquals(UserStatus.PENDING, user.getStatus())
        assertTrue(user.getRoles().isEmpty())
        assertNull(user.getLastLogin())
        assertNull(user.getActivatedAt())
        assertNull(user.getDeactivatedAt())
        assertEquals(now, user.getCreatedAt())
        assertEquals(now, user.getLastModified())
        assertEquals("John Doe", user.getFullName())
    }

    @Test
    fun `should create active user with valid data`() {
        val params =
            UserParams(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
                now = now,
            )
        val user = UserTestFactory.createActiveUser(params)

        // Then
        assertEquals(UserStatus.ACTIVE, user.getStatus())
        assertEquals(now, user.getActivatedAt())
        assertTrue(user.canAccess())
    }

    @Test
    fun `should normalize email to lowercase`() {
        val params =
            UserParams(
                tenantId = testTenantId,
                email = "TEST@EXAMPLE.COM",
                firstName = testFirstName,
                lastName = testLastName,
            )
        val user = UserTestFactory.createPendingUser(params)

        // Then
        assertEquals("test@example.com", user.getEmail())
    }

    @Test
    fun `should trim whitespace from name fields`() {
        val params =
            UserParams(
                tenantId = testTenantId,
                email = testEmail,
                firstName = "  John  ",
                lastName = "  Doe  ",
            )
        val user = UserTestFactory.createPendingUser(params)

        // Then
        assertEquals("John", user.getFirstName())
        assertEquals("Doe", user.getLastName())
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
        val params =
            UserParams(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )
        val user = UserTestFactory.createPendingUser(params)
        UserTestFactory.activateUser(user, now)

        // Then
        assertEquals(UserStatus.ACTIVE, user.getStatus())
        assertEquals(now, user.getActivatedAt())
        assertEquals(now, user.getLastModified())
        assertTrue(user.canAccess())
    }

    @Test
    fun `should activate suspended user`() {
        val params =
            UserParams(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )
        val user = UserTestFactory.createActiveUser(params)
        UserTestFactory.suspendUser(user)
        UserTestFactory.activateUser(user, now)

        // Then
        assertEquals(UserStatus.ACTIVE, user.getStatus())
        assertEquals(now, user.getLastModified())
    }

    @Test
    fun `should not activate already active user`() {
        val params =
            UserParams(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )
        val user = UserTestFactory.createActiveUser(params)
        assertThrows<IllegalArgumentException> { user.activate() }
    }

    @Test
    fun `should not activate deactivated user`() {
        val params =
            UserParams(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )
        val user = UserTestFactory.createActiveUser(params)
        UserTestFactory.deactivateUser(user)
        assertThrows<IllegalArgumentException> { user.activate() }
    }

    @Test
    fun `should suspend active user`() {
        val params =
            UserParams(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )
        val user = UserTestFactory.createActiveUser(params)
        UserTestFactory.suspendUser(user, now = now)

        // Then
        assertEquals(UserStatus.SUSPENDED, user.getStatus())
        assertEquals(now, user.getLastModified())
        assertFalse(user.canAccess())
    }

    @Test
    fun `should not suspend pending user`() {
        val params =
            UserParams(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )
        val user = UserTestFactory.createPendingUser(params)
        assertThrows<IllegalArgumentException> { user.suspend() }
    }

    @Test
    fun `should reactivate suspended user`() {
        val params =
            UserParams(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )
        val user = UserTestFactory.createActiveUser(params)
        UserTestFactory.suspendUser(user)
        UserTestFactory.reactivateUser(user, now)

        // Then
        assertEquals(UserStatus.ACTIVE, user.getStatus())
        assertEquals(now, user.getLastModified())
        assertTrue(user.canAccess())
    }

    @Test
    fun `should not reactivate active user`() {
        val params =
            UserParams(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )
        val user = UserTestFactory.createActiveUser(params)
        assertThrows<IllegalArgumentException> { user.reactivate() }
    }

    @Test
    fun `should deactivate any user except already deactivated`() {
        val params =
            UserParams(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )
        val user = UserTestFactory.createActiveUser(params)
        UserTestFactory.deactivateUser(user, now = now)

        // Then
        assertEquals(UserStatus.DEACTIVATED, user.getStatus())
        assertEquals(now, user.getDeactivatedAt())
        assertEquals(now, user.getLastModified())
        assertFalse(user.canAccess())
    }

    @Test
    fun `should not deactivate already deactivated user`() {
        val params =
            UserParams(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )
        val user = UserTestFactory.createActiveUser(params)
        UserTestFactory.deactivateUser(user)
        assertThrows<IllegalArgumentException> { user.deactivate() }
    }

    @Test
    fun `should update user profile`() {
        val params =
            UserParams(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )
        val user = UserTestFactory.createActiveUser(params)
        UserTestFactory.updateUserProfile(user, "Jane", "Smith", now)

        // Then
        assertEquals("Jane", user.getFirstName())
        assertEquals("Smith", user.getLastName())
        assertEquals("Jane Smith", user.getFullName())
        assertEquals(now, user.getLastModified())
    }

    @Test
    fun `should not update profile of deactivated user`() {
        val params =
            UserParams(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )
        val user = UserTestFactory.createActiveUser(params)
        UserTestFactory.deactivateUser(user)
        assertThrows<IllegalStateException> { user.updateProfile("Jane", "Smith") }
    }

    @Test
    fun `should record user login`() {
        val params =
            UserParams(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )
        val user = UserTestFactory.createActiveUser(params)
        UserTestFactory.recordUserLogin(user, now)

        // Then
        assertEquals(now, user.getLastLogin())
        assertEquals(now, user.getLastModified())
    }

    @Test
    fun `should not allow suspended user to access system`() {
        val params =
            UserParams(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )
        val user = UserTestFactory.createActiveUser(params)
        UserTestFactory.suspendUser(user)
        assertFalse(user.canAccess())
        assertEquals(UserStatus.SUSPENDED, user.getStatus())
    }

    @Test
    fun `should check if user belongs to tenant`() {
        val params =
            UserParams(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )
        val user = UserTestFactory.createActiveUser(params)
        val otherTenantId = TenantId.fromString("550e8400-e29b-41d4-a716-446655440001")
        assertTrue(user.belongsToTenant(testTenantId))
        assertFalse(user.belongsToTenant(otherTenantId))
    }

    @Test
    fun `should check user operational status`() {
        val params =
            UserParams(
                tenantId = testTenantId,
                email = testEmail,
                firstName = testFirstName,
                lastName = testLastName,
            )
        val pendingUser = UserTestFactory.createPendingUser(params)
        val activeUser = UserTestFactory.createActiveUser(params)
        val suspendedUser = UserTestFactory.createActiveUser(params)
        UserTestFactory.suspendUser(suspendedUser)
        val deactivatedUser = UserTestFactory.createActiveUser(params)
        UserTestFactory.deactivateUser(deactivatedUser)
        assertFalse(pendingUser.isOperational())
        assertTrue(activeUser.isOperational())
        assertTrue(suspendedUser.isOperational())
        assertFalse(deactivatedUser.isOperational())
    }
}

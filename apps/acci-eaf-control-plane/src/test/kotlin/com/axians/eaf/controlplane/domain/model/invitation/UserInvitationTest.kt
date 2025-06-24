package com.axians.eaf.controlplane.domain.model.invitation

import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.UserInvitation
import com.axians.eaf.controlplane.domain.model.user.UserInvitationId
import com.axians.eaf.controlplane.domain.model.user.UserInvitationStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Comprehensive unit tests for UserInvitation domain model. */
class UserInvitationTest {
    @Test
    fun `should create user invitation successfully`() {
        // Given
        val invitationId = UserInvitationId.generate()
        val email = "test@example.com"
        val tenantId = TenantId.generate()
        val roleIds = setOf("admin-role", "user-role")
        val invitedBy = "admin@example.com"
        val expiresIn = Duration.ofDays(7)

        // When
        val invitation =
            UserInvitation.create(
                id = invitationId,
                email = email,
                tenantId = tenantId,
                roleIds = roleIds,
                invitedBy = invitedBy,
                expiresIn = expiresIn,
            )

        // Then
        assertEquals(invitationId, invitation.id)
        assertEquals(email, invitation.email)
        assertEquals(tenantId, invitation.tenantId)
        assertEquals(roleIds, invitation.roleIds)
        assertEquals(invitedBy, invitation.invitedBy)
        assertEquals(UserInvitationStatus.PENDING, invitation.status)
        assertNotNull(invitation.token)
        assertTrue(invitation.token.length >= 32)
        assertNotNull(invitation.createdAt)
        assertNotNull(invitation.expiresAt)
        assertTrue(invitation.expiresAt.isAfter(invitation.createdAt))
    }

    @Test
    fun `should accept invitation successfully`() {
        // Given
        val invitation =
            UserInvitation.create(
                id = UserInvitationId.generate(),
                email = "test@example.com",
                tenantId = TenantId.generate(),
                roleIds = setOf("user-role"),
                invitedBy = "admin@example.com",
                expiresIn = Duration.ofDays(7),
            )

        // When
        val acceptedInvitation = invitation.accept()

        // Then
        assertEquals(UserInvitationStatus.ACCEPTED, acceptedInvitation.status)
        assertNotNull(acceptedInvitation.acceptedAt)
    }

    @Test
    fun `should reject invitation successfully`() {
        // Given
        val invitation =
            UserInvitation.create(
                id = UserInvitationId.generate(),
                email = "test@example.com",
                tenantId = TenantId.generate(),
                roleIds = setOf("user-role"),
                invitedBy = "admin@example.com",
                expiresIn = Duration.ofDays(7),
            )

        // When
        val rejectedInvitation = invitation.reject()

        // Then
        assertEquals(UserInvitationStatus.REJECTED, rejectedInvitation.status)
    }

    @Test
    fun `should check if invitation is pending correctly`() {
        // Given
        val pendingInvitation =
            UserInvitation.create(
                id = UserInvitationId.generate(),
                email = "test@example.com",
                tenantId = TenantId.generate(),
                roleIds = setOf("user-role"),
                invitedBy = "admin@example.com",
                expiresIn = Duration.ofDays(7),
            )
        val acceptedInvitation = pendingInvitation.accept()

        // When & Then
        assertTrue(pendingInvitation.isPending())
        assertFalse(acceptedInvitation.isPending())
    }

    @Test
    fun `should reject invitation creation with invalid email`() {
        // Given & When & Then
        assertThrows<IllegalArgumentException> {
            UserInvitation.create(
                id = UserInvitationId.generate(),
                email = "invalid-email",
                tenantId = TenantId.generate(),
                roleIds = setOf("user-role"),
                invitedBy = "admin@example.com",
                expiresIn = Duration.ofDays(7),
            )
        }
    }

    @Test
    fun `should reject invitation creation with empty role ids`() {
        // Given & When & Then
        assertThrows<IllegalArgumentException> {
            UserInvitation.create(
                id = UserInvitationId.generate(),
                email = "test@example.com",
                tenantId = TenantId.generate(),
                roleIds = emptySet(),
                invitedBy = "admin@example.com",
                expiresIn = Duration.ofDays(7),
            )
        }
    }

    @Test
    fun `should handle multiple role assignments`() {
        // Given
        val roleIds = setOf("admin-role", "user-role", "manager-role")

        // When
        val invitation =
            UserInvitation.create(
                id = UserInvitationId.generate(),
                email = "test@example.com",
                tenantId = TenantId.generate(),
                roleIds = roleIds,
                invitedBy = "admin@example.com",
                expiresIn = Duration.ofDays(7),
            )

        // Then
        assertEquals(3, invitation.roleIds.size)
        assertEquals(roleIds, invitation.roleIds)
    }

    @Test
    fun `should generate unique tokens for different invitations`() {
        // Given & When
        val invitations =
            (1..5).map {
                UserInvitation.create(
                    id = UserInvitationId.generate(),
                    email = "test$it@example.com",
                    tenantId = TenantId.generate(),
                    roleIds = setOf("user-role"),
                    invitedBy = "admin@example.com",
                    expiresIn = Duration.ofDays(7),
                )
            }

        // Then
        val tokens = invitations.map { it.token }.toSet()
        assertEquals(5, tokens.size) // All tokens should be unique
        tokens.forEach { token ->
            assertTrue(token.length >= 32) // Each token should be secure
        }
    }
}

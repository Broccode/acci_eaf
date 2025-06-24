package com.axians.eaf.controlplane.domain.model.user

import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64

/**
 * Domain entity representing a user invitation in the system.
 *
 * User invitations manage the process of inviting new users to join a tenant with specific roles.
 * They include secure tokens and expiry logic.
 */
data class UserInvitation(
    val id: UserInvitationId,
    val email: String,
    val tenantId: TenantId,
    val roleIds: Set<String>,
    val invitedBy: String,
    val status: UserInvitationStatus,
    val token: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val updatedAt: Instant,
    val acceptedAt: Instant? = null,
) {
    companion object {
        private val secureRandom = SecureRandom()

        /**
         * Creates a new user invitation.
         *
         * @param id Unique identifier for the invitation
         * @param email Email address of the invitee
         * @param tenantId Tenant the user is being invited to
         * @param roleIds Role IDs to assign to the user
         * @param invitedBy Email of the person sending the invitation
         * @param expiresIn Duration until the invitation expires
         * @return A new UserInvitation instance
         */
        fun create(
            id: UserInvitationId,
            email: String,
            tenantId: TenantId,
            roleIds: Set<String>,
            invitedBy: String,
            expiresIn: Duration,
        ): UserInvitation {
            validateEmailFormat(email)
            require(roleIds.isNotEmpty()) { "User invitation must have at least one role" }
            require(invitedBy.isNotBlank()) { "Invited by cannot be blank" }
            require(expiresIn.toMillis() > 0) { "Expiry duration must be positive" }

            val now = Instant.now()
            return UserInvitation(
                id = id,
                email = email,
                tenantId = tenantId,
                roleIds = roleIds,
                invitedBy = invitedBy,
                status = UserInvitationStatus.PENDING,
                token = generateSecureToken(),
                createdAt = now,
                expiresAt = now.plus(expiresIn),
                updatedAt = now,
                acceptedAt = null,
            )
        }

        /** Generates a cryptographically secure token for the invitation. */
        private fun generateSecureToken(): String {
            val tokenBytes = ByteArray(32)
            secureRandom.nextBytes(tokenBytes)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes)
        }

        /** Validates email format using a simple regex. */
        private fun validateEmailFormat(email: String) {
            require(email.isNotBlank()) { "Email cannot be blank" }
            val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
            require(emailRegex.matches(email)) { "Invalid email format: $email" }
        }
    }

    /**
     * Accepts this invitation.
     *
     * @return A new UserInvitation instance with accepted status
     * @throws IllegalStateException if the invitation cannot be accepted
     */
    fun accept(): UserInvitation {
        require(canBeAccepted()) {
            "Cannot accept invitation: status=$status, expired=${isExpired()}"
        }

        return copy(
            status = UserInvitationStatus.ACCEPTED,
            acceptedAt = Instant.now(),
            updatedAt = Instant.now(),
        )
    }

    /**
     * Rejects this invitation.
     *
     * @return A new UserInvitation instance with rejected status
     * @throws IllegalStateException if the invitation is not in pending status
     */
    fun reject(): UserInvitation {
        require(isPending()) { "Can only reject pending invitations" }

        return copy(status = UserInvitationStatus.REJECTED, updatedAt = Instant.now())
    }

    /**
     * Revokes this invitation.
     *
     * @return A new UserInvitation instance with revoked status
     */
    fun revoke(): UserInvitation = copy(status = UserInvitationStatus.REVOKED, updatedAt = Instant.now())

    /** Checks if this invitation is expired. */
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    /** Checks if this invitation is in pending status. */
    fun isPending(): Boolean = status == UserInvitationStatus.PENDING

    /** Checks if this invitation can be accepted. */
    fun canBeAccepted(): Boolean = isPending() && !isExpired()

    /** Checks equality based on invitation ID. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserInvitation) return false
        return id == other.id
    }

    /** Returns hash code based on invitation ID. */
    override fun hashCode(): Int = id.hashCode()
}

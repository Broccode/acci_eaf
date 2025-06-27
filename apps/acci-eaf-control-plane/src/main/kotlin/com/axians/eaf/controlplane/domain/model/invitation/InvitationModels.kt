package com.axians.eaf.controlplane.domain.model.invitation

import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.RoleId
import com.axians.eaf.controlplane.domain.model.user.UserId
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID

/** Value object representing a unique invitation identifier. */
@JvmInline
value class InvitationId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Invitation ID cannot be blank" }
    }

    companion object {
        fun generate(): InvitationId = InvitationId(UUID.randomUUID().toString())

        fun fromString(value: String): InvitationId = InvitationId(value)
    }

    override fun toString(): String = value
}

/** Value object representing a secure invitation token. */
@JvmInline
value class SecureToken(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Secure token cannot be blank" }
        require(value.length >= 32) { "Secure token must be at least 32 characters" }
    }

    companion object {
        private val secureRandom = SecureRandom()
        private val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        /** Generate a cryptographically secure random token. */
        fun generate(length: Int = 64): SecureToken {
            require(length >= 32) { "Token length must be at least 32 characters" }

            val token = (1..length).map { chars[secureRandom.nextInt(chars.length)] }.joinToString("")

            return SecureToken(token)
        }

        fun fromString(value: String): SecureToken = SecureToken(value)
    }

    override fun toString(): String = value
}

/** Represents the current status of an invitation. */
enum class InvitationStatus {
    /** Invitation has been sent and is awaiting acceptance. */
    PENDING,

    /** Invitation has been accepted and user account created. */
    ACCEPTED,

    /** Invitation has expired and is no longer valid. */
    EXPIRED,

    /** Invitation has been manually cancelled. */
    CANCELLED,
    ;

    /** Check if this status indicates the invitation is still active. */
    fun isActive(): Boolean = this == PENDING

    /** Check if this status indicates the invitation is completed. */
    fun isCompleted(): Boolean = this == ACCEPTED

    /** Check if this status indicates the invitation is no longer valid. */
    fun isInvalid(): Boolean = this in setOf(EXPIRED, CANCELLED)
}

/** Domain events for invitation lifecycle. */
sealed class InvitationEvent {
    abstract val invitationId: InvitationId
    abstract val tenantId: TenantId
    abstract val timestamp: Instant

    data class InvitationSent(
        override val invitationId: InvitationId,
        override val tenantId: TenantId,
        val email: String,
        val invitedBy: UserId,
        val roles: Set<RoleId>,
        override val timestamp: Instant = Instant.now(),
    ) : InvitationEvent()

    data class InvitationAccepted(
        override val invitationId: InvitationId,
        override val tenantId: TenantId,
        val email: String,
        val createdUserId: UserId,
        override val timestamp: Instant = Instant.now(),
    ) : InvitationEvent()

    data class InvitationExpired(
        override val invitationId: InvitationId,
        override val tenantId: TenantId,
        val email: String,
        override val timestamp: Instant = Instant.now(),
    ) : InvitationEvent()

    data class InvitationCancelled(
        override val invitationId: InvitationId,
        override val tenantId: TenantId,
        val email: String,
        val cancelledBy: UserId,
        val reason: String,
        override val timestamp: Instant = Instant.now(),
    ) : InvitationEvent()

    data class InvitationResent(
        override val invitationId: InvitationId,
        override val tenantId: TenantId,
        val email: String,
        val resentBy: UserId,
        override val timestamp: Instant = Instant.now(),
    ) : InvitationEvent()
}

/** Represents a user invitation aggregate root. */
data class Invitation(
    val id: InvitationId,
    val tenantId: TenantId,
    val email: String,
    val firstName: String,
    val lastName: String,
    val roles: Set<RoleId>,
    val invitedBy: UserId,
    val token: SecureToken,
    val status: InvitationStatus,
    val expiresAt: Instant,
    val createdAt: Instant,
    val acceptedAt: Instant? = null,
    val customMessage: String? = null,
) {
    init {
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(email.contains("@")) { "Email must be valid" }
        require(firstName.isNotBlank()) { "First name cannot be blank" }
        require(lastName.isNotBlank()) { "Last name cannot be blank" }
        require(roles.isNotEmpty()) { "At least one role must be assigned" }
        require(expiresAt.isAfter(createdAt)) { "Expiration date must be after creation date" }
        require(status != InvitationStatus.ACCEPTED || acceptedAt != null) {
            "Accepted invitations must have an acceptance timestamp"
        }
    }

    /** Check if this invitation is currently valid and can be accepted. */
    fun isValid(): Boolean = status.isActive() && !isExpired()

    /** Check if this invitation has expired. */
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    /** Check if this invitation can be cancelled. */
    fun canBeCancelled(): Boolean = status.isActive() && !isExpired()

    /** Check if this invitation can be resent. */
    fun canBeResent(): Boolean = status.isActive()

    /** Accept this invitation, returning updated invitation with new status. */
    fun accept(): Invitation {
        require(isValid()) { "Cannot accept invalid or expired invitation" }

        return copy(status = InvitationStatus.ACCEPTED, acceptedAt = Instant.now())
    }

    /** Cancel this invitation, returning updated invitation with new status. */
    fun cancel(): Invitation {
        require(canBeCancelled()) { "Cannot cancel this invitation" }

        return copy(status = InvitationStatus.CANCELLED)
    }

    /** Mark this invitation as expired. */
    fun expire(): Invitation = copy(status = InvitationStatus.EXPIRED)

    /** Create a new invitation with refreshed token and extended expiration. */
    fun resend(newExpirationDays: Int = 7): Invitation {
        require(canBeResent()) { "Cannot resend this invitation" }

        return copy(
            token = SecureToken.generate(),
            expiresAt = Instant.now().plusSeconds(newExpirationDays * 24 * 60 * 60L),
            status = InvitationStatus.PENDING,
        )
    }

    /** Get the full name of the invited user. */
    fun getFullName(): String = "$firstName $lastName"

    companion object {
        /** Create a new invitation with generated token and expiration. */
        fun create(
            tenantId: TenantId,
            email: String,
            firstName: String,
            lastName: String,
            roles: Set<RoleId>,
            invitedBy: UserId,
            expirationDays: Int = 7,
            customMessage: String? = null,
        ): Invitation {
            val now = Instant.now()
            return Invitation(
                id = InvitationId.generate(),
                tenantId = tenantId,
                email = email,
                firstName = firstName,
                lastName = lastName,
                roles = roles,
                invitedBy = invitedBy,
                token = SecureToken.generate(),
                status = InvitationStatus.PENDING,
                expiresAt = now.plusSeconds(expirationDays * 24 * 60 * 60L),
                createdAt = now,
                customMessage = customMessage,
            )
        }
    }
}

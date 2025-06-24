package com.axians.eaf.controlplane.domain.model.user

import java.util.UUID

/**
 * Value object representing a unique identifier for user invitations.
 *
 * This ID is used to uniquely identify invitation instances within the system and provides
 * referential integrity for invitation-related operations.
 */
@JvmInline
value class UserInvitationId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "UserInvitation ID cannot be blank" }
    }

    companion object {
        /** Generates a new random UserInvitationId. */
        fun generate(): UserInvitationId = UserInvitationId(UUID.randomUUID().toString())

        /** Creates a UserInvitationId from a string representation. */
        fun fromString(value: String): UserInvitationId = UserInvitationId(value)
    }

    /** Returns the string representation of this invitation ID. */
    override fun toString(): String = value
}

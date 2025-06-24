package com.axians.eaf.controlplane.domain.model.user

/**
 * Enum representing the various states of a user invitation.
 *
 * This enum tracks the lifecycle of user invitations from creation through to completion or
 * cancellation.
 */
enum class UserInvitationStatus {
    /** The invitation has been sent and is awaiting a response. */
    PENDING,

    /** The invitation has been accepted by the recipient. */
    ACCEPTED,

    /** The invitation has been rejected by the recipient. */
    REJECTED,

    /** The invitation has been revoked by an administrator. */
    REVOKED,

    /** The invitation has expired and is no longer valid. */
    EXPIRED,
}

package com.axians.eaf.controlplane.domain.model.user

/**
 * Represents the current status of a user in the system.
 * - PENDING: User has been created but hasn't activated their account
 * - ACTIVE: User is operational and can access services
 * - SUSPENDED: User access is temporarily disabled
 * - DEACTIVATED: User account is permanently disabled
 */
enum class UserStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    DEACTIVATED,
    ;

    /** Checks if the user can access the system. */
    fun canAccess(): Boolean = this == ACTIVE

    /** Checks if the user can be activated. */
    fun canBeActivated(): Boolean = this in setOf(PENDING, SUSPENDED)

    /** Checks if the user can be suspended. */
    fun canBeSuspended(): Boolean = this == ACTIVE

    /** Checks if the user can be reactivated. */
    fun canBeReactivated(): Boolean = this == SUSPENDED

    /** Checks if the user is operational (excludes pending state). */
    fun isOperational(): Boolean = this in setOf(ACTIVE, SUSPENDED)
}

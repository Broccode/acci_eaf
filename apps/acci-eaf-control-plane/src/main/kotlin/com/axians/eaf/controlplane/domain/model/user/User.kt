package com.axians.eaf.controlplane.domain.model.user

import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import java.time.Instant

/**
 * User aggregate root representing an individual user in the EAF platform. Encapsulates all
 * user-related business logic and invariants.
 */
data class User(
    val id: UserId,
    val tenantId: TenantId,
    val email: String,
    val firstName: String,
    val lastName: String,
    val status: UserStatus,
    val roles: Set<Role>,
    val lastLogin: Instant?,
    val createdAt: Instant,
    val lastModified: Instant,
    val activatedAt: Instant? = null,
    val deactivatedAt: Instant? = null,
) {
    init {
        require(email.isNotBlank()) { "User email cannot be blank" }
        require(isValidEmail(email)) { "User email must be valid: $email" }
        require(firstName.isNotBlank()) { "User first name cannot be blank" }
        require(lastName.isNotBlank()) { "User last name cannot be blank" }
        require(firstName.length <= 50) {
            "First name cannot exceed 50 characters: ${firstName.length}"
        }
        require(lastName.length <= 50) { "Last name cannot exceed 50 characters: ${lastName.length}" }
        require(lastModified >= createdAt) { "Last modified cannot be before created date" }
        require(status != UserStatus.ACTIVE || activatedAt != null) {
            "Active users must have an activation date"
        }
        require(status != UserStatus.DEACTIVATED || deactivatedAt != null) {
            "Deactivated users must have a deactivation date"
        }
        require(lastLogin == null || activatedAt != null) {
            "Users cannot have login history before activation"
        }
    }

    companion object {
        /** Creates a new pending user that needs to activate their account. */
        fun createPending(
            tenantId: TenantId,
            email: String,
            firstName: String,
            lastName: String,
            initialRoles: Set<Role> = emptySet(),
            now: Instant = Instant.now(),
        ): User =
            User(
                id = UserId.generate(),
                tenantId = tenantId,
                email = email.trim().lowercase(),
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                status = UserStatus.PENDING,
                roles = initialRoles,
                lastLogin = null,
                createdAt = now,
                lastModified = now,
            )

        /** Creates a new active user (for admin-created users). */
        fun createActive(
            tenantId: TenantId,
            email: String,
            firstName: String,
            lastName: String,
            initialRoles: Set<Role> = emptySet(),
            now: Instant = Instant.now(),
        ): User =
            User(
                id = UserId.generate(),
                tenantId = tenantId,
                email = email.trim().lowercase(),
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                status = UserStatus.ACTIVE,
                roles = initialRoles,
                lastLogin = null,
                createdAt = now,
                lastModified = now,
                activatedAt = now,
            )

        private fun isValidEmail(email: String): Boolean {
            val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
            return email.matches(emailRegex)
        }
    }

    /** Activates a pending user account. */
    fun activate(now: Instant = Instant.now()): User {
        require(status.canBeActivated()) { "Cannot activate user in status: $status" }

        return copy(
            status = UserStatus.ACTIVE,
            activatedAt = now,
            lastModified = now,
        )
    }

    /** Suspends an active user account. */
    fun suspend(now: Instant = Instant.now()): User {
        require(status.canBeSuspended()) { "Cannot suspend user in status: $status" }

        return copy(
            status = UserStatus.SUSPENDED,
            lastModified = now,
        )
    }

    /** Reactivates a suspended user account. */
    fun reactivate(now: Instant = Instant.now()): User {
        require(status.canBeReactivated()) { "Cannot reactivate user in status: $status" }

        return copy(
            status = UserStatus.ACTIVE,
            lastModified = now,
        )
    }

    /** Permanently deactivates a user account. */
    fun deactivate(now: Instant = Instant.now()): User {
        require(status != UserStatus.DEACTIVATED) { "User is already deactivated" }

        return copy(
            status = UserStatus.DEACTIVATED,
            deactivatedAt = now,
            lastModified = now,
        )
    }

    /** Updates user profile information. */
    fun updateProfile(
        newFirstName: String,
        newLastName: String,
        now: Instant = Instant.now(),
    ): User {
        require(status != UserStatus.DEACTIVATED) { "Cannot update deactivated user profile" }

        return copy(
            firstName = newFirstName.trim(),
            lastName = newLastName.trim(),
            lastModified = now,
        )
    }

    /** Assigns a role to the user. */
    fun assignRole(
        role: Role,
        now: Instant = Instant.now(),
    ): User {
        require(status != UserStatus.DEACTIVATED) { "Cannot assign roles to deactivated users" }
        require(!hasRole(role)) { "User already has role: ${role.name}" }

        return copy(
            roles = roles + role,
            lastModified = now,
        )
    }

    /** Removes a role from the user. */
    fun removeRole(
        role: Role,
        now: Instant = Instant.now(),
    ): User {
        require(status != UserStatus.DEACTIVATED) { "Cannot remove roles from deactivated users" }
        require(hasRole(role)) { "User does not have role: ${role.name}" }

        return copy(
            roles = roles - role,
            lastModified = now,
        )
    }

    /** Records a successful login. */
    fun recordLogin(now: Instant = Instant.now()): User {
        require(status.canAccess()) { "User cannot login in status: $status" }

        return copy(
            lastLogin = now,
            lastModified = now,
        )
    }

    /** Checks if the user has a specific role. */
    fun hasRole(role: Role): Boolean = roles.contains(role)

    /** Checks if the user has a role with a specific name. */
    fun hasRole(roleName: String): Boolean = roles.any { it.name == roleName }

    /** Checks if the user has a specific permission. */
    fun hasPermission(permission: Permission): Boolean = roles.any { it.hasPermission(permission) }

    /** Checks if the user has permission for a specific resource and action. */
    fun hasPermission(
        resource: String,
        action: String,
    ): Boolean = roles.any { it.hasPermission(resource, action) }

    /** Gets all permissions from all roles. */
    fun getAllPermissions(): Set<Permission> = roles.flatMap { it.permissions }.toSet()

    /** Checks if the user can access the system. */
    fun canAccess(): Boolean = status.canAccess()

    /** Checks if the user is operational (activated but not necessarily active). */
    fun isOperational(): Boolean = status.isOperational()

    /** Gets the user's full name. */
    fun getFullName(): String = "$firstName $lastName"

    /** Gets the user's display name (for UI purposes). */
    fun getDisplayName(): String = getFullName()

    /** Checks if the user belongs to a specific tenant. */
    fun belongsToTenant(targetTenantId: TenantId): Boolean = tenantId == targetTenantId
}

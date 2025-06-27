package com.axians.eaf.iam.domain.model

import java.time.Instant
import java.util.UUID

/**
 * User aggregate root representing a user within a specific tenant in the EAF system. This entity
 * contains pure business logic and has no infrastructure dependencies.
 */
data class User(
    val userId: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val email: String,
    val username: String? = null,
    val passwordHash: String? = null,
    val role: UserRole,
    val status: UserStatus = UserStatus.PENDING_ACTIVATION,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val lastLogin: Instant? = null,
) {
    init {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(tenantId.isNotBlank()) { "Tenant ID cannot be blank" }
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(isValidEmail(email)) { "Email must be valid" }
        username?.let { require(it.isNotBlank()) { "Username cannot be blank when provided" } }
    }

    companion object {
        private fun isValidEmail(email: String): Boolean = email.contains("@") && email.length <= 320

        /** Factory method to create an initial Tenant Administrator. */
        fun createTenantAdmin(
            tenantId: String,
            email: String,
        ): User {
            require(tenantId.isNotBlank()) { "Tenant ID cannot be blank" }
            require(email.isNotBlank()) { "Email cannot be blank" }
            require(isValidEmail(email)) { "Email must be valid" }

            return User(
                tenantId = tenantId,
                email = email.trim().lowercase(),
                role = UserRole.TENANT_ADMIN,
                status = UserStatus.PENDING_ACTIVATION,
            )
        }

        /**
         * Factory method to create a SuperAdmin user. SuperAdmins have system-wide privileges and are
         * associated with a tenant.
         */
        fun createSuperAdmin(
            tenantId: String,
            email: String,
        ): User {
            require(tenantId.isNotBlank()) { "Tenant ID cannot be blank" }
            require(email.isNotBlank()) { "Email cannot be blank" }
            require(isValidEmail(email)) { "Email must be valid" }

            return User(
                tenantId = tenantId,
                email = email.trim().lowercase(),
                role = UserRole.SUPER_ADMIN,
                status = UserStatus.ACTIVE, // SuperAdmins are created active
            )
        }

        /** Factory method to create a regular user within a tenant. */
        fun createUser(
            tenantId: String,
            email: String,
            username: String? = null,
        ): User {
            require(tenantId.isNotBlank()) { "Tenant ID cannot be blank" }
            require(email.isNotBlank()) { "Email cannot be blank" }
            require(isValidEmail(email)) { "Email must be valid" }

            return User(
                tenantId = tenantId,
                email = email.trim().lowercase(),
                username = username?.trim(),
                role = UserRole.USER,
                status = UserStatus.PENDING_ACTIVATION,
            )
        }
    }

    /** Domain method to activate the user account. */
    fun activate(): User =
        copy(
            status = UserStatus.ACTIVE,
            updatedAt = Instant.now(),
        )

    /** Domain method to deactivate the user account. */
    fun deactivate(): User =
        copy(
            status = UserStatus.INACTIVE,
            updatedAt = Instant.now(),
        )

    /** Domain method to suspend the user account. */
    fun suspend(): User =
        copy(
            status = UserStatus.SUSPENDED,
            updatedAt = Instant.now(),
        )

    /** Domain method to set the password hash. */
    fun setPasswordHash(passwordHash: String): User {
        require(passwordHash.isNotBlank()) { "Password hash cannot be blank" }
        require(passwordHash.length >= 60) { "Password hash appears to be invalid (too short)" }

        return copy(
            passwordHash = passwordHash,
            updatedAt = Instant.now(),
        )
    }

    /** Domain method to record successful login. */
    fun recordLogin(): User =
        copy(
            lastLogin = Instant.now(),
            updatedAt = Instant.now(),
        )

    /** Domain method to check if user is active. */
    fun isActive(): Boolean = status == UserStatus.ACTIVE

    /** Domain method to check if user is a tenant administrator. */
    fun isTenantAdmin(): Boolean = role == UserRole.TENANT_ADMIN

    /** Domain method to check if user is a super administrator. */
    fun isSuperAdmin(): Boolean = role == UserRole.SUPER_ADMIN
}

/** Enum representing the possible roles of a user. */
enum class UserRole {
    SUPER_ADMIN,
    TENANT_ADMIN,
    USER,
}

/** Enum representing the possible states of a user. */
enum class UserStatus {
    PENDING_ACTIVATION,
    ACTIVE,
    INACTIVE,
    SUSPENDED,
}

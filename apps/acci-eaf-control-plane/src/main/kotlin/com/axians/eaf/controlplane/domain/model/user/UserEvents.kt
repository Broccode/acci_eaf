package com.axians.eaf.controlplane.domain.model.user

import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import java.time.Instant

/**
 * Domain events for user lifecycle operations. These events are published when user state changes
 * occur.
 */
sealed class UserEvent {
    abstract val userId: UserId
    abstract val tenantId: TenantId
    abstract val timestamp: Instant
}

/** Published when a new user is created. */
data class UserCreatedEvent(
    override val userId: UserId,
    override val tenantId: TenantId,
    val email: String,
    val firstName: String,
    val lastName: String,
    val status: UserStatus,
    val initialRoles: Set<String>,
    override val timestamp: Instant = Instant.now(),
) : UserEvent()

/** Published when a pending user activates their account. */
data class UserActivatedEvent(
    override val userId: UserId,
    override val tenantId: TenantId,
    val email: String,
    val fullName: String,
    override val timestamp: Instant = Instant.now(),
) : UserEvent()

/** Published when a user is suspended. */
data class UserSuspendedEvent(
    override val userId: UserId,
    override val tenantId: TenantId,
    val email: String,
    val fullName: String,
    val reason: String? = null,
    override val timestamp: Instant = Instant.now(),
) : UserEvent()

/** Published when a suspended user is reactivated. */
data class UserReactivatedEvent(
    override val userId: UserId,
    override val tenantId: TenantId,
    val email: String,
    val fullName: String,
    override val timestamp: Instant = Instant.now(),
) : UserEvent()

/** Published when a user is permanently deactivated. */
data class UserDeactivatedEvent(
    override val userId: UserId,
    override val tenantId: TenantId,
    val email: String,
    val fullName: String,
    val reason: String? = null,
    override val timestamp: Instant = Instant.now(),
) : UserEvent()

/** Published when user profile information is updated. */
data class UserProfileUpdatedEvent(
    override val userId: UserId,
    override val tenantId: TenantId,
    val email: String,
    val oldFirstName: String,
    val newFirstName: String,
    val oldLastName: String,
    val newLastName: String,
    override val timestamp: Instant = Instant.now(),
) : UserEvent()

/** Published when a role is assigned to a user. */
data class UserRoleAssignedEvent(
    override val userId: UserId,
    override val tenantId: TenantId,
    val email: String,
    val fullName: String,
    val roleName: String,
    val roleScope: RoleScope,
    override val timestamp: Instant = Instant.now(),
) : UserEvent()

/** Published when a role is removed from a user. */
data class UserRoleRemovedEvent(
    override val userId: UserId,
    override val tenantId: TenantId,
    val email: String,
    val fullName: String,
    val roleName: String,
    val roleScope: RoleScope,
    override val timestamp: Instant = Instant.now(),
) : UserEvent()

/** Published when a user successfully logs in. */
data class UserLoginEvent(
    override val userId: UserId,
    override val tenantId: TenantId,
    val email: String,
    val fullName: String,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    override val timestamp: Instant = Instant.now(),
) : UserEvent()

/** Published when a password reset is requested. */
data class UserPasswordResetRequestedEvent(
    override val userId: UserId,
    override val tenantId: TenantId,
    val email: String,
    val fullName: String,
    val resetToken: String,
    override val timestamp: Instant = Instant.now(),
) : UserEvent()

/** Published when a password reset is completed. */
data class UserPasswordResetCompletedEvent(
    override val userId: UserId,
    override val tenantId: TenantId,
    val email: String,
    val fullName: String,
    override val timestamp: Instant = Instant.now(),
) : UserEvent()

package com.axians.eaf.controlplane.domain.model.user

import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import java.time.Instant

/**
 * User aggregate root representing an individual user in the EAF platform. Encapsulates all
 * user-related business logic and invariants.
 *
 * This is an Axon Framework aggregate that uses event sourcing to track all state changes through
 * domain events. All business operations should result in domain events being applied.
 */
class User {
    @AggregateIdentifier private var id: UserId? = null
    private var tenantId: TenantId? = null
    private var email: String = ""
    private var firstName: String = ""
    private var lastName: String = ""
    private var status: UserStatus = UserStatus.PENDING
    private var roles: Set<Role> = emptySet()
    private var lastLogin: Instant? = null
    private var createdAt: Instant? = null
    private var lastModified: Instant? = null
    private var activatedAt: Instant? = null
    private var deactivatedAt: Instant? = null

    // Default constructor required by Axon
    constructor()

    companion object {
        private const val MAX_NAME_LENGTH = 50

        /** Creates a new pending user that needs to activate their account. */
        fun createPending(
            tenantId: TenantId,
            email: String,
            firstName: String,
            lastName: String,
            initialRoles: Set<Role> = emptySet(),
            now: Instant = Instant.now(),
        ): User {
            validateUserCreation(email, firstName, lastName)

            val userId = UserId.generate()
            val user = User()

            val event =
                UserCreatedEvent(
                    userId = userId,
                    tenantId = tenantId,
                    email = email.trim().lowercase(),
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    status = UserStatus.PENDING,
                    initialRoles = initialRoles.map { it.name }.toSet(),
                    timestamp = now,
                )

            AggregateLifecycle.apply(event)
            return user
        }

        /** Creates a new active user (for admin-created users). */
        fun createActive(
            tenantId: TenantId,
            email: String,
            firstName: String,
            lastName: String,
            initialRoles: Set<Role> = emptySet(),
            now: Instant = Instant.now(),
        ): User {
            validateUserCreation(email, firstName, lastName)

            val userId = UserId.generate()
            val user = User()

            val event =
                UserCreatedEvent(
                    userId = userId,
                    tenantId = tenantId,
                    email = email.trim().lowercase(),
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    status = UserStatus.ACTIVE,
                    initialRoles = initialRoles.map { it.name }.toSet(),
                    timestamp = now,
                )

            AggregateLifecycle.apply(event)

            // If created as active, also apply activation event
            val activationEvent =
                UserActivatedEvent(
                    userId = userId,
                    tenantId = tenantId,
                    email = email.trim().lowercase(),
                    fullName = "${firstName.trim()} ${lastName.trim()}",
                    timestamp = now,
                )

            AggregateLifecycle.apply(activationEvent)
            return user
        }

        private fun validateUserCreation(
            email: String,
            firstName: String,
            lastName: String,
        ) {
            require(email.isNotBlank()) { "User email cannot be blank" }
            require(isValidEmail(email)) { "User email must be valid: $email" }
            require(firstName.isNotBlank()) { "User first name cannot be blank" }
            require(lastName.isNotBlank()) { "User last name cannot be blank" }
            require(firstName.length <= MAX_NAME_LENGTH) {
                "First name cannot exceed $MAX_NAME_LENGTH characters: ${firstName.length}"
            }
            require(lastName.length <= MAX_NAME_LENGTH) {
                "Last name cannot exceed $MAX_NAME_LENGTH characters: ${lastName.length}"
            }
        }

        private fun isValidEmail(email: String): Boolean {
            val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
            return email.matches(emailRegex)
        }
    }

    /** Activates a pending user account. */
    fun activate(now: Instant = Instant.now()) {
        require(status.canBeActivated()) { "Cannot activate user in status: $status" }

        val event =
            UserActivatedEvent(
                userId = getId(),
                tenantId = getTenantId(),
                email = email,
                fullName = getFullName(),
                timestamp = now,
            )

        AggregateLifecycle.apply(event)
    }

    /** Suspends an active user account. */
    fun suspend(
        reason: String? = null,
        now: Instant = Instant.now(),
    ) {
        require(status.canBeSuspended()) { "Cannot suspend user in status: $status" }

        val event =
            UserSuspendedEvent(
                userId = getId(),
                tenantId = getTenantId(),
                email = email,
                fullName = getFullName(),
                reason = reason,
                timestamp = now,
            )

        AggregateLifecycle.apply(event)
    }

    /** Reactivates a suspended user account. */
    fun reactivate(now: Instant = Instant.now()) {
        require(status.canBeReactivated()) { "Cannot reactivate user in status: $status" }

        val event =
            UserReactivatedEvent(
                userId = getId(),
                tenantId = getTenantId(),
                email = email,
                fullName = getFullName(),
                timestamp = now,
            )

        AggregateLifecycle.apply(event)
    }

    /** Permanently deactivates a user account. */
    fun deactivate(
        reason: String? = null,
        now: Instant = Instant.now(),
    ) {
        require(status != UserStatus.DEACTIVATED) { "User is already deactivated" }

        val event =
            UserDeactivatedEvent(
                userId = getId(),
                tenantId = getTenantId(),
                email = email,
                fullName = getFullName(),
                reason = reason,
                timestamp = now,
            )

        AggregateLifecycle.apply(event)
    }

    /** Updates user profile information. */
    fun updateProfile(
        newFirstName: String,
        newLastName: String,
        now: Instant = Instant.now(),
    ) {
        check(status != UserStatus.DEACTIVATED) { "Cannot update deactivated user profile" }
        require(newFirstName.isNotBlank()) { "First name cannot be blank" }
        require(newLastName.isNotBlank()) { "Last name cannot be blank" }
        require(newFirstName.length <= MAX_NAME_LENGTH) {
            "First name cannot exceed $MAX_NAME_LENGTH characters"
        }
        require(newLastName.length <= MAX_NAME_LENGTH) {
            "Last name cannot exceed $MAX_NAME_LENGTH characters"
        }

        val event =
            UserProfileUpdatedEvent(
                userId = getId(),
                tenantId = getTenantId(),
                email = email,
                oldFirstName = firstName,
                newFirstName = newFirstName.trim(),
                oldLastName = lastName,
                newLastName = newLastName.trim(),
                timestamp = now,
            )

        AggregateLifecycle.apply(event)
    }

    /** Assigns a role to the user. */
    fun assignRole(
        role: Role,
        now: Instant = Instant.now(),
    ) {
        check(status != UserStatus.DEACTIVATED) { "Cannot assign roles to deactivated users" }
        check(!hasRole(role)) { "User already has role: ${role.name}" }

        val event =
            UserRoleAssignedEvent(
                userId = getId(),
                tenantId = getTenantId(),
                email = email,
                fullName = getFullName(),
                roleName = role.name,
                roleScope = role.scope,
                timestamp = now,
            )

        AggregateLifecycle.apply(event)
    }

    /** Removes a role from the user. */
    fun removeRole(
        role: Role,
        now: Instant = Instant.now(),
    ) {
        check(status != UserStatus.DEACTIVATED) { "Cannot remove roles from deactivated users" }
        check(hasRole(role)) { "User does not have role: ${role.name}" }

        val event =
            UserRoleRemovedEvent(
                userId = getId(),
                tenantId = getTenantId(),
                email = email,
                fullName = getFullName(),
                roleName = role.name,
                roleScope = role.scope,
                timestamp = now,
            )

        AggregateLifecycle.apply(event)
    }

    /** Records a successful login for the user. */
    fun recordLogin(now: Instant = Instant.now()) {
        val event =
            UserLoginEvent(
                userId = getId(),
                tenantId = getTenantId(),
                email = email,
                fullName = getFullName(),
                timestamp = now,
            )
        AggregateLifecycle.apply(event)
    }

    // Event Sourcing Handlers - These methods are called by Axon to rebuild aggregate state

    @EventSourcingHandler
    fun on(event: UserCreatedEvent) {
        this.id = event.userId
        this.tenantId = event.tenantId
        this.email = event.email
        this.firstName = event.firstName
        this.lastName = event.lastName
        this.status = event.status
        // Role reconstruction requires repository access, handled at application layer
        this.roles = emptySet() // Roles will be loaded separately from role assignments
        this.createdAt = event.timestamp
        this.lastModified = event.timestamp
    }

    @EventSourcingHandler
    fun on(event: UserActivatedEvent) {
        this.status = UserStatus.ACTIVE
        this.activatedAt = event.timestamp
        this.lastModified = event.timestamp
    }

    @EventSourcingHandler
    fun on(event: UserSuspendedEvent) {
        this.status = UserStatus.SUSPENDED
        this.lastModified = event.timestamp
    }

    @EventSourcingHandler
    fun on(event: UserReactivatedEvent) {
        this.status = UserStatus.ACTIVE
        this.lastModified = event.timestamp
    }

    @EventSourcingHandler
    fun on(event: UserDeactivatedEvent) {
        this.status = UserStatus.DEACTIVATED
        this.deactivatedAt = event.timestamp
        this.lastModified = event.timestamp
    }

    @EventSourcingHandler
    fun on(event: UserProfileUpdatedEvent) {
        this.firstName = event.newFirstName
        this.lastName = event.newLastName
        this.lastModified = event.timestamp
    }

    @EventSourcingHandler
    fun on(event: UserRoleAssignedEvent) {
        // Role reconstruction requires repository access, handled at application layer
        // Role objects cannot be reconstructed in the aggregate without dependencies
        this.lastModified = event.timestamp
    }

    @EventSourcingHandler
    fun on(event: UserRoleRemovedEvent) {
        // Role reconstruction requires repository access, handled at application layer
        // Role objects cannot be reconstructed in the aggregate without dependencies
        this.lastModified = event.timestamp
    }

    @EventSourcingHandler
    fun on(event: UserLoginEvent) {
        this.lastLogin = event.timestamp
        this.lastModified = event.timestamp
    }

    // Query methods (read-only, no state changes)

    fun getId(): UserId = id ?: error("User ID not initialized")

    fun getTenantId(): TenantId = tenantId ?: error("Tenant ID not initialized")

    fun getEmail(): String = email

    fun getFirstName(): String = firstName

    fun getLastName(): String = lastName

    fun getStatus(): UserStatus = status

    fun getRoles(): Set<Role> = roles

    fun getLastLogin(): Instant? = lastLogin

    fun getCreatedAt(): Instant = createdAt ?: error("Created at not initialized")

    fun getLastModified(): Instant = lastModified ?: error("Last modified not initialized")

    fun getActivatedAt(): Instant? = activatedAt

    fun getDeactivatedAt(): Instant? = deactivatedAt

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
    fun belongsToTenant(targetTenantId: TenantId): Boolean = getTenantId() == targetTenantId
}

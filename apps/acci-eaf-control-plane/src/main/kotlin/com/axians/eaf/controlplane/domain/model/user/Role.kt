package com.axians.eaf.controlplane.domain.model.user

import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import java.time.Instant
import java.util.UUID

/** Value object representing a unique role identifier. */
@JvmInline
value class RoleId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Role ID cannot be blank" }
    }

    companion object {
        fun generate(): RoleId = RoleId(UUID.randomUUID().toString())

        fun fromString(value: String): RoleId = RoleId(value)
    }

    override fun toString(): String = value
}

/** Value object representing a unique permission identifier. */
@JvmInline
value class PermissionId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Permission ID cannot be blank" }
    }

    companion object {
        fun generate(): PermissionId = PermissionId(UUID.randomUUID().toString())

        fun fromString(value: String): PermissionId = PermissionId(value)
    }

    override fun toString(): String = value
}

/** Represents the scope of a role - either platform-wide or tenant-specific. */
enum class RoleScope {
    PLATFORM, // Role applies across the entire platform
    TENANT, // Role applies only within a specific tenant
}

/** Represents a permission that can be granted to roles. */
data class Permission(
    val id: PermissionId,
    val name: String,
    val description: String,
    val resource: String,
    val action: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        /** Creates a new permission with the specified details. */
        fun create(
            id: PermissionId,
            name: String,
            description: String,
            resource: String,
            action: String,
        ): Permission {
            require(name.isNotBlank()) { "Permission name cannot be blank" }
            require(description.isNotBlank()) { "Permission description cannot be blank" }
            require(resource.isNotBlank()) { "Permission resource cannot be blank" }
            require(action.isNotBlank()) { "Permission action cannot be blank" }

            val now = Instant.now()
            return Permission(
                id = id,
                name = name,
                description = description,
                resource = resource,
                action = action,
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    /** Updates the description of this permission. */
    fun updateDescription(newDescription: String): Permission {
        require(newDescription.isNotBlank()) { "Permission description cannot be blank" }

        return copy(description = newDescription, updatedAt = Instant.now())
    }

    /** Returns the permission in "resource:action" format. */
    fun toPermissionString(): String = "$resource:$action"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Permission) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

/** Represents a role that groups permissions together. */
data class Role(
    val id: RoleId,
    val name: String,
    val description: String,
    val permissions: Set<Permission>,
    val scope: RoleScope,
    val tenantId: TenantId? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        /** Creates a new platform-scoped role. */
        fun createPlatformRole(
            id: RoleId,
            name: String,
            description: String,
        ): Role {
            require(name.isNotBlank()) { "Role name cannot be blank" }
            require(description.isNotBlank()) { "Role description cannot be blank" }

            val now = Instant.now()
            return Role(
                id = id,
                name = name,
                description = description,
                permissions = emptySet(),
                scope = RoleScope.PLATFORM,
                tenantId = null,
                createdAt = now,
                updatedAt = now,
            )
        }

        /** Creates a new tenant-scoped role. */
        fun createTenantRole(
            id: RoleId,
            name: String,
            description: String,
            tenantId: TenantId,
        ): Role {
            require(name.isNotBlank()) { "Role name cannot be blank" }
            require(description.isNotBlank()) { "Role description cannot be blank" }

            val now = Instant.now()
            return Role(
                id = id,
                name = name,
                description = description,
                permissions = emptySet(),
                scope = RoleScope.TENANT,
                tenantId = tenantId,
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    /** Checks if this role has a specific permission. */
    fun hasPermission(permission: Permission): Boolean = permissions.contains(permission)

    /** Checks if this role has a permission for a specific resource and action. */
    fun hasPermission(
        resource: String,
        action: String,
    ): Boolean = permissions.any { it.resource == resource && it.action == action }

    /** Adds a permission to this role. */
    fun addPermission(permission: Permission): Role =
        if (permissions.contains(permission)) {
            this
        } else {
            copy(permissions = permissions + permission, updatedAt = Instant.now())
        }

    /** Removes a permission from this role. */
    fun removePermission(permission: Permission): Role =
        copy(permissions = permissions - permission, updatedAt = Instant.now())

    /** Updates the details of this role. */
    fun updateDetails(
        name: String,
        description: String,
    ): Role {
        require(name.isNotBlank()) { "Role name cannot be blank" }
        require(description.isNotBlank()) { "Role description cannot be blank" }

        return copy(name = name, description = description, updatedAt = Instant.now())
    }

    /** Creates a copy of this role with an additional permission. */
    fun withPermission(permission: Permission): Role = copy(permissions = permissions + permission)

    /** Creates a copy of this role without a specific permission. */
    fun withoutPermission(permission: Permission): Role = copy(permissions = permissions - permission)

    /** Checks if this is a platform-level role. */
    fun isPlatformRole(): Boolean = scope == RoleScope.PLATFORM

    /** Checks if this is a platform-scoped role. */
    fun isPlatformScoped(): Boolean = scope == RoleScope.PLATFORM

    /** Checks if this is a tenant-specific role. */
    fun isTenantRole(): Boolean = scope == RoleScope.TENANT

    /** Checks if this is a tenant-scoped role. */
    fun isTenantScoped(): Boolean = scope == RoleScope.TENANT

    /** Checks if this role belongs to a specific tenant. */
    fun belongsToTenant(tenantId: TenantId): Boolean = this.tenantId == tenantId
}

package com.axians.eaf.controlplane.domain.port

import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.Permission
import com.axians.eaf.controlplane.domain.model.user.PermissionId
import com.axians.eaf.controlplane.domain.model.user.Role
import com.axians.eaf.controlplane.domain.model.user.RoleId
import com.axians.eaf.controlplane.domain.model.user.RoleScope

/**
 * Repository port for role and permission aggregate persistence. This is a domain contract that
 * will be implemented by infrastructure adapters.
 */
interface RoleRepository {
    // Role operations

    /**
     * Saves a role to the repository. For new roles, this will create them. For existing ones, this
     * will update them.
     */
    suspend fun saveRole(role: Role): Role

    /** Finds a role by its unique identifier. */
    suspend fun findRoleById(roleId: RoleId): Role?

    /** Finds roles by scope and optional tenant ID. */
    suspend fun findRolesByScope(
        scope: RoleScope,
        tenantId: TenantId? = null,
    ): List<Role>

    /**
     * Finds all roles for a specific tenant (includes both tenant-scoped and platform-scoped roles).
     */
    suspend fun findRolesByTenantId(tenantId: TenantId): List<Role>

    /** Finds a role by name within a scope and tenant. */
    suspend fun findRoleByName(
        name: String,
        scope: RoleScope,
        tenantId: TenantId? = null,
    ): Role?

    /** Checks if a role exists with the given ID. */
    suspend fun existsRoleById(roleId: RoleId): Boolean

    /** Checks if a role exists with the given name within a scope and tenant. */
    suspend fun existsRoleByName(
        name: String,
        scope: RoleScope,
        tenantId: TenantId? = null,
    ): Boolean

    /** Deletes a role by ID. Returns true if the role was found and deleted. */
    suspend fun deleteRoleById(roleId: RoleId): Boolean

    // Permission operations

    /** Saves a permission to the repository. */
    suspend fun savePermission(permission: Permission): Permission

    /** Finds a permission by its unique identifier. */
    suspend fun findPermissionById(permissionId: PermissionId): Permission?

    /** Finds all available permissions. */
    suspend fun findAllPermissions(): List<Permission>

    /** Finds permissions by resource. */
    suspend fun findPermissionsByResource(resource: String): List<Permission>

    /** Checks if a permission exists with the given ID. */
    suspend fun existsPermissionById(permissionId: PermissionId): Boolean

    /** Checks if a permission exists with the given resource and action. */
    suspend fun existsPermissionByResourceAndAction(
        resource: String,
        action: String,
    ): Boolean

    /** Deletes a permission by ID. Returns true if the permission was found and deleted. */
    suspend fun deletePermissionById(permissionId: PermissionId): Boolean
}

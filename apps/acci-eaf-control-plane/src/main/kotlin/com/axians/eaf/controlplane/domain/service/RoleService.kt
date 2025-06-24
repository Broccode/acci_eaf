package com.axians.eaf.controlplane.domain.service

import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.Permission
import com.axians.eaf.controlplane.domain.model.user.PermissionId
import com.axians.eaf.controlplane.domain.model.user.Role
import com.axians.eaf.controlplane.domain.model.user.RoleId
import com.axians.eaf.controlplane.domain.model.user.RoleScope
import com.axians.eaf.controlplane.domain.port.RoleRepository
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Domain service for role and permission management operations. Encapsulates business logic for
 * role lifecycle management and permission assignment.
 */
@Service
class RoleService(
    private val roleRepository: RoleRepository,
) {
    // Role Management Operations

    /** Creates a new role with the specified details. */
    suspend fun createRole(
        name: String,
        description: String,
        scope: RoleScope,
        tenantId: String? = null,
        permissions: Set<String> = emptySet(),
    ): CreateRoleResult {
        val parsedTenantId = tenantId?.let { TenantId.fromString(it) }

        // Validate scope and tenant ID relationship
        if (scope == RoleScope.TENANT && parsedTenantId == null) {
            return CreateRoleResult.failure("Tenant-scoped roles must have a tenant ID")
        }
        if (scope == RoleScope.PLATFORM && parsedTenantId != null) {
            return CreateRoleResult.failure("Platform-scoped roles cannot have a tenant ID")
        }

        // Check if role name already exists in the same scope/tenant
        if (roleRepository.existsRoleByName(name, scope, parsedTenantId)) {
            val scopeDescription =
                if (scope == RoleScope.PLATFORM) "platform" else "tenant $tenantId"
            return CreateRoleResult.failure(
                "Role with name '$name' already exists in $scopeDescription",
            )
        }

        // Resolve permissions
        val resolvedPermissions = mutableSetOf<Permission>()
        for (permissionId in permissions) {
            val permission =
                roleRepository.findPermissionById(PermissionId.fromString(permissionId))
                    ?: return CreateRoleResult.failure(
                        "Permission not found: $permissionId",
                    )
            resolvedPermissions.add(permission)
        }

        // Create role
        val role =
            if (scope == RoleScope.PLATFORM) {
                Role.createPlatformRole(
                    id = RoleId.fromString(UUID.randomUUID().toString()),
                    name = name,
                    description = description,
                )
            } else {
                Role.createTenantRole(
                    id = RoleId.fromString(UUID.randomUUID().toString()),
                    name = name,
                    description = description,
                    tenantId = parsedTenantId!!,
                )
            }.let { baseRole ->
                // Add permissions to the role
                resolvedPermissions.fold(baseRole) { acc, permission ->
                    acc.addPermission(permission)
                }
            }

        val savedRole = roleRepository.saveRole(role)
        return CreateRoleResult.success(savedRole)
    }

    /** Updates an existing role's details. */
    suspend fun updateRole(
        roleId: String,
        name: String,
        description: String,
    ): UpdateRoleResult {
        val role =
            roleRepository.findRoleById(RoleId.fromString(roleId))
                ?: return UpdateRoleResult.notFound("Role not found: $roleId")

        // Check if the new name conflicts with another role in the same scope/tenant
        if (name != role.name && roleRepository.existsRoleByName(name, role.scope, role.tenantId)) {
            val scopeDescription =
                if (role.scope == RoleScope.PLATFORM) "platform" else "tenant ${role.tenantId}"
            return UpdateRoleResult.failure(
                "Role with name '$name' already exists in $scopeDescription",
            )
        }

        val updatedRole = role.copy(name = name, description = description)
        val savedRole = roleRepository.saveRole(updatedRole)

        return UpdateRoleResult.success(savedRole)
    }

    /** Deletes a role by ID. */
    suspend fun deleteRole(roleId: String): DeleteRoleResult {
        val role =
            roleRepository.findRoleById(RoleId.fromString(roleId))
                ?: return DeleteRoleResult.notFound("Role not found: $roleId")

        val deleted = roleRepository.deleteRoleById(role.id)
        return if (deleted) {
            DeleteRoleResult.success("Role '${role.name}' deleted successfully")
        } else {
            DeleteRoleResult.failure("Failed to delete role: $roleId")
        }
    }

    /** Assigns a permission to a role. */
    suspend fun assignPermissionToRole(
        roleId: String,
        permissionId: String,
    ): RolePermissionResult {
        val role =
            roleRepository.findRoleById(RoleId.fromString(roleId))
                ?: return RolePermissionResult.notFound("Role not found: $roleId")

        val permission =
            roleRepository.findPermissionById(PermissionId.fromString(permissionId))
                ?: return RolePermissionResult.notFound(
                    "Permission not found: $permissionId",
                )

        if (role.hasPermission(permission)) {
            return RolePermissionResult.failure(
                "Role '${role.name}' already has permission '${permission.name}'",
            )
        }

        val updatedRole = role.withPermission(permission)
        roleRepository.saveRole(updatedRole)

        return RolePermissionResult.success(
            "Permission '${permission.name}' assigned to role '${role.name}'",
        )
    }

    /** Removes a permission from a role. */
    suspend fun removePermissionFromRole(
        roleId: String,
        permissionId: String,
    ): RolePermissionResult {
        val role =
            roleRepository.findRoleById(RoleId.fromString(roleId))
                ?: return RolePermissionResult.notFound("Role not found: $roleId")

        val permission =
            roleRepository.findPermissionById(PermissionId.fromString(permissionId))
                ?: return RolePermissionResult.notFound(
                    "Permission not found: $permissionId",
                )

        if (!role.hasPermission(permission)) {
            return RolePermissionResult.failure(
                "Role '${role.name}' does not have permission '${permission.name}'",
            )
        }

        val updatedRole = role.withoutPermission(permission)
        roleRepository.saveRole(updatedRole)

        return RolePermissionResult.success(
            "Permission '${permission.name}' removed from role '${role.name}'",
        )
    }

    /** Lists roles by scope and optional tenant ID. */
    suspend fun listRoles(
        scope: RoleScope,
        tenantId: String? = null,
    ): List<Role> {
        val parsedTenantId = tenantId?.let { TenantId.fromString(it) }
        return roleRepository.findRolesByScope(scope, parsedTenantId)
    }

    /** Gets all roles available for a specific tenant (platform + tenant-scoped). */
    suspend fun getRolesForTenant(tenantId: String): List<Role> =
        roleRepository.findRolesByTenantId(TenantId.fromString(tenantId))

    /** Gets a role by ID. */
    suspend fun getRoleById(roleId: String): Role? = roleRepository.findRoleById(RoleId.fromString(roleId))

    // Permission Management Operations

    /** Creates a new permission. */
    suspend fun createPermission(
        name: String,
        description: String,
        resource: String,
        action: String,
    ): CreatePermissionResult {
        // Check if permission already exists
        if (roleRepository.existsPermissionByResourceAndAction(resource, action)) {
            return CreatePermissionResult.failure(
                "Permission for resource '$resource' and action '$action' already exists",
            )
        }

        val permission =
            Permission.create(
                id = PermissionId.fromString(UUID.randomUUID().toString()),
                name = name,
                description = description,
                resource = resource,
                action = action,
            )

        val savedPermission = roleRepository.savePermission(permission)
        return CreatePermissionResult.success(savedPermission)
    }

    /** Gets all available permissions. */
    suspend fun getAllPermissions(): List<Permission> = roleRepository.findAllPermissions()

    /** Gets permissions by resource. */
    suspend fun getPermissionsByResource(resource: String): List<Permission> =
        roleRepository.findPermissionsByResource(resource)

    /** Gets a permission by ID. */
    suspend fun getPermissionById(permissionId: String): Permission? =
        roleRepository.findPermissionById(PermissionId.fromString(permissionId))
}

// Result types for role operations
sealed class CreateRoleResult {
    data class Success(
        val role: Role,
    ) : CreateRoleResult()

    data class Failure(
        val message: String,
    ) : CreateRoleResult()

    companion object {
        fun success(role: Role): CreateRoleResult = Success(role)

        fun failure(message: String): CreateRoleResult = Failure(message)
    }
}

sealed class UpdateRoleResult {
    data class Success(
        val role: Role,
    ) : UpdateRoleResult()

    data class NotFound(
        val message: String,
    ) : UpdateRoleResult()

    data class Failure(
        val message: String,
    ) : UpdateRoleResult()

    companion object {
        fun success(role: Role): UpdateRoleResult = Success(role)

        fun notFound(message: String): UpdateRoleResult = NotFound(message)

        fun failure(message: String): UpdateRoleResult = Failure(message)
    }
}

sealed class DeleteRoleResult {
    data class Success(
        val message: String,
    ) : DeleteRoleResult()

    data class NotFound(
        val message: String,
    ) : DeleteRoleResult()

    data class Failure(
        val message: String,
    ) : DeleteRoleResult()

    companion object {
        fun success(message: String): DeleteRoleResult = Success(message)

        fun notFound(message: String): DeleteRoleResult = NotFound(message)

        fun failure(message: String): DeleteRoleResult = Failure(message)
    }
}

sealed class RolePermissionResult {
    data class Success(
        val message: String,
    ) : RolePermissionResult()

    data class NotFound(
        val message: String,
    ) : RolePermissionResult()

    data class Failure(
        val message: String,
    ) : RolePermissionResult()

    companion object {
        fun success(message: String): RolePermissionResult = Success(message)

        fun notFound(message: String): RolePermissionResult = NotFound(message)

        fun failure(message: String): RolePermissionResult = Failure(message)
    }
}

// Result types for permission operations
sealed class CreatePermissionResult {
    data class Success(
        val permission: Permission,
    ) : CreatePermissionResult()

    data class Failure(
        val message: String,
    ) : CreatePermissionResult()

    companion object {
        fun success(permission: Permission): CreatePermissionResult = Success(permission)

        fun failure(message: String): CreatePermissionResult = Failure(message)
    }
}

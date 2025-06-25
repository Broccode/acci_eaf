package com.axians.eaf.controlplane.infrastructure.adapter.input

import com.axians.eaf.controlplane.application.dto.role.CreatePermissionRequest
import com.axians.eaf.controlplane.application.dto.role.CreatePermissionResponse
import com.axians.eaf.controlplane.application.dto.role.CreateRoleRequest
import com.axians.eaf.controlplane.application.dto.role.CreateRoleResponse
import com.axians.eaf.controlplane.application.dto.role.DeleteRoleResponse
import com.axians.eaf.controlplane.application.dto.role.PermissionDto
import com.axians.eaf.controlplane.application.dto.role.RoleDto
import com.axians.eaf.controlplane.application.dto.role.RolePermissionResponse
import com.axians.eaf.controlplane.application.dto.role.UpdateRoleRequest
import com.axians.eaf.controlplane.application.dto.role.UpdateRoleResponse
import com.axians.eaf.controlplane.domain.model.user.RoleScope
import com.axians.eaf.controlplane.domain.service.CreatePermissionResult
import com.axians.eaf.controlplane.domain.service.CreateRoleResult
import com.axians.eaf.controlplane.domain.service.DeleteRoleResult
import com.axians.eaf.controlplane.domain.service.RolePermissionResult
import com.axians.eaf.controlplane.domain.service.RoleService
import com.axians.eaf.controlplane.domain.service.UpdateRoleResult
import com.vaadin.flow.server.auth.AnonymousAllowed
import jakarta.annotation.security.RolesAllowed
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.validation.annotation.Validated

/**
 * Hilla endpoint for role management operations. Provides type-safe access to role lifecycle and
 * permission management.
 *
 * FIXME: Temporarily disabled due to KotlinNullabilityPlugin crash in Vaadin 24.8.0 See:
 * https://github.com/vaadin/hilla/issues/3443 Remove comment from @Endpoint when Vaadin/Hilla ships
 * the fix.
 */

// @Endpoint
@Service
@Validated
// @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN", "TENANT_ADMIN")
class RoleManagementEndpoint(
        private val roleService: RoleService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RoleManagementEndpoint::class.java)
    }

    // Role Management Operations

    /** Creates a new role with the specified configuration. */
    fun createRole(
            @Valid request: CreateRoleRequest,
    ): CreateRoleResponse {
        logger.info("Creating role: {} with scope: {}", request.name, request.scope)

        return runBlocking {
            try {
                val result =
                        roleService.createRole(
                                name = request.name,
                                description = request.description,
                                scope = request.scope,
                                tenantId = request.tenantId,
                                permissions = request.getUniquePermissions(),
                        )

                when (result) {
                    is CreateRoleResult.Success -> {
                        logger.info("Role created successfully: {}", result.role.id.value)
                        CreateRoleResponse.success(result.role)
                    }
                    is CreateRoleResult.Failure -> {
                        logger.warn("Failed to create role: {}", result.message)
                        CreateRoleResponse.failure(result.message)
                    }
                }
            } catch (exception: Exception) {
                logger.error("Error creating role: ${request.name}", exception)
                CreateRoleResponse.failure("Failed to create role: ${exception.message}")
            }
        }
    }

    /** Updates an existing role's details. */
    fun updateRole(
            roleId: String,
            @Valid request: UpdateRoleRequest,
    ): UpdateRoleResponse? {
        logger.info("Updating role: {}", roleId)

        return runBlocking {
            try {
                val result =
                        roleService.updateRole(
                                roleId = roleId,
                                name = request.name,
                                description = request.description,
                        )

                when (result) {
                    is UpdateRoleResult.Success -> {
                        logger.info("Role updated successfully: {}", roleId)
                        UpdateRoleResponse.success(result.role)
                    }
                    is UpdateRoleResult.NotFound -> {
                        logger.warn("Role not found for update: {}", roleId)
                        null
                    }
                    is UpdateRoleResult.Failure -> {
                        logger.warn("Failed to update role {}: {}", roleId, result.message)
                        null
                    }
                }
            } catch (exception: Exception) {
                logger.error("Error updating role: $roleId", exception)
                null
            }
        }
    }

    /** Deletes a role by ID. */
    @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN") // Only platform admins can delete roles
    fun deleteRole(roleId: String): DeleteRoleResponse? {
        logger.info("Deleting role: {}", roleId)

        return runBlocking {
            try {
                val role = roleService.getRoleById(roleId)
                if (role == null) {
                    logger.warn("Role not found for deletion: {}", roleId)
                    return@runBlocking null
                }

                val result = roleService.deleteRole(roleId)

                when (result) {
                    is DeleteRoleResult.Success -> {
                        logger.info("Role deleted successfully: {}", roleId)
                        DeleteRoleResponse.success(roleId, role.name)
                    }
                    is DeleteRoleResult.NotFound -> {
                        logger.warn("Role not found for deletion: {}", roleId)
                        null
                    }
                    is DeleteRoleResult.Failure -> {
                        logger.warn("Failed to delete role {}: {}", roleId, result.message)
                        null
                    }
                }
            } catch (exception: Exception) {
                logger.error("Error deleting role: $roleId", exception)
                null
            }
        }
    }

    /** Assigns a permission to a role. */
    fun assignPermission(
            roleId: String,
            permissionId: String,
    ): RolePermissionResponse? {
        logger.info("Assigning permission {} to role {}", permissionId, roleId)

        return runBlocking {
            try {
                val role = roleService.getRoleById(roleId)
                val permission = roleService.getPermissionById(permissionId)

                if (role == null || permission == null) {
                    logger.warn(
                            "Role or permission not found: roleId={}, permissionId={}",
                            roleId,
                            permissionId,
                    )
                    return@runBlocking null
                }

                val result = roleService.assignPermissionToRole(roleId, permissionId)

                when (result) {
                    is RolePermissionResult.Success -> {
                        logger.info(
                                "Permission assigned successfully: {} to {}",
                                permissionId,
                                roleId,
                        )
                        RolePermissionResponse.assigned(
                                roleId,
                                role.name,
                                permissionId,
                                permission.name,
                        )
                    }
                    is RolePermissionResult.NotFound -> {
                        logger.warn("Role or permission not found: {}", result.message)
                        null
                    }
                    is RolePermissionResult.Failure -> {
                        logger.warn("Failed to assign permission: {}", result.message)
                        null
                    }
                }
            } catch (exception: Exception) {
                logger.error("Error assigning permission $permissionId to role $roleId", exception)
                null
            }
        }
    }

    /** Removes a permission from a role. */
    fun removePermission(
            roleId: String,
            permissionId: String,
    ): RolePermissionResponse? {
        logger.info("Removing permission {} from role {}", permissionId, roleId)

        return runBlocking {
            try {
                val role = roleService.getRoleById(roleId)
                val permission = roleService.getPermissionById(permissionId)

                if (role == null || permission == null) {
                    logger.warn(
                            "Role or permission not found: roleId={}, permissionId={}",
                            roleId,
                            permissionId,
                    )
                    return@runBlocking null
                }

                val result = roleService.removePermissionFromRole(roleId, permissionId)

                when (result) {
                    is RolePermissionResult.Success -> {
                        logger.info(
                                "Permission removed successfully: {} from {}",
                                permissionId,
                                roleId,
                        )
                        RolePermissionResponse.removed(
                                roleId,
                                role.name,
                                permissionId,
                                permission.name,
                        )
                    }
                    is RolePermissionResult.NotFound -> {
                        logger.warn("Role or permission not found: {}", result.message)
                        null
                    }
                    is RolePermissionResult.Failure -> {
                        logger.warn("Failed to remove permission: {}", result.message)
                        null
                    }
                }
            } catch (exception: Exception) {
                logger.error("Error removing permission $permissionId from role $roleId", exception)
                null
            }
        }
    }

    /** Gets a role by ID. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun getRole(roleId: String): RoleDto? {
        logger.debug("Retrieving role: {}", roleId)

        return runBlocking {
            try {
                val role = roleService.getRoleById(roleId)
                if (role != null) {
                    logger.debug("Role retrieved: {}", roleId)
                    RoleDto.fromDomain(role)
                } else {
                    logger.warn("Role not found: {}", roleId)
                    null
                }
            } catch (exception: Exception) {
                logger.error("Error retrieving role: $roleId", exception)
                null
            }
        }
    }

    /** Lists roles by scope and optional tenant ID. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun listRoles(
            scope: RoleScope,
            tenantId: String? = null,
    ): List<RoleDto> {
        logger.debug("Listing roles with scope: {} and tenantId: {}", scope, tenantId)

        return runBlocking {
            try {
                val roles = roleService.listRoles(scope, tenantId)
                logger.debug("Retrieved {} roles", roles.size)
                roles.map { RoleDto.fromDomain(it) }
            } catch (exception: Exception) {
                logger.error("Error listing roles", exception)
                emptyList()
            }
        }
    }

    /** Gets all roles available for a specific tenant (platform + tenant-scoped). */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun getRolesForTenant(tenantId: String): List<RoleDto> {
        logger.debug("Getting roles for tenant: {}", tenantId)

        return runBlocking {
            try {
                val roles = roleService.getRolesForTenant(tenantId)
                logger.debug("Retrieved {} roles for tenant {}", roles.size, tenantId)
                roles.map { RoleDto.fromDomain(it) }
            } catch (exception: Exception) {
                logger.error("Error getting roles for tenant: $tenantId", exception)
                emptyList()
            }
        }
    }

    // Permission Management Operations

    /** Creates a new permission. */
    @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN") // Only platform admins can create permissions
    fun createPermission(
            @Valid request: CreatePermissionRequest,
    ): CreatePermissionResponse {
        logger.info("Creating permission: {} for resource: {}", request.name, request.resource)

        return runBlocking {
            try {
                val result =
                        roleService.createPermission(
                                name = request.name,
                                description = request.description,
                                resource = request.resource,
                                action = request.action,
                        )

                when (result) {
                    is CreatePermissionResult.Success -> {
                        logger.info(
                                "Permission created successfully: {}",
                                result.permission.id.value,
                        )
                        CreatePermissionResponse.success(result.permission)
                    }
                    is CreatePermissionResult.Failure -> {
                        logger.warn("Failed to create permission: {}", result.message)
                        CreatePermissionResponse.failure(result.message)
                    }
                }
            } catch (exception: Exception) {
                logger.error("Error creating permission: ${request.name}", exception)
                CreatePermissionResponse.failure(
                        "Failed to create permission: ${exception.message}",
                )
            }
        }
    }

    /** Gets all available permissions. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun listPermissions(): List<PermissionDto> {
        logger.debug("Listing all permissions")

        return runBlocking {
            try {
                val permissions = roleService.getAllPermissions()
                logger.debug("Retrieved {} permissions", permissions.size)
                permissions.map { PermissionDto.fromDomain(it) }
            } catch (exception: Exception) {
                logger.error("Error listing permissions", exception)
                emptyList()
            }
        }
    }

    /** Gets permissions by resource. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun getPermissionsByResource(resource: String): List<PermissionDto> {
        logger.debug("Getting permissions for resource: {}", resource)

        return runBlocking {
            try {
                val permissions = roleService.getPermissionsByResource(resource)
                logger.debug("Retrieved {} permissions for resource {}", permissions.size, resource)
                permissions.map { PermissionDto.fromDomain(it) }
            } catch (exception: Exception) {
                logger.error("Error getting permissions for resource: $resource", exception)
                emptyList()
            }
        }
    }

    /** Gets a permission by ID. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun getPermission(permissionId: String): PermissionDto? {
        logger.debug("Retrieving permission: {}", permissionId)

        return runBlocking {
            try {
                val permission = roleService.getPermissionById(permissionId)
                if (permission != null) {
                    logger.debug("Permission retrieved: {}", permissionId)
                    PermissionDto.fromDomain(permission)
                } else {
                    logger.warn("Permission not found: {}", permissionId)
                    null
                }
            } catch (exception: Exception) {
                logger.error("Error retrieving permission: $permissionId", exception)
                null
            }
        }
    }
}

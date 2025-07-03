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
import com.axians.eaf.controlplane.infrastructure.adapter.input.common.EndpointExceptionHandler
import com.axians.eaf.core.annotations.HillaWorkaround
import com.vaadin.flow.server.auth.AnonymousAllowed
import com.vaadin.hilla.Endpoint
import jakarta.annotation.security.RolesAllowed
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated

@Endpoint
@RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN", "TENANT_ADMIN")
@HillaWorkaround(
    description =
        "Endpoint was preemptively disabled due to Hilla issue #3443, but analysis shows no DTOs were affected. Re-enabling and marking for audit.",
)
/**
 * Hilla endpoint for role management operations. Provides type-safe access to role lifecycle and
 * permission management.
 */
@Validated
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
    ): CreateRoleResponse? =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "createRole",
            details = "name=${request.name}, scope=${request.scope}",
        ) {
            runBlocking {
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
            }
        }

    /** Updates an existing role's details. */
    fun updateRole(
        roleId: String,
        @Valid request: UpdateRoleRequest,
    ): UpdateRoleResponse? =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "updateRole",
            details = "roleId=$roleId",
        ) {
            runBlocking {
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
            }
        }

    /** Deletes a role by ID. */
    @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN") // Only platform admins can delete roles
    fun deleteRole(roleId: String): DeleteRoleResponse? =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "deleteRole",
            details = "roleId=$roleId",
        ) {
            runBlocking {
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
            }
        }

    /** Assigns a permission to a role. */
    fun assignPermission(
        roleId: String,
        permissionId: String,
    ): RolePermissionResponse? =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "assignPermission",
            details = "roleId=$roleId, permissionId=$permissionId",
        ) {
            runBlocking {
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
            }
        }

    /** Removes a permission from a role. */
    fun removePermission(
        roleId: String,
        permissionId: String,
    ): RolePermissionResponse? =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "removePermission",
            details = "roleId=$roleId, permissionId=$permissionId",
        ) {
            runBlocking {
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
            }
        }

    /** Gets a role by ID. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun getRole(roleId: String): RoleDto? =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "getRole",
            details = "roleId=$roleId",
        ) {
            runBlocking {
                val role = roleService.getRoleById(roleId)
                if (role != null) {
                    logger.debug("Role retrieved: {}", roleId)
                    RoleDto.fromDomain(role)
                } else {
                    logger.warn("Role not found: {}", roleId)
                    null
                }
            }
        }

    /** Lists roles by scope and optional tenant ID. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun listRoles(
        scope: RoleScope,
        tenantId: String? = null,
    ): List<RoleDto> =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "listRoles",
            details = "scope=$scope, tenantId=$tenantId",
        ) {
            runBlocking {
                val roles = roleService.listRoles(scope, tenantId)
                logger.debug("Retrieved {} roles", roles.size)
                roles.map { RoleDto.fromDomain(it) }
            }
        }
            ?: emptyList()

    /** Gets all roles available for a specific tenant (platform + tenant-scoped). */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun getRolesForTenant(tenantId: String): List<RoleDto> =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "getRolesForTenant",
            details = "tenantId=$tenantId",
        ) {
            runBlocking {
                val roles = roleService.getRolesForTenant(tenantId)
                logger.debug("Retrieved {} roles for tenant {}", roles.size, tenantId)
                roles.map { RoleDto.fromDomain(it) }
            }
        }
            ?: emptyList()

    // Permission Management Operations

    /** Creates a new permission. */
    @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN") // Only platform admins can create permissions
    fun createPermission(
        @Valid request: CreatePermissionRequest,
    ): CreatePermissionResponse? =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "createPermission",
            details = "name=${request.name}, resource=${request.resource}",
        ) {
            runBlocking {
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
            }
        }

    /** Gets all available permissions. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun listPermissions(): List<PermissionDto> =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "listPermissions",
            details = "",
        ) {
            runBlocking {
                val permissions = roleService.getAllPermissions()
                logger.debug("Retrieved {} permissions", permissions.size)
                permissions.map { PermissionDto.fromDomain(it) }
            }
        }
            ?: emptyList()

    /** Gets permissions by resource. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun getPermissionsByResource(resource: String): List<PermissionDto> =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "getPermissionsByResource",
            details = "resource=$resource",
        ) {
            runBlocking {
                val permissions = roleService.getPermissionsByResource(resource)
                logger.debug(
                    "Retrieved {} permissions for resource {}",
                    permissions.size,
                    resource,
                )
                permissions.map { PermissionDto.fromDomain(it) }
            }
        }
            ?: emptyList()

    /** Gets a permission by ID. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun getPermission(permissionId: String): PermissionDto? =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "getPermission",
            details = "permissionId=$permissionId",
        ) {
            runBlocking {
                val permission = roleService.getPermissionById(permissionId)
                if (permission != null) {
                    logger.debug("Permission retrieved: {}", permissionId)
                    PermissionDto.fromDomain(permission)
                } else {
                    logger.warn("Permission not found: {}", permissionId)
                    null
                }
            }
        }
}

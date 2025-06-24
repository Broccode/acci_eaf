package com.axians.eaf.controlplane.application.dto.role

import com.axians.eaf.controlplane.application.dto.user.PermissionDto
import com.axians.eaf.controlplane.application.dto.user.RoleDto
import com.axians.eaf.controlplane.domain.model.user.Permission
import com.axians.eaf.controlplane.domain.model.user.Role
import com.axians.eaf.controlplane.domain.model.user.RoleScope
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

/** Request to create a new role. */
data class CreateRoleRequest(
    @field:NotBlank(message = "Role name cannot be blank")
    @field:Size(max = 100, message = "Role name cannot exceed 100 characters")
    val name: String,
    @field:NotBlank(message = "Description cannot be blank")
    @field:Size(max = 500, message = "Description cannot exceed 500 characters")
    val description: String,
    val scope: RoleScope,
    val tenantId: String? = null,
    val permissions: Set<String> = emptySet(),
)

/** Response after successful role creation. */
data class CreateRoleResponse(
    val role: RoleDto,
    val message: String,
    val timestamp: Instant,
) {
    companion object {
        fun success(role: Role): CreateRoleResponse =
            CreateRoleResponse(
                role = RoleDto.fromDomain(role),
                message = "Role '${role.name}' created successfully",
                timestamp = Instant.now(),
            )

        fun failure(message: String): CreateRoleResponse =
            CreateRoleResponse(
                role =
                    RoleDto(
                        id = "",
                        name = "",
                        description = "",
                        scope = RoleScope.PLATFORM,
                        tenantId = null,
                        permissions = emptySet(),
                    ),
                message = message,
                timestamp = Instant.now(),
            )
    }
}

/** Request to update an existing role. */
data class UpdateRoleRequest(
    @field:NotBlank(message = "Role name cannot be blank")
    @field:Size(max = 100, message = "Role name cannot exceed 100 characters")
    val name: String,
    @field:NotBlank(message = "Description cannot be blank")
    @field:Size(max = 500, message = "Description cannot exceed 500 characters")
    val description: String,
)

/** Response for role update operations. */
data class UpdateRoleResponse(
    val role: RoleDto,
    val message: String,
    val timestamp: Instant,
) {
    companion object {
        fun success(role: Role): UpdateRoleResponse =
            UpdateRoleResponse(
                role = RoleDto.fromDomain(role),
                message = "Role '${role.name}' updated successfully",
                timestamp = Instant.now(),
            )
    }
}

/** Response for role deletion operations. */
data class DeleteRoleResponse(
    val roleId: String,
    val roleName: String,
    val message: String,
    val timestamp: Instant,
) {
    companion object {
        fun success(
            roleId: String,
            roleName: String,
        ): DeleteRoleResponse =
            DeleteRoleResponse(
                roleId = roleId,
                roleName = roleName,
                message = "Role '$roleName' deleted successfully",
                timestamp = Instant.now(),
            )
    }
}

/** Response for role-permission assignment operations. */
data class RolePermissionResponse(
    val roleId: String,
    val roleName: String,
    val permissionId: String,
    val permissionName: String,
    val action: String, // "assigned" or "removed"
    val message: String,
    val timestamp: Instant,
) {
    companion object {
        fun assigned(
            roleId: String,
            roleName: String,
            permissionId: String,
            permissionName: String,
        ): RolePermissionResponse =
            RolePermissionResponse(
                roleId = roleId,
                roleName = roleName,
                permissionId = permissionId,
                permissionName = permissionName,
                action = "assigned",
                message = "Permission '$permissionName' assigned to role '$roleName'",
                timestamp = Instant.now(),
            )

        fun removed(
            roleId: String,
            roleName: String,
            permissionId: String,
            permissionName: String,
        ): RolePermissionResponse =
            RolePermissionResponse(
                roleId = roleId,
                roleName = roleName,
                permissionId = permissionId,
                permissionName = permissionName,
                action = "removed",
                message = "Permission '$permissionName' removed from role '$roleName'",
                timestamp = Instant.now(),
            )
    }
}

/** Summary role information for lists. */
data class RoleSummary(
    val id: String,
    val name: String,
    val description: String,
    val scope: RoleScope,
    val tenantId: String?,
    val permissionCount: Int,
    val userCount: Int = 0, // Will be populated by service
)

/** Request to create a new permission. */
data class CreatePermissionRequest(
    @field:NotBlank(message = "Permission name cannot be blank")
    @field:Size(max = 100, message = "Permission name cannot exceed 100 characters")
    val name: String,
    @field:NotBlank(message = "Description cannot be blank")
    @field:Size(max = 500, message = "Description cannot exceed 500 characters")
    val description: String,
    @field:NotBlank(message = "Resource cannot be blank")
    @field:Size(max = 50, message = "Resource cannot exceed 50 characters")
    val resource: String,
    @field:NotBlank(message = "Action cannot be blank")
    @field:Size(max = 50, message = "Action cannot exceed 50 characters")
    val action: String,
)

/** Response after successful permission creation. */
data class CreatePermissionResponse(
    val permission: PermissionDto,
    val message: String,
    val timestamp: Instant,
) {
    companion object {
        fun success(permission: Permission): CreatePermissionResponse =
            CreatePermissionResponse(
                permission = PermissionDto.fromDomain(permission),
                message = "Permission '${permission.name}' created successfully",
                timestamp = Instant.now(),
            )

        fun failure(message: String): CreatePermissionResponse =
            CreatePermissionResponse(
                permission =
                    PermissionDto(
                        id = "",
                        name = "",
                        description = "",
                        resource = "",
                        action = "",
                        permissionString = "",
                    ),
                message = message,
                timestamp = Instant.now(),
            )
    }
}

/** Filter criteria for role queries. */
data class RoleFilter(
    val scope: RoleScope? = null,
    val tenantId: String? = null,
    val namePattern: String? = null,
    val permissionIds: Set<String> = emptySet(),
    val includePermissions: Boolean = false,
    val includeUserCount: Boolean = false,
)

/** Filter criteria for permission queries. */
data class PermissionFilter(
    val resource: String? = null,
    val action: String? = null,
    val namePattern: String? = null,
)

/** Generic operation result for simple operations. */
data class RoleOperationResult(
    val success: Boolean,
    val message: String,
    val timestamp: Instant,
) {
    companion object {
        fun success(message: String): RoleOperationResult =
            RoleOperationResult(
                success = true,
                message = message,
                timestamp = Instant.now(),
            )

        fun failure(message: String): RoleOperationResult =
            RoleOperationResult(
                success = false,
                message = message,
                timestamp = Instant.now(),
            )
    }
}

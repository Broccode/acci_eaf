package com.axians.eaf.controlplane.application.dto.user

import com.axians.eaf.controlplane.domain.model.user.Permission
import com.axians.eaf.controlplane.domain.model.user.Role
import com.axians.eaf.controlplane.domain.model.user.RoleScope
import com.axians.eaf.controlplane.domain.model.user.User
import com.axians.eaf.controlplane.domain.model.user.UserStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.Instant

/** Request to create a new user. */
data class CreateUserRequest(
    @field:NotBlank(message = "Email cannot be blank")
    @field:Email(message = "Email must be valid")
    val email: String,
    @field:NotBlank(message = "First name cannot be blank")
    @field:Size(max = 50, message = "First name cannot exceed 50 characters")
    val firstName: String,
    @field:NotBlank(message = "Last name cannot be blank")
    @field:Size(max = 50, message = "Last name cannot exceed 50 characters")
    val lastName: String,
    @field:NotBlank(message = "Tenant ID cannot be blank") val tenantId: String,
    val roles: List<String> = emptyList(),
    val sendWelcomeEmail: Boolean = true,
    val activateImmediately: Boolean = false,
) {
    init {
        require(roles.toSet().size == roles.size) { "Duplicate roles are not allowed" }
        require(roles.all { it.isNotBlank() }) { "Role names cannot be blank" }
    }

    /** Get unique roles as a Set for domain layer processing. */
    fun getUniqueRoles(): Set<String> = roles.toSet()
}

/** Response after successful user creation. */
data class CreateUserResponse(
    val userId: String,
    val email: String,
    val fullName: String,
    val status: UserStatus,
    val message: String,
) {
    companion object {
        fun success(
            userId: String,
            email: String,
            fullName: String,
            status: UserStatus,
        ): CreateUserResponse =
            CreateUserResponse(
                userId = userId,
                email = email,
                fullName = fullName,
                status = status,
                message = "User created successfully",
            )

        fun failure(message: String): CreateUserResponse =
            CreateUserResponse(
                userId = "",
                email = "",
                fullName = "",
                status = UserStatus.PENDING,
                message = message,
            )
    }
}

/** Request to update an existing user's profile. */
data class UpdateUserRequest(
    @field:NotBlank(message = "First name cannot be blank")
    @field:Size(max = 50, message = "First name cannot exceed 50 characters")
    val firstName: String,
    @field:NotBlank(message = "Last name cannot be blank")
    @field:Size(max = 50, message = "Last name cannot exceed 50 characters")
    val lastName: String,
)

/** Detailed user information response. */
data class UserDetailsResponse(
    val user: UserDto,
    val permissions: List<String>,
    val lastLoginFormatted: String?,
    val accountAge: String,
)

/** Basic user information DTO. */
data class UserDto(
    val id: String,
    val tenantId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val status: UserStatus,
    val roles: List<RoleDto>,
    val lastLogin: Instant?,
    val createdAt: Instant,
    val lastModified: Instant,
    val activatedAt: Instant?,
    val deactivatedAt: Instant?,
) {
    companion object {
        /** Creates DTO from domain model. */
        fun fromDomain(user: User): UserDto =
            UserDto(
                id = user.id.value,
                tenantId = user.tenantId.value,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                fullName = user.getFullName(),
                status = user.status,
                roles = user.roles.map { RoleDto.fromDomain(it) }.toList(),
                lastLogin = user.lastLogin,
                createdAt = user.createdAt,
                lastModified = user.lastModified,
                activatedAt = user.activatedAt,
                deactivatedAt = user.deactivatedAt,
            )
    }
}

/** Role information DTO. */
data class RoleDto(
    val id: String,
    val name: String,
    val description: String,
    val scope: RoleScope,
    val tenantId: String?,
    val permissions: List<PermissionDto>,
) {
    companion object {
        /** Creates DTO from domain model. */
        fun fromDomain(role: Role): RoleDto =
            RoleDto(
                id = role.id.value,
                name = role.name,
                description = role.description,
                scope = role.scope,
                tenantId = role.tenantId?.value,
                permissions =
                    role.permissions.map { PermissionDto.fromDomain(it) }.toList(),
            )
    }
}

/** Permission information DTO. */
data class PermissionDto(
    val id: String,
    val name: String,
    val description: String,
    val resource: String,
    val action: String,
    val permissionString: String,
) {
    companion object {
        /** Creates DTO from domain model. */
        fun fromDomain(permission: Permission): PermissionDto =
            PermissionDto(
                id = permission.id.value,
                name = permission.name,
                description = permission.description,
                resource = permission.resource,
                action = permission.action,
                permissionString = permission.toPermissionString(),
            )
    }
}

/** Summary user information for lists. */
data class UserSummary(
    val id: String,
    val email: String,
    val fullName: String,
    val status: UserStatus,
    val roles: List<String>,
    val lastLogin: Instant?,
    val createdAt: Instant,
)

/** Response for user status change operations. */
data class UserStatusResponse(
    val userId: String,
    val email: String,
    val fullName: String,
    val oldStatus: UserStatus,
    val newStatus: UserStatus,
    val message: String,
    val timestamp: Instant,
) {
    companion object {
        fun success(context: UserStatusContext): UserStatusResponse =
            UserStatusResponse(
                userId = context.userId,
                email = context.email,
                fullName = context.fullName,
                oldStatus = context.oldStatus,
                newStatus = context.newStatus,
                message = "User ${context.action} successfully",
                timestamp = Instant.now(),
            )
    }
}

/** Context data for creating user status responses. Addresses detekt LongParameterList issue. */
data class UserStatusContext(
    val userId: String,
    val email: String,
    val fullName: String,
    val oldStatus: UserStatus,
    val newStatus: UserStatus,
    val action: String,
)

/** Response for role assignment operations. */
data class UserRoleResponse(
    val userId: String,
    val email: String,
    val fullName: String,
    val roleName: String,
    val action: String, // "assigned" or "removed"
    val message: String,
    val timestamp: Instant,
) {
    companion object {
        fun assigned(
            userId: String,
            email: String,
            fullName: String,
            roleName: String,
        ): UserRoleResponse =
            UserRoleResponse(
                userId = userId,
                email = email,
                fullName = fullName,
                roleName = roleName,
                action = "assigned",
                message = "Role '$roleName' assigned successfully",
                timestamp = Instant.now(),
            )

        fun removed(
            userId: String,
            email: String,
            fullName: String,
            roleName: String,
        ): UserRoleResponse =
            UserRoleResponse(
                userId = userId,
                email = email,
                fullName = fullName,
                roleName = roleName,
                action = "removed",
                message = "Role '$roleName' removed successfully",
                timestamp = Instant.now(),
            )
    }
}

/** Response for password reset operations. */
data class PasswordResetResponse(
    val userId: String,
    val email: String,
    val message: String,
    val resetToken: String? = null,
    val expiresAt: Instant? = null,
) {
    companion object {
        fun success(
            userId: String,
            email: String,
            resetToken: String,
            expiresAt: Instant,
        ): PasswordResetResponse =
            PasswordResetResponse(
                userId = userId,
                email = email,
                message = "Password reset initiated successfully",
                resetToken = resetToken,
                expiresAt = expiresAt,
            )

        fun failure(message: String): PasswordResetResponse =
            PasswordResetResponse(
                userId = "",
                email = "",
                message = message,
            )
    }
}

/** Request for bulk user operations. */
data class BulkUserUpdateRequest(
    @field:NotEmpty(message = "User IDs cannot be empty") val userIds: List<String>,
    val operation: BulkUserOperation,
    val roleId: String? = null,
    val reason: String? = null,
) {
    init {
        require(userIds.toSet().size == userIds.size) { "Duplicate user IDs are not allowed" }
        require(userIds.all { it.isNotBlank() }) { "User IDs cannot be blank" }
    }

    /** Get unique user IDs as a Set for domain layer processing. */
    fun getUniqueUserIds(): Set<String> = userIds.toSet()
}

/** Supported bulk user operations. */
enum class BulkUserOperation {
    ACTIVATE,
    SUSPEND,
    REACTIVATE,
    DEACTIVATE,
    ASSIGN_ROLE,
    REMOVE_ROLE,
}

/** Response for bulk user operations. */
data class BulkUpdateResponse(
    val requestedCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val results: List<BulkOperationResult>,
    val summary: String,
) {
    companion object {
        fun create(results: List<BulkOperationResult>): BulkUpdateResponse {
            val successCount = results.count { it.success }
            val failureCount = results.count { !it.success }
            return BulkUpdateResponse(
                requestedCount = results.size,
                successCount = successCount,
                failureCount = failureCount,
                results = results,
                summary =
                    "Processed ${results.size} users: $successCount successful, $failureCount failed",
            )
        }
    }
}

/** Result for individual user in bulk operation. */
data class BulkOperationResult(
    val userId: String,
    val email: String,
    val success: Boolean,
    val message: String,
)

/** Filter criteria for user queries. */
data class UserFilter(
    val tenantId: String? = null,
    val status: UserStatus? = null,
    val emailPattern: String? = null,
    val namePattern: String? = null,
    val roleNames: List<String> = emptyList(),
    val createdAfter: Instant? = null,
    val createdBefore: Instant? = null,
    val lastLoginAfter: Instant? = null,
    val lastLoginBefore: Instant? = null,
    val page: Int = 0,
    val size: Int = 20,
) {
    init {
        require(roleNames.toSet().size == roleNames.size) {
            "Duplicate role names are not allowed in filter"
        }
    }

    /** Get unique role names as a Set for domain layer processing. */
    fun getUniqueRoleNames(): Set<String> = roleNames.toSet()
}

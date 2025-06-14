package com.axians.eaf.iam.web

import com.axians.eaf.core.security.EafSecurityContextHolder
import com.axians.eaf.iam.application.port.inbound.CreateUserCommand
import com.axians.eaf.iam.application.port.inbound.CreateUserUseCase
import com.axians.eaf.iam.application.port.inbound.ListUsersInTenantQuery
import com.axians.eaf.iam.application.port.inbound.ListUsersInTenantUseCase
import com.axians.eaf.iam.application.port.inbound.UpdateUserStatusCommand
import com.axians.eaf.iam.application.port.inbound.UpdateUserStatusUseCase
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST Controller for user management operations.
 * Provides endpoints for creating, listing, and updating users within tenants.
 */
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/users")
class UserController(
    private val createUserUseCase: CreateUserUseCase,
    private val listUsersInTenantUseCase: ListUsersInTenantUseCase,
    private val updateUserStatusUseCase: UpdateUserStatusUseCase,
    private val securityContextHolder: EafSecurityContextHolder,
) {
    /**
     * Validates that the authenticated user can access the specified tenant.
     * @param tenantId the tenant ID from the path parameter
     * @throws IllegalArgumentException if the user cannot access the tenant
     */
    private fun validateTenantAccess(tenantId: String) {
        val authenticatedTenantId = securityContextHolder.getTenantId()
        if (authenticatedTenantId != tenantId) {
            throw IllegalArgumentException(
                "Access denied: User from tenant '$authenticatedTenantId' cannot access tenant '$tenantId'",
            )
        }
    }

    /**
     * Create a new user within a tenant.
     * Only TENANT_ADMIN users can create new users.
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_TENANT_ADMIN')")
    fun createUser(
        @PathVariable tenantId: String,
        @Valid @RequestBody request: CreateUserRequest,
    ): ResponseEntity<CreateUserResponse> {
        validateTenantAccess(tenantId)

        val command =
            CreateUserCommand(
                tenantId = tenantId,
                email = request.email,
                username = request.username,
            )
        val result = createUserUseCase.createUser(command)

        val response =
            CreateUserResponse(
                userId = result.userId,
                tenantId = result.tenantId,
                email = result.email,
                username = result.username,
                status = result.status,
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * List all users within a specific tenant.
     * Only TENANT_ADMIN users can list users in their tenant.
     */
    @GetMapping
    @PreAuthorize("hasRole('ROLE_TENANT_ADMIN')")
    fun listUsers(
        @PathVariable tenantId: String,
    ): ResponseEntity<ListUsersResponse> {
        validateTenantAccess(tenantId)

        val query = ListUsersInTenantQuery(tenantId = tenantId)
        val result = listUsersInTenantUseCase.listUsers(query)

        val response =
            ListUsersResponse(
                tenantId = result.tenantId,
                users =
                    result.users.map { user ->
                        UserSummaryResponse(
                            userId = user.userId,
                            email = user.email,
                            username = user.username,
                            role = user.role,
                            status = user.status,
                        )
                    },
            )
        return ResponseEntity.ok(response)
    }

    /**
     * Update the status of a user within a tenant.
     * Only TENANT_ADMIN users can update user status.
     */
    @PutMapping("/{userId}/status")
    @PreAuthorize("hasRole('ROLE_TENANT_ADMIN')")
    fun updateUserStatus(
        @PathVariable tenantId: String,
        @PathVariable userId: String,
        @Valid @RequestBody request: UpdateUserStatusRequest,
    ): ResponseEntity<UpdateUserStatusResponse> {
        validateTenantAccess(tenantId)

        val command =
            UpdateUserStatusCommand(
                tenantId = tenantId,
                userId = userId,
                newStatus = request.newStatus,
            )
        val result = updateUserStatusUseCase.updateUserStatus(command)

        val response =
            UpdateUserStatusResponse(
                userId = result.userId,
                tenantId = result.tenantId,
                email = result.email,
                username = result.username,
                previousStatus = result.previousStatus,
                newStatus = result.newStatus,
            )
        return ResponseEntity.ok(response)
    }
}

// Request/Response DTOs

data class CreateUserRequest(
    @field:Email(message = "Email must be valid")
    @field:NotBlank(message = "Email cannot be blank")
    val email: String,
    val username: String? = null,
)

data class CreateUserResponse(
    val userId: String,
    val tenantId: String,
    val email: String,
    val username: String?,
    val status: String,
)

data class ListUsersResponse(
    val tenantId: String,
    val users: List<UserSummaryResponse>,
)

data class UserSummaryResponse(
    val userId: String,
    val email: String,
    val username: String?,
    val role: String,
    val status: String,
)

data class UpdateUserStatusRequest(
    @field:NotBlank(message = "New status cannot be blank")
    val newStatus: String,
)

data class UpdateUserStatusResponse(
    val userId: String,
    val tenantId: String,
    val email: String,
    val username: String?,
    val previousStatus: String,
    val newStatus: String,
)

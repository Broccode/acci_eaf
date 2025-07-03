package com.axians.eaf.controlplane.infrastructure.adapter.input

import com.axians.eaf.controlplane.application.dto.tenant.PagedResponse
import com.axians.eaf.controlplane.application.dto.user.CreateUserRequest
import com.axians.eaf.controlplane.application.dto.user.CreateUserResponse
import com.axians.eaf.controlplane.application.dto.user.PasswordResetResponse
import com.axians.eaf.controlplane.application.dto.user.UserDetailsResponse
import com.axians.eaf.controlplane.application.dto.user.UserDto
import com.axians.eaf.controlplane.application.dto.user.UserFilter
import com.axians.eaf.controlplane.application.dto.user.UserStatusContext
import com.axians.eaf.controlplane.application.dto.user.UserStatusResponse
import com.axians.eaf.controlplane.application.dto.user.UserSummary
import com.axians.eaf.controlplane.domain.model.user.UserStatus
import com.axians.eaf.controlplane.domain.service.CreateUserResult
import com.axians.eaf.controlplane.domain.service.PasswordResetResult
import com.axians.eaf.controlplane.domain.service.UserDetailsResult
import com.axians.eaf.controlplane.domain.service.UserService
import com.axians.eaf.controlplane.domain.service.UserStatusResult
import com.axians.eaf.controlplane.infrastructure.adapter.input.common.EndpointExceptionHandler
import com.axians.eaf.controlplane.infrastructure.adapter.input.common.ResponseConstants
import com.axians.eaf.core.annotations.HillaWorkaround
import com.vaadin.flow.server.auth.AnonymousAllowed
import com.vaadin.hilla.Endpoint
import jakarta.annotation.security.RolesAllowed
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import java.time.Duration
import java.time.Instant

@Endpoint
@RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN", "TENANT_ADMIN")
@HillaWorkaround(
    description =
        "Endpoint was preemptively disabled due to Hilla issue #3443, but analysis shows no DTOs were affected. Re-enabling and marking for audit.",
)
/**
 * Hilla endpoint for user management operations. Provides type-safe frontend access to user
 * lifecycle management.
 */
@Validated
class UserManagementEndpoint(
    private val userService: UserService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UserManagementEndpoint::class.java)
    }

    /** Creates a new user with the specified details. */
    fun createUser(
        @Valid request: CreateUserRequest,
    ): CreateUserResponse? =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "createUser",
            details = "email=${request.email}, tenantId=${request.tenantId}",
        ) {
            runBlocking {
                val result =
                    userService.createUser(
                        email = request.email,
                        firstName = request.firstName,
                        lastName = request.lastName,
                        tenantId = request.tenantId,
                        activateImmediately = request.activateImmediately,
                    )

                when (result) {
                    is CreateUserResult.Success ->
                        CreateUserResponse.success(
                            result.userId,
                            result.email,
                            result.fullName,
                            result.status,
                        )
                    is CreateUserResult.Failure -> CreateUserResponse.failure(result.message)
                }
            }
        }
            ?: CreateUserResponse.failure("Failed to create user")

    /** Retrieves detailed information about a specific user. */
    fun getUser(userId: String): UserDetailsResponse? =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "getUser",
            details = "userId=$userId",
        ) {
            runBlocking {
                when (val result = userService.getUserDetails(userId)) {
                    is UserDetailsResult.Success ->
                        UserDetailsResponse(
                            user = UserDto.fromDomain(result.details.user),
                            permissions = result.details.permissions.toList(),
                            lastLoginFormatted = result.details.lastLoginFormatted,
                            accountAge = result.details.accountAge,
                        )
                    is UserDetailsResult.NotFound -> null
                }
            }
        }

    /** Activates a pending user account. */
    fun activateUser(userId: String): UserStatusResponse? =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "activateUser",
            details = "userId=$userId",
        ) {
            runBlocking {
                val result = userService.activateUser(userId)

                when (result) {
                    is UserStatusResult.Success ->
                        UserStatusResponse.success(
                            UserStatusContext(
                                userId = result.userId,
                                email = result.email,
                                fullName = result.fullName,
                                oldStatus = result.oldStatus,
                                newStatus = result.newStatus,
                                action = result.action,
                            ),
                        )
                    is UserStatusResult.NotFound, is UserStatusResult.Failure -> null
                }
            }
        }

    /** Suspends an active user account. */
    fun suspendUser(
        userId: String,
        reason: String?,
    ): UserStatusResponse? =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "suspendUser",
            details = "userId=$userId, reason=$reason",
        ) {
            runBlocking {
                val result = userService.suspendUser(userId)

                when (result) {
                    is UserStatusResult.Success ->
                        UserStatusResponse.success(
                            UserStatusContext(
                                userId = result.userId,
                                email = result.email,
                                fullName = result.fullName,
                                oldStatus = result.oldStatus,
                                newStatus = result.newStatus,
                                action = result.action,
                            ),
                        )
                    is UserStatusResult.NotFound, is UserStatusResult.Failure -> null
                }
            }
        }

    /** Initiates a password reset for a user. */
    fun resetPassword(userId: String): PasswordResetResponse? =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "resetPassword",
            details = "userId=$userId",
        ) {
            runBlocking {
                val result = userService.resetPassword(userId)

                when (result) {
                    is PasswordResetResult.Success ->
                        PasswordResetResponse.success(
                            userId = result.userId,
                            email = result.email,
                            resetToken = result.resetToken,
                            expiresAt = result.expiresAt,
                        )
                    is PasswordResetResult.NotFound ->
                        PasswordResetResponse.failure("User not found: $userId")
                    is PasswordResetResult.Failure ->
                        PasswordResetResponse.failure(result.message)
                }
            }
        }
            ?: PasswordResetResponse.failure("Failed to reset password")

    /** Lists all users with optional filtering and pagination. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun listUsers(filter: UserFilter): PagedResponse<UserSummary>? =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "listUsers",
            details = "filter=$filter",
        ) { runBlocking { userService.listUsers(filter) } }
            ?: PagedResponse.of(emptyList(), filter.page, filter.size, 0)

    /** Gets a summary of users for a specific tenant. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun getUsersForTenant(
        tenantId: String,
        page: Int = 0,
        size: Int = ResponseConstants.DEFAULT_PAGE_SIZE,
    ): PagedResponse<UserSummary> {
        val filter =
            UserFilter(
                tenantId = tenantId,
                page = page,
                size = size,
            )
        return listUsers(filter) ?: PagedResponse.of(emptyList(), filter.page, filter.size, 0)
    }

    /** Checks if a user exists and can access the system. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun canUserAccess(userId: String): Boolean? =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "canUserAccess",
            details = "userId=$userId",
        ) {
            runBlocking {
                when (val result = userService.getUserDetails(userId)) {
                    is UserDetailsResult.Success ->
                        result.details.user
                            .getStatus()
                            .canAccess()
                    is UserDetailsResult.NotFound -> false
                }
            }
        }
            ?: false

    /** Gets user statistics for dashboard purposes. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun getUserStatistics(tenantId: String): UserStatistics? =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "getUserStatistics",
            details = "tenantId=$tenantId",
        ) {
            runBlocking {
                val allUsers =
                    userService.listUsers(
                        UserFilter(
                            tenantId = tenantId,
                            size = ResponseConstants.MAX_PAGE_SIZE,
                        ),
                    )
                val totalUsers = allUsers.totalElements.toInt()
                val activeUsers = allUsers.content.count { it.status.canAccess() }
                val pendingUsers = allUsers.content.count { it.status == UserStatus.PENDING }
                val suspendedUsers =
                    allUsers.content.count { it.status == UserStatus.SUSPENDED }

                UserStatistics(
                    totalUsers = totalUsers,
                    activeUsers = activeUsers,
                    pendingUsers = pendingUsers,
                    suspendedUsers = suspendedUsers,
                    recentlyActive =
                        allUsers.content.count {
                            it.lastLogin != null &&
                                Duration
                                    .between(it.lastLogin, Instant.now())
                                    .toDays() <=
                                ResponseConstants.RECENT_ACTIVITY_DAYS
                        },
                )
            }
        }
            ?: UserStatistics(0, 0, 0, 0, 0)
}

/** User statistics for dashboard display. */
data class UserStatistics(
    val totalUsers: Int,
    val activeUsers: Int,
    val pendingUsers: Int,
    val suspendedUsers: Int,
    val recentlyActive: Int,
)

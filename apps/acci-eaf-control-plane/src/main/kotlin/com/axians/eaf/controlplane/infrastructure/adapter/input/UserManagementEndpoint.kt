package com.axians.eaf.controlplane.infrastructure.adapter.input

import com.axians.eaf.controlplane.application.dto.tenant.PagedResponse
import com.axians.eaf.controlplane.application.dto.user.CreateUserRequest
import com.axians.eaf.controlplane.application.dto.user.CreateUserResponse
import com.axians.eaf.controlplane.application.dto.user.PasswordResetResponse
import com.axians.eaf.controlplane.application.dto.user.UserDetailsResponse
import com.axians.eaf.controlplane.application.dto.user.UserDto
import com.axians.eaf.controlplane.application.dto.user.UserFilter
import com.axians.eaf.controlplane.application.dto.user.UserStatusResponse
import com.axians.eaf.controlplane.application.dto.user.UserSummary
import com.axians.eaf.controlplane.domain.model.user.UserStatus
import com.axians.eaf.controlplane.domain.service.CreateUserResult
import com.axians.eaf.controlplane.domain.service.PasswordResetResult
import com.axians.eaf.controlplane.domain.service.UserDetailsResult
import com.axians.eaf.controlplane.domain.service.UserService
import com.axians.eaf.controlplane.domain.service.UserStatusResult
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
    ): CreateUserResponse {
        logger.info("Creating user: {} in tenant: {}", request.email, request.tenantId)

        return runBlocking {
            try {
                val result =
                    userService.createUser(
                        email = request.email,
                        firstName = request.firstName,
                        lastName = request.lastName,
                        tenantId = request.tenantId,
                        activateImmediately = request.activateImmediately,
                    )

                when (result) {
                    is CreateUserResult.Success -> {
                        logger.info(
                            "User created successfully: {} ({})",
                            result.userId,
                            result.email,
                        )
                        CreateUserResponse.success(
                            result.userId,
                            result.email,
                            result.fullName,
                            result.status,
                        )
                    }
                    is CreateUserResult.Failure -> {
                        logger.warn("Failed to create user: {}", result.message)
                        CreateUserResponse.failure(result.message)
                    }
                }
            } catch (exception: Exception) {
                logger.error("Error creating user: ${request.email}", exception)
                CreateUserResponse.failure("Failed to create user: ${exception.message}")
            }
        }
    }

    /** Retrieves detailed information about a specific user. */
    fun getUser(userId: String): UserDetailsResponse? {
        logger.debug("Retrieving user details: {}", userId)

        return runBlocking {
            try {
                when (val result = userService.getUserDetails(userId)) {
                    is UserDetailsResult.Success -> {
                        logger.debug("User details retrieved: {}", userId)
                        UserDetailsResponse(
                            user = UserDto.fromDomain(result.details.user),
                            permissions = result.details.permissions.toList(),
                            lastLoginFormatted = result.details.lastLoginFormatted,
                            accountAge = result.details.accountAge,
                        )
                    }
                    is UserDetailsResult.NotFound -> {
                        logger.warn("User not found: {}", userId)
                        null
                    }
                }
            } catch (exception: Exception) {
                logger.error("Error retrieving user: $userId", exception)
                null
            }
        }
    }

    /** Activates a pending user account. */
    fun activateUser(userId: String): UserStatusResponse? {
        logger.info("Activating user: {}", userId)

        return runBlocking {
            try {
                val result = userService.activateUser(userId)

                when (result) {
                    is UserStatusResult.Success -> {
                        logger.info("User activated successfully: {}", userId)
                        UserStatusResponse.success(
                            userId = result.userId,
                            email = result.email,
                            fullName = result.fullName,
                            oldStatus = result.oldStatus,
                            newStatus = result.newStatus,
                            action = result.action,
                        )
                    }
                    is UserStatusResult.NotFound,
                    is UserStatusResult.Failure,
                    -> {
                        logger.warn(
                            "Failed to activate user {}: {}",
                            userId,
                            when (result) {
                                is UserStatusResult.NotFound -> result.message
                                is UserStatusResult.Failure -> result.message
                                else -> "Unknown error"
                            },
                        )
                        null
                    }
                }
            } catch (exception: Exception) {
                logger.error("Error activating user: $userId", exception)
                null
            }
        }
    }

    /** Suspends an active user account. */
    fun suspendUser(
        userId: String,
        reason: String?,
    ): UserStatusResponse? {
        logger.info("Suspending user: {} with reason: {}", userId, reason)

        return runBlocking {
            try {
                val result = userService.suspendUser(userId)

                when (result) {
                    is UserStatusResult.Success -> {
                        logger.info("User suspended successfully: {}", userId)
                        UserStatusResponse.success(
                            userId = result.userId,
                            email = result.email,
                            fullName = result.fullName,
                            oldStatus = result.oldStatus,
                            newStatus = result.newStatus,
                            action = result.action,
                        )
                    }
                    is UserStatusResult.NotFound,
                    is UserStatusResult.Failure,
                    -> {
                        logger.warn(
                            "Failed to suspend user {}: {}",
                            userId,
                            when (result) {
                                is UserStatusResult.NotFound -> result.message
                                is UserStatusResult.Failure -> result.message
                                else -> "Unknown error"
                            },
                        )
                        null
                    }
                }
            } catch (exception: Exception) {
                logger.error("Error suspending user: $userId", exception)
                null
            }
        }
    }

    /** Initiates a password reset for a user. */
    fun resetPassword(userId: String): PasswordResetResponse {
        logger.info("Initiating password reset for user: {}", userId)

        return runBlocking {
            try {
                val result = userService.resetPassword(userId)

                when (result) {
                    is PasswordResetResult.Success -> {
                        logger.info("Password reset initiated for user: {}", userId)
                        PasswordResetResponse.success(
                            userId = result.userId,
                            email = result.email,
                            resetToken = result.resetToken,
                            expiresAt = result.expiresAt,
                        )
                    }
                    is PasswordResetResult.NotFound -> {
                        logger.warn("User not found for password reset: {}", userId)
                        PasswordResetResponse.failure("User not found: $userId")
                    }
                    is PasswordResetResult.Failure -> {
                        logger.warn(
                            "Failed to reset password for user {}: {}",
                            userId,
                            result.message,
                        )
                        PasswordResetResponse.failure(result.message)
                    }
                }
            } catch (exception: Exception) {
                logger.error("Error resetting password for user: $userId", exception)
                PasswordResetResponse.failure("Failed to reset password: ${exception.message}")
            }
        }
    }

    /** Lists all users with optional filtering and pagination. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun listUsers(filter: UserFilter): PagedResponse<UserSummary> {
        logger.debug("Listing users with filter: {}", filter)

        return runBlocking {
            try {
                val result = userService.listUsers(filter)
                logger.debug("Retrieved {} users", result.content.size)
                result
            } catch (exception: Exception) {
                logger.error("Error listing users", exception)
                PagedResponse.of(emptyList(), filter.page, filter.size, 0)
            }
        }
    }

    /** Gets a summary of users for a specific tenant. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun getUsersForTenant(
        tenantId: String,
        page: Int = 0,
        size: Int = 20,
    ): PagedResponse<UserSummary> {
        logger.debug("Retrieving users for tenant: {} (page: {}, size: {})", tenantId, page, size)

        val filter =
            UserFilter(
                tenantId = tenantId,
                page = page,
                size = size,
            )

        return listUsers(filter)
    }

    /** Checks if a user exists and can access the system. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun canUserAccess(userId: String): Boolean =
        runBlocking {
            try {
                when (val result = userService.getUserDetails(userId)) {
                    is UserDetailsResult.Success ->
                        result.details.user.status
                            .canAccess()
                    is UserDetailsResult.NotFound -> false
                }
            } catch (exception: Exception) {
                logger.error("Error checking user access: $userId", exception)
                false
            }
        }

    /** Gets user statistics for dashboard purposes. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun getUserStatistics(tenantId: String): UserStatistics {
        logger.debug("Getting user statistics for tenant: {}", tenantId)

        return runBlocking {
            try {
                val allUsers = userService.listUsers(UserFilter(tenantId = tenantId, size = 1000))
                val totalUsers = allUsers.totalElements.toInt()
                val activeUsers = allUsers.content.count { it.status.canAccess() }
                val pendingUsers = allUsers.content.count { it.status == UserStatus.PENDING }
                val suspendedUsers = allUsers.content.count { it.status == UserStatus.SUSPENDED }

                UserStatistics(
                    totalUsers = totalUsers,
                    activeUsers = activeUsers,
                    pendingUsers = pendingUsers,
                    suspendedUsers = suspendedUsers,
                    recentlyActive =
                        allUsers.content.count {
                            it.lastLogin != null &&
                                java.time.Duration
                                    .between(
                                        it.lastLogin,
                                        java.time.Instant.now(),
                                    ).toDays() <= 7
                        },
                )
            } catch (exception: Exception) {
                logger.error("Error getting user statistics for tenant: $tenantId", exception)
                UserStatistics(0, 0, 0, 0, 0)
            }
        }
    }
}

/** User statistics for dashboard display. */
data class UserStatistics(
    val totalUsers: Int,
    val activeUsers: Int,
    val pendingUsers: Int,
    val suspendedUsers: Int,
    val recentlyActive: Int,
)

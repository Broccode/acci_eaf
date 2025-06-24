package com.axians.eaf.controlplane.domain.service

import com.axians.eaf.controlplane.application.dto.tenant.PagedResponse
import com.axians.eaf.controlplane.application.dto.user.UserDetailsResponse
import com.axians.eaf.controlplane.application.dto.user.UserDto
import com.axians.eaf.controlplane.application.dto.user.UserFilter
import com.axians.eaf.controlplane.application.dto.user.UserSummary
import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.User
import com.axians.eaf.controlplane.domain.model.user.UserId
import com.axians.eaf.controlplane.domain.model.user.UserStatus
import com.axians.eaf.controlplane.domain.port.UserRepository
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Domain service for user management operations. Encapsulates business logic for user lifecycle
 * management.
 */
@Service
class UserService(
    private val userRepository: UserRepository,
) {
    /** Creates a new user with the specified details. */
    suspend fun createUser(
        email: String,
        firstName: String,
        lastName: String,
        tenantId: String,
        activateImmediately: Boolean = false,
    ): CreateUserResult {
        val targetTenantId = TenantId.fromString(tenantId)

        // Check if user already exists in this tenant
        if (userRepository.existsByEmailAndTenantId(email, targetTenantId)) {
            return CreateUserResult.failure(
                "User with email '$email' already exists in this tenant",
            )
        }

        // Create user (pending or active based on flag)
        val user =
            if (activateImmediately) {
                User.createActive(
                    tenantId = targetTenantId,
                    email = email,
                    firstName = firstName,
                    lastName = lastName,
                )
            } else {
                User.createPending(
                    tenantId = targetTenantId,
                    email = email,
                    firstName = firstName,
                    lastName = lastName,
                )
            }

        // Save user
        val savedUser = userRepository.save(user)

        return CreateUserResult.success(
            userId = savedUser.id.value,
            email = savedUser.email,
            fullName = savedUser.getFullName(),
            status = savedUser.status,
        )
    }

    /** Retrieves detailed user information. */
    suspend fun getUserDetails(userId: String): UserDetailsResult {
        val user =
            userRepository.findById(UserId.fromString(userId))
                ?: return UserDetailsResult.notFound("User not found: $userId")

        val permissions = user.getAllPermissions().map { it.toPermissionString() }.toSet()
        val lastLoginFormatted = user.lastLogin?.let { formatRelativeTime(it) }
        val accountAge = formatRelativeTime(user.createdAt)

        val response =
            UserDetailsResponse(
                user = UserDto.fromDomain(user),
                permissions = permissions,
                lastLoginFormatted = lastLoginFormatted,
                accountAge = accountAge,
            )

        return UserDetailsResult.success(response)
    }

    /** Activates a pending user account. */
    suspend fun activateUser(userId: String): UserStatusResult {
        val user =
            userRepository.findById(UserId.fromString(userId))
                ?: return UserStatusResult.notFound("User not found: $userId")

        if (!user.status.canBeActivated()) {
            return UserStatusResult.failure("Cannot activate user in status: ${user.status}")
        }

        val activatedUser = user.activate()
        userRepository.save(activatedUser)

        return UserStatusResult.success(
            userId = userId,
            email = user.email,
            fullName = user.getFullName(),
            oldStatus = user.status,
            newStatus = activatedUser.status,
            action = "activated",
        )
    }

    /** Suspends an active user account. */
    suspend fun suspendUser(userId: String): UserStatusResult {
        val user =
            userRepository.findById(UserId.fromString(userId))
                ?: return UserStatusResult.notFound("User not found: $userId")

        if (!user.status.canBeSuspended()) {
            return UserStatusResult.failure("Cannot suspend user in status: ${user.status}")
        }

        val suspendedUser = user.suspend()
        userRepository.save(suspendedUser)

        return UserStatusResult.success(
            userId = userId,
            email = user.email,
            fullName = user.getFullName(),
            oldStatus = user.status,
            newStatus = suspendedUser.status,
            action = "suspended",
        )
    }

    /** Initiates a password reset for a user. */
    suspend fun resetPassword(userId: String): PasswordResetResult {
        val user =
            userRepository.findById(UserId.fromString(userId))
                ?: return PasswordResetResult.notFound("User not found: $userId")

        if (user.status == UserStatus.DEACTIVATED) {
            return PasswordResetResult.failure("Cannot reset password for deactivated user")
        }

        // Generate secure reset token
        val resetToken = generateSecureToken()
        val expiresAt = Instant.now().plus(Duration.ofHours(24))

        return PasswordResetResult.success(
            userId = userId,
            email = user.email,
            resetToken = resetToken,
            expiresAt = expiresAt,
        )
    }

    /** Lists users with filtering and pagination. */
    suspend fun listUsers(filter: UserFilter): PagedResponse<UserSummary> = userRepository.findAll(filter)

    private fun generateSecureToken(): String = UUID.randomUUID().toString().replace("-", "")

    private fun formatRelativeTime(instant: Instant): String {
        val duration = Duration.between(instant, Instant.now())
        return when {
            duration.toDays() > 0 -> "${duration.toDays()} days ago"
            duration.toHours() > 0 -> "${duration.toHours()} hours ago"
            duration.toMinutes() > 0 -> "${duration.toMinutes()} minutes ago"
            else -> "Just now"
        }
    }
}

/** Result types for user operations. */
sealed class CreateUserResult {
    data class Success(
        val userId: String,
        val email: String,
        val fullName: String,
        val status: UserStatus,
    ) : CreateUserResult()

    data class Failure(
        val message: String,
    ) : CreateUserResult()

    companion object {
        fun success(
            userId: String,
            email: String,
            fullName: String,
            status: UserStatus,
        ): CreateUserResult = Success(userId, email, fullName, status)

        fun failure(message: String): CreateUserResult = Failure(message)
    }
}

sealed class UserDetailsResult {
    data class Success(
        val details: UserDetailsResponse,
    ) : UserDetailsResult()

    data class NotFound(
        val message: String,
    ) : UserDetailsResult()

    companion object {
        fun success(details: UserDetailsResponse): UserDetailsResult = Success(details)

        fun notFound(message: String): UserDetailsResult = NotFound(message)
    }
}

sealed class UserStatusResult {
    data class Success(
        val userId: String,
        val email: String,
        val fullName: String,
        val oldStatus: UserStatus,
        val newStatus: UserStatus,
        val action: String,
    ) : UserStatusResult()

    data class NotFound(
        val message: String,
    ) : UserStatusResult()

    data class Failure(
        val message: String,
    ) : UserStatusResult()

    companion object {
        fun success(
            userId: String,
            email: String,
            fullName: String,
            oldStatus: UserStatus,
            newStatus: UserStatus,
            action: String,
        ): UserStatusResult = Success(userId, email, fullName, oldStatus, newStatus, action)

        fun notFound(message: String): UserStatusResult = NotFound(message)

        fun failure(message: String): UserStatusResult = Failure(message)
    }
}

sealed class PasswordResetResult {
    data class Success(
        val userId: String,
        val email: String,
        val resetToken: String,
        val expiresAt: Instant,
    ) : PasswordResetResult()

    data class NotFound(
        val message: String,
    ) : PasswordResetResult()

    data class Failure(
        val message: String,
    ) : PasswordResetResult()

    companion object {
        fun success(
            userId: String,
            email: String,
            resetToken: String,
            expiresAt: Instant,
        ): PasswordResetResult = Success(userId, email, resetToken, expiresAt)

        fun notFound(message: String): PasswordResetResult = NotFound(message)

        fun failure(message: String): PasswordResetResult = Failure(message)
    }
}

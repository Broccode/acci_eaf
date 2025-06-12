package com.axians.eaf.iam.application.service

import com.axians.eaf.iam.application.port.inbound.UpdateUserStatusCommand
import com.axians.eaf.iam.application.port.inbound.UpdateUserStatusResult
import com.axians.eaf.iam.application.port.inbound.UpdateUserStatusUseCase
import com.axians.eaf.iam.application.port.outbound.FindUsersByTenantIdPort
import com.axians.eaf.iam.domain.model.UserStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Application service for updating user status within a tenant.
 * This service handles user activation, deactivation, and suspension.
 */
@Service
class UpdateUserStatusService(
    private val findUsersByTenantIdPort: FindUsersByTenantIdPort,
) : UpdateUserStatusUseCase {
    private val logger = LoggerFactory.getLogger(UpdateUserStatusService::class.java)

    override fun updateUserStatus(command: UpdateUserStatusCommand): UpdateUserStatusResult {
        logger.debug(
            "Updating user status for user: {} in tenant: {} to status: {}",
            command.userId,
            command.tenantId,
            command.newStatus,
        )

        // Validate input
        require(command.tenantId.isNotBlank()) { "Tenant ID cannot be blank" }
        require(command.userId.isNotBlank()) { "User ID cannot be blank" }

        // Validate status
        val newStatus =
            try {
                UserStatus.valueOf(command.newStatus)
            } catch (e: IllegalArgumentException) {
                val validStatuses = UserStatus.entries.joinToString(", ") { it.name }
                throw IllegalArgumentException(
                    "Invalid status: ${command.newStatus}. Valid statuses are: $validStatuses",
                )
            }

        // Find the user
        val existingUser =
            findUsersByTenantIdPort.findUserByIdAndTenantId(command.userId, command.tenantId)
                ?: throw IllegalArgumentException(
                    "User with ID ${command.userId} not found in tenant ${command.tenantId}",
                )

        val previousStatus = existingUser.status

        // Apply status change using domain methods
        val updatedUser =
            when (newStatus) {
                UserStatus.ACTIVE -> existingUser.activate()
                UserStatus.INACTIVE -> existingUser.deactivate()
                UserStatus.SUSPENDED -> existingUser.suspend()
                UserStatus.PENDING_ACTIVATION -> existingUser.copy(status = UserStatus.PENDING_ACTIVATION)
            }

        // Save the updated user
        val savedUser = findUsersByTenantIdPort.saveUser(updatedUser)

        logger.info(
            "Successfully updated user status for user: {} in tenant: {} from {} to {}",
            savedUser.userId,
            savedUser.tenantId,
            previousStatus.name,
            savedUser.status.name,
        )

        return UpdateUserStatusResult(
            userId = savedUser.userId,
            tenantId = savedUser.tenantId,
            email = savedUser.email,
            username = savedUser.username,
            previousStatus = previousStatus.name,
            newStatus = savedUser.status.name,
        )
    }
}

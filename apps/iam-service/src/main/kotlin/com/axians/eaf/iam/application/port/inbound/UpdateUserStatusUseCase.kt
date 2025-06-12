package com.axians.eaf.iam.application.port.inbound

import com.axians.eaf.core.hexagonal.port.InboundPort

/**
 * Inbound port for updating a user's status within a tenant.
 * This use case is typically invoked by a TENANT_ADMIN to activate/deactivate users.
 */
interface UpdateUserStatusUseCase : InboundPort {
    /**
     * Update the status of a user within the specified tenant.
     *
     * @param command The command containing user status update details
     * @return The result containing the updated user information
     */
    fun updateUserStatus(command: UpdateUserStatusCommand): UpdateUserStatusResult
}

/**
 * Command for updating a user's status.
 */
data class UpdateUserStatusCommand(
    val tenantId: String,
    val userId: String,
    val newStatus: String,
)

/**
 * Result of user status update operation.
 */
data class UpdateUserStatusResult(
    val userId: String,
    val tenantId: String,
    val email: String,
    val username: String?,
    val previousStatus: String,
    val newStatus: String,
)

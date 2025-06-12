package com.axians.eaf.iam.application.port.inbound

import com.axians.eaf.core.hexagonal.port.InboundPort

/**
 * Inbound port for creating a new user within a tenant.
 * This use case is typically invoked by a TENANT_ADMIN.
 */
interface CreateUserUseCase : InboundPort<CreateUserCommand, CreateUserResult> {
    /**
     * Create a new user within the specified tenant.
     *
     * @param command The command containing user creation details
     * @return The result containing the created user information
     */
    fun createUser(command: CreateUserCommand): CreateUserResult

    /**
     * Handle method required by InboundPort interface.
     */
    override fun handle(command: CreateUserCommand): CreateUserResult = createUser(command)
}

/**
 * Command for creating a new user.
 */
data class CreateUserCommand(
    val tenantId: String,
    val email: String,
    val username: String? = null,
)

/**
 * Result of user creation operation.
 */
data class CreateUserResult(
    val userId: String,
    val tenantId: String,
    val email: String,
    val username: String?,
    val status: String,
)

package com.axians.eaf.iam.application.service

import com.axians.eaf.iam.application.port.inbound.CreateUserCommand
import com.axians.eaf.iam.application.port.inbound.CreateUserResult
import com.axians.eaf.iam.application.port.inbound.CreateUserUseCase
import com.axians.eaf.iam.application.port.outbound.FindUsersByTenantIdPort
import com.axians.eaf.iam.application.port.outbound.SaveTenantPort
import com.axians.eaf.iam.domain.model.User
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Application service for creating users within a tenant. This service orchestrates the user
 * creation process and enforces business rules.
 */
@Service
class CreateUserService(
    private val findUsersByTenantIdPort: FindUsersByTenantIdPort,
    private val saveTenantPort: SaveTenantPort,
) : CreateUserUseCase {
    private val logger = LoggerFactory.getLogger(CreateUserService::class.java)

    override fun createUser(command: CreateUserCommand): CreateUserResult {
        logger.debug("Creating user with email: {} in tenant: {}", command.email, command.tenantId)

        // Validate input
        require(command.tenantId.isNotBlank()) { "Tenant ID cannot be blank" }
        require(command.email.isNotBlank()) { "Email cannot be blank" }

        // Check if email already exists across all tenants
        require(!saveTenantPort.existsByEmail(command.email)) {
            "User with email ${command.email} already exists"
        }

        // Create user using domain factory method
        val user =
            User.createUser(
                tenantId = command.tenantId,
                email = command.email,
                username = command.username,
            )

        // Save user
        val savedUser = findUsersByTenantIdPort.saveUser(user)

        logger.info(
            "Successfully created user with ID: {} for tenant: {}",
            savedUser.userId,
            savedUser.tenantId,
        )

        // Log placeholder for email invitation
        logger.info("TODO: Send invitation email to {} for user activation", savedUser.email)

        return CreateUserResult(
            userId = savedUser.userId,
            tenantId = savedUser.tenantId,
            email = savedUser.email,
            username = savedUser.username,
            status = savedUser.status.name,
        )
    }
}

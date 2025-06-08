package com.axians.eaf.core.example.hexagonal.application.service

import com.axians.eaf.core.example.hexagonal.application.port.inbound.CreateUserCommand
import com.axians.eaf.core.example.hexagonal.application.port.inbound.CreateUserResult
import com.axians.eaf.core.example.hexagonal.application.port.inbound.CreateUserUseCase
import com.axians.eaf.core.example.hexagonal.domain.model.User
import com.axians.eaf.core.example.hexagonal.domain.port.out.UserRepository
import java.util.UUID

/**
 * Example application service that implements the CreateUserUseCase.
 *
 * This demonstrates how application services should be structured in the EAF.
 * The service coordinates between the domain and infrastructure layers,
 * implementing the business logic while remaining independent of infrastructure concerns.
 */
class CreateUserService(
    private val userRepository: UserRepository,
) : CreateUserUseCase {
    override fun handle(command: CreateUserCommand): CreateUserResult {
        // Validate business rules
        validateCommand(command)

        // Check if user already exists
        val existingUser = userRepository.findByUsername(command.username, command.tenantId)
        if (existingUser != null) {
            throw IllegalArgumentException(
                "User with username '${command.username}' already exists in tenant '${command.tenantId}'",
            )
        }

        // Create domain entity
        val user =
            User(
                id = generateUserId(),
                username = command.username,
                email = command.email,
                tenantId = command.tenantId,
            )

        // Persist via outbound port
        val savedUser = userRepository.save(user)

        // Return result
        return CreateUserResult(
            userId = savedUser.id,
            username = savedUser.username,
            email = savedUser.email,
            tenantId = savedUser.tenantId,
        )
    }

    private fun validateCommand(command: CreateUserCommand) {
        require(command.username.isNotBlank()) { "Username cannot be blank" }
        require(command.email.isNotBlank()) { "Email cannot be blank" }
        require(command.tenantId.isNotBlank()) { "Tenant ID cannot be blank" }
        require(command.email.contains("@")) { "Email must be valid" }
    }

    private fun generateUserId(): String = UUID.randomUUID().toString()
}

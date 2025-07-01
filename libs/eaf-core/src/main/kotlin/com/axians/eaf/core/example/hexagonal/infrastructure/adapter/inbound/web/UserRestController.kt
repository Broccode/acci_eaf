package com.axians.eaf.core.example.hexagonal.infrastructure.adapter.inbound.web

import com.axians.eaf.core.example.hexagonal.application.port.inbound.CreateUserCommand
import com.axians.eaf.core.example.hexagonal.application.port.inbound.CreateUserUseCase

/**
 * Example inbound adapter - REST controller.
 *
 * This demonstrates how inbound adapters should be structured in the EAF. The adapter translates
 * HTTP requests into domain commands and delegates to the application core via inbound ports.
 *
 * Note: This is a simplified example. In a real Spring application, this would be annotated with
 * @RestController and use proper Spring Web annotations.
 */
class UserRestController(
    private val createUserUseCase: CreateUserUseCase,
) {
    companion object {
        private const val EXAMPLE_TENANT_ID = "example-tenant-id"
    }

    /**
     * Example REST endpoint for creating a user.
     *
     * In a real implementation, this would be annotated with:
     * @PostMapping("/api/v1/users")
     * @ResponseStatus(HttpStatus.CREATED)
     */
    fun createUser(request: CreateUserRequest): CreateUserResponse {
        // Translate HTTP request to domain command
        val command =
            CreateUserCommand(
                username = request.username,
                email = request.email,
                tenantId =
                EXAMPLE_TENANT_ID, // In real implementation, extract from security
                // context
            )

        // Delegate to application core
        val result = createUserUseCase.handle(command)

        // Translate domain result to HTTP response
        return CreateUserResponse(
            userId = result.userId,
            username = result.username,
            email = result.email,
        )
    }
}

/** Example HTTP request DTO. */
data class CreateUserRequest(
    val username: String,
    val email: String,
)

/** Example HTTP response DTO. */
data class CreateUserResponse(
    val userId: String,
    val username: String,
    val email: String,
)

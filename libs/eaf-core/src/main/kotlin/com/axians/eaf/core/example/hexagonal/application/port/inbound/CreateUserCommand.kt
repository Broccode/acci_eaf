package com.axians.eaf.core.example.hexagonal.application.port.inbound

/**
 * Example command for creating a user.
 * This demonstrates how commands should be structured in the EAF.
 */
data class CreateUserCommand(
    val username: String,
    val email: String,
    val tenantId: String,
)

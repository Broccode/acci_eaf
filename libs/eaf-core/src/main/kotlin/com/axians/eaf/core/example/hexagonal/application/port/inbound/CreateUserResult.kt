package com.axians.eaf.core.example.hexagonal.application.port.inbound

/**
 * Example result for user creation.
 * This demonstrates how results should be structured in the EAF.
 */
data class CreateUserResult(
    val userId: String,
    val username: String,
    val email: String,
    val tenantId: String,
)

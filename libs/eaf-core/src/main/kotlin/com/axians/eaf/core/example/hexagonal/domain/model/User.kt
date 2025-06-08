package com.axians.eaf.core.example.hexagonal.domain.model

/**
 * Example domain entity representing a User.
 * This demonstrates how domain entities should be structured in the EAF.
 */
data class User(
    val id: String,
    val username: String,
    val email: String,
    val tenantId: String,
)

package com.axians.eaf.iam.client

import java.security.Principal

/**
 * Represents the authenticated principal in the EAF system.
 * Contains essential context information extracted from a validated JWT token.
 */
data class EafPrincipal(
    val tenantId: String,
    val userId: String?,
    val username: String?,
    val email: String?,
    val roles: Set<String> = emptySet(),
    val permissions: Set<String> = emptySet(),
) : Principal {
    override fun getName(): String = username ?: email ?: userId ?: "unknown"

    /**
     * Checks if the principal has the specified role.
     */
    fun hasRole(role: String): Boolean = roles.contains(role)

    /**
     * Checks if the principal has the specified permission.
     */
    fun hasPermission(permission: String): Boolean = permissions.contains(permission)

    /**
     * Checks if the principal has any of the specified roles.
     */
    fun hasAnyRole(vararg roles: String): Boolean = roles.any { this.roles.contains(it) }

    /**
     * Checks if the principal has all of the specified roles.
     */
    fun hasAllRoles(vararg roles: String): Boolean = roles.all { this.roles.contains(it) }
}

package com.axians.eaf.iam.application.port.outbound

import com.axians.eaf.core.hexagonal.port.OutboundPort
import com.axians.eaf.iam.domain.model.User

/**
 * Outbound port interface for querying and persisting user data.
 * This defines what the application needs from external persistence systems for user operations.
 */
interface FindUsersByTenantIdPort : OutboundPort {
    /**
     * Find all users belonging to a specific tenant.
     *
     * @param tenantId The ID of the tenant
     * @return List of users in the tenant
     */
    fun findUsersByTenantId(tenantId: String): List<User>

    /**
     * Find a specific user by their ID within a tenant.
     *
     * @param userId The ID of the user
     * @param tenantId The ID of the tenant
     * @return The user if found, null otherwise
     */
    fun findUserByIdAndTenantId(
        userId: String,
        tenantId: String,
    ): User?

    /**
     * Save a user entity.
     *
     * @param user The user to save
     * @return The saved user
     */
    fun saveUser(user: User): User
}

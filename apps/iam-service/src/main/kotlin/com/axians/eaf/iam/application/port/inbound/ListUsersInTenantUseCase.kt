package com.axians.eaf.iam.application.port.inbound

import com.axians.eaf.core.hexagonal.port.InboundPort

/**
 * Inbound port for listing all users within a specific tenant.
 * This use case is typically invoked by a TENANT_ADMIN to view users in their tenant.
 */
interface ListUsersInTenantUseCase : InboundPort<ListUsersInTenantQuery, ListUsersInTenantResult> {
    /**
     * List all users within the specified tenant.
     *
     * @param query The query containing the tenant ID
     * @return The result containing the list of users in the tenant
     */
    fun listUsers(query: ListUsersInTenantQuery): ListUsersInTenantResult

    /**
     * Handle method required by InboundPort interface.
     */
    override fun handle(command: ListUsersInTenantQuery): ListUsersInTenantResult = listUsers(command)
}

/**
 * Query for listing users in a tenant.
 */
data class ListUsersInTenantQuery(
    val tenantId: String,
)

/**
 * Result of listing users in a tenant.
 */
data class ListUsersInTenantResult(
    val tenantId: String,
    val users: List<UserSummary>,
)

/**
 * Summary information about a user.
 */
data class UserSummary(
    val userId: String,
    val email: String,
    val username: String?,
    val role: String,
    val status: String,
)

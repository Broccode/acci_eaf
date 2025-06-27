package com.axians.eaf.iam.application.port.outbound

import com.axians.eaf.core.hexagonal.port.OutboundPort
import com.axians.eaf.iam.domain.model.Tenant
import com.axians.eaf.iam.domain.model.User

/**
 * Outbound port interface for persisting tenant and user data. This defines what the application
 * needs from external persistence systems.
 */
interface SaveTenantPort : OutboundPort {
    /**
     * Save a new tenant and its initial administrator in a transactional operation.
     *
     * @param tenant The tenant to save
     * @param tenantAdmin The initial tenant administrator user
     * @return A result containing the saved entities
     */
    fun saveTenantWithAdmin(
        tenant: Tenant,
        tenantAdmin: User,
    ): SaveTenantResult

    /**
     * Check if a tenant with the given name already exists.
     *
     * @param tenantName The name to check for uniqueness
     * @return true if a tenant with this name exists, false otherwise
     */
    fun existsByTenantName(tenantName: String): Boolean

    /**
     * Check if a user with the given email already exists across all tenants.
     *
     * @param email The email to check for uniqueness
     * @return true if a user with this email exists, false otherwise
     */
    fun existsByEmail(email: String): Boolean
}

/** Result object for tenant and admin save operation. */
data class SaveTenantResult(
    val savedTenant: Tenant,
    val savedTenantAdmin: User,
)

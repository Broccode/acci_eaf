package com.axians.eaf.controlplane.domain.model.tenant

/**
 * Represents the current status of a tenant in the system.
 *
 * - ACTIVE: Tenant is operational and users can access services
 * - SUSPENDED: Tenant access is temporarily disabled
 * - ARCHIVED: Tenant is permanently disabled but data is retained
 */
enum class TenantStatus {
    ACTIVE,
    SUSPENDED,
    ARCHIVED,
    ;

    /** Checks if the tenant is in an operational state. */
    fun isOperational(): Boolean = this == ACTIVE

    /** Checks if the tenant can be reactivated. */
    fun canBeReactivated(): Boolean = this == SUSPENDED
}

package com.axians.eaf.eventsourcing.axon

/**
 * Temporary interface for tenant context management. This will be replaced by the full
 * implementation in Story 4.1.3.
 */
interface TenantContextHolder {
    /**
     * Gets the current tenant ID from the context.
     * @return The current tenant ID or null if no tenant is set
     */
    fun getCurrentTenantId(): String?
}

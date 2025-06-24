package com.axians.eaf.controlplane.domain.port

import com.axians.eaf.controlplane.application.dto.tenant.PagedResponse
import com.axians.eaf.controlplane.application.dto.tenant.TenantFilter
import com.axians.eaf.controlplane.application.dto.tenant.TenantSummary
import com.axians.eaf.controlplane.domain.model.tenant.Tenant
import com.axians.eaf.controlplane.domain.model.tenant.TenantId

/**
 * Repository port for tenant aggregate persistence. This is a domain contract that will be
 * implemented by infrastructure adapters.
 */
interface TenantRepository {
    /**
     * Saves a tenant to the repository. For new tenants, this will create them. For existing ones,
     * this will update them.
     */
    suspend fun save(tenant: Tenant): Tenant

    /** Finds a tenant by its unique identifier. */
    suspend fun findById(tenantId: TenantId): Tenant?

    /** Finds a tenant by name (case-insensitive). */
    suspend fun findByName(name: String): Tenant?

    /** Checks if a tenant exists with the given ID. */
    suspend fun existsById(tenantId: TenantId): Boolean

    /** Checks if a tenant exists with the given name (case-insensitive). */
    suspend fun existsByName(name: String): Boolean

    /** Finds all tenants matching the given filter criteria. */
    suspend fun findAll(filter: TenantFilter): PagedResponse<TenantSummary>

    /** Counts the total number of users for a specific tenant. */
    suspend fun countUsersByTenantId(tenantId: TenantId): Int

    /** Counts the number of active users for a specific tenant. */
    suspend fun countActiveUsersByTenantId(tenantId: TenantId): Int

    /** Gets the timestamp of the last user activity for a tenant. */
    suspend fun getLastUserActivity(tenantId: TenantId): java.time.Instant?

    /** Deletes a tenant by ID (typically only used for testing). */
    suspend fun deleteById(tenantId: TenantId): Boolean
}

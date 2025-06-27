package com.axians.eaf.iam.infrastructure.adapter.outbound.persistence

import com.axians.eaf.iam.infrastructure.adapter.outbound.persistence.entity.TenantEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * JPA Repository for Tenant entities. Provides database operations for tenant management with
 * proper tenant isolation.
 */
@Repository
interface TenantJpaRepository : JpaRepository<TenantEntity, String> {
    /**
     * Check if a tenant with the given name exists (case-sensitive).
     *
     * @param name The tenant name to check
     * @return true if a tenant with this exact name exists, false otherwise
     */
    fun existsByName(name: String): Boolean

    /**
     * Find a tenant by name (case-sensitive).
     *
     * @param name The tenant name to search for
     * @return The tenant if found, null otherwise
     */
    fun findByName(name: String): TenantEntity?
}

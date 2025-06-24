package com.axians.eaf.controlplane.domain.model.tenant

import java.time.Instant

/**
 * Tenant aggregate root representing a customer organization in the EAF platform. Encapsulates all
 * tenant-related business logic and invariants.
 */
data class Tenant(
    val id: TenantId,
    val name: String,
    val status: TenantStatus,
    val settings: TenantSettings,
    val createdAt: Instant,
    val lastModified: Instant,
    val archivedAt: Instant? = null,
) {
    init {
        require(name.isNotBlank()) { "Tenant name cannot be blank" }
        require(name.length <= 100) { "Tenant name cannot exceed 100 characters: ${name.length}" }
        require(lastModified >= createdAt) { "Last modified cannot be before created date" }
        require(status != TenantStatus.ARCHIVED || archivedAt != null) {
            "Archived tenant must have an archive date"
        }
        require(status == TenantStatus.ARCHIVED || archivedAt == null) {
            "Only archived tenants can have an archive date"
        }
    }

    companion object {
        /** Creates a new tenant with the specified configuration. */
        fun create(
            name: String,
            settings: TenantSettings,
            now: Instant = Instant.now(),
        ): Tenant =
            Tenant(
                id = TenantId.generate(),
                name = name.trim(),
                status = TenantStatus.ACTIVE,
                settings = settings,
                createdAt = now,
                lastModified = now,
            )
    }

    /** Updates the tenant's name and settings. */
    fun updateDetails(
        newName: String,
        newSettings: TenantSettings,
        now: Instant = Instant.now(),
    ): Tenant {
        require(status != TenantStatus.ARCHIVED) { "Cannot update archived tenant" }

        return copy(
            name = newName.trim(),
            settings = newSettings,
            lastModified = now,
        )
    }

    /** Suspends the tenant, preventing user access. */
    fun suspend(now: Instant = Instant.now()): Tenant {
        require(status == TenantStatus.ACTIVE) { "Can only suspend active tenants" }

        return copy(
            status = TenantStatus.SUSPENDED,
            lastModified = now,
        )
    }

    /** Reactivates a suspended tenant. */
    fun reactivate(now: Instant = Instant.now()): Tenant {
        require(status == TenantStatus.SUSPENDED) { "Can only reactivate suspended tenants" }

        return copy(
            status = TenantStatus.ACTIVE,
            lastModified = now,
        )
    }

    /** Archives the tenant permanently. */
    fun archive(now: Instant = Instant.now()): Tenant {
        require(status != TenantStatus.ARCHIVED) { "Tenant is already archived" }

        return copy(
            status = TenantStatus.ARCHIVED,
            archivedAt = now,
            lastModified = now,
        )
    }

    /** Checks if the tenant is operational (can be used by users). */
    fun isOperational(): Boolean = status.isOperational()

    /** Checks if the tenant can be modified. */
    fun canBeModified(): Boolean = status != TenantStatus.ARCHIVED

    /** Gets the tenant's primary domain from settings. */
    fun getPrimaryDomain(): String = settings.allowedDomains.first()
}

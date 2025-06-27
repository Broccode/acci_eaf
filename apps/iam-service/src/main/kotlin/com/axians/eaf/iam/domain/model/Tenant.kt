package com.axians.eaf.iam.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Tenant aggregate root representing a multi-tenant organization within the EAF system. This entity
 * contains pure business logic and has no infrastructure dependencies.
 */
data class Tenant(
    val tenantId: String = UUID.randomUUID().toString(),
    val name: String,
    val status: TenantStatus = TenantStatus.ACTIVE,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    companion object {
        /** Factory method to create a new tenant with proper domain validation. */
        fun create(name: String): Tenant {
            require(name.isNotBlank()) { "Tenant name cannot be blank" }
            require(name.length <= 255) { "Tenant name cannot exceed 255 characters" }

            return Tenant(
                name = name.trim(),
            )
        }
    }

    /** Domain method to activate the tenant. */
    fun activate(): Tenant =
        copy(
            status = TenantStatus.ACTIVE,
            updatedAt = Instant.now(),
        )

    /** Domain method to deactivate the tenant. */
    fun deactivate(): Tenant =
        copy(
            status = TenantStatus.INACTIVE,
            updatedAt = Instant.now(),
        )

    /** Domain method to check if tenant is active. */
    fun isActive(): Boolean = status == TenantStatus.ACTIVE
}

/** Enum representing the possible states of a tenant. */
enum class TenantStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
}

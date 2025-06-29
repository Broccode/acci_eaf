package com.axians.eaf.core.tenancy

import java.time.Instant

/**
 * Data class to hold tenant metadata and context information.
 *
 * Provides additional tenant information beyond just the tenant ID, useful for enhanced tenant
 * context management and auditing.
 */
data class TenantContext(
    /** The unique tenant identifier. */
    val tenantId: String,
    /** The display name of the tenant. */
    val tenantName: String? = null,
    /** The tenant's organization or company name. */
    val organizationName: String? = null,
    /** The tenant's region or data center location. */
    val region: String? = null,
    /** The tenant's subscription tier or plan. */
    val subscriptionTier: String? = null,
    /** When this tenant context was established. */
    val establishedAt: Instant = Instant.now(),
    /** Additional metadata as key-value pairs. */
    val metadata: Map<String, String> = emptyMap(),
) {
    /** Creates a minimal TenantContext with just the tenant ID. */
    constructor(
        tenantId: String,
    ) : this(
        tenantId = tenantId,
        tenantName = null,
        organizationName = null,
        region = null,
        subscriptionTier = null,
        establishedAt = Instant.now(),
        metadata = emptyMap(),
    )

    /** Validates that the tenant context has all required fields. */
    fun validate() {
        require(tenantId.isNotBlank()) { "Tenant ID cannot be blank" }
        require(tenantId.length <= 64) { "Tenant ID cannot exceed 64 characters" }
        require(tenantId.matches(Regex("[a-zA-Z0-9_-]+"))) {
            "Tenant ID can only contain alphanumeric characters, underscores, and hyphens"
        }
    }

    /** Returns a copy of this context with additional metadata. */
    fun withMetadata(
        key: String,
        value: String,
    ): TenantContext = copy(metadata = metadata + (key to value))

    /** Returns a copy of this context with additional metadata. */
    fun withMetadata(additionalMetadata: Map<String, String>): TenantContext =
        copy(metadata = metadata + additionalMetadata)
}

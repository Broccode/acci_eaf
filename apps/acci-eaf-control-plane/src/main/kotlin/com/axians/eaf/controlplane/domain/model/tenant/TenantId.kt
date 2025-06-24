package com.axians.eaf.controlplane.domain.model.tenant

import java.util.UUID

/**
 * Value object representing a unique tenant identifier. Ensures type safety and prevents primitive
 * obsession.
 */
@JvmInline
value class TenantId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Tenant ID cannot be blank" }
        require(isValidUuid(value)) { "Tenant ID must be a valid UUID: $value" }
    }

    companion object {
        fun generate(): TenantId = TenantId(UUID.randomUUID().toString())

        fun fromString(value: String): TenantId = TenantId(value)

        private fun isValidUuid(value: String): Boolean =
            try {
                UUID.fromString(value)
                true
            } catch (e: IllegalArgumentException) {
                false
            }
    }

    override fun toString(): String = value
}

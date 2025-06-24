package com.axians.eaf.controlplane.domain.model.user

import java.util.UUID

/**
 * Value object representing a unique user identifier. Ensures type safety and prevents primitive
 * obsession.
 */
@JvmInline
value class UserId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "User ID cannot be blank" }
        require(isValidUuid(value)) { "User ID must be a valid UUID: $value" }
    }

    companion object {
        fun generate(): UserId = UserId(UUID.randomUUID().toString())

        fun fromString(value: String): UserId = UserId(value)

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

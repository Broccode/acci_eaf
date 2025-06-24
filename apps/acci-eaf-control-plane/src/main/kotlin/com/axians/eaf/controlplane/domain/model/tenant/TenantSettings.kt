package com.axians.eaf.controlplane.domain.model.tenant

/** Configuration settings for a tenant. Contains operational limits and feature toggles. */
data class TenantSettings(
    val maxUsers: Int,
    val allowedDomains: List<String>,
    val features: Set<String>,
    val customSettings: Map<String, Any> = emptyMap(),
) {
    init {
        require(maxUsers > 0) { "Max users must be greater than 0, got: $maxUsers" }
        require(allowedDomains.isNotEmpty()) { "At least one allowed domain must be specified" }
        require(allowedDomains.all { it.isNotBlank() }) { "All allowed domains must be non-blank" }
        require(allowedDomains.all { isValidDomain(it) }) {
            "All domains must be valid: $allowedDomains"
        }
    }

    companion object {
        /** Creates default tenant settings for new tenants. */
        fun default(primaryDomain: String): TenantSettings =
            TenantSettings(
                maxUsers = 100,
                allowedDomains = listOf(primaryDomain),
                features = setOf("user-management", "basic-reporting"),
            )

        private fun isValidDomain(domain: String): Boolean {
            // Basic domain validation - can be enhanced as needed
            val domainRegex =
                "^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?))*$"
                    .toRegex()
            return domain.matches(domainRegex) && domain.length <= 253
        }
    }

    /** Checks if a specific feature is enabled for this tenant. */
    fun hasFeature(featureName: String): Boolean = features.contains(featureName)

    /** Creates a copy with an additional feature enabled. */
    fun withFeature(featureName: String): TenantSettings = copy(features = features + featureName)

    /** Creates a copy with a feature disabled. */
    fun withoutFeature(featureName: String): TenantSettings = copy(features = features - featureName)
}

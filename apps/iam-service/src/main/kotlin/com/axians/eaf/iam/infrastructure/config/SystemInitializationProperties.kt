package com.axians.eaf.iam.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** Configuration properties for system initialization settings. */
@ConfigurationProperties(prefix = "eaf.system")
data class SystemInitializationProperties(
    /**
     * Whether to initialize the system with a default tenant and superadmin user on startup. This
     * is useful for single-tenant deployments or initial system setup.
     */
    val initializeDefaultTenant: Boolean = false,
    /** The name of the default tenant to create. */
    val defaultTenantName: String = "DefaultTenant",
    /** The email address for the default superadmin user. */
    val defaultSuperAdminEmail: String = "admin@example.com",
)

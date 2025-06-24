package com.axians.eaf.controlplane.infrastructure.configuration.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "eaf.control-plane")
data class ControlPlaneProperties(
    val features: FeatureProperties,
    val security: SecurityProperties,
)

data class FeatureProperties(
    val tenantCreation: Boolean = true,
    val userManagement: Boolean = true,
    val licenseOverview: Boolean = true,
)

data class SecurityProperties(
    val sessionTimeout: Duration = Duration.ofMinutes(30),
    val maxLoginAttempts: Int = 5,
    val requireSsl: Boolean = false,
    val contentSecurityPolicy: String = "default-src 'self'",
)

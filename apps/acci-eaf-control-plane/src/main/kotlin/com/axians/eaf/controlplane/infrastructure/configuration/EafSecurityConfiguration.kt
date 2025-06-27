package com.axians.eaf.controlplane.infrastructure.configuration

import com.axians.eaf.core.security.EafSecurityContextHolder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

/**
 * Security configuration for multi-tenant access control and monitoring.
 *
 * This configuration enables:
 * 1. AspectJ AOP for security aspects
 * 2. Method-level security annotations
 * 3. Multi-tenant access control
 * 4. Security monitoring and audit integration
 */
@Configuration
@EnableAspectJAutoProxy
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
class EafSecurityConfiguration {
    /**
     * Creates an EafSecurityContextHolder bean if none exists. This provides a fallback
     * implementation for testing and development.
     */
    @Bean
    @ConditionalOnMissingBean(EafSecurityContextHolder::class)
    fun eafSecurityContextHolder(): EafSecurityContextHolder =
        com.axians.eaf.core.security
            .DefaultEafSecurityContextHolder()

    /** Security properties for configuring various security features. */
    @Bean fun securityProperties(): SecurityProperties = SecurityProperties()
}

/** Configuration properties for security features. */
data class SecurityProperties(
    val sessionTimeoutMinutes: Int = 30,
    val maxLoginAttempts: Int = 5,
    val enableAuditLogging: Boolean = true,
    val enableSecurityMonitoring: Boolean = true,
    val requireSsl: Boolean = false,
    // Authentication security properties
    val authLockoutDurationMinutes: Long = 15,
    val maxFailedAuthAttempts: Int = 5,
    // Rate limiting properties
    val defaultRateLimitMaxCalls: Int = 100,
    val defaultRateLimitWindowSeconds: Long = 60,
    // Monitoring configuration
    val monitoring: MonitoringProperties = MonitoringProperties(),
)

data class MonitoringProperties(
    val suspiciousActivityWindowMinutes: Long = 30,
    val enableSecurityAlerts: Boolean = true,
)

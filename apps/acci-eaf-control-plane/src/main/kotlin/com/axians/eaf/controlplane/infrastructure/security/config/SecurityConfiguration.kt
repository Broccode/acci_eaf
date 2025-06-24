package com.axians.eaf.controlplane.infrastructure.security.config

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
class SecurityConfiguration {
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
    @Bean
    fun securityProperties(): SecurityProperties = SecurityProperties()
}

/** Configuration properties for security features. */
data class SecurityProperties(
    /** Whether to enable tenant isolation validation */
    val enableTenantIsolation: Boolean = true,
    /** Whether to enable security monitoring and alerting */
    val enableSecurityMonitoring: Boolean = true,
    /** Whether to enable audit logging for security events */
    val enableSecurityAuditLogging: Boolean = true,
    /** Maximum number of failed authentication attempts before lockout */
    val maxFailedAuthAttempts: Int = 5,
    /** Duration in minutes for authentication lockout */
    val authLockoutDurationMinutes: Long = 15,
    /** Whether to enable rate limiting for security-sensitive endpoints */
    val enableRateLimiting: Boolean = true,
    /** Default rate limit: max calls per time window */
    val defaultRateLimitMaxCalls: Int = 100,
    /** Default rate limit time window in seconds */
    val defaultRateLimitWindowSeconds: Long = 60,
    /** Whether to block requests when rate limit is exceeded */
    val blockOnRateLimitExceeded: Boolean = true,
    /** Whether to require HTTPS for sensitive operations */
    val requireHttpsForSensitiveOps: Boolean = true,
    /** List of IP addresses that are always allowed (for monitoring, health checks, etc.) */
    val allowedIpAddresses: Set<String> = setOf("127.0.0.1", "::1"),
    /** List of IP addresses that are always blocked */
    val blockedIpAddresses: Set<String> = emptySet(),
    /** Whether to enable detailed security logging */
    val enableDetailedSecurityLogging: Boolean = false,
    /** Security monitoring configuration */
    val monitoring: SecurityMonitoringProperties = SecurityMonitoringProperties(),
    /** Tenant isolation configuration */
    val tenantIsolation: TenantIsolationProperties = TenantIsolationProperties(),
)

/** Configuration properties for security monitoring. */
data class SecurityMonitoringProperties(
    /** Whether to send alerts for security violations */
    val enableSecurityAlerts: Boolean = true,
    /** Whether to send alerts for failed authentication attempts */
    val alertOnFailedAuth: Boolean = true,
    /** Whether to send alerts for permission escalation attempts */
    val alertOnPermissionEscalation: Boolean = true,
    /** Whether to send alerts for suspicious activity */
    val alertOnSuspiciousActivity: Boolean = true,
    /** Threshold for triggering suspicious activity alerts */
    val suspiciousActivityThreshold: Int = 10,
    /** Time window in minutes for suspicious activity detection */
    val suspiciousActivityWindowMinutes: Long = 5,
    /** Whether to include request details in security alerts */
    val includeRequestDetailsInAlerts: Boolean = true,
)

/** Configuration properties for tenant isolation. */
data class TenantIsolationProperties(
    /** Whether to enforce strict tenant isolation */
    val enforceStrictIsolation: Boolean = true,
    /** Whether to allow global administrators to bypass tenant restrictions */
    val allowGlobalAdminBypass: Boolean = true,
    /** List of roles that have global access privileges */
    val globalAccessRoles: Set<String> = setOf("SUPER_ADMIN", "PLATFORM_ADMIN"),
    /** Whether to log all tenant access attempts */
    val logAllTenantAccess: Boolean = true,
    /** Whether to validate tenant status before granting access */
    val validateTenantStatus: Boolean = true,
    /** Whether to block access to archived tenants */
    val blockArchivedTenants: Boolean = true,
    /** Whether to block access to suspended tenants */
    val blockSuspendedTenants: Boolean = true,
)

package com.axians.eaf.controlplane.infrastructure.configuration.monitoring

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.event.AbstractAuthenticationEvent
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** Configuration for security monitoring including authentication and authorization events */
@Configuration
class SecurityMonitoringConfiguration {
    @Bean
    fun securityEventListener(
        meterRegistry: MeterRegistry,
        securityAuditLogger: SecurityAuditLogger,
    ): SecurityEventListener = SecurityEventListener(meterRegistry, securityAuditLogger)

    @Bean fun securityAuditLogger(): SecurityAuditLogger = SecurityAuditLogger()

    @Bean
    fun securityMetrics(meterRegistry: MeterRegistry): SecurityMetrics = SecurityMetrics(meterRegistry)
}

/** Listens to Spring Security events and records metrics and audit logs */
@Component
class SecurityEventListener(
    private val meterRegistry: MeterRegistry,
    private val securityAuditLogger: SecurityAuditLogger,
) : ApplicationListener<AbstractAuthenticationEvent> {
    private val loginAttemptsCounter =
        Counter
            .builder("eaf.controlplane.security.login.attempts.total")
            .description("Total login attempts")
            .register(meterRegistry)

    private val loginSuccessCounter =
        Counter
            .builder("eaf.controlplane.security.login.success.total")
            .description("Successful login attempts")
            .register(meterRegistry)

    private val loginFailureCounter =
        Counter
            .builder("eaf.controlplane.security.login.failures.total")
            .description("Failed login attempts")
            .register(meterRegistry)

    private val loginDurationTimer =
        Timer
            .builder("eaf.controlplane.security.login.duration")
            .description("Time taken for login process")
            .register(meterRegistry)

    override fun onApplicationEvent(event: AbstractAuthenticationEvent) {
        when (event) {
            is AuthenticationSuccessEvent -> handleSuccessfulLogin(event)
            is AuthenticationFailureBadCredentialsEvent -> handleFailedLogin(event)
        }
    }

    private fun handleSuccessfulLogin(event: AuthenticationSuccessEvent) {
        val authentication = event.authentication
        val username = authentication.name
        val clientIp = getClientIp()
        val tenantId = extractTenantId(authentication)

        // Record metrics
        Counter
            .builder("eaf.controlplane.security.login.attempts.total")
            .tag("status", "success")
            .tag("tenantId", tenantId)
            .register(meterRegistry)
            .increment()
        Counter
            .builder("eaf.controlplane.security.login.success.total")
            .tag("username", username)
            .tag("tenantId", tenantId)
            .register(meterRegistry)
            .increment()

        // Audit logging
        securityAuditLogger.logSuccessfulLogin(username, clientIp, tenantId)
    }

    private fun handleFailedLogin(event: AuthenticationFailureBadCredentialsEvent) {
        val authentication = event.authentication
        val username = authentication.name
        val clientIp = getClientIp()
        val tenantId = extractTenantId(authentication)

        // Record metrics
        Counter
            .builder("eaf.controlplane.security.login.attempts.total")
            .tag("status", "failure")
            .tag("tenantId", tenantId)
            .register(meterRegistry)
            .increment()
        Counter
            .builder("eaf.controlplane.security.login.failures.total")
            .tag("username", username)
            .tag("tenantId", tenantId)
            .tag("reason", "bad_credentials")
            .register(meterRegistry)
            .increment()

        // Audit logging
        securityAuditLogger.logFailedLogin(username, clientIp, tenantId, "Bad credentials")
    }

    private fun getClientIp(): String {
        // This would need to be injected from the HTTP request context
        // For now, return unknown
        return "unknown"
    }

    private fun extractTenantId(authentication: Authentication): String {
        // Extract tenant ID from authentication context
        // This depends on how EAF IAM integrates tenant context
        return "unknown"
    }
}

/** Structured audit logging for security events */
@Component
class SecurityAuditLogger {
    private val logger = LoggerFactory.getLogger("SECURITY_AUDIT")

    fun logSuccessfulLogin(
        username: String,
        clientIp: String,
        tenantId: String,
    ) {
        MDC.put("eventType", "LOGIN_SUCCESS")
        MDC.put("username", username)
        MDC.put("clientIp", clientIp)
        MDC.put("tenantId", tenantId)
        MDC.put("timestamp", Instant.now().toString())

        logger.info("Successful login: user={}, ip={}, tenant={}", username, clientIp, tenantId)

        MDC.clear()
    }

    fun logFailedLogin(
        username: String,
        clientIp: String,
        tenantId: String,
        reason: String,
    ) {
        MDC.put("eventType", "LOGIN_FAILURE")
        MDC.put("username", username)
        MDC.put("clientIp", clientIp)
        MDC.put("tenantId", tenantId)
        MDC.put("reason", reason)
        MDC.put("timestamp", Instant.now().toString())

        logger.warn(
            "Failed login attempt: user={}, ip={}, tenant={}, reason={}",
            username,
            clientIp,
            tenantId,
            reason,
        )

        MDC.clear()
    }

    fun logAuthorizationDenied(
        username: String,
        resource: String,
        action: String,
        tenantId: String,
    ) {
        MDC.put("eventType", "AUTHORIZATION_DENIED")
        MDC.put("username", username)
        MDC.put("resource", resource)
        MDC.put("action", action)
        MDC.put("tenantId", tenantId)
        MDC.put("timestamp", Instant.now().toString())

        logger.warn(
            "Authorization denied: user={}, resource={}, action={}, tenant={}",
            username,
            resource,
            action,
            tenantId,
        )

        MDC.clear()
    }

    fun logPasswordReset(
        username: String,
        adminUser: String,
        tenantId: String,
    ) {
        MDC.put("eventType", "PASSWORD_RESET")
        MDC.put("username", username)
        MDC.put("adminUser", adminUser)
        MDC.put("tenantId", tenantId)
        MDC.put("timestamp", Instant.now().toString())

        logger.info("Password reset: user={}, admin={}, tenant={}", username, adminUser, tenantId)

        MDC.clear()
    }

    fun logRoleAssignment(
        username: String,
        role: String,
        adminUser: String,
        tenantId: String,
    ) {
        MDC.put("eventType", "ROLE_ASSIGNMENT")
        MDC.put("username", username)
        MDC.put("role", role)
        MDC.put("adminUser", adminUser)
        MDC.put("tenantId", tenantId)
        MDC.put("timestamp", Instant.now().toString())

        logger.info(
            "Role assignment: user={}, role={}, admin={}, tenant={}",
            username,
            role,
            adminUser,
            tenantId,
        )

        MDC.clear()
    }
}

/** Security-specific metrics for monitoring authentication and authorization patterns */
@Component
class SecurityMetrics(
    private val meterRegistry: MeterRegistry,
) {
    // Rate limiting and abuse detection
    private val rateLimitViolationsCounter =
        Counter
            .builder("eaf.controlplane.security.rate.limit.violations.total")
            .description("Rate limit violations detected")
            .register(meterRegistry)

    private val suspiciousActivityCounter =
        Counter
            .builder("eaf.controlplane.security.suspicious.activity.total")
            .description("Suspicious activity patterns detected")
            .register(meterRegistry)

    // Session management
    private val sessionCreatedCounter =
        Counter
            .builder("eaf.controlplane.security.sessions.created.total")
            .description("Total sessions created")
            .register(meterRegistry)

    private val sessionInvalidatedCounter =
        Counter
            .builder("eaf.controlplane.security.sessions.invalidated.total")
            .description("Total sessions invalidated")
            .register(meterRegistry)

    // Track login attempts per IP for abuse detection
    private val loginAttemptsByIp = ConcurrentHashMap<String, AtomicLong>()

    fun recordRateLimitViolation(
        clientIp: String,
        endpoint: String,
    ) {
        Counter
            .builder("eaf.controlplane.security.rate.limit.violations.total")
            .tag("clientIp", clientIp)
            .tag("endpoint", endpoint)
            .register(meterRegistry)
            .increment()
    }

    fun recordSuspiciousActivity(
        activityType: String,
        clientIp: String,
        username: String?,
    ) {
        Counter
            .builder("eaf.controlplane.security.suspicious.activity.total")
            .tag("activityType", activityType)
            .tag("clientIp", clientIp)
            .tag("username", username ?: "unknown")
            .register(meterRegistry)
            .increment()
    }

    fun recordSessionCreated(
        username: String,
        tenantId: String,
    ) {
        Counter
            .builder("eaf.controlplane.security.sessions.created.total")
            .tag("username", username)
            .tag("tenantId", tenantId)
            .register(meterRegistry)
            .increment()
    }

    fun recordSessionInvalidated(
        username: String,
        tenantId: String,
        reason: String,
    ) {
        Counter
            .builder("eaf.controlplane.security.sessions.invalidated.total")
            .tag("username", username)
            .tag("tenantId", tenantId)
            .tag("reason", reason)
            .register(meterRegistry)
            .increment()
    }

    fun incrementLoginAttempts(clientIp: String): Long =
        loginAttemptsByIp.computeIfAbsent(clientIp) { AtomicLong(0) }.incrementAndGet()

    fun resetLoginAttempts(clientIp: String) {
        loginAttemptsByIp.remove(clientIp)
    }

    fun getLoginAttempts(clientIp: String): Long = loginAttemptsByIp[clientIp]?.get() ?: 0
}

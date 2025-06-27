package com.axians.eaf.controlplane.infrastructure.configuration.monitoring

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

/**
 * Configuration for business metrics tracking administrative actions and operational KPIs for the
 * Control Plane.
 */
@Configuration
class BusinessMetricsConfiguration {
    /** Business metrics collector for Control Plane administrative operations */
    @Bean
    fun controlPlaneMetrics(meterRegistry: MeterRegistry): ControlPlaneMetrics = ControlPlaneMetrics(meterRegistry)
}

/**
 * Collects and exposes business metrics for Control Plane operations. These metrics help monitor
 * administrative activities and system health.
 */
@Component
class ControlPlaneMetrics(
    private val meterRegistry: MeterRegistry,
) {
    // Tenant Management Metrics
    private val tenantsCreatedCounter =
        Counter
            .builder("eaf.controlplane.tenants.created.total")
            .description("Total number of tenants created")
            .register(meterRegistry)

    private val tenantsDeletedCounter =
        Counter
            .builder("eaf.controlplane.tenants.deleted.total")
            .description("Total number of tenants deleted")
            .register(meterRegistry)

    private val tenantCreationTimer =
        Timer
            .builder("eaf.controlplane.tenants.creation.duration")
            .description("Time taken to create a tenant")
            .register(meterRegistry)

    // User Management Metrics
    private val usersCreatedCounter =
        Counter
            .builder("eaf.controlplane.users.created.total")
            .description("Total number of users created")
            .register(meterRegistry)

    private val usersDeletedCounter =
        Counter
            .builder("eaf.controlplane.users.deleted.total")
            .description("Total number of users deleted")
            .register(meterRegistry)

    private val passwordResetsCounter =
        Counter
            .builder("eaf.controlplane.users.password.resets.total")
            .description("Total number of password resets performed")
            .register(meterRegistry)

    // Authentication & Security Metrics
    private val loginAttemptsCounter =
        Counter
            .builder("eaf.controlplane.auth.login.attempts.total")
            .description("Total login attempts")
            .tag("status", "unknown")
            .register(meterRegistry)

    private val loginSuccessCounter =
        Counter
            .builder("eaf.controlplane.auth.login.success.total")
            .description("Successful login attempts")
            .register(meterRegistry)

    private val loginFailureCounter =
        Counter
            .builder("eaf.controlplane.auth.login.failures.total")
            .description("Failed login attempts")
            .register(meterRegistry)

    private val sessionTimeoutCounter =
        Counter
            .builder("eaf.controlplane.auth.session.timeouts.total")
            .description("Total session timeouts")
            .register(meterRegistry)

    // Administrative Actions Metrics
    private val roleAssignmentsCounter =
        Counter
            .builder("eaf.controlplane.roles.assignments.total")
            .description("Total role assignments performed")
            .register(meterRegistry)

    private val configurationChangesCounter =
        Counter
            .builder("eaf.controlplane.config.changes.total")
            .description("Total configuration changes made")
            .register(meterRegistry)

    private val auditLogEntriesCounter =
        Counter
            .builder("eaf.controlplane.audit.entries.total")
            .description("Total audit log entries created")
            .register(meterRegistry)

    // System Performance Metrics
    private val activeTenants = AtomicLong(0)
    private val activeUsers = AtomicLong(0)
    private val activeSessions = AtomicLong(0)

    init {
    /*
    TODO: Fix gauge registration - temporarily disabled to resolve compilation

    Register gauges for current system state using simple function references

    Gauge.builder("eaf.controlplane.tenants.active.current")
            .description("Current number of active tenants")
            .register(meterRegistry) { activeTenants.get().toDouble() }

    Gauge.builder("eaf.controlplane.users.active.current")
            .description("Current number of active users")
            .register(meterRegistry) { activeUsers.get().toDouble() }

    Gauge.builder("eaf.controlplane.sessions.active.current")
            .description("Current number of active sessions")
            .register(meterRegistry) { activeSessions.get().toDouble() }
     */
    }

    // Tenant Management Operations
    fun recordTenantCreated(
        tenantId: String,
        adminUserId: String,
    ) {
        Counter
            .builder("eaf.controlplane.tenants.created.total")
            .tag("tenantId", tenantId)
            .tag("adminUserId", adminUserId)
            .register(meterRegistry)
            .increment()
        activeTenants.incrementAndGet()
    }

    fun recordTenantDeleted(
        tenantId: String,
        adminUserId: String,
    ) {
        Counter
            .builder("eaf.controlplane.tenants.deleted.total")
            .tag("tenantId", tenantId)
            .tag("adminUserId", adminUserId)
            .register(meterRegistry)
            .increment()
        activeTenants.decrementAndGet()
    }

    fun recordTenantCreationTime(durationMillis: Long) {
        tenantCreationTimer.record(durationMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
    }

    // User Management Operations
    fun recordUserCreated(
        tenantId: String,
        userId: String,
        adminUserId: String,
    ) {
        Counter
            .builder("eaf.controlplane.users.created.total")
            .tag("tenantId", tenantId)
            .tag("userId", userId)
            .tag("adminUserId", adminUserId)
            .register(meterRegistry)
            .increment()
        activeUsers.incrementAndGet()
    }

    fun recordUserDeleted(
        tenantId: String,
        userId: String,
        adminUserId: String,
    ) {
        Counter
            .builder("eaf.controlplane.users.deleted.total")
            .tag("tenantId", tenantId)
            .tag("userId", userId)
            .tag("adminUserId", adminUserId)
            .register(meterRegistry)
            .increment()
        activeUsers.decrementAndGet()
    }

    fun recordPasswordReset(
        tenantId: String,
        userId: String,
        adminUserId: String,
    ) {
        Counter
            .builder("eaf.controlplane.users.password.resets.total")
            .tag("tenantId", tenantId)
            .tag("userId", userId)
            .tag("adminUserId", adminUserId)
            .register(meterRegistry)
            .increment()
    }

    // Authentication & Security Operations
    fun recordLoginAttempt(
        tenantId: String?,
        username: String,
        success: Boolean,
        clientIp: String,
    ) {
        Counter
            .builder("eaf.controlplane.auth.login.attempts.total")
            .tag("tenantId", tenantId ?: "unknown")
            .tag("username", username)
            .tag("status", if (success) "success" else "failure")
            .tag("clientIp", clientIp)
            .register(meterRegistry)
            .increment()

        if (success) {
            Counter
                .builder("eaf.controlplane.auth.login.success.total")
                .tag("tenantId", tenantId ?: "unknown")
                .tag("username", username)
                .register(meterRegistry)
                .increment()
            activeSessions.incrementAndGet()
        } else {
            Counter
                .builder("eaf.controlplane.auth.login.failures.total")
                .tag("tenantId", tenantId ?: "unknown")
                .tag("username", username)
                .tag("clientIp", clientIp)
                .register(meterRegistry)
                .increment()
        }
    }

    fun recordSessionTimeout(
        tenantId: String,
        userId: String,
    ) {
        Counter
            .builder("eaf.controlplane.auth.session.timeouts.total")
            .tag("tenantId", tenantId)
            .tag("userId", userId)
            .register(meterRegistry)
            .increment()
        activeSessions.decrementAndGet()
    }

    // Administrative Actions
    fun recordRoleAssignment(
        tenantId: String,
        userId: String,
        role: String,
        adminUserId: String,
    ) {
        Counter
            .builder("eaf.controlplane.roles.assignments.total")
            .tag("tenantId", tenantId)
            .tag("userId", userId)
            .tag("role", role)
            .tag("adminUserId", adminUserId)
            .register(meterRegistry)
            .increment()
    }

    fun recordConfigurationChange(
        configKey: String,
        adminUserId: String,
        tenantId: String? = null,
    ) {
        Counter
            .builder("eaf.controlplane.config.changes.total")
            .tag("configKey", configKey)
            .tag("adminUserId", adminUserId)
            .tag("tenantId", tenantId ?: "global")
            .register(meterRegistry)
            .increment()
    }

    fun recordAuditLogEntry(
        action: String,
        tenantId: String?,
        userId: String,
        resourceType: String,
    ) {
        Counter
            .builder("eaf.controlplane.audit.entries.total")
            .tag("action", action)
            .tag("tenantId", tenantId ?: "unknown")
            .tag("userId", userId)
            .tag("resourceType", resourceType)
            .register(meterRegistry)
            .increment()
    }

    // Gauge Updates for Current State
    fun updateActiveTenants(count: Long) {
        activeTenants.set(count)
    }

    fun updateActiveUsers(count: Long) {
        activeUsers.set(count)
    }

    fun updateActiveSessions(count: Long) {
        activeSessions.set(count)
    }
}

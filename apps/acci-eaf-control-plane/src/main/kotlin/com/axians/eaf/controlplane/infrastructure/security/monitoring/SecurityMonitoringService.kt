package com.axians.eaf.controlplane.infrastructure.security.monitoring

import com.axians.eaf.controlplane.domain.model.audit.AdminAction
import com.axians.eaf.controlplane.domain.service.AuditService
import com.axians.eaf.controlplane.infrastructure.security.config.SecurityProperties
import com.axians.eaf.core.security.EafSecurityContextHolder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Service for monitoring security events and detecting suspicious activities.
 *
 * This service tracks various security metrics and can detect patterns that indicate potential
 * security threats or policy violations.
 */
@Service
class SecurityMonitoringService(
    private val securityProperties: SecurityProperties,
    private val auditService: AuditService,
    private val securityContextHolder: EafSecurityContextHolder,
) {
    private val logger = LoggerFactory.getLogger(SecurityMonitoringService::class.java)

    // In-memory storage for tracking security events (in production, this could be Redis or a
    // database)
    private val failedAuthAttempts = ConcurrentHashMap<String, MutableList<Instant>>()
    private val suspiciousActivities = ConcurrentHashMap<String, AtomicLong>()
    private val rateLimitTracking = ConcurrentHashMap<String, MutableList<Instant>>()
    private val tenantAccessAttempts = ConcurrentHashMap<String, MutableList<TenantAccessAttempt>>()

    /** Records a failed authentication attempt and checks for brute force attacks. */
    fun recordFailedAuthAttempt(
        identifier: String,
        ipAddress: String? = null,
    ): SecurityThreatLevel {
        val now = Instant.now()
        val key = identifier // Could be username, IP address, or combination

        val attempts = failedAuthAttempts.computeIfAbsent(key) { mutableListOf() }

        synchronized(attempts) {
            // Clean up old attempts (older than lockout duration)
            val cutoff =
                now.minus(securityProperties.authLockoutDurationMinutes, ChronoUnit.MINUTES)
            attempts.removeIf { it.isBefore(cutoff) }

            // Add current attempt
            attempts.add(now)

            val recentAttempts = attempts.size

            logger.warn(
                "Failed authentication attempt #{} for identifier: {}, IP: {}",
                recentAttempts,
                identifier,
                ipAddress ?: "unknown",
            )

            // Determine threat level based on number of attempts
            val threatLevel =
                when {
                    recentAttempts >= securityProperties.maxFailedAuthAttempts * 2 ->
                        SecurityThreatLevel.CRITICAL
                    recentAttempts >= securityProperties.maxFailedAuthAttempts ->
                        SecurityThreatLevel.HIGH
                    recentAttempts >= securityProperties.maxFailedAuthAttempts / 2 ->
                        SecurityThreatLevel.MEDIUM
                    else -> SecurityThreatLevel.LOW
                }

            // Log security event
            logSecurityEvent(
                eventType = "FAILED_AUTH_ATTEMPT",
                identifier = identifier,
                threatLevel = threatLevel,
                details =
                    mapOf(
                        "attemptCount" to recentAttempts,
                        "ipAddress" to (ipAddress ?: "unknown"),
                        "timestamp" to now.toString(),
                    ),
            )

            // Send alert if threshold exceeded
            if (recentAttempts >= securityProperties.maxFailedAuthAttempts) {
                sendSecurityAlert(
                    alertType = "BRUTE_FORCE_ATTEMPT",
                    message =
                        "Potential brute force attack detected for identifier: $identifier",
                    threatLevel = threatLevel,
                    details =
                        mapOf(
                            "identifier" to identifier,
                            "attemptCount" to recentAttempts,
                            "ipAddress" to (ipAddress ?: "unknown"),
                            "timeWindow" to
                                "${securityProperties.authLockoutDurationMinutes} minutes",
                        ),
                )
            }

            return threatLevel
        }
    }

    /** Records successful authentication and clears failed attempt tracking. */
    fun recordSuccessfulAuth(identifier: String) {
        failedAuthAttempts.remove(identifier)
        logger.debug(
            "Successful authentication for identifier: {}, cleared failed attempts",
            identifier,
        )
    }

    /** Records a tenant access attempt and detects cross-tenant access patterns. */
    fun recordTenantAccessAttempt(
        userTenantId: String,
        targetTenantId: String,
        methodName: String,
        granted: Boolean,
        reason: String,
    ): SecurityThreatLevel {
        val now = Instant.now()
        val userId = securityContextHolder.getUserId() ?: "unknown"
        val key = "$userId:$userTenantId"

        val attempt =
            TenantAccessAttempt(
                targetTenantId = targetTenantId,
                methodName = methodName,
                granted = granted,
                reason = reason,
                timestamp = now,
            )

        val attempts = tenantAccessAttempts.computeIfAbsent(key) { mutableListOf() }

        synchronized(attempts) {
            // Clean up old attempts (older than monitoring window)
            val cutoff =
                now.minus(
                    securityProperties.monitoring.suspiciousActivityWindowMinutes,
                    ChronoUnit.MINUTES,
                )
            attempts.removeIf { it.timestamp.isBefore(cutoff) }

            attempts.add(attempt)

            // Analyze patterns for suspicious activity
            val threatLevel = analyzeTenantAccessPatterns(userTenantId, targetTenantId, attempts)

            if (threatLevel.ordinal >= SecurityThreatLevel.MEDIUM.ordinal) {
                logger.warn(
                    "Suspicious tenant access pattern detected for user in tenant {}: {}",
                    userTenantId,
                    threatLevel,
                )

                sendSecurityAlert(
                    alertType = "SUSPICIOUS_TENANT_ACCESS",
                    message = "Suspicious cross-tenant access pattern detected",
                    threatLevel = threatLevel,
                    details =
                        mapOf(
                            "userTenantId" to userTenantId,
                            "targetTenantId" to targetTenantId,
                            "recentAttempts" to attempts.size,
                            "deniedAttempts" to attempts.count { !it.granted },
                            "timeWindow" to
                                "${securityProperties.monitoring.suspiciousActivityWindowMinutes} minutes",
                        ),
                )
            }

            return threatLevel
        }
    }

    /** Checks rate limiting for a given key (user, IP, tenant, etc.). */
    fun checkRateLimit(
        key: String,
        maxCalls: Int = securityProperties.defaultRateLimitMaxCalls,
        windowSeconds: Long = securityProperties.defaultRateLimitWindowSeconds,
    ): RateLimitResult {
        val now = Instant.now()
        val requests = rateLimitTracking.computeIfAbsent(key) { mutableListOf() }

        synchronized(requests) {
            // Clean up old requests
            val cutoff = now.minusSeconds(windowSeconds)
            requests.removeIf { it.isBefore(cutoff) }

            val currentCount = requests.size

            if (currentCount >= maxCalls) {
                logger.warn(
                    "Rate limit exceeded for key: {} ({} requests in {} seconds)",
                    key,
                    currentCount,
                    windowSeconds,
                )

                logSecurityEvent(
                    eventType = "RATE_LIMIT_EXCEEDED",
                    identifier = key,
                    threatLevel = SecurityThreatLevel.MEDIUM,
                    details =
                        mapOf(
                            "currentCount" to currentCount,
                            "maxAllowed" to maxCalls,
                            "windowSeconds" to windowSeconds,
                        ),
                )

                return RateLimitResult(
                    allowed = false,
                    currentCount = currentCount,
                    maxAllowed = maxCalls,
                    resetTime = now.plusSeconds(windowSeconds),
                )
            } else {
                // Add current request
                requests.add(now)

                return RateLimitResult(
                    allowed = true,
                    currentCount = currentCount + 1,
                    maxAllowed = maxCalls,
                    resetTime = now.plusSeconds(windowSeconds),
                )
            }
        }
    }

    /** Records a security violation for monitoring purposes. */
    fun recordSecurityViolation(
        violationType: String,
        description: String,
        metadata: Map<String, Any> = emptyMap(),
    ) {
        logger.warn("Security violation recorded: {} - {}", violationType, description)

        try {
            kotlinx.coroutines.runBlocking {
                auditService.logAdminAction(
                    action = AdminAction.CUSTOM_ACTION,
                    targetType = "security_monitoring",
                    targetId = securityContextHolder.getUserId() ?: "unknown",
                    details =
                        metadata +
                            mapOf(
                                "eventType" to violationType,
                                "description" to description,
                                "timestamp" to Instant.now().toString(),
                                "securityEvent" to true,
                            ),
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to log security event: {}", e.message, e)
        }
    }

    /** Gets current security metrics for monitoring dashboards. */
    fun getSecurityMetrics(): SecurityMetrics {
        val now = Instant.now()
        val windowStart =
            now.minus(
                securityProperties.monitoring.suspiciousActivityWindowMinutes,
                ChronoUnit.MINUTES,
            )

        return SecurityMetrics(
            activeFailedAuthAttempts =
                failedAuthAttempts.values.sumOf { attempts ->
                    attempts.count { it.isAfter(windowStart) }
                },
            activeTenantAccessAttempts =
                tenantAccessAttempts.values.sumOf { attempts ->
                    attempts.count { it.timestamp.isAfter(windowStart) }
                },
            rateLimitViolations =
                rateLimitTracking.values.sumOf { requests ->
                    requests.count { it.isAfter(windowStart) }
                },
            monitoringWindowMinutes =
                securityProperties.monitoring.suspiciousActivityWindowMinutes,
            timestamp = now,
        )
    }

    /** Analyzes tenant access patterns to detect suspicious behavior. */
    private fun analyzeTenantAccessPatterns(
        userTenantId: String,
        targetTenantId: String,
        attempts: List<TenantAccessAttempt>,
    ): SecurityThreatLevel {
        if (attempts.isEmpty()) return SecurityThreatLevel.LOW

        val deniedAttempts = attempts.count { !it.granted }
        val crossTenantAttempts = attempts.count { it.targetTenantId != userTenantId }
        val uniqueTargetTenants = attempts.map { it.targetTenantId }.distinct().size

        return when {
            // High number of denied cross-tenant attempts
            deniedAttempts >= 10 && crossTenantAttempts >= 10 -> SecurityThreatLevel.CRITICAL

            // Multiple different tenant targets
            uniqueTargetTenants >= 5 -> SecurityThreatLevel.HIGH

            // Moderate cross-tenant activity
            deniedAttempts >= 5 || crossTenantAttempts >= 5 -> SecurityThreatLevel.MEDIUM
            else -> SecurityThreatLevel.LOW
        }
    }

    /** Logs security events for audit purposes. */
    private fun logSecurityEvent(
        eventType: String,
        identifier: String,
        threatLevel: SecurityThreatLevel,
        details: Map<String, Any>,
    ) {
        try {
            kotlinx.coroutines.runBlocking {
                auditService.logAdminAction(
                    action = AdminAction.CUSTOM_ACTION,
                    targetType = "security_monitoring",
                    targetId = identifier,
                    details =
                        details +
                            mapOf(
                                "eventType" to eventType,
                                "threatLevel" to threatLevel.name,
                                "securityEvent" to true,
                            ),
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to log security event: {}", e.message, e)
        }
    }

    /** Sends security alerts (in production, this could integrate with alerting systems). */
    private fun sendSecurityAlert(
        alertType: String,
        message: String,
        threatLevel: SecurityThreatLevel,
        details: Map<String, Any>,
    ) {
        if (!securityProperties.monitoring.enableSecurityAlerts) {
            return
        }

        // In production, this would integrate with alerting systems like:
        // - Email notifications
        // - Slack/Teams webhooks
        // - PagerDuty/OpsGenie
        // - SIEM systems

        logger.warn(
            "SECURITY ALERT [{}] {}: {} - Details: {}",
            threatLevel,
            alertType,
            message,
            details,
        )

        // For now, we'll just log structured alert data
        val alertData =
            mapOf(
                "alertType" to alertType,
                "message" to message,
                "threatLevel" to threatLevel.name,
                "timestamp" to Instant.now().toString(),
                "details" to details,
            )

        logger.warn("Security alert data: {}", alertData)
    }

    /** Data class for tenant access attempt tracking. */
    private data class TenantAccessAttempt(
        val targetTenantId: String,
        val methodName: String,
        val granted: Boolean,
        val reason: String,
        val timestamp: Instant,
    )
}

/** Security threat levels. */
enum class SecurityThreatLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

/** Result of rate limiting check. */
data class RateLimitResult(
    val allowed: Boolean,
    val currentCount: Int,
    val maxAllowed: Int,
    val resetTime: Instant,
)

/** Security metrics for monitoring dashboards. */
data class SecurityMetrics(
    val activeFailedAuthAttempts: Int,
    val activeTenantAccessAttempts: Int,
    val rateLimitViolations: Int,
    val monitoringWindowMinutes: Long,
    val timestamp: Instant,
)

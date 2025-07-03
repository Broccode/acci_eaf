package com.axians.eaf.core.tenancy.integration

import com.axians.eaf.core.tenancy.TenantContextException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Security validator for tenant context operations with rate limiting and abuse detection.
 *
 * This component provides protection against tenant context manipulation attempts and implements
 * rate limiting for tenant context operations to prevent abuse.
 */
@Component
class TenantContextSecurityValidator(
    private val properties: TenantContextIntegrationProperties,
) {
    private val logger = LoggerFactory.getLogger(TenantContextSecurityValidator::class.java)

    // Rate limiting data structures
    private val rateLimitCounters = ConcurrentHashMap<String, RateLimitCounter>()
    private val suspiciousActivities = ConcurrentHashMap<String, SuspiciousActivityTracker>()

    companion object {
        private const val DEFAULT_MAX_REQUESTS_PER_MINUTE = 100
        private const val DEFAULT_MAX_SUSPICIOUS_ATTEMPTS = 10
        private const val RATE_LIMIT_WINDOW_MINUTES = 1L
        private const val SUSPICIOUS_ACTIVITY_WINDOW_MINUTES = 5L

        // Security validation constants
        private const val MAX_TENANT_SWITCHES_THRESHOLD = 20
        private const val MAX_HEADER_REQUESTS_THRESHOLD = 50
        private const val HEADER_REQUEST_SUSPICIOUS_SCORE = 3
        private const val INVALID_ATTEMPT_SUSPICIOUS_SCORE = 5
    }

    /**
     * Validates a tenant ID for security concerns.
     *
     * @param tenantId The tenant ID to validate
     * @param source The source of the tenant ID (e.g., "jwt", "header", "manual")
     * @param clientIdentifier Client identifier for rate limiting (IP address or user ID)
     * @throws TenantContextException if validation fails
     */
    fun validateTenantIdSecurity(
        tenantId: String,
        source: String,
        clientIdentifier: String = "unknown",
    ) {
        // Basic format validation
        validateTenantIdFormat(tenantId)

        // Rate limiting check
        checkRateLimit(clientIdentifier, source)

        // Suspicious activity detection
        detectSuspiciousActivity(tenantId, source, clientIdentifier)

        logger.debug("Tenant ID security validation passed: tenant={}, source={}", tenantId, source)
    }

    /**
     * Validates tenant ID format and content for security.
     *
     * @param tenantId The tenant ID to validate
     * @throws TenantContextException if format is invalid
     */
    private fun validateTenantIdFormat(tenantId: String) {
        val validationError =
            when {
                tenantId.isBlank() -> "Tenant ID cannot be blank"
                tenantId.length > properties.maxTenantIdLength ->
                    "Tenant ID exceeds maximum length of ${properties.maxTenantIdLength} characters"
                !tenantId.matches(Regex("^[a-zA-Z0-9_-]+$")) ->
                    "Invalid characters detected. Only alphanumeric, underscore, and hyphen are allowed."
                tenantId.startsWith("-") || tenantId.endsWith("-") ->
                    "Tenant ID cannot start or end with hyphen"
                tenantId.startsWith("_") || tenantId.endsWith("_") ->
                    "Tenant ID cannot start or end with underscore"
                else -> null
            }

        if (validationError != null) {
            logger.warn("Invalid tenant ID format: {}", validationError)
            throw TenantContextException(validationError)
        }
    }

    /**
     * Checks rate limiting for tenant context operations.
     *
     * @param clientIdentifier Client identifier for rate limiting
     * @param operation The operation being performed
     * @throws TenantContextException if rate limit is exceeded
     */
    private fun checkRateLimit(
        clientIdentifier: String,
        operation: String,
    ) {
        val now = Instant.now()
        val counter = rateLimitCounters.computeIfAbsent(clientIdentifier) { RateLimitCounter(now) }

        synchronized(counter) {
            // Reset counter if window has passed
            if (ChronoUnit.MINUTES.between(counter.windowStart, now) >= RATE_LIMIT_WINDOW_MINUTES) {
                counter.reset(now)
            }

            counter.requestCount.incrementAndGet()

            if (counter.requestCount.get() > DEFAULT_MAX_REQUESTS_PER_MINUTE) {
                logger.warn(
                    "Rate limit exceeded for client: {} operation: {} count: {}",
                    clientIdentifier,
                    operation,
                    counter.requestCount.get(),
                )
                throw TenantContextException(
                    "Rate limit exceeded. Too many tenant context operations from this client.",
                )
            }
        }
    }

    /**
     * Detects suspicious activity patterns.
     *
     * @param tenantId The tenant ID being accessed
     * @param source The source of the request
     * @param clientIdentifier Client identifier
     */
    private fun detectSuspiciousActivity(
        tenantId: String,
        source: String,
        clientIdentifier: String,
    ) {
        val now = Instant.now()
        val tracker =
            suspiciousActivities.computeIfAbsent(clientIdentifier) {
                SuspiciousActivityTracker(now)
            }

        synchronized(tracker) {
            // Reset tracker if window has passed
            if (ChronoUnit.MINUTES.between(tracker.windowStart, now) >=
                SUSPICIOUS_ACTIVITY_WINDOW_MINUTES
            ) {
                tracker.reset(now)
            }

            // Check for suspicious patterns
            var suspiciousScore = 0

            // Pattern 1: Rapid tenant switching
            if (tracker.lastTenantId != null && tracker.lastTenantId != tenantId) {
                tracker.tenantSwitchCount.incrementAndGet()
                if (tracker.tenantSwitchCount.get() > MAX_TENANT_SWITCHES_THRESHOLD) {
                    suspiciousScore += 2
                }
            }
            tracker.lastTenantId = tenantId

            // Pattern 2: Multiple header-based requests (possible injection attempts)
            if (source == "header") {
                tracker.headerRequestCount.incrementAndGet()
                if (tracker.headerRequestCount.get() > MAX_HEADER_REQUESTS_THRESHOLD) {
                    suspiciousScore += HEADER_REQUEST_SUSPICIOUS_SCORE
                }
            }

            // Pattern 3: Invalid tenant ID attempts
            try {
                validateTenantIdFormat(tenantId)
            } catch (e: TenantContextException) {
                logger.debug(
                    "Invalid tenant ID format detected during suspicious activity check: {}",
                    e.message,
                )
                tracker.invalidAttemptCount.incrementAndGet()
                suspiciousScore += INVALID_ATTEMPT_SUSPICIOUS_SCORE
            }

            tracker.totalSuspiciousScore += suspiciousScore

            if (tracker.totalSuspiciousScore > DEFAULT_MAX_SUSPICIOUS_ATTEMPTS) {
                logger.error(
                    "Suspicious activity detected for client: {} score: {} tenant: {}",
                    clientIdentifier,
                    tracker.totalSuspiciousScore,
                    tenantId,
                )
                throw TenantContextException(
                    "Suspicious activity detected. Tenant context operations temporarily blocked.",
                )
            }

            if (suspiciousScore > 0) {
                logger.warn(
                    "Suspicious activity detected: client={} tenant={} source={} score={}",
                    clientIdentifier,
                    tenantId,
                    source,
                    suspiciousScore,
                )
            }
        }
    }

    /**
     * Validates tenant context transition for security.
     *
     * @param fromTenantId Previous tenant ID (null if none)
     * @param toTenantId New tenant ID
     * @param clientIdentifier Client identifier
     */
    fun validateTenantContextTransition(
        fromTenantId: String?,
        toTenantId: String,
        clientIdentifier: String = "unknown",
    ) {
        validateTenantIdSecurity(toTenantId, "transition", clientIdentifier)

        // Additional transition-specific validation
        if (fromTenantId != null && fromTenantId == toTenantId) {
            logger.debug("Tenant context transition to same tenant: {}", toTenantId)
        } else {
            logger.debug(
                "Tenant context transition: from={} to={} client={}",
                fromTenantId,
                toTenantId,
                clientIdentifier,
            )
        }
    }

    /**
     * Cleans up expired rate limiting and suspicious activity data. Should be called periodically
     * to prevent memory leaks.
     */
    fun cleanupExpiredData() {
        val now = Instant.now()
        var cleaned = 0

        // Clean rate limit counters
        rateLimitCounters.entries.removeIf { (_, counter) ->
            val expired =
                ChronoUnit.MINUTES.between(counter.windowStart, now) >
                    RATE_LIMIT_WINDOW_MINUTES * 2
            if (expired) cleaned++
            expired
        }

        // Clean suspicious activity trackers
        suspiciousActivities.entries.removeIf { (_, tracker) ->
            val expired =
                ChronoUnit.MINUTES.between(tracker.windowStart, now) >
                    SUSPICIOUS_ACTIVITY_WINDOW_MINUTES * 2
            if (expired) cleaned++
            expired
        }

        if (cleaned > 0) {
            logger.debug("Cleaned up {} expired security tracking entries", cleaned)
        }
    }

    /** Gets security statistics for monitoring. */
    fun getSecurityStatistics(): SecurityStatistics {
        val now = Instant.now()

        val activeRateLimiters =
            rateLimitCounters.values.count { counter ->
                ChronoUnit.MINUTES.between(counter.windowStart, now) <=
                    RATE_LIMIT_WINDOW_MINUTES
            }

        val activeSuspiciousTrackers =
            suspiciousActivities.values.count { tracker ->
                ChronoUnit.MINUTES.between(tracker.windowStart, now) <=
                    SUSPICIOUS_ACTIVITY_WINDOW_MINUTES
            }

        return SecurityStatistics(
            activeRateLimiters = activeRateLimiters,
            activeSuspiciousActivityTrackers = activeSuspiciousTrackers,
            totalRateLimitEntries = rateLimitCounters.size,
            totalSuspiciousActivityEntries = suspiciousActivities.size,
        )
    }

    /** Rate limiting counter for a specific client. */
    private data class RateLimitCounter(
        var windowStart: Instant,
        val requestCount: AtomicInteger = AtomicInteger(0),
    ) {
        fun reset(newWindowStart: Instant) {
            windowStart = newWindowStart
            requestCount.set(0)
        }
    }

    /** Suspicious activity tracker for a specific client. */
    private data class SuspiciousActivityTracker(
        var windowStart: Instant,
        val tenantSwitchCount: AtomicInteger = AtomicInteger(0),
        val headerRequestCount: AtomicInteger = AtomicInteger(0),
        val invalidAttemptCount: AtomicInteger = AtomicInteger(0),
        var totalSuspiciousScore: Int = 0,
        var lastTenantId: String? = null,
    ) {
        fun reset(newWindowStart: Instant) {
            windowStart = newWindowStart
            tenantSwitchCount.set(0)
            headerRequestCount.set(0)
            invalidAttemptCount.set(0)
            totalSuspiciousScore = 0
            lastTenantId = null
        }
    }

    /** Security statistics for monitoring and alerting. */
    data class SecurityStatistics(
        val activeRateLimiters: Int,
        val activeSuspiciousActivityTrackers: Int,
        val totalRateLimitEntries: Int,
        val totalSuspiciousActivityEntries: Int,
    )
}

package com.axians.eaf.controlplane.infrastructure.adapter.input.common

import java.time.Instant

/**
 * Common constants for endpoint responses and validation. Addresses detekt MagicNumber issues by
 * centralizing numeric constants.
 */
object ResponseConstants {
    // Version constants
    const val API_VERSION = "1.0.0"
    const val APPLICATION_VERSION = "1.0.0-SNAPSHOT"

    // Pagination constants
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 1000
    const val DEFAULT_PAGE_NUMBER = 0

    // Message validation constants
    const val MAX_MESSAGE_LENGTH = 1000
    const val MAX_REASON_LENGTH = 255

    // Time constants (in seconds)
    const val DEFAULT_INVITATION_EXPIRY_DAYS = 7
    const val INVITATION_EXPIRY_SECONDS = DEFAULT_INVITATION_EXPIRY_DAYS * 24 * 60 * 60L
    const val DEFAULT_AUDIT_LOOKBACK_DAYS = 1
    const val AUDIT_LOOKBACK_SECONDS = DEFAULT_AUDIT_LOOKBACK_DAYS * 24 * 60 * 60L
    const val EXTENDED_AUDIT_LOOKBACK_DAYS = 7
    const val EXTENDED_AUDIT_LOOKBACK_SECONDS = EXTENDED_AUDIT_LOOKBACK_DAYS * 24 * 60 * 60L
    const val RECENT_ACTIVITY_DAYS = 7L

    // Health check constants
    const val DEFAULT_HEALTH_CHECK_LIMIT = 100
    const val MAX_HEALTH_CHECK_LIMIT = 500
    const val SECURITY_EVENTS_PAGE_SIZE = 500
    const val HIGH_RISK_EVENTS_PAGE_SIZE = 200
    const val TARGET_AUDIT_PAGE_SIZE = 100

    // Password validation constants
    const val MIN_PASSWORD_LENGTH = 8
    const val PASSWORD_RESET_EXPIRY_MINUTES = 15

    // Build info placeholders
    const val DEFAULT_BUILD_TIME = "2024-01-01T00:00:00Z"
    const val DEFAULT_ENVIRONMENT = "development"
    const val DEFAULT_SPRING_BOOT_VERSION = "3.3.1"
    const val DEFAULT_HILLA_VERSION = "2.5.8"

    // Database connection constants
    const val DEFAULT_CONNECTION_POOL_SIZE = 10
    const val DEFAULT_ACTIVE_CONNECTIONS = 2

    // Response time constants
    const val TYPICAL_DB_RESPONSE_TIME_MS = 12
    const val TYPICAL_IAM_RESPONSE_TIME_MS = 23
    const val TYPICAL_NATS_RESPONSE_TIME_MS = 8

    // Tenant/User validation constants
    const val MAX_TENANT_NAME_LENGTH = 100
    const val MAX_USER_NAME_LENGTH = 50
    const val MAX_EMAIL_LENGTH = 254 // RFC 5321 standard

    // Collection size limits
    const val MAX_ROLES_PER_USER = 10
    const val MAX_PERMISSIONS_PER_ROLE = 50

    // Monitoring and metrics constants
    const val SECURITY_MONITORING_WINDOW_MINUTES = 15
    const val RATE_LIMIT_WINDOW_MINUTES = 60
    const val SUSPICIOUS_ACTIVITY_THRESHOLD = 5
}

/**
 * Common response metadata factory. Provides consistent metadata generation across all endpoint
 * responses.
 */
object ResponseMetadataFactory {
    fun createResponseMetadata(
        requestType: String,
        includeTimestamp: Boolean = true,
    ): ResponseMetadata =
        ResponseMetadata(
            timestamp = if (includeTimestamp) Instant.now() else Instant.EPOCH,
            requestId = "$requestType-${System.currentTimeMillis()}",
            version = ResponseConstants.API_VERSION,
        )

    fun createErrorMetadata(
        requestType: String,
        errorCode: String? = null,
    ): ResponseMetadata =
        ResponseMetadata(
            timestamp = Instant.now(),
            requestId = "$requestType-error-${System.currentTimeMillis()}",
            version = ResponseConstants.API_VERSION,
        )
}

/** Common response metadata structure. */
data class ResponseMetadata(
    val timestamp: Instant,
    val requestId: String,
    val version: String,
)

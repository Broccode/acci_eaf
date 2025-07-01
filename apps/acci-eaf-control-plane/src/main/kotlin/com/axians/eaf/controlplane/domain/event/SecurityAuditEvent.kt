package com.axians.eaf.controlplane.domain.event

import java.time.Instant

/**
 * Domain event representing security audit activities.
 *
 * This event is published when security-related actions occur in the system, replacing direct audit
 * service calls to eliminate circular dependencies. The audit service can subscribe to these events
 * for persistent audit logging.
 */
data class SecurityAuditEvent(
    val eventId: String,
    val timestamp: Instant,
    val tenantId: String?,
    val userId: String?,
    val action: SecurityAction,
    val resource: SecurityResource,
    val outcome: SecurityOutcome,
    val details: Map<String, Any>,
    val correlationId: String?,
) {
    companion object {
        fun tenantAccess(context: TenantAccessContext) =
            SecurityAuditEvent(
                eventId =
                    java.util.UUID
                        .randomUUID()
                        .toString(),
                timestamp = Instant.now(),
                tenantId = context.tenantId,
                userId = context.userId,
                action =
                    if (context.accessGranted) {
                        SecurityAction.ACCESS_GRANTED
                    } else {
                        SecurityAction.ACCESS_DENIED
                    },
                resource =
                    SecurityResource(
                        type = "tenant_access",
                        id = context.targetTenantId ?: "unknown",
                        attributes =
                            mapOf(
                                "method" to context.methodName,
                                "userTenantId" to context.userTenantId,
                                "targetTenantId" to
                                    (context.targetTenantId ?: "null"),
                            ),
                    ),
                outcome =
                    if (context.accessGranted) {
                        SecurityOutcome.SUCCESS
                    } else {
                        SecurityOutcome.DENIED
                    },
                details =
                    mapOf(
                        "accessReason" to context.accessReason,
                        "globalAccess" to context.globalAccess,
                        "timestamp" to System.currentTimeMillis(),
                    ),
                correlationId = context.correlationId,
            )

        fun securityViolation(
            tenantId: String?,
            userId: String?,
            methodName: String,
            exception: SecurityException,
            correlationId: String?,
        ) = SecurityAuditEvent(
            eventId =
                java.util.UUID
                    .randomUUID()
                    .toString(),
            timestamp = Instant.now(),
            tenantId = tenantId,
            userId = userId,
            action = SecurityAction.SECURITY_VIOLATION,
            resource =
                SecurityResource(
                    type = "security_violation",
                    id = methodName,
                    attributes =
                        mapOf(
                            "violationType" to
                                exception.javaClass.simpleName,
                            "method" to methodName,
                        ),
                ),
            outcome = SecurityOutcome.VIOLATION,
            details =
                mapOf(
                    "message" to (exception.message ?: "Unknown error"),
                    "timestamp" to System.currentTimeMillis(),
                ),
            correlationId = correlationId,
        )
    }
}

/** Context data for tenant access audit events. Addresses detekt LongParameterList issue. */
data class TenantAccessContext(
    val tenantId: String?,
    val userId: String?,
    val methodName: String,
    val userTenantId: String,
    val targetTenantId: String?,
    val accessGranted: Boolean,
    val accessReason: String,
    val globalAccess: Boolean,
    val correlationId: String?,
)

/** Types of security actions that can be audited. */
enum class SecurityAction {
    ACCESS_GRANTED,
    ACCESS_DENIED,
    SECURITY_VIOLATION,
    AUTHENTICATION_FAILURE,
    AUTHORIZATION_FAILURE,
}

/** Security resource being accessed or protected. */
data class SecurityResource(
    val type: String,
    val id: String,
    val attributes: Map<String, Any> = emptyMap(),
)

/** Outcome of the security operation. */
enum class SecurityOutcome {
    SUCCESS,
    DENIED,
    VIOLATION,
    ERROR,
}

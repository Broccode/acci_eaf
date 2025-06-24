package com.axians.eaf.controlplane.domain.exception

import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.UserId

/** Base class for all security-related exceptions in the control plane. */
abstract class SecurityException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/** Exception thrown when a user attempts to access resources outside their tenant scope. */
class InsufficientPermissionException(
    message: String,
    val requestedTenantId: String? = null,
    val userTenantId: String? = null,
    val requiredRoles: Set<String> = emptySet(),
    cause: Throwable? = null,
) : SecurityException(message, cause) {
    companion object {
        fun forTenantAccess(
            userTenantId: String,
            requestedTenantId: String,
            action: String? = null,
        ): InsufficientPermissionException {
            val message =
                buildString {
                    append(
                        "Access denied to tenant '$requestedTenantId' for user from tenant '$userTenantId'",
                    )
                    if (action != null) {
                        append(" when attempting to $action")
                    }
                }
            return InsufficientPermissionException(
                message = message,
                requestedTenantId = requestedTenantId,
                userTenantId = userTenantId,
            )
        }

        fun forMissingRole(
            requiredRoles: Set<String>,
            action: String? = null,
        ): InsufficientPermissionException {
            val message =
                buildString {
                    append("Access denied. Required roles: ${requiredRoles.joinToString(", ")}")
                    if (action != null) {
                        append(" for action: $action")
                    }
                }
            return InsufficientPermissionException(
                message = message,
                requiredRoles = requiredRoles,
            )
        }
    }
}

/** Exception thrown when tenant isolation is violated. */
class TenantIsolationViolationException(
    message: String,
    val violationType: String,
    val tenantId: TenantId,
    val resourceType: String,
    val resourceId: String,
    cause: Throwable? = null,
) : SecurityException(message, cause)

/** Exception thrown when authentication context is missing or invalid. */
class AuthenticationRequiredException(
    message: String = "Authentication is required for this operation",
    cause: Throwable? = null,
) : SecurityException(message, cause)

/** Exception thrown when a user attempts to access a disabled or archived tenant. */
class TenantAccessDeniedException(
    message: String,
    val tenantId: TenantId,
    val tenantStatus: String,
    cause: Throwable? = null,
) : SecurityException(message, cause) {
    companion object {
        fun forInactiveTenant(
            tenantId: TenantId,
            status: String,
        ): TenantAccessDeniedException =
            TenantAccessDeniedException(
                message = "Access denied to tenant '${tenantId.value}' with status '$status'",
                tenantId = tenantId,
                tenantStatus = status,
            )
    }
}

/** Exception thrown when security context manipulation is attempted. */
class SecurityContextManipulationException(
    message: String,
    val attemptedAction: String,
    val userId: UserId? = null,
    cause: Throwable? = null,
) : SecurityException(message, cause)

/** Exception thrown when rate limiting or security quotas are exceeded. */
class SecurityQuotaExceededException(
    message: String,
    val quotaType: String,
    val currentValue: Long,
    val maxAllowed: Long,
    val resetTime: java.time.Instant? = null,
    cause: Throwable? = null,
) : SecurityException(message, cause)

/** Exception thrown when suspicious security activity is detected. */
class SuspiciousActivityException(
    message: String,
    val activityType: String,
    val detectionReason: String,
    val riskLevel: SecurityRiskLevel,
    val metadata: Map<String, Any> = emptyMap(),
    cause: Throwable? = null,
) : SecurityException(message, cause)

/** Security risk levels for suspicious activity detection. */
enum class SecurityRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

package com.axians.eaf.controlplane.infrastructure.security.annotation

/**
 * Annotation to mark methods that require tenant-specific access validation.
 *
 * When applied to a method, this annotation triggers the TenantSecurityAspect to validate that the
 * current user has access to the tenant specified in the method arguments.
 *
 * The aspect will:
 * 1. Extract the tenant ID from the security context
 * 2. Extract the target tenant ID from method arguments
 * 3. Verify access based on tenant isolation rules
 * 4. Allow global administrators (SUPER_ADMIN, PLATFORM_ADMIN) to access any tenant
 * 5. Allow tenant administrators to access only their own tenant
 *
 * @param tenantIdParamName The name of the parameter containing the target tenant ID.
 * ```
 *                          Defaults to "tenantId". The aspect will look for this parameter
 *                          in the method arguments to determine the target tenant.
 * @param allowGlobalAccess
 * ```
 * Whether to allow global administrators to bypass tenant restrictions.
 * ```
 *                          Defaults to true.
 * @param auditAccess
 * ```
 * Whether to log access attempts to the audit trail.
 * ```
 *                    Defaults to true for security monitoring.
 * ```
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RequiresTenantAccess(
    val tenantIdParamName: String = "tenantId",
    val allowGlobalAccess: Boolean = true,
    val auditAccess: Boolean = true,
)

/**
 * Annotation to mark methods that require specific roles for access.
 *
 * This extends Spring Security's @RolesAllowed with additional features for audit logging and
 * tenant-aware role validation.
 *
 * @param roles The roles required to access this method
 * @param requireAll Whether all roles are required (AND) or any role is sufficient (OR).
 * ```
 *                   Defaults to false (OR logic).
 * @param auditAccess
 * ```
 * Whether to log access attempts. Defaults to true.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RequiresRoles(
    val roles: Array<String>,
    val requireAll: Boolean = false,
    val auditAccess: Boolean = true,
)

/**
 * Annotation to mark methods that require specific permissions.
 *
 * Validates that the current user has the specified permissions within their tenant context.
 *
 * @param permissions The permissions required to access this method
 * @param requireAll Whether all permissions are required (AND) or any permission is sufficient
 * (OR).
 * ```
 *                   Defaults to false (OR logic).
 * @param tenantScoped
 * ```
 * Whether permissions should be validated within tenant scope.
 * ```
 *                     Defaults to true.
 * @param auditAccess
 * ```
 * Whether to log access attempts. Defaults to true.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RequiresPermissions(
    val permissions: Array<String>,
    val requireAll: Boolean = false,
    val tenantScoped: Boolean = true,
    val auditAccess: Boolean = true,
)

/**
 * Annotation to mark methods that should be rate limited for security.
 *
 * Prevents abuse by limiting the number of calls per time period.
 *
 * @param maxCalls Maximum number of calls allowed in the time window
 * @param timeWindowSeconds Time window in seconds for rate limiting
 * @param scope The scope for rate limiting (USER, TENANT, GLOBAL)
 * @param blockOnExceeded Whether to block the call or just log when exceeded
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RateLimit(
    val maxCalls: Int,
    val timeWindowSeconds: Long,
    val scope: RateLimitScope = RateLimitScope.USER,
    val blockOnExceeded: Boolean = true,
)

/** Scopes for rate limiting. */
enum class RateLimitScope {
    /** Rate limit per individual user */
    USER,

    /** Rate limit per tenant */
    TENANT,

    /** Global rate limit across all users */
    GLOBAL,

    /** Rate limit per IP address */
    IP_ADDRESS,
}

/**
 * Annotation to mark methods that handle sensitive data requiring extra security.
 *
 * Methods marked with this annotation will have additional security monitoring, stricter access
 * controls, and comprehensive audit logging.
 *
 * @param sensitivityLevel The level of data sensitivity
 * @param requireEncryption Whether the data must be encrypted in transit/storage
 * @param logDataAccess Whether to log detailed data access information
 * @param requireSecureChannel Whether to require HTTPS/secure channels
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class SensitiveData(
    val sensitivityLevel: SensitivityLevel = SensitivityLevel.MEDIUM,
    val requireEncryption: Boolean = true,
    val logDataAccess: Boolean = true,
    val requireSecureChannel: Boolean = true,
)

/** Data sensitivity levels. */
enum class SensitivityLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

/**
 * Annotation to mark methods that should validate tenant status before access.
 *
 * Ensures that operations are only performed on active tenants, preventing access to suspended or
 * archived tenants.
 *
 * @param allowedStatuses The tenant statuses that are allowed for this operation
 * @param blockSuspended Whether to block access for suspended tenants
 * @param blockArchived Whether to block access for archived tenants
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ValidateTenantStatus(
    val allowedStatuses: Array<String> = ["ACTIVE"],
    val blockSuspended: Boolean = true,
    val blockArchived: Boolean = true,
)

/**
 * Annotation to mark methods that should trigger security monitoring.
 *
 * Enables enhanced monitoring and alerting for security-critical operations.
 *
 * @param monitoringLevel The level of monitoring to apply
 * @param alertOnFailure Whether to send alerts on access failures
 * @param alertOnSuccess Whether to send alerts on successful access (for high-risk operations)
 * @param includeRequestDetails Whether to include full request details in monitoring
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class SecurityMonitoring(
    val monitoringLevel: MonitoringLevel = MonitoringLevel.STANDARD,
    val alertOnFailure: Boolean = true,
    val alertOnSuccess: Boolean = false,
    val includeRequestDetails: Boolean = true,
)

/** Security monitoring levels. */
enum class MonitoringLevel {
    MINIMAL,
    STANDARD,
    ENHANCED,
    COMPREHENSIVE,
}

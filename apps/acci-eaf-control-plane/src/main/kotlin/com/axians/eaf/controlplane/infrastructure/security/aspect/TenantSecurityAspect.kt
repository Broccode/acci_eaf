package com.axians.eaf.controlplane.infrastructure.security.aspect

import com.axians.eaf.controlplane.domain.exception.AuthenticationRequiredException
import com.axians.eaf.controlplane.domain.exception.InsufficientPermissionException
import com.axians.eaf.controlplane.domain.model.audit.AdminAction
import com.axians.eaf.controlplane.domain.service.AuditService
import com.axians.eaf.controlplane.infrastructure.security.annotation.RequiresTenantAccess
import com.axians.eaf.core.security.EafSecurityContextHolder
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.lang.reflect.Parameter

/**
 * Security aspect that enforces multi-tenant access control.
 *
 * This aspect intercepts methods annotated with @RequiresTenantAccess and validates that the
 * current user has proper access to the target tenant based on:
 * 1. User's tenant context
 * 2. Target tenant ID from method parameters
 * 3. User's roles and permissions
 * 4. Global access rules for super administrators
 */
@Aspect
@Component
class TenantSecurityAspect(
    private val securityContextHolder: EafSecurityContextHolder,
    private val auditService: AuditService,
) {
    private val logger = LoggerFactory.getLogger(TenantSecurityAspect::class.java)

    /**
     * Intercepts method calls annotated with @RequiresTenantAccess. Validates tenant access and
     * logs security events.
     */
    @Around("@annotation(requiresTenantAccess)")
    fun enforceTenantAccess(
        joinPoint: ProceedingJoinPoint,
        requiresTenantAccess: RequiresTenantAccess,
    ): Any? {
        val startTime = System.currentTimeMillis()
        val methodName = "${joinPoint.signature.declaringTypeName}.${joinPoint.signature.name}"

        try {
            // Validate authentication
            if (!securityContextHolder.isAuthenticated()) {
                throw AuthenticationRequiredException(
                    "Authentication required for tenant-scoped operation",
                )
            }

            val userTenantId =
                securityContextHolder.getTenantIdOrNull()
                    ?: throw AuthenticationRequiredException(
                        "No tenant context found in authentication",
                    )

            // Extract target tenant ID from method arguments
            val targetTenantId =
                extractTenantIdFromArguments(joinPoint, requiresTenantAccess.tenantIdParamName)

            // Validate tenant access
            val accessResult =
                validateTenantAccess(
                    userTenantId = userTenantId,
                    targetTenantId = targetTenantId,
                    allowGlobalAccess = requiresTenantAccess.allowGlobalAccess,
                    methodName = methodName,
                )

            // Log access attempt if auditing is enabled
            if (requiresTenantAccess.auditAccess) {
                logTenantAccess(
                    methodName = methodName,
                    userTenantId = userTenantId,
                    targetTenantId = targetTenantId,
                    accessGranted = accessResult.granted,
                    accessReason = accessResult.reason,
                    globalAccess = accessResult.globalAccess,
                )
            }

            // Proceed with method execution if access is granted
            val result = joinPoint.proceed()

            // Log successful completion
            val duration = System.currentTimeMillis() - startTime
            logger.debug(
                "Tenant access validation completed for {} in {}ms. User: {}, Target: {}, Access: {}",
                methodName,
                duration,
                userTenantId,
                targetTenantId,
                accessResult.reason,
            )

            return result
        } catch (e: SecurityException) {
            // Log security violation
            val duration = System.currentTimeMillis() - startTime
            logger.warn(
                "Tenant access denied for {} after {}ms: {}",
                methodName,
                duration,
                e.message,
            )

            // Audit the security violation
            if (requiresTenantAccess.auditAccess) {
                auditSecurityViolation(methodName, e)
            }

            throw e
        } catch (e: Exception) {
            logger.error(
                "Unexpected error during tenant access validation for {}: {}",
                methodName,
                e.message,
                e,
            )
            throw e
        }
    }

    /** Validates whether the user has access to the target tenant. */
    private fun validateTenantAccess(
        userTenantId: String,
        targetTenantId: String?,
        allowGlobalAccess: Boolean,
        methodName: String,
    ): TenantAccessResult {
        // If no target tenant is specified, allow access (method doesn't require tenant-specific
        // validation)
        if (targetTenantId == null) {
            return TenantAccessResult(
                granted = true,
                reason = "No target tenant specified",
                globalAccess = false,
            )
        }

        // Check if user has global access privileges
        if (allowGlobalAccess && hasGlobalAccess()) {
            return TenantAccessResult(
                granted = true,
                reason = "Global administrator access",
                globalAccess = true,
            )
        }

        // Check if user is accessing their own tenant
        if (userTenantId == targetTenantId) {
            return TenantAccessResult(
                granted = true,
                reason = "Same tenant access",
                globalAccess = false,
            )
        }

        // Access denied - user trying to access different tenant without global privileges
        throw InsufficientPermissionException.forTenantAccess(
            userTenantId = userTenantId,
            requestedTenantId = targetTenantId,
            action = methodName,
        )
    }

    /** Checks if the current user has global access privileges. */
    private fun hasGlobalAccess(): Boolean = securityContextHolder.hasAnyRole("SUPER_ADMIN", "PLATFORM_ADMIN")

    /** Extracts tenant ID from method arguments. */
    private fun extractTenantIdFromArguments(
        joinPoint: ProceedingJoinPoint,
        tenantIdParamName: String,
    ): String? {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val parameterNames = signature.parameterNames
        val args = joinPoint.args

        // Look for parameter by name
        val paramIndex = parameterNames.indexOf(tenantIdParamName)
        if (paramIndex >= 0 && paramIndex < args.size) {
            val value = args[paramIndex]
            return when (value) {
                is String -> value
                is com.axians.eaf.controlplane.domain.model.tenant.TenantId -> value.value
                else -> value?.toString()
            }
        }

        // Look for DTOs or objects that might contain tenant ID
        for (arg in args) {
            val tenantId = extractTenantIdFromObject(arg, tenantIdParamName)
            if (tenantId != null) {
                return tenantId
            }
        }

        // Look for @PathVariable or @RequestParam annotations (if available)
        return extractTenantIdFromAnnotations(method.parameters, args, tenantIdParamName)
    }

    /** Attempts to extract tenant ID from object properties. */
    private fun extractTenantIdFromObject(
        obj: Any?,
        tenantIdParamName: String,
    ): String? {
        if (obj == null) return null

        try {
            val clazz = obj.javaClass

            // Try to find a field with the specified name
            try {
                val field = clazz.getDeclaredField(tenantIdParamName)
                field.isAccessible = true
                val value = field.get(obj)
                return when (value) {
                    is String -> value
                    is com.axians.eaf.controlplane.domain.model.tenant.TenantId -> value.value
                    else -> value?.toString()
                }
            } catch (e: NoSuchFieldException) {
                // Field not found, try getter method
            }

            // Try to find a getter method
            val getterNames =
                listOf(
                    "get${tenantIdParamName.replaceFirstChar { it.uppercase() }}",
                    "get$tenantIdParamName",
                    tenantIdParamName,
                )

            for (getterName in getterNames) {
                try {
                    val method = clazz.getDeclaredMethod(getterName)
                    method.isAccessible = true
                    val value = method.invoke(obj)
                    return when (value) {
                        is String -> value
                        is com.axians.eaf.controlplane.domain.model.tenant.TenantId -> value.value
                        else -> value?.toString()
                    }
                } catch (e: NoSuchMethodException) {
                    // Method not found, continue trying
                }
            }
        } catch (e: Exception) {
            logger.debug("Could not extract tenant ID from object: {}", e.message)
        }

        return null
    }

    /** Attempts to extract tenant ID from method parameter annotations. */
    private fun extractTenantIdFromAnnotations(
        parameters: Array<Parameter>,
        args: Array<Any?>,
        tenantIdParamName: String,
    ): String? {
        // This could be extended to handle @PathVariable, @RequestParam, etc.
        // For now, we'll just return null as those are typically handled by the framework
        return null
    }

    /** Logs tenant access attempts for audit purposes. */
    private fun logTenantAccess(
        methodName: String,
        userTenantId: String,
        targetTenantId: String?,
        accessGranted: Boolean,
        accessReason: String,
        globalAccess: Boolean,
    ) {
        try {
            kotlinx.coroutines.runBlocking {
                auditService.logAdminAction(
                    action =
                        if (accessGranted) {
                            AdminAction.CUSTOM_ACTION
                        } else {
                            AdminAction.ACCESS_DENIED
                        },
                    targetType = "tenant_access",
                    targetId = targetTenantId ?: "unknown",
                    details =
                        mapOf(
                            "method" to methodName,
                            "userTenantId" to userTenantId,
                            "targetTenantId" to (targetTenantId ?: "null"),
                            "accessReason" to accessReason,
                            "globalAccess" to globalAccess,
                            "accessType" to
                                if (accessGranted) {
                                    "ACCESS_GRANTED"
                                } else {
                                    "ACCESS_DENIED"
                                },
                            "timestamp" to System.currentTimeMillis(),
                        ),
                )
            }
        } catch (e: Exception) {
            logger.warn("Failed to log tenant access audit entry: {}", e.message)
        }
    }

    /** Audits security violations. */
    private fun auditSecurityViolation(
        methodName: String,
        exception: SecurityException,
    ) {
        try {
            kotlinx.coroutines.runBlocking {
                auditService.logAdminAction(
                    action = AdminAction.ACCESS_DENIED,
                    targetType = "security_violation",
                    targetId = methodName,
                    details =
                        mapOf(
                            "violationType" to exception.javaClass.simpleName,
                            "message" to (exception.message ?: "Unknown error"),
                            "method" to methodName,
                            "timestamp" to System.currentTimeMillis(),
                        ),
                )
            }
        } catch (e: Exception) {
            logger.warn("Failed to log security violation audit entry: {}", e.message)
        }
    }

    /** Result of tenant access validation. */
    private data class TenantAccessResult(
        val granted: Boolean,
        val reason: String,
        val globalAccess: Boolean,
    )
}

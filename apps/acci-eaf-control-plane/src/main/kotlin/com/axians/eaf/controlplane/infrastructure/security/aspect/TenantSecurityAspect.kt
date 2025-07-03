package com.axians.eaf.controlplane.infrastructure.security.aspect

import com.axians.eaf.controlplane.domain.event.SecurityAuditEvent
import com.axians.eaf.controlplane.domain.event.TenantAccessContext
import com.axians.eaf.controlplane.domain.exception.AuthenticationRequiredException
import com.axians.eaf.controlplane.domain.exception.InsufficientPermissionException
import com.axians.eaf.controlplane.infrastructure.security.annotation.RequiresTenantAccess
import com.axians.eaf.core.security.EafSecurityContextHolder
import com.axians.eaf.eventing.NatsEventPublisher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.util.concurrent.TimeoutException

/**
 * Security aspect that enforces multi-tenant access control.
 *
 * This aspect intercepts methods annotated with @RequiresTenantAccess and validates that the
 * current user has proper access to the target tenant based on:
 * 1. User's tenant context
 * 2. Target tenant ID from method parameters
 * 3. User's roles and permissions
 * 4. Global access rules for super administrators
 *
 * **Refactored for Story 4.2.3**: Removed circular dependency on AuditService. Audit logging is now
 * handled through SecurityAuditEvent publishing. Added comprehensive MDC context enrichment for
 * structured logging.
 */
@Aspect
@Component
class TenantSecurityAspect(
    private val securityContextHolder: EafSecurityContextHolder,
    private val eventPublisher: NatsEventPublisher,
) {
    private val logger = LoggerFactory.getLogger(TenantSecurityAspect::class.java)

    /**
     * Intercepts method calls annotated with @RequiresTenantAccess. Validates tenant access,
     * enriches MDC logging context, and publishes security audit events.
     */
    @Around("@annotation(requiresTenantAccess)")
    fun enforceTenantAccess(
        joinPoint: ProceedingJoinPoint,
        requiresTenantAccess: RequiresTenantAccess,
    ): Any? {
        val startTime = System.currentTimeMillis()
        val securityContext = extractSecurityContext(joinPoint, requiresTenantAccess)

        enrichMdcContext(securityContext)

        return try {
            executeSecurityValidation(joinPoint, requiresTenantAccess, securityContext, startTime)
        } finally {
            cleanupMdcContext()
        }
    }

    private fun executeSecurityValidation(
        joinPoint: ProceedingJoinPoint,
        requiresTenantAccess: RequiresTenantAccess,
        securityContext: SecurityContext,
        startTime: Long,
    ): Any? =
        try {
            validateAuthentication()
            val validationResult =
                performTenantValidation(joinPoint, requiresTenantAccess, securityContext)

            if (requiresTenantAccess.auditAccess) {
                publishTenantAccessEvent(validationResult.auditContext)
            }

            updateMdcWithTargetTenant(
                validationResult.targetTenantId,
                validationResult.validUserTenantId,
            )

            val result = joinPoint.proceed()
            logSuccessfulAccess(securityContext.methodName, startTime, validationResult)

            result
        } catch (e: SecurityException) {
            handleSecurityException(
                SecurityExceptionContext(
                    exception = e,
                    methodName = securityContext.methodName,
                    startTime = startTime,
                    userTenantId = securityContext.userTenantId,
                    userId = securityContext.userId,
                    correlationId = securityContext.correlationId,
                    auditAccess = requiresTenantAccess.auditAccess,
                ),
            )
            throw e
        } catch (e: IllegalArgumentException) {
            logger.error(
                "Invalid argument during tenant access validation for {}: {}",
                securityContext.methodName,
                e.message,
                e,
            )
            throw e
        } catch (e: IllegalStateException) {
            logger.error(
                "Invalid state during tenant access validation for {}: {}",
                securityContext.methodName,
                e.message,
                e,
            )
            throw e
        } catch (e: TimeoutException) {
            logger.error(
                "Timeout during tenant access validation for {}: {}",
                securityContext.methodName,
                e.message,
                e,
            )
            throw IllegalStateException("Security validation timeout", e)
        }

    private fun extractSecurityContext(
        joinPoint: ProceedingJoinPoint,
        requiresTenantAccess: RequiresTenantAccess,
    ): SecurityContext {
        val methodName = "${joinPoint.signature.declaringTypeName}.${joinPoint.signature.name}"
        val userTenantId = securityContextHolder.getTenantIdOrNull()
        val userId = securityContextHolder.getUserId()
        val correlationId =
            MDC.get("correlation_id") ?: java.util.UUID
                .randomUUID()
                .toString()

        return SecurityContext(
            methodName = methodName,
            userTenantId = userTenantId,
            userId = userId,
            correlationId = correlationId,
        )
    }

    private fun performTenantValidation(
        joinPoint: ProceedingJoinPoint,
        requiresTenantAccess: RequiresTenantAccess,
        securityContext: SecurityContext,
    ): ValidationResult {
        val validUserTenantId =
            securityContext.userTenantId
                ?: throw AuthenticationRequiredException(
                    "No tenant context found in authentication",
                )

        val targetTenantId =
            extractTenantIdFromArguments(joinPoint, requiresTenantAccess.tenantIdParamName)

        val accessResult =
            validateTenantAccess(
                userTenantId = validUserTenantId,
                targetTenantId = targetTenantId,
                allowGlobalAccess = requiresTenantAccess.allowGlobalAccess,
                methodName = securityContext.methodName,
            )

        val auditContext =
            TenantAccessAuditContext(
                methodName = securityContext.methodName,
                userTenantId = validUserTenantId,
                userId = securityContext.userId,
                targetTenantId = targetTenantId,
                accessGranted = accessResult.granted,
                accessReason = accessResult.reason,
                globalAccess = accessResult.globalAccess,
                correlationId = securityContext.correlationId,
            )

        return ValidationResult(
            accessResult = accessResult,
            targetTenantId = targetTenantId,
            validUserTenantId = validUserTenantId,
            auditContext = auditContext,
        )
    }

    private fun updateMdcWithTargetTenant(
        targetTenantId: String?,
        userTenantId: String,
    ) {
        if (targetTenantId != null && targetTenantId != userTenantId) {
            MDC.put("target_tenant_id", targetTenantId)
        }
    }

    /** Validates that the user is authenticated. */
    private fun validateAuthentication() {
        if (!securityContextHolder.isAuthenticated()) {
            throw AuthenticationRequiredException(
                "Authentication required for tenant-scoped operation",
            )
        }
    }

    /** Logs successful tenant access. */
    private fun logSuccessfulAccess(
        methodName: String,
        startTime: Long,
        validationResult: ValidationResult,
    ) {
        val duration = System.currentTimeMillis() - startTime
        logger.debug(
            "Tenant access validation completed for {} in {}ms. User: {}, Target: {}, Access: {}",
            methodName,
            duration,
            validationResult.validUserTenantId,
            validationResult.targetTenantId,
            validationResult.accessResult.reason,
        )
    }

    /** Handles security exceptions with proper logging and audit events. */
    private fun handleSecurityException(context: SecurityExceptionContext) {
        val duration = System.currentTimeMillis() - context.startTime
        logger.warn(
            "Tenant access denied for {} after {}ms: {}",
            context.methodName,
            duration,
            context.exception.message,
        )

        if (context.auditAccess) {
            publishSecurityViolationEvent(
                SecurityViolationContext(
                    methodName = context.methodName,
                    tenantId = context.userTenantId,
                    userId = context.userId,
                    exception = context.exception,
                    correlationId = context.correlationId,
                ),
            )
        }
    }

    /** Cleans up MDC context after request processing. */
    private fun cleanupMdcContext() {
        MDC.remove("tenant_id")
        MDC.remove("user_id")
        MDC.remove("security_method")
        MDC.remove("target_tenant_id")
        MDC.remove("user_roles")
    }

    /** Enriches MDC logging context with security-related information for structured logging. */
    private fun enrichMdcContext(securityContext: SecurityContext) {
        MDC.put("tenant_id", securityContext.userTenantId ?: "unknown")
        MDC.put("user_id", securityContext.userId ?: "system")
        MDC.put("security_method", securityContext.methodName)
        MDC.put("correlation_id", securityContext.correlationId)

        // Add security context metadata
        if (securityContextHolder.isAuthenticated()) {
            val roles =
                securityContextHolder.getAuthentication()?.authorities?.joinToString(",") {
                    it.authority.removePrefix("ROLE_")
                }
            if (!roles.isNullOrBlank()) {
                MDC.put("user_roles", roles)
            }
        }
    }

    /**
     * Publishes tenant access event for audit trail via event-driven architecture. Replaces direct
     * audit service calls to eliminate circular dependency.
     */
    private fun publishTenantAccessEvent(context: TenantAccessAuditContext) {
        try {
            val securityEvent =
                SecurityAuditEvent.tenantAccess(
                    TenantAccessContext(
                        tenantId = context.userTenantId,
                        userId = context.userId,
                        methodName = context.methodName,
                        userTenantId = context.userTenantId,
                        targetTenantId = context.targetTenantId,
                        accessGranted = context.accessGranted,
                        accessReason = context.accessReason,
                        globalAccess = context.globalAccess,
                        correlationId = context.correlationId,
                    ),
                )

            // Publish event asynchronously to avoid blocking security validation
            GlobalScope.launch {
                try {
                    eventPublisher.publish(
                        subject = "security.audit.tenant_access",
                        tenantId = context.userTenantId,
                        event = securityEvent,
                    )
                } catch (e: IllegalStateException) {
                    logger.warn(
                        "Failed to publish tenant access audit event - invalid state: {}",
                        e.message,
                        e,
                    )
                } catch (e: TimeoutException) {
                    logger.warn(
                        "Failed to publish tenant access audit event - timeout: {}",
                        e.message,
                        e,
                    )
                } catch (e: SecurityException) {
                    logger.warn(
                        "Failed to publish tenant access audit event - security error: {}",
                        e.message,
                        e,
                    )
                }
            }

            logger.debug(
                "Published tenant access audit event for method {} with outcome: {}",
                context.methodName,
                if (context.accessGranted) "GRANTED" else "DENIED",
            )
        } catch (e: IllegalArgumentException) {
            // Event creation failure should not break security validation
            logger.warn("Failed to create tenant access audit event: {}", e.message, e)
        } catch (e: IllegalStateException) {
            // Event publishing state issue should not break security validation
            logger.warn(
                "Failed to publish tenant access audit event due to invalid state: {}",
                e.message,
                e,
            )
        }
    }

    /**
     * Publishes security violation event for audit trail via event-driven architecture. Replaces
     * direct audit service calls to eliminate circular dependency.
     */
    private fun publishSecurityViolationEvent(context: SecurityViolationContext) {
        try {
            val securityEvent =
                SecurityAuditEvent.securityViolation(
                    tenantId = context.tenantId,
                    userId = context.userId,
                    methodName = context.methodName,
                    exception = context.exception,
                    correlationId = context.correlationId,
                )

            // Publish event asynchronously to avoid blocking security validation
            GlobalScope.launch {
                try {
                    eventPublisher.publish(
                        subject = "security.audit.violation",
                        tenantId = context.tenantId ?: "unknown",
                        event = securityEvent,
                    )
                } catch (e: IllegalStateException) {
                    logger.warn(
                        "Failed to publish security violation audit event - invalid state: {}",
                        e.message,
                        e,
                    )
                } catch (e: TimeoutException) {
                    logger.warn(
                        "Failed to publish security violation audit event - timeout: {}",
                        e.message,
                        e,
                    )
                } catch (e: SecurityException) {
                    logger.warn(
                        "Failed to publish security violation audit event - security error: {}",
                        e.message,
                        e,
                    )
                }
            }

            logger.debug(
                "Published security violation audit event for method {} with exception: {}",
                context.methodName,
                context.exception.javaClass.simpleName,
            )
        } catch (e: IllegalArgumentException) {
            // Event creation failure should not break security validation
            logger.warn("Failed to create security violation audit event: {}", e.message, e)
        } catch (e: IllegalStateException) {
            // Event publishing state issue should not break security validation
            logger.warn(
                "Failed to publish security violation audit event due to invalid state: {}",
                e.message,
                e,
            )
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

        return null
    }

    /** Attempts to extract tenant ID from object properties. */
    private fun extractTenantIdFromObject(
        obj: Any?,
        tenantIdParamName: String,
    ): String? {
        if (obj == null) return null

        return extractFromField(obj, tenantIdParamName) ?: extractFromGetter(obj, tenantIdParamName)
    }

    /** Extracts tenant ID from object field. */
    private fun extractFromField(
        obj: Any,
        tenantIdParamName: String,
    ): String? =
        try {
            val field = obj.javaClass.getDeclaredField(tenantIdParamName)
            field.isAccessible = true
            val value = field.get(obj)
            when (value) {
                is String -> value
                is com.axians.eaf.controlplane.domain.model.tenant.TenantId -> value.value
                else -> value?.toString()
            }
        } catch (e: NoSuchFieldException) {
            null
        } catch (e: SecurityException) {
            logger.debug(
                "Security exception accessing field {}: {}",
                tenantIdParamName,
                e.message,
            )
            null
        } catch (e: IllegalAccessException) {
            logger.debug("Illegal access to field {}: {}", tenantIdParamName, e.message)
            null
        }

    /** Extracts tenant ID from getter method. */
    private fun extractFromGetter(
        obj: Any,
        tenantIdParamName: String,
    ): String? {
        val getterNames =
            listOf(
                "get${tenantIdParamName.replaceFirstChar { it.uppercase() }}",
                "get$tenantIdParamName",
                tenantIdParamName,
            )

        for (getterName in getterNames) {
            try {
                val method = obj.javaClass.getDeclaredMethod(getterName)
                method.isAccessible = true
                val value = method.invoke(obj)
                return when (value) {
                    is String -> value
                    is com.axians.eaf.controlplane.domain.model.tenant.TenantId -> value.value
                    else -> value?.toString()
                }
            } catch (e: NoSuchMethodException) {
                continue
            } catch (e: SecurityException) {
                logger.debug("Security exception accessing method {}: {}", getterName, e.message)
                continue
            } catch (e: IllegalAccessException) {
                logger.debug("Illegal access to method {}: {}", getterName, e.message)
                continue
            } catch (e: java.lang.reflect.InvocationTargetException) {
                logger.debug("Error invoking method {}: {}", getterName, e.targetException?.message)
                continue
            }
        }
        return null
    }

    /** Security context data for method validation. */
    private data class SecurityContext(
        val methodName: String,
        val userTenantId: String?,
        val userId: String?,
        val correlationId: String,
    )

    /** Context for tenant access audit events. */
    private data class TenantAccessAuditContext(
        val methodName: String,
        val userTenantId: String,
        val userId: String?,
        val targetTenantId: String?,
        val accessGranted: Boolean,
        val accessReason: String,
        val globalAccess: Boolean,
        val correlationId: String,
    )

    /** Context for security violation events. */
    private data class SecurityViolationContext(
        val methodName: String,
        val tenantId: String?,
        val userId: String?,
        val exception: SecurityException,
        val correlationId: String,
    )

    /** Context for handling security exceptions. */
    private data class SecurityExceptionContext(
        val exception: SecurityException,
        val methodName: String,
        val startTime: Long,
        val userTenantId: String?,
        val userId: String?,
        val correlationId: String,
        val auditAccess: Boolean,
    )

    /** Result of validation process. */
    private data class ValidationResult(
        val accessResult: TenantAccessResult,
        val targetTenantId: String?,
        val validUserTenantId: String,
        val auditContext: TenantAccessAuditContext,
    )

    /** Result of tenant access validation. */
    private data class TenantAccessResult(
        val granted: Boolean,
        val reason: String,
        val globalAccess: Boolean,
    )
}

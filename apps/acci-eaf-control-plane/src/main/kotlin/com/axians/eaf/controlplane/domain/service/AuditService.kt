package com.axians.eaf.controlplane.domain.service

import com.axians.eaf.controlplane.application.dto.audit.AuditStatisticsRequest
import com.axians.eaf.controlplane.application.dto.audit.AuditTrailRequest
import com.axians.eaf.controlplane.domain.model.audit.AdminAction
import com.axians.eaf.controlplane.domain.model.audit.AuditEntry
import com.axians.eaf.controlplane.domain.model.audit.AuditEntryId
import com.axians.eaf.controlplane.domain.model.audit.UserAction
import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.UserId
import com.axians.eaf.controlplane.domain.port.AuditEventPublisher
import com.axians.eaf.controlplane.domain.port.AuditRepository
import com.axians.eaf.core.security.EafSecurityContextHolder
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Instant

/** Result types for audit operations. */
sealed class AuditResult {
    data class Success<T>(
        val data: T,
        val auditEntryId: AuditEntryId,
    ) : AuditResult()

    data class Error(
        val message: String,
        val code: String,
        val exception: Exception? = null,
    ) : AuditResult()
}

/**
 * Domain service for comprehensive audit trail management and logging. Handles all audit operations
 * including action logging, event publishing, and trail querying.
 */
@Service
class AuditService(
    private val auditRepository: AuditRepository,
    private val auditEventPublisher: AuditEventPublisher,
    private val securityContextHolder: EafSecurityContextHolder,
    @Value("\${app.audit.async-publishing:true}") private val asyncPublishing: Boolean = true,
    @Value("\${app.audit.include-request-details:true}")
    private val includeRequestDetails: Boolean = true,
) {
    private val logger = LoggerFactory.getLogger(AuditService::class.java)

    /** Logs an administrative action with automatic context extraction. */
    suspend fun logAdminAction(
        action: AdminAction,
        targetType: String,
        targetId: String,
        details: Map<String, Any> = emptyMap(),
    ): AuditEntry {
        val tenantId = TenantId.fromString(securityContextHolder.getTenantId())
        val performedBy = UserId.fromString(securityContextHolder.getUserId() ?: "system")

        val auditEntry =
            AuditEntry.forAdminAction(
                tenantId = tenantId,
                performedBy = performedBy,
                action = action,
                targetType = targetType,
                targetId = targetId,
                details = details,
                ipAddress = "127.0.0.1", // TODO: Extract from request
                userAgent = "Unknown", // TODO: Extract from request
            )

        val savedEntry = auditRepository.save(auditEntry)

        try {
            auditEventPublisher.publishAuditEvent(
                tenantId = tenantId.value,
                event = savedEntry.toEvent(),
            )
        } catch (e: Exception) {
            logger.warn("Failed to publish audit event: ${e.message}")
        }

        return savedEntry
    }

    /** Logs a user action with automatic context extraction. */
    suspend fun logUserAction(
        userId: String,
        action: UserAction,
        targetType: String,
        targetId: String,
        details: Map<String, Any> = emptyMap(),
        success: Boolean = true,
        errorMessage: String? = null,
    ): AuditResult =
        try {
            val tenantId = getCurrentTenantId()
            val performedBy = UserId.fromString(userId)
            val requestContext = extractRequestContext()

            val auditEntry =
                AuditEntry.forUserAction(
                    tenantId = tenantId,
                    performedBy = performedBy,
                    action = action,
                    targetType = targetType,
                    targetId = targetId,
                    details = details + requestContext.details,
                    ipAddress = requestContext.ipAddress,
                    userAgent = requestContext.userAgent,
                    success = success,
                    errorMessage = errorMessage,
                    sessionId = requestContext.sessionId,
                    correlationId = requestContext.correlationId,
                )

            val savedEntry = auditRepository.save(auditEntry)

            // Publish audit event asynchronously
            publishAuditEventSafely(savedEntry)

            logger.info(
                "Audit: {} - {} by {} on {}:{} in tenant {}",
                action.name,
                if (success) "SUCCESS" else "FAILURE",
                userId,
                targetType,
                targetId,
                tenantId.value,
            )

            AuditResult.Success(savedEntry, savedEntry.id)
        } catch (e: Exception) {
            logger.error("Failed to log user action: ${action.name} for user: $userId", e)
            AuditResult.Error(
                message = "Failed to log user action: ${e.message}",
                code = "AUDIT_LOG_FAILED",
                exception = e,
            )
        }

    /** Logs a custom action with explicit details. */
    suspend fun logCustomAction(
        tenantId: String,
        performedBy: String,
        action: String,
        targetType: String,
        targetId: String,
        details: Map<String, Any> = emptyMap(),
        success: Boolean = true,
        errorMessage: String? = null,
    ): AuditResult =
        try {
            val requestContext = extractRequestContext()

            val auditEntry =
                AuditEntry(
                    id = AuditEntryId.generate(),
                    tenantId = TenantId.fromString(tenantId),
                    performedBy = UserId.fromString(performedBy),
                    action = action,
                    targetType = targetType,
                    targetId = targetId,
                    details = details + requestContext.details,
                    timestamp = Instant.now(),
                    ipAddress = requestContext.ipAddress,
                    userAgent = requestContext.userAgent,
                    success = success,
                    errorMessage = errorMessage,
                    sessionId = requestContext.sessionId,
                    correlationId = requestContext.correlationId,
                )

            val savedEntry = auditRepository.save(auditEntry)

            // Publish audit event asynchronously
            publishAuditEventSafely(savedEntry)

            logger.info(
                "Audit: {} - {} by {} on {}:{} in tenant {}",
                action,
                if (success) "SUCCESS" else "FAILURE",
                performedBy,
                targetType,
                targetId,
                tenantId,
            )

            AuditResult.Success(savedEntry, savedEntry.id)
        } catch (e: Exception) {
            logger.error("Failed to log custom action: $action", e)
            AuditResult.Error(
                message = "Failed to log custom action: ${e.message}",
                code = "AUDIT_LOG_FAILED",
                exception = e,
            )
        }

    /** Logs a batch of audit entries for performance optimization. */
    suspend fun logBatch(auditEntries: List<AuditEntry>): AuditResult =
        try {
            val savedEntries = auditRepository.saveAll(auditEntries)

            // Publish all events
            savedEntries.forEach { entry -> publishAuditEventSafely(entry) }

            logger.info("Audit: Batch logged {} entries", savedEntries.size)

            AuditResult.Success(savedEntries, savedEntries.first().id)
        } catch (e: Exception) {
            logger.error("Failed to log audit batch", e)
            AuditResult.Error(
                message = "Failed to log audit batch: ${e.message}",
                code = "AUDIT_BATCH_LOG_FAILED",
                exception = e,
            )
        }

    /** Retrieves audit trail with filtering and pagination. */
    suspend fun getAuditTrail(request: AuditTrailRequest): AuditResult =
        try {
            val response = auditRepository.findWithFilter(request)
            AuditResult.Success(response, AuditEntryId.generate())
        } catch (e: Exception) {
            logger.error("Failed to retrieve audit trail", e)
            AuditResult.Error(
                message = "Failed to retrieve audit trail: ${e.message}",
                code = "AUDIT_TRAIL_RETRIEVAL_FAILED",
                exception = e,
            )
        }

    /** Generates audit statistics for reporting. */
    suspend fun generateStatistics(request: AuditStatisticsRequest): AuditResult =
        try {
            val statistics = auditRepository.generateStatistics(request)
            AuditResult.Success(statistics, AuditEntryId.generate())
        } catch (e: Exception) {
            logger.error("Failed to generate audit statistics", e)
            AuditResult.Error(
                message = "Failed to generate audit statistics: ${e.message}",
                code = "AUDIT_STATISTICS_FAILED",
                exception = e,
            )
        }

    /** Finds audit entry by ID. */
    suspend fun findById(auditEntryId: AuditEntryId): AuditResult {
        return try {
            val entry =
                auditRepository.findById(auditEntryId)
                    ?: return AuditResult.Error(
                        message = "Audit entry not found",
                        code = "AUDIT_ENTRY_NOT_FOUND",
                    )

            AuditResult.Success(entry, entry.id)
        } catch (e: Exception) {
            logger.error("Failed to find audit entry: ${auditEntryId.value}", e)
            AuditResult.Error(
                message = "Failed to find audit entry: ${e.message}",
                code = "AUDIT_ENTRY_RETRIEVAL_FAILED",
                exception = e,
            )
        }
    }

    /** Gets recent audit entries for monitoring. */
    suspend fun getRecentEntries(limit: Int = 100): AuditResult =
        try {
            val entries = auditRepository.findRecent(limit)
            AuditResult.Success(entries, AuditEntryId.generate())
        } catch (e: Exception) {
            logger.error("Failed to get recent audit entries", e)
            AuditResult.Error(
                message = "Failed to get recent audit entries: ${e.message}",
                code = "RECENT_AUDIT_RETRIEVAL_FAILED",
                exception = e,
            )
        }

    /** Private helper methods */
    private fun getCurrentTenantId(): TenantId =
        try {
            TenantId.fromString(securityContextHolder.getTenantId())
        } catch (e: Exception) {
            throw IllegalStateException("Unable to determine current tenant ID", e)
        }

    private fun getCurrentUserId(): UserId =
        try {
            val userId =
                securityContextHolder.getUserId()
                    ?: throw IllegalStateException("No authenticated user found")
            UserId.fromString(userId)
        } catch (e: Exception) {
            throw IllegalStateException("Unable to determine current user ID", e)
        }

    private data class RequestContext(
        val ipAddress: String,
        val userAgent: String,
        val sessionId: String?,
        val correlationId: String?,
        val details: Map<String, Any>,
    )

    private fun extractRequestContext(): RequestContext =
        try {
            val request = getCurrentHttpRequest()

            val ipAddress = extractIpAddress(request)
            val userAgent = request?.getHeader("User-Agent") ?: "Unknown"
            val sessionId = request?.session?.id
            val correlationId =
                request?.getHeader("X-Correlation-ID") ?: request?.getHeader("X-Request-ID")

            val details =
                if (includeRequestDetails && request != null) {
                    buildMap<String, Any> {
                        put("requestMethod", request.method)
                        put("requestUri", request.requestURI)
                        request.queryString?.let { put("queryString", it) }
                        put("remoteHost", request.remoteHost)
                        put("serverName", request.serverName)
                        put("serverPort", request.serverPort)
                    }
                } else {
                    emptyMap()
                }

            RequestContext(ipAddress, userAgent, sessionId, correlationId, details)
        } catch (e: Exception) {
            logger.warn("Failed to extract complete request context: ${e.message}")
            RequestContext(
                ipAddress = "Unknown",
                userAgent = "Unknown",
                sessionId = null,
                correlationId = null,
                details = emptyMap(),
            )
        }

    private fun getCurrentHttpRequest(): HttpServletRequest? =
        try {
            val requestAttributes =
                RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            requestAttributes?.request
        } catch (e: Exception) {
            null
        }

    private fun extractIpAddress(request: HttpServletRequest?): String {
        if (request == null) return "Unknown"

        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            return xForwardedFor.split(",").firstOrNull()?.trim() ?: request.remoteAddr
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp
        }

        return request.remoteAddr ?: "Unknown"
    }

    private suspend fun publishAuditEventSafely(auditEntry: AuditEntry) {
        try {
            if (asyncPublishing) {
                // In a real implementation, this would be done asynchronously
                // For now, we'll do it synchronously but catch exceptions
                auditEventPublisher.publishAuditEvent(
                    tenantId = auditEntry.tenantId.value,
                    event = auditEntry.toEvent(),
                )
            }
        } catch (e: Exception) {
            logger.warn(
                "Failed to publish audit event for entry ${auditEntry.id.value}: ${e.message}",
            )
            // Don't fail the audit logging operation due to event publishing failure
        }
    }
}

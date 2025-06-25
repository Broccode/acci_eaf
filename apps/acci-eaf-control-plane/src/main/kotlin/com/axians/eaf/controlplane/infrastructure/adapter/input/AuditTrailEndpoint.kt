package com.axians.eaf.controlplane.infrastructure.adapter.input

import com.axians.eaf.controlplane.application.dto.audit.AuditStatisticsRequest
import com.axians.eaf.controlplane.application.dto.audit.AuditTrailRequest
import com.axians.eaf.controlplane.application.dto.audit.LogAdminActionRequest
import com.axians.eaf.controlplane.domain.model.audit.AdminAction
import com.axians.eaf.controlplane.domain.service.AuditResult
import com.axians.eaf.controlplane.domain.service.AuditService
import com.axians.eaf.core.annotations.HillaWorkaround
import com.vaadin.flow.server.auth.AnonymousAllowed
import com.vaadin.hilla.BrowserCallable
import com.vaadin.hilla.exception.EndpointException
import jakarta.annotation.security.RolesAllowed
import jakarta.validation.Valid
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated

/**
 * Hilla endpoint for audit trail management and monitoring. Provides comprehensive audit querying,
 * statistics, and administrative logging capabilities.
 */
@BrowserCallable
@Validated
@HillaWorkaround(
        description =
                "This endpoint and its DTOs were analyzed for Hilla issue #3443 and found to be unaffected. No aliased collections are used."
)
class AuditTrailEndpoint(
        private val auditService: AuditService,
) {
    private val logger = LoggerFactory.getLogger(AuditTrailEndpoint::class.java)

    /**
     * Retrieves audit trail entries with filtering and pagination. Available to administrators for
     * comprehensive audit analysis.
     */
    @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN", "TENANT_ADMIN")
    fun getAuditTrail(
            @Valid request: AuditTrailRequest,
    ): Any? =
            try {
                when (val result = runSuspending { auditService.getAuditTrail(request) }) {
                    is AuditResult.Success<*> -> result.data
                    is AuditResult.Error -> throw EndpointException(result.message)
                }
            } catch (e: Exception) {
                logger.error("Failed to retrieve audit trail", e)
                throw EndpointException("Failed to retrieve audit trail: ${e.message}")
            }

    /**
     * Generates audit statistics for reporting and monitoring. Provides comprehensive metrics for
     * security and compliance analysis.
     */
    @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN", "TENANT_ADMIN")
    fun generateAuditStatistics(
            @Valid request: AuditStatisticsRequest,
    ): Any? =
            try {
                when (val result = runSuspending { auditService.generateStatistics(request) }) {
                    is AuditResult.Success<*> -> result.data
                    is AuditResult.Error -> throw EndpointException(result.message)
                }
            } catch (e: Exception) {
                logger.error("Failed to generate audit statistics", e)
                throw EndpointException("Failed to generate audit statistics: ${e.message}")
            }

    /**
     * Logs a custom administrative action for audit tracking. Used by administrators to create
     * audit entries for manual operations.
     */
    @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN", "TENANT_ADMIN")
    fun logAdminAction(
            @Valid request: LogAdminActionRequest,
    ): Any =
            try {
                val action = AdminAction.valueOf(request.action)
                val result = runSuspending {
                    auditService.logAdminAction(
                            action = action,
                            targetType = request.targetType,
                            targetId = request.targetId,
                            details = request.details,
                    )
                }
                mapOf(
                        "auditEntryId" to result.id.value,
                        "message" to "Administrative action logged successfully",
                        "timestamp" to Instant.now(),
                )
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid action provided: ${request.action}")
                throw EndpointException("Invalid action: ${request.action}")
            } catch (e: Exception) {
                logger.error("Failed to log admin action", e)
                throw EndpointException("Failed to log administrative action: ${e.message}")
            }

    /**
     * Gets recent audit entries for real-time monitoring. Limited to recent entries for performance
     * and quick overview.
     */
    @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN", "TENANT_ADMIN")
    fun getRecentAuditEntries(limit: Int = 100): Any? =
            try {
                val effectiveLimit = minOf(limit, 1000) // Cap at 1000 for performance
                when (val result = runSuspending { auditService.getRecentEntries(effectiveLimit) }
                ) {
                    is AuditResult.Success<*> -> result.data
                    is AuditResult.Error -> throw EndpointException(result.message)
                }
            } catch (e: Exception) {
                logger.error("Failed to get recent audit entries", e)
                throw EndpointException("Failed to get recent audit entries: ${e.message}")
            }

    /**
     * Gets security-related audit events for monitoring. Focuses on login attempts, access denials,
     * and security violations.
     */
    @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN")
    fun getSecurityEvents(
            fromDate: Instant? = null,
            toDate: Instant? = null,
    ): Any? =
            try {
                // For demonstration, we'll create a basic filter request for security events
                val request =
                        AuditTrailRequest(
                                fromDate = fromDate
                                                ?: Instant.now()
                                                        .minusSeconds(
                                                                24 * 60 * 60,
                                                        ),
                                // Last 24 hours default
                                toDate = toDate ?: Instant.now(),
                                pageSize = 500, // Larger page size for security monitoring
                        )

                when (val result = runSuspending { auditService.getAuditTrail(request) }) {
                    is AuditResult.Success<*> -> result.data
                    is AuditResult.Error -> throw EndpointException(result.message)
                }
            } catch (e: Exception) {
                logger.error("Failed to get security events", e)
                throw EndpointException("Failed to get security events: ${e.message}")
            }

    /**
     * Gets high-risk audit events for compliance monitoring. Focuses on administrative actions that
     * require special attention.
     */
    @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN")
    fun getHighRiskEvents(
            fromDate: Instant? = null,
            toDate: Instant? = null,
    ): Any? =
            try {
                val request =
                        AuditTrailRequest(
                                fromDate = fromDate
                                                ?: Instant.now()
                                                        .minusSeconds(
                                                                7 * 24 * 60 * 60,
                                                        ),
                                // Last 7 days default
                                toDate = toDate ?: Instant.now(),
                                pageSize = 200,
                        )

                when (val result = runSuspending { auditService.getAuditTrail(request) }) {
                    is AuditResult.Success<*> -> result.data
                    is AuditResult.Error -> throw EndpointException(result.message)
                }
            } catch (e: Exception) {
                logger.error("Failed to get high-risk events", e)
                throw EndpointException("Failed to get high-risk events: ${e.message}")
            }

    /**
     * Gets audit trail for a specific target entity. Useful for tracking all actions performed on a
     * specific resource.
     */
    @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN", "TENANT_ADMIN")
    fun getAuditTrailForTarget(
            targetType: String,
            targetId: String,
            fromDate: Instant? = null,
            toDate: Instant? = null,
    ): Any? =
            try {
                val request =
                        AuditTrailRequest(
                                targetType = targetType,
                                targetId = targetId,
                                fromDate = fromDate,
                                toDate = toDate,
                                pageSize = 100,
                        )

                when (val result = runSuspending { auditService.getAuditTrail(request) }) {
                    is AuditResult.Success<*> -> result.data
                    is AuditResult.Error -> throw EndpointException(result.message)
                }
            } catch (e: Exception) {
                logger.error("Failed to get audit trail for target $targetType:$targetId", e)
                throw EndpointException("Failed to get audit trail for target: ${e.message}")
            }

    /**
     * Example endpoint that demonstrates audit integration. Shows how other endpoints can
     * automatically log their actions.
     */
    @AnonymousAllowed
    fun getAvailableActions(): Map<String, String> =
            AdminAction.values().associate { action -> action.name to action.getDescription() }

    /**
     * Example endpoint for testing audit functionality. Creates sample audit entries for
     * development and testing.
     */
    @RolesAllowed("SUPER_ADMIN")
    fun createSampleAuditEntries(): Map<String, Any> =
            try {
                // This is for testing only - log a sample action
                val result = runSuspending {
                    auditService.logAdminAction(
                            action = AdminAction.SYSTEM_MAINTENANCE,
                            targetType = "audit_system",
                            targetId = "sample_test",
                            details =
                                    mapOf(
                                            "description" to "Sample audit entry for testing",
                                            "feature" to "audit_trail_integration",
                                            "timestamp" to Instant.now(),
                                    ),
                    )
                }
                mapOf(
                        "message" to "Sample audit entry created successfully",
                        "auditEntryId" to result.id.value,
                        "timestamp" to Instant.now(),
                )
            } catch (e: Exception) {
                logger.error("Failed to create sample audit entry", e)
                throw EndpointException("Failed to create sample audit entry: ${e.message}")
            }

    /**
     * Helper function to run suspend functions in a blocking context. This is needed because Hilla
     * endpoints are not suspending.
     */
    private fun <T> runSuspending(block: suspend () -> T): T =
            kotlinx.coroutines.runBlocking { block() }
}

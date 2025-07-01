package com.axians.eaf.controlplane.infrastructure.adapter.input

import com.axians.eaf.controlplane.application.dto.audit.AuditStatisticsRequest
import com.axians.eaf.controlplane.application.dto.audit.AuditTrailRequest
import com.axians.eaf.controlplane.application.dto.audit.LogAdminActionRequest
import com.axians.eaf.controlplane.domain.model.audit.AdminAction
import com.axians.eaf.controlplane.domain.service.AuditResult
import com.axians.eaf.controlplane.domain.service.AuditService
import com.axians.eaf.controlplane.infrastructure.adapter.input.common.EndpointExceptionHandler
import com.axians.eaf.controlplane.infrastructure.adapter.input.common.ResponseConstants
import com.axians.eaf.core.annotations.HillaWorkaround
import com.vaadin.flow.server.auth.AnonymousAllowed
import com.vaadin.hilla.BrowserCallable
import com.vaadin.hilla.exception.EndpointException
import jakarta.annotation.security.RolesAllowed
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import java.time.Instant

/**
 * Hilla endpoint for audit trail management and monitoring. Provides comprehensive audit querying,
 * statistics, and administrative logging capabilities.
 */
@BrowserCallable
@Validated
@HillaWorkaround(
    description =
        "This endpoint and its DTOs were analyzed for Hilla issue #3443 and found to be unaffected. No aliased collections are used.",
)
class AuditTrailEndpoint(
    private val auditService: AuditService,
) {
    private val logger = LoggerFactory.getLogger(AuditTrailEndpoint::class.java)

    /** Processes audit result and returns data or throws appropriate exception. */
    private fun processAuditResult(result: AuditResult): Any =
        when (result) {
            is AuditResult.Success<*> ->
                result.data
                    ?: throw EndpointException(
                        "Null result from successful audit operation",
                    )
            is AuditResult.Error -> throw EndpointException(result.message)
        }

    /**
     * Retrieves audit trail entries with filtering and pagination. Available to administrators for
     * comprehensive audit analysis.
     */
    @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN", "TENANT_ADMIN")
    fun getAuditTrail(
        @Valid request: AuditTrailRequest,
    ): Any =
        EndpointExceptionHandler.handleEndpointExecution("getAuditTrail", logger) {
            val result = runSuspending { auditService.getAuditTrail(request) }
            processAuditResult(result)
        }

    /**
     * Generates audit statistics for reporting and monitoring. Provides comprehensive metrics for
     * security and compliance analysis.
     */
    @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN", "TENANT_ADMIN")
    fun generateAuditStatistics(
        @Valid request: AuditStatisticsRequest,
    ): Any =
        EndpointExceptionHandler.handleEndpointExecution("generateAuditStatistics", logger) {
            val result = runSuspending { auditService.generateStatistics(request) }
            processAuditResult(result)
        }

    /**
     * Logs a custom administrative action for audit tracking. Used by administrators to create
     * audit entries for manual operations.
     */
    @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN", "TENANT_ADMIN")
    fun logAdminAction(
        @Valid request: LogAdminActionRequest,
    ): Any =
        EndpointExceptionHandler.handleEndpointExecution("logAdminAction", logger) {
            val action = AdminAction.valueOf(request.action)
            val result =
                runSuspending {
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
        }

    /**
     * Gets recent audit entries for real-time monitoring. Limited to recent entries for performance
     * and quick overview.
     */
    @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN", "TENANT_ADMIN")
    fun getRecentAuditEntries(limit: Int = ResponseConstants.DEFAULT_HEALTH_CHECK_LIMIT): Any =
        EndpointExceptionHandler.handleEndpointExecution("getRecentAuditEntries", logger) {
            val effectiveLimit = minOf(limit, ResponseConstants.MAX_PAGE_SIZE)
            val result = runSuspending { auditService.getRecentEntries(effectiveLimit) }
            processAuditResult(result)
        }

    /**
     * Gets security-related audit events for monitoring. Focuses on login attempts, access denials,
     * and security violations.
     */
    @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN")
    fun getSecurityEvents(
        fromDate: Instant? = null,
        toDate: Instant? = null,
    ): Any =
        EndpointExceptionHandler.handleEndpointExecution("getSecurityEvents", logger) {
            val request =
                AuditTrailRequest(
                    fromDate =
                        fromDate
                            ?: Instant
                                .now()
                                .minusSeconds(
                                    ResponseConstants
                                        .AUDIT_LOOKBACK_SECONDS,
                                ),
                    toDate = toDate ?: Instant.now(),
                    pageSize = ResponseConstants.SECURITY_EVENTS_PAGE_SIZE,
                )

            val result = runSuspending { auditService.getAuditTrail(request) }
            processAuditResult(result)
        }

    /**
     * Gets high-risk audit events for compliance monitoring. Focuses on administrative actions that
     * require special attention.
     */
    @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN")
    fun getHighRiskEvents(
        fromDate: Instant? = null,
        toDate: Instant? = null,
    ): Any =
        EndpointExceptionHandler.handleEndpointExecution("getHighRiskEvents", logger) {
            val request =
                AuditTrailRequest(
                    fromDate =
                        fromDate
                            ?: Instant
                                .now()
                                .minusSeconds(
                                    ResponseConstants
                                        .EXTENDED_AUDIT_LOOKBACK_SECONDS,
                                ),
                    toDate = toDate ?: Instant.now(),
                    pageSize = ResponseConstants.HIGH_RISK_EVENTS_PAGE_SIZE,
                )

            val result = runSuspending { auditService.getAuditTrail(request) }
            processAuditResult(result)
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
    ): Any =
        EndpointExceptionHandler.handleEndpointExecution("getAuditTrailForTarget", logger) {
            val request =
                AuditTrailRequest(
                    targetType = targetType,
                    targetId = targetId,
                    fromDate = fromDate,
                    toDate = toDate,
                    pageSize = ResponseConstants.TARGET_AUDIT_PAGE_SIZE,
                )

            val result = runSuspending { auditService.getAuditTrail(request) }
            processAuditResult(result)
        }

    /**
     * Example endpoint that demonstrates audit integration. Shows how other endpoints can
     * automatically log their actions.
     */
    @AnonymousAllowed
    fun getAvailableActions(): Map<String, String> =
        EndpointExceptionHandler.handleEndpointExecution("getAvailableActions", logger) {
            AdminAction.values().associate { action -> action.name to action.getDescription() }
        }

    /**
     * Example endpoint for testing audit functionality. Creates sample audit entries for
     * development and testing.
     */
    @RolesAllowed("SUPER_ADMIN")
    fun createSampleAuditEntries(): Map<String, Any> =
        EndpointExceptionHandler.handleEndpointExecution("createSampleAuditEntries", logger) {
            val result =
                runSuspending {
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
        }

    /**
     * Helper function to run suspend functions in a blocking context. This is needed because Hilla
     * endpoints are not suspending.
     */
    private fun <T> runSuspending(block: suspend () -> T): T = kotlinx.coroutines.runBlocking { block() }
}

package com.axians.eaf.controlplane.application.dto.audit

import com.axians.eaf.controlplane.domain.model.audit.AuditEntry
import com.axians.eaf.controlplane.domain.model.audit.AuditSeverity
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.time.Instant

/** Request to query audit trail with filtering and pagination. */
data class AuditTrailRequest(
    val targetType: String? = null,
    val targetId: String? = null,
    val performedBy: String? = null,
    val action: String? = null,
    val severity: AuditSeverity? = null,
    val success: Boolean? = null,
    val tenantId: String? = null,
    @field:PastOrPresent(message = "From date cannot be in the future")
    val fromDate: Instant? = null,
    val toDate: Instant? = null,
    val ipAddress: String? = null,
    val sessionId: String? = null,
    val correlationId: String? = null,
    @field:Min(value = 1, message = "Page size must be at least 1") val pageSize: Int = 50,
    @field:Min(value = 0, message = "Page number must be non-negative") val pageNumber: Int = 0,
    val sortBy: String = "timestamp",
    val sortDirection: String = "DESC",
) {
    init {
        require(pageSize <= 1000) { "Page size cannot exceed 1000" }
        require(sortBy in ALLOWED_SORT_FIELDS) { "Invalid sort field: $sortBy" }
        require(sortDirection in setOf("ASC", "DESC")) { "Sort direction must be ASC or DESC" }
        require(fromDate == null || toDate == null || !fromDate.isAfter(toDate)) {
            "From date cannot be after to date"
        }
    }

    companion object {
        val ALLOWED_SORT_FIELDS =
            setOf("timestamp", "action", "targetType", "severity", "performedBy", "tenantId")
    }
}

/** Response containing audit trail entries with pagination. */
data class AuditTrailResponse(
    val entries: List<AuditEntryDto>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
)

/** DTO representation of an audit entry. */
data class AuditEntryDto(
    val id: String,
    val tenantId: String,
    val performedBy: String,
    val action: String,
    val actionDescription: String,
    val targetType: String,
    val targetId: String,
    val details: Map<String, Any>,
    val timestamp: Instant,
    val ipAddress: String,
    val userAgent: String,
    val severity: AuditSeverity,
    val success: Boolean,
    val errorMessage: String?,
    val sessionId: String?,
    val correlationId: String?,
    val isSecurityEvent: Boolean,
    val isHighRiskAction: Boolean,
)

/** Request to log a custom administrative action. */
data class LogAdminActionRequest(
    @field:NotBlank(message = "Action is required") val action: String,
    @field:NotBlank(message = "Target type is required") val targetType: String,
    @field:NotBlank(message = "Target ID is required") val targetId: String,
    val details: Map<String, Any> = emptyMap(),
    val success: Boolean = true,
    @field:Size(max = 1000, message = "Error message cannot exceed 1000 characters")
    val errorMessage: String? = null,
)

/** Request to log a custom user action. */
data class LogUserActionRequest(
    @field:NotBlank(message = "User ID is required") val userId: String,
    @field:NotBlank(message = "Action is required") val action: String,
    @field:NotBlank(message = "Target type is required") val targetType: String,
    @field:NotBlank(message = "Target ID is required") val targetId: String,
    val details: Map<String, Any> = emptyMap(),
    val success: Boolean = true,
    @field:Size(max = 1000, message = "Error message cannot exceed 1000 characters")
    val errorMessage: String? = null,
)

/** Response for audit entry creation. */
data class AuditEntryResponse(
    val auditEntryId: String,
    val message: String,
    val timestamp: Instant,
)

/** Summary statistics for audit entries. */
data class AuditStatistics(
    val totalEntries: Long,
    val successfulActions: Long,
    val failedActions: Long,
    val securityEvents: Long,
    val highRiskActions: Long,
    val byAction: Map<String, Long>,
    val bySeverity: Map<AuditSeverity, Long>,
    val byDay: Map<String, Long>,
)

/** Request for audit statistics with time range. */
data class AuditStatisticsRequest(
    @field:NotNull(message = "From date is required")
    @field:PastOrPresent(message = "From date cannot be in the future")
    val fromDate: Instant,
    @field:NotNull(message = "To date is required") val toDate: Instant,
    val tenantId: String? = null,
    val performedBy: String? = null,
) {
    init {
        require(!fromDate.isAfter(toDate)) { "From date cannot be after to date" }
        require(fromDate.isAfter(toDate.minusSeconds(365 * 24 * 60 * 60))) {
            "Date range cannot exceed 1 year"
        }
    }
}

/** Filter for real-time audit monitoring. */
data class AuditMonitoringFilter(
    val severities: Set<AuditSeverity> = emptySet(),
    val actions: Set<String> = emptySet(),
    val tenantIds: Set<String> = emptySet(),
    val includeSecurityEvents: Boolean = true,
    val includeHighRiskActions: Boolean = true,
    val includeFailures: Boolean = true,
)

/** Real-time audit event notification. */
data class AuditEventNotification(
    val auditEntryId: String,
    val tenantId: String,
    val action: String,
    val actionDescription: String,
    val targetType: String,
    val targetId: String,
    val performedBy: String,
    val severity: AuditSeverity,
    val timestamp: Instant,
    val isSecurityEvent: Boolean,
    val isHighRiskAction: Boolean,
    val success: Boolean,
    val errorMessage: String?,
)

/** Extension functions to convert between domain and DTO objects. */
fun AuditEntry.toDto(): AuditEntryDto =
    AuditEntryDto(
        id = id.value,
        tenantId = tenantId.value,
        performedBy = performedBy.value,
        action = action,
        actionDescription = getDescription(),
        targetType = targetType,
        targetId = targetId,
        details = details,
        timestamp = timestamp,
        ipAddress = ipAddress,
        userAgent = userAgent,
        severity = severity,
        success = success,
        errorMessage = errorMessage,
        sessionId = sessionId,
        correlationId = correlationId,
        isSecurityEvent = isSecurityEvent(),
        isHighRiskAction = isHighRiskAction(),
    )

fun AuditEntry.toEventNotification(): AuditEventNotification =
    AuditEventNotification(
        auditEntryId = id.value,
        tenantId = tenantId.value,
        action = action,
        actionDescription = getDescription(),
        targetType = targetType,
        targetId = targetId,
        performedBy = performedBy.value,
        severity = severity,
        timestamp = timestamp,
        isSecurityEvent = isSecurityEvent(),
        isHighRiskAction = isHighRiskAction(),
        success = success,
        errorMessage = errorMessage,
    )

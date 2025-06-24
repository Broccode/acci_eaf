package com.axians.eaf.controlplane.domain.port

import com.axians.eaf.controlplane.application.dto.audit.AuditStatistics
import com.axians.eaf.controlplane.application.dto.audit.AuditStatisticsRequest
import com.axians.eaf.controlplane.application.dto.audit.AuditTrailRequest
import com.axians.eaf.controlplane.application.dto.audit.AuditTrailResponse
import com.axians.eaf.controlplane.domain.model.audit.AuditEntry
import com.axians.eaf.controlplane.domain.model.audit.AuditEntryId
import com.axians.eaf.controlplane.domain.model.audit.AuditSeverity
import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.UserId
import java.time.Instant

/**
 * Repository port for audit entry persistence and querying. This is a domain contract that will be
 * implemented by infrastructure adapters.
 */
interface AuditRepository {
    /** Saves an audit entry to the repository. */
    suspend fun save(auditEntry: AuditEntry): AuditEntry

    /** Saves multiple audit entries in a batch operation. */
    suspend fun saveAll(auditEntries: List<AuditEntry>): List<AuditEntry>

    /** Finds an audit entry by its unique identifier. */
    suspend fun findById(auditEntryId: AuditEntryId): AuditEntry?

    /** Finds audit entries with filtering and pagination. */
    suspend fun findWithFilter(request: AuditTrailRequest): AuditTrailResponse

    /** Finds all audit entries for a specific tenant. */
    suspend fun findByTenant(
        tenantId: TenantId,
        fromDate: Instant? = null,
        toDate: Instant? = null,
    ): List<AuditEntry>

    /** Finds audit entries by performer within a time range. */
    suspend fun findByPerformer(
        performedBy: UserId,
        fromDate: Instant? = null,
        toDate: Instant? = null,
    ): List<AuditEntry>

    /** Finds audit entries by action type within a time range. */
    suspend fun findByAction(
        action: String,
        fromDate: Instant? = null,
        toDate: Instant? = null,
    ): List<AuditEntry>

    /** Finds audit entries by target within a time range. */
    suspend fun findByTarget(
        targetType: String,
        targetId: String,
        fromDate: Instant? = null,
        toDate: Instant? = null,
    ): List<AuditEntry>

    /** Finds audit entries by severity level. */
    suspend fun findBySeverity(
        severity: AuditSeverity,
        fromDate: Instant? = null,
        toDate: Instant? = null,
    ): List<AuditEntry>

    /** Finds failed audit entries (success = false). */
    suspend fun findFailedEntries(
        fromDate: Instant? = null,
        toDate: Instant? = null,
    ): List<AuditEntry>

    /** Finds security-related audit entries. */
    suspend fun findSecurityEvents(
        fromDate: Instant? = null,
        toDate: Instant? = null,
    ): List<AuditEntry>

    /** Finds high-risk audit entries. */
    suspend fun findHighRiskActions(
        fromDate: Instant? = null,
        toDate: Instant? = null,
    ): List<AuditEntry>

    /** Finds audit entries by session ID. */
    suspend fun findBySessionId(sessionId: String): List<AuditEntry>

    /** Finds audit entries by correlation ID. */
    suspend fun findByCorrelationId(correlationId: String): List<AuditEntry>

    /** Finds audit entries by IP address within a time range. */
    suspend fun findByIpAddress(
        ipAddress: String,
        fromDate: Instant? = null,
        toDate: Instant? = null,
    ): List<AuditEntry>

    /** Gets recent audit entries (last N entries). */
    suspend fun findRecent(limit: Int = 100): List<AuditEntry>

    /** Gets recent audit entries for a specific tenant. */
    suspend fun findRecentByTenant(
        tenantId: TenantId,
        limit: Int = 100,
    ): List<AuditEntry>

    /** Counts audit entries by various criteria. */
    suspend fun countByTenant(tenantId: TenantId): Long

    suspend fun countByAction(action: String): Long

    suspend fun countBySeverity(severity: AuditSeverity): Long

    suspend fun countByTimeRange(
        fromDate: Instant,
        toDate: Instant,
    ): Long

    suspend fun countFailedEntries(
        fromDate: Instant? = null,
        toDate: Instant? = null,
    ): Long

    suspend fun countSecurityEvents(
        fromDate: Instant? = null,
        toDate: Instant? = null,
    ): Long

    suspend fun countHighRiskActions(
        fromDate: Instant? = null,
        toDate: Instant? = null,
    ): Long

    /** Generates audit statistics for a given time range. */
    suspend fun generateStatistics(request: AuditStatisticsRequest): AuditStatistics

    /** Checks if an audit entry exists with the given ID. */
    suspend fun existsById(auditEntryId: AuditEntryId): Boolean

    /** Deletes audit entries older than the specified date (for retention management). */
    suspend fun deleteEntriesOlderThan(cutoffDate: Instant): Long

    /** Deletes all audit entries for a specific tenant (used when tenant is archived). */
    suspend fun deleteByTenant(tenantId: TenantId): Long

    /** Archives old audit entries to a different storage (optional optimization). */
    suspend fun archiveEntriesOlderThan(cutoffDate: Instant): Long
}

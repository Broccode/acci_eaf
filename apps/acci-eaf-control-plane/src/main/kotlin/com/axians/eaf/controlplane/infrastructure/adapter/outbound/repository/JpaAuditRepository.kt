package com.axians.eaf.controlplane.infrastructure.adapter.outbound.repository

import com.axians.eaf.controlplane.domain.model.audit.AuditSeverity
import com.axians.eaf.controlplane.infrastructure.adapter.outbound.entity.AuditEntryEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

/** JPA repository interface for audit entries with custom query methods. */
@Repository
interface JpaAuditRepository : JpaRepository<AuditEntryEntity, String> {
    // Basic filtering queries
    fun findByTenantIdOrderByTimestampDesc(tenantId: String): List<AuditEntryEntity>

    fun findByTenantIdAndTimestampBetweenOrderByTimestampDesc(
        tenantId: String,
        fromDate: Instant,
        toDate: Instant,
    ): List<AuditEntryEntity>

    fun findByPerformedByOrderByTimestampDesc(performedBy: String): List<AuditEntryEntity>

    fun findByPerformedByAndTimestampBetweenOrderByTimestampDesc(
        performedBy: String,
        fromDate: Instant,
        toDate: Instant,
    ): List<AuditEntryEntity>

    fun findByActionOrderByTimestampDesc(action: String): List<AuditEntryEntity>

    fun findByActionAndTimestampBetweenOrderByTimestampDesc(
        action: String,
        fromDate: Instant,
        toDate: Instant,
    ): List<AuditEntryEntity>

    fun findByTargetTypeAndTargetIdOrderByTimestampDesc(
        targetType: String,
        targetId: String,
    ): List<AuditEntryEntity>

    fun findByTargetTypeAndTargetIdAndTimestampBetweenOrderByTimestampDesc(
        targetType: String,
        targetId: String,
        fromDate: Instant,
        toDate: Instant,
    ): List<AuditEntryEntity>

    fun findBySeverityOrderByTimestampDesc(severity: AuditSeverity): List<AuditEntryEntity>

    fun findBySeverityAndTimestampBetweenOrderByTimestampDesc(
        severity: AuditSeverity,
        fromDate: Instant,
        toDate: Instant,
    ): List<AuditEntryEntity>

    fun findBySuccessOrderByTimestampDesc(success: Boolean): List<AuditEntryEntity>

    fun findBySuccessAndTimestampBetweenOrderByTimestampDesc(
        success: Boolean,
        fromDate: Instant,
        toDate: Instant,
    ): List<AuditEntryEntity>

    fun findBySessionIdOrderByTimestampDesc(sessionId: String): List<AuditEntryEntity>

    fun findByCorrelationIdOrderByTimestampDesc(correlationId: String): List<AuditEntryEntity>

    fun findByIpAddressOrderByTimestampDesc(ipAddress: String): List<AuditEntryEntity>

    fun findByIpAddressAndTimestampBetweenOrderByTimestampDesc(
        ipAddress: String,
        fromDate: Instant,
        toDate: Instant,
    ): List<AuditEntryEntity>

    // Security and monitoring queries
    @Query(
        """
        SELECT a FROM AuditEntryEntity a
        WHERE a.action IN ('LOGIN_SUCCESSFUL', 'LOGIN_FAILED', 'LOGOUT', 'ACCESS_DENIED', 'PERMISSION_ESCALATION_ATTEMPTED')
        ORDER BY a.timestamp DESC
    """,
    )
    fun findSecurityEvents(): List<AuditEntryEntity>

    @Query(
        """
        SELECT a FROM AuditEntryEntity a
        WHERE a.action IN ('LOGIN_SUCCESSFUL', 'LOGIN_FAILED', 'LOGOUT', 'ACCESS_DENIED', 'PERMISSION_ESCALATION_ATTEMPTED')
        AND a.timestamp BETWEEN :fromDate AND :toDate
        ORDER BY a.timestamp DESC
    """,
    )
    fun findSecurityEventsBetween(
        @Param("fromDate") fromDate: Instant,
        @Param("toDate") toDate: Instant,
    ): List<AuditEntryEntity>

    @Query(
        """
        SELECT a FROM AuditEntryEntity a
        WHERE a.action IN ('TENANT_ARCHIVED', 'USER_BULK_OPERATION', 'ROLE_DELETED', 'PERMISSION_DELETED', 'CONFIGURATION_CHANGED', 'BULK_DATA_IMPORT', 'BULK_DATA_EXPORT')
        ORDER BY a.timestamp DESC
    """,
    )
    fun findHighRiskActions(): List<AuditEntryEntity>

    @Query(
        """
        SELECT a FROM AuditEntryEntity a
        WHERE a.action IN ('TENANT_ARCHIVED', 'USER_BULK_OPERATION', 'ROLE_DELETED', 'PERMISSION_DELETED', 'CONFIGURATION_CHANGED', 'BULK_DATA_IMPORT', 'BULK_DATA_EXPORT')
        AND a.timestamp BETWEEN :fromDate AND :toDate
        ORDER BY a.timestamp DESC
    """,
    )
    fun findHighRiskActionsBetween(
        @Param("fromDate") fromDate: Instant,
        @Param("toDate") toDate: Instant,
    ): List<AuditEntryEntity>

    // Counting queries
    fun countByTenantId(tenantId: String): Long

    fun countByAction(action: String): Long

    fun countBySeverity(severity: AuditSeverity): Long

    fun countByTimestampBetween(
        fromDate: Instant,
        toDate: Instant,
    ): Long

    fun countBySuccessAndTimestampBetween(
        success: Boolean,
        fromDate: Instant,
        toDate: Instant,
    ): Long

    @Query(
        "SELECT COUNT(a) FROM AuditEntryEntity a WHERE a.success = false AND (:fromDate IS NULL OR a.timestamp >= :fromDate) AND (:toDate IS NULL OR a.timestamp <= :toDate)",
    )
    fun countFailedEntries(
        @Param("fromDate") fromDate: Instant?,
        @Param("toDate") toDate: Instant?,
    ): Long

    @Query(
        """
        SELECT COUNT(a) FROM AuditEntryEntity a
        WHERE a.action IN ('LOGIN_SUCCESSFUL', 'LOGIN_FAILED', 'LOGOUT', 'ACCESS_DENIED', 'PERMISSION_ESCALATION_ATTEMPTED')
        AND (:fromDate IS NULL OR a.timestamp >= :fromDate)
        AND (:toDate IS NULL OR a.timestamp <= :toDate)
    """,
    )
    fun countSecurityEvents(
        @Param("fromDate") fromDate: Instant?,
        @Param("toDate") toDate: Instant?,
    ): Long

    @Query(
        """
        SELECT COUNT(a) FROM AuditEntryEntity a
        WHERE a.action IN ('TENANT_ARCHIVED', 'USER_BULK_OPERATION', 'ROLE_DELETED', 'PERMISSION_DELETED', 'CONFIGURATION_CHANGED', 'BULK_DATA_IMPORT', 'BULK_DATA_EXPORT')
        AND (:fromDate IS NULL OR a.timestamp >= :fromDate)
        AND (:toDate IS NULL OR a.timestamp <= :toDate)
    """,
    )
    fun countHighRiskActions(
        @Param("fromDate") fromDate: Instant?,
        @Param("toDate") toDate: Instant?,
    ): Long

    // Recent entries with limit
    @Query("SELECT a FROM AuditEntryEntity a ORDER BY a.timestamp DESC")
    fun findTopOrderByTimestampDesc(pageable: Pageable): List<AuditEntryEntity>

    @Query(
        "SELECT a FROM AuditEntryEntity a WHERE a.tenantId = :tenantId ORDER BY a.timestamp DESC",
    )
    fun findTopByTenantIdOrderByTimestampDesc(
        @Param("tenantId") tenantId: String,
        pageable: Pageable,
    ): List<AuditEntryEntity>

    // Statistics queries
    @Query(
        """
        SELECT a.action, COUNT(a) FROM AuditEntryEntity a
        WHERE a.timestamp BETWEEN :fromDate AND :toDate
        AND (:tenantId IS NULL OR a.tenantId = :tenantId)
        AND (:performedBy IS NULL OR a.performedBy = :performedBy)
        GROUP BY a.action
    """,
    )
    fun countByActionBetween(
        @Param("fromDate") fromDate: Instant,
        @Param("toDate") toDate: Instant,
        @Param("tenantId") tenantId: String?,
        @Param("performedBy") performedBy: String?,
    ): List<Array<Any>>

    @Query(
        """
        SELECT a.severity, COUNT(a) FROM AuditEntryEntity a
        WHERE a.timestamp BETWEEN :fromDate AND :toDate
        AND (:tenantId IS NULL OR a.tenantId = :tenantId)
        AND (:performedBy IS NULL OR a.performedBy = :performedBy)
        GROUP BY a.severity
    """,
    )
    fun countBySeverityBetween(
        @Param("fromDate") fromDate: Instant,
        @Param("toDate") toDate: Instant,
        @Param("tenantId") tenantId: String?,
        @Param("performedBy") performedBy: String?,
    ): List<Array<Any>>

    @Query(
        """
        SELECT DATE(a.timestamp) as day, COUNT(a) FROM AuditEntryEntity a
        WHERE a.timestamp BETWEEN :fromDate AND :toDate
        AND (:tenantId IS NULL OR a.tenantId = :tenantId)
        AND (:performedBy IS NULL OR a.performedBy = :performedBy)
        GROUP BY DATE(a.timestamp)
        ORDER BY day
    """,
    )
    fun countByDayBetween(
        @Param("fromDate") fromDate: Instant,
        @Param("toDate") toDate: Instant,
        @Param("tenantId") tenantId: String?,
        @Param("performedBy") performedBy: String?,
    ): List<Array<Any>>

    // Complex filtering with pagination
    @Query(
        """
        SELECT a FROM AuditEntryEntity a
        WHERE (:targetType IS NULL OR a.targetType = :targetType)
        AND (:targetId IS NULL OR a.targetId = :targetId)
        AND (:performedBy IS NULL OR a.performedBy = :performedBy)
        AND (:action IS NULL OR a.action = :action)
        AND (:severity IS NULL OR a.severity = :severity)
        AND (:success IS NULL OR a.success = :success)
        AND (:tenantId IS NULL OR a.tenantId = :tenantId)
        AND (:fromDate IS NULL OR a.timestamp >= :fromDate)
        AND (:toDate IS NULL OR a.timestamp <= :toDate)
        AND (:ipAddress IS NULL OR a.ipAddress = :ipAddress)
        AND (:sessionId IS NULL OR a.sessionId = :sessionId)
        AND (:correlationId IS NULL OR a.correlationId = :correlationId)
        ORDER BY a.timestamp DESC
    """,
    )
    fun findWithFilters(
        @Param("targetType") targetType: String?,
        @Param("targetId") targetId: String?,
        @Param("performedBy") performedBy: String?,
        @Param("action") action: String?,
        @Param("severity") severity: AuditSeverity?,
        @Param("success") success: Boolean?,
        @Param("tenantId") tenantId: String?,
        @Param("fromDate") fromDate: Instant?,
        @Param("toDate") toDate: Instant?,
        @Param("ipAddress") ipAddress: String?,
        @Param("sessionId") sessionId: String?,
        @Param("correlationId") correlationId: String?,
        pageable: Pageable,
    ): Page<AuditEntryEntity>

    // Maintenance queries
    @Modifying
    @Query("DELETE FROM AuditEntryEntity a WHERE a.timestamp < :cutoffDate")
    fun deleteByTimestampBefore(
        @Param("cutoffDate") cutoffDate: Instant,
    ): Long

    @Modifying
    @Query("DELETE FROM AuditEntryEntity a WHERE a.tenantId = :tenantId")
    fun deleteByTenantId(
        @Param("tenantId") tenantId: String,
    ): Long
}

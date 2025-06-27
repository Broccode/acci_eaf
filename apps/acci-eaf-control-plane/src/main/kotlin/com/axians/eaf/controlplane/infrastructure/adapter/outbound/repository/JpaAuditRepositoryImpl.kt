package com.axians.eaf.controlplane.infrastructure.adapter.outbound.repository

import com.axians.eaf.controlplane.application.dto.audit.AuditStatistics
import com.axians.eaf.controlplane.application.dto.audit.AuditStatisticsRequest
import com.axians.eaf.controlplane.application.dto.audit.AuditTrailRequest
import com.axians.eaf.controlplane.application.dto.audit.AuditTrailResponse
import com.axians.eaf.controlplane.application.dto.audit.toDto
import com.axians.eaf.controlplane.domain.model.audit.AuditEntry
import com.axians.eaf.controlplane.domain.model.audit.AuditEntryId
import com.axians.eaf.controlplane.domain.model.audit.AuditSeverity
import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.UserId
import com.axians.eaf.controlplane.domain.port.AuditRepository
import com.axians.eaf.controlplane.infrastructure.adapter.outbound.entity.AuditEntryEntity
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * JPA-based implementation of the AuditRepository domain port. Provides comprehensive audit trail
 * persistence and querying capabilities.
 *
 * This is an adapter that bridges the domain port with Spring Data JPA infrastructure.
 */
@Component
class JpaAuditRepositoryImpl(
    private val jpaRepository: JpaAuditRepository,
) : AuditRepository {
    override suspend fun save(auditEntry: AuditEntry): AuditEntry {
        val entity = AuditEntryEntity.fromDomain(auditEntry)
        val savedEntity = jpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override suspend fun saveAll(auditEntries: List<AuditEntry>): List<AuditEntry> {
        val entities = auditEntries.map { AuditEntryEntity.fromDomain(it) }
        val savedEntities = jpaRepository.saveAll(entities)
        return savedEntities.map { it.toDomain() }
    }

    override suspend fun findById(auditEntryId: AuditEntryId): AuditEntry? =
        jpaRepository.findById(auditEntryId.value).orElse(null)?.toDomain()

    override suspend fun findWithFilter(request: AuditTrailRequest): AuditTrailResponse {
        val sort = createSort(request.sortBy, request.sortDirection)
        val pageable = PageRequest.of(request.pageNumber, request.pageSize, sort)

        val page =
            jpaRepository.findWithFilters(
                targetType = request.targetType,
                targetId = request.targetId,
                performedBy = request.performedBy,
                action = request.action,
                severity = request.severity,
                success = request.success,
                tenantId = request.tenantId,
                fromDate = request.fromDate,
                toDate = request.toDate,
                ipAddress = request.ipAddress,
                sessionId = request.sessionId,
                correlationId = request.correlationId,
                pageable = pageable,
            )

        return AuditTrailResponse(
            entries = page.content.map { it.toDomain().toDto() },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            currentPage = page.number,
            pageSize = page.size,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious(),
        )
    }

    override suspend fun findByTenant(
        tenantId: TenantId,
        fromDate: Instant?,
        toDate: Instant?,
    ): List<AuditEntry> {
        val entities =
            if (fromDate != null && toDate != null) {
                jpaRepository.findByTenantIdAndTimestampBetweenOrderByTimestampDesc(
                    tenantId.value,
                    fromDate,
                    toDate,
                )
            } else {
                jpaRepository.findByTenantIdOrderByTimestampDesc(tenantId.value)
            }
        return entities.map { it.toDomain() }
    }

    override suspend fun findByPerformer(
        performedBy: UserId,
        fromDate: Instant?,
        toDate: Instant?,
    ): List<AuditEntry> {
        val entities =
            if (fromDate != null && toDate != null) {
                jpaRepository.findByPerformedByAndTimestampBetweenOrderByTimestampDesc(
                    performedBy.value,
                    fromDate,
                    toDate,
                )
            } else {
                jpaRepository.findByPerformedByOrderByTimestampDesc(performedBy.value)
            }
        return entities.map { it.toDomain() }
    }

    override suspend fun findByAction(
        action: String,
        fromDate: Instant?,
        toDate: Instant?,
    ): List<AuditEntry> {
        val entities =
            if (fromDate != null && toDate != null) {
                jpaRepository.findByActionAndTimestampBetweenOrderByTimestampDesc(
                    action,
                    fromDate,
                    toDate,
                )
            } else {
                jpaRepository.findByActionOrderByTimestampDesc(action)
            }
        return entities.map { it.toDomain() }
    }

    override suspend fun findByTarget(
        targetType: String,
        targetId: String,
        fromDate: Instant?,
        toDate: Instant?,
    ): List<AuditEntry> {
        val entities =
            if (fromDate != null && toDate != null) {
                jpaRepository
                    .findByTargetTypeAndTargetIdAndTimestampBetweenOrderByTimestampDesc(
                        targetType,
                        targetId,
                        fromDate,
                        toDate,
                    )
            } else {
                jpaRepository.findByTargetTypeAndTargetIdOrderByTimestampDesc(
                    targetType,
                    targetId,
                )
            }
        return entities.map { it.toDomain() }
    }

    override suspend fun findBySeverity(
        severity: AuditSeverity,
        fromDate: Instant?,
        toDate: Instant?,
    ): List<AuditEntry> {
        val entities =
            if (fromDate != null && toDate != null) {
                jpaRepository.findBySeverityAndTimestampBetweenOrderByTimestampDesc(
                    severity,
                    fromDate,
                    toDate,
                )
            } else {
                jpaRepository.findBySeverityOrderByTimestampDesc(severity)
            }
        return entities.map { it.toDomain() }
    }

    override suspend fun findFailedEntries(
        fromDate: Instant?,
        toDate: Instant?,
    ): List<AuditEntry> {
        val entities =
            if (fromDate != null && toDate != null) {
                jpaRepository.findBySuccessAndTimestampBetweenOrderByTimestampDesc(
                    false,
                    fromDate,
                    toDate,
                )
            } else {
                jpaRepository.findBySuccessOrderByTimestampDesc(false)
            }
        return entities.map { it.toDomain() }
    }

    override suspend fun findSecurityEvents(
        fromDate: Instant?,
        toDate: Instant?,
    ): List<AuditEntry> {
        val entities =
            if (fromDate != null && toDate != null) {
                jpaRepository.findSecurityEventsBetween(fromDate, toDate)
            } else {
                jpaRepository.findSecurityEvents()
            }
        return entities.map { it.toDomain() }
    }

    override suspend fun findHighRiskActions(
        fromDate: Instant?,
        toDate: Instant?,
    ): List<AuditEntry> {
        val entities =
            if (fromDate != null && toDate != null) {
                jpaRepository.findHighRiskActionsBetween(fromDate, toDate)
            } else {
                jpaRepository.findHighRiskActions()
            }
        return entities.map { it.toDomain() }
    }

    override suspend fun findBySessionId(sessionId: String): List<AuditEntry> =
        jpaRepository.findBySessionIdOrderByTimestampDesc(sessionId).map { it.toDomain() }

    override suspend fun findByCorrelationId(correlationId: String): List<AuditEntry> =
        jpaRepository.findByCorrelationIdOrderByTimestampDesc(correlationId).map {
            it.toDomain()
        }

    override suspend fun findByIpAddress(
        ipAddress: String,
        fromDate: Instant?,
        toDate: Instant?,
    ): List<AuditEntry> {
        val entities =
            if (fromDate != null && toDate != null) {
                jpaRepository.findByIpAddressAndTimestampBetweenOrderByTimestampDesc(
                    ipAddress,
                    fromDate,
                    toDate,
                )
            } else {
                jpaRepository.findByIpAddressOrderByTimestampDesc(ipAddress)
            }
        return entities.map { it.toDomain() }
    }

    override suspend fun findRecent(limit: Int): List<AuditEntry> {
        val pageable = PageRequest.of(0, limit)
        return jpaRepository.findTopOrderByTimestampDesc(pageable).map { it.toDomain() }
    }

    override suspend fun findRecentByTenant(
        tenantId: TenantId,
        limit: Int,
    ): List<AuditEntry> {
        val pageable = PageRequest.of(0, limit)
        return jpaRepository.findTopByTenantIdOrderByTimestampDesc(tenantId.value, pageable).map {
            it.toDomain()
        }
    }

    override suspend fun countByTenant(tenantId: TenantId): Long = jpaRepository.countByTenantId(tenantId.value)

    override suspend fun countByAction(action: String): Long = jpaRepository.countByAction(action)

    override suspend fun countBySeverity(severity: AuditSeverity): Long = jpaRepository.countBySeverity(severity)

    override suspend fun countByTimeRange(
        fromDate: Instant,
        toDate: Instant,
    ): Long = jpaRepository.countByTimestampBetween(fromDate, toDate)

    override suspend fun countFailedEntries(
        fromDate: Instant?,
        toDate: Instant?,
    ): Long = jpaRepository.countFailedEntries(fromDate, toDate)

    override suspend fun countSecurityEvents(
        fromDate: Instant?,
        toDate: Instant?,
    ): Long = jpaRepository.countSecurityEvents(fromDate, toDate)

    override suspend fun countHighRiskActions(
        fromDate: Instant?,
        toDate: Instant?,
    ): Long = jpaRepository.countHighRiskActions(fromDate, toDate)

    override suspend fun generateStatistics(request: AuditStatisticsRequest): AuditStatistics {
        val totalEntries = jpaRepository.countByTimestampBetween(request.fromDate, request.toDate)
        val successfulActions =
            jpaRepository.countBySuccessAndTimestampBetween(
                true,
                request.fromDate,
                request.toDate,
            )
        val failedActions =
            jpaRepository.countBySuccessAndTimestampBetween(
                false,
                request.fromDate,
                request.toDate,
            )
        val securityEvents = countSecurityEvents(request.fromDate, request.toDate)
        val highRiskActions = countHighRiskActions(request.fromDate, request.toDate)

        // Generate action statistics
        val actionStats =
            jpaRepository
                .countByActionBetween(
                    request.fromDate,
                    request.toDate,
                    request.tenantId,
                    request.performedBy,
                ).associate { row -> row[0].toString() to (row[1] as Number).toLong() }

        // Generate severity statistics
        val severityStats =
            jpaRepository
                .countBySeverityBetween(
                    request.fromDate,
                    request.toDate,
                    request.tenantId,
                    request.performedBy,
                ).associate { row ->
                    AuditSeverity.valueOf(row[0].toString()) to (row[1] as Number).toLong()
                }

        // Generate daily statistics
        val dailyStats =
            jpaRepository
                .countByDayBetween(
                    request.fromDate,
                    request.toDate,
                    request.tenantId,
                    request.performedBy,
                ).associate { row ->
                    // Convert date to string format
                    val date = row[0] as java.sql.Date
                    val localDate = date.toLocalDate()
                    localDate.format(DateTimeFormatter.ISO_LOCAL_DATE) to
                        (row[1] as Number).toLong()
                }

        return AuditStatistics(
            totalEntries = totalEntries,
            successfulActions = successfulActions,
            failedActions = failedActions,
            securityEvents = securityEvents,
            highRiskActions = highRiskActions,
            byAction = actionStats,
            bySeverity = severityStats,
            byDay = dailyStats,
        )
    }

    override suspend fun existsById(auditEntryId: AuditEntryId): Boolean = jpaRepository.existsById(auditEntryId.value)

    override suspend fun deleteEntriesOlderThan(cutoffDate: Instant): Long =
        jpaRepository.deleteByTimestampBefore(cutoffDate)

    override suspend fun deleteByTenant(tenantId: TenantId): Long = jpaRepository.deleteByTenantId(tenantId.value)

    override suspend fun archiveEntriesOlderThan(cutoffDate: Instant): Long {
        // For now, archiving is the same as deletion
        // In a production system, this might move entries to a different table or storage
        return deleteEntriesOlderThan(cutoffDate)
    }

    private fun createSort(
        sortBy: String,
        direction: String,
    ): Sort {
        val sortDirection =
            if (direction.uppercase() == "ASC") {
                Sort.Direction.ASC
            } else {
                Sort.Direction.DESC
            }

        return when (sortBy) {
            "timestamp" -> Sort.by(sortDirection, "timestamp")
            "action" -> Sort.by(sortDirection, "action")
            "targetType" -> Sort.by(sortDirection, "targetType")
            "severity" -> Sort.by(sortDirection, "severity")
            "performedBy" -> Sort.by(sortDirection, "performedBy")
            "tenantId" -> Sort.by(sortDirection, "tenantId")
            else -> Sort.by(Sort.Direction.DESC, "timestamp") // Default fallback
        }
    }
}

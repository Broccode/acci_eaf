package com.axians.eaf.controlplane.infrastructure.adapter.outbound.entity

import com.axians.eaf.controlplane.domain.model.audit.AuditEntry
import com.axians.eaf.controlplane.domain.model.audit.AuditEntryId
import com.axians.eaf.controlplane.domain.model.audit.AuditSeverity
import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.UserId
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

/** JPA entity for audit entries. Maps to the audit_entries table. */
@Entity
@Table(name = "audit_entries")
data class AuditEntryEntity(
    @Id @Column(name = "id", length = 36) val id: String,
    @Column(name = "tenant_id", nullable = false, length = 36) val tenantId: String,
    @Column(name = "performed_by", nullable = false, length = 36) val performedBy: String,
    @Column(name = "action", nullable = false, length = 100) val action: String,
    @Column(name = "target_type", nullable = false, length = 100) val targetType: String,
    @Column(name = "target_id", nullable = false, length = 255) val targetId: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    val details: String = "{}",
    @Column(name = "timestamp", nullable = false) val timestamp: Instant,
    @Column(name = "ip_address", nullable = false, length = 45) val ipAddress: String,
    @Column(name = "user_agent", nullable = false, columnDefinition = "TEXT") val userAgent: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    val severity: AuditSeverity = AuditSeverity.INFO,
    @Column(name = "success", nullable = false) val success: Boolean = true,
    @Column(name = "error_message", columnDefinition = "TEXT") val errorMessage: String? = null,
    @Column(name = "session_id", length = 255) val sessionId: String? = null,
    @Column(name = "correlation_id", length = 255) val correlationId: String? = null,
) {
    companion object {
        private val objectMapper = ObjectMapper()
        private val mapTypeRef = object : TypeReference<Map<String, Any>>() {}

        /** Converts a domain AuditEntry to a JPA entity. */
        fun fromDomain(auditEntry: AuditEntry): AuditEntryEntity =
            AuditEntryEntity(
                id = auditEntry.id.value,
                tenantId = auditEntry.tenantId.value,
                performedBy = auditEntry.performedBy.value,
                action = auditEntry.action,
                targetType = auditEntry.targetType,
                targetId = auditEntry.targetId,
                details = objectMapper.writeValueAsString(auditEntry.details),
                timestamp = auditEntry.timestamp,
                ipAddress = auditEntry.ipAddress,
                userAgent = auditEntry.userAgent,
                severity = auditEntry.severity,
                success = auditEntry.success,
                errorMessage = auditEntry.errorMessage,
                sessionId = auditEntry.sessionId,
                correlationId = auditEntry.correlationId,
            )
    }

    /** Converts this JPA entity to a domain AuditEntry. */
    fun toDomain(): AuditEntry {
        val detailsMap =
            try {
                objectMapper.readValue(details, mapTypeRef)
            } catch (e: Exception) {
                emptyMap<String, Any>()
            }

        return AuditEntry(
            id = AuditEntryId.fromString(id),
            tenantId = TenantId.fromString(tenantId),
            performedBy = UserId.fromString(performedBy),
            action = action,
            targetType = targetType,
            targetId = targetId,
            details = detailsMap,
            timestamp = timestamp,
            ipAddress = ipAddress,
            userAgent = userAgent,
            severity = severity,
            success = success,
            errorMessage = errorMessage,
            sessionId = sessionId,
            correlationId = correlationId,
        )
    }
}

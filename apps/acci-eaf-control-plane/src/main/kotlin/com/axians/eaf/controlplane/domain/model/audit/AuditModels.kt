package com.axians.eaf.controlplane.domain.model.audit

import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.UserId
import java.time.Instant
import java.util.UUID

/** Value object representing a unique audit entry identifier. */
@JvmInline
value class AuditEntryId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Audit entry ID cannot be blank" }
    }

    companion object {
        fun generate(): AuditEntryId = AuditEntryId(UUID.randomUUID().toString())

        fun fromString(value: String): AuditEntryId = AuditEntryId(value)
    }

    override fun toString(): String = value
}

/** Enumeration of administrative actions that can be audited. */
enum class AdminAction {
    // Tenant Management
    TENANT_CREATED,
    TENANT_UPDATED,
    TENANT_SUSPENDED,
    TENANT_REACTIVATED,
    TENANT_ARCHIVED,

    // User Management
    USER_CREATED,
    USER_UPDATED,
    USER_ACTIVATED,
    USER_DEACTIVATED,
    USER_PASSWORD_RESET,
    USER_ROLES_ASSIGNED,
    USER_ROLES_REMOVED,
    USER_BULK_OPERATION,

    // Role & Permission Management
    ROLE_CREATED,
    ROLE_UPDATED,
    ROLE_DELETED,
    PERMISSION_ASSIGNED_TO_ROLE,
    PERMISSION_REMOVED_FROM_ROLE,
    PERMISSION_CREATED,
    PERMISSION_DELETED,

    // Invitation Management
    INVITATION_SENT,
    INVITATION_RESENT,
    INVITATION_CANCELLED,
    INVITATION_EXPIRED,

    // Security Actions
    LOGIN_SUCCESSFUL,
    LOGIN_FAILED,
    LOGOUT,
    ACCESS_DENIED,
    PERMISSION_ESCALATION_ATTEMPTED,

    // System Actions
    CONFIGURATION_CHANGED,
    BULK_DATA_IMPORT,
    BULK_DATA_EXPORT,
    SYSTEM_MAINTENANCE,

    // Generic
    CUSTOM_ACTION,
    ;

    /** Get a human-readable description of this action. */
    fun getDescription(): String =
        when (this) {
            TENANT_CREATED -> "Tenant created"
            TENANT_UPDATED -> "Tenant updated"
            TENANT_SUSPENDED -> "Tenant suspended"
            TENANT_REACTIVATED -> "Tenant reactivated"
            TENANT_ARCHIVED -> "Tenant archived"
            USER_CREATED -> "User created"
            USER_UPDATED -> "User updated"
            USER_ACTIVATED -> "User activated"
            USER_DEACTIVATED -> "User deactivated"
            USER_PASSWORD_RESET -> "User password reset"
            USER_ROLES_ASSIGNED -> "User roles assigned"
            USER_ROLES_REMOVED -> "User roles removed"
            USER_BULK_OPERATION -> "Bulk user operation performed"
            ROLE_CREATED -> "Role created"
            ROLE_UPDATED -> "Role updated"
            ROLE_DELETED -> "Role deleted"
            PERMISSION_ASSIGNED_TO_ROLE -> "Permission assigned to role"
            PERMISSION_REMOVED_FROM_ROLE -> "Permission removed from role"
            PERMISSION_CREATED -> "Permission created"
            PERMISSION_DELETED -> "Permission deleted"
            INVITATION_SENT -> "User invitation sent"
            INVITATION_RESENT -> "User invitation resent"
            INVITATION_CANCELLED -> "User invitation cancelled"
            INVITATION_EXPIRED -> "User invitation expired"
            LOGIN_SUCCESSFUL -> "Successful login"
            LOGIN_FAILED -> "Failed login attempt"
            LOGOUT -> "User logout"
            ACCESS_DENIED -> "Access denied"
            PERMISSION_ESCALATION_ATTEMPTED -> "Permission escalation attempted"
            CONFIGURATION_CHANGED -> "System configuration changed"
            BULK_DATA_IMPORT -> "Bulk data import"
            BULK_DATA_EXPORT -> "Bulk data export"
            SYSTEM_MAINTENANCE -> "System maintenance performed"
            CUSTOM_ACTION -> "Custom action"
        }

    /** Check if this action represents a security-related event. */
    fun isSecurityEvent(): Boolean =
        when (this) {
            LOGIN_SUCCESSFUL,
            LOGIN_FAILED,
            LOGOUT,
            ACCESS_DENIED,
            PERMISSION_ESCALATION_ATTEMPTED,
            -> true
            else -> false
        }

    /** Check if this action represents a high-risk operation. */
    fun isHighRiskAction(): Boolean =
        when (this) {
            TENANT_ARCHIVED,
            USER_BULK_OPERATION,
            ROLE_DELETED,
            PERMISSION_DELETED,
            CONFIGURATION_CHANGED,
            BULK_DATA_IMPORT,
            BULK_DATA_EXPORT,
            -> true
            else -> false
        }
}

/** Enumeration of user actions that can be audited. */
enum class UserAction {
    // Profile Management
    PROFILE_UPDATED,
    PASSWORD_CHANGED,
    EMAIL_VERIFIED,

    // Content Actions
    CONTENT_CREATED,
    CONTENT_UPDATED,
    CONTENT_DELETED,
    CONTENT_VIEWED,

    // Collaboration Actions
    DOCUMENT_SHARED,
    DOCUMENT_UNSHARED,
    COMMENT_ADDED,
    COMMENT_UPDATED,
    COMMENT_DELETED,

    // System Interactions
    FILE_UPLOADED,
    FILE_DOWNLOADED,
    EXPORT_REQUESTED,
    REPORT_GENERATED,

    // Generic
    CUSTOM_USER_ACTION,
    ;

    /** Get a human-readable description of this action. */
    fun getDescription(): String =
        when (this) {
            PROFILE_UPDATED -> "Profile updated"
            PASSWORD_CHANGED -> "Password changed"
            EMAIL_VERIFIED -> "Email verified"
            CONTENT_CREATED -> "Content created"
            CONTENT_UPDATED -> "Content updated"
            CONTENT_DELETED -> "Content deleted"
            CONTENT_VIEWED -> "Content viewed"
            DOCUMENT_SHARED -> "Document shared"
            DOCUMENT_UNSHARED -> "Document unshared"
            COMMENT_ADDED -> "Comment added"
            COMMENT_UPDATED -> "Comment updated"
            COMMENT_DELETED -> "Comment deleted"
            FILE_UPLOADED -> "File uploaded"
            FILE_DOWNLOADED -> "File downloaded"
            EXPORT_REQUESTED -> "Export requested"
            REPORT_GENERATED -> "Report generated"
            CUSTOM_USER_ACTION -> "Custom user action"
        }
}

/** Represents the severity level of an audit entry. */
enum class AuditSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL,
    ;

    /** Check if this severity level requires immediate attention. */
    fun requiresAttention(): Boolean = this in setOf(ERROR, CRITICAL)
}

/** Domain events for audit operations. */
sealed class AuditEvent {
    abstract val auditEntryId: AuditEntryId
    abstract val tenantId: TenantId
    abstract val timestamp: Instant

    data class AuditEntryCreated(
        override val auditEntryId: AuditEntryId,
        override val tenantId: TenantId,
        val action: String,
        val targetType: String,
        val targetId: String,
        val performedBy: UserId,
        val severity: AuditSeverity,
        override val timestamp: Instant = Instant.now(),
    ) : AuditEvent()

    data class SecurityViolationDetected(
        override val auditEntryId: AuditEntryId,
        override val tenantId: TenantId,
        val violationType: String,
        val details: Map<String, Any>,
        val performedBy: UserId?,
        override val timestamp: Instant = Instant.now(),
    ) : AuditEvent()

    data class HighRiskActionPerformed(
        override val auditEntryId: AuditEntryId,
        override val tenantId: TenantId,
        val action: AdminAction,
        val targetType: String,
        val targetId: String,
        val performedBy: UserId,
        val requiresApproval: Boolean,
        override val timestamp: Instant = Instant.now(),
    ) : AuditEvent()
}

/** Represents an audit trail entry aggregate root. */
data class AuditEntry(
    val id: AuditEntryId,
    val tenantId: TenantId,
    val performedBy: UserId,
    val action: String,
    val targetType: String,
    val targetId: String,
    val details: Map<String, Any>,
    val timestamp: Instant,
    val ipAddress: String,
    val userAgent: String,
    val severity: AuditSeverity = AuditSeverity.INFO,
    val success: Boolean = true,
    val errorMessage: String? = null,
    val sessionId: String? = null,
    val correlationId: String? = null,
) {
    init {
        require(action.isNotBlank()) { "Action cannot be blank" }
        require(targetType.isNotBlank()) { "Target type cannot be blank" }
        require(targetId.isNotBlank()) { "Target ID cannot be blank" }
        require(ipAddress.isNotBlank()) { "IP address cannot be blank" }
        require(userAgent.isNotBlank()) { "User agent cannot be blank" }
        require(!success || errorMessage == null) { "Successful actions cannot have error messages" }
        require(success || !errorMessage.isNullOrBlank()) { "Failed actions must have error messages" }
    }

    /** Check if this audit entry represents a security event. */
    fun isSecurityEvent(): Boolean =
        try {
            AdminAction.valueOf(action).isSecurityEvent()
        } catch (e: IllegalArgumentException) {
            false
        }

    /** Check if this audit entry represents a high-risk action. */
    fun isHighRiskAction(): Boolean =
        try {
            AdminAction.valueOf(action).isHighRiskAction()
        } catch (e: IllegalArgumentException) {
            false
        }

    /** Get a human-readable description of this audit entry. */
    fun getDescription(): String =
        try {
            AdminAction.valueOf(action).getDescription()
        } catch (e: IllegalArgumentException) {
            try {
                UserAction.valueOf(action).getDescription()
            } catch (e: IllegalArgumentException) {
                action.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
            }
        }

    /** Create an audit event for this entry. */
    fun toEvent(): AuditEvent =
        when {
            isSecurityEvent() && !success ->
                AuditEvent.SecurityViolationDetected(
                    auditEntryId = id,
                    tenantId = tenantId,
                    violationType = action,
                    details =
                        details +
                            buildMap<String, Any> {
                                errorMessage?.let { put("error", it) }
                                put("ipAddress", ipAddress)
                                put("userAgent", userAgent)
                                sessionId?.let { put("sessionId", it) }
                                correlationId?.let { put("correlationId", it) }
                            },
                    performedBy = performedBy,
                    timestamp = timestamp,
                )
            isHighRiskAction() ->
                AuditEvent.HighRiskActionPerformed(
                    auditEntryId = id,
                    tenantId = tenantId,
                    action = AdminAction.valueOf(action),
                    targetType = targetType,
                    targetId = targetId,
                    performedBy = performedBy,
                    requiresApproval = severity == AuditSeverity.CRITICAL,
                    timestamp = timestamp,
                )
            else ->
                AuditEvent.AuditEntryCreated(
                    auditEntryId = id,
                    tenantId = tenantId,
                    action = action,
                    targetType = targetType,
                    targetId = targetId,
                    performedBy = performedBy,
                    severity = severity,
                    timestamp = timestamp,
                )
        }

    companion object {
        /** Create an audit entry for an administrative action. */
        fun forAdminAction(
            tenantId: TenantId,
            performedBy: UserId,
            action: AdminAction,
            targetType: String,
            targetId: String,
            details: Map<String, Any> = emptyMap(),
            ipAddress: String,
            userAgent: String,
            success: Boolean = true,
            errorMessage: String? = null,
            sessionId: String? = null,
            correlationId: String? = null,
        ): AuditEntry {
            val severity =
                when {
                    !success && action.isSecurityEvent() -> AuditSeverity.CRITICAL
                    !success -> AuditSeverity.ERROR
                    action.isHighRiskAction() -> AuditSeverity.WARNING
                    else -> AuditSeverity.INFO
                }

            return AuditEntry(
                id = AuditEntryId.generate(),
                tenantId = tenantId,
                performedBy = performedBy,
                action = action.name,
                targetType = targetType,
                targetId = targetId,
                details = details,
                timestamp = Instant.now(),
                ipAddress = ipAddress,
                userAgent = userAgent,
                severity = severity,
                success = success,
                errorMessage = errorMessage,
                sessionId = sessionId,
                correlationId = correlationId,
            )
        }

        /** Create an audit entry for a user action. */
        fun forUserAction(
            tenantId: TenantId,
            performedBy: UserId,
            action: UserAction,
            targetType: String,
            targetId: String,
            details: Map<String, Any> = emptyMap(),
            ipAddress: String,
            userAgent: String,
            success: Boolean = true,
            errorMessage: String? = null,
            sessionId: String? = null,
            correlationId: String? = null,
        ): AuditEntry =
            AuditEntry(
                id = AuditEntryId.generate(),
                tenantId = tenantId,
                performedBy = performedBy,
                action = action.name,
                targetType = targetType,
                targetId = targetId,
                details = details,
                timestamp = Instant.now(),
                ipAddress = ipAddress,
                userAgent = userAgent,
                severity = if (success) AuditSeverity.INFO else AuditSeverity.ERROR,
                success = success,
                errorMessage = errorMessage,
                sessionId = sessionId,
                correlationId = correlationId,
            )
    }
}

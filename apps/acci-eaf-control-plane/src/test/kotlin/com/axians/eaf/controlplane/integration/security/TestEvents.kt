package com.axians.eaf.controlplane.integration.security

import com.axians.eaf.controlplane.domain.model.user.UserStatus
import java.time.Instant

/** Test event for user creation. Used to verify security context propagation in events. */
data class TestUserCreatedEvent(
    val userId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val tenantId: String,
    val timestamp: Instant = Instant.now(),
) {
    init {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(tenantId.isNotBlank()) { "Tenant ID cannot be blank" }
    }
}

/** Test event for tenant creation. Used to verify admin role security context propagation. */
data class TestTenantCreatedEvent(
    val tenantId: String,
    val name: String,
    val adminEmail: String,
    val timestamp: Instant = Instant.now(),
) {
    init {
        require(tenantId.isNotBlank()) { "Tenant ID cannot be blank" }
        require(name.isNotBlank()) { "Tenant name cannot be blank" }
        require(adminEmail.isNotBlank()) { "Admin email cannot be blank" }
    }
}

/** Test event for user status updates. Used to verify security context in state changes. */
data class TestUserStatusUpdatedEvent(
    val userId: String,
    val oldStatus: UserStatus,
    val newStatus: UserStatus,
    val reason: String?,
    val updatedBy: String,
    val tenantId: String,
    val timestamp: Instant = Instant.now(),
) {
    init {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(updatedBy.isNotBlank()) { "Updated by cannot be blank" }
        require(tenantId.isNotBlank()) { "Tenant ID cannot be blank" }
    }
}

/** Test event for system maintenance operations. Used to verify system context handling. */
data class TestSystemMaintenanceEvent(
    val eventType: String,
    val description: String,
    val timestamp: Instant = Instant.now(),
) {
    init {
        require(eventType.isNotBlank()) { "Event type cannot be blank" }
        require(description.isNotBlank()) { "Description cannot be blank" }
    }
}

/**
 * Test event for bulk operations. Used to verify performance and concurrency in event propagation.
 */
data class TestBulkUsersCreatedEvent(
    val batchId: String,
    val userIds: List<String>,
    val tenantId: String,
    val totalCount: Int,
    val timestamp: Instant = Instant.now(),
) {
    init {
        require(batchId.isNotBlank()) { "Batch ID cannot be blank" }
        require(userIds.isNotEmpty()) { "User IDs cannot be empty" }
        require(tenantId.isNotBlank()) { "Tenant ID cannot be blank" }
        require(totalCount > 0) { "Total count must be positive" }
    }
}

/** Test event for security violations. Used to verify audit trail for security incidents. */
data class TestSecurityViolationEvent(
    val violationType: String,
    val userId: String?,
    val tenantId: String?,
    val description: String,
    val severity: SecurityViolationSeverity,
    val ipAddress: String?,
    val userAgent: String?,
    val timestamp: Instant = Instant.now(),
) {
    init {
        require(violationType.isNotBlank()) { "Violation type cannot be blank" }
        require(description.isNotBlank()) { "Description cannot be blank" }
    }
}

enum class SecurityViolationSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

/** Test event for admin actions. Used to verify admin role security context propagation. */
data class TestAdminActionEvent(
    val adminUserId: String,
    val action: String,
    val timestamp: Instant = Instant.now(),
) {
    init {
        require(adminUserId.isNotBlank()) { "Admin user ID cannot be blank" }
        require(action.isNotBlank()) { "Action cannot be blank" }
    }
}

/** Test event for system operations. Used to verify system context handling. */
data class TestSystemEvent(
    val operation: String,
    val timestamp: Instant = Instant.now(),
) {
    init {
        require(operation.isNotBlank()) { "Operation cannot be blank" }
    }
}

/** Test event for user activation. Used to verify state change security context. */
data class TestUserActivatedEvent(
    val userId: String,
    val timestamp: Instant = Instant.now(),
) {
    init {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
    }
}

/** Test event for email operations. Used to verify notification security context. */
data class TestEmailSentEvent(
    val userId: String,
    val emailType: String,
    val timestamp: Instant = Instant.now(),
) {
    init {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(emailType.isNotBlank()) { "Email type cannot be blank" }
    }
}

/** Test event for concurrent operations. Used to verify concurrency security context. */
data class TestConcurrentEvent(
    val operationId: String,
    val timestamp: Instant = Instant.now(),
) {
    init {
        require(operationId.isNotBlank()) { "Operation ID cannot be blank" }
    }
}

/** Test event for performance testing. Used to verify performance with security context. */
data class TestPerformanceEvent(
    val performanceId: String,
    val timestamp: Instant = Instant.now(),
) {
    init {
        require(performanceId.isNotBlank()) { "Performance ID cannot be blank" }
    }
}

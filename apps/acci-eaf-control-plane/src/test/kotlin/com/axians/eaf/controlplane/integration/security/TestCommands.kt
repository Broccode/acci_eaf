package com.axians.eaf.controlplane.integration.security

/**
 * Test command for creating a user. Used to test security context propagation in command handling.
 */
data class CreateTestUserCommand(
    val userId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
) {
    init {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(firstName.isNotBlank()) { "First name cannot be blank" }
        require(lastName.isNotBlank()) { "Last name cannot be blank" }
    }
}

/** Test command for creating a tenant. Used to test admin role security context propagation. */
data class CreateTestTenantCommand(
    val tenantId: String,
    val name: String,
    val adminEmail: String,
) {
    init {
        require(tenantId.isNotBlank()) { "Tenant ID cannot be blank" }
        require(name.isNotBlank()) { "Tenant name cannot be blank" }
        require(adminEmail.isNotBlank()) { "Admin email cannot be blank" }
    }
}

/** Test command for updating user status. Used to test different permission levels. */
data class UpdateTestUserStatusCommand(
    val userId: String,
    val newStatus: UserStatus,
    val reason: String? = null,
) {
    init {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
    }
}

/** Test command for bulk operations. Used to test performance and concurrency scenarios. */
data class BulkCreateTestUsersCommand(
    val users: List<CreateTestUserCommand>,
    val batchId: String,
) {
    init {
        require(users.isNotEmpty()) { "User list cannot be empty" }
        require(batchId.isNotBlank()) { "Batch ID cannot be blank" }
    }
}

enum class UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    PENDING_ACTIVATION,
}

package com.axians.eaf.controlplane.infrastructure.adapter.outbound.repository

import com.axians.eaf.controlplane.application.dto.tenant.PagedResponse
import com.axians.eaf.controlplane.application.dto.user.UserFilter
import com.axians.eaf.controlplane.application.dto.user.UserSummary
import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.User
import com.axians.eaf.controlplane.domain.model.user.UserId
import com.axians.eaf.controlplane.domain.model.user.UserStatus
import com.axians.eaf.controlplane.domain.port.UserRepository
import com.axians.eaf.iam.client.CreateUserRequest
import com.axians.eaf.iam.client.IamServiceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * UserRepository implementation that delegates to the IAM service. This follows the microservices
 * architecture where the Control Plane acts as an administrative interface and the IAM service owns
 * user data.
 */
@Repository
class IamServiceUserRepository(
    private val iamServiceClient: IamServiceClient,
) : UserRepository {
    override suspend fun save(user: User): User =
        withContext(Dispatchers.IO) {
            if (user.id.value.startsWith("new-")) {
                // Create new user via IAM service
                val request =
                    CreateUserRequest(
                        email = user.email,
                        username = "${user.firstName}.${user.lastName}".lowercase(),
                    )
                val response = iamServiceClient.createUser(user.tenantId.value, request)

                // Convert response back to domain model
                user.copy(id = UserId.fromString(response.userId), lastModified = Instant.now())
            } else {
                // Update existing user - for now, just update status if it changed
                // Would need additional IAM service APIs for full user updates
                user
            }
        }

    override suspend fun findById(userId: UserId): User? =
        withContext(Dispatchers.IO) {
            // Would need additional IAM service API to find user by ID across tenants
            // For now, not implemented
            null
        }

    override suspend fun findByEmailAndTenantId(
        email: String,
        tenantId: TenantId,
    ): User? =
        withContext(Dispatchers.IO) {
            try {
                val response = iamServiceClient.listUsers(tenantId.value)
                val userSummary = response.users.find { it.email == email }
                userSummary?.let { convertToUser(it, tenantId) }
            } catch (e: Exception) {
                null
            }
        }

    override suspend fun findByEmail(email: String): User? =
        withContext(Dispatchers.IO) {
            // Would need additional IAM service API to find user by email across all tenants
            // For now, not implemented
            null
        }

    override suspend fun existsById(userId: UserId): Boolean = withContext(Dispatchers.IO) { findById(userId) != null }

    override suspend fun existsByEmailAndTenantId(
        email: String,
        tenantId: TenantId,
    ): Boolean = withContext(Dispatchers.IO) { findByEmailAndTenantId(email, tenantId) != null }

    override suspend fun findAll(filter: UserFilter): PagedResponse<UserSummary> =
        withContext(Dispatchers.IO) {
            if (filter.tenantId != null) {
                try {
                    val response = iamServiceClient.listUsers(filter.tenantId)
                    val filteredUsers =
                        response.users.filter { user ->
                            (filter.status == null || user.status == filter.status.name) &&
                                run {
                                    val searchTerm = filter.emailPattern ?: filter.namePattern
                                    if (searchTerm == null) {
                                        true
                                    } else {
                                        user.email.contains(
                                            searchTerm,
                                            ignoreCase = true,
                                        ) ||
                                            (
                                                user.username?.contains(
                                                    searchTerm,
                                                    ignoreCase = true,
                                                ) == true
                                            )
                                    }
                                }
                        }

                    // Simple pagination
                    val start = filter.page * filter.size
                    val end = minOf(start + filter.size, filteredUsers.size)
                    val pagedUsers =
                        if (start < filteredUsers.size) {
                            filteredUsers.subList(start, end)
                        } else {
                            emptyList()
                        }

                    PagedResponse(
                        content = pagedUsers.map { convertToUserSummary(it) },
                        page = filter.page,
                        size = filter.size,
                        totalElements = filteredUsers.size.toLong(),
                        totalPages = (filteredUsers.size + filter.size - 1) / filter.size,
                    )
                } catch (e: Exception) {
                    PagedResponse(
                        content = emptyList(),
                        page = filter.page,
                        size = filter.size,
                        totalElements = 0,
                        totalPages = 0,
                    )
                }
            } else {
                // Would need cross-tenant user listing API
                PagedResponse(
                    content = emptyList(),
                    page = filter.page,
                    size = filter.size,
                    totalElements = 0,
                    totalPages = 0,
                )
            }
        }

    override suspend fun findByTenantId(tenantId: TenantId): List<User> =
        withContext(Dispatchers.IO) {
            try {
                val response = iamServiceClient.listUsers(tenantId.value)
                response.users.map { convertToUser(it, tenantId) }
            } catch (e: Exception) {
                emptyList()
            }
        }

    override suspend fun findByTenantIdAndStatus(
        tenantId: TenantId,
        status: UserStatus,
    ): List<User> =
        withContext(Dispatchers.IO) {
            try {
                val response = iamServiceClient.listUsers(tenantId.value)
                response.users.filter { it.status == status.name }.map { convertToUser(it, tenantId) }
            } catch (e: Exception) {
                emptyList()
            }
        }

    override suspend fun findByIds(userIds: Set<UserId>): List<User> =
        withContext(Dispatchers.IO) {
            // Would need additional IAM service API for bulk user retrieval
            // For now, not implemented
            emptyList()
        }

    override suspend fun countByTenantId(tenantId: TenantId): Long =
        withContext(Dispatchers.IO) {
            try {
                val response = iamServiceClient.listUsers(tenantId.value)
                response.users.size.toLong()
            } catch (e: Exception) {
                0L
            }
        }

    override suspend fun countByTenantIdAndStatus(
        tenantId: TenantId,
        status: UserStatus,
    ): Long =
        withContext(Dispatchers.IO) {
            try {
                val response = iamServiceClient.listUsers(tenantId.value)
                response.users.count { it.status == status.name }.toLong()
            } catch (e: Exception) {
                0L
            }
        }

    override suspend fun findInactiveUsers(
        tenantId: TenantId,
        sinceDate: Instant,
    ): List<User> =
        withContext(Dispatchers.IO) {
            // Would need additional IAM service API for activity tracking
            // For now, not implemented
            emptyList()
        }

    override suspend fun deleteById(userId: UserId): Boolean =
        withContext(Dispatchers.IO) {
            // Would need additional IAM service API for user deletion
            // For now, not implemented
            false
        }

    override suspend fun deleteByTenantId(tenantId: TenantId): Int =
        withContext(Dispatchers.IO) {
            // Would need additional IAM service API for bulk user deletion
            // For now, not implemented
            0
        }

    private fun convertToUser(
        userSummary: com.axians.eaf.iam.client.UserSummaryResponse,
        tenantId: TenantId,
    ): User {
        // Parse first and last name from username or email
        val parts = userSummary.username?.split(".") ?: userSummary.email.split("@")[0].split(".")
        val firstName = parts.getOrNull(0)?.replaceFirstChar { it.uppercase() } ?: "Unknown"
        val lastName = parts.getOrNull(1)?.replaceFirstChar { it.uppercase() } ?: "User"

        return User(
            id = UserId.fromString(userSummary.userId),
            tenantId = tenantId,
            email = userSummary.email,
            firstName = firstName,
            lastName = lastName,
            status = UserStatus.valueOf(userSummary.status),
            roles = emptySet(), // Roles mapping not yet implemented
            lastLogin = null, // Would need additional data from IAM service
            createdAt = Instant.now(), // Would need additional data from IAM service
            lastModified = Instant.now(),
        )
    }

    private fun convertToUserSummary(userSummary: com.axians.eaf.iam.client.UserSummaryResponse): UserSummary {
        val parts = userSummary.username?.split(".") ?: userSummary.email.split("@")[0].split(".")
        val firstName = parts.getOrNull(0)?.replaceFirstChar { it.uppercase() } ?: "Unknown"
        val lastName = parts.getOrNull(1)?.replaceFirstChar { it.uppercase() } ?: "User"

        return UserSummary(
            id = userSummary.userId,
            email = userSummary.email,
            fullName = "$firstName $lastName",
            status = UserStatus.valueOf(userSummary.status),
            roles = emptyList(), // Role mapping pending
            lastLogin = null, // Would need additional data from IAM service
            createdAt = Instant.now(), // Would need additional data from IAM service
        )
    }
}

package com.axians.eaf.controlplane.domain.port

import com.axians.eaf.controlplane.application.dto.tenant.PagedResponse
import com.axians.eaf.controlplane.application.dto.user.UserFilter
import com.axians.eaf.controlplane.application.dto.user.UserSummary
import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.User
import com.axians.eaf.controlplane.domain.model.user.UserId
import com.axians.eaf.controlplane.domain.model.user.UserStatus

/**
 * Repository port for user aggregate persistence. This is a domain contract that will be
 * implemented by infrastructure adapters.
 */
interface UserRepository {
    /**
     * Saves a user to the repository. For new users, this will create them. For existing ones, this
     * will update them.
     */
    suspend fun save(user: User): User

    /** Finds a user by their unique identifier. */
    suspend fun findById(userId: UserId): User?

    /** Finds a user by email within a specific tenant. */
    suspend fun findByEmailAndTenantId(
        email: String,
        tenantId: TenantId,
    ): User?

    /** Finds a user by email (across all tenants - for platform operations). */
    suspend fun findByEmail(email: String): User?

    /** Checks if a user exists with the given ID. */
    suspend fun existsById(userId: UserId): Boolean

    /** Checks if a user exists with the given email within a tenant. */
    suspend fun existsByEmailAndTenantId(
        email: String,
        tenantId: TenantId,
    ): Boolean

    /** Finds all users matching the given filter criteria. */
    suspend fun findAll(filter: UserFilter): PagedResponse<UserSummary>

    /** Finds all users belonging to a specific tenant. */
    suspend fun findByTenantId(tenantId: TenantId): List<User>

    /** Finds users by their status within a specific tenant. */
    suspend fun findByTenantIdAndStatus(
        tenantId: TenantId,
        status: UserStatus,
    ): List<User>

    /** Finds users by multiple IDs (for bulk operations). */
    suspend fun findByIds(userIds: Set<UserId>): List<User>

    /** Counts the total number of users in a specific tenant. */
    suspend fun countByTenantId(tenantId: TenantId): Long

    /** Counts the number of active users in a specific tenant. */
    suspend fun countByTenantIdAndStatus(
        tenantId: TenantId,
        status: UserStatus,
    ): Long

    /** Finds users who haven't logged in since a specific date. */
    suspend fun findInactiveUsers(
        tenantId: TenantId,
        sinceDate: java.time.Instant,
    ): List<User>

    /** Deletes a user by ID (typically only used for testing). */
    suspend fun deleteById(userId: UserId): Boolean

    /** Deletes all users for a specific tenant (used when tenant is archived). */
    suspend fun deleteByTenantId(tenantId: TenantId): Int
}

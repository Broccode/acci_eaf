package com.axians.eaf.iam.infrastructure.adapter.outbound.persistence

import com.axians.eaf.iam.infrastructure.adapter.outbound.persistence.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * JPA Repository for User entities.
 * Provides database operations for user management with proper tenant isolation.
 */
@Repository
interface UserJpaRepository : JpaRepository<UserEntity, String> {
    /**
     * Check if a user with the given email exists across all tenants (case-insensitive).
     *
     * @param email The email to check (will be compared case-insensitively)
     * @return true if a user with this email exists, false otherwise
     */
    @Query(
        "SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UserEntity u WHERE LOWER(u.email) = LOWER(:email)",
    )
    fun existsByEmailIgnoreCase(
        @Param("email") email: String,
    ): Boolean

    /**
     * Find a user by email (case-insensitive).
     *
     * @param email The email to search for
     * @return The user if found, null otherwise
     */
    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.email) = LOWER(:email)")
    fun findByEmailIgnoreCase(
        @Param("email") email: String,
    ): UserEntity?

    /**
     * Find all users belonging to a specific tenant.
     *
     * @param tenantId The tenant ID
     * @return List of users in the tenant
     */
    fun findByTenantId(tenantId: String): List<UserEntity>

    /**
     * Find a user by email within a specific tenant (case-insensitive).
     *
     * @param email The email to search for
     * @param tenantId The tenant ID
     * @return The user if found, null otherwise
     */
    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.email) = LOWER(:email) AND u.tenantId = :tenantId")
    fun findByEmailAndTenantIdIgnoreCase(
        @Param("email") email: String,
        @Param("tenantId") tenantId: String,
    ): UserEntity?
}

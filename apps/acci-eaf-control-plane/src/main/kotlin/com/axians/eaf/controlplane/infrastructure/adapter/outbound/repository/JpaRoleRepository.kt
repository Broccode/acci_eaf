package com.axians.eaf.controlplane.infrastructure.adapter.outbound.repository

import com.axians.eaf.controlplane.domain.model.user.RoleScope
import com.axians.eaf.controlplane.infrastructure.adapter.outbound.entity.RoleEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * JPA repository interface for RoleEntity persistence operations. Supports both platform-wide and
 * tenant-scoped role queries.
 */
@Repository
interface JpaRoleRepository : JpaRepository<RoleEntity, UUID> {
    /**
     * Find a role by name within a specific scope and tenant. For platform roles, tenantId should
     * be null.
     */
    fun findByNameAndScopeAndTenantId(
        name: String,
        scope: RoleScope,
        tenantId: UUID?,
    ): RoleEntity?

    /** Find all roles by scope. */
    fun findByScope(scope: RoleScope): List<RoleEntity>

    /** Find all roles for a specific tenant (includes tenant-scoped roles only). */
    fun findByScopeAndTenantId(
        scope: RoleScope,
        tenantId: UUID,
    ): List<RoleEntity>

    /** Find all platform-wide roles. */
    fun findByScopeAndTenantIdIsNull(scope: RoleScope): List<RoleEntity>

    /** Find all system roles (both platform and tenant-scoped). */
    fun findByIsSystemRole(isSystemRole: Boolean): List<RoleEntity>

    /** Find system roles by scope. */
    fun findByScopeAndIsSystemRole(
        scope: RoleScope,
        isSystemRole: Boolean,
    ): List<RoleEntity>

    /** Find roles by names within a scope and tenant. */
    fun findByNameInAndScopeAndTenantId(
        names: Collection<String>,
        scope: RoleScope,
        tenantId: UUID?,
    ): List<RoleEntity>

    /** Check if a role exists by name within scope and tenant. */
    fun existsByNameAndScopeAndTenantId(
        name: String,
        scope: RoleScope,
        tenantId: UUID?,
    ): Boolean

    /** Find all roles with their permissions eagerly loaded. */
    @Query("SELECT DISTINCT r FROM RoleEntity r LEFT JOIN FETCH r.permissions")
    fun findAllWithPermissions(): List<RoleEntity>

    /** Find a role by ID with permissions eagerly loaded. */
    @Query("SELECT r FROM RoleEntity r LEFT JOIN FETCH r.permissions WHERE r.id = :roleId")
    fun findByIdWithPermissions(
        @Param("roleId") roleId: UUID,
    ): RoleEntity?

    /** Find roles by scope with permissions eagerly loaded. */
    @Query(
        "SELECT DISTINCT r FROM RoleEntity r LEFT JOIN FETCH r.permissions WHERE r.scope = :scope",
    )
    fun findByScopeWithPermissions(
        @Param("scope") scope: RoleScope,
    ): List<RoleEntity>

    /** Find tenant roles with permissions eagerly loaded. */
    @Query(
        "SELECT DISTINCT r FROM RoleEntity r LEFT JOIN FETCH r.permissions WHERE r.scope = :scope AND r.tenantId = :tenantId",
    )
    fun findByScopeAndTenantIdWithPermissions(
        @Param("scope") scope: RoleScope,
        @Param("tenantId") tenantId: UUID,
    ): List<RoleEntity>

    /** Find platform roles with permissions eagerly loaded. */
    @Query(
        "SELECT DISTINCT r FROM RoleEntity r LEFT JOIN FETCH r.permissions WHERE r.scope = 'PLATFORM' AND r.tenantId IS NULL",
    )
    fun findPlatformRolesWithPermissions(): List<RoleEntity>

    /** Count roles by tenant. */
    fun countByScopeAndTenantId(
        scope: RoleScope,
        tenantId: UUID,
    ): Long

    /** Count platform roles. */
    fun countByScopeAndTenantIdIsNull(scope: RoleScope): Long

    /** Delete non-system roles by tenant (used when tenant is archived). */
    @Query(
        "DELETE FROM RoleEntity r WHERE r.scope = 'TENANT' AND r.tenantId = :tenantId AND r.isSystemRole = false",
    )
    fun deleteNonSystemRolesByTenantId(
        @Param("tenantId") tenantId: UUID,
    ): Int
}

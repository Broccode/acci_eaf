package com.axians.eaf.controlplane.infrastructure.adapter.outbound.repository

import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.Permission
import com.axians.eaf.controlplane.domain.model.user.PermissionId
import com.axians.eaf.controlplane.domain.model.user.Role
import com.axians.eaf.controlplane.domain.model.user.RoleId
import com.axians.eaf.controlplane.domain.model.user.RoleScope
import com.axians.eaf.controlplane.domain.port.RoleRepository
import com.axians.eaf.controlplane.infrastructure.adapter.outbound.entity.PermissionEntity
import com.axians.eaf.controlplane.infrastructure.adapter.outbound.entity.RoleEntity
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * JPA-based implementation of the RoleRepository domain port. Provides persistent storage for roles
 * and permissions using PostgreSQL database.
 */
@Repository
@Transactional
class JpaRoleRepositoryImpl(
    private val jpaRoleRepository: JpaRoleRepository,
    private val jpaPermissionRepository: JpaPermissionRepository,
) : RoleRepository {
    override suspend fun saveRole(role: Role): Role {
        // Find all permission entities that this role should have
        val permissionEntities =
            role.permissions
                .mapNotNull { permission ->
                    jpaPermissionRepository
                        .findById(UUID.fromString(permission.id.value))
                        .orElse(null)
                }.toSet()

        // Create role entity with permissions
        val roleEntity = RoleEntity.fromDomainWithPermissions(role, permissionEntities)
        val savedEntity = jpaRoleRepository.save(roleEntity)

        return savedEntity.toDomain()
    }

    override suspend fun findRoleById(roleId: RoleId): Role? =
        jpaRoleRepository.findByIdWithPermissions(UUID.fromString(roleId.value))?.toDomain()

    override suspend fun findRolesByScope(
        scope: RoleScope,
        tenantId: TenantId?,
    ): List<Role> =
        if (tenantId != null) {
            jpaRoleRepository.findByScopeAndTenantIdWithPermissions(
                scope,
                UUID.fromString(tenantId.value),
            )
        } else {
            jpaRoleRepository.findByScopeWithPermissions(scope)
        }.map { it.toDomain() }

    override suspend fun findRolesByTenantId(tenantId: TenantId): List<Role> =
        jpaRoleRepository
            .findByScopeAndTenantIdWithPermissions(
                scope = RoleScope.TENANT,
                tenantId = UUID.fromString(tenantId.value),
            ).map { it.toDomain() }

    override suspend fun findRoleByName(
        name: String,
        scope: RoleScope,
        tenantId: TenantId?,
    ): Role? =
        jpaRoleRepository
            .findByNameAndScopeAndTenantId(
                name = name,
                scope = scope,
                tenantId = tenantId?.value?.let { UUID.fromString(it) },
            )?.toDomain()

    override suspend fun existsRoleById(roleId: RoleId): Boolean =
        jpaRoleRepository.existsById(UUID.fromString(roleId.value))

    override suspend fun existsRoleByName(
        name: String,
        scope: RoleScope,
        tenantId: TenantId?,
    ): Boolean =
        jpaRoleRepository.existsByNameAndScopeAndTenantId(
            name = name,
            scope = scope,
            tenantId = tenantId?.value?.let { UUID.fromString(it) },
        )

    override suspend fun deleteRoleById(roleId: RoleId): Boolean =
        if (jpaRoleRepository.existsById(UUID.fromString(roleId.value))) {
            jpaRoleRepository.deleteById(UUID.fromString(roleId.value))
            true
        } else {
            false
        }

    // Permission management methods

    override suspend fun savePermission(permission: Permission): Permission {
        val entity = PermissionEntity.fromDomain(permission)
        val savedEntity = jpaPermissionRepository.save(entity)
        return savedEntity.toDomain()
    }

    override suspend fun findPermissionById(permissionId: PermissionId): Permission? =
        jpaPermissionRepository
            .findById(UUID.fromString(permissionId.value))
            .map { it.toDomain() }
            .orElse(null)

    override suspend fun findAllPermissions(): List<Permission> =
        jpaPermissionRepository.findAllOrderedByResourceAndAction().map {
            it.toDomain()
        }

    override suspend fun findPermissionsByResource(resource: String): List<Permission> =
        jpaPermissionRepository.findByResource(resource).map {
            it.toDomain()
        }

    override suspend fun existsPermissionById(permissionId: PermissionId): Boolean =
        jpaPermissionRepository.existsById(UUID.fromString(permissionId.value))

    override suspend fun existsPermissionByResourceAndAction(
        resource: String,
        action: String,
    ): Boolean = jpaPermissionRepository.findByResourceAndAction(resource, action).isNotEmpty()

    override suspend fun deletePermissionById(permissionId: PermissionId): Boolean =
        if (jpaPermissionRepository.existsById(UUID.fromString(permissionId.value))) {
            jpaPermissionRepository.deleteById(UUID.fromString(permissionId.value))
            true
        } else {
            false
        }
}

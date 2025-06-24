package com.axians.eaf.controlplane.infrastructure.adapter.outbound.entity

import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.Role
import com.axians.eaf.controlplane.domain.model.user.RoleId
import com.axians.eaf.controlplane.domain.model.user.RoleScope
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

/**
 * JPA entity for Role persistence. Maps to the roles table in the database. Supports both
 * platform-wide and tenant-scoped roles with many-to-many relationships to permissions.
 */
@Entity
@Table(
    name = "roles",
    indexes =
        [
            Index(name = "idx_roles_scope", columnList = "scope"),
            Index(name = "idx_roles_tenant_id", columnList = "tenant_id"),
        ],
)
class RoleEntity(
    @Id @Column(name = "id", columnDefinition = "UUID") val id: UUID = UUID.randomUUID(),
    @Column(name = "name", nullable = false, length = 100) val name: String,
    @Column(name = "description", columnDefinition = "TEXT") val description: String?,
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    val scope: RoleScope,
    @Column(name = "tenant_id", columnDefinition = "UUID") val tenantId: UUID? = null,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
    @ManyToMany(fetch = FetchType.LAZY, cascade = [CascadeType.MERGE])
    @JoinTable(
        name = "role_permissions",
        joinColumns = [JoinColumn(name = "role_id")],
        inverseJoinColumns = [JoinColumn(name = "permission_id")],
    )
    val permissions: MutableSet<PermissionEntity> = mutableSetOf(),
) {
    /** Convert this entity to a domain Role object. */
    fun toDomain(): Role {
        val baseRole =
            if (scope == RoleScope.PLATFORM) {
                Role.createPlatformRole(
                    id = RoleId(id.toString()),
                    name = name,
                    description = description ?: "",
                )
            } else {
                Role.createTenantRole(
                    id = RoleId(id.toString()),
                    name = name,
                    description = description ?: "",
                    tenantId = tenantId?.let { TenantId(it.toString()) }!!,
                )
            }

        // Add permissions to the role
        return permissions.fold(baseRole) { acc, permissionEntity ->
            acc.addPermission(permissionEntity.toDomain())
        }
    }

    /** Add a permission to this role. */
    fun addPermission(permission: PermissionEntity) {
        permissions.add(permission)
    }

    /** Remove a permission from this role. */
    fun removePermission(permission: PermissionEntity) {
        permissions.remove(permission)
    }

    /** Clear all permissions from this role. */
    fun clearPermissions() {
        permissions.clear()
    }

    companion object {
        /**
         * Create an entity from a domain Role object. Note: Permissions need to be managed
         * separately for proper JPA handling.
         */
        fun fromDomain(role: Role): RoleEntity =
            RoleEntity(
                id = UUID.fromString(role.id.value),
                name = role.name,
                description = role.description,
                scope = role.scope,
                tenantId = role.tenantId?.value?.let { UUID.fromString(it) },
            )

        /**
         * Create an entity from a domain Role object with permissions. This method properly handles
         * the ManyToMany relationship.
         */
        fun fromDomainWithPermissions(
            role: Role,
            permissionEntities: Set<PermissionEntity>,
        ): RoleEntity {
            val entity = fromDomain(role)
            entity.permissions.addAll(permissionEntities)
            return entity
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RoleEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "RoleEntity(id=$id, name='$name', scope=$scope, tenantId=$tenantId)"
}

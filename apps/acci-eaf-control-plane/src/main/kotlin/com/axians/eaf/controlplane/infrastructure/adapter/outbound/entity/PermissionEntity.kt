package com.axians.eaf.controlplane.infrastructure.adapter.outbound.entity

import com.axians.eaf.controlplane.domain.model.user.Permission
import com.axians.eaf.controlplane.domain.model.user.PermissionId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

/** JPA entity for Permission persistence. Maps to the permissions table in the database. */
@Entity
@Table(
    name = "permissions",
    uniqueConstraints = [UniqueConstraint(name = "uk_permission_name", columnNames = ["name"])],
    indexes = [Index(name = "idx_permissions_resource_action", columnList = "resource, action")],
)
class PermissionEntity(
    @Id @Column(name = "id", columnDefinition = "UUID") val id: UUID = UUID.randomUUID(),
    @Column(name = "name", nullable = false, length = 100) val name: String,
    @Column(name = "description", columnDefinition = "TEXT") val description: String?,
    @Column(name = "resource", nullable = false, length = 50) val resource: String,
    @Column(name = "action", nullable = false, length = 50) val action: String,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
) {
    /** Convert this entity to a domain Permission object. */
    fun toDomain(): Permission =
        Permission.create(
            id = PermissionId(id.toString()),
            name = name,
            description = description ?: "",
            resource = resource,
            action = action,
        )

    companion object {
        /** Create an entity from a domain Permission object. */
        fun fromDomain(permission: Permission): PermissionEntity =
            PermissionEntity(
                id = UUID.fromString(permission.id.value),
                name = permission.name,
                description = permission.description,
                resource = permission.resource,
                action = permission.action,
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PermissionEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "PermissionEntity(id=$id, name='$name', resource='$resource', action='$action')"
}

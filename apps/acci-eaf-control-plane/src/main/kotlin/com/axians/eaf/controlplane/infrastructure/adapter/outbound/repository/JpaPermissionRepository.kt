package com.axians.eaf.controlplane.infrastructure.adapter.outbound.repository

import com.axians.eaf.controlplane.infrastructure.adapter.outbound.entity.PermissionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/** JPA repository interface for PermissionEntity persistence operations. */
@Repository
interface JpaPermissionRepository : JpaRepository<PermissionEntity, UUID> {
    /** Find a permission by its unique name. */
    fun findByName(name: String): PermissionEntity?

    /** Find permissions by resource type. */
    fun findByResource(resource: String): List<PermissionEntity>

    /** Find permissions by resource and action. */
    fun findByResourceAndAction(
        resource: String,
        action: String,
    ): List<PermissionEntity>

    /** Find permissions by a list of names. */
    fun findByNameIn(names: Collection<String>): List<PermissionEntity>

    /** Check if a permission exists by name. */
    fun existsByName(name: String): Boolean

    /** Find all permissions ordered by resource and action. */
    @Query("SELECT p FROM PermissionEntity p ORDER BY p.resource, p.action, p.name")
    fun findAllOrderedByResourceAndAction(): List<PermissionEntity>

    /** Find permissions by resource pattern (using LIKE). */
    @Query(
        "SELECT p FROM PermissionEntity p WHERE p.resource LIKE :resourcePattern ORDER BY p.resource, p.action",
    )
    fun findByResourcePattern(
        @Param("resourcePattern") resourcePattern: String,
    ): List<PermissionEntity>
}

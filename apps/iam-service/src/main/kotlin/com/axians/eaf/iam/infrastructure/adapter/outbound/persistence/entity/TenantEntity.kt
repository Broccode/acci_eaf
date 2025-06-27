package com.axians.eaf.iam.infrastructure.adapter.outbound.persistence.entity

import com.axians.eaf.iam.domain.model.Tenant
import com.axians.eaf.iam.domain.model.TenantStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "tenants")
data class TenantEntity(
    @Id @Column(name = "tenant_id", nullable = false, unique = true) val tenantId: String,
    @Column(name = "name", nullable = false, length = 255, unique = true) val name: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    val status: TenantStatus,
    @Column(name = "created_at", nullable = false) val createdAt: Instant,
    @Column(name = "updated_at", nullable = false) val updatedAt: Instant,
)

fun TenantEntity.toDomain(): Tenant =
    Tenant(
        tenantId = this.tenantId,
        name = this.name,
        status = this.status,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )

fun Tenant.toEntity(): TenantEntity =
    TenantEntity(
        tenantId = this.tenantId,
        name = this.name,
        status = this.status,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )

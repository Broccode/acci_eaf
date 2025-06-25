package com.axians.eaf.controlplane.domain.service

import com.axians.eaf.controlplane.application.dto.tenant.PagedResponse
import com.axians.eaf.controlplane.application.dto.tenant.TenantFilter
import com.axians.eaf.controlplane.application.dto.tenant.TenantSummary
import com.axians.eaf.controlplane.domain.model.tenant.Tenant
import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.tenant.TenantSettings
import com.axians.eaf.controlplane.domain.model.tenant.TenantStatus
import com.axians.eaf.controlplane.domain.port.TenantRepository
import java.time.Instant

/**
 * Domain service for managing tenants. It orchestrates all tenant-related operations, ensuring that
 * business rules are enforced.
 */
class TenantService(
    private val tenantRepository: TenantRepository,
) {
    /** Creates a new tenant with the specified details. */
    suspend fun createTenant(
        name: String,
        adminEmail: String,
        settings: TenantSettings,
    ): CreateTenantResult {
        // Check if tenant name already exists
        if (tenantRepository.existsByName(name)) {
            return CreateTenantResult.failure("Tenant with name '$name' already exists")
        }

        // Create new tenant
        val tenant =
            Tenant.create(
                name = name,
                settings = settings,
            )

        // Save tenant
        val savedTenant = tenantRepository.save(tenant)

        return CreateTenantResult.success(
            tenantId = savedTenant.id.value,
            tenantName = savedTenant.name,
            adminEmail = adminEmail,
        )
    }

    /** Retrieves detailed tenant information including user statistics. */
    suspend fun getTenantDetails(tenantId: String): TenantDetailsResult {
        val tenant =
            tenantRepository.findById(TenantId.fromString(tenantId))
                ?: return TenantDetailsResult.notFound("Tenant not found: $tenantId")

        val userCount = tenantRepository.countUsersByTenantId(tenant.id)
        val activeUsers = tenantRepository.countActiveUsersByTenantId(tenant.id)
        val lastActivity = tenantRepository.getLastUserActivity(tenant.id)

        val details =
            TenantDetails(
                tenant = tenant,
                userCount = userCount,
                activeUsers = activeUsers,
                lastActivity = lastActivity,
            )

        return TenantDetailsResult.success(details)
    }

    /** Updates an existing tenant's details. */
    suspend fun updateTenant(
        tenantId: String,
        name: String,
        settings: TenantSettings,
    ): UpdateTenantResult {
        val tenant =
            tenantRepository.findById(TenantId.fromString(tenantId))
                ?: return UpdateTenantResult.notFound("Tenant not found: $tenantId")

        // Check if the new name conflicts with another tenant
        if (name != tenant.name && tenantRepository.existsByName(name)) {
            return UpdateTenantResult.failure("Tenant with name '$name' already exists")
        }

        // Update tenant details
        val updatedTenant = tenant.updateDetails(name, settings)
        val savedTenant = tenantRepository.save(updatedTenant)

        return UpdateTenantResult.success(savedTenant)
    }

    /** Suspends a tenant, preventing user access. */
    suspend fun suspendTenant(
        tenantId: String,
        reason: String? = null,
    ): TenantOperationResult {
        val tenant =
            tenantRepository.findById(TenantId.fromString(tenantId))
                ?: return TenantOperationResult.notFound("Tenant not found: $tenantId")

        if (tenant.status != TenantStatus.ACTIVE) {
            return TenantOperationResult.failure(
                "Can only suspend active tenants. Current status: ${tenant.status}",
            )
        }

        val suspendedTenant = tenant.suspend()
        tenantRepository.save(suspendedTenant)

        return TenantOperationResult.success("Tenant suspended successfully")
    }

    /** Reactivates a suspended tenant. */
    suspend fun reactivateTenant(tenantId: String): TenantOperationResult {
        val tenant =
            tenantRepository.findById(TenantId.fromString(tenantId))
                ?: return TenantOperationResult.notFound("Tenant not found: $tenantId")

        if (tenant.status != TenantStatus.SUSPENDED) {
            return TenantOperationResult.failure(
                "Can only reactivate suspended tenants. Current status: ${tenant.status}",
            )
        }

        val reactivatedTenant = tenant.reactivate()
        tenantRepository.save(reactivatedTenant)

        return TenantOperationResult.success("Tenant reactivated successfully")
    }

    /** Archives a tenant permanently. */
    suspend fun archiveTenant(
        tenantId: String,
        reason: String? = null,
    ): ArchiveTenantResult {
        val tenant =
            tenantRepository.findById(TenantId.fromString(tenantId))
                ?: return ArchiveTenantResult.notFound("Tenant not found: $tenantId")

        if (tenant.status == TenantStatus.ARCHIVED) {
            return ArchiveTenantResult.failure("Tenant is already archived")
        }

        val archivedTenant = tenant.archive()
        tenantRepository.save(archivedTenant)

        return ArchiveTenantResult.success(
            tenantId = tenantId,
            archivedAt = archivedTenant.archivedAt!!,
        )
    }

    /** Lists tenants with filtering and pagination. */
    suspend fun listTenants(filter: TenantFilter): PagedResponse<TenantSummary> = tenantRepository.findAll(filter)

    /** Checks if a tenant exists and is operational. */
    suspend fun isTenantOperational(tenantId: String): Boolean {
        val tenant = tenantRepository.findById(TenantId.fromString(tenantId))
        return tenant?.isOperational() ?: false
    }
}

data class TenantDetails(
    val tenant: Tenant,
    val userCount: Int,
    val activeUsers: Int,
    val lastActivity: Instant?,
)

/** Result types for tenant operations. */
sealed class CreateTenantResult {
    data class Success(
        val tenantId: String,
        val tenantName: String,
        val adminEmail: String,
    ) : CreateTenantResult()

    data class Failure(
        val message: String,
    ) : CreateTenantResult()

    companion object {
        fun success(
            tenantId: String,
            tenantName: String,
            adminEmail: String,
        ): CreateTenantResult = Success(tenantId, tenantName, adminEmail)

        fun failure(message: String): CreateTenantResult = Failure(message)
    }
}

sealed class TenantDetailsResult {
    data class Success(
        val details: TenantDetails,
    ) : TenantDetailsResult()

    data class NotFound(
        val message: String,
    ) : TenantDetailsResult()

    companion object {
        fun success(details: TenantDetails): TenantDetailsResult = Success(details)

        fun notFound(message: String): TenantDetailsResult = NotFound(message)
    }
}

sealed class UpdateTenantResult {
    data class Success(
        val tenant: Tenant,
    ) : UpdateTenantResult()

    data class NotFound(
        val message: String,
    ) : UpdateTenantResult()

    data class Failure(
        val message: String,
    ) : UpdateTenantResult()

    companion object {
        fun success(tenant: Tenant): UpdateTenantResult = Success(tenant)

        fun notFound(message: String): UpdateTenantResult = NotFound(message)

        fun failure(message: String): UpdateTenantResult = Failure(message)
    }
}

sealed class TenantOperationResult {
    data class Success(
        val message: String,
    ) : TenantOperationResult()

    data class NotFound(
        val message: String,
    ) : TenantOperationResult()

    data class Failure(
        val message: String,
    ) : TenantOperationResult()

    companion object {
        fun success(message: String): TenantOperationResult = Success(message)

        fun notFound(message: String): TenantOperationResult = NotFound(message)

        fun failure(message: String): TenantOperationResult = Failure(message)
    }
}

sealed class ArchiveTenantResult {
    data class Success(
        val tenantId: String,
        val archivedAt: Instant,
    ) : ArchiveTenantResult()

    data class NotFound(
        val message: String,
    ) : ArchiveTenantResult()

    data class Failure(
        val message: String,
    ) : ArchiveTenantResult()

    companion object {
        fun success(
            tenantId: String,
            archivedAt: Instant,
        ): ArchiveTenantResult = Success(tenantId, archivedAt)

        fun notFound(message: String): ArchiveTenantResult = NotFound(message)

        fun failure(message: String): ArchiveTenantResult = Failure(message)
    }
}

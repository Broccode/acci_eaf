package com.axians.eaf.controlplane.infrastructure.adapter.input

import com.axians.eaf.controlplane.application.dto.tenant.ArchiveTenantResponse
import com.axians.eaf.controlplane.application.dto.tenant.CreateTenantRequest
import com.axians.eaf.controlplane.application.dto.tenant.CreateTenantResponse
import com.axians.eaf.controlplane.application.dto.tenant.PagedResponse
import com.axians.eaf.controlplane.application.dto.tenant.TenantDetailsResponse
import com.axians.eaf.controlplane.application.dto.tenant.TenantDto
import com.axians.eaf.controlplane.application.dto.tenant.TenantFilter
import com.axians.eaf.controlplane.application.dto.tenant.TenantSummary
import com.axians.eaf.controlplane.application.dto.tenant.UpdateTenantRequest
import com.axians.eaf.controlplane.domain.service.ArchiveTenantResult
import com.axians.eaf.controlplane.domain.service.CreateTenantResult
import com.axians.eaf.controlplane.domain.service.TenantDetailsResult
import com.axians.eaf.controlplane.domain.service.TenantOperationResult
import com.axians.eaf.controlplane.domain.service.TenantService
import com.axians.eaf.controlplane.domain.service.UpdateTenantResult
import com.axians.eaf.controlplane.infrastructure.security.annotation.RequiresTenantAccess
import com.vaadin.flow.server.auth.AnonymousAllowed
import jakarta.annotation.security.RolesAllowed
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.validation.annotation.Validated

/**
 * Hilla endpoint for tenant management operations. Provides type-safe access to tenant lifecycle
 * and configuration management.
 *
 * FIXME: Temporarily disabled due to KotlinNullabilityPlugin crash in Vaadin 24.8.0 See:
 * https://github.com/vaadin/hilla/issues/3443 Remove comment from @Endpoint when Vaadin/Hilla ships
 * the fix.
 */

// @Endpoint
@Service
@Validated
// @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN")
class TenantManagementEndpoint(
        private val tenantService: TenantService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TenantManagementEndpoint::class.java)
    }

    /** Creates a new tenant with the specified configuration. */
    fun createTenant(
            @Valid request: CreateTenantRequest,
    ): CreateTenantResponse {
        logger.info("Creating tenant: {}", request.name)

        return runBlocking {
            try {
                val result =
                        tenantService.createTenant(
                                name = request.name,
                                adminEmail = request.adminEmail,
                                settings = request.settings.toDomain(),
                        )

                when (result) {
                    is CreateTenantResult.Success -> {
                        logger.info("Tenant created successfully: {}", result.tenantId)
                        CreateTenantResponse.success(
                                tenantId = result.tenantId,
                                name = result.tenantName,
                                adminUserId = null, // Will be set when user service is implemented
                        )
                    }
                    is CreateTenantResult.Failure -> {
                        logger.warn("Failed to create tenant: {}", result.message)
                        CreateTenantResponse.failure(result.message)
                    }
                }
            } catch (exception: Exception) {
                logger.error("Error creating tenant: ${request.name}", exception)
                CreateTenantResponse.failure("Failed to create tenant: ${exception.message}")
            }
        }
    }

    /** Retrieves detailed information about a specific tenant. */
    @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN", "TENANT_ADMIN")
    @RequiresTenantAccess
    fun getTenant(tenantId: String): TenantDetailsResponse? {
        logger.debug("Retrieving tenant details: {}", tenantId)

        return runBlocking {
            try {
                when (val result = tenantService.getTenantDetails(tenantId)) {
                    is TenantDetailsResult.Success -> {
                        logger.debug("Tenant details retrieved: {}", tenantId)
                        TenantDetailsResponse(
                                tenant = TenantDto.fromDomain(result.details.tenant),
                                userCount = result.details.userCount,
                                activeUsers = result.details.activeUsers,
                                lastActivity = result.details.lastActivity,
                        )
                    }
                    is TenantDetailsResult.NotFound -> {
                        logger.warn("Tenant not found: {}", tenantId)
                        null
                    }
                }
            } catch (exception: Exception) {
                logger.error("Error retrieving tenant: $tenantId", exception)
                null
            }
        }
    }

    /** Updates an existing tenant's details. */
    @RequiresTenantAccess
    fun updateTenant(
            tenantId: String,
            @Valid request: UpdateTenantRequest,
    ): TenantDetailsResponse? {
        logger.info("Updating tenant: {}", tenantId)

        return runBlocking {
            try {
                val result =
                        tenantService.updateTenant(
                                tenantId = tenantId,
                                name = request.name,
                                settings = request.settings.toDomain(),
                        )

                when (result) {
                    is UpdateTenantResult.Success -> {
                        logger.info("Tenant updated successfully: {}", tenantId)
                        // After updating, fetch the full details to return a consistent response
                        getTenant(tenantId)
                    }
                    is UpdateTenantResult.NotFound -> {
                        logger.warn("Tenant not found for update: {}", tenantId)
                        null
                    }
                    is UpdateTenantResult.Failure -> {
                        logger.warn("Failed to update tenant {}: {}", tenantId, result.message)
                        null
                    }
                }
            } catch (exception: Exception) {
                logger.error("Error updating tenant: $tenantId", exception)
                null
            }
        }
    }

    /** Suspends a tenant, preventing user access. */
    @RequiresTenantAccess
    fun suspendTenant(
            tenantId: String,
            reason: String?,
    ): Boolean {
        logger.info("Suspending tenant: {} with reason: {}", tenantId, reason)

        return runBlocking {
            try {
                val result = tenantService.suspendTenant(tenantId, reason)

                when (result) {
                    is TenantOperationResult.Success -> {
                        logger.info("Tenant suspended successfully: {}", tenantId)
                        true
                    }
                    is TenantOperationResult.NotFound, is TenantOperationResult.Failure -> {
                        logger.warn(
                                "Failed to suspend tenant {}: {}",
                                tenantId,
                                when (result) {
                                    is TenantOperationResult.NotFound -> result.message
                                    is TenantOperationResult.Failure -> result.message
                                    else -> "Unknown error"
                                },
                        )
                        false
                    }
                }
            } catch (exception: Exception) {
                logger.error("Error suspending tenant: $tenantId", exception)
                false
            }
        }
    }

    /** Reactivates a suspended tenant. */
    fun reactivateTenant(tenantId: String): Boolean {
        logger.info("Reactivating tenant: {}", tenantId)

        return runBlocking {
            try {
                val result = tenantService.reactivateTenant(tenantId)

                when (result) {
                    is TenantOperationResult.Success -> {
                        logger.info("Tenant reactivated successfully: {}", tenantId)
                        true
                    }
                    is TenantOperationResult.NotFound, is TenantOperationResult.Failure -> {
                        logger.warn(
                                "Failed to reactivate tenant {}: {}",
                                tenantId,
                                when (result) {
                                    is TenantOperationResult.NotFound -> result.message
                                    is TenantOperationResult.Failure -> result.message
                                    else -> "Unknown error"
                                },
                        )
                        false
                    }
                }
            } catch (exception: Exception) {
                logger.error("Error reactivating tenant: $tenantId", exception)
                false
            }
        }
    }

    /** Archives a tenant permanently. */
    @RolesAllowed("SUPER_ADMIN") // Only super admin can archive tenants
    fun archiveTenant(
            tenantId: String,
            reason: String?,
    ): ArchiveTenantResponse? {
        logger.info("Archiving tenant: {} with reason: {}", tenantId, reason)

        return runBlocking {
            try {
                val result = tenantService.archiveTenant(tenantId, reason)

                when (result) {
                    is ArchiveTenantResult.Success -> {
                        logger.info("Tenant archived successfully: {}", tenantId)
                        ArchiveTenantResponse.success(result.tenantId, result.archivedAt)
                    }
                    is ArchiveTenantResult.NotFound, is ArchiveTenantResult.Failure -> {
                        logger.warn(
                                "Failed to archive tenant {}: {}",
                                tenantId,
                                when (result) {
                                    is ArchiveTenantResult.NotFound -> result.message
                                    is ArchiveTenantResult.Failure -> result.message
                                    else -> "Unknown error"
                                },
                        )
                        null
                    }
                }
            } catch (exception: Exception) {
                logger.error("Error archiving tenant: $tenantId", exception)
                null
            }
        }
    }

    /** Lists all tenants with optional filtering and pagination. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun listTenants(filter: TenantFilter): PagedResponse<TenantSummary> {
        logger.debug("Listing tenants with filter: {}", filter)

        return runBlocking {
            try {
                val result = tenantService.listTenants(filter)
                logger.debug("Retrieved {} tenants", result.content.size)
                result
            } catch (exception: Exception) {
                logger.error("Error listing tenants", exception)
                PagedResponse.of(emptyList(), filter.page, filter.size, 0)
            }
        }
    }

    /** Checks if a tenant is operational. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun isTenantOperational(tenantId: String): Boolean = runBlocking {
        try {
            tenantService.isTenantOperational(tenantId)
        } catch (exception: Exception) {
            logger.error("Error checking tenant operational status: $tenantId", exception)
            false
        }
    }
}

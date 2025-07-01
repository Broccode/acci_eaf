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
import com.axians.eaf.controlplane.infrastructure.adapter.input.common.EndpointExceptionHandler
import com.axians.eaf.controlplane.infrastructure.security.annotation.RequiresTenantAccess
import com.axians.eaf.core.annotations.HillaWorkaround
import com.vaadin.flow.server.auth.AnonymousAllowed
import com.vaadin.hilla.Endpoint
import jakarta.annotation.security.RolesAllowed
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated

@Endpoint
@RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN")
@HillaWorkaround(
    description =
        "Endpoint was preemptively disabled due to Hilla issue #3443, but analysis shows no DTOs were affected. Re-enabling and marking for audit.",
)
/**
 * Hilla endpoint for tenant management operations. Provides type-safe access to tenant lifecycle
 * and configuration management.
 */
@Validated
class TenantManagementEndpoint(
    private val tenantService: TenantService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TenantManagementEndpoint::class.java)
    }

    /** Creates a new tenant with the specified configuration. */
    fun createTenant(
        @Valid request: CreateTenantRequest,
    ): CreateTenantResponse =
        EndpointExceptionHandler.handleEndpointExecution("createTenant", logger) {
            logger.info("Creating tenant: {}", request.name)

            runBlocking {
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
                            adminUserId =
                            null, // Will be set when user service is implemented
                        )
                    }
                    is CreateTenantResult.Failure -> {
                        logger.warn("Failed to create tenant: {}", result.message)
                        CreateTenantResponse.failure(result.message)
                    }
                }
            }
        }

    /** Retrieves detailed information about a specific tenant. */
    @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN", "TENANT_ADMIN")
    @RequiresTenantAccess
    fun getTenant(tenantId: String): TenantDetailsResponse? =
        EndpointExceptionHandler.handleEndpointExecution("getTenant", logger) {
            logger.debug("Retrieving tenant details: {}", tenantId)

            runBlocking {
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
            }
        }

    /** Updates an existing tenant's details. */
    @RequiresTenantAccess
    fun updateTenant(
        tenantId: String,
        @Valid request: UpdateTenantRequest,
    ): TenantDetailsResponse? =
        EndpointExceptionHandler.handleEndpointExecution("updateTenant", logger) {
            logger.info("Updating tenant: {}", tenantId)

            runBlocking {
                val result =
                    tenantService.updateTenant(
                        tenantId = tenantId,
                        name = request.name,
                        settings = request.settings.toDomain(),
                    )

                when (result) {
                    is UpdateTenantResult.Success -> {
                        logger.info("Tenant updated successfully: {}", tenantId)
                        // After updating, fetch the full details to return a consistent
                        // response
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
            }
        }

    /** Suspends a tenant, preventing user access. */
    @RequiresTenantAccess
    fun suspendTenant(
        tenantId: String,
        reason: String?,
    ): Boolean =
        EndpointExceptionHandler.handleEndpointExecution("suspendTenant", logger) {
            logger.info("Suspending tenant: {} with reason: {}", tenantId, reason)

            runBlocking {
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
            }
        }

    /** Reactivates a suspended tenant. */
    fun reactivateTenant(tenantId: String): Boolean =
        EndpointExceptionHandler.handleEndpointExecution("reactivateTenant", logger) {
            logger.info("Reactivating tenant: {}", tenantId)

            runBlocking {
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
            }
        }

    /** Archives a tenant permanently. */
    @RolesAllowed("SUPER_ADMIN") // Only super admin can archive tenants
    fun archiveTenant(
        tenantId: String,
        reason: String?,
    ): ArchiveTenantResponse? =
        EndpointExceptionHandler.handleEndpointExecution("archiveTenant", logger) {
            logger.info("Archiving tenant: {} with reason: {}", tenantId, reason)

            runBlocking {
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
            }
        }

    /** Lists all tenants with optional filtering and pagination. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun listTenants(filter: TenantFilter): PagedResponse<TenantSummary> =
        EndpointExceptionHandler.handleEndpointExecution("listTenants", logger) {
            logger.debug("Listing tenants with filter: {}", filter)

            runBlocking {
                val result = tenantService.listTenants(filter)
                logger.debug("Retrieved {} tenants", result.content.size)
                result
            }
        }

    /** Checks if a tenant is operational. */
    @AnonymousAllowed // Temporarily for testing - will be secured later
    fun isTenantOperational(tenantId: String): Boolean =
        EndpointExceptionHandler.handleEndpointExecution("isTenantOperational", logger) {
            runBlocking { tenantService.isTenantOperational(tenantId) }
        }
}

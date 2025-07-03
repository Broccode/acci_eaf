package com.axians.eaf.controlplane.infrastructure.adapter.outbound.repository

import com.axians.eaf.controlplane.application.dto.tenant.PagedResponse
import com.axians.eaf.controlplane.application.dto.tenant.TenantFilter
import com.axians.eaf.controlplane.application.dto.tenant.TenantSummary
import com.axians.eaf.controlplane.domain.model.tenant.Tenant
import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.tenant.TenantSettings
import com.axians.eaf.controlplane.domain.model.tenant.TenantStatus
import com.axians.eaf.controlplane.domain.port.TenantRepository
import com.axians.eaf.iam.client.CreateTenantRequest
import com.axians.eaf.iam.client.IamServiceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.io.IOException
import java.time.Instant

/**
 * TenantRepository implementation that delegates to the IAM service. This follows the microservices
 * architecture where the Control Plane acts as an administrative interface and the IAM service owns
 * tenant data.
 */
@Repository
class IamServiceTenantRepository(
    private val iamServiceClient: IamServiceClient,
) : TenantRepository {
    private val logger = LoggerFactory.getLogger(IamServiceTenantRepository::class.java)

    override suspend fun save(tenant: Tenant): Tenant =
        withContext(Dispatchers.IO) {
            try {
                // Try to get existing tenant first to determine if this is create or update
                val existingTenant = findById(tenant.id)

                if (existingTenant == null) {
                    // Create new tenant via IAM service
                    val request =
                        CreateTenantRequest(
                            tenantName = tenant.name,
                            initialAdminEmail =
                                "admin@${tenant.name.lowercase().replace(" ", "")}.com",
                        )
                    val response = iamServiceClient.createTenant(request)

                    // Convert response back to domain model
                    tenant.copy(
                        id = TenantId.fromString(response.tenantId),
                        lastModified = Instant.now(),
                    )
                } else {
                    // Update existing tenant - would need additional IAM service API
                    // For now, return the tenant as-is
                    tenant
                }
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                // If there's an error, it might be a new tenant creation
                // Try to create it via IAM service
                logger.warn(
                    "Failed to find existing tenant, attempting creation: {}",
                    e.message,
                )
                try {
                    val request =
                        CreateTenantRequest(
                            tenantName = tenant.name,
                            initialAdminEmail =
                                "admin@${tenant.name.lowercase().replace(" ", "")}.com",
                        )
                    val response = iamServiceClient.createTenant(request)

                    // Convert response back to domain model
                    tenant.copy(
                        id = TenantId.fromString(response.tenantId),
                        lastModified = Instant.now(),
                    )
                } catch (
                    @Suppress("TooGenericExceptionCaught") createException: Exception,
                ) {
                    logger.error(
                        "Failed to create tenant after initial find failed. Original error will be thrown.",
                        createException,
                    )
                    // If creation also fails, throw the original exception
                    throw e
                }
            }
        }

    override suspend fun findById(tenantId: TenantId): Tenant? =
        withContext(Dispatchers.IO) {
            try {
                val response = iamServiceClient.getTenant(tenantId.value)
                response?.let { convertToTenant(it) }
            } catch (e: IOException) {
                logger.warn("Failed to get tenant {} from IAM service: {}", tenantId, e.message)
                null
            } catch (e: Exception) {
                logger.error("Unexpected error getting tenant {} from IAM service", tenantId, e)
                null
            }
        }

    override suspend fun findByName(name: String): Tenant? =
        withContext(Dispatchers.IO) {
            // Would need additional IAM service API for findByName
            // For now, not implemented
            null
        }

    override suspend fun existsById(tenantId: TenantId): Boolean =
        withContext(Dispatchers.IO) { findById(tenantId) != null }

    override suspend fun existsByName(name: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                findByName(name) != null
            } catch (e: IOException) {
                logger.warn(
                    "Failed to check existence of tenant {} from IAM service: {}",
                    name,
                    e.message,
                )
                // If we can't check, assume it doesn't exist to allow creation to proceed
                false
            } catch (e: Exception) {
                logger.error(
                    "Unexpected error checking existence of tenant {} from IAM service",
                    name,
                    e,
                )
                // If we can't check, assume it doesn't exist to allow creation to proceed
                false
            }
        }

    override suspend fun findAll(filter: TenantFilter): PagedResponse<TenantSummary> =
        withContext(Dispatchers.IO) {
            // Would need additional IAM service API for listing tenants with filters
            // For now, return empty response
            PagedResponse(
                content = emptyList(),
                page = filter.page,
                size = filter.size,
                totalElements = 0,
                totalPages = 0,
            )
        }

    override suspend fun countUsersByTenantId(tenantId: TenantId): Int =
        withContext(Dispatchers.IO) {
            try {
                val response = iamServiceClient.listUsers(tenantId.value)
                response.users.size
            } catch (e: IOException) {
                logger.warn(
                    "Failed to count users for tenant {} from IAM service: {}",
                    tenantId,
                    e.message,
                )
                0
            } catch (e: Exception) {
                logger.error(
                    "Unexpected error counting users for tenant {} from IAM service",
                    tenantId,
                    e,
                )
                0
            }
        }

    override suspend fun countActiveUsersByTenantId(tenantId: TenantId): Int =
        withContext(Dispatchers.IO) {
            try {
                val response = iamServiceClient.listUsers(tenantId.value)
                response.users.count { it.status == "ACTIVE" }
            } catch (e: IOException) {
                logger.warn(
                    "Failed to count active users for tenant {} from IAM service: {}",
                    tenantId,
                    e.message,
                )
                0
            } catch (e: Exception) {
                logger.error(
                    "Unexpected error counting active users for tenant {} from IAM service",
                    tenantId,
                    e,
                )
                0
            }
        }

    override suspend fun getLastUserActivity(tenantId: TenantId): Instant? =
        withContext(Dispatchers.IO) {
            // Would need additional IAM service API for user activity tracking
            // For now, return null
            null
        }

    override suspend fun deleteById(tenantId: TenantId): Boolean =
        withContext(Dispatchers.IO) {
            // Would need additional IAM service API for tenant deletion
            // For now, not implemented
            false
        }

    private fun convertToTenant(response: com.axians.eaf.iam.client.TenantResponse): Tenant =
        Tenant(
            id = TenantId.fromString(response.tenantId),
            name = response.tenantName,
            status = TenantStatus.valueOf(response.status),
            settings =
                TenantSettings.default(
                    "example.com",
                ), // Default settings since IAM service doesn't provide
            // detailed settings yet
            createdAt = Instant.now(), // TODO: Parse createdAt from IAM service response
            lastModified = Instant.now(),
            archivedAt = null,
        )
}

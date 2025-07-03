package com.axians.eaf.controlplane.infrastructure.adapter.input

import com.axians.eaf.controlplane.application.dto.tenant.CreateTenantRequest
import com.axians.eaf.controlplane.application.dto.tenant.PagedResponse
import com.axians.eaf.controlplane.application.dto.tenant.TenantFilter
import com.axians.eaf.controlplane.application.dto.tenant.TenantSettingsDto
import com.axians.eaf.controlplane.application.dto.tenant.TenantSummary
import com.axians.eaf.controlplane.application.dto.tenant.UpdateTenantRequest
import com.axians.eaf.controlplane.domain.model.tenant.Tenant
import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.tenant.TenantStatus
import com.axians.eaf.controlplane.domain.service.ArchiveTenantResult
import com.axians.eaf.controlplane.domain.service.CreateTenantResult
import com.axians.eaf.controlplane.domain.service.TenantDetails
import com.axians.eaf.controlplane.domain.service.TenantDetailsResult
import com.axians.eaf.controlplane.domain.service.TenantOperationResult
import com.axians.eaf.controlplane.domain.service.TenantService
import com.axians.eaf.controlplane.domain.service.UpdateTenantResult
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class TenantManagementEndpointTest {
    private val tenantService = mockk<TenantService>()
    private val endpoint = TenantManagementEndpoint(tenantService)

    private val validSettingsDto =
        TenantSettingsDto(
            maxUsers = 100,
            allowedDomains = listOf("example.com"),
            features = listOf("basic-features"),
        )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `should create tenant successfully via endpoint`() =
        runTest {
            // Given
            val request =
                CreateTenantRequest(
                    name = "Test Corporation",
                    adminEmail = "admin@test.com",
                    settings = validSettingsDto,
                    notifyAdmin = true,
                )

            val serviceResult =
                CreateTenantResult.success(
                    tenantId = "tenant-123",
                    tenantName = "Test Corporation",
                    adminEmail = "admin@test.com",
                )

            coEvery {
                tenantService.createTenant(
                    name = request.name,
                    adminEmail = request.adminEmail,
                    settings = any(),
                )
            } returns serviceResult

            // When
            val response = endpoint.createTenant(request)

            // Then
            assertThat(response.tenantId).isEqualTo("tenant-123")
            assertThat(response.name).isEqualTo("Test Corporation")
            assertThat(response.message).contains("Tenant created successfully")

            coVerify {
                tenantService.createTenant(
                    name = "Test Corporation",
                    adminEmail = "admin@test.com",
                    settings = any(),
                )
            }
        }

    @Test
    fun `should handle tenant creation failure via endpoint`() =
        runTest {
            // Given
            val request =
                CreateTenantRequest(
                    name = "Duplicate Name",
                    adminEmail = "admin@duplicate.com",
                    settings = validSettingsDto,
                )

            val serviceResult =
                CreateTenantResult.failure(
                    "Tenant with name 'Duplicate Name' already exists",
                )

            coEvery { tenantService.createTenant(any(), any(), any()) } returns serviceResult

            // When
            val response = endpoint.createTenant(request)

            // Then
            assertThat(response.tenantId).isEmpty()
            assertThat(response.name).isEmpty()
            assertThat(response.message)
                .contains("Tenant with name 'Duplicate Name' already exists")
        }

    @Test
    fun `should get tenant details successfully via endpoint`() =
        runTest {
            // Given
            val tenantId = "550e8400-e29b-41d4-a716-446655440001"
            val now = Instant.now()
            val tenant =
                Tenant(
                    id = TenantId(tenantId),
                    name = "Retrieved Tenant",
                    status = TenantStatus.ACTIVE,
                    settings = validSettingsDto.toDomain(),
                    createdAt = now,
                    lastModified = now,
                )
            val tenantDetails =
                TenantDetails(
                    tenant = tenant,
                    userCount = 25,
                    activeUsers = 20,
                    lastActivity = now.minusSeconds(3600),
                )

            val serviceResult = TenantDetailsResult.success(tenantDetails)

            coEvery { tenantService.getTenantDetails(tenantId) } returns serviceResult

            // When
            val response = endpoint.getTenant(tenantId)

            // Then
            assertThat(response).isNotNull
            assertThat(response!!.tenant.id).isEqualTo(tenantId)
            assertThat(response.tenant.name).isEqualTo("Retrieved Tenant")
            assertThat(response.userCount).isEqualTo(25)
            assertThat(response.activeUsers).isEqualTo(20)

            coVerify { tenantService.getTenantDetails(tenantId) }
        }

    @Test
    fun `should return null when tenant not found via endpoint`() =
        runTest {
            // Given
            val tenantId = "non-existent"
            val serviceResult = TenantDetailsResult.notFound("Tenant not found: $tenantId")

            coEvery { tenantService.getTenantDetails(tenantId) } returns serviceResult

            // When
            val response = endpoint.getTenant(tenantId)

            // Then
            assertThat(response).isNull()

            coVerify { tenantService.getTenantDetails(tenantId) }
        }

    @Test
    fun `should update tenant successfully via endpoint`() =
        runTest {
            // Given
            val tenantId = "550e8400-e29b-41d4-a716-446655440002"
            val now = Instant.now()
            val updateRequest =
                UpdateTenantRequest(
                    name = "Updated Name",
                    settings =
                        TenantSettingsDto(
                            maxUsers = 200,
                            allowedDomains = listOf("updated.com"),
                            features = listOf("advanced-features"),
                        ),
                )

            val updatedTenant =
                Tenant(
                    id = TenantId(tenantId),
                    name = "Updated Name",
                    status = TenantStatus.ACTIVE,
                    settings = updateRequest.settings.toDomain(),
                    createdAt = now,
                    lastModified = now,
                )

            val serviceResult = UpdateTenantResult.success(updatedTenant)

            // Mock both update and subsequent get calls
            coEvery {
                tenantService.updateTenant(
                    tenantId = tenantId,
                    name = updateRequest.name,
                    settings = any(),
                )
            } returns serviceResult

            val details =
                TenantDetails(
                    tenant = updatedTenant,
                    userCount = 30,
                    activeUsers = 25,
                    lastActivity = now,
                )
            coEvery { tenantService.getTenantDetails(tenantId) } returns
                TenantDetailsResult.success(details)

            // When
            val response = endpoint.updateTenant(tenantId, updateRequest)

            // Then
            assertThat(response).isNotNull
            assertThat(response!!.tenant.name).isEqualTo("Updated Name")
            assertThat(response.tenant.settings.maxUsers).isEqualTo(200)

            coVerify { tenantService.updateTenant(tenantId, "Updated Name", any()) }
            coVerify { tenantService.getTenantDetails(tenantId) }
        }

    @Test
    fun `should return null when updating non-existent tenant via endpoint`() =
        runTest {
            // Given
            val tenantId = "non-existent"
            val updateRequest =
                UpdateTenantRequest(
                    name = "Updated Name",
                    settings = validSettingsDto,
                )

            val serviceResult = UpdateTenantResult.notFound("Tenant not found: $tenantId")

            coEvery { tenantService.updateTenant(any(), any(), any()) } returns serviceResult

            // When
            val response = endpoint.updateTenant(tenantId, updateRequest)

            // Then
            assertThat(response).isNull()

            coVerify { tenantService.updateTenant(tenantId, "Updated Name", any()) }
        }

    @Test
    fun `should suspend tenant successfully via endpoint`() =
        runTest {
            // Given
            val tenantId = "tenant-suspend"
            val reason = "Policy violation"
            val serviceResult = TenantOperationResult.success("Tenant suspended successfully")

            coEvery { tenantService.suspendTenant(tenantId, reason) } returns serviceResult

            // When
            val result = endpoint.suspendTenant(tenantId, reason)

            // Then
            assertThat(result).isTrue()

            coVerify { tenantService.suspendTenant(tenantId, reason) }
        }

    @Test
    fun `should return false when suspending tenant fails via endpoint`() =
        runTest {
            // Given
            val tenantId = "tenant-fail"
            val serviceResult = TenantOperationResult.failure("Can only suspend active tenants")

            coEvery { tenantService.suspendTenant(tenantId, null) } returns serviceResult

            // When
            val result = endpoint.suspendTenant(tenantId, null)

            // Then
            assertThat(result).isFalse()

            coVerify { tenantService.suspendTenant(tenantId, null) }
        }

    @Test
    fun `should reactivate tenant successfully via endpoint`() =
        runTest {
            // Given
            val tenantId = "tenant-reactivate"
            val serviceResult = TenantOperationResult.success("Tenant reactivated successfully")

            coEvery { tenantService.reactivateTenant(tenantId) } returns serviceResult

            // When
            val result = endpoint.reactivateTenant(tenantId)

            // Then
            assertThat(result).isTrue()

            coVerify { tenantService.reactivateTenant(tenantId) }
        }

    @Test
    fun `should archive tenant successfully via endpoint`() =
        runTest {
            // Given
            val tenantId = "tenant-archive"
            val reason = "End of contract"
            val archiveTime = Instant.now()
            val serviceResult = ArchiveTenantResult.success(tenantId, archiveTime)

            coEvery { tenantService.archiveTenant(tenantId, reason) } returns serviceResult

            // When
            val response = endpoint.archiveTenant(tenantId, reason)

            // Then
            assertThat(response).isNotNull
            assertThat(response!!.tenantId).isEqualTo(tenantId)
            assertThat(response.archivedAt).isEqualTo(archiveTime)
            assertThat(response.message).contains("Tenant archived successfully")

            coVerify { tenantService.archiveTenant(tenantId, reason) }
        }

    @Test
    fun `should return null when archiving tenant fails via endpoint`() =
        runTest {
            // Given
            val tenantId = "tenant-archive-fail"
            val serviceResult = ArchiveTenantResult.failure("Tenant is already archived")

            coEvery { tenantService.archiveTenant(tenantId, null) } returns serviceResult

            // When
            val response = endpoint.archiveTenant(tenantId, null)

            // Then
            assertThat(response).isNull()

            coVerify { tenantService.archiveTenant(tenantId, null) }
        }

    @Test
    fun `should list tenants with filtering via endpoint`() =
        runTest {
            // Given
            val filter =
                TenantFilter(
                    status = TenantStatus.ACTIVE,
                    namePattern = "test",
                    page = 0,
                    size = 10,
                )

            val tenantSummaries =
                listOf(
                    TenantSummary(
                        id = "tenant-1",
                        name = "Test Tenant 1",
                        status = TenantStatus.ACTIVE,
                        userCount = 15,
                        createdAt = Instant.now(),
                        lastActivity = Instant.now(),
                    ),
                    TenantSummary(
                        id = "tenant-2",
                        name = "Test Tenant 2",
                        status = TenantStatus.ACTIVE,
                        userCount = 25,
                        createdAt = Instant.now(),
                        lastActivity = Instant.now(),
                    ),
                )

            val expectedResponse = PagedResponse.of(tenantSummaries, 0, 10, 2)

            coEvery { tenantService.listTenants(filter) } returns expectedResponse

            // When
            val response = endpoint.listTenants(filter)

            // Then
            assertThat(response.content).hasSize(2)
            assertThat(response.content[0].name).isEqualTo("Test Tenant 1")
            assertThat(response.content[1].name).isEqualTo("Test Tenant 2")
            assertThat(response.totalElements).isEqualTo(2)
            assertThat(response.page).isEqualTo(0)
            assertThat(response.size).isEqualTo(10)

            coVerify { tenantService.listTenants(filter) }
        }

    @Test
    fun `should check tenant operational status via endpoint`() =
        runTest {
            // Given
            val operationalTenantId = "operational-tenant"
            val nonOperationalTenantId = "non-operational-tenant"

            coEvery { tenantService.isTenantOperational(operationalTenantId) } returns true
            coEvery { tenantService.isTenantOperational(nonOperationalTenantId) } returns false

            // When & Then
            assertThat(endpoint.isTenantOperational(operationalTenantId)).isTrue()
            assertThat(endpoint.isTenantOperational(nonOperationalTenantId)).isFalse()

            coVerify { tenantService.isTenantOperational(operationalTenantId) }
            coVerify { tenantService.isTenantOperational(nonOperationalTenantId) }
        }

    @Test
    fun `should handle service exceptions gracefully in endpoint`() =
        runTest {
            // Given
            val tenantId = "550e8400-e29b-41d4-a716-446655440003"
            coEvery { tenantService.getTenantDetails(tenantId) } throws
                RuntimeException("Database connection failed")

            // When & Then - Expect EndpointException to be thrown
            val exception =
                assertThrows<com.vaadin.hilla.exception.EndpointException> {
                    endpoint.getTenant(tenantId)
                }

            assertThat(exception.message)
                .contains("getTenant failed: Runtime error - Database connection failed")

            coVerify { tenantService.getTenantDetails(tenantId) }
        }
}

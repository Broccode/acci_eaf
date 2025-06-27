package com.axians.eaf.controlplane.domain.service

import com.axians.eaf.controlplane.application.dto.tenant.TenantFilter
import com.axians.eaf.controlplane.domain.model.tenant.Tenant
import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.tenant.TenantSettings
import com.axians.eaf.controlplane.domain.model.tenant.TenantStatus
import com.axians.eaf.controlplane.domain.port.TenantRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.eq
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TenantServiceTest {
    private val tenantRepository = mockk<TenantRepository>()
    private val tenantService = TenantService(tenantRepository)

    private val validSettings = TenantSettings.default("example.com")
    private val testTenantId = TenantId.generate()
    private val now = Instant.now()

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `should create tenant successfully when name is unique`() =
        runTest {
            // Given
            val tenantName = "Acme Corporation"
            val adminEmail = "admin@acme.com"

            coEvery { tenantRepository.existsByName(tenantName) } returns false
            val createdTenant = Tenant.create(tenantName, validSettings, now)
            coEvery { tenantRepository.save(any<Tenant>()) } returns createdTenant

            // When
            val result = tenantService.createTenant(tenantName, adminEmail, validSettings)

            // Then
            assertTrue(result is CreateTenantResult.Success)
            val success = result as CreateTenantResult.Success
            assertEquals(tenantName, success.tenantName)
            assertEquals(adminEmail, success.adminEmail)
            assertTrue(success.tenantId.isNotBlank())

            coVerify { tenantRepository.existsByName(tenantName) }
            coVerify { tenantRepository.save(any<Tenant>()) }
        }

    @Test
    fun `should fail to create tenant when name already exists`() =
        runTest {
            // Given
            val tenantName = "Existing Corporation"
            val adminEmail = "admin@existing.com"

            coEvery { tenantRepository.existsByName(tenantName) } returns true

            // When
            val result = tenantService.createTenant(tenantName, adminEmail, validSettings)

            // Then
            assertTrue(result is CreateTenantResult.Failure)
            val failure = result as CreateTenantResult.Failure
            assertContains(failure.message, "Tenant with name '$tenantName' already exists")

            coVerify { tenantRepository.existsByName(tenantName) }
            coVerify(exactly = 0) { tenantRepository.save(any<Tenant>()) }
        }

    @Test
    fun `should get tenant details successfully when tenant exists`() =
        runTest {
            // Given
            val tenant = Tenant.create("Test Tenant", validSettings, now)
            val userCount = 25
            val activeUsers = 20
            val lastActivity = now.minusSeconds(3600)

            coEvery { tenantRepository.findById(eq<TenantId>(tenant.id)) } returns tenant
            coEvery { tenantRepository.countUsersByTenantId(tenant.id) } returns userCount
            coEvery { tenantRepository.countActiveUsersByTenantId(tenant.id) } returns activeUsers
            coEvery { tenantRepository.getLastUserActivity(tenant.id) } returns lastActivity

            // When
            val result = tenantService.getTenantDetails(tenant.id.value)

            // Then
            assertTrue(result is TenantDetailsResult.Success)
            val success = result as TenantDetailsResult.Success
            assertEquals(tenant.id.value, success.details.tenant.id)
            assertEquals(tenant.name, success.details.tenant.name)
            assertEquals(userCount, success.details.userCount)
            assertEquals(activeUsers, success.details.activeUsers)
            assertEquals(lastActivity, success.details.lastActivity)
        }

    @Test
    fun `should return not found when getting details for non-existent tenant`() =
        runTest {
            // Given
            val tenantId = "non-existent-id"

            coEvery { tenantRepository.findById(eq<TenantId>(TenantId.fromString(tenantId))) } returns
                null

            // When
            val result = tenantService.getTenantDetails(tenantId)

            // Then
            assertTrue(result is TenantDetailsResult.NotFound)
            val notFound = result as TenantDetailsResult.NotFound
            assertContains(notFound.message, "Tenant not found: $tenantId")
        }

    @Test
    fun `should update tenant successfully when new name is unique`() =
        runTest {
            // Given
            val tenant = Tenant.create("Original Name", validSettings, now)
            val newName = "Updated Name"
            val newSettings =
                TenantSettings(
                    maxUsers = 200,
                    allowedDomains = listOf("updated.com"),
                    features = setOf("new-feature"),
                )

            coEvery { tenantRepository.findById(eq<TenantId>(tenant.id)) } returns tenant
            coEvery { tenantRepository.existsByName(newName) } returns false
            val updatedTenant = tenant.copy(name = newName, settings = newSettings)
            coEvery { tenantRepository.save(any<Tenant>()) } returns updatedTenant

            // When
            val result = tenantService.updateTenant(tenant.id.value, newName, newSettings)

            // Then
            assertTrue(result is UpdateTenantResult.Success)
            val success = result as UpdateTenantResult.Success
            assertEquals(newName, success.tenant.name)

            coVerify { tenantRepository.findById(eq<TenantId>(tenant.id)) }
            coVerify { tenantRepository.existsByName(newName) }
            coVerify { tenantRepository.save(any<Tenant>()) }
        }

    @Test
    fun `should update tenant successfully when keeping same name`() =
        runTest {
            // Given
            val originalName = "Same Name"
            val tenant = Tenant.create(originalName, validSettings, now)
            val newSettings =
                TenantSettings(
                    maxUsers = 300,
                    allowedDomains = listOf("same.com"),
                    features = setOf("updated-feature"),
                )

            coEvery { tenantRepository.findById(eq<TenantId>(tenant.id)) } returns tenant
            val updatedTenant = tenant.copy(settings = newSettings)
            coEvery { tenantRepository.save(any<Tenant>()) } returns updatedTenant

            // When
            val result = tenantService.updateTenant(tenant.id.value, originalName, newSettings)

            // Then
            assertTrue(result is UpdateTenantResult.Success)

            coVerify { tenantRepository.findById(eq<TenantId>(tenant.id)) }
            coVerify(exactly = 0) {
                tenantRepository.existsByName(any<String>())
            } // explicit type for K2
            coVerify { tenantRepository.save(any<Tenant>()) }
        }

    @Test
    fun `should fail to update tenant when new name conflicts with existing tenant`() =
        runTest {
            // Given
            val tenant = Tenant.create("Original Name", validSettings, now)
            val conflictingName = "Conflicting Name"

            coEvery { tenantRepository.findById(eq<TenantId>(tenant.id)) } returns tenant
            coEvery { tenantRepository.existsByName(conflictingName) } returns true

            // When
            val result = tenantService.updateTenant(tenant.id.value, conflictingName, validSettings)

            // Then
            assertTrue(result is UpdateTenantResult.Failure)
            val failure = result as UpdateTenantResult.Failure
            assertContains(failure.message, "Tenant with name '$conflictingName' already exists")

            coVerify(exactly = 0) { tenantRepository.save(any<Tenant>()) }
        }

    @Test
    fun `should suspend active tenant successfully`() =
        runTest {
            // Given
            val tenant = Tenant.create("Test Tenant", validSettings, now)
            val reason = "Policy violation"

            coEvery { tenantRepository.findById(eq<TenantId>(tenant.id)) } returns tenant
            val suspendedTenant = tenant.suspend(Instant.now())
            coEvery { tenantRepository.save(any<Tenant>()) } returns suspendedTenant

            // When
            val result = tenantService.suspendTenant(tenant.id.value, reason)

            // Then
            assertTrue(result is TenantOperationResult.Success)
            val success = result as TenantOperationResult.Success
            assertContains(success.message, "Tenant suspended successfully")

            coVerify { tenantRepository.save(any<Tenant>()) }
        }

    @Test
    fun `should fail to suspend non-active tenant`() =
        runTest {
            // Given
            val tenant = Tenant.create("Test Tenant", validSettings, now).suspend(now.plusSeconds(1000))

            coEvery { tenantRepository.findById(eq<TenantId>(tenant.id)) } returns tenant

            // When
            val result = tenantService.suspendTenant(tenant.id.value)

            // Then
            assertTrue(result is TenantOperationResult.Failure)
            val failure = result as TenantOperationResult.Failure
            assertContains(failure.message, "Can only suspend active tenants")

            coVerify(exactly = 0) { tenantRepository.save(any<Tenant>()) }
        }

    @Test
    fun `should reactivate suspended tenant successfully`() =
        runTest {
            // Given
            val tenant = Tenant.create("Test Tenant", validSettings, now).suspend(now.plusSeconds(1000))

            coEvery { tenantRepository.findById(eq<TenantId>(tenant.id)) } returns tenant
            val reactivatedTenant = tenant.reactivate(Instant.now())
            coEvery { tenantRepository.save(any<Tenant>()) } returns reactivatedTenant

            // When
            val result = tenantService.reactivateTenant(tenant.id.value)

            // Then
            assertTrue(result is TenantOperationResult.Success)
            val success = result as TenantOperationResult.Success
            assertContains(success.message, "Tenant reactivated successfully")

            coVerify { tenantRepository.save(any<Tenant>()) }
        }

    @Test
    fun `should fail to reactivate non-suspended tenant`() =
        runTest {
            // Given
            val tenant = Tenant.create("Test Tenant", validSettings, now)

            coEvery { tenantRepository.findById(eq<TenantId>(tenant.id)) } returns tenant

            // When
            val result = tenantService.reactivateTenant(tenant.id.value)

            // Then
            assertTrue(result is TenantOperationResult.Failure)
            val failure = result as TenantOperationResult.Failure
            assertContains(failure.message, "Can only reactivate suspended tenants")

            coVerify(exactly = 0) { tenantRepository.save(any<Tenant>()) }
        }

    @Test
    fun `should archive tenant successfully`() =
        runTest {
            // Given
            val tenant = Tenant.create("Test Tenant", validSettings, now)
            val reason = "End of contract"

            coEvery { tenantRepository.findById(eq<TenantId>(tenant.id)) } returns tenant
            val archivedTenant = tenant.archive(Instant.now())
            coEvery { tenantRepository.save(any<Tenant>()) } returns archivedTenant

            // When
            val result = tenantService.archiveTenant(tenant.id.value, reason)

            // Then
            assertTrue(result is ArchiveTenantResult.Success)
            val success = result as ArchiveTenantResult.Success
            assertEquals(tenant.id.value, success.tenantId)
            assertNotNull(success.archivedAt)

            coVerify { tenantRepository.save(any<Tenant>()) }
        }

    @Test
    fun `should fail to archive already archived tenant`() =
        runTest {
            // Given
            val tenant = Tenant.create("Test Tenant", validSettings, now).archive(now.plusSeconds(1000))

            coEvery { tenantRepository.findById(eq<TenantId>(tenant.id)) } returns tenant

            // When
            val result = tenantService.archiveTenant(tenant.id.value)

            // Then
            assertTrue(result is ArchiveTenantResult.Failure)
            val failure = result as ArchiveTenantResult.Failure
            assertContains(failure.message, "Tenant is already archived")

            coVerify(exactly = 0) { tenantRepository.save(any<Tenant>()) }
        }

    @Test
    fun `should list tenants with filtering`() =
        runTest {
            // Given
            val filter =
                TenantFilter(
                    status = TenantStatus.ACTIVE,
                    namePattern = "test",
                    page = 0,
                    size = 10,
                )
            val expectedResponse =
                mockk<
                    com.axians.eaf.controlplane.application.dto.tenant.PagedResponse<
                        com.axians.eaf.controlplane.application.dto.tenant.TenantSummary,
                    >,
                >()

            coEvery { tenantRepository.findAll(filter) } returns expectedResponse

            // When
            val result = tenantService.listTenants(filter)

            // Then
            assertEquals(expectedResponse, result)

            coVerify { tenantRepository.findAll(filter) }
        }

    @Test
    fun `should check if tenant is operational correctly`() =
        runTest {
            // Given
            val activeTenant = Tenant.create("Active Tenant", validSettings, now)
            val suspendedTenant =
                Tenant.create("Suspended Tenant", validSettings, now).suspend(now.plusSeconds(1000))

            coEvery { tenantRepository.findById(eq<TenantId>(activeTenant.id)) } returns activeTenant
            coEvery { tenantRepository.findById(eq<TenantId>(suspendedTenant.id)) } returns
                suspendedTenant
            coEvery {
                tenantRepository.findById(eq<TenantId>(TenantId.fromString("non-existent")))
            } returns null

            // When & Then
            assertTrue(tenantService.isTenantOperational(activeTenant.id.value))
            assertFalse(tenantService.isTenantOperational(suspendedTenant.id.value))
            assertFalse(tenantService.isTenantOperational("non-existent"))
        }
}

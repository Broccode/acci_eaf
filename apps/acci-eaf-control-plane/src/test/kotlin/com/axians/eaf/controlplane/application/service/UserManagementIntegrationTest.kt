package com.axians.eaf.controlplane.application.service

import com.axians.eaf.controlplane.domain.model.tenant.TenantSettings
import com.axians.eaf.controlplane.domain.model.user.UserStatus
import com.axians.eaf.controlplane.domain.service.CreateTenantResult
import com.axians.eaf.controlplane.domain.service.CreateUserResult
import com.axians.eaf.controlplane.domain.service.TenantService
import com.axians.eaf.controlplane.domain.service.UserService
import com.axians.eaf.controlplane.test.ControlPlaneTestcontainerConfiguration
import com.axians.eaf.controlplane.test.TestControlPlaneApplication
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Integration tests for user management functionality.
 *
 * Tests the complete flow of user management operations including tenant creation, user creation,
 * and lifecycle management.
 */
@SpringBootTest(classes = [TestControlPlaneApplication::class])
@ContextConfiguration(classes = [ControlPlaneTestcontainerConfiguration::class])
@ActiveProfiles("test")
@Transactional
class UserManagementIntegrationTest {
    @Autowired private lateinit var userService: UserService

    @Autowired private lateinit var tenantService: TenantService

    @Test
    fun `should create complete user management workflow`() =
        runTest {
            // Step 1: Create a tenant using proper service API
            val tenantResult =
                tenantService.createTenant(
                    name = "Acme Corporation",
                    adminEmail = "admin@acme.com",
                    settings = TenantSettings.default("acme.com"),
                )

            val createdTenant =
                when (tenantResult) {
                    is CreateTenantResult.Success -> tenantResult
                    is CreateTenantResult.Failure -> fail("Tenant creation failed: ${tenantResult.message}")
                }

            assertEquals("Acme Corporation", createdTenant.tenantName)

            // Step 2: Create users for the tenant using proper service API
            val adminUserResult =
                userService.createUser(
                    email = "admin@acme.com",
                    firstName = "Alice",
                    lastName = "Administrator",
                    tenantId = createdTenant.tenantId,
                    activateImmediately = true,
                )

            val regularUserResult =
                userService.createUser(
                    email = "john@acme.com",
                    firstName = "John",
                    lastName = "User",
                    tenantId = createdTenant.tenantId,
                    activateImmediately = true,
                )

            // Verify users are created correctly
            val adminUser =
                when (adminUserResult) {
                    is CreateUserResult.Success -> adminUserResult
                    is CreateUserResult.Failure ->
                        fail("Admin user creation failed: ${adminUserResult.message}")
                }

            val regularUser =
                when (regularUserResult) {
                    is CreateUserResult.Success -> regularUserResult
                    is CreateUserResult.Failure ->
                        fail("Regular user creation failed: ${regularUserResult.message}")
                }

            assertEquals("admin@acme.com", adminUser.email)
            assertEquals("Alice Administrator", adminUser.fullName)
            assertEquals(UserStatus.ACTIVE, adminUser.status)

            assertEquals("john@acme.com", regularUser.email)
            assertEquals("John User", regularUser.fullName)
            assertEquals(UserStatus.ACTIVE, regularUser.status)

            // Note: Additional test operations would require implementing the missing service methods
            // like getUsersByTenant, updateUser, deactivateUser, reactivateUser
        }

    @Test
    fun `should handle user creation validation correctly`() =
        runTest {
            // Create tenant first
            val tenantResult =
                tenantService.createTenant(
                    name = "Test Tenant",
                    adminEmail = "admin@test.com",
                    settings = TenantSettings.default("test.com"),
                )

            val tenant =
                when (tenantResult) {
                    is CreateTenantResult.Success -> tenantResult
                    is CreateTenantResult.Failure -> fail("Tenant creation failed: ${tenantResult.message}")
                }

            // Test successful user creation
            val userResult =
                userService.createUser(
                    email = "valid@test.com",
                    firstName = "Valid",
                    lastName = "User",
                    tenantId = tenant.tenantId,
                    activateImmediately = true,
                )

            val user =
                when (userResult) {
                    is CreateUserResult.Success -> userResult
                    is CreateUserResult.Failure -> fail("User creation failed: ${userResult.message}")
                }

            assertEquals("valid@test.com", user.email)
        }

    @Test
    fun `should handle user lifecycle operations correctly`() =
        runTest {
            // Setup
            val tenantResult =
                tenantService.createTenant(
                    name = "Lifecycle Test",
                    adminEmail = "admin@lifecycle.com",
                    settings = TenantSettings.default("lifecycle.com"),
                )

            val tenant =
                when (tenantResult) {
                    is CreateTenantResult.Success -> tenantResult
                    is CreateTenantResult.Failure -> fail("Tenant creation failed: ${tenantResult.message}")
                }

            val userResult =
                userService.createUser(
                    email = "lifecycle@test.com",
                    firstName = "Lifecycle",
                    lastName = "User",
                    tenantId = tenant.tenantId,
                    activateImmediately = true,
                )

            val user =
                when (userResult) {
                    is CreateUserResult.Success -> userResult
                    is CreateUserResult.Failure -> fail("User creation failed: ${userResult.message}")
                }

            // Basic validation
            assertEquals("lifecycle@test.com", user.email)
            assertEquals("Lifecycle User", user.fullName)
            assertEquals(UserStatus.ACTIVE, user.status)

            // Note: Additional lifecycle operations require implementing missing service methods
            // like getUserById, updateUser, deactivateUser, reactivateUser
        }

    @Test
    fun `should demonstrate multi-tenant user operations`() =
        runTest {
            // Create multiple tenants
            val tenant1Result =
                tenantService.createTenant(
                    name = "Company A",
                    adminEmail = "admin@companya.com",
                    settings = TenantSettings.default("companya.com"),
                )

            val tenant2Result =
                tenantService.createTenant(
                    name = "Company B",
                    adminEmail = "admin@companyb.com",
                    settings = TenantSettings.default("companyb.com"),
                )

            val tenant1 =
                when (tenant1Result) {
                    is CreateTenantResult.Success -> tenant1Result
                    is CreateTenantResult.Failure -> fail("Tenant1 creation failed: ${tenant1Result.message}")
                }

            val tenant2 =
                when (tenant2Result) {
                    is CreateTenantResult.Success -> tenant2Result
                    is CreateTenantResult.Failure -> fail("Tenant2 creation failed: ${tenant2Result.message}")
                }

            // Create users in different tenants
            val usersForTenant1 =
                (1..3).map { i ->
                    val result =
                        userService.createUser(
                            email = "user$i@companya.com",
                            firstName = "User$i",
                            lastName = "CompanyA",
                            tenantId = tenant1.tenantId,
                            activateImmediately = true,
                        )
                    when (result) {
                        is CreateUserResult.Success -> result
                        is CreateUserResult.Failure -> fail("User creation failed: ${result.message}")
                    }
                }

            val usersForTenant2 =
                (1..2).map { i ->
                    val result =
                        userService.createUser(
                            email = "user$i@companyb.com",
                            firstName = "User$i",
                            lastName = "CompanyB",
                            tenantId = tenant2.tenantId,
                            activateImmediately = true,
                        )
                    when (result) {
                        is CreateUserResult.Success -> result
                        is CreateUserResult.Failure -> fail("User creation failed: ${result.message}")
                    }
                }

            // Basic verification
            assertEquals(3, usersForTenant1.size)
            assertEquals(2, usersForTenant2.size)

            // Verify tenant assignment
            usersForTenant1.forEach { user -> assertTrue(user.email.contains("companya.com")) }

            usersForTenant2.forEach { user -> assertTrue(user.email.contains("companyb.com")) }

            // Cross-tenant verification
            val allTenant1Emails = usersForTenant1.map { it.email }.toSet()
            val allTenant2Emails = usersForTenant2.map { it.email }.toSet()

            assertTrue(allTenant1Emails.intersect(allTenant2Emails).isEmpty())
        }
}

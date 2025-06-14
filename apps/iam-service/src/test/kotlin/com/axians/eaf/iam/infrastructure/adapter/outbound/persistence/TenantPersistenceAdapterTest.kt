package com.axians.eaf.iam.infrastructure.adapter.outbound.persistence

import com.axians.eaf.iam.PostgresTestcontainerConfiguration
import com.axians.eaf.iam.domain.model.Tenant
import com.axians.eaf.iam.domain.model.TenantStatus
import com.axians.eaf.iam.domain.model.User
import com.axians.eaf.iam.domain.model.UserRole
import com.axians.eaf.iam.domain.model.UserStatus
import com.axians.eaf.iam.infrastructure.config.JpaConfig
import com.axians.eaf.iam.test.TestIamServiceApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(classes = [TestIamServiceApplication::class])
@Testcontainers
@ActiveProfiles("test")
@Transactional
@Import(PostgresTestcontainerConfiguration::class, JpaConfig::class)
class TenantPersistenceAdapterTest {
    @Autowired
    private lateinit var tenantPersistenceAdapter: TenantPersistenceAdapter

    @Test
    fun `should save tenant with admin successfully`() {
        // Given
        val tenant =
            Tenant(
                tenantId = "test-tenant",
                name = "Test Tenant",
                status = TenantStatus.ACTIVE,
            )

        val tenantAdmin =
            User(
                userId = "admin-user-id",
                tenantId = "test-tenant",
                email = "admin@test-tenant.com",
                username = "admin",
                role = UserRole.TENANT_ADMIN,
                status = UserStatus.PENDING_ACTIVATION,
            )

        // When
        val result = tenantPersistenceAdapter.saveTenantWithAdmin(tenant, tenantAdmin)

        // Then
        assertNotNull(result)
        assertEquals("test-tenant", result.savedTenant.tenantId)
        assertEquals("Test Tenant", result.savedTenant.name)
        assertEquals("admin-user-id", result.savedTenantAdmin.userId)
        assertEquals("admin@test-tenant.com", result.savedTenantAdmin.email)
    }

    @Test
    fun `should check tenant name existence correctly`() {
        // Given
        val tenant =
            Tenant(
                tenantId = "existing-tenant",
                name = "Existing Tenant",
                status = TenantStatus.ACTIVE,
            )

        val tenantAdmin =
            User(
                userId = "admin-user-id",
                tenantId = "existing-tenant",
                email = "admin@existing-tenant.com",
                role = UserRole.TENANT_ADMIN,
                status = UserStatus.PENDING_ACTIVATION,
            )

        // When
        tenantPersistenceAdapter.saveTenantWithAdmin(tenant, tenantAdmin)

        // Then
        assertTrue(tenantPersistenceAdapter.existsByTenantName("Existing Tenant"))
        assertFalse(tenantPersistenceAdapter.existsByTenantName("Non-Existing Tenant"))
    }

    @Test
    fun `should check email existence correctly`() {
        // Given
        val tenant =
            Tenant(
                tenantId = "email-test-tenant",
                name = "Email Test Tenant",
                status = TenantStatus.ACTIVE,
            )

        val tenantAdmin =
            User(
                userId = "admin-user-id",
                tenantId = "email-test-tenant",
                email = "admin@email-test.com",
                role = UserRole.TENANT_ADMIN,
                status = UserStatus.PENDING_ACTIVATION,
            )

        // When
        tenantPersistenceAdapter.saveTenantWithAdmin(tenant, tenantAdmin)

        // Then
        assertTrue(tenantPersistenceAdapter.existsByEmail("admin@email-test.com"))
        assertTrue(tenantPersistenceAdapter.existsByEmail("ADMIN@EMAIL-TEST.COM")) // Case insensitive
        assertFalse(tenantPersistenceAdapter.existsByEmail("nonexistent@email.com"))
    }
}

package com.axians.eaf.iam.infrastructure.adapter.outbound.persistence

import com.axians.eaf.iam.PostgresTestcontainerConfiguration
import com.axians.eaf.iam.TestIamServiceApplication
import com.axians.eaf.iam.domain.model.Tenant
import com.axians.eaf.iam.domain.model.User
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
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
@Import(PostgresTestcontainerConfiguration::class)
@Disabled("Temporarily disabled until testcontainer configuration is fixed")
class TenantPersistenceAdapterTest {
    @Autowired
    private lateinit var tenantPersistenceAdapter: TenantPersistenceAdapter

    @Autowired
    private lateinit var tenantJpaRepository: TenantJpaRepository

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @AfterEach
    fun cleanup() {
        userJpaRepository.deleteAll()
        tenantJpaRepository.deleteAll()
    }

    @Test
    fun `should save tenant with admin in single transaction when valid entities provided`() {
        // Given
        val tenant = Tenant.create("Test Company")
        val admin = User.createTenantAdmin(tenant.tenantId, "admin@testcompany.com")

        // When
        val result = tenantPersistenceAdapter.saveTenantWithAdmin(tenant, admin)

        // Then
        assertNotNull(result)
        assertEquals(tenant.tenantId, result.savedTenant.tenantId)
        assertEquals(tenant.name, result.savedTenant.name)
        assertTrue(result.savedTenant.isActive())

        assertEquals(admin.userId, result.savedTenantAdmin.userId)
        assertEquals(admin.email, result.savedTenantAdmin.email)
        assertEquals(admin.tenantId, result.savedTenantAdmin.tenantId)
        assertTrue(result.savedTenantAdmin.isTenantAdmin())
    }

    @Test
    fun `should return true when tenant name already exists`() {
        // Given
        val tenant = Tenant.create("Existing Company")
        val admin = User.createTenantAdmin(tenant.tenantId, "admin@existing.com")
        tenantPersistenceAdapter.saveTenantWithAdmin(tenant, admin)

        // When
        val exists = tenantPersistenceAdapter.existsByTenantName("Existing Company")

        // Then
        assertTrue(exists)
    }

    @Test
    fun `should return false when tenant name does not exist`() {
        // When
        val exists = tenantPersistenceAdapter.existsByTenantName("Non-Existing Company")

        // Then
        assertFalse(exists)
    }

    @Test
    fun `should handle tenant name case sensitivity correctly`() {
        // Given
        val tenant = Tenant.create("Test Company")
        val admin = User.createTenantAdmin(tenant.tenantId, "admin@test.com")
        tenantPersistenceAdapter.saveTenantWithAdmin(tenant, admin)

        // When & Then
        assertTrue(tenantPersistenceAdapter.existsByTenantName("Test Company"))
        assertFalse(tenantPersistenceAdapter.existsByTenantName("test company"))
        assertFalse(tenantPersistenceAdapter.existsByTenantName("TEST COMPANY"))
    }

    @Test
    fun `should return true when email already exists across tenants`() {
        // Given
        val tenant1 = Tenant.create("Company 1")
        val admin1 = User.createTenantAdmin(tenant1.tenantId, "existing@email.com")
        tenantPersistenceAdapter.saveTenantWithAdmin(tenant1, admin1)

        // When
        val exists = tenantPersistenceAdapter.existsByEmail("existing@email.com")

        // Then
        assertTrue(exists)
    }

    @Test
    fun `should return false when email does not exist`() {
        // When
        val exists = tenantPersistenceAdapter.existsByEmail("nonexisting@email.com")

        // Then
        assertFalse(exists)
    }

    @Test
    fun `should handle email case insensitivity correctly`() {
        // Given
        val tenant = Tenant.create("Test Company")
        val admin = User.createTenantAdmin(tenant.tenantId, "test@example.com")
        tenantPersistenceAdapter.saveTenantWithAdmin(tenant, admin)

        // When & Then
        assertTrue(tenantPersistenceAdapter.existsByEmail("test@example.com"))
        assertTrue(tenantPersistenceAdapter.existsByEmail("TEST@EXAMPLE.COM"))
        assertTrue(tenantPersistenceAdapter.existsByEmail("Test@Example.Com"))
    }
}

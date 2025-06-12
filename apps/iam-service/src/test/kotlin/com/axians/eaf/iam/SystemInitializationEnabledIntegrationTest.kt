package com.axians.eaf.iam

import com.axians.eaf.iam.domain.model.UserRole
import com.axians.eaf.iam.domain.model.UserStatus
import com.axians.eaf.iam.infrastructure.adapter.outbound.persistence.TenantJpaRepository
import com.axians.eaf.iam.infrastructure.adapter.outbound.persistence.UserJpaRepository
import com.axians.eaf.iam.infrastructure.config.JpaConfig
import com.axians.eaf.iam.infrastructure.config.SystemInitializationProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(
    classes = [com.axians.eaf.iam.test.TestIamServiceApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
)
@Testcontainers
@ActiveProfiles("test")
@Transactional
@Import(PostgresTestcontainerConfiguration::class, JpaConfig::class)
@TestPropertySource(
    properties = [
        "eaf.system.initialize-default-tenant=true", // Enable auto-initialization
        "eaf.system.default-tenant-name=AutoInitTenant",
        "eaf.system.default-super-admin-email=autoinit@test.com",
    ],
)
class SystemInitializationEnabledIntegrationTest {
    @Autowired
    private lateinit var tenantJpaRepository: TenantJpaRepository

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var properties: SystemInitializationProperties

    @Test
    fun `should automatically initialize system on startup when enabled`() {
        // Given - the application has started with initialization enabled
        assertTrue(properties.initializeDefaultTenant)

        // Then - verify that the DataInitializerRunner has already created the data
        val tenants = tenantJpaRepository.findAll()
        assertEquals(1, tenants.size)
        val tenant = tenants.first()
        assertEquals("AutoInitTenant", tenant.name)
        assertNotNull(tenant.tenantId)

        // Verify superadmin user was created
        val users = userJpaRepository.findAll()
        assertEquals(1, users.size)
        val user = users.first()
        assertEquals("autoinit@test.com", user.email)
        assertEquals(UserRole.SUPER_ADMIN, user.role)
        assertEquals(UserStatus.ACTIVE, user.status)
        assertEquals(tenant.tenantId, user.tenantId)
    }

    @Test
    fun `should verify configuration properties are correctly set for auto-initialization`() {
        // Then
        assertTrue(properties.initializeDefaultTenant)
        assertEquals("AutoInitTenant", properties.defaultTenantName)
        assertEquals("autoinit@test.com", properties.defaultSuperAdminEmail)
    }
}

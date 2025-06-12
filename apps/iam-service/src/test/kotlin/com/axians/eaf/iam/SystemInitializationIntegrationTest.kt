package com.axians.eaf.iam

import com.axians.eaf.iam.application.service.SystemInitializationService
import com.axians.eaf.iam.domain.model.UserRole
import com.axians.eaf.iam.domain.model.UserStatus
import com.axians.eaf.iam.infrastructure.adapter.inbound.DataInitializerRunner
import com.axians.eaf.iam.infrastructure.adapter.outbound.persistence.TenantJpaRepository
import com.axians.eaf.iam.infrastructure.adapter.outbound.persistence.UserJpaRepository
import com.axians.eaf.iam.infrastructure.config.JpaConfig
import com.axians.eaf.iam.infrastructure.config.SystemInitializationProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
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
        "eaf.system.initialize-default-tenant=false",
        "eaf.system.default-tenant-name=IntegrationTestTenant",
        "eaf.system.default-super-admin-email=integration@test.com",
    ],
)
class SystemInitializationIntegrationTest {
    @Autowired
    private lateinit var systemInitializationService: SystemInitializationService

    @Autowired
    private lateinit var dataInitializerRunner: DataInitializerRunner

    @Autowired
    private lateinit var tenantJpaRepository: TenantJpaRepository

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var properties: SystemInitializationProperties

    @Test
    fun `should initialize system with default tenant and superadmin via service`() {
        // Given - clean database
        assertEquals(0, tenantJpaRepository.count())
        assertEquals(0, userJpaRepository.count())

        // When
        val result = systemInitializationService.initializeDefaultTenantIfRequired()

        // Then
        assertTrue(result.wasInitialized)
        assertEquals("IntegrationTestTenant", result.message)

        // Verify tenant was created
        val tenants = tenantJpaRepository.findAll()
        assertEquals(1, tenants.size)
        val tenant = tenants.first()
        assertEquals("IntegrationTestTenant", tenant.name)
        assertNotNull(tenant.tenantId)

        // Verify superadmin user was created
        val users = userJpaRepository.findAll()
        assertEquals(1, users.size)
        val user = users.first()
        assertEquals("integration@test.com", user.email)
        assertEquals(UserRole.SUPER_ADMIN, user.role)
        assertEquals(UserStatus.ACTIVE, user.status)
        assertEquals(tenant.tenantId, user.tenantId)
    }

    @Test
    fun `should not initialize when system is already initialized`() {
        // Given - initialize system first
        systemInitializationService.initializeDefaultTenantIfRequired()
        assertEquals(1, tenantJpaRepository.count())
        assertEquals(1, userJpaRepository.count())

        // When - try to initialize again
        val result = systemInitializationService.initializeDefaultTenantIfRequired()

        // Then
        assertFalse(result.wasInitialized)
        assertTrue(result.message.contains("already initialized"))

        // Verify no additional records were created
        assertEquals(1, tenantJpaRepository.count())
        assertEquals(1, userJpaRepository.count())
    }

    @Test
    fun `should not initialize via application runner when disabled`() {
        // Given - clean database and disabled configuration
        assertEquals(0, tenantJpaRepository.count())
        assertEquals(0, userJpaRepository.count())
        assertFalse(properties.initializeDefaultTenant)

        // When
        dataInitializerRunner.run(null as ApplicationArguments?)

        // Then - no data should be created
        assertEquals(0, tenantJpaRepository.count())
        assertEquals(0, userJpaRepository.count())
    }

    @Test
    fun `should verify configuration properties are correctly injected`() {
        // Then
        assertFalse(properties.initializeDefaultTenant)
        assertEquals("IntegrationTestTenant", properties.defaultTenantName)
        assertEquals("integration@test.com", properties.defaultSuperAdminEmail)
    }
}

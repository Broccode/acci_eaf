package com.axians.eaf.iam

import com.axians.eaf.iam.infrastructure.adapter.outbound.persistence.TenantJpaRepository
import com.axians.eaf.iam.infrastructure.adapter.outbound.persistence.UserJpaRepository
import com.axians.eaf.iam.infrastructure.config.JpaConfig
import com.axians.eaf.iam.test.TestIamServiceApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(
    classes = [TestIamServiceApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = ["eaf.system.initialize-default-tenant=true"],
)
@Testcontainers
@ActiveProfiles("test")
@Transactional
@Import(PostgresTestcontainerConfiguration::class, JpaConfig::class)
class SystemInitializationEnabledIntegrationTest {
    @Autowired
    private lateinit var tenantJpaRepository: TenantJpaRepository

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Test
    fun `should initialize default tenant and admin user when enabled`() {
        // Given & When
        val tenantCount = tenantJpaRepository.count()
        val userCount = userJpaRepository.count()

        // Then
        assertEquals(1, tenantCount)
        assertEquals(1, userCount)
    }
}

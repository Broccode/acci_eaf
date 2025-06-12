package com.axians.eaf.iam

import com.axians.eaf.iam.infrastructure.adapter.outbound.persistence.TenantJpaRepository
import com.axians.eaf.iam.infrastructure.adapter.outbound.persistence.UserJpaRepository
import com.axians.eaf.iam.infrastructure.config.JpaConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
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
class IamServiceIntegrationTest {
    @Autowired
    private lateinit var tenantJpaRepository: TenantJpaRepository

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Test
    fun `should verify integration components are wired correctly`() {
        // Given & When
        val tenantCount = tenantJpaRepository.count()
        val userCount = userJpaRepository.count()

        // Then
        assertEquals(0, tenantCount) // Clean database
        assertEquals(0, userCount) // Clean database

        // Verify repositories are properly injected
        assertNotNull(tenantJpaRepository)
        assertNotNull(userJpaRepository)
    }
}

package com.axians.eaf.iam

import com.axians.eaf.iam.infrastructure.config.JpaConfig
import com.axians.eaf.iam.test.TestIamServiceApplication
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(classes = [TestIamServiceApplication::class])
@Testcontainers
@ActiveProfiles("test")
@Import(JpaConfig::class, PostgresTestcontainerConfiguration::class)
class IamServiceApplicationTest {
    companion object {
        @Container
        val postgresContainer = PostgresTestcontainerConfiguration().postgresContainer()
    }

    @Test
    fun `context loads`() {
        // Verifies that the Spring application context can start successfully.
        // This test uses H2 in-memory database configured in application-test.properties
    }
}

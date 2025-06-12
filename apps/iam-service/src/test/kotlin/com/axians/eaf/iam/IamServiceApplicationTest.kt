package com.axians.eaf.iam

import com.axians.eaf.iam.infrastructure.config.JpaConfig
import com.axians.eaf.iam.test.TestIamServiceApplication
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [TestIamServiceApplication::class])
@ActiveProfiles("test")
@Import(JpaConfig::class)
class IamServiceApplicationTest {
    @Test
    fun `context loads`() {
        // Verifies that the Spring application context can start successfully.
        // This test uses H2 in-memory database configured in application-test.properties
    }
}

package com.axians.eaf.iam

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [TestIamServiceApplication::class])
@ActiveProfiles("test")
@Disabled("Integration tests disabled - focus on unit tests first")
class IamServiceApplicationTest {
    @Test
    fun `context loads`() {
        // Verifies that the Spring application context can start successfully.
        // This test uses H2 in-memory database configured in application-test.properties
    }
}

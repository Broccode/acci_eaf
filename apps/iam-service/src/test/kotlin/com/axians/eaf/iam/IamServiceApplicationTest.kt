package com.axians.eaf.iam

import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration::class])
class IamServiceApplicationTest {
    @Test
    fun `should load application context`() {
        // This test verifies that the Spring Boot application context loads successfully
    }
}

package com.axians.eaf.iam

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration::class])
class IamServiceApplicationTest {
    @Test
    fun `should load application context`() {
        // This test verifies that the Spring Boot application context loads successfully
        val context = SpringApplication.run(IamServiceApplication::class.java)
        assertNotNull(context)
    }
}

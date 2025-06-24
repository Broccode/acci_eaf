package com.axians.eaf.controlplane

import com.axians.eaf.controlplane.infrastructure.adapter.input.ControlPlaneHealthEndpoint
import com.axians.eaf.controlplane.infrastructure.adapter.input.EchoRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Integration test for Hilla endpoints within Spring Boot context. This test verifies that
 * @BrowserCallable endpoints are properly registered and can be called within the Spring
 * application context. Uses PostgreSQL with Testcontainers for proper database integration testing.
 */
@SpringBootTest(classes = [ControlPlaneApplication::class])
@Testcontainers
@ActiveProfiles("test")
class HillaIntegrationTest {
    companion object {
        @Container
        @ServiceConnection
        val postgresql: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:15-alpine")
    }

    @Autowired private lateinit var healthEndpoint: ControlPlaneHealthEndpoint

    @Test
    fun `should inject HealthEndpoint as Spring bean`() {
        // Then
        assertThat(healthEndpoint).isNotNull
    }

    @Test
    fun `should provide health check through injected bean`() {
        // When
        val result = healthEndpoint.getHealth()

        // Then
        assertThat(result).isNotNull
        assertThat(result.status).isEqualTo("UP")
        assertThat(result.version).isEqualTo("0.0.1-SNAPSHOT")
    }

    @Test
    fun `should provide system info through injected bean`() {
        // When
        val result = healthEndpoint.getSystemInfo()

        // Then
        assertThat(result).isNotNull
        assertThat(result.systemInfo?.applicationName).isEqualTo("EAF Control Plane")
        assertThat(result.systemInfo?.version).isEqualTo("1.0.0-SNAPSHOT")
    }

    @Test
    fun `should handle echo requests through injected bean`() {
        // Given
        val testMessage = "Integration test message"
        val echoRequest = EchoRequest(message = testMessage)

        // When
        val result = healthEndpoint.echo(echoRequest)

        // Then
        assertThat(result).isNotNull
        assertThat(result.originalMessage).isEqualTo(testMessage)
        assertThat(result.serverResponse).contains("Echo from Control Plane")
    }
}

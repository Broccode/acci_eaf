package com.axians.eaf.controlplane.infrastructure.adapter.input

import com.vaadin.hilla.exception.EndpointException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

/** Enhanced unit tests for HealthEndpoint following TDD principles and testing type safety */
class HealthEndpointTest {
    private val healthEndpoint = ControlPlaneHealthEndpoint()

    @Test
    fun `should return basic health status`() {
        // When
        val result = healthEndpoint.getHealth()

        // Then - Validate basic response structure
        assertThat(result.status).isEqualTo("UP")
        assertThat(result.service).isEqualTo("ACCI EAF Control Plane")
        assertThat(result.version).isEqualTo("1.0.0-SNAPSHOT")
        assertThat(result.timestamp).isNotEmpty()
    }

    @Test
    fun `should return detailed health check with service connectivity`() {
        // When
        val result = healthEndpoint.getDetailedHealth()

        // Then - Validate detailed response structure
        assertThat(result.status).isIn("UP", "DOWN", "DEGRADED", "UNKNOWN")
        assertThat(result.service).isEqualTo("ACCI EAF Control Plane")
        assertThat(result.version).isEqualTo("1.0.0-SNAPSHOT")
        assertThat(result.timestamp).isNotEmpty()
        assertThat(result.serviceChecks).isNotNull
        assertThat(result.summary).isNotNull

        // Validate summary information
        assertThat(result.summary).containsKey("totalChecks")
        assertThat(result.summary).containsKey("upServices")
        assertThat(result.summary).containsKey("downServices")
        assertThat(result.summary).containsKey("configuredServices")
    }

    @Test
    fun `should return enhanced system information with comprehensive details`() {
        // When
        val result = healthEndpoint.getSystemInfo()

        // Then - Validate enhanced response structure
        assertThat(result.success).isTrue()
        assertThat(result.systemInfo).isNotNull
        assertThat(result.error).isNull()

        result.systemInfo?.let { sysInfo ->
            assertThat(sysInfo.applicationName).isEqualTo("EAF Control Plane")
            assertThat(sysInfo.version).isEqualTo("1.0.0-SNAPSHOT")
            assertThat(sysInfo.environment).isEqualTo("development")
            assertThat(sysInfo.buildTime).isNotBlank()
            assertThat(sysInfo.javaVersion).isNotEmpty()
            assertThat(sysInfo.springBootVersion).isEqualTo("3.3.1")
            assertThat(sysInfo.hillaVersion).isEqualTo("2.5.8")
            assertThat(sysInfo.uptime).matches("\\d+m")
        }

        // Validate metadata structure
        assertThat(result.metadata.timestamp).isBeforeOrEqualTo(Instant.now())
        assertThat(result.metadata.requestId).startsWith("sysinfo-")
        assertThat(result.metadata.version).isEqualTo("1.0.0")
    }

    @Test
    fun `should validate echo request and provide enhanced response`() {
        // Given
        val request = EchoRequest(message = "Hello from frontend", includeTimestamp = true)

        // When
        val result = healthEndpoint.echo(request)

        // Then - Validate enhanced response structure
        assertThat(result.success).isTrue()
        assertThat(result.originalMessage).isEqualTo("Hello from frontend")
        assertThat(result.serverResponse).contains("Echo from Control Plane")
        assertThat(result.timestamp).isBeforeOrEqualTo(Instant.now())
        assertThat(result.messageLength).isEqualTo("Hello from frontend".length)
        assertThat(result.error).isNull()

        // Validate metadata
        assertThat(result.metadata.timestamp).isBeforeOrEqualTo(Instant.now())
        assertThat(result.metadata.requestId).startsWith("echo-")
        assertThat(result.metadata.version).isEqualTo("1.0.0")
    }

    @Test
    fun `should reject null or blank message in echo endpoint`() {
        // Given - Null message
        val nullRequest = EchoRequest(message = null)

        // When & Then
        val nullException = assertThrows<EndpointException> { healthEndpoint.echo(nullRequest) }
        assertThat(nullException.message).contains("Message cannot be null or blank")

        // Given - Blank message
        val blankRequest = EchoRequest(message = "   ")

        // When & Then
        val blankException = assertThrows<EndpointException> { healthEndpoint.echo(blankRequest) }
        assertThat(blankException.message).contains("Message cannot be null or blank")
    }

    @Test
    fun `should reject messages that are too long in echo endpoint`() {
        // Given - Message exceeding 1000 characters
        val longMessage = "a".repeat(1001)
        val request = EchoRequest(message = longMessage)

        // When & Then
        val exception = assertThrows<EndpointException> { healthEndpoint.echo(request) }
        assertThat(exception.message).contains("Message too long (max 1000 characters)")
    }

    @Test
    fun `should handle maximum allowed message length in echo endpoint`() {
        // Given - Message exactly at 1000 character limit
        val maxMessage = "a".repeat(1000)
        val request = EchoRequest(message = maxMessage)

        // When
        val result = healthEndpoint.echo(request)

        // Then
        assertThat(result.success).isTrue()
        assertThat(result.originalMessage).hasSize(1000)
        assertThat(result.messageLength).isEqualTo(1000)
    }

    @Test
    fun `should validate response type consistency across all endpoints`() {
        // When - Call all endpoints
        val healthResult = healthEndpoint.getHealth()
        val systemResult = healthEndpoint.getSystemInfo()
        val echoResult = healthEndpoint.echo(EchoRequest("test"))

        // Then - All should have consistent timestamp and version patterns
        assertThat(healthResult.timestamp).isNotEmpty()
        assertThat(healthResult.version).isEqualTo("1.0.0-SNAPSHOT")

        assertThat(systemResult.metadata.timestamp).isBeforeOrEqualTo(Instant.now())
        assertThat(systemResult.metadata.version).isEqualTo("1.0.0")

        assertThat(echoResult.metadata.timestamp).isBeforeOrEqualTo(Instant.now())
        assertThat(echoResult.metadata.version).isEqualTo("1.0.0")
    }
}

package com.axians.eaf.controlplane.infrastructure.adapter.outbound.health

import com.axians.eaf.eventing.config.NatsEventingProperties
import com.axians.eaf.iam.client.EafIamProperties
import io.mockk.every
import io.mockk.mockk
import io.nats.client.Connection
import io.nats.client.Statistics
import org.junit.jupiter.api.Test
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import kotlin.test.assertEquals

/** Tests for enhanced EAF health indicators that perform actual connectivity checks. */
class EafHealthIndicatorsTest {
    @Test
    fun `EafIamHealthIndicator should return UP status when IAM service is available`() {
        // Given
        val iamProperties = EafIamProperties(serviceUrl = "http://localhost:8081")
        val restTemplate = mockk<RestTemplate>()
        val requestFactory = mockk<ClientHttpRequestFactory>()

        every { restTemplate.requestFactory } returns requestFactory
        every {
            restTemplate.getForObject("http://localhost:8081/actuator/health", Map::class.java)
        } returns
            mapOf(
                "status" to "UP",
                "components" to mapOf("database" to mapOf("status" to "UP")),
            )

        val healthIndicator = EafIamHealthIndicator(iamProperties, restTemplate)

        // When
        val result = healthIndicator.checkHealth()

        // Then
        assertEquals("UP", result.status)
        assertEquals("EAF IAM Service", result.service)
        assertEquals("http://localhost:8081", result.details["serviceUrl"])
        assertEquals("available", result.details["connectivity"])
        assertEquals("UP", result.details["responseStatus"])
    }

    @Test
    fun `EafIamHealthIndicator should return DOWN status when IAM service is unavailable`() {
        // Given
        val iamProperties = EafIamProperties(serviceUrl = "http://localhost:8081")
        val restTemplate = mockk<RestTemplate>()
        val requestFactory = mockk<ClientHttpRequestFactory>()

        every { restTemplate.requestFactory } returns requestFactory
        every { restTemplate.getForObject(any<String>(), Map::class.java) } throws
            RestClientException("Connection refused")

        val healthIndicator = EafIamHealthIndicator(iamProperties, restTemplate)

        // When
        val result = healthIndicator.checkHealth()

        // Then
        assertEquals("DOWN", result.status)
        assertEquals("EAF IAM Service", result.service)
        assertEquals("error", result.details["connectivity"])
        val error = result.details["error"] as Map<String, Any>
        assertEquals("RestClientException", error["exceptionType"])
    }

    @Test
    fun `EafNatsHealthIndicator should return UP status when NATS connection is active`() {
        // Given
        val natsProperties =
            NatsEventingProperties(
                servers = listOf("nats://localhost:4222"),
                connectionName = "test-connection",
            )
        val connection = mockk<Connection>()
        val statistics = mockk<Statistics>()

        every { connection.status } returns Connection.Status.CONNECTED
        every { connection.serverInfo } returns null
        every { connection.jetStream() } returns mockk()
        every { connection.statistics } returns statistics
        every { statistics.inMsgs } returns 100L
        every { statistics.outMsgs } returns 50L
        every { statistics.inBytes } returns 1024L
        every { statistics.outBytes } returns 512L
        every { statistics.reconnects } returns 2L

        val healthIndicator = EafNatsHealthIndicator(connection, natsProperties)

        // When
        val result = healthIndicator.checkHealth()

        // Then
        assertEquals("UP", result.status)
        assertEquals("EAF NATS Messaging", result.service)
        assertEquals(listOf("nats://localhost:4222"), result.details["servers"])
        assertEquals("test-connection", result.details["connectionName"])

        val connectionDetails = result.details["connectionDetails"] as Map<String, Any>
        assertEquals(true, connectionDetails["isConnected"])
        assertEquals("UP", connectionDetails["status"])
    }

    @Test
    fun `EafNatsHealthIndicator should return DOWN status when NATS connection is not active`() {
        // Given
        val natsProperties =
            NatsEventingProperties(
                servers = listOf("nats://localhost:4222"),
                connectionName = "test-connection",
            )
        val connection = mockk<Connection>()

        every { connection.status } returns Connection.Status.DISCONNECTED

        val healthIndicator = EafNatsHealthIndicator(connection, natsProperties)

        // When
        val result = healthIndicator.checkHealth()

        // Then
        assertEquals("DOWN", result.status)
        assertEquals("EAF NATS Messaging", result.service)

        val connectionDetails = result.details["connectionDetails"] as Map<String, Any>
        assertEquals(false, connectionDetails["isConnected"])
        assertEquals("DOWN", connectionDetails["status"])
        assertEquals("NATS connection is not in CONNECTED state", connectionDetails["reason"])
    }
}

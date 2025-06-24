package com.axians.eaf.controlplane.infrastructure.adapter.outbound.health

import com.axians.eaf.eventing.config.NatsEventingProperties
import io.nats.client.Connection
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

/**
 * Custom health indicator for EAF NATS messaging service connectivity. This indicator performs
 * actual connectivity checks against the configured NATS cluster.
 *
 * Provides operational monitoring capabilities as required by AC 5 (Operational Monitoring).
 */
@Component
@ConditionalOnBean(Connection::class)
class EafNatsHealthIndicator(
    private val natsConnection: Connection,
    private val natsEventingProperties: NatsEventingProperties,
) {
    data class HealthStatus(
        val status: String,
        val service: String,
        val lastCheck: String,
        val details: Map<String, Any> = emptyMap(),
    )

    fun checkHealth(): HealthStatus =
        try {
            val startTime = Instant.now()
            val connectionDetails = performNatsHealthCheck()
            val responseTime = Duration.between(startTime, Instant.now()).toMillis()

            HealthStatus(
                status = connectionDetails["status"] as String,
                service = "EAF NATS Messaging",
                lastCheck = Instant.now().toString(),
                details =
                    mapOf(
                        "servers" to natsEventingProperties.servers,
                        "connectionName" to natsEventingProperties.connectionName,
                        "responseTimeMs" to responseTime,
                        "connectionDetails" to connectionDetails,
                    ),
            )
        } catch (exception: Exception) {
            HealthStatus(
                status = "DOWN",
                service = "EAF NATS Messaging",
                lastCheck = Instant.now().toString(),
                details =
                    mapOf(
                        "servers" to natsEventingProperties.servers,
                        "connectionName" to natsEventingProperties.connectionName,
                        "connectivity" to "error",
                        "error" to
                            mapOf(
                                "exceptionType" to
                                    exception::class.simpleName,
                                "message" to
                                    (
                                        exception.message
                                            ?: "Unknown error"
                                    ),
                            ),
                    ),
            )
        }

    /**
     * Performs comprehensive NATS connection health check. Checks connection status, server info,
     * and JetStream availability.
     */
    private fun performNatsHealthCheck(): Map<String, Any> {
        val connectionStatus = natsConnection.status
        val isConnected = connectionStatus == Connection.Status.CONNECTED

        val details =
            mutableMapOf<String, Any>(
                "connectionStatus" to connectionStatus.name,
                "isConnected" to isConnected,
            )

        if (isConnected) {
            try {
                // Get server information
                val serverInfo = natsConnection.serverInfo
                details["serverInfo"] =
                    mapOf(
                        "serverId" to (serverInfo?.serverId ?: "unknown"),
                        "serverName" to (serverInfo?.serverName ?: "unknown"),
                        "version" to (serverInfo?.version ?: "unknown"),
                        "maxPayload" to (serverInfo?.maxPayload ?: -1),
                        "jetStreamAvailable" to (serverInfo?.isJetStreamAvailable ?: false),
                    )

                // Check basic JetStream connectivity (simplified)
                try {
                    val jetStream = natsConnection.jetStream()
                    details["jetStreamStatus"] = "available"
                    details["jetStreamConnected"] = true
                } catch (e: Exception) {
                    details["jetStreamStatus"] = "error: ${e.message}"
                    details["jetStreamConnected"] = false
                }

                // Get connection statistics
                details["statistics"] =
                    mapOf(
                        "messagesIn" to natsConnection.statistics.inMsgs,
                        "messagesOut" to natsConnection.statistics.outMsgs,
                        "bytesIn" to natsConnection.statistics.inBytes,
                        "bytesOut" to natsConnection.statistics.outBytes,
                        "reconnects" to natsConnection.statistics.reconnects,
                    )

                details["status"] = "UP"
            } catch (e: Exception) {
                details["error"] = "Failed to retrieve server info: ${e.message}"
                details["status"] = "DOWN"
            }
        } else {
            details["status"] = "DOWN"
            details["reason"] = "NATS connection is not in CONNECTED state"
        }

        return details
    }
}

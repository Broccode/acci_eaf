package com.axians.eaf.controlplane.infrastructure.adapter.input

import com.axians.eaf.controlplane.infrastructure.adapter.outbound.health.EafIamHealthIndicator
import com.axians.eaf.controlplane.infrastructure.adapter.outbound.health.EafNatsHealthIndicator
import com.vaadin.hilla.Endpoint
import com.vaadin.hilla.exception.EndpointException
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Health endpoint for the control plane service.
 *
 * FIXME: Test endpoint to isolate KotlinNullabilityPlugin crash See:
 * https://github.com/vaadin/hilla/issues/3443
 */
@Endpoint
@Service
class ControlPlaneHealthEndpoint(
    private val eafIamHealthIndicator: EafIamHealthIndicator? = null,
    private val eafNatsHealthIndicator: EafNatsHealthIndicator? = null,
) {
    // TODO: Inject health indicators when available
    // @Autowired
    // private lateinit var healthIndicators: Map<String, HealthIndicator>

    /** Basic health check endpoint. */
    fun getHealth(): HealthResponse =
        HealthResponse(
            status = "UP",
            timestamp = Instant.now().toString(),
            service = "ACCI EAF Control Plane",
            version = "0.0.1-SNAPSHOT",
        )

    /**
     * Comprehensive health check including EAF service connectivity. Tests actual connectivity to
     * IAM and NATS services.
     */
    fun getDetailedHealth(): DetailedHealthResponse {
        val overallStatus = mutableListOf<String>()
        val serviceChecks = mutableMapOf<String, Any>()

        // Check EAF IAM connectivity
        eafIamHealthIndicator?.let { indicator ->
            try {
                val iamHealth = runBlocking { indicator.checkHealth() }
                serviceChecks["eafIam"] = iamHealth
                overallStatus.add(iamHealth.status)
            } catch (e: Exception) {
                serviceChecks["eafIam"] =
                    mapOf(
                        "status" to "DOWN",
                        "error" to "Health check failed: ${e.message}",
                    )
                overallStatus.add("DOWN")
            }
        }
            ?: run {
                serviceChecks["eafIam"] =
                    mapOf(
                        "status" to "NOT_CONFIGURED",
                        "message" to "EAF IAM health indicator not available",
                    )
            }

        // Check EAF NATS connectivity
        eafNatsHealthIndicator?.let { indicator ->
            try {
                val natsHealth = indicator.checkHealth()
                serviceChecks["eafNats"] = natsHealth
                overallStatus.add(natsHealth.status)
            } catch (e: Exception) {
                serviceChecks["eafNats"] =
                    mapOf(
                        "status" to "DOWN",
                        "error" to "Health check failed: ${e.message}",
                    )
                overallStatus.add("DOWN")
            }
        }
            ?: run {
                serviceChecks["eafNats"] =
                    mapOf(
                        "status" to "NOT_CONFIGURED",
                        "message" to "EAF NATS health indicator not available",
                    )
            }

        // Determine overall status
        val finalStatus =
            when {
                overallStatus.any { it == "DOWN" } -> "DOWN"
                overallStatus.any { it == "DEGRADED" } -> "DEGRADED"
                overallStatus.all { it == "UP" } -> "UP"
                else -> "UNKNOWN"
            }

        return DetailedHealthResponse(
            status = finalStatus,
            timestamp = Instant.now().toString(),
            service = "ACCI EAF Control Plane",
            version = "0.0.1-SNAPSHOT",
            serviceChecks = serviceChecks,
            summary =
                mapOf(
                    "totalChecks" to serviceChecks.size,
                    "upServices" to overallStatus.count { it == "UP" },
                    "downServices" to overallStatus.count { it == "DOWN" },
                    "configuredServices" to overallStatus.size,
                ),
        )
    }

    /** Get comprehensive system information with validation */
    fun getSystemInfo(): SystemInfoResponse =
        try {
            SystemInfoResponse(
                success = true,
                systemInfo =
                    SystemInfo(
                        applicationName = "EAF Control Plane",
                        version = "1.0.0-SNAPSHOT",
                        buildTime = "2024-01-01T00:00:00Z", // TODO: Replace
                        // with actual
                        // build time
                        environment = "development",
                        javaVersion = System.getProperty("java.version"),
                        springBootVersion = "3.3.1",
                        hillaVersion = "2.5.8",
                        uptime = calculateUptime(),
                    ),
                metadata =
                    ResponseMetadata(
                        timestamp = Instant.now(),
                        requestId = "sysinfo-${System.currentTimeMillis()}",
                        version = "1.0.0",
                    ),
            )
        } catch (exception: Exception) {
            SystemInfoResponse(
                success = false,
                error = "Failed to retrieve system information: ${exception.message}",
                metadata =
                    ResponseMetadata(
                        timestamp = Instant.now(),
                        requestId = "sysinfo-error-${System.currentTimeMillis()}",
                        version = "1.0.0",
                    ),
            )
        }

    /** Enhanced echo endpoint with input validation */
    fun echo(request: EchoRequest): EchoResponse =
        try {
            // Validate input
            if (request.message.isNullOrBlank()) {
                throw ValidationException("Message cannot be null or blank")
            }

            if (request.message.length > 1000) {
                throw ValidationException("Message too long (max 1000 characters)")
            }

            EchoResponse(
                success = true,
                originalMessage = request.message,
                timestamp = Instant.now(),
                serverResponse = "Echo from Control Plane at ${Instant.now()}",
                messageLength = request.message.length,
                metadata =
                    ResponseMetadata(
                        timestamp = Instant.now(),
                        requestId = "echo-${System.currentTimeMillis()}",
                        version = "1.0.0",
                    ),
            )
        } catch (exception: ValidationException) {
            throw EndpointException("Invalid echo request: ${exception.message}")
        } catch (exception: Exception) {
            throw EndpointException("Echo failed: ${exception.message}")
        }

    // Private helper methods

    private fun checkDatabaseHealth(): ServiceHealth {
        // Simulate database health check
        return ServiceHealth(
            status = "UP",
            details =
                mapOf(
                    "connectionPool" to "active",
                    "totalConnections" to "10",
                    "activeConnections" to "2",
                ),
            responseTime = "12ms",
        )
    }

    private fun checkIamHealth(): ServiceHealth {
        // Simulate IAM service health check
        return ServiceHealth(
            status = "UP",
            details =
                mapOf(
                    "serviceUrl" to "http://localhost:8081",
                    "lastCheck" to Instant.now().toString(),
                ),
            responseTime = "23ms",
        )
    }

    private fun checkNatsHealth(): ServiceHealth {
        // Simulate NATS health check
        return ServiceHealth(
            status = "UP",
            details = mapOf("connectionStatus" to "CONNECTED", "subjects" to "5"),
            responseTime = "8ms",
        )
    }

    private fun determineOverallStatus(serviceHealths: Collection<ServiceHealth>): String =
        when {
            serviceHealths.all { it.status == "UP" } -> "healthy"
            serviceHealths.any { it.status == "DOWN" } -> "unhealthy"
            else -> "degraded"
        }

    private fun calculateUptime(): String {
        // Simple uptime calculation
        val uptimeMs = System.currentTimeMillis() - startTime
        val minutes = uptimeMs / (1000 * 60)
        return "${minutes}m"
    }

    companion object {
        private val startTime = System.currentTimeMillis()
    }
}

// Enhanced Response Models for Type Safety

/** Basic health response model. */
data class HealthResponse(
    val status: String,
    val timestamp: String,
    val service: String,
    val version: String,
)

/** Detailed health response model with EAF service connectivity details. */
data class DetailedHealthResponse(
    val status: String,
    val timestamp: String,
    val service: String,
    val version: String,
    val serviceChecks: Map<String, Any>,
    val summary: Map<String, Any>,
)

data class ServiceHealth(
    val status: String,
    val details: Map<String, String>,
    val responseTime: String,
)

data class HealthMetadata(
    val checkDuration: String,
    val environment: String,
    val nodeId: String,
)

/** Enhanced response model for system information */
data class SystemInfoResponse(
    val success: Boolean,
    val systemInfo: SystemInfo? = null,
    val error: String? = null,
    val metadata: ResponseMetadata,
)

data class SystemInfo(
    val applicationName: String,
    val version: String,
    val buildTime: String,
    val environment: String,
    val javaVersion: String,
    val springBootVersion: String,
    val hillaVersion: String,
    val uptime: String,
)

/** Request model for echo testing */
data class EchoRequest(
    val message: String?,
    val includeTimestamp: Boolean = true,
)

/** Enhanced response model for echo testing */
data class EchoResponse(
    val success: Boolean,
    val originalMessage: String,
    val timestamp: Instant,
    val serverResponse: String,
    val messageLength: Int,
    val metadata: ResponseMetadata,
    val error: String? = null,
)

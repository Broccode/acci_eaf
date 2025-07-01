package com.axians.eaf.controlplane.infrastructure.adapter.input

import com.axians.eaf.controlplane.infrastructure.adapter.input.common.EndpointExceptionHandler
import com.axians.eaf.controlplane.infrastructure.adapter.input.common.ResponseConstants
import com.axians.eaf.controlplane.infrastructure.adapter.input.common.ResponseMetadata
import com.axians.eaf.controlplane.infrastructure.adapter.input.common.ResponseMetadataFactory
import com.axians.eaf.controlplane.infrastructure.adapter.input.common.ValidationException
import com.axians.eaf.controlplane.infrastructure.adapter.outbound.health.EafIamHealthIndicator
import com.axians.eaf.controlplane.infrastructure.adapter.outbound.health.EafNatsHealthIndicator
import com.vaadin.hilla.Endpoint
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(ControlPlaneHealthEndpoint::class.java)

    /** Basic health check endpoint. */
    fun getHealth(): HealthResponse =
        EndpointExceptionHandler.handleEndpointExecution("getHealth", logger) {
            HealthResponse(
                status = "UP",
                timestamp = Instant.now().toString(),
                service = "ACCI EAF Control Plane",
                version = ResponseConstants.APPLICATION_VERSION,
            )
        }

    /**
     * Comprehensive health check including EAF service connectivity. Tests actual connectivity to
     * IAM and NATS services.
     */
    fun getDetailedHealth(): DetailedHealthResponse =
        EndpointExceptionHandler.handleEndpointExecution("getDetailedHealth", logger) {
            val overallStatus = mutableListOf<String>()
            val serviceChecks = mutableMapOf<String, Any>()

            // Check EAF IAM connectivity
            checkIamService(serviceChecks, overallStatus)

            // Check EAF NATS connectivity
            checkNatsService(serviceChecks, overallStatus)

            // Determine overall status
            val finalStatus = determineOverallHealthStatus(overallStatus)

            DetailedHealthResponse(
                status = finalStatus,
                timestamp = Instant.now().toString(),
                service = "ACCI EAF Control Plane",
                version = ResponseConstants.APPLICATION_VERSION,
                serviceChecks = serviceChecks,
                summary = createHealthSummary(serviceChecks, overallStatus),
            )
        }

    /** Get comprehensive system information with validation */
    fun getSystemInfo(): SystemInfoResponse =
        EndpointExceptionHandler.handleEndpointExecution("getSystemInfo", logger) {
            SystemInfoResponse(
                success = true,
                systemInfo = createSystemInfo(),
                metadata = ResponseMetadataFactory.createResponseMetadata("sysinfo"),
            )
        }

    /** Enhanced echo endpoint with input validation */
    fun echo(request: EchoRequest): EchoResponse =
        EndpointExceptionHandler.handleEndpointExecution("echo", logger) {
            validateEchoRequest(request)

            EchoResponse(
                success = true,
                originalMessage = request.message!!,
                timestamp = Instant.now(),
                serverResponse = "Echo from Control Plane at ${Instant.now()}",
                messageLength = request.message.length,
                metadata = ResponseMetadataFactory.createResponseMetadata("echo"),
            )
        }

    // Private helper methods

    private fun checkIamService(
        serviceChecks: MutableMap<String, Any>,
        overallStatus: MutableList<String>,
    ) {
        eafIamHealthIndicator?.let { indicator ->
            val result =
                EndpointExceptionHandler.handleHealthCheckExecution("EAF IAM", logger) {
                    runBlocking { indicator.checkHealth() }
                }

            when (result) {
                is EndpointExceptionHandler.HealthCheckResult.Success -> {
                    serviceChecks["eafIam"] = result.data
                    overallStatus.add(result.data.status)
                }
                is EndpointExceptionHandler.HealthCheckResult.Failure -> {
                    serviceChecks["eafIam"] =
                        mapOf(
                            "status" to result.status,
                            "error" to result.error,
                        )
                    overallStatus.add(result.status)
                }
            }
        }
            ?: run {
                serviceChecks["eafIam"] =
                    mapOf(
                        "status" to "NOT_CONFIGURED",
                        "message" to "EAF IAM health indicator not available",
                    )
            }
    }

    private fun checkNatsService(
        serviceChecks: MutableMap<String, Any>,
        overallStatus: MutableList<String>,
    ) {
        eafNatsHealthIndicator?.let { indicator ->
            val result =
                EndpointExceptionHandler.handleHealthCheckExecution("EAF NATS", logger) {
                    indicator.checkHealth()
                }

            when (result) {
                is EndpointExceptionHandler.HealthCheckResult.Success -> {
                    serviceChecks["eafNats"] = result.data
                    overallStatus.add(result.data.status)
                }
                is EndpointExceptionHandler.HealthCheckResult.Failure -> {
                    serviceChecks["eafNats"] =
                        mapOf(
                            "status" to result.status,
                            "error" to result.error,
                        )
                    overallStatus.add(result.status)
                }
            }
        }
            ?: run {
                serviceChecks["eafNats"] =
                    mapOf(
                        "status" to "NOT_CONFIGURED",
                        "message" to "EAF NATS health indicator not available",
                    )
            }
    }

    private fun determineOverallHealthStatus(overallStatus: List<String>): String =
        when {
            overallStatus.any { it == "DOWN" } -> "DOWN"
            overallStatus.any { it == "DEGRADED" } -> "DEGRADED"
            overallStatus.all { it == "UP" } -> "UP"
            else -> "UNKNOWN"
        }

    private fun createHealthSummary(
        serviceChecks: Map<String, Any>,
        overallStatus: List<String>,
    ): Map<String, Any> =
        mapOf(
            "totalChecks" to serviceChecks.size,
            "upServices" to overallStatus.count { it == "UP" },
            "downServices" to overallStatus.count { it == "DOWN" },
            "configuredServices" to overallStatus.size,
        )

    private fun createSystemInfo(): SystemInfo =
        SystemInfo(
            applicationName = "EAF Control Plane",
            version = ResponseConstants.APPLICATION_VERSION,
            buildTime = ResponseConstants.DEFAULT_BUILD_TIME,
            environment = ResponseConstants.DEFAULT_ENVIRONMENT,
            javaVersion = System.getProperty("java.version"),
            springBootVersion = ResponseConstants.DEFAULT_SPRING_BOOT_VERSION,
            hillaVersion = ResponseConstants.DEFAULT_HILLA_VERSION,
            uptime = calculateUptime(),
        )

    private fun validateEchoRequest(request: EchoRequest) {
        if (request.message.isNullOrBlank()) {
            throw ValidationException("Message cannot be null or blank")
        }

        if (request.message.length > ResponseConstants.MAX_MESSAGE_LENGTH) {
            throw ValidationException(
                "Message too long (max ${ResponseConstants.MAX_MESSAGE_LENGTH} characters)",
            )
        }
    }

    private fun calculateUptime(): String {
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

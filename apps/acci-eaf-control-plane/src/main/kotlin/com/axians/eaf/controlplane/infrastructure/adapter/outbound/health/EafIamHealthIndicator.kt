package com.axians.eaf.controlplane.infrastructure.adapter.outbound.health

import com.axians.eaf.iam.client.EafIamProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.time.Instant

/**
 * Custom health indicator for EAF IAM service connectivity. This indicator performs actual
 * connectivity checks against the configured IAM service.
 *
 * Provides operational monitoring capabilities as required by AC 5 (Operational Monitoring).
 */
@Component
@ConditionalOnBean(EafIamProperties::class)
class EafIamHealthIndicator(
    private val eafIamProperties: EafIamProperties,
    private val restTemplate: RestTemplate = RestTemplate(),
) {
    companion object {
        private const val HEALTH_ENDPOINT = "/actuator/health"
        private const val TIMEOUT_MS = 5000L
    }

    data class HealthStatus(
        val status: String,
        val service: String,
        val lastCheck: String,
        val details: Map<String, Any> = emptyMap(),
    )

    fun checkHealth(): HealthStatus =
        try {
            val startTime = Instant.now()
            val healthResponse = performHealthCheck()
            val responseTime = Duration.between(startTime, Instant.now()).toMillis()

            if (healthResponse != null) {
                HealthStatus(
                    status = "UP",
                    service = "EAF IAM Service",
                    lastCheck = Instant.now().toString(),
                    details =
                        mapOf(
                            "serviceUrl" to eafIamProperties.serviceUrl,
                            "connectivity" to "available",
                            "responseTimeMs" to responseTime,
                            "healthEndpoint" to
                                "${eafIamProperties.serviceUrl}$HEALTH_ENDPOINT",
                            "responseStatus" to
                                (healthResponse["status"] ?: "unknown"),
                        ),
                )
            } else {
                HealthStatus(
                    status = "DOWN",
                    service = "EAF IAM Service",
                    lastCheck = Instant.now().toString(),
                    details =
                        mapOf(
                            "serviceUrl" to eafIamProperties.serviceUrl,
                            "connectivity" to "unavailable",
                            "error" to "Service returned null or invalid response",
                        ),
                )
            }
        } catch (exception: Exception) {
            val errorDetails =
                when (exception) {
                    is RestClientException -> {
                        mapOf(
                            "exceptionType" to "RestClientException",
                            "message" to (exception.message ?: "REST call failed"),
                        )
                    }
                    else -> {
                        mapOf(
                            "exceptionType" to exception::class.simpleName,
                            "message" to (exception.message ?: "Unknown error"),
                        )
                    }
                }

            HealthStatus(
                status = "DOWN",
                service = "EAF IAM Service",
                lastCheck = Instant.now().toString(),
                details =
                    mapOf(
                        "serviceUrl" to eafIamProperties.serviceUrl,
                        "connectivity" to "error",
                        "healthEndpoint" to
                            "${eafIamProperties.serviceUrl}$HEALTH_ENDPOINT",
                        "error" to errorDetails,
                    ),
            )
        }

    /** Performs actual health check against the IAM service health endpoint using RestTemplate. */
    private fun performHealthCheck(): Map<String, Any>? =
        try {
            // Set a timeout for the REST call
            restTemplate.requestFactory?.let { factory ->
                when (factory) {
                    is org.springframework.http.client.SimpleClientHttpRequestFactory -> {
                        factory.setConnectTimeout(TIMEOUT_MS.toInt())
                        factory.setReadTimeout(TIMEOUT_MS.toInt())
                    }
                }
            }

            val healthUrl = "${eafIamProperties.serviceUrl}$HEALTH_ENDPOINT"

            @Suppress("UNCHECKED_CAST")
            val response =
                restTemplate.getForObject(healthUrl, Map::class.java) as? Map<String, Any>

            response
        } catch (e: Exception) {
            // Re-throw to be handled by the main checkHealth method
            throw e
        }
}

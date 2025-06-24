package com.axians.eaf.controlplane.infrastructure.configuration.monitoring

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

/**
 * Configuration for monitoring infrastructure beans including RestTemplate, CircuitBreaker
 * registry, and external service connections.
 */
@Configuration
class MonitoringConfiguration {
    /** RestTemplate for making HTTP calls to external services like EAF IAM */
    @Bean
    fun restTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate =
        restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .build()

    /** CircuitBreaker registry for resilience patterns */
    @Bean fun circuitBreakerRegistry(): CircuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()

    /*
     * NATS connection for messaging health checks Note: This is a basic configuration - should be
     * enhanced with proper connection pooling. Temporarily disabled to fix compilation issues
     */

    // @Bean
    // fun natsConnection(): Connection {
    //     return try {
    //         Nats.connect("nats://localhost:4222")
    //     } catch (ex: Exception) {
    //         // Return a mock/stub connection for development
    //         // In production, this should fail fast or use a proper fallback
    //         throw IllegalStateException("Could not connect to NATS server: ${ex.message}", ex)
    //     }
    // }
}

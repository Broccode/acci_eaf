package com.axians.eaf.eventing.config

import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * Spring Boot auto-configuration for NATS eventing SDK.
 *
 * This configuration provides default beans for NATS connection and event publishing.
 * All beans can be overridden by providing custom implementations.
 */
@Configuration
@EnableConfigurationProperties(NatsEventingProperties::class)
class NatsEventingConfiguration {
    private val logger = LoggerFactory.getLogger(NatsEventingConfiguration::class.java)

    /**
     * Creates a NATS connection bean with configuration from application properties.
     *
     * @param properties NATS configuration properties
     * @return Configured NATS connection
     */
    @Bean
    @ConditionalOnMissingBean
    fun natsConnection(properties: NatsEventingProperties): Connection {
        logger.info("Configuring NATS connection with servers: {}", properties.servers)

        val options =
            Options
                .Builder()
                .servers(properties.servers.toTypedArray())
                .connectionName(properties.connectionName)
                .maxReconnects(properties.maxReconnects)
                .reconnectWait(Duration.ofMillis(properties.reconnectWaitMs))
                .connectionTimeout(Duration.ofMillis(properties.connectionTimeoutMs))
                .pingInterval(Duration.ofMillis(properties.pingIntervalMs))
                .build()

        return try {
            val connection = Nats.connect(options)
            logger.info("Successfully connected to NATS servers: {}", properties.servers)
            connection
        } catch (e: Exception) {
            logger.error("Failed to connect to NATS servers: {}", properties.servers, e)
            throw IllegalStateException("Unable to establish NATS connection", e)
        }
    }
}

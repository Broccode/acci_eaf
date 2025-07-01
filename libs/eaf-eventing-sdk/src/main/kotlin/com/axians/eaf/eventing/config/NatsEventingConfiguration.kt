package com.axians.eaf.eventing.config

import com.axians.eaf.eventing.consumer.IdempotentProjectorService
import com.axians.eaf.eventing.consumer.NatsJetStreamListenerProcessor
import com.axians.eaf.eventing.consumer.ProjectorRegistry
import io.nats.client.Connection
import io.nats.client.JetStream
import io.nats.client.JetStreamApiException
import io.nats.client.Nats
import io.nats.client.Options
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.EnableAsync
import java.time.Duration

/**
 * Spring Boot auto-configuration for NATS eventing SDK.
 *
 * This configuration provides default beans for NATS connection and event publishing. All beans can
 * be overridden by providing custom implementations.
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(NatsEventingProperties::class)
open class NatsEventingConfiguration {
    private val logger = LoggerFactory.getLogger(NatsEventingConfiguration::class.java)

    /**
     * Creates a NATS connection bean with configuration from application properties.
     *
     * @param properties NATS configuration properties
     * @return Configured NATS connection
     */
    @Bean
    @ConditionalOnMissingBean
    open fun natsConnection(properties: NatsEventingProperties): Connection {
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
        } catch (e: java.io.IOException) {
            logger.error("Failed to connect to NATS servers: {}", properties.servers, e)
            throw IllegalStateException("Unable to establish NATS connection", e)
        } catch (e: java.lang.InterruptedException) {
            Thread.currentThread().interrupt()
            logger.error("Interrupted while connecting to NATS servers", e)
            throw IllegalStateException("Interrupted while connecting to NATS", e)
        }
    }

    /**
     * Creates a JetStream context bean for stream-based messaging.
     *
     * @param connection NATS connection
     * @return JetStream context
     */
    @Bean
    @ConditionalOnMissingBean
    open fun jetStream(connection: Connection): JetStream =
        try {
            val jetStream = connection.jetStream()
            logger.info("JetStream context created successfully")
            jetStream
        } catch (e: java.io.IOException) {
            logger.error("Failed to create JetStream context", e)
            throw IllegalStateException("Unable to create JetStream context", e)
        }

    /** Starts NATS JetStream listeners and projectors when the application is ready. */
    @EventListener(ApplicationReadyEvent::class)
    open fun startNatsListeners(event: ApplicationReadyEvent) {
        val applicationContext = event.applicationContext
        val properties = applicationContext.getBean(NatsEventingProperties::class.java)

        if (properties.consumer.autoStartup) {
            logger.info("Starting NATS JetStream listeners...")

            // Try to start regular NATS listeners if processor exists
            try {
                val listenerProcessor =
                    applicationContext.getBean(NatsJetStreamListenerProcessor::class.java)
                listenerProcessor.startListeners()
                logger.info("NATS JetStream listeners started successfully")
            } catch (e: org.springframework.beans.BeansException) {
                logger.info("No NatsJetStreamListenerProcessor found, skipping regular listeners")
            }

            logger.info("Starting EAF projector-based consumers...")
            try {
                val connection = applicationContext.getBean(Connection::class.java)
                val projectorService =
                    applicationContext.getBean(IdempotentProjectorService::class.java)
                logger.debug("Found JetStream and IdempotentProjectorService beans")
                startProjectorConsumers(connection, projectorService, properties)
                logger.info("EAF projector-based consumers started successfully")
            } catch (e: org.springframework.beans.BeansException) {
                logger.warn("Failed to start projector consumers: {}", e.message, e)
            }
        } else {
            logger.info("Auto-startup disabled for NATS JetStream listeners and projectors")
        }
    }

    /** Starts NATS consumers for all registered EAF projectors. */
    private fun startProjectorConsumers(
        connection: Connection,
        projectorService: IdempotentProjectorService,
        properties: NatsEventingProperties,
    ) {
        val projectors = ProjectorRegistry.getAllProjectors()
        logger.info("ProjectorRegistry contains {} projectors", projectors.size)
        if (projectors.isEmpty()) {
            logger.warn("No EAF projectors found to start - ProjectorRegistry is empty!")
            logger.debug(
                "This usually means EafProjectorEventHandlerProcessor hasn't discovered " +
                    "any @EafProjectorEventHandler methods",
            )
            return
        }

        logger.info("Starting {} EAF projector consumers", projectors.size)

        projectors.forEach { projector ->
            try {
                startProjectorConsumer(connection, projectorService, projector, properties)
                logger.info(
                    "Started consumer for projector '{}' on subject '{}'",
                    projector.projectorName,
                    projector.subject,
                )
            } catch (e: java.io.IOException) {
                logger.error(
                    "Failed to start consumer for projector '{}' on subject '{}': {}",
                    projector.projectorName,
                    projector.subject,
                    e.message,
                    e,
                )
                throw IllegalStateException("Failed to start projector consumer", e)
            } catch (e: JetStreamApiException) {
                logger.error(
                    "Failed to start consumer for projector '{}' on subject '{}': {}",
                    projector.projectorName,
                    projector.subject,
                    e.message,
                    e,
                )
                throw IllegalStateException("Failed to start projector consumer", e)
            }
        }
    }

    /** Starts a NATS consumer for a single projector. */
    private fun startProjectorConsumer(
        connection: Connection,
        projectorService: IdempotentProjectorService,
        projector: com.axians.eaf.eventing.consumer.ProjectorDefinition,
        properties: NatsEventingProperties,
    ) {
        val tenantAwareSubject = buildTenantAwareSubject(projector.subject, properties)

        val dispatcher =
            connection.createDispatcher { msg ->
                kotlinx.coroutines.runBlocking {
                    projectorService.processNatsMessage(projector.projectorName, msg)
                }
            }
        dispatcher.subscribe(tenantAwareSubject, projector.durableName)

        logger.debug(
            "Subscription created for projector '{}' with subject '{}' and queue '{}'",
            projector.projectorName,
            tenantAwareSubject,
            projector.durableName,
        )
    }

    /** Builds a tenant-aware subject string. */
    private fun buildTenantAwareSubject(
        subject: String,
        properties: NatsEventingProperties,
    ): String {
        val tenantPrefix = properties.defaultTenantId
        return if (subject.startsWith(tenantPrefix)) {
            subject
        } else {
            "$tenantPrefix.$subject"
        }
    }
}

package com.axians.eaf.eventsourcing.axon

import com.fasterxml.jackson.databind.ObjectMapper
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Spring configuration for EAF Axon Framework 5.0.0-M2 integration.
 *
 * This configuration:
 * - Registers the EAF EventStorageEngine as the primary implementation
 * - Configures proper Jackson ObjectMapper for event serialization
 * - Provides fallback configuration when EAF storage is disabled
 * - Ensures proper Spring Boot auto-configuration integration
 * - Updated for Axon Framework 5.0.0-M2 compatibility
 */
@Configuration
class AxonEventSourcingConfiguration {
    private val logger = LoggerFactory.getLogger(AxonEventSourcingConfiguration::class.java)

    /**
     * Configures the EAF PostgreSQL EventStorageEngine as the primary storage engine.
     *
     * This bean will be used by Axon for all event storage operations, providing:
     * - Multi-tenant event isolation
     * - Optimistic concurrency control
     * - Custom tracking token support
     * - Integration with EAF Event Store SDK
     * - Axon Framework 5.0.0-M2 compatibility with MessageStream API
     */
    @Bean
    @Primary
    @ConditionalOnProperty(
        prefix = "eaf.eventsourcing.axon",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun eafEventStorageEngine(
        eventStoreRepository: com.axians.eaf.eventsourcing.port.EventStoreRepository,
        eventMessageMapper: AxonEventMessageMapper,
        exceptionHandler: AxonExceptionHandler,
    ): EventStorageEngine {
        logger.info(
            "Configuring EAF PostgreSQL EventStorageEngine as primary storage engine for Axon 4.11.2",
        )

        return EafPostgresEventStorageEngine(
            eventStoreRepository = eventStoreRepository,
            eventMessageMapper = eventMessageMapper,
            exceptionHandler = exceptionHandler,
        )
    }

    /**
     * Configures the AxonEventMessageMapper for event translation.
     *
     * Uses the configured Jackson ObjectMapper to ensure consistent serialization between EAF
     * components and Axon Framework 5.0.0-M2.
     */
    @Bean
    @ConditionalOnMissingBean
    fun axonEventMessageMapper(objectMapper: ObjectMapper): AxonEventMessageMapper {
        logger.debug(
            "Configuring AxonEventMessageMapper with Jackson ObjectMapper for Axon 5.0.0-M2",
        )
        return AxonEventMessageMapper(objectMapper)
    }

    /** Configures the AxonExceptionHandler for comprehensive error handling. */
    @Bean
    @ConditionalOnMissingBean
    fun axonExceptionHandler(): AxonExceptionHandler {
        logger.debug("Configuring AxonExceptionHandler for Axon 5.0.0-M2")
        return AxonExceptionHandler()
    }

    /**
     * Fallback EventStorageEngine when EAF storage is disabled.
     *
     * This provides an in-memory storage engine for testing or development scenarios where the full
     * EAF event store is not available.
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "eaf.eventsourcing.axon",
        name = ["enabled"],
        havingValue = "false",
    )
    @ConditionalOnMissingBean
    fun fallbackEventStorageEngine(): EventStorageEngine {
        logger.warn(
            "EAF EventStorageEngine disabled - using fallback InMemoryEventStorageEngine for Axon 5.0.0-M2",
        )
        return InMemoryEventStorageEngine()
    }
}

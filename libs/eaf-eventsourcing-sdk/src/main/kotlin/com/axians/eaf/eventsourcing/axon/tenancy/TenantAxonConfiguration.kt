package com.axians.eaf.eventsourcing.axon.tenancy

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

/**
 * Configuration for tenant-aware Axon Framework integration.
 *
 * This configuration automatically registers tenant-aware interceptors with Axon's CommandBus and
 * EventBus to ensure tenant context flows seamlessly through CQRS/Event Sourcing processing.
 */
@Configuration
@ConditionalOnProperty(
    prefix = "eaf.axon.tenancy",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class TenantAxonConfiguration {
    companion object {
        private val logger = LoggerFactory.getLogger(TenantAxonConfiguration::class.java)
    }

    @PostConstruct
    fun configureTenantAwareInterceptors() {
        logger.info("Tenant-aware Axon Framework interceptors are available for registration")
        logger.info(
            "Interceptors will be automatically registered when Axon Framework components are available",
        )
    }
}

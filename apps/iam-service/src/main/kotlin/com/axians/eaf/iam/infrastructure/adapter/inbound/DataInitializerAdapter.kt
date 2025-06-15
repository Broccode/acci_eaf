package com.axians.eaf.iam.infrastructure.adapter.inbound

import com.axians.eaf.iam.application.service.SystemInitializationService
import com.axians.eaf.iam.infrastructure.config.SystemInitializationProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * Application runner that triggers system initialization on startup.
 * This adapter is responsible for creating default tenant and superadmin user
 * when the system is configured for single-tenant deployment.
 */
@Component
class DataInitializerAdapter(
    private val systemInitializationService: SystemInitializationService,
    private val properties: SystemInitializationProperties,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(DataInitializerAdapter::class.java)

    override fun run(args: ApplicationArguments?) {
        if (!properties.initializeDefaultTenant) {
            logger.debug("System initialization is disabled via configuration")
            return
        }

        logger.info("Starting system initialization process...")

        try {
            val result = systemInitializationService.initializeDefaultTenantIfRequired()

            if (result.wasInitialized) {
                logger.info("System initialization completed: ${result.message}")
            } else {
                logger.info("System initialization skipped: ${result.message}")
            }
        } catch (exception: Exception) {
            logger.error("System initialization failed", exception)
            // Don't rethrow - allow application to start even if initialization fails
            // This prevents the application from failing to start due to initialization issues
        }
    }
}

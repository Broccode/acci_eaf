package com.axians.eaf.iam.application.service

import com.axians.eaf.iam.application.port.outbound.SaveTenantPort
import com.axians.eaf.iam.domain.model.Tenant
import com.axians.eaf.iam.domain.model.User
import com.axians.eaf.iam.infrastructure.config.SystemInitializationProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Application service responsible for system initialization.
 * This service orchestrates the creation of default tenant and superadmin user
 * for single-tenant deployments or initial system setup.
 */
@Service
@Transactional
class SystemInitializationService(
    private val saveTenantPort: SaveTenantPort,
    private val properties: SystemInitializationProperties,
) {
    private val logger = LoggerFactory.getLogger(SystemInitializationService::class.java)

    /**
     * Initialize the system with default tenant and superadmin user if required.
     * This operation is idempotent - it will not create duplicates if the system
     * is already initialized.
     *
     * @return InitializationResult indicating whether initialization was performed
     */
    fun initializeDefaultTenantIfRequired(): InitializationResult {
        logger.info("Checking if system initialization is required...")

        // Check if default tenant already exists
        if (saveTenantPort.existsByTenantName(properties.defaultTenantName)) {
            val message = "System already initialized - default tenant '${properties.defaultTenantName}' exists"
            logger.info(message)
            return InitializationResult(wasInitialized = false, message = message)
        }

        // Check if superadmin email already exists
        if (saveTenantPort.existsByEmail(properties.defaultSuperAdminEmail)) {
            val message = "System already initialized - superadmin email '${properties.defaultSuperAdminEmail}' exists"
            logger.info(message)
            return InitializationResult(wasInitialized = false, message = message)
        }

        // Create default tenant and superadmin user
        logger.info(
            "Initializing system with default tenant '${properties.defaultTenantName}' and superadmin '${properties.defaultSuperAdminEmail}'",
        )

        val defaultTenant = Tenant.create(properties.defaultTenantName)
        val superAdminUser =
            User.createSuperAdmin(
                tenantId = defaultTenant.tenantId,
                email = properties.defaultSuperAdminEmail,
            )

        try {
            val result = saveTenantPort.saveTenantWithAdmin(defaultTenant, superAdminUser)

            logger.info("System initialization completed successfully")
            logger.info("Created default tenant: ${result.savedTenant.name} (ID: ${result.savedTenant.tenantId})")
            logger.info(
                "Created superadmin user: ${result.savedTenantAdmin.email} (ID: ${result.savedTenantAdmin.userId})",
            )

            // Log invitation details as placeholder for AC 5 (email notification)
            logger.info("INVITATION PLACEHOLDER: SuperAdmin account created for ${result.savedTenantAdmin.email}")
            logger.info("INVITATION PLACEHOLDER: Please set up password and access tenant '${result.savedTenant.name}'")

            return InitializationResult(
                wasInitialized = true,
                message = properties.defaultTenantName,
            )
        } catch (exception: Exception) {
            logger.error("Failed to initialize system", exception)
            throw exception
        }
    }
}

/**
 * Result of system initialization operation.
 */
data class InitializationResult(
    val wasInitialized: Boolean,
    val message: String,
)

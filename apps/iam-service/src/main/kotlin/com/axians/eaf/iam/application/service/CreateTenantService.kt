package com.axians.eaf.iam.application.service

import com.axians.eaf.iam.application.port.inbound.CreateTenantCommand
import com.axians.eaf.iam.application.port.inbound.CreateTenantResult
import com.axians.eaf.iam.application.port.inbound.CreateTenantUseCase
import com.axians.eaf.iam.application.port.outbound.SaveTenantPort
import com.axians.eaf.iam.domain.model.Tenant
import com.axians.eaf.iam.domain.model.User
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Application service that implements the Create Tenant use case. This service orchestrates the
 * domain logic and uses outbound ports for external interactions.
 */
@Service
@Transactional
class CreateTenantService(
    private val saveTenantPort: SaveTenantPort,
) : CreateTenantUseCase {
    private val logger = LoggerFactory.getLogger(CreateTenantService::class.java)

    /**
     * Handles the tenant creation command by:
     * 1. Validating the command input
     * 2. Checking for uniqueness constraints
     * 3. Creating domain entities
     * 4. Saving via outbound port
     * 5. Logging invitation details (placeholder for AC 5)
     */
    override fun handle(command: CreateTenantCommand): CreateTenantResult {
        logger.info("Processing tenant creation for tenant name: {}", command.tenantName)

        // Validate command input
        validateCommand(command)

        val trimmedTenantName = command.tenantName.trim()
        val trimmedEmail = command.initialAdminEmail.trim().lowercase()

        // Check uniqueness constraints
        checkUniquenessConstraints(trimmedTenantName, trimmedEmail)

        // Create domain entities using factory methods
        val tenant = Tenant.create(trimmedTenantName)
        val tenantAdmin = User.createTenantAdmin(tenant.tenantId, trimmedEmail)

        // Save entities via outbound port
        val saveResult = saveTenantPort.saveTenantWithAdmin(tenant, tenantAdmin)

        // Log invitation details (placeholder for email notification - AC 5)
        val invitationDetails = "Invitation link sent to $trimmedEmail for tenant $trimmedTenantName"
        logger.info(
            "Tenant created successfully: {} with admin: {}. {}",
            tenant.tenantId,
            tenantAdmin.userId,
            invitationDetails,
        )

        return CreateTenantResult(
            tenantId = saveResult.savedTenant.tenantId,
            tenantName = saveResult.savedTenant.name,
            tenantAdminUserId = saveResult.savedTenantAdmin.userId,
            tenantAdminEmail = saveResult.savedTenantAdmin.email,
            invitationDetails = invitationDetails,
        )
    }

    private fun validateCommand(command: CreateTenantCommand) {
        require(command.tenantName.isNotBlank()) { "Tenant name cannot be blank" }
        require(command.initialAdminEmail.isNotBlank()) { "Admin email cannot be blank" }
        require(isValidEmail(command.initialAdminEmail)) { "Email must be valid" }
    }

    private fun checkUniquenessConstraints(
        tenantName: String,
        email: String,
    ) {
        if (saveTenantPort.existsByTenantName(tenantName)) {
            throw IllegalArgumentException("Tenant with name '$tenantName' already exists")
        }

        if (saveTenantPort.existsByEmail(email)) {
            throw IllegalArgumentException("User with email '$email' already exists")
        }
    }

    private fun isValidEmail(email: String): Boolean = email.contains("@") && email.length <= 320
}

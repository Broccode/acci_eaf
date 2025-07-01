package com.axians.eaf.iam.infrastructure.adapter.outbound.persistence

import com.axians.eaf.iam.application.port.outbound.SaveTenantPort
import com.axians.eaf.iam.application.port.outbound.SaveTenantResult
import com.axians.eaf.iam.domain.model.Tenant
import com.axians.eaf.iam.domain.model.User
import com.axians.eaf.iam.infrastructure.adapter.outbound.persistence.entity.toDomain
import com.axians.eaf.iam.infrastructure.adapter.outbound.persistence.entity.toEntity
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Outbound adapter that implements the SaveTenantPort using JPA repositories. This adapter handles
 * all persistence concerns for tenant and user entities.
 */
@Component
@Transactional
class TenantPersistenceAdapter(
    private val tenantRepository: TenantJpaRepository,
    private val userRepository: UserJpaRepository,
) : SaveTenantPort {
    private val logger = LoggerFactory.getLogger(TenantPersistenceAdapter::class.java)

    /**
     * Save a new tenant and its initial administrator in a single transaction.
     *
     * @param tenant The tenant to save
     * @param tenantAdmin The initial tenant administrator user
     * @return A result containing the saved entities
     */
    override fun saveTenantWithAdmin(
        tenant: Tenant,
        tenantAdmin: User,
    ): SaveTenantResult {
        logger.debug("Saving tenant {} with admin user {}", tenant.tenantId, tenantAdmin.userId)

        // Verify tenant and user are associated
        require(tenant.tenantId == tenantAdmin.tenantId) {
            "Tenant ID mismatch: tenant=${tenant.tenantId}, user.tenantId=${tenantAdmin.tenantId}"
        }

        try {
            // Save tenant first
            val savedTenantEntity = tenantRepository.save(tenant.toEntity())
            logger.debug("Saved tenant: {}", savedTenantEntity.tenantId)

            // Save tenant admin
            val savedAdminEntity = userRepository.save(tenantAdmin.toEntity())
            logger.debug("Saved tenant admin: {}", savedAdminEntity.userId)

            return SaveTenantResult(savedTenantEntity.toDomain(), savedAdminEntity.toDomain())
        } catch (dataException: DataAccessException) {
            logger.error(
                "Database error while saving tenant {} with admin {}: {}",
                tenant.tenantId,
                tenantAdmin.userId,
                dataException.message,
                dataException,
            )
            throw dataException
        } catch (validationException: IllegalArgumentException) {
            logger.error(
                "Validation error while saving tenant {} with admin {}: {}",
                tenant.tenantId,
                tenantAdmin.userId,
                validationException.message,
                validationException,
            )
            throw validationException
        }
    }

    /**
     * Check if a tenant with the given name already exists (case-sensitive).
     *
     * @param tenantName The name to check for uniqueness
     * @return true if a tenant with this name exists, false otherwise
     */
    override fun existsByTenantName(tenantName: String): Boolean {
        logger.debug("Checking if tenant name exists: {}", tenantName)
        return tenantRepository.existsByName(tenantName)
    }

    /**
     * Check if a user with the given email already exists across all tenants (case-insensitive).
     *
     * @param email The email to check for uniqueness
     * @return true if a user with this email exists, false otherwise
     */
    override fun existsByEmail(email: String): Boolean {
        logger.debug("Checking if email exists: {}", email)
        return userRepository.existsByEmailIgnoreCase(email)
    }
}

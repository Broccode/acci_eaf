package com.axians.eaf.iam.infrastructure.adapter.outbound.persistence

import com.axians.eaf.iam.application.port.outbound.FindUsersByTenantIdPort
import com.axians.eaf.iam.domain.model.User
import com.axians.eaf.iam.infrastructure.adapter.outbound.persistence.entity.toDomain
import com.axians.eaf.iam.infrastructure.adapter.outbound.persistence.entity.toEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Persistence adapter for user-related operations.
 * This adapter implements the FindUsersByTenantIdPort and provides
 * the concrete implementation for user persistence operations.
 */
@Component
class UserPersistenceAdapter(
    private val userJpaRepository: UserJpaRepository,
) : FindUsersByTenantIdPort {
    private val logger = LoggerFactory.getLogger(UserPersistenceAdapter::class.java)

    override fun findUsersByTenantId(tenantId: String): List<User> {
        logger.debug("Finding users for tenant: {}", tenantId)

        val userEntities = userJpaRepository.findByTenantId(tenantId)
        val users = userEntities.map { it.toDomain() }

        logger.debug("Found {} users for tenant: {}", users.size, tenantId)
        return users
    }

    override fun findUserByIdAndTenantId(
        userId: String,
        tenantId: String,
    ): User? {
        logger.debug("Finding user with ID: {} in tenant: {}", userId, tenantId)

        val userEntity = userJpaRepository.findById(userId).orElse(null)

        return if (userEntity != null && userEntity.tenantId == tenantId) {
            logger.debug("Found user with ID: {} in tenant: {}", userId, tenantId)
            userEntity.toDomain()
        } else {
            logger.debug("User with ID: {} not found in tenant: {}", userId, tenantId)
            null
        }
    }

    override fun saveUser(user: User): User {
        logger.debug("Saving user with ID: {} in tenant: {}", user.userId, user.tenantId)

        val userEntity = user.toEntity()
        val savedEntity = userJpaRepository.save(userEntity)
        val savedUser = savedEntity.toDomain()

        logger.debug("Successfully saved user with ID: {} in tenant: {}", savedUser.userId, savedUser.tenantId)
        return savedUser
    }
}

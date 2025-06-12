package com.axians.eaf.iam.application.service

import com.axians.eaf.iam.application.port.inbound.ListUsersInTenantQuery
import com.axians.eaf.iam.application.port.inbound.ListUsersInTenantResult
import com.axians.eaf.iam.application.port.inbound.ListUsersInTenantUseCase
import com.axians.eaf.iam.application.port.inbound.UserSummary
import com.axians.eaf.iam.application.port.outbound.FindUsersByTenantIdPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Application service for listing users within a tenant.
 * This service retrieves all users belonging to a specific tenant.
 */
@Service
class ListUsersInTenantService(
    private val findUsersByTenantIdPort: FindUsersByTenantIdPort,
) : ListUsersInTenantUseCase {
    private val logger = LoggerFactory.getLogger(ListUsersInTenantService::class.java)

    override fun listUsers(query: ListUsersInTenantQuery): ListUsersInTenantResult {
        logger.debug("Listing users for tenant: {}", query.tenantId)

        // Validate input
        require(query.tenantId.isNotBlank()) { "Tenant ID cannot be blank" }

        // Find all users in the tenant
        val users = findUsersByTenantIdPort.findUsersByTenantId(query.tenantId)

        logger.debug("Found {} users in tenant: {}", users.size, query.tenantId)

        // Convert to summary objects
        val userSummaries =
            users.map { user ->
                UserSummary(
                    userId = user.userId,
                    email = user.email,
                    username = user.username,
                    role = user.role.name,
                    status = user.status.name,
                )
            }

        return ListUsersInTenantResult(
            tenantId = query.tenantId,
            users = userSummaries,
        )
    }
}

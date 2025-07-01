package com.axians.eaf.controlplane.infrastructure.adapter.outbound.iam

import com.axians.eaf.controlplane.application.dto.tenant.PagedResponse
import com.axians.eaf.controlplane.application.dto.user.UserFilter
import com.axians.eaf.controlplane.application.dto.user.UserSummary
import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.User
import com.axians.eaf.controlplane.domain.model.user.UserId
import com.axians.eaf.controlplane.domain.model.user.UserStatus
import com.axians.eaf.controlplane.domain.port.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Repository
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.springframework.web.client.postForObject
import java.time.Instant

/**
 * IAM Service adapter implementing UserRepository port.
 *
 * Delegates all user management operations to the IAM Service via HTTP calls. This maintains proper
 * separation of concerns where the Control Plane focuses on administrative operations while user
 * persistence is handled by the IAM Service.
 */
@Repository
class IamUserRepositoryAdapter(
    private val restTemplate: RestTemplate,
    @Value("\${eaf.iam.service-url:http://localhost:8081}") private val iamServiceUrl: String,
) : UserRepository {
    private val logger = LoggerFactory.getLogger(IamUserRepositoryAdapter::class.java)

    override suspend fun save(user: User): User {
        val url = "$iamServiceUrl/api/users"
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val request = HttpEntity(user.toIamRequest(), headers)

        return try {
            val response = restTemplate.postForObject<IamUserResponse>(url, request)
            response?.toDomainUser()
                ?: throw IamServiceOperationFailedException(
                    operation = "save user",
                    reason = "IAM service returned null response",
                )
        } catch (e: HttpClientErrorException) {
            logger.error(
                "Failed to save user to IAM service. Status: ${e.statusCode}, Body: ${e.responseBodyAsString}",
            )
            throw IamServiceErrorResponseException(
                statusCode = e.statusCode.value(),
                message = "Failed to save user: ${e.message}",
                cause = e,
            )
        } catch (e: RestClientException) {
            logger.error("Communication failure with IAM service while saving user", e)
            throw IamServiceCommunicationException(
                operation = "save user",
                cause = e,
            )
        }
    }

    override suspend fun findById(userId: UserId): User? =
        try {
            val url = "$iamServiceUrl/api/users/${userId.value}"
            val response = restTemplate.getForObject<IamUserResponse>(url)
            response?.toDomainUser()
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                logger.debug("User not found with ID: ${userId.value}")
                null
            } else {
                logger.error(
                    "Error retrieving user by ID from IAM service. Status: ${e.statusCode}",
                )
                throw IamServiceErrorResponseException(
                    statusCode = e.statusCode.value(),
                    message = "Failed to find user by ID: ${e.message}",
                    cause = e,
                )
            }
        } catch (e: RestClientException) {
            logger.error(
                "Communication failure with IAM service while finding user by ID: ${userId.value}",
                e,
            )
            throw IamServiceCommunicationException(
                operation = "find user by ID",
                cause = e,
            )
        }

    override suspend fun findByEmailAndTenantId(
        email: String,
        tenantId: TenantId,
    ): User? =
        try {
            val url =
                "$iamServiceUrl/api/users/by-email-and-tenant?email=$email&tenantId=${tenantId.value}"
            val response = restTemplate.getForObject<IamUserResponse>(url)
            response?.toDomainUser()
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                logger.debug("User not found with email: $email and tenant: ${tenantId.value}")
                null
            } else {
                logger.error(
                    "Error retrieving user by email and tenant from IAM service. Status: ${e.statusCode}",
                )
                throw IamServiceErrorResponseException(
                    statusCode = e.statusCode.value(),
                    message = "Failed to find user by email and tenant: ${e.message}",
                    cause = e,
                )
            }
        } catch (e: RestClientException) {
            logger.error(
                "Communication failure with IAM service while finding user by email and tenant",
                e,
            )
            throw IamServiceCommunicationException(
                operation = "find user by email and tenant",
                cause = e,
            )
        }

    override suspend fun findByEmail(email: String): User? =
        try {
            val url = "$iamServiceUrl/api/users/by-email?email=$email"
            val response = restTemplate.getForObject<IamUserResponse>(url)
            response?.toDomainUser()
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                logger.debug("User not found with email: $email")
                null
            } else {
                logger.error(
                    "Error retrieving user by email from IAM service. Status: ${e.statusCode}",
                )
                throw IamServiceErrorResponseException(
                    statusCode = e.statusCode.value(),
                    message = "Failed to find user by email: ${e.message}",
                    cause = e,
                )
            }
        } catch (e: RestClientException) {
            logger.error(
                "Communication failure with IAM service while finding user by email: $email",
                e,
            )
            throw IamServiceCommunicationException(
                operation = "find user by email",
                cause = e,
            )
        }

    override suspend fun existsById(userId: UserId): Boolean =
        try {
            val url = "$iamServiceUrl/api/users/${userId.value}/exists"
            restTemplate.getForObject<Boolean>(url) ?: false
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                false
            } else {
                logger.error(
                    "Error checking user existence in IAM service. Status: ${e.statusCode}",
                )
                throw IamServiceErrorResponseException(
                    statusCode = e.statusCode.value(),
                    message = "Failed to check user existence: ${e.message}",
                    cause = e,
                )
            }
        } catch (e: RestClientException) {
            logger.error(
                "Communication failure with IAM service while checking user existence",
                e,
            )
            throw IamServiceCommunicationException(
                operation = "check user existence",
                cause = e,
            )
        }

    override suspend fun existsByEmailAndTenantId(
        email: String,
        tenantId: TenantId,
    ): Boolean =
        try {
            val url = "$iamServiceUrl/api/users/exists?email=$email&tenantId=${tenantId.value}"
            restTemplate.getForObject<Boolean>(url) ?: false
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                false
            } else {
                logger.error(
                    "Error checking user existence by email and tenant. Status: ${e.statusCode}",
                )
                throw IamServiceErrorResponseException(
                    statusCode = e.statusCode.value(),
                    message =
                        "Failed to check user existence by email and tenant: ${e.message}",
                    cause = e,
                )
            }
        } catch (e: RestClientException) {
            logger.error(
                "Communication failure with IAM service while checking user existence by email and tenant",
                e,
            )
            throw IamServiceCommunicationException(
                operation = "check user existence by email and tenant",
                cause = e,
            )
        }

    override suspend fun findAll(filter: UserFilter): PagedResponse<UserSummary> =
        try {
            val url = "$iamServiceUrl/api/users/search"
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val request = HttpEntity(filter, headers)

            val response = restTemplate.postForObject<PagedResponse<UserSummary>>(url, request)
            response ?: PagedResponse.of<UserSummary>(emptyList(), 0, 10, 0)
        } catch (e: HttpClientErrorException) {
            logger.error("Error searching users in IAM service. Status: ${e.statusCode}")
            throw IamServiceErrorResponseException(
                statusCode = e.statusCode.value(),
                message = "Failed to search users: ${e.message}",
                cause = e,
            )
        } catch (e: RestClientException) {
            logger.error("Communication failure with IAM service while searching users", e)
            throw IamServiceCommunicationException(
                operation = "search users",
                cause = e,
            )
        }

    override suspend fun findByTenantId(tenantId: TenantId): List<User> =
        try {
            val url = "$iamServiceUrl/api/users/by-tenant/${tenantId.value}"
            val responses = restTemplate.getForObject<List<IamUserResponse>>(url) ?: emptyList()
            responses.map { it.toDomainUser() }
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                logger.debug("No users found for tenant: ${tenantId.value}")
                emptyList()
            } else {
                logger.error("Error retrieving users by tenant. Status: ${e.statusCode}")
                throw IamServiceErrorResponseException(
                    statusCode = e.statusCode.value(),
                    message = "Failed to find users by tenant: ${e.message}",
                    cause = e,
                )
            }
        } catch (e: RestClientException) {
            logger.error(
                "Communication failure with IAM service while finding users by tenant",
                e,
            )
            throw IamServiceCommunicationException(
                operation = "find users by tenant",
                cause = e,
            )
        }

    override suspend fun findByTenantIdAndStatus(
        tenantId: TenantId,
        status: UserStatus,
    ): List<User> =
        try {
            val url = "$iamServiceUrl/api/users/by-tenant/${tenantId.value}?status=$status"
            val responses = restTemplate.getForObject<List<IamUserResponse>>(url) ?: emptyList()
            responses.map { it.toDomainUser() }
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                logger.debug(
                    "No users found for tenant: ${tenantId.value} with status: $status",
                )
                emptyList()
            } else {
                logger.error(
                    "Error retrieving users by tenant and status. Status: ${e.statusCode}",
                )
                throw IamServiceErrorResponseException(
                    statusCode = e.statusCode.value(),
                    message = "Failed to find users by tenant and status: ${e.message}",
                    cause = e,
                )
            }
        } catch (e: RestClientException) {
            logger.error(
                "Communication failure with IAM service while finding users by tenant and status",
                e,
            )
            throw IamServiceCommunicationException(
                operation = "find users by tenant and status",
                cause = e,
            )
        }

    override suspend fun findByIds(userIds: Set<UserId>): List<User> =
        try {
            val url = "$iamServiceUrl/api/users/by-ids"
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val request = HttpEntity(userIds.map { it.value }, headers)

            val responses =
                restTemplate.postForObject<List<IamUserResponse>>(url, request)
                    ?: emptyList()
            responses.map { it.toDomainUser() }
        } catch (e: HttpClientErrorException) {
            logger.error("Error retrieving users by IDs. Status: ${e.statusCode}")
            throw IamServiceErrorResponseException(
                statusCode = e.statusCode.value(),
                message = "Failed to find users by IDs: ${e.message}",
                cause = e,
            )
        } catch (e: RestClientException) {
            logger.error("Communication failure with IAM service while finding users by IDs", e)
            throw IamServiceCommunicationException(
                operation = "find users by IDs",
                cause = e,
            )
        }

    override suspend fun countByTenantId(tenantId: TenantId): Long =
        try {
            val url = "$iamServiceUrl/api/users/count/by-tenant/${tenantId.value}"
            restTemplate.getForObject<Long>(url) ?: 0L
        } catch (e: HttpClientErrorException) {
            logger.error("Error counting users by tenant. Status: ${e.statusCode}")
            throw IamServiceErrorResponseException(
                statusCode = e.statusCode.value(),
                message = "Failed to count users by tenant: ${e.message}",
                cause = e,
            )
        } catch (e: RestClientException) {
            logger.error(
                "Communication failure with IAM service while counting users by tenant",
                e,
            )
            throw IamServiceCommunicationException(
                operation = "count users by tenant",
                cause = e,
            )
        }

    override suspend fun countByTenantIdAndStatus(
        tenantId: TenantId,
        status: UserStatus,
    ): Long =
        try {
            val url =
                "$iamServiceUrl/api/users/count/by-tenant/${tenantId.value}?status=$status"
            restTemplate.getForObject<Long>(url) ?: 0L
        } catch (e: HttpClientErrorException) {
            logger.error("Error counting users by tenant and status. Status: ${e.statusCode}")
            throw IamServiceErrorResponseException(
                statusCode = e.statusCode.value(),
                message = "Failed to count users by tenant and status: ${e.message}",
                cause = e,
            )
        } catch (e: RestClientException) {
            logger.error(
                "Communication failure with IAM service while counting users by tenant and status",
                e,
            )
            throw IamServiceCommunicationException(
                operation = "count users by tenant and status",
                cause = e,
            )
        }

    override suspend fun findInactiveUsers(
        tenantId: TenantId,
        sinceDate: Instant,
    ): List<User> =
        try {
            val url =
                "$iamServiceUrl/api/users/inactive/by-tenant/${tenantId.value}?since=${sinceDate.epochSecond}"
            val responses = restTemplate.getForObject<List<IamUserResponse>>(url) ?: emptyList()
            responses.map { it.toDomainUser() }
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                logger.debug("No inactive users found for tenant: ${tenantId.value}")
                emptyList()
            } else {
                logger.error("Error retrieving inactive users. Status: ${e.statusCode}")
                throw IamServiceErrorResponseException(
                    statusCode = e.statusCode.value(),
                    message = "Failed to find inactive users: ${e.message}",
                    cause = e,
                )
            }
        } catch (e: RestClientException) {
            logger.error(
                "Communication failure with IAM service while finding inactive users",
                e,
            )
            throw IamServiceCommunicationException(
                operation = "find inactive users",
                cause = e,
            )
        }

    override suspend fun deleteById(userId: UserId): Boolean =
        try {
            val url = "$iamServiceUrl/api/users/${userId.value}"
            restTemplate.delete(url)
            true
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                logger.debug("User not found for deletion: ${userId.value}")
                false
            } else {
                logger.error("Error deleting user. Status: ${e.statusCode}")
                throw IamServiceErrorResponseException(
                    statusCode = e.statusCode.value(),
                    message = "Failed to delete user: ${e.message}",
                    cause = e,
                )
            }
        } catch (e: RestClientException) {
            logger.error("Communication failure with IAM service while deleting user", e)
            throw IamServiceCommunicationException(
                operation = "delete user",
                cause = e,
            )
        }

    override suspend fun deleteByTenantId(tenantId: TenantId): Int =
        try {
            val url = "$iamServiceUrl/api/users/by-tenant/${tenantId.value}"
            val response = restTemplate.exchange(url, HttpMethod.DELETE, null, Int::class.java)
            response.body ?: 0
        } catch (e: HttpClientErrorException) {
            logger.error("Error deleting users by tenant. Status: ${e.statusCode}")
            throw IamServiceErrorResponseException(
                statusCode = e.statusCode.value(),
                message = "Failed to delete users by tenant: ${e.message}",
                cause = e,
            )
        } catch (e: RestClientException) {
            logger.error(
                "Communication failure with IAM service while deleting users by tenant",
                e,
            )
            throw IamServiceCommunicationException(
                operation = "delete users by tenant",
                cause = e,
            )
        }
}

/** IAM Service request DTO for user operations */
data class IamUserRequest(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val tenantId: String,
    val status: UserStatus,
    val roles: List<String> = emptyList(),
)

/** IAM Service response DTO for user operations */
data class IamUserResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val tenantId: String,
    val status: UserStatus,
    val roles: List<String> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastLogin: Instant? = null,
) {
    fun toDomainUser(): User {
        // Note: For now, we'll create a simplified User object
        // In a real implementation, we'd need to fetch role details from the role service
        return User
            .createActive(
                tenantId = TenantId.fromString(tenantId),
                email = email,
                firstName = firstName,
                lastName = lastName,
                initialRoles = emptySet(), // Roles would be resolved separately
                now = createdAt,
            ).copy(
                id = UserId.fromString(id),
                status = status,
                lastLogin = lastLogin,
                lastModified = updatedAt,
            )
    }
}

/** Extension function to convert domain User to IAM request */
private fun User.toIamRequest(): IamUserRequest =
    IamUserRequest(
        id = id.value,
        email = email,
        firstName = firstName,
        lastName = lastName,
        tenantId = tenantId.value,
        status = status,
        roles = roles.map { it.id.value },
    )

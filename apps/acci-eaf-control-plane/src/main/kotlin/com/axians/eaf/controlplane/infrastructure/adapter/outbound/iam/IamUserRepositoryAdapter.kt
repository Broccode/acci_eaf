package com.axians.eaf.controlplane.infrastructure.adapter.outbound.iam

import com.axians.eaf.controlplane.application.dto.tenant.PagedResponse
import com.axians.eaf.controlplane.application.dto.user.UserFilter
import com.axians.eaf.controlplane.application.dto.user.UserSummary
import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.User
import com.axians.eaf.controlplane.domain.model.user.UserId
import com.axians.eaf.controlplane.domain.model.user.UserStatus
import com.axians.eaf.controlplane.domain.port.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Repository
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
    override suspend fun save(user: User): User {
        val url = "$iamServiceUrl/api/users"
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val request = HttpEntity(user.toIamRequest(), headers)

        val response = restTemplate.postForObject<IamUserResponse>(url, request)
        return response?.toDomainUser() ?: throw RuntimeException("Failed to save user to IAM service")
    }

    override suspend fun findById(userId: UserId): User? =
        try {
            val url = "$iamServiceUrl/api/users/${userId.value}"
            val response = restTemplate.getForObject<IamUserResponse>(url)
            response?.toDomainUser()
        } catch (e: Exception) {
            null // User not found
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
        } catch (e: Exception) {
            null // User not found
        }

    override suspend fun findByEmail(email: String): User? =
        try {
            val url = "$iamServiceUrl/api/users/by-email?email=$email"
            val response = restTemplate.getForObject<IamUserResponse>(url)
            response?.toDomainUser()
        } catch (e: Exception) {
            null // User not found
        }

    override suspend fun existsById(userId: UserId): Boolean =
        try {
            val url = "$iamServiceUrl/api/users/${userId.value}/exists"
            restTemplate.getForObject<Boolean>(url) ?: false
        } catch (e: Exception) {
            false
        }

    override suspend fun existsByEmailAndTenantId(
        email: String,
        tenantId: TenantId,
    ): Boolean =
        try {
            val url = "$iamServiceUrl/api/users/exists?email=$email&tenantId=${tenantId.value}"
            restTemplate.getForObject<Boolean>(url) ?: false
        } catch (e: Exception) {
            false
        }

    override suspend fun findAll(filter: UserFilter): PagedResponse<UserSummary> =
        try {
            val url = "$iamServiceUrl/api/users/search"
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val request = HttpEntity(filter, headers)

            val response = restTemplate.postForObject<PagedResponse<UserSummary>>(url, request)
            response ?: PagedResponse.of<UserSummary>(emptyList(), 0, 10, 0)
        } catch (e: Exception) {
            PagedResponse.of<UserSummary>(emptyList(), 0, 10, 0)
        }

    override suspend fun findByTenantId(tenantId: TenantId): List<User> =
        try {
            val url = "$iamServiceUrl/api/users/by-tenant/${tenantId.value}"
            val responses = restTemplate.getForObject<List<IamUserResponse>>(url) ?: emptyList()
            responses.map { it.toDomainUser() }
        } catch (e: Exception) {
            emptyList()
        }

    override suspend fun findByTenantIdAndStatus(
        tenantId: TenantId,
        status: UserStatus,
    ): List<User> =
        try {
            val url = "$iamServiceUrl/api/users/by-tenant/${tenantId.value}?status=$status"
            val responses = restTemplate.getForObject<List<IamUserResponse>>(url) ?: emptyList()
            responses.map { it.toDomainUser() }
        } catch (e: Exception) {
            emptyList()
        }

    override suspend fun findByIds(userIds: Set<UserId>): List<User> =
        try {
            val url = "$iamServiceUrl/api/users/by-ids"
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val request = HttpEntity(userIds.map { it.value }, headers)

            val responses =
                restTemplate.postForObject<List<IamUserResponse>>(url, request) ?: emptyList()
            responses.map { it.toDomainUser() }
        } catch (e: Exception) {
            emptyList()
        }

    override suspend fun countByTenantId(tenantId: TenantId): Long =
        try {
            val url = "$iamServiceUrl/api/users/count/by-tenant/${tenantId.value}"
            restTemplate.getForObject<Long>(url) ?: 0L
        } catch (e: Exception) {
            0L
        }

    override suspend fun countByTenantIdAndStatus(
        tenantId: TenantId,
        status: UserStatus,
    ): Long =
        try {
            val url = "$iamServiceUrl/api/users/count/by-tenant/${tenantId.value}?status=$status"
            restTemplate.getForObject<Long>(url) ?: 0L
        } catch (e: Exception) {
            0L
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
        } catch (e: Exception) {
            emptyList()
        }

    override suspend fun deleteById(userId: UserId): Boolean =
        try {
            val url = "$iamServiceUrl/api/users/${userId.value}"
            restTemplate.delete(url)
            true
        } catch (e: Exception) {
            false
        }

    override suspend fun deleteByTenantId(tenantId: TenantId): Int =
        try {
            val url = "$iamServiceUrl/api/users/by-tenant/${tenantId.value}"
            val response = restTemplate.exchange(url, HttpMethod.DELETE, null, Int::class.java)
            response.body ?: 0
        } catch (e: Exception) {
            0
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

package com.axians.eaf.iam.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * HTTP client for communicating with the IAM service REST APIs. This provides the microservices
 * integration layer for tenant and user management.
 */
@Component
class IamServiceClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${eaf.iam.service.url:http://localhost:8081}") private val iamServiceUrl: String,
) {
    // Tenant operations
    fun createTenant(request: CreateTenantRequest): CreateTenantResponse {
        val url = "$iamServiceUrl/api/v1/tenants"
        val headers = createHeaders()
        val entity = HttpEntity(request, headers)

        return restTemplate.postForObject(url, entity, CreateTenantResponse::class.java)
            ?: throw IamServiceException("Failed to create tenant")
    }

    fun getTenant(tenantId: String): TenantResponse? {
        val url = "$iamServiceUrl/api/v1/tenants/$tenantId"
        val headers = createHeaders()
        val entity = HttpEntity<Void>(headers)

        return try {
            restTemplate.exchange(url, HttpMethod.GET, entity, TenantResponse::class.java).body
        } catch (e: Exception) {
            null
        }
    }

    // User operations
    fun createUser(
        tenantId: String,
        request: CreateUserRequest,
    ): CreateUserResponse {
        val url = "$iamServiceUrl/api/v1/tenants/$tenantId/users"
        val headers = createHeaders()
        val entity = HttpEntity(request, headers)

        return restTemplate.postForObject(url, entity, CreateUserResponse::class.java)
            ?: throw IamServiceException("Failed to create user")
    }

    fun listUsers(tenantId: String): ListUsersResponse {
        val url = "$iamServiceUrl/api/v1/tenants/$tenantId/users"
        val headers = createHeaders()
        val entity = HttpEntity<Void>(headers)

        return restTemplate
            .exchange(url, HttpMethod.GET, entity, ListUsersResponse::class.java)
            .body
            ?: throw IamServiceException("Failed to list users")
    }

    fun updateUserStatus(
        tenantId: String,
        userId: String,
        request: UpdateUserStatusRequest,
    ): UpdateUserStatusResponse {
        val url = "$iamServiceUrl/api/v1/tenants/$tenantId/users/$userId/status"
        val headers = createHeaders()
        val entity = HttpEntity(request, headers)

        return restTemplate
            .exchange(
                url,
                HttpMethod.PUT,
                entity,
                UpdateUserStatusResponse::class.java,
            ).body
            ?: throw IamServiceException("Failed to update user status")
    }

    private fun createHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        // TODO: Add authentication headers (JWT token) when security is implemented
        return headers
    }
}

// DTOs matching the IAM service APIs
data class CreateTenantRequest(
    val tenantName: String,
    val initialAdminEmail: String,
)

data class CreateTenantResponse(
    val tenantId: String,
    val tenantName: String,
    val tenantAdminUserId: String,
    val tenantAdminEmail: String,
    val invitationDetails: String,
)

data class TenantResponse(
    val tenantId: String,
    val tenantName: String,
    val status: String,
    val createdAt: String,
)

data class CreateUserRequest(
    val email: String,
    val username: String? = null,
)

data class CreateUserResponse(
    val userId: String,
    val tenantId: String,
    val email: String,
    val username: String?,
    val status: String,
)

data class ListUsersResponse(
    val tenantId: String,
    val users: List<UserSummaryResponse>,
)

data class UserSummaryResponse(
    val userId: String,
    val email: String,
    val username: String?,
    val role: String,
    val status: String,
)

data class UpdateUserStatusRequest(
    val newStatus: String,
)

data class UpdateUserStatusResponse(
    val userId: String,
    val tenantId: String,
    val email: String,
    val username: String?,
    val previousStatus: String,
    val newStatus: String,
)

class IamServiceException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

package com.axians.eaf.iam.integration

import com.axians.eaf.iam.PostgresTestcontainerConfiguration
import com.axians.eaf.iam.infrastructure.config.JpaConfig
import com.axians.eaf.iam.test.TestIamServiceApplication
import com.axians.eaf.iam.web.CreateUserRequest
import com.axians.eaf.iam.web.UpdateUserStatusRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Integration test for user management functionality.
 * Tests the complete flow from web layer to persistence layer.
 */
@SpringBootTest(
    classes = [TestIamServiceApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = ["eaf.system.initialize-default-tenant=false"],
)
@Testcontainers
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
@Import(PostgresTestcontainerConfiguration::class, JpaConfig::class)
class UserManagementIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @WithMockUser(roles = ["TENANT_ADMIN"])
    fun `should create user successfully through complete flow`() {
        // Given
        val request =
            CreateUserRequest(
                email = "newuser@example.com",
                username = "newuser",
            )

        // When & Then
        mockMvc
            .perform(
                post("/api/v1/tenants/test-tenant-123/users")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.tenantId").value("test-tenant-123"))
            .andExpect(jsonPath("$.email").value("newuser@example.com"))
            .andExpect(jsonPath("$.username").value("newuser"))
            .andExpect(jsonPath("$.status").value("PENDING_ACTIVATION"))
            .andExpect(jsonPath("$.userId").exists())
    }

    @Test
    @WithMockUser(roles = ["TENANT_ADMIN"])
    fun `should list users successfully through complete flow`() {
        // Given - First create a user
        val createRequest =
            CreateUserRequest(
                email = "listuser@example.com",
                username = "listuser",
            )

        mockMvc
            .perform(
                post("/api/v1/tenants/test-tenant-456/users")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)),
            ).andExpect(status().isCreated)

        // When & Then - List users
        mockMvc
            .perform(
                get("/api/v1/tenants/test-tenant-456/users"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.tenantId").value("test-tenant-456"))
            .andExpect(jsonPath("$.users").isArray)
            .andExpect(jsonPath("$.users.length()").value(1))
            .andExpect(jsonPath("$.users[0].email").value("listuser@example.com"))
            .andExpect(jsonPath("$.users[0].username").value("listuser"))
            .andExpect(jsonPath("$.users[0].role").value("USER"))
            .andExpect(jsonPath("$.users[0].status").value("PENDING_ACTIVATION"))
    }

    @Test
    @WithMockUser(roles = ["TENANT_ADMIN"])
    fun `should update user status successfully through complete flow`() {
        // Given - First create a user
        val createRequest =
            CreateUserRequest(
                email = "statususer@example.com",
                username = "statususer",
            )

        val createResult =
            mockMvc
                .perform(
                    post("/api/v1/tenants/test-tenant-789/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)),
                ).andExpect(status().isCreated)
                .andReturn()

        val createResponse = objectMapper.readTree(createResult.response.contentAsString)
        val userId = createResponse.get("userId").asText()

        // When & Then - Update user status
        val updateRequest = UpdateUserStatusRequest(newStatus = "ACTIVE")

        mockMvc
            .perform(
                put("/api/v1/tenants/test-tenant-789/users/{userId}/status", userId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.tenantId").value("test-tenant-789"))
            .andExpect(jsonPath("$.email").value("statususer@example.com"))
            .andExpect(jsonPath("$.username").value("statususer"))
            .andExpect(jsonPath("$.previousStatus").value("PENDING_ACTIVATION"))
            .andExpect(jsonPath("$.newStatus").value("ACTIVE"))
    }

    @Test
    @WithMockUser(roles = ["TENANT_ADMIN"])
    fun `should prevent creating user with duplicate email`() {
        // Given - First create a user
        val firstRequest =
            CreateUserRequest(
                email = "duplicate@example.com",
                username = "firstuser",
            )

        mockMvc
            .perform(
                post("/api/v1/tenants/test-tenant-duplicate/users")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(firstRequest)),
            ).andExpect(status().isCreated)

        // When & Then - Try to create another user with same email
        val duplicateRequest =
            CreateUserRequest(
                email = "duplicate@example.com",
                username = "seconduser",
            )

        mockMvc
            .perform(
                post("/api/v1/tenants/test-tenant-duplicate/users")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(duplicateRequest)),
            ).andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(roles = ["TENANT_ADMIN"])
    fun `should return empty list when no users in tenant`() {
        // When & Then
        mockMvc
            .perform(
                get("/api/v1/tenants/empty-tenant/users"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.tenantId").value("empty-tenant"))
            .andExpect(jsonPath("$.users").isArray)
            .andExpect(jsonPath("$.users.length()").value(0))
    }

    @Test
    @WithMockUser(roles = ["TENANT_ADMIN"])
    fun `should return error when updating non-existent user`() {
        // Given
        val updateRequest = UpdateUserStatusRequest(newStatus = "ACTIVE")

        // When & Then
        mockMvc
            .perform(
                put("/api/v1/tenants/test-tenant/users/{userId}/status", "non-existent-user")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should return unauthorized when no authentication provided`() {
        // Given
        val request =
            CreateUserRequest(
                email = "user@example.com",
                username = "user",
            )

        // When & Then
        mockMvc
            .perform(
                post("/api/v1/tenants/test-tenant/users")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `should return forbidden when insufficient privileges`() {
        // Given
        val request =
            CreateUserRequest(
                email = "user@example.com",
                username = "user",
            )

        // When & Then
        mockMvc
            .perform(
                post("/api/v1/tenants/test-tenant/users")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isForbidden)
    }
}

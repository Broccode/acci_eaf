package com.axians.eaf.iam.integration

import com.axians.eaf.core.security.EafSecurityContextHolder
import com.axians.eaf.iam.PostgresTestcontainerConfiguration
import com.axians.eaf.iam.infrastructure.adapter.inbound.web.CreateUserRequest
import com.axians.eaf.iam.infrastructure.adapter.inbound.web.UpdateUserStatusRequest
import com.axians.eaf.iam.infrastructure.config.JpaConfig
import com.axians.eaf.iam.test.TestIamServiceApplication
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
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
 * Integration test for user management functionality. Tests the complete flow from web layer to
 * persistence layer.
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
    @Autowired private lateinit var mockMvc: MockMvc

    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var securityContextHolder: EafSecurityContextHolder

    @BeforeEach
    fun setUp() {
        // Setup default security context for TENANT_ADMIN role
        every { securityContextHolder.getTenantId() } returns "test-tenant-123"
        every { securityContextHolder.hasRole("TENANT_ADMIN") } returns true
        every { securityContextHolder.isAuthenticated() } returns true
    }

    @Test
    fun `should load context`() {
        // This test simply ensures that the application context loads correctly
        // for the user management features.
    }

    @Test
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
                    .with(user("testuser").roles("TENANT_ADMIN"))
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
    fun `should list users successfully through complete flow`() {
        // Given - Setup security context for different tenant
        every { securityContextHolder.getTenantId() } returns "test-tenant-456"

        // First create a user
        val createRequest =
            CreateUserRequest(
                email = "listuser@example.com",
                username = "listuser",
            )

        mockMvc
            .perform(
                post("/api/v1/tenants/test-tenant-456/users")
                    .with(user("testuser").roles("TENANT_ADMIN"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)),
            ).andExpect(status().isCreated)

        // When & Then - List users
        mockMvc
            .perform(
                get("/api/v1/tenants/test-tenant-456/users")
                    .with(user("testuser").roles("TENANT_ADMIN")),
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
    fun `should update user status successfully through complete flow`() {
        // Given - Setup security context for different tenant
        every { securityContextHolder.getTenantId() } returns "test-tenant-789"

        // First create a user
        val createRequest =
            CreateUserRequest(
                email = "statususer@example.com",
                username = "statususer",
            )

        val createResult =
            mockMvc
                .perform(
                    post("/api/v1/tenants/test-tenant-789/users")
                        .with(user("testuser").roles("TENANT_ADMIN"))
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
                    .with(user("testuser").roles("TENANT_ADMIN"))
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
    fun `should prevent creating user with duplicate email`() {
        // Given - Setup security context for different tenant
        every { securityContextHolder.getTenantId() } returns "test-tenant-duplicate"

        // First create a user
        val firstRequest =
            CreateUserRequest(
                email = "duplicate@example.com",
                username = "firstuser",
            )

        mockMvc
            .perform(
                post("/api/v1/tenants/test-tenant-duplicate/users")
                    .with(user("testuser").roles("TENANT_ADMIN"))
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
                    .with(user("testuser").roles("TENANT_ADMIN"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(duplicateRequest)),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should return empty list when no users in tenant`() {
        // Given - Setup security context for empty tenant
        every { securityContextHolder.getTenantId() } returns "empty-tenant"

        // When & Then
        mockMvc
            .perform(
                get("/api/v1/tenants/empty-tenant/users").with(user("testuser").roles("TENANT_ADMIN")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.tenantId").value("empty-tenant"))
            .andExpect(jsonPath("$.users").isArray)
            .andExpect(jsonPath("$.users.length()").value(0))
    }

    @Test
    fun `should reject access to different tenant`() {
        // Given - User authenticated for one tenant trying to access another
        every { securityContextHolder.getTenantId() } returns "user-tenant"

        val request =
            CreateUserRequest(
                email = "unauthorized@example.com",
                username = "unauthorized",
            )

        // When & Then - Should be rejected
        mockMvc
            .perform(
                post("/api/v1/tenants/different-tenant/users")
                    .with(user("testuser").roles("TENANT_ADMIN"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isBadRequest)
    }
}

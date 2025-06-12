package com.axians.eaf.iam.web

import com.axians.eaf.iam.application.port.inbound.CreateUserCommand
import com.axians.eaf.iam.application.port.inbound.CreateUserResult
import com.axians.eaf.iam.application.port.inbound.CreateUserUseCase
import com.axians.eaf.iam.application.port.inbound.ListUsersInTenantQuery
import com.axians.eaf.iam.application.port.inbound.ListUsersInTenantResult
import com.axians.eaf.iam.application.port.inbound.ListUsersInTenantUseCase
import com.axians.eaf.iam.application.port.inbound.UpdateUserStatusCommand
import com.axians.eaf.iam.application.port.inbound.UpdateUserStatusResult
import com.axians.eaf.iam.application.port.inbound.UpdateUserStatusUseCase
import com.axians.eaf.iam.application.port.inbound.UserSummary
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(UserController::class)
class UserControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var createUserUseCase: CreateUserUseCase

    @Autowired
    private lateinit var listUsersInTenantUseCase: ListUsersInTenantUseCase

    @Autowired
    private lateinit var updateUserStatusUseCase: UpdateUserStatusUseCase

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun createUserUseCase(): CreateUserUseCase = mockk()

        @Bean
        @Primary
        fun listUsersInTenantUseCase(): ListUsersInTenantUseCase = mockk()

        @Bean
        @Primary
        fun updateUserStatusUseCase(): UpdateUserStatusUseCase = mockk()
    }

    @Test
    @WithMockUser(roles = ["TENANT_ADMIN"])
    fun `should create user successfully when valid request provided`() {
        // Given
        val request =
            CreateUserRequest(
                tenantId = "tenant-123",
                email = "user@example.com",
                username = "testuser",
            )
        val result =
            CreateUserResult(
                userId = "user-123",
                tenantId = "tenant-123",
                email = "user@example.com",
                username = "testuser",
                status = "PENDING_ACTIVATION",
            )

        every { createUserUseCase.createUser(any()) } returns result

        // When & Then
        mockMvc
            .perform(
                post("/api/v1/users")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.userId").value("user-123"))
            .andExpect(jsonPath("$.tenantId").value("tenant-123"))
            .andExpect(jsonPath("$.email").value("user@example.com"))
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.status").value("PENDING_ACTIVATION"))

        verify { createUserUseCase.createUser(CreateUserCommand("tenant-123", "user@example.com", "testuser")) }
    }

    @Test
    @WithMockUser(roles = ["TENANT_ADMIN"])
    fun `should list users successfully when valid tenant id provided`() {
        // Given
        val tenantId = "tenant-123"
        val users =
            listOf(
                UserSummary("user-1", "user1@example.com", "user1", "USER", "ACTIVE"),
                UserSummary("user-2", "admin@example.com", null, "TENANT_ADMIN", "PENDING_ACTIVATION"),
            )
        val result = ListUsersInTenantResult(tenantId, users)

        every { listUsersInTenantUseCase.listUsers(any()) } returns result

        // When & Then
        mockMvc
            .perform(
                get("/api/v1/users")
                    .param("tenantId", tenantId),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.tenantId").value("tenant-123"))
            .andExpect(jsonPath("$.users").isArray)
            .andExpect(jsonPath("$.users.length()").value(2))
            .andExpect(jsonPath("$.users[0].userId").value("user-1"))
            .andExpect(jsonPath("$.users[0].email").value("user1@example.com"))
            .andExpect(jsonPath("$.users[1].userId").value("user-2"))
            .andExpect(jsonPath("$.users[1].email").value("admin@example.com"))

        verify { listUsersInTenantUseCase.listUsers(ListUsersInTenantQuery(tenantId)) }
    }

    @Test
    @WithMockUser(roles = ["TENANT_ADMIN"])
    fun `should update user status successfully when valid request provided`() {
        // Given
        val tenantId = "tenant-123"
        val userId = "user-123"
        val request = UpdateUserStatusRequest(newStatus = "ACTIVE")
        val result =
            UpdateUserStatusResult(
                userId = userId,
                tenantId = tenantId,
                email = "user@example.com",
                username = "testuser",
                previousStatus = "PENDING_ACTIVATION",
                newStatus = "ACTIVE",
            )

        every { updateUserStatusUseCase.updateUserStatus(any()) } returns result

        // When & Then
        mockMvc
            .perform(
                put("/api/v1/users/{userId}/status", userId)
                    .with(csrf())
                    .param("tenantId", tenantId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.tenantId").value(tenantId))
            .andExpect(jsonPath("$.previousStatus").value("PENDING_ACTIVATION"))
            .andExpect(jsonPath("$.newStatus").value("ACTIVE"))

        verify { updateUserStatusUseCase.updateUserStatus(UpdateUserStatusCommand(tenantId, userId, "ACTIVE")) }
    }

    @Test
    fun `should return unauthorized when no authentication provided`() {
        // Given
        val request =
            CreateUserRequest(
                tenantId = "tenant-123",
                email = "user@example.com",
                username = "testuser",
            )

        // When & Then
        mockMvc
            .perform(
                post("/api/v1/users")
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
                tenantId = "tenant-123",
                email = "user@example.com",
                username = "testuser",
            )

        // When & Then
        mockMvc
            .perform(
                post("/api/v1/users")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isForbidden)
    }
}

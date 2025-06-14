package com.axians.eaf.iam.web

import com.axians.eaf.core.security.EafSecurityContextHolder
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
import com.axians.eaf.iam.test.TestIamServiceApplication
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@SpringBootTest(classes = [TestIamServiceApplication::class])
@AutoConfigureMockMvc
class UserControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var createUserUseCase: CreateUserUseCase

    @MockkBean
    private lateinit var listUsersInTenantUseCase: ListUsersInTenantUseCase

    @MockkBean
    private lateinit var updateUserStatusUseCase: UpdateUserStatusUseCase

    @MockkBean
    private lateinit var securityContextHolder: EafSecurityContextHolder

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        // Setup default security context for TENANT_ADMIN role
        every { securityContextHolder.getTenantId() } returns "tenant-123"
        every { securityContextHolder.hasRole("TENANT_ADMIN") } returns true
        every { securityContextHolder.isAuthenticated() } returns true
    }

    @Test
    fun `should create user successfully when valid request provided`() {
        // Given
        val request =
            mapOf(
                "email" to "test@example.com",
                "username" to "johndoe",
            )
        val command =
            CreateUserCommand(
                tenantId = "tenant-123",
                email = "test@example.com",
                username = "johndoe",
            )
        val result =
            CreateUserResult(
                userId = UUID.randomUUID().toString(),
                tenantId = "tenant-123",
                email = "test@example.com",
                username = "johndoe",
                status = "PENDING_ACTIVATION",
            )

        every { createUserUseCase.createUser(any()) } returns result

        // When & Then
        mockMvc
            .perform(
                post("/api/v1/tenants/tenant-123/users")
                    .with(user("testuser").roles("TENANT_ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.userId").value(result.userId))
            .andExpect(jsonPath("$.tenantId").value("tenant-123"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.username").value("johndoe"))
            .andExpect(jsonPath("$.status").value("PENDING_ACTIVATION"))

        verify { createUserUseCase.createUser(any()) }
    }

    @Test
    fun `should list users for tenant successfully`() {
        // Given
        val query = ListUsersInTenantQuery(tenantId = "tenant-123")
        val userSummary =
            UserSummary(
                userId = "user-456",
                email = "user@example.com",
                username = "johndoe",
                role = "USER",
                status = "ACTIVE",
            )
        val result = ListUsersInTenantResult(tenantId = "tenant-123", users = listOf(userSummary))

        every { listUsersInTenantUseCase.listUsers(query) } returns result

        // When & Then
        mockMvc
            .perform(
                get("/api/v1/tenants/tenant-123/users")
                    .with(user("testuser").roles("TENANT_ADMIN")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.tenantId").value("tenant-123"))
            .andExpect(jsonPath("$.users").isArray)
            .andExpect(jsonPath("$.users[0].userId").value("user-456"))
            .andExpect(jsonPath("$.users[0].email").value("user@example.com"))
            .andExpect(jsonPath("$.users[0].username").value("johndoe"))
            .andExpect(jsonPath("$.users[0].role").value("USER"))
            .andExpect(jsonPath("$.users[0].status").value("ACTIVE"))

        verify { listUsersInTenantUseCase.listUsers(query) }
    }

    @Test
    fun `should update user status successfully when valid request provided`() {
        // Given
        val command =
            UpdateUserStatusCommand(
                tenantId = "tenant-123",
                userId = "user-456",
                newStatus = "INACTIVE",
            )
        val result =
            UpdateUserStatusResult(
                userId = "user-456",
                tenantId = "tenant-123",
                email = "user@example.com",
                username = "johndoe",
                previousStatus = "ACTIVE",
                newStatus = "INACTIVE",
            )

        every { updateUserStatusUseCase.updateUserStatus(command) } returns result

        // When & Then
        mockMvc
            .perform(
                put("/api/v1/tenants/tenant-123/users/user-456/status")
                    .with(user("testuser").roles("TENANT_ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("newStatus" to "INACTIVE"))),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value("user-456"))
            .andExpect(jsonPath("$.tenantId").value("tenant-123"))
            .andExpect(jsonPath("$.email").value("user@example.com"))
            .andExpect(jsonPath("$.username").value("johndoe"))
            .andExpect(jsonPath("$.previousStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.newStatus").value("INACTIVE"))

        verify { updateUserStatusUseCase.updateUserStatus(command) }
    }

    @Test
    fun `should return forbidden when user has insufficient privileges`() {
        // Given - User without TENANT_ADMIN role
        every { securityContextHolder.hasRole("TENANT_ADMIN") } returns false

        // When & Then
        mockMvc
            .perform(
                get("/api/v1/tenants/tenant-123/users")
                    .with(user("testuser").roles("USER")),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `should reject access to different tenant`() {
        // Given - User authenticated for different tenant
        every { securityContextHolder.getTenantId() } returns "user-tenant"

        // When & Then
        mockMvc
            .perform(
                get("/api/v1/tenants/different-tenant/users")
                    .with(user("testuser").roles("TENANT_ADMIN")),
            ).andExpect(status().isBadRequest)
    }
}

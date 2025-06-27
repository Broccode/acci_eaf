package com.axians.eaf.iam.infrastructure.adapter.inbound.web

import com.axians.eaf.core.security.EafSecurityContextHolder
import com.axians.eaf.iam.application.port.inbound.CreateTenantResult
import com.axians.eaf.iam.application.port.inbound.CreateTenantUseCase
import com.axians.eaf.iam.test.TestIamServiceApplication
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(classes = [TestIamServiceApplication::class])
@AutoConfigureMockMvc
class TenantWebAdapterTest {
    @Autowired private lateinit var mockMvc: MockMvc

    @MockkBean private lateinit var createTenantUseCase: CreateTenantUseCase

    @MockkBean private lateinit var securityContextHolder: EafSecurityContextHolder

    @Autowired private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should create tenant successfully when valid request provided`() {
        // Given
        val request =
            CreateTenantRequest(
                tenantName = "New Tenant",
                initialAdminEmail = "admin@newtenant.com",
            )
        val result =
            CreateTenantResult(
                tenantId = "tenant-id-123",
                tenantName = "New Tenant",
                tenantAdminUserId = "user-id-456",
                tenantAdminEmail = "admin@newtenant.com",
                invitationDetails = "Invitation sent",
            )
        every { createTenantUseCase.handle(any()) } returns result

        // When & Then
        mockMvc
            .perform(
                post("/api/v1/tenants")
                    .with(user("superadmin").roles("SUPERADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.tenantId").value(result.tenantId))
            .andExpect(jsonPath("$.tenantName").value(result.tenantName))
            .andExpect(jsonPath("$.tenantAdminUserId").value(result.tenantAdminUserId))
            .andExpect(jsonPath("$.tenantAdminEmail").value(result.tenantAdminEmail))
            .andExpect(jsonPath("$.invitationDetails").value(result.invitationDetails))
    }

    @Test
    fun `should return forbidden when user has insufficient privileges`() {
        // Given
        val request =
            CreateTenantRequest(
                tenantName = "Another Tenant",
                initialAdminEmail = "admin@another.com",
            )

        // When & Then
        mockMvc
            .perform(
                post("/api/v1/tenants")
                    .with(user("testuser").roles("USER")) // Not a SUPERADMIN
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `should return bad request for invalid input`() {
        // Given
        val request =
            CreateTenantRequest(
                tenantName = "", // Blank name
                initialAdminEmail = "not-an-email",
            )

        // When & Then
        mockMvc
            .perform(
                post("/api/v1/tenants")
                    .with(user("superadmin").roles("SUPERADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isBadRequest)
    }
}

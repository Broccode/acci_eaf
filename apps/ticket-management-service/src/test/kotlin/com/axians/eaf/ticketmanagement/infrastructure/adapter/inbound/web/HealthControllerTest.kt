package com.axians.eaf.ticketmanagement.infrastructure.adapter.inbound.web

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Test for the HealthController to verify basic service functionality.
 * This follows TDD principles - test first, then implementation.
 */
@WebMvcTest(HealthController::class)
class HealthControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    @WithMockUser
    fun `should return health status when health endpoint is called`() {
        // When & Then
        mockMvc
            .perform(get("/api/health"))
            .andExpect(status().isOk)
            .andExpect(content().json("""{"status": "UP", "service": "ticket-management-service"}"""))
    }

    @Test
    @WithMockUser
    fun `should return service info when info endpoint is called`() {
        // When & Then
        mockMvc
            .perform(get("/api/info"))
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    """{"name": "Ticket Management Service", "version": "1.0.0", "description": "EAF Pilot Service for validating framework capabilities"}""",
                ),
            )
    }
}

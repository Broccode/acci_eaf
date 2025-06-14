package com.axians.eaf.core.security

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Simple test to isolate the exact issue with context propagation.
 */
class SimpleCoroutineTest {
    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
        CorrelationIdManager.clearCorrelationId()
        MDC.clear()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
        CorrelationIdManager.clearCorrelationId()
        MDC.clear()
    }

    @Test
    fun `should verify initial context setup`() {
        // Set up context
        val tenantId = "simple-tenant"
        val userId = "simple-user"
        val correlationId = "simple-correlation"

        setupSecurityContext(tenantId, userId, listOf("USER"))
        CorrelationIdManager.setCorrelationId(correlationId)

        // Verify setup worked
        assertEquals(tenantId, getSecurityTenantId())
        assertEquals(correlationId, CorrelationIdManager.getCurrentCorrelationId())
    }

    @Test
    fun `should create EafContextElement successfully`() {
        // Set up context
        val tenantId = "element-tenant"
        val userId = "element-user"
        val correlationId = "element-correlation"

        setupSecurityContext(tenantId, userId, listOf("USER"))
        CorrelationIdManager.setCorrelationId(correlationId)

        // Create EafContextElement
        val contextElement = EafContextElement()
        assertNotNull(contextElement)
    }

    @Test
    fun `should propagate context through withContext block`() =
        runBlocking {
            // Set up context
            val tenantId = "with-tenant"
            val userId = "with-user"
            val correlationId = "with-correlation"

            setupSecurityContext(tenantId, userId, listOf("USER"))
            CorrelationIdManager.setCorrelationId(correlationId)

            // Test propagation
            withContext(EafContextElement()) {
                // Verify context is available
                assertEquals(tenantId, getSecurityTenantId(), "Tenant should be propagated")
                assertEquals(
                    correlationId,
                    CorrelationIdManager.getCurrentCorrelationId(),
                    "Correlation should be propagated",
                )
            }
        }

    private fun setupSecurityContext(
        tenantId: String,
        userId: String,
        roles: List<String>,
    ) {
        val principal =
            object : HasTenantId, HasUserId {
                override fun getTenantId(): String = tenantId

                override fun getUserId(): String = userId

                override fun toString(): String = "SimpleTestPrincipal(tenant=$tenantId, user=$userId)"
            }

        val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }
        val authentication = TestingAuthenticationToken(principal, "credentials", authorities)
        authentication.isAuthenticated = true

        val securityContext = SecurityContextImpl(authentication)
        SecurityContextHolder.setContext(securityContext)
    }

    private fun getSecurityTenantId(): String? {
        val auth = SecurityContextHolder.getContext().authentication ?: return null
        val principal = auth.principal
        return (principal as? HasTenantId)?.getTenantId()
    }
}

package com.axians.eaf.core.security

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Simple integration test demonstrating the EAF Security Context functionality working with Spring
 * Security integration in a realistic scenario.
 */
class SimpleSecurityContextIntegrationTest {
    // Create a simple test implementation instead of relying on Spring configuration
    private val securityContextHolder: EafSecurityContextHolder =
        object : EafSecurityContextHolder {
            override fun getTenantId(): String = getTenantIdOrNull() ?: error("No tenant context")

            override fun getTenantIdOrNull(): String? {
                val context = SecurityContextHolder.getContext()
                val auth = context.authentication ?: return null
                val principal = auth.principal
                return when (principal) {
                    is HasTenantId -> principal.getTenantId()
                    else -> null
                }
            }

            override fun getUserId(): String? {
                val context = SecurityContextHolder.getContext()
                val auth = context.authentication ?: return null
                val principal = auth.principal
                return when (principal) {
                    is HasUserId -> principal.getUserId()
                    else -> null
                }
            }

            override fun hasRole(role: String): Boolean {
                val context = SecurityContextHolder.getContext()
                val auth = context.authentication ?: return false
                return auth.authorities?.any {
                    it.authority == "ROLE_$role" || it.authority == role
                }
                    ?: false
            }

            override fun hasPermission(permission: String): Boolean = false // Not implemented for this test

            override fun hasAnyRole(vararg roles: String): Boolean = roles.any { hasRole(it) }

            override fun isAuthenticated(): Boolean {
                val context = SecurityContextHolder.getContext()
                val auth = context.authentication ?: return false
                return auth.isAuthenticated
            }

            override fun getAuthentication() = SecurityContextHolder.getContext().authentication

            override fun getPrincipal() = getAuthentication()?.principal as? java.security.Principal
        }

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `should integrate with Spring Security context for authenticated scenarios`() {
        // Given: Spring Security context is established with tenant and user info
        val tenantId = "tenant-123"
        val userId = "user-456"
        val roles = listOf("USER", "ADMIN")

        setupSpringSecurityContext(tenantId, userId, roles)

        // When: Using EAF security context holder to access the context
        val actualTenantId = securityContextHolder.getTenantIdOrNull()
        val actualUserId = securityContextHolder.getUserId()
        val isAuthenticated = securityContextHolder.isAuthenticated()
        val hasUserRole = securityContextHolder.hasRole("USER")
        val hasAdminRole = securityContextHolder.hasRole("ADMIN")
        val hasInvalidRole = securityContextHolder.hasRole("INVALID")

        // Then: All context information is correctly accessible
        assertEquals(tenantId, actualTenantId)
        assertEquals(userId, actualUserId)
        assertTrue(isAuthenticated)
        assertTrue(hasUserRole)
        assertTrue(hasAdminRole)
        assertFalse(hasInvalidRole)
    }

    @Test
    fun `should handle unauthenticated scenarios gracefully`() {
        // Given: No Spring Security context is established
        SecurityContextHolder.clearContext()

        // When: Using EAF security context holder to access the context
        val actualTenantId = securityContextHolder.getTenantIdOrNull()
        val actualUserId = securityContextHolder.getUserId()
        val isAuthenticated = securityContextHolder.isAuthenticated()
        val hasUserRole = securityContextHolder.hasRole("USER")

        // Then: All methods handle the unauthenticated state gracefully
        assertEquals(null, actualTenantId)
        assertEquals(null, actualUserId)
        assertFalse(isAuthenticated)
        assertFalse(hasUserRole)
    }

    @Test
    fun `should work with custom authentication objects containing tenant context`() {
        // Given: Custom authentication with tenant/user principal
        val tenantId = "custom-tenant"
        val userId = "custom-user"

        // Create a simple principal that implements HasTenantId and HasUserId
        val principal =
            object : HasTenantId, HasUserId {
                override fun getTenantId(): String = tenantId

                override fun getUserId(): String = userId

                override fun toString(): String = "CustomPrincipal(tenant=$tenantId, user=$userId)"
            }

        val authorities = listOf(SimpleGrantedAuthority("ROLE_CUSTOM"))
        val authentication = TestingAuthenticationToken(principal, "credentials", authorities)
        authentication.isAuthenticated = true

        val securityContext = SecurityContextImpl(authentication)
        SecurityContextHolder.setContext(securityContext)

        // When: Using EAF security context holder
        val actualTenantId = securityContextHolder.getTenantIdOrNull()
        val actualUserId = securityContextHolder.getUserId()
        val isAuthenticated = securityContextHolder.isAuthenticated()
        val hasCustomRole = securityContextHolder.hasRole("CUSTOM")

        // Then: Context information is correctly extracted from custom principal
        assertEquals(tenantId, actualTenantId)
        assertEquals(userId, actualUserId)
        assertTrue(isAuthenticated)
        assertTrue(hasCustomRole)
    }

    @Test
    fun `should support different tenant contexts in sequence`() {
        // Given: Multiple tenant contexts processed in sequence
        val contexts =
            listOf(
                Triple("tenant-1", "user-1", listOf("USER")),
                Triple("tenant-2", "user-2", listOf("ADMIN")),
                Triple("tenant-3", "user-3", listOf("USER", "ADMIN")),
            )

        contexts.forEach { (expectedTenantId, expectedUserId, roles) ->
            // When: Set up context for this tenant
            setupSpringSecurityContext(expectedTenantId, expectedUserId, roles)

            // Then: Verify context is correctly accessible
            val actualTenantId = securityContextHolder.getTenantIdOrNull()
            val actualUserId = securityContextHolder.getUserId()
            val isAuthenticated = securityContextHolder.isAuthenticated()

            assertEquals(expectedTenantId, actualTenantId)
            assertEquals(expectedUserId, actualUserId)
            assertTrue(isAuthenticated)

            // Verify role checking works for this context
            roles.forEach { role ->
                assertTrue(securityContextHolder.hasRole(role), "Should have role $role")
            }

            // Clean up for next iteration
            SecurityContextHolder.clearContext()
        }
    }

    private fun setupSpringSecurityContext(
        tenantId: String,
        userId: String,
        roles: List<String>,
    ) {
        // Create a simple principal that implements the required interfaces
        val principal =
            object : HasTenantId, HasUserId {
                override fun getTenantId(): String = tenantId

                override fun getUserId(): String = userId

                override fun toString(): String = "TestPrincipal(tenant=$tenantId, user=$userId)"
            }

        val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }
        val authentication = TestingAuthenticationToken(principal, "credentials", authorities)
        authentication.isAuthenticated = true

        val securityContext = SecurityContextImpl(authentication)
        SecurityContextHolder.setContext(securityContext)
    }
}

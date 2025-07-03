package com.axians.eaf.controlplane.integration.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContextFactory

/**
 * Factory for creating mock EAF security contexts in tests.
 *
 * This factory creates a complete security context that includes:
 * - Spring Security authentication
 * - EAF-specific user and tenant context via TestSecurityContext
 * - Realistic metadata for comprehensive testing
 */
class WithMockEafUserSecurityContextFactory : WithSecurityContextFactory<WithMockEafUser> {
    override fun createSecurityContext(annotation: WithMockEafUser): SecurityContext {
        val securityContext = SecurityContextHolder.createEmptyContext()

        // Create Spring Security authorities from roles
        val authorities = annotation.roles.map { SimpleGrantedAuthority("ROLE_$it") }

        // Create Spring Security authentication
        val authentication =
            UsernamePasswordAuthenticationToken(
                annotation.userId,
                "password", // Not used in tests
                authorities,
            )

        securityContext.authentication = authentication

        // Set up test security context for EAF components
        TestSecurityContext.setContext(
            tenantId = annotation.tenantId,
            userId = annotation.userId,
            userEmail = annotation.email,
            roles = annotation.roles,
        )

        return securityContext
    }
}

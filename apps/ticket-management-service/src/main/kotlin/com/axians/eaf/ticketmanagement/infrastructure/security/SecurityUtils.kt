package com.axians.eaf.ticketmanagement.infrastructure.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

/**
 * Utility class for Spring Security.
 *
 * This class provides helper methods to access security context information,
 * specifically for a stateless, JWT-based authentication context.
 */
@Component
class SecurityUtils(
    private val userDetailsService: UserDetailsService,
) {
    /**
     * Retrieves the UserDetails object for the currently authenticated user from a JWT.
     *
     * In a stateless setup, the principal from the SecurityContext is a `Jwt` object.
     * This method extracts the username from the JWT's subject and uses the `UserDetailsService`
     * to load the full user details.
     *
     * It also handles the mock user principal provided by Spring Security's testing
     * framework (`@WithMockUser`) for seamless integration testing.
     *
     * @return A nullable [UserDetails] if the user is authenticated, otherwise null.
     */
    fun getAuthenticatedUser(): UserDetails? {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        return when (principal) {
            is Jwt -> {
                val username = principal.subject
                userDetailsService.loadUserByUsername(username)
            }
            is UserDetails -> { // Handle mock user in tests
                principal
            }
            else -> null
        }
    }
}

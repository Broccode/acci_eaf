package com.axians.eaf.ticketmanagement.infrastructure.adapter.inbound.hilla

import com.axians.eaf.ticketmanagement.infrastructure.adapter.inbound.hilla.dto.UserInfo
import com.axians.eaf.ticketmanagement.infrastructure.security.SecurityUtils
import com.vaadin.flow.server.auth.AnonymousAllowed
import com.vaadin.hilla.Endpoint

/**
 * Hilla endpoint to provide user information to the frontend.
 *
 * This service is used by the `useAuth` hook in the React client to determine
 * the authentication state and retrieve details about the logged-in user in
 * a stateless security context.
 */
@Endpoint
@AnonymousAllowed
class UserInfoService(
    private val securityUtils: SecurityUtils,
) {
    /**
     * Retrieves the current user's information.
     *
     * It uses the `SecurityUtils` helper to get the authenticated user's details
     * from the JWT principal. If the user is authenticated, it returns their name,
     * username, and roles. If not, it returns a representation of an anonymous user.
     *
     * @return A [UserInfo] DTO containing the user's details.
     */
    fun getUserInfo(): UserInfo =
        securityUtils.getAuthenticatedUser()?.let { userDetails ->
            val roles = userDetails.authorities.map { it.authority }.toSet()
            UserInfo(
                name = userDetails.username, // Using username as name for this example
                username = userDetails.username,
                roles = roles,
                anonymous = false,
            )
        } ?: UserInfo(name = null, username = null, roles = emptySet(), anonymous = true)
}

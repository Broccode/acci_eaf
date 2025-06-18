package com.axians.eaf.ticketmanagement.infrastructure.adapter.inbound.hilla.dto

/**
 * DTO for providing non-sensitive user information to the frontend.
 * This is used by the `useAuth` hook to populate the client-side auth state.
 *
 * @param name The user's full name or display name.
 * @param username The user's login name.
 * @param roles A set of roles/authorities the user possesses.
 * @param anonymous Whether the user is anonymous (not logged in).
 */
data class UserInfo(
    val name: String?,
    val username: String?,
    val roles: Set<String>,
    val anonymous: Boolean,
)

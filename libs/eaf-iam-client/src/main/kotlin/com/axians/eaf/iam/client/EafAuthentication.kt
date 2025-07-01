package com.axians.eaf.iam.client

import com.axians.eaf.core.security.HasTenantId
import com.axians.eaf.core.security.HasUserId
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

/**
 * Spring Security Authentication implementation for EAF. Wraps an EafPrincipal and provides access
 * to authorities based on roles and permissions.
 */
class EafAuthentication(
    private val eafPrincipal: EafPrincipal,
    private val rawToken: String? = null,
) : Authentication,
    HasTenantId,
    HasUserId {
    private var authenticated: Boolean = true

    override fun getName(): String = eafPrincipal.name

    override fun getAuthorities(): Collection<GrantedAuthority> {
        val authorities = mutableSetOf<GrantedAuthority>()

        // Add role-based authorities with ROLE_ prefix (Spring Security convention)
        eafPrincipal.roles.forEach { role -> authorities.add(SimpleGrantedAuthority("ROLE_$role")) }

        // Add permission-based authorities
        eafPrincipal.permissions.forEach { permission ->
            authorities.add(SimpleGrantedAuthority(permission))
        }

        return authorities
    }

    override fun getCredentials(): Any? = rawToken

    override fun getDetails(): Any? = null

    override fun getPrincipal(): EafPrincipal = eafPrincipal

    override fun isAuthenticated(): Boolean = authenticated

    override fun setAuthenticated(isAuthenticated: Boolean) {
        require(!isAuthenticated) {
            "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead"
        }
        this.authenticated = false
    }

    /** Convenience method to get the tenant ID. */
    override fun getTenantId(): String = eafPrincipal.tenantId

    /** Convenience method to get the user ID. */
    override fun getUserId(): String? = eafPrincipal.userId
}

package com.axians.eaf.core.security

import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import java.security.Principal

/**
 * Interface for accessing EAF security context information. Provides a clean abstraction over
 * Spring Security's SecurityContextHolder.
 */
interface EafSecurityContextHolder {
    /**
     * Gets the current tenant ID from the security context.
     * @return the tenant ID of the current authenticated user
     * @throws IllegalStateException if no authenticated user or no tenant ID available
     */
    fun getTenantId(): String

    /**
     * Gets the current tenant ID from the security context, returning null if not available.
     * @return the tenant ID of the current authenticated user, or null if not authenticated
     */
    fun getTenantIdOrNull(): String?

    /**
     * Gets the current user ID from the security context.
     * @return the user ID of the current authenticated user, or null if not available
     */
    fun getUserId(): String?

    /**
     * Gets the current authenticated principal.
     * @return the current principal, or null if not authenticated
     */
    fun getPrincipal(): Principal?

    /**
     * Checks if the current user has the specified role.
     * @param role the role to check
     * @return true if the user has the role, false otherwise
     */
    fun hasRole(role: String): Boolean

    /**
     * Checks if the current user has the specified permission.
     * @param permission the permission to check
     * @return true if the user has the permission, false otherwise
     */
    fun hasPermission(permission: String): Boolean

    /**
     * Checks if the current user has any of the specified roles.
     * @param roles the roles to check
     * @return true if the user has any of the roles, false otherwise
     */
    fun hasAnyRole(vararg roles: String): Boolean

    /**
     * Checks if there is an authenticated user in the current context.
     * @return true if authenticated, false otherwise
     */
    fun isAuthenticated(): Boolean

    /**
     * Gets the current Spring Security Authentication object.
     * @return the current authentication, or null if not authenticated
     */
    fun getAuthentication(): Authentication?
}

/**
 * Default implementation of EafSecurityContextHolder. Uses Spring Security's SecurityContextHolder
 * to access authentication information.
 */
class DefaultEafSecurityContextHolder : EafSecurityContextHolder {
    override fun getTenantId(): String = getTenantIdOrNull() ?: error("No tenant ID available in security context")

    override fun getTenantIdOrNull(): String? {
        val authentication = getAuthentication() ?: return null

        // Try to get tenant ID from EafAuthentication or principal
        val eafAuth = authentication as? HasTenantId
        return eafAuth?.getTenantId() ?: (authentication.principal as? HasTenantId)?.getTenantId()
    }

    override fun getUserId(): String? {
        val authentication = getAuthentication() ?: return null

        // Try to get user ID from EafAuthentication or principal
        val eafAuth = authentication as? HasUserId
        return eafAuth?.getUserId() ?: (authentication.principal as? HasUserId)?.getUserId()
    }

    override fun getPrincipal(): Principal? = getAuthentication()?.principal as? Principal

    override fun hasRole(role: String): Boolean {
        val authentication = getAuthentication() ?: return false
        return authentication.authorities?.any {
            it.authority == "ROLE_$role" || it.authority == role
        }
            ?: false
    }

    override fun hasPermission(permission: String): Boolean {
        val authentication = getAuthentication() ?: return false
        return authentication.authorities?.any { it.authority == permission } ?: false
    }

    override fun hasAnyRole(vararg roles: String): Boolean = roles.any { hasRole(it) }

    override fun isAuthenticated(): Boolean = getAuthentication()?.isAuthenticated ?: false

    override fun getAuthentication(): Authentication? = SecurityContextHolder.getContext()?.authentication
}

/** Interface for objects that can provide a tenant ID. */
interface HasTenantId {
    fun getTenantId(): String
}

/** Interface for objects that can provide a user ID. */
interface HasUserId {
    fun getUserId(): String?
}

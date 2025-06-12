package com.axians.eaf.iam.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.authority.SimpleGrantedAuthority

class EafAuthenticationTest {
    @Test
    fun `should return principal name`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
            )
        val authentication = EafAuthentication(principal, "jwt-token")

        // When
        val name = authentication.name

        // Then
        assertThat(name).isEqualTo("john.doe")
    }

    @Test
    fun `should return authorities with ROLE_ prefix for roles`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
                roles = setOf("PILOT_APP_ADMIN", "PILOT_APP_USER"),
                permissions = setOf("user:read", "user:write"),
            )
        val authentication = EafAuthentication(principal, "jwt-token")

        // When
        val authorities = authentication.authorities

        // Then
        assertThat(authorities).containsExactlyInAnyOrder(
            SimpleGrantedAuthority("ROLE_PILOT_APP_ADMIN"),
            SimpleGrantedAuthority("ROLE_PILOT_APP_USER"),
            SimpleGrantedAuthority("user:read"),
            SimpleGrantedAuthority("user:write"),
        )
    }

    @Test
    fun `should return empty authorities when no roles or permissions`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
            )
        val authentication = EafAuthentication(principal, "jwt-token")

        // When
        val authorities = authentication.authorities

        // Then
        assertThat(authorities).isEmpty()
    }

    @Test
    fun `should return raw token as credentials`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
            )
        val token = "jwt-token-123"
        val authentication = EafAuthentication(principal, token)

        // When
        val credentials = authentication.credentials

        // Then
        assertThat(credentials).isEqualTo(token)
    }

    @Test
    fun `should return null credentials when no token provided`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
            )
        val authentication = EafAuthentication(principal)

        // When
        val credentials = authentication.credentials

        // Then
        assertThat(credentials).isNull()
    }

    @Test
    fun `should return null details`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
            )
        val authentication = EafAuthentication(principal)

        // When
        val details = authentication.details

        // Then
        assertThat(details).isNull()
    }

    @Test
    fun `should return principal`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
            )
        val authentication = EafAuthentication(principal)

        // When
        val returnedPrincipal = authentication.principal

        // Then
        assertThat(returnedPrincipal).isEqualTo(principal)
    }

    @Test
    fun `should be authenticated by default`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
            )
        val authentication = EafAuthentication(principal)

        // When & Then
        assertThat(authentication.isAuthenticated).isTrue()
    }

    @Test
    fun `should allow setting authenticated to false`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
            )
        val authentication = EafAuthentication(principal)

        // When
        authentication.setAuthenticated(false)

        // Then
        assertThat(authentication.isAuthenticated).isFalse()
    }

    @Test
    fun `should throw exception when trying to set authenticated to true`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
            )
        val authentication = EafAuthentication(principal)

        // When & Then
        assertThrows<IllegalArgumentException> {
            authentication.setAuthenticated(true)
        }
    }

    @Test
    fun `should return tenant ID`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
            )
        val authentication = EafAuthentication(principal)

        // When
        val tenantId = authentication.getTenantId()

        // Then
        assertThat(tenantId).isEqualTo("tenant-123")
    }

    @Test
    fun `should return user ID`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
            )
        val authentication = EafAuthentication(principal)

        // When
        val userId = authentication.getUserId()

        // Then
        assertThat(userId).isEqualTo("user-456")
    }

    @Test
    fun `should return null user ID when not provided`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = null,
                username = "john.doe",
                email = "john@example.com",
            )
        val authentication = EafAuthentication(principal)

        // When
        val userId = authentication.getUserId()

        // Then
        assertThat(userId).isNull()
    }
}

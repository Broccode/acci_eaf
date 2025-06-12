package com.axians.eaf.iam.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EafPrincipalTest {
    @Test
    fun `should return correct name when username is provided`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
            )

        // When
        val name = principal.name

        // Then
        assertThat(name).isEqualTo("john.doe")
    }

    @Test
    fun `should return email as name when username is null`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = null,
                email = "john@example.com",
            )

        // When
        val name = principal.name

        // Then
        assertThat(name).isEqualTo("john@example.com")
    }

    @Test
    fun `should return userId as name when username and email are null`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = null,
                email = null,
            )

        // When
        val name = principal.name

        // Then
        assertThat(name).isEqualTo("user-456")
    }

    @Test
    fun `should return unknown as name when all identifiers are null`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = null,
                username = null,
                email = null,
            )

        // When
        val name = principal.name

        // Then
        assertThat(name).isEqualTo("unknown")
    }

    @Test
    fun `should return true when principal has the specified role`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
                roles = setOf("PILOT_APP_ADMIN", "PILOT_APP_USER"),
            )

        // When & Then
        assertThat(principal.hasRole("PILOT_APP_ADMIN")).isTrue()
        assertThat(principal.hasRole("PILOT_APP_USER")).isTrue()
    }

    @Test
    fun `should return false when principal does not have the specified role`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
                roles = setOf("PILOT_APP_USER"),
            )

        // When & Then
        assertThat(principal.hasRole("PILOT_APP_ADMIN")).isFalse()
        assertThat(principal.hasRole("NONEXISTENT_ROLE")).isFalse()
    }

    @Test
    fun `should return true when principal has the specified permission`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
                permissions = setOf("user:read", "user:write"),
            )

        // When & Then
        assertThat(principal.hasPermission("user:read")).isTrue()
        assertThat(principal.hasPermission("user:write")).isTrue()
    }

    @Test
    fun `should return false when principal does not have the specified permission`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
                permissions = setOf("user:read"),
            )

        // When & Then
        assertThat(principal.hasPermission("user:write")).isFalse()
        assertThat(principal.hasPermission("admin:delete")).isFalse()
    }

    @Test
    fun `should return true when principal has any of the specified roles`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
                roles = setOf("PILOT_APP_USER"),
            )

        // When & Then
        assertThat(principal.hasAnyRole("PILOT_APP_ADMIN", "PILOT_APP_USER")).isTrue()
        assertThat(principal.hasAnyRole("PILOT_APP_USER", "OTHER_ROLE")).isTrue()
    }

    @Test
    fun `should return false when principal has none of the specified roles`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
                roles = setOf("PILOT_APP_USER"),
            )

        // When & Then
        assertThat(principal.hasAnyRole("PILOT_APP_ADMIN", "OTHER_ROLE")).isFalse()
    }

    @Test
    fun `should return true when principal has all of the specified roles`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
                roles = setOf("PILOT_APP_ADMIN", "PILOT_APP_USER", "OTHER_ROLE"),
            )

        // When & Then
        assertThat(principal.hasAllRoles("PILOT_APP_ADMIN", "PILOT_APP_USER")).isTrue()
    }

    @Test
    fun `should return false when principal does not have all of the specified roles`() {
        // Given
        val principal =
            EafPrincipal(
                tenantId = "tenant-123",
                userId = "user-456",
                username = "john.doe",
                email = "john@example.com",
                roles = setOf("PILOT_APP_USER"),
            )

        // When & Then
        assertThat(principal.hasAllRoles("PILOT_APP_ADMIN", "PILOT_APP_USER")).isFalse()
    }
}

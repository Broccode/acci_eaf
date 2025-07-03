package com.axians.eaf.controlplane.infrastructure.adapter.input

import com.vaadin.hilla.exception.EndpointException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

/**
 * Unit tests for UserInfoEndpoint following TDD principles. Tests type safety, validation, and
 * error handling for Hilla endpoints.
 */
class UserInfoEndpointTest {
    private val userInfoEndpoint = UserInfoEndpoint()

    @Test
    fun `should return current user info with proper type structure`() {
        // When
        val result = userInfoEndpoint.getCurrentUser()

        // Then - Validate type safety and structure
        assertThat(result.success).isTrue()
        assertThat(result.user).isNotNull
        assertThat(result.permissions).isNotNull
        assertThat(result.metadata).isNotNull
        assertThat(result.error).isNull()

        // Validate user info structure
        result.user?.let { user ->
            assertThat(user.userId).isNotEmpty()
            assertThat(user.username).isEqualTo("admin@demo-tenant.com")
            assertThat(user.email).isEqualTo("admin@demo-tenant.com")
            assertThat(user.firstName).isEqualTo("Demo")
            assertThat(user.lastName).isEqualTo("Administrator")
            assertThat(user.roles).contains("TENANT_ADMIN", "CONTROL_PLANE_USER")
            assertThat(user.tenantId).isEqualTo("tenant-demo-123")
            assertThat(user.tenantName).isEqualTo("Demo Tenant Corporation")
            assertThat(user.lastLoginAt).isBefore(Instant.now())
            assertThat(user.accountStatus).isEqualTo("ACTIVE")
        }

        // Validate permissions structure
        result.permissions?.let { permissions ->
            assertThat(permissions.canCreateTenants).isFalse() // No SUPER_ADMIN role
            assertThat(permissions.canManageUsers).isTrue() // Has TENANT_ADMIN role
            assertThat(permissions.canViewAuditLogs).isTrue()
            assertThat(permissions.canModifyConfiguration).isFalse() // No SUPER_ADMIN role
        }

        // Validate metadata structure
        assertThat(result.metadata.timestamp).isBeforeOrEqualTo(Instant.now())
        assertThat(result.metadata.requestId).startsWith("req-")
        assertThat(result.metadata.version).isEqualTo("1.0.0")
    }

    @Test
    fun `should return user preferences with correct type structure`() {
        // When
        val result = userInfoEndpoint.getUserPreferences()

        // Then - Validate type safety
        assertThat(result.success).isTrue()
        assertThat(result.preferences).isNotNull
        assertThat(result.metadata).isNotNull
        assertThat(result.error).isNull()

        // Validate preferences structure
        result.preferences?.let { prefs ->
            assertThat(prefs.theme).isEqualTo("dark")
            assertThat(prefs.language).isEqualTo("en")
            assertThat(prefs.timezone).isEqualTo("UTC")
            assertThat(prefs.dateFormat).isEqualTo("yyyy-MM-dd")
            assertThat(prefs.itemsPerPage).isEqualTo(25)
            assertThat(prefs.enableNotifications).isTrue()
            assertThat(prefs.dashboardLayout).containsKey("widgets")
            assertThat(prefs.dashboardLayout).containsKey("columns")
        }
    }

    @Test
    fun `should update user preferences with valid request`() {
        // Given
        val request =
            UpdatePreferencesRequest(
                theme = "light",
                language = "de",
                timezone = "Europe/Berlin",
                dateFormat = "dd.MM.yyyy",
                itemsPerPage = 50,
                enableNotifications = false,
                dashboardLayout = mapOf("layout" to "grid", "columns" to 2),
            )

        // When
        val result = userInfoEndpoint.updateUserPreferences(request)

        // Then - Validate updated preferences
        assertThat(result.success).isTrue()
        assertThat(result.preferences).isNotNull

        result.preferences?.let { prefs ->
            assertThat(prefs.theme).isEqualTo("light")
            assertThat(prefs.language).isEqualTo("de")
            assertThat(prefs.timezone).isEqualTo("Europe/Berlin")
            assertThat(prefs.dateFormat).isEqualTo("dd.MM.yyyy")
            assertThat(prefs.itemsPerPage).isEqualTo(50)
            assertThat(prefs.enableNotifications).isFalse()
            assertThat(prefs.dashboardLayout["layout"]).isEqualTo("grid")
        }
    }

    @Test
    fun `should reject invalid theme preference`() {
        // Given
        val request = UpdatePreferencesRequest(theme = "invalid-theme")

        // When & Then
        val exception =
            assertThrows<EndpointException> { userInfoEndpoint.updateUserPreferences(request) }

        assertThat(exception.message)
            .contains("Invalid theme: invalid-theme. Must be one of: [light, dark, auto]")
    }

    @Test
    fun `should reject unsupported language preference`() {
        // Given
        val request = UpdatePreferencesRequest(language = "invalid-lang")

        // When & Then
        val exception =
            assertThrows<EndpointException> { userInfoEndpoint.updateUserPreferences(request) }

        assertThat(exception.message).contains("Unsupported language")
    }

    @Test
    fun `should reject invalid items per page preference`() {
        // Given - Test both too low and too high values
        val tooLowRequest = UpdatePreferencesRequest(itemsPerPage = 2)
        val tooHighRequest = UpdatePreferencesRequest(itemsPerPage = 150)

        // When & Then - Too low
        val lowException =
            assertThrows<EndpointException> {
                userInfoEndpoint.updateUserPreferences(tooLowRequest)
            }
        assertThat(lowException.message).contains("Items per page must be between 5 and 100")

        // When & Then - Too high
        val highException =
            assertThrows<EndpointException> {
                userInfoEndpoint.updateUserPreferences(tooHighRequest)
            }
        assertThat(highException.message).contains("Items per page must be between 5 and 100")
    }

    @Test
    fun `should handle partial preference updates`() {
        // Given - Only update theme
        val request = UpdatePreferencesRequest(theme = "auto")

        // When
        val result = userInfoEndpoint.updateUserPreferences(request)

        // Then - Only theme should change, others should use defaults
        assertThat(result.success).isTrue()
        result.preferences?.let { prefs ->
            assertThat(prefs.theme).isEqualTo("auto")
            assertThat(prefs.language).isEqualTo("en") // Default
            assertThat(prefs.timezone).isEqualTo("UTC") // Default
            assertThat(prefs.itemsPerPage).isEqualTo(25) // Default
        }
    }

    @Test
    fun `should validate response metadata consistency`() {
        // When
        val userInfoResult = userInfoEndpoint.getCurrentUser()
        val preferencesResult = userInfoEndpoint.getUserPreferences()

        // Then - All responses should have consistent metadata structure
        assertThat(userInfoResult.metadata.version).isEqualTo("1.0.0")
        assertThat(preferencesResult.metadata.version).isEqualTo("1.0.0")

        assertThat(userInfoResult.metadata.requestId).isNotEmpty()
        assertThat(preferencesResult.metadata.requestId).isNotEmpty()

        assertThat(userInfoResult.metadata.timestamp).isBeforeOrEqualTo(Instant.now())
        assertThat(preferencesResult.metadata.timestamp).isBeforeOrEqualTo(Instant.now())
    }
}

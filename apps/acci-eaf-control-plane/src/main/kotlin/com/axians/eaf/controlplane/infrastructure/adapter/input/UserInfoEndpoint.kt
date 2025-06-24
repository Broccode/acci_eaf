package com.axians.eaf.controlplane.infrastructure.adapter.input

import com.vaadin.hilla.Endpoint
import com.vaadin.hilla.exception.EndpointException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * User information endpoint that provides authenticated user details and tenant context.
 * Demonstrates EAF IAM integration and type-safe Hilla communication.
 */
@Endpoint
@Service
class UserInfoEndpoint {
    // TODO: Inject EAF security context holder when available
    // @Autowired
    // private lateinit var eafSecurityContextHolder: EafSecurityContextHolder

    /**
     * Get current authenticated user information with tenant context. Requires valid
     * authentication.
     */
    @PreAuthorize("isAuthenticated()")
    fun getCurrentUser(): UserInfoResponse =
        try {
            // TODO: Replace with actual EAF security context when available
            // val userId = eafSecurityContextHolder.getUserId()
            // val tenantId = eafSecurityContextHolder.getTenantId()
            // val userRoles = eafSecurityContextHolder.getUserRoles()

            // Mock implementation for demonstration
            val userId = "user-${System.currentTimeMillis()}"
            val tenantId = "tenant-demo-123"
            val userRoles = listOf("TENANT_ADMIN", "CONTROL_PLANE_USER")

            UserInfoResponse(
                success = true,
                user =
                    UserInfo(
                        userId = userId,
                        username = "admin@demo-tenant.com",
                        email = "admin@demo-tenant.com",
                        firstName = "Demo",
                        lastName = "Administrator",
                        roles = userRoles,
                        tenantId = tenantId,
                        tenantName = "Demo Tenant Corporation",
                        lastLoginAt =
                            Instant
                                .now()
                                .minusSeconds(300), // 5 minutes ago
                        accountStatus = "ACTIVE",
                    ),
                permissions =
                    UserPermissions(
                        canCreateTenants =
                            userRoles.contains("SUPER_ADMIN"),
                        canManageUsers = userRoles.contains("TENANT_ADMIN"),
                        canViewAuditLogs = true,
                        canModifyConfiguration =
                            userRoles.contains("SUPER_ADMIN"),
                    ),
                metadata =
                    ResponseMetadata(
                        timestamp = Instant.now(),
                        requestId = "req-${System.currentTimeMillis()}",
                        version = "1.0.0",
                    ),
            )
        } catch (exception: Exception) {
            throw EndpointException(
                "Failed to retrieve user information: ${exception.message}",
            )
        }

    /** Get user preferences and settings. Demonstrates complex object handling in Hilla. */
    @PreAuthorize("isAuthenticated()")
    fun getUserPreferences(): UserPreferencesResponse =
        try {
            UserPreferencesResponse(
                success = true,
                preferences =
                    UserPreferences(
                        theme = "dark",
                        language = "en",
                        timezone = "UTC",
                        dateFormat = "yyyy-MM-dd",
                        itemsPerPage = 25,
                        enableNotifications = true,
                        dashboardLayout =
                            mapOf(
                                "widgets" to
                                    listOf(
                                        "tenants",
                                        "users",
                                        "activity",
                                    ),
                                "columns" to 3,
                            ),
                    ),
                metadata =
                    ResponseMetadata(
                        timestamp = Instant.now(),
                        requestId = "pref-${System.currentTimeMillis()}",
                        version = "1.0.0",
                    ),
            )
        } catch (exception: Exception) {
            throw EndpointException(
                "Failed to retrieve user preferences: ${exception.message}",
            )
        }

    /**
     * Update user preferences with validation. Demonstrates input validation and error
     * handling.
     */
    @PreAuthorize("isAuthenticated()")
    fun updateUserPreferences(request: UpdatePreferencesRequest): UserPreferencesResponse =
        try {
            // Validate request
            validatePreferencesRequest(request)

            // TODO: Persist preferences when user repository is available
            // userPreferencesRepository.save(request.toEntity(currentUserId))

            // Return updated preferences
            UserPreferencesResponse(
                success = true,
                preferences =
                    UserPreferences(
                        theme = request.theme ?: "light",
                        language = request.language ?: "en",
                        timezone = request.timezone ?: "UTC",
                        dateFormat = request.dateFormat ?: "yyyy-MM-dd",
                        itemsPerPage = request.itemsPerPage ?: 25,
                        enableNotifications =
                            request.enableNotifications
                                ?: true,
                        dashboardLayout =
                            request.dashboardLayout
                                ?: emptyMap(),
                    ),
                metadata =
                    ResponseMetadata(
                        timestamp = Instant.now(),
                        requestId = "update-${System.currentTimeMillis()}",
                        version = "1.0.0",
                    ),
            )
        } catch (exception: ValidationException) {
            throw EndpointException("Invalid preferences: ${exception.message}")
        } catch (exception: Exception) {
            throw EndpointException(
                "Failed to update preferences: ${exception.message}",
            )
        }

    private fun validatePreferencesRequest(request: UpdatePreferencesRequest) {
        request.theme?.let { theme ->
            if (theme !in listOf("light", "dark", "auto")) {
                throw ValidationException(
                    "Invalid theme: $theme. Must be 'light', 'dark', or 'auto'",
                )
            }
        }

        request.language?.let { language ->
            if (language !in listOf("en", "de", "fr", "es")) {
                throw ValidationException("Unsupported language: $language")
            }
        }

        request.itemsPerPage?.let { itemsPerPage ->
            if (itemsPerPage < 5 || itemsPerPage > 100) {
                throw ValidationException(
                    "Items per page must be between 5 and 100",
                )
            }
        }
    }
}

// Model classes are defined in CommonModels.kt for proper Hilla TypeScript generation

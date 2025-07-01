package com.axians.eaf.controlplane.infrastructure.adapter.input

import com.axians.eaf.controlplane.infrastructure.adapter.input.common.EndpointExceptionHandler
import com.axians.eaf.controlplane.infrastructure.adapter.input.common.ResponseConstants
import com.axians.eaf.controlplane.infrastructure.adapter.input.common.ValidationException
import com.vaadin.hilla.Endpoint
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import java.time.Instant

/**
 * Hilla endpoint for user information operations.
 *
 * FIXME: Temporarily disabled due to KotlinNullabilityPlugin crash in Vaadin 24.8.0 See:
 * https://github.com/vaadin/hilla/issues/3443 Remove comment from @Endpoint when Vaadin/Hilla ships
 * the fix.
 */
@Endpoint
class UserInfoEndpoint {
    companion object {
        private val logger = LoggerFactory.getLogger(UserInfoEndpoint::class.java)

        // Validation constants
        private val VALID_THEMES = listOf("light", "dark", "auto")
        private val VALID_LANGUAGES = listOf("en", "de", "fr", "es")
        private const val MIN_ITEMS_PER_PAGE = 5
        private const val MAX_ITEMS_PER_PAGE = 100
        private const val DEFAULT_ITEMS_PER_PAGE = 25
        private const val DEFAULT_LAST_LOGIN_OFFSET_SECONDS = 300L // 5 minutes ago
    }

    // TODO: Inject EAF security context holder when available
    // @Autowired
    // private lateinit var eafSecurityContextHolder: EafSecurityContextHolder

    /**
     * Get current authenticated user information with tenant context. Requires valid
     * authentication.
     */
    @PreAuthorize("isAuthenticated()")
    fun getCurrentUser(): UserInfoResponse =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "getCurrentUser",
            details = "",
        ) {
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
                                .minusSeconds(
                                    DEFAULT_LAST_LOGIN_OFFSET_SECONDS,
                                ),
                        accountStatus = "ACTIVE",
                    ),
                permissions =
                    UserPermissions(
                        canCreateTenants = userRoles.contains("SUPER_ADMIN"),
                        canManageUsers = userRoles.contains("TENANT_ADMIN"),
                        canViewAuditLogs = true,
                        canModifyConfiguration = userRoles.contains("SUPER_ADMIN"),
                    ),
                metadata =
                    ResponseMetadata(
                        timestamp = Instant.now(),
                        requestId = "req-${System.currentTimeMillis()}",
                        version = ResponseConstants.API_VERSION,
                    ),
            )
        }
            ?: throw ValidationException("Failed to retrieve user information")

    /** Get user preferences and settings. Demonstrates complex object handling in Hilla. */
    @PreAuthorize("isAuthenticated()")
    fun getUserPreferences(): UserPreferencesResponse =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "getUserPreferences",
            details = "",
        ) {
            UserPreferencesResponse(
                success = true,
                preferences =
                    UserPreferences(
                        theme = "dark",
                        language = "en",
                        timezone = "UTC",
                        dateFormat = "yyyy-MM-dd",
                        itemsPerPage = DEFAULT_ITEMS_PER_PAGE,
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
                        version = ResponseConstants.API_VERSION,
                    ),
            )
        }
            ?: throw ValidationException("Failed to retrieve user preferences")

    /**
     * Update user preferences with validation. Demonstrates input validation and error handling.
     */
    @PreAuthorize("isAuthenticated()")
    fun updateUserPreferences(request: UpdatePreferencesRequest): UserPreferencesResponse =
        EndpointExceptionHandler.handleEndpointOperation(
            logger = logger,
            operation = "updateUserPreferences",
            details = "request=$request",
        ) {
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
                        itemsPerPage =
                            request.itemsPerPage
                                ?: DEFAULT_ITEMS_PER_PAGE,
                        enableNotifications = request.enableNotifications ?: true,
                        dashboardLayout = request.dashboardLayout ?: emptyMap(),
                    ),
                metadata =
                    ResponseMetadata(
                        timestamp = Instant.now(),
                        requestId = "update-${System.currentTimeMillis()}",
                        version = ResponseConstants.API_VERSION,
                    ),
            )
        }
            ?: throw ValidationException("Failed to update preferences")

    private fun validatePreferencesRequest(request: UpdatePreferencesRequest) {
        request.theme?.let { theme ->
            if (theme !in VALID_THEMES) {
                throw ValidationException("Invalid theme: $theme. Must be one of: $VALID_THEMES")
            }
        }

        request.language?.let { language ->
            if (language !in VALID_LANGUAGES) {
                throw ValidationException("Unsupported language: $language")
            }
        }

        request.itemsPerPage?.let { itemsPerPage ->
            if (itemsPerPage < MIN_ITEMS_PER_PAGE || itemsPerPage > MAX_ITEMS_PER_PAGE) {
                throw ValidationException(
                    "Items per page must be between $MIN_ITEMS_PER_PAGE and $MAX_ITEMS_PER_PAGE",
                )
            }
        }
    }
}

// Model classes are defined in CommonModels.kt for proper Hilla TypeScript generation

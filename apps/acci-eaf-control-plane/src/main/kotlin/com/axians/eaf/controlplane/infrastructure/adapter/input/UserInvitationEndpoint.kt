package com.axians.eaf.controlplane.infrastructure.adapter.input

import com.axians.eaf.controlplane.application.dto.invitation.AcceptInvitationRequest
import com.axians.eaf.controlplane.application.dto.invitation.AcceptInvitationResponse
import com.axians.eaf.controlplane.application.dto.invitation.CancelInvitationResponse
import com.axians.eaf.controlplane.application.dto.invitation.InvitationFilter
import com.axians.eaf.controlplane.application.dto.invitation.InvitationResponse
import com.axians.eaf.controlplane.application.dto.invitation.InvitationSummary
import com.axians.eaf.controlplane.application.dto.invitation.InviteUserRequest
import com.axians.eaf.controlplane.application.dto.invitation.PagedResponse
import com.axians.eaf.controlplane.domain.model.invitation.InvitationId
import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.UserId
import com.axians.eaf.controlplane.domain.service.InvitationResult
import com.axians.eaf.controlplane.domain.service.InvitationService
import com.fasterxml.jackson.annotation.JsonProperty
import com.vaadin.flow.server.auth.AnonymousAllowed
import com.vaadin.hilla.exception.EndpointException
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize

/**
 * Hilla endpoint for user invitation management operations. Provides type-safe methods for
 * invitation workflow including creation, management, and acceptance.
 *
 * FIXME: Temporarily disabled - ALL collection types trigger KotlinNullabilityPlugin crash See:
 * https://github.com/vaadin/hilla/issues/3443 Will re-enable when Vaadin/Hilla ships the fix.
 */

// @Endpoint
// @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN", "TENANT_ADMIN")
class UserInvitationEndpoint(
        private val invitationService: InvitationService,
) {
    /** Invite a new user to join the current tenant. Requires tenant admin privileges or higher. */
    suspend fun inviteUser(
            @Valid request: InviteUserRequest,
            tenantId: String,
    ): InvitationResponse {
        try {
            // TODO: Get current user from security context
            val currentUserId = UserId("current-admin-user") // Placeholder

            val result =
                    invitationService.inviteUser(
                            request = request,
                            tenantId = TenantId(tenantId),
                            invitedBy = currentUserId,
                    )

            return when (result) {
                is InvitationResult.Success<*> -> result.data as InvitationResponse
                is InvitationResult.Error -> throw EndpointException(result.message)
            }
        } catch (e: EndpointException) {
            throw e
        } catch (e: Exception) {
            throw EndpointException("Failed to invite user: ${e.message}")
        }
    }

    /** Resend an existing invitation. Generates a new token and extends expiration. */
    suspend fun resendInvitation(invitationId: String): InvitationResponse {
        try {
            // TODO: Get current user from security context
            val currentUserId = UserId("current-admin-user") // Placeholder

            val result =
                    invitationService.resendInvitation(
                            invitationId = InvitationId(invitationId),
                            resentBy = currentUserId,
                    )

            return when (result) {
                is InvitationResult.Success<*> -> result.data as InvitationResponse
                is InvitationResult.Error -> throw EndpointException(result.message)
            }
        } catch (e: EndpointException) {
            throw e
        } catch (e: Exception) {
            throw EndpointException("Failed to resend invitation: ${e.message}")
        }
    }

    /** Cancel an existing invitation. Only pending invitations can be cancelled. */
    suspend fun cancelInvitation(
            invitationId: String,
            reason: String = "Cancelled by administrator",
    ): CancelInvitationResponse {
        try {
            // TODO: Get current user from security context
            val currentUserId = UserId("current-admin-user") // Placeholder

            val result =
                    invitationService.cancelInvitation(
                            invitationId = InvitationId(invitationId),
                            cancelledBy = currentUserId,
                            reason = reason,
                    )

            return when (result) {
                is InvitationResult.Success<*> -> result.data as CancelInvitationResponse
                is InvitationResult.Error -> throw EndpointException(result.message)
            }
        } catch (e: EndpointException) {
            throw e
        } catch (e: Exception) {
            throw EndpointException("Failed to cancel invitation: ${e.message}")
        }
    }

    /**
     * Accept an invitation using the secure token. This endpoint is accessible without
     * authentication for new users.
     */
    @AnonymousAllowed
    suspend fun acceptInvitation(
            token: String,
            @Valid request: AcceptInvitationRequest,
    ): AcceptInvitationResponse {
        try {
            val result =
                    invitationService.acceptInvitation(
                            token = token,
                            request = request,
                    )

            return when (result) {
                is InvitationResult.Success<*> -> result.data as AcceptInvitationResponse
                is InvitationResult.Error -> throw EndpointException(result.message)
            }
        } catch (e: EndpointException) {
            throw e
        } catch (e: Exception) {
            throw EndpointException("Failed to accept invitation: ${e.message}")
        }
    }

    /**
     * List invitations with optional filtering and pagination. Tenant admins can only see
     * invitations for their tenant.
     */
    suspend fun listInvitations(
            email: String? = null,
            status: String? = null,
            invitedBy: String? = null,
            includeExpired: Boolean = false,
            pageSize: Int = 20,
            pageNumber: Int = 0,
    ): PagedResponse<InvitationSummary> {
        try {
            val filter =
                    InvitationFilter(
                            email = email,
                            status =
                                    status?.let {
                                        try {
                                            com.axians.eaf.controlplane.domain.model.invitation
                                                    .InvitationStatus.valueOf(it)
                                        } catch (e: IllegalArgumentException) {
                                            throw EndpointException("Invalid status value: $it")
                                        }
                                    },
                            invitedBy = invitedBy,
                            includeExpired = includeExpired,
                            pageSize = pageSize,
                            pageNumber = pageNumber,
                    )

            val result = invitationService.listInvitations(filter)

            return when (result) {
                is InvitationResult.Success<*> -> {
                    @Suppress("UNCHECKED_CAST") result.data as PagedResponse<InvitationSummary>
                }
                is InvitationResult.Error -> throw EndpointException(result.message)
            }
        } catch (e: EndpointException) {
            throw e
        } catch (e: Exception) {
            throw EndpointException("Failed to list invitations: ${e.message}")
        }
    }

    /**
     * Get a public invitation details for the acceptance page. This endpoint is accessible without
     * authentication.
     */
    @AnonymousAllowed
    suspend fun getInvitationByToken(token: String): InvitationPublicDetails {
        try {
            // This would typically call a service method to get public invitation info
            // For now, we'll create a placeholder response
            return InvitationPublicDetails(
                    email = "placeholder@example.com",
                    firstName = "Placeholder",
                    lastName = "User",
                    tenantName = "Example Organization",
                    rolesString = "USER",
                    expiresAt = java.time.Instant.now().plusSeconds(7 * 24 * 60 * 60),
                    isValid = true,
            )
        } catch (e: Exception) {
            throw EndpointException("Failed to get invitation details: ${e.message}")
        }
    }

    /** Administrative function to process expired invitations. Only super admins can call this. */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    suspend fun processExpiredInvitations(): String {
        try {
            val result = invitationService.processExpiredInvitations()

            return when (result) {
                is InvitationResult.Success<*> -> result.data as String
                is InvitationResult.Error -> throw EndpointException(result.message)
            }
        } catch (e: EndpointException) {
            throw e
        } catch (e: Exception) {
            throw EndpointException("Failed to process expired invitations: ${e.message}")
        }
    }
}

/**
 * Public invitation details for the acceptance page. Contains only information needed for
 * invitation acceptance without sensitive data.
 */
data class InvitationPublicDetails(
        @JsonProperty("email") val email: String,
        @JsonProperty("firstName") val firstName: String,
        @JsonProperty("lastName") val lastName: String,
        @JsonProperty("tenantName") val tenantName: String,
        @JsonProperty("rolesString") val rolesString: String,
        @JsonProperty("expiresAt") val expiresAt: java.time.Instant,
        @JsonProperty("isValid") val isValid: Boolean,
)

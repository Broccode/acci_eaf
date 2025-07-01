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
import com.axians.eaf.controlplane.infrastructure.adapter.input.common.EndpointExceptionHandler
import com.axians.eaf.controlplane.infrastructure.adapter.input.common.ResponseConstants
import com.fasterxml.jackson.annotation.JsonProperty
import com.vaadin.flow.server.auth.AnonymousAllowed
import com.vaadin.hilla.exception.EndpointException
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize

// @Endpoint
// @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN", "TENANT_ADMIN")

/**
 * Hilla endpoint for user invitation management operations. Provides type-safe methods for
 * invitation workflow including creation, management, and acceptance.
 *
 * FIXME: Temporarily disabled - ALL collection types trigger KotlinNullabilityPlugin crash See:
 * https://github.com/vaadin/hilla/issues/3443 Will re-enable when Vaadin/Hilla ships the fix.
 */
class UserInvitationEndpoint(
    private val invitationService: InvitationService,
) {
    private val logger = LoggerFactory.getLogger(UserInvitationEndpoint::class.java)

    /** Processes invitation result and returns data or throws appropriate exception. */
    private fun processInvitationResult(result: InvitationResult): Any =
        when (result) {
            is InvitationResult.Success<*> ->
                result.data
                    ?: throw EndpointException("Null result from successful operation")
            is InvitationResult.Error -> throw EndpointException(result.message)
        }

    /** Invite a new user to join the current tenant. Requires tenant admin privileges or higher. */
    suspend fun inviteUser(
        @Valid request: InviteUserRequest,
        tenantId: String,
    ): InvitationResponse =
        EndpointExceptionHandler.handleSuspendEndpointExecution("inviteUser", logger) {
            val currentUserId = UserId("current-admin-user") // Placeholder

            val result =
                invitationService.inviteUser(
                    request = request,
                    tenantId = TenantId(tenantId),
                    invitedBy = currentUserId,
                )

            processInvitationResult(result) as InvitationResponse
        }

    /** Resend an existing invitation. Generates a new token and extends expiration. */
    suspend fun resendInvitation(invitationId: String): InvitationResponse =
        EndpointExceptionHandler.handleSuspendEndpointExecution("resendInvitation", logger) {
            val currentUserId = UserId("current-admin-user") // Placeholder

            val result =
                invitationService.resendInvitation(
                    invitationId = InvitationId(invitationId),
                    resentBy = currentUserId,
                )

            processInvitationResult(result) as InvitationResponse
        }

    /** Cancel an existing invitation. Only pending invitations can be cancelled. */
    suspend fun cancelInvitation(
        invitationId: String,
        reason: String = "Cancelled by administrator",
    ): CancelInvitationResponse =
        EndpointExceptionHandler.handleSuspendEndpointExecution("cancelInvitation", logger) {
            val currentUserId = UserId("current-admin-user") // Placeholder

            val result =
                invitationService.cancelInvitation(
                    invitationId = InvitationId(invitationId),
                    cancelledBy = currentUserId,
                    reason = reason,
                )

            processInvitationResult(result) as CancelInvitationResponse
        }

    /**
     * Accept an invitation using the secure token. This endpoint is accessible without
     * authentication for new users.
     */
    @AnonymousAllowed
    suspend fun acceptInvitation(
        @Valid request: AcceptInvitationRequest,
    ): AcceptInvitationResponse =
        EndpointExceptionHandler.handleSuspendEndpointExecution("acceptInvitation", logger) {
            val result =
                invitationService.acceptInvitation(
                    token = request.token,
                    request = request,
                )

            processInvitationResult(result) as AcceptInvitationResponse
        }

    /**
     * List invitations with optional filtering and pagination. Tenant admins can only see
     * invitations for their tenant.
     */
    suspend fun listInvitations(
        params: InvitationListParameters = InvitationListParameters(),
    ): PagedResponse<InvitationSummary> =
        EndpointExceptionHandler.handleSuspendEndpointExecution("listInvitations", logger) {
            val filter =
                InvitationFilter(
                    email = params.email,
                    status =
                        params.status?.let {
                            com.axians.eaf.controlplane.domain.model.invitation
                                .InvitationStatus
                                .valueOf(
                                    it,
                                )
                        },
                    invitedBy = params.invitedBy,
                    includeExpired = params.includeExpired,
                    pageSize = params.pageSize,
                    pageNumber = params.pageNumber,
                )

            val result = invitationService.listInvitations(filter)

            @Suppress("UNCHECKED_CAST")
            processInvitationResult(result) as PagedResponse<InvitationSummary>
        }

    /**
     * Get a public invitation details for the acceptance page. This endpoint is accessible without
     * authentication.
     */
    @AnonymousAllowed
    suspend fun getInvitationByToken(token: String): InvitationPublicDetails =
        EndpointExceptionHandler.handleSuspendEndpointExecution(
            "getInvitationByToken",
            logger,
        ) {
            // This would typically call a service method to get public invitation info
            // For now, we'll create a placeholder response
            InvitationPublicDetails(
                email = "placeholder@example.com",
                firstName = "Placeholder",
                lastName = "User",
                tenantName = "Example Organization",
                rolesString = "USER",
                expiresAt =
                    java.time.Instant
                        .now()
                        .plusSeconds(ResponseConstants.INVITATION_EXPIRY_SECONDS),
                isValid = true,
            )
        }

    /** Administrative function to process expired invitations. Only super admins can call this. */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    suspend fun processExpiredInvitations(): String =
        EndpointExceptionHandler.handleSuspendEndpointExecution(
            "processExpiredInvitations",
            logger,
        ) {
            val result = invitationService.processExpiredInvitations()
            processInvitationResult(result) as String
        }
}

/**
 * Parameters for listing invitations with optional filtering and pagination. Addresses detekt
 * LongParameterList issue by grouping related parameters.
 */
data class InvitationListParameters(
    val email: String? = null,
    val status: String? = null,
    val invitedBy: String? = null,
    val includeExpired: Boolean = false,
    val pageSize: Int = ResponseConstants.DEFAULT_PAGE_SIZE,
    val pageNumber: Int = ResponseConstants.DEFAULT_PAGE_NUMBER,
)

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

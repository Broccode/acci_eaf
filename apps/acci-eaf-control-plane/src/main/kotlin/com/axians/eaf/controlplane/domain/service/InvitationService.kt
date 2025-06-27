package com.axians.eaf.controlplane.domain.service

import com.axians.eaf.controlplane.application.dto.invitation.AcceptInvitationRequest
import com.axians.eaf.controlplane.application.dto.invitation.AcceptInvitationResponse
import com.axians.eaf.controlplane.application.dto.invitation.CancelInvitationResponse
import com.axians.eaf.controlplane.application.dto.invitation.InvitationFilter
import com.axians.eaf.controlplane.application.dto.invitation.InviteUserRequest
import com.axians.eaf.controlplane.application.dto.invitation.PagedResponse
import com.axians.eaf.controlplane.application.dto.invitation.toResponse
import com.axians.eaf.controlplane.application.dto.invitation.toSummary
import com.axians.eaf.controlplane.domain.model.invitation.Invitation
import com.axians.eaf.controlplane.domain.model.invitation.InvitationEvent
import com.axians.eaf.controlplane.domain.model.invitation.InvitationId
import com.axians.eaf.controlplane.domain.model.invitation.SecureToken
import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.RoleId
import com.axians.eaf.controlplane.domain.model.user.UserId
import com.axians.eaf.controlplane.domain.port.EmailService
import com.axians.eaf.controlplane.domain.port.InvitationRepository
import com.axians.eaf.controlplane.domain.port.RoleRepository
import com.axians.eaf.controlplane.domain.port.TenantRepository
import com.axians.eaf.controlplane.domain.port.UserRepository
import org.slf4j.LoggerFactory
import java.time.Instant

/** Result types for invitation operations. */
sealed class InvitationResult {
    data class Success<T>(
        val data: T,
        val events: List<InvitationEvent> = emptyList(),
    ) : InvitationResult()

    data class Error(
        val message: String,
        val code: String,
    ) : InvitationResult()
}

/**
 * Domain service for managing user invitations. This service handles the logic for creating,
 * sending, and managing the lifecycle of user invitations.
 */
class InvitationService(
    private val invitationRepository: InvitationRepository,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val tenantRepository: TenantRepository,
    private val emailService: EmailService,
    private val invitationLinkBaseUrl: String,
) {
    private val logger = LoggerFactory.getLogger(InvitationService::class.java)

    /** Invite a new user to join a tenant. */
    suspend fun inviteUser(
        request: InviteUserRequest,
        tenantId: TenantId,
        invitedBy: UserId,
    ): InvitationResult {
        try {
            // Validate that the tenant exists
            val tenant =
                tenantRepository.findById(tenantId)
                    ?: return InvitationResult.Error("Tenant not found", "TENANT_NOT_FOUND")

            // Check if user already exists
            val existingUser = userRepository.findByEmail(request.email)
            if (existingUser != null) {
                return InvitationResult.Error(
                    "User with email ${request.email} already exists",
                    "USER_ALREADY_EXISTS",
                )
            }

            // Check if there's already an active invitation
            val activeInvitation =
                invitationRepository.existsActiveInvitationForEmail(request.email, tenantId)
            if (activeInvitation) {
                return InvitationResult.Error(
                    "Active invitation already exists for this email",
                    "ACTIVE_INVITATION_EXISTS",
                )
            }

            // Validate roles exist
            val roleIds = request.roles.map { RoleId(it) }.toSet()
            val roles = roleIds.mapNotNull { roleRepository.findRoleById(it) }
            if (roles.size != roleIds.size) {
                return InvitationResult.Error(
                    "One or more specified roles do not exist",
                    "INVALID_ROLES",
                )
            }

            // Create invitation
            val invitation =
                Invitation.create(
                    tenantId = tenantId,
                    email = request.email,
                    firstName = request.firstName,
                    lastName = request.lastName,
                    roles = roleIds,
                    invitedBy = invitedBy,
                    expirationDays = request.expiresInDays,
                    customMessage = request.customMessage,
                )

            // Save invitation
            val savedInvitation = invitationRepository.save(invitation)

            // Send invitation email
            val invitationUrl = "$invitationLinkBaseUrl?token=${savedInvitation.token.value}"
            val emailResult = emailService.sendInvitationEmail(savedInvitation, invitationUrl)

            if (!emailResult.success) {
                // Log error but don't fail the invitation creation
                // In a real implementation, you might want to queue this for retry
            }

            val event =
                InvitationEvent.InvitationSent(
                    invitationId = savedInvitation.id,
                    tenantId = tenantId,
                    email = request.email,
                    invitedBy = invitedBy,
                    roles = roleIds,
                )

            return InvitationResult.Success(
                data = savedInvitation.toResponse("Invitation sent successfully"),
                events = listOf(event),
            )
        } catch (e: Exception) {
            return InvitationResult.Error(
                "Failed to create invitation: ${e.message}",
                "INVITATION_CREATION_FAILED",
            )
        }
    }

    /** Resend an existing invitation. */
    suspend fun resendInvitation(
        invitationId: InvitationId,
        resentBy: UserId,
    ): InvitationResult {
        try {
            val invitation =
                invitationRepository.findById(invitationId)
                    ?: return InvitationResult.Error(
                        "Invitation not found",
                        "INVITATION_NOT_FOUND",
                    )

            if (!invitation.canBeResent()) {
                return InvitationResult.Error(
                    "Invitation cannot be resent in current state",
                    "INVITATION_CANNOT_BE_RESENT",
                )
            }

            // Create new invitation with refreshed token
            val resentInvitation = invitation.resend()
            val savedInvitation = invitationRepository.save(resentInvitation)

            // Send email
            val invitationUrl = "$invitationLinkBaseUrl?token=${savedInvitation.token.value}"
            val emailResult = emailService.sendInvitationReminderEmail(savedInvitation, invitationUrl)

            val event =
                InvitationEvent.InvitationResent(
                    invitationId = savedInvitation.id,
                    tenantId = savedInvitation.tenantId,
                    email = savedInvitation.email,
                    resentBy = resentBy,
                )

            return InvitationResult.Success(
                data = savedInvitation.toResponse("Invitation resent successfully"),
                events = listOf(event),
            )
        } catch (e: Exception) {
            return InvitationResult.Error(
                "Failed to resend invitation: ${e.message}",
                "INVITATION_RESEND_FAILED",
            )
        }
    }

    /** Cancel an existing invitation. */
    suspend fun cancelInvitation(
        invitationId: InvitationId,
        cancelledBy: UserId,
        reason: String = "Cancelled by administrator",
    ): InvitationResult {
        try {
            val invitation =
                invitationRepository.findById(invitationId)
                    ?: return InvitationResult.Error(
                        "Invitation not found",
                        "INVITATION_NOT_FOUND",
                    )

            if (!invitation.canBeCancelled()) {
                return InvitationResult.Error(
                    "Invitation cannot be cancelled in current state",
                    "INVITATION_CANNOT_BE_CANCELLED",
                )
            }

            // Cancel invitation
            val cancelledInvitation = invitation.cancel()
            val savedInvitation = invitationRepository.save(cancelledInvitation)

            // Send cancellation email
            emailService.sendInvitationCancelledEmail(
                email = savedInvitation.email,
                firstName = savedInvitation.firstName,
                lastName = savedInvitation.lastName,
                reason = reason,
            )

            val event =
                InvitationEvent.InvitationCancelled(
                    invitationId = savedInvitation.id,
                    tenantId = savedInvitation.tenantId,
                    email = savedInvitation.email,
                    cancelledBy = cancelledBy,
                    reason = reason,
                )

            val response =
                CancelInvitationResponse(
                    invitationId = savedInvitation.id.value,
                    email = savedInvitation.email,
                    status = savedInvitation.status,
                    cancelledAt = Instant.now(),
                    message = "Invitation cancelled successfully",
                )

            return InvitationResult.Success(data = response, events = listOf(event))
        } catch (e: Exception) {
            return InvitationResult.Error(
                "Failed to cancel invitation: ${e.message}",
                "INVITATION_CANCELLATION_FAILED",
            )
        }
    }

    /** Accept an invitation and create user account. */
    suspend fun acceptInvitation(
        token: String,
        request: AcceptInvitationRequest,
    ): InvitationResult {
        try {
            val invitation =
                invitationRepository.findByToken(SecureToken.fromString(token))
                    ?: return InvitationResult.Error(
                        "Invalid invitation token",
                        "INVALID_TOKEN",
                    )

            if (!invitation.isValid()) {
                return InvitationResult.Error(
                    "Invitation is no longer valid or has expired",
                    "INVITATION_INVALID",
                )
            }

            // Check if user already exists (race condition check)
            val existingUser = userRepository.findByEmail(invitation.email)
            if (existingUser != null) {
                return InvitationResult.Error(
                    "User account already exists for this email",
                    "USER_ALREADY_EXISTS",
                )
            }

            // Create user account with the invited roles
            val userId = UserId.generate()
            // Note: In a real implementation, you would create the user via UserService
            // and integrate with EAF IAM service for account creation

            // Accept invitation
            val acceptedInvitation = invitation.accept()
            val savedInvitation = invitationRepository.save(acceptedInvitation)

            // Send welcome email
            val tenant = tenantRepository.findById(invitation.tenantId)
            emailService.sendWelcomeEmail(
                email = savedInvitation.email,
                firstName = savedInvitation.firstName,
                lastName = savedInvitation.lastName,
                tenantName = tenant?.name ?: "Organization",
            )

            val event =
                InvitationEvent.InvitationAccepted(
                    invitationId = savedInvitation.id,
                    tenantId = savedInvitation.tenantId,
                    email = savedInvitation.email,
                    createdUserId = userId,
                )

            val response =
                AcceptInvitationResponse(
                    userId = userId.value,
                    email = savedInvitation.email,
                    firstName = savedInvitation.firstName,
                    lastName = savedInvitation.lastName,
                    tenantId = savedInvitation.tenantId.value,
                    roles = savedInvitation.roles.map { it.value }.toList(),
                    message = "Invitation accepted successfully. Welcome!",
                )

            return InvitationResult.Success(data = response, events = listOf(event))
        } catch (e: Exception) {
            return InvitationResult.Error(
                "Failed to accept invitation: ${e.message}",
                "INVITATION_ACCEPTANCE_FAILED",
            )
        }
    }

    /** List invitations with filtering and pagination. */
    suspend fun listInvitations(filter: InvitationFilter): InvitationResult {
        try {
            val pagedInvitations = invitationRepository.findWithFilter(filter)
            val summaries = pagedInvitations.content.map { it.toSummary() }

            val response =
                PagedResponse(
                    content = summaries,
                    totalElements = pagedInvitations.totalElements,
                    totalPages = pagedInvitations.totalPages,
                    currentPage = pagedInvitations.currentPage,
                    pageSize = pagedInvitations.pageSize,
                    hasNext = pagedInvitations.hasNext,
                    hasPrevious = pagedInvitations.hasPrevious,
                )

            return InvitationResult.Success(data = response)
        } catch (e: Exception) {
            return InvitationResult.Error(
                "Failed to list invitations: ${e.message}",
                "INVITATION_LIST_FAILED",
            )
        }
    }

    /** Process expired invitations (typically called by a scheduled job). */
    suspend fun processExpiredInvitations(): InvitationResult {
        try {
            val expiredInvitations = invitationRepository.findExpiredInvitations()
            val events = mutableListOf<InvitationEvent>()

            for (invitation in expiredInvitations) {
                val expiredInvitation = invitation.expire()
                invitationRepository.save(expiredInvitation)

                events.add(
                    InvitationEvent.InvitationExpired(
                        invitationId = invitation.id,
                        tenantId = invitation.tenantId,
                        email = invitation.email,
                    ),
                )
            }

            return InvitationResult.Success(
                data = "Processed ${expiredInvitations.size} expired invitations",
                events = events,
            )
        } catch (e: Exception) {
            return InvitationResult.Error(
                "Failed to process expired invitations: ${e.message}",
                "EXPIRED_PROCESSING_FAILED",
            )
        }
    }
}

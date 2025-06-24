package com.axians.eaf.controlplane.domain.port

import com.axians.eaf.controlplane.application.dto.invitation.InvitationFilter
import com.axians.eaf.controlplane.application.dto.invitation.PagedResponse
import com.axians.eaf.controlplane.domain.model.invitation.Invitation
import com.axians.eaf.controlplane.domain.model.invitation.InvitationId
import com.axians.eaf.controlplane.domain.model.invitation.InvitationStatus
import com.axians.eaf.controlplane.domain.model.invitation.SecureToken
import com.axians.eaf.controlplane.domain.model.tenant.TenantId

/**
 * Repository port for invitation aggregate persistence. This is a domain contract that will be
 * implemented by infrastructure adapters.
 */
interface InvitationRepository {
    /** Saves an invitation to the repository. For new invitations, this will create them. */
    suspend fun save(invitation: Invitation): Invitation

    /** Finds an invitation by its unique identifier. */
    suspend fun findById(invitationId: InvitationId): Invitation?

    /** Finds an invitation by its secure token. */
    suspend fun findByToken(token: SecureToken): Invitation?

    /** Finds invitations by email address. */
    suspend fun findByEmail(email: String): List<Invitation>

    /** Finds all invitations for a specific tenant. */
    suspend fun findByTenant(tenantId: TenantId): List<Invitation>

    /** Finds invitations with the specified status. */
    suspend fun findByStatus(status: InvitationStatus): List<Invitation>

    /** Finds all expired invitations that need to be marked as expired. */
    suspend fun findExpiredInvitations(): List<Invitation>

    /** Finds invitations matching the specified filter with pagination. */
    suspend fun findWithFilter(filter: InvitationFilter): PagedResponse<Invitation>

    /** Checks if an invitation exists with the given ID. */
    suspend fun existsById(invitationId: InvitationId): Boolean

    /** Checks if an active invitation exists for the given email address within a tenant. */
    suspend fun existsActiveInvitationForEmail(
        email: String,
        tenantId: TenantId,
    ): Boolean

    /** Deletes an invitation by ID. Returns true if the invitation was found and deleted. */
    suspend fun deleteById(invitationId: InvitationId): Boolean

    /** Deletes all invitations for a specific tenant (used when tenant is archived). */
    suspend fun deleteByTenant(tenantId: TenantId): Int
}

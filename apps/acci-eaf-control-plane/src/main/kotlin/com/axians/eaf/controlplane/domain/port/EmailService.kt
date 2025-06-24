package com.axians.eaf.controlplane.domain.port

import com.axians.eaf.controlplane.domain.model.invitation.Invitation

/**
 * Service port for email operations. This is a domain contract that will be implemented by
 * infrastructure adapters for email service integration.
 */
interface EmailService {
    /** Email result indicating success or failure. */
    data class EmailResult(
        val success: Boolean,
        val messageId: String? = null,
        val error: String? = null,
    )

    /** Send an invitation email to a new user. */
    suspend fun sendInvitationEmail(
        invitation: Invitation,
        invitationUrl: String,
    ): EmailResult

    /** Send a resent invitation email. */
    suspend fun sendInvitationReminderEmail(
        invitation: Invitation,
        invitationUrl: String,
    ): EmailResult

    /** Send notification that an invitation has been cancelled. */
    suspend fun sendInvitationCancelledEmail(
        email: String,
        firstName: String,
        lastName: String,
        reason: String,
    ): EmailResult

    /** Send welcome email after invitation acceptance. */
    suspend fun sendWelcomeEmail(
        email: String,
        firstName: String,
        lastName: String,
        tenantName: String,
    ): EmailResult

    /** Send password reset email. */
    suspend fun sendPasswordResetEmail(
        email: String,
        firstName: String,
        lastName: String,
        resetUrl: String,
    ): EmailResult

    /** Send administrative action notification email. */
    suspend fun sendAdminNotificationEmail(
        adminEmail: String,
        subject: String,
        content: String,
        actionDetails: Map<String, Any> = emptyMap(),
    ): EmailResult
}

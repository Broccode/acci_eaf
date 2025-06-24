package com.axians.eaf.controlplane.application.dto.invitation

import com.axians.eaf.controlplane.domain.model.invitation.Invitation
import com.axians.eaf.controlplane.domain.model.invitation.InvitationStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.Instant

/** Request to invite a new user. */
data class InviteUserRequest(
    @field:Email(message = "Valid email address is required")
    @field:NotBlank(message = "Email is required")
    val email: String,
    @field:NotBlank(message = "First name is required")
    @field:Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    val firstName: String,
    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    val lastName: String,
    @field:NotEmpty(message = "At least one role must be assigned") val roles: Set<String>,
    @field:Size(max = 500, message = "Custom message cannot exceed 500 characters")
    val customMessage: String? = null,
    @field:Min(value = 1, message = "Invitation must be valid for at least 1 day")
    @field:Max(value = 30, message = "Invitation cannot be valid for more than 30 days")
    val expiresInDays: Int = 7,
)

/** Response for successful invitation creation. */
data class InvitationResponse(
    val invitationId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val roles: Set<String>,
    val status: InvitationStatus,
    val expiresAt: Instant,
    val createdAt: Instant,
    val invitedBy: String,
    val message: String,
)

/** Request to accept an invitation. */
data class AcceptInvitationRequest(
    @field:NotBlank(message = "Token is required") val token: String,
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    val password: String,
    @field:NotBlank(message = "Password confirmation is required") val confirmPassword: String,
) {
    init {
        require(password == confirmPassword) { "Password and confirmation must match" }
    }
}

/** Response for successful invitation acceptance. */
data class AcceptInvitationResponse(
    val userId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val tenantId: String,
    val roles: Set<String>,
    val message: String,
)

/** Response for invitation cancellation. */
data class CancelInvitationResponse(
    val invitationId: String,
    val email: String,
    val status: InvitationStatus,
    val cancelledAt: Instant,
    val message: String,
)

/** Summary information about an invitation for listing purposes. */
data class InvitationSummary(
    val invitationId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val roles: Set<String>,
    val status: InvitationStatus,
    val expiresAt: Instant,
    val createdAt: Instant,
    val acceptedAt: Instant?,
    val invitedBy: String,
    val isExpired: Boolean,
)

/** Filter criteria for listing invitations. */
data class InvitationFilter(
    val email: String? = null,
    val status: InvitationStatus? = null,
    val invitedBy: String? = null,
    val includeExpired: Boolean = false,
    val fromDate: Instant? = null,
    val toDate: Instant? = null,
    val pageSize: Int = 20,
    val pageNumber: Int = 0,
) {
    init {
        require(pageSize in 1..100) { "Page size must be between 1 and 100" }
        require(pageNumber >= 0) { "Page number must be non-negative" }
    }
}

/** Generic paged response for lists. */
data class PagedResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
)

/** Extension functions to convert between domain and DTO objects. */
fun Invitation.toSummary(): InvitationSummary =
    InvitationSummary(
        invitationId = id.value,
        email = email,
        firstName = firstName,
        lastName = lastName,
        roles = roles.map { it.value }.toSet(),
        status = status,
        expiresAt = expiresAt,
        createdAt = createdAt,
        acceptedAt = acceptedAt,
        invitedBy = invitedBy.value,
        isExpired = isExpired(),
    )

fun Invitation.toResponse(message: String): InvitationResponse =
    InvitationResponse(
        invitationId = id.value,
        email = email,
        firstName = firstName,
        lastName = lastName,
        roles = roles.map { it.value }.toSet(),
        status = status,
        expiresAt = expiresAt,
        createdAt = createdAt,
        invitedBy = invitedBy.value,
        message = message,
    )

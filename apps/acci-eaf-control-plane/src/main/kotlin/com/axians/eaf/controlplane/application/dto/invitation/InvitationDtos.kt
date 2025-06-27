package com.axians.eaf.controlplane.application.dto.invitation

import com.axians.eaf.controlplane.domain.model.invitation.Invitation
import com.axians.eaf.controlplane.domain.model.invitation.InvitationStatus
import com.fasterxml.jackson.annotation.JsonProperty
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
    @field:JsonProperty("email")
    val email: String,
    @field:NotBlank(message = "First name is required")
    @field:Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    @field:JsonProperty("firstName")
    val firstName: String,
    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @field:JsonProperty("lastName")
    val lastName: String,
    @field:NotEmpty(message = "At least one role must be assigned")
    @field:JsonProperty("roles")
    val roles: List<String>,
    @field:Size(max = 500, message = "Custom message cannot exceed 500 characters")
    @field:JsonProperty("customMessage")
    val customMessage: String? = null,
    @field:Min(value = 1, message = "Invitation must be valid for at least 1 day")
    @field:Max(value = 30, message = "Invitation cannot be valid for more than 30 days")
    @field:JsonProperty("expiresInDays")
    val expiresInDays: Int = 7,
) {
    init {
        require(roles.toSet().size == roles.size) { "Duplicate roles are not allowed" }
        require(roles.all { it.isNotBlank() }) { "Role names cannot be blank" }
    }

    /** Get unique roles as a Set for domain layer processing. */
    fun getUniqueRoles(): Set<String> = roles.toSet()
}

/** Response for successful invitation creation. */
data class InvitationResponse(
    @field:JsonProperty("invitationId") val invitationId: String,
    @field:JsonProperty("email") val email: String,
    @field:JsonProperty("firstName") val firstName: String,
    @field:JsonProperty("lastName") val lastName: String,
    @field:JsonProperty("roles") val roles: List<String>,
    @field:JsonProperty("status") val status: InvitationStatus,
    @field:JsonProperty("expiresAt") val expiresAt: Instant,
    @field:JsonProperty("createdAt") val createdAt: Instant,
    @field:JsonProperty("invitedBy") val invitedBy: String,
    @field:JsonProperty("message") val message: String,
)

/** Request to accept an invitation. */
data class AcceptInvitationRequest(
    @field:NotBlank(message = "Token is required") @field:JsonProperty("token") val token: String,
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @field:JsonProperty("password")
    val password: String,
    @field:NotBlank(message = "Password confirmation is required")
    @field:JsonProperty("confirmPassword")
    val confirmPassword: String,
) {
    init {
        require(password == confirmPassword) { "Password and confirmation must match" }
    }
}

/** Response for successful invitation acceptance. */
data class AcceptInvitationResponse(
    @field:JsonProperty("userId") val userId: String,
    @field:JsonProperty("email") val email: String,
    @field:JsonProperty("firstName") val firstName: String,
    @field:JsonProperty("lastName") val lastName: String,
    @field:JsonProperty("tenantId") val tenantId: String,
    @field:JsonProperty("roles") val roles: List<String>,
    @field:JsonProperty("message") val message: String,
)

/** Response for invitation cancellation. */
data class CancelInvitationResponse(
    @field:JsonProperty("invitationId") val invitationId: String,
    @field:JsonProperty("email") val email: String,
    @field:JsonProperty("status") val status: InvitationStatus,
    @field:JsonProperty("cancelledAt") val cancelledAt: Instant,
    @field:JsonProperty("message") val message: String,
)

/** Summary information about an invitation for listing purposes. */
data class InvitationSummary(
    @field:JsonProperty("invitationId") val invitationId: String,
    @field:JsonProperty("email") val email: String,
    @field:JsonProperty("firstName") val firstName: String,
    @field:JsonProperty("lastName") val lastName: String,
    @field:JsonProperty("roles") val roles: List<String>,
    @field:JsonProperty("status") val status: InvitationStatus,
    @field:JsonProperty("expiresAt") val expiresAt: Instant,
    @field:JsonProperty("createdAt") val createdAt: Instant,
    @field:JsonProperty("acceptedAt") val acceptedAt: Instant?,
    @field:JsonProperty("invitedBy") val invitedBy: String,
    @field:JsonProperty("isExpired") val isExpired: Boolean,
)

/** Filter criteria for listing invitations. */
data class InvitationFilter(
    @field:JsonProperty("email") val email: String? = null,
    @field:JsonProperty("status") val status: InvitationStatus? = null,
    @field:JsonProperty("invitedBy") val invitedBy: String? = null,
    @field:JsonProperty("includeExpired") val includeExpired: Boolean = false,
    @field:JsonProperty("fromDate") val fromDate: Instant? = null,
    @field:JsonProperty("toDate") val toDate: Instant? = null,
    @field:JsonProperty("pageSize") val pageSize: Int = 20,
    @field:JsonProperty("pageNumber") val pageNumber: Int = 0,
) {
    init {
        require(pageSize in 1..100) { "Page size must be between 1 and 100" }
        require(pageNumber >= 0) { "Page number must be non-negative" }
    }
}

/** Generic paged response for lists. */
data class PagedResponse<T>(
    @field:JsonProperty("content") val content: List<T>,
    @field:JsonProperty("totalElements") val totalElements: Long,
    @field:JsonProperty("totalPages") val totalPages: Int,
    @field:JsonProperty("currentPage") val currentPage: Int,
    @field:JsonProperty("pageSize") val pageSize: Int,
    @field:JsonProperty("hasNext") val hasNext: Boolean,
    @field:JsonProperty("hasPrevious") val hasPrevious: Boolean,
)

/** Extension functions to convert between domain and DTO objects. */
fun Invitation.toSummary(): InvitationSummary =
    InvitationSummary(
        invitationId = id.value,
        email = email,
        firstName = firstName,
        lastName = lastName,
        roles = roles.map { it.value }.toList(),
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
        roles = roles.map { it.value }.toList(),
        status = status,
        expiresAt = expiresAt,
        createdAt = createdAt,
        invitedBy = invitedBy.value,
        message = message,
    )

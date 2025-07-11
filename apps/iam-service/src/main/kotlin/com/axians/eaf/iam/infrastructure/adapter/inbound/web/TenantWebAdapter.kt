package com.axians.eaf.iam.infrastructure.adapter.inbound.web

import com.axians.eaf.iam.application.port.inbound.CreateTenantCommand
import com.axians.eaf.iam.application.port.inbound.CreateTenantUseCase
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Web adapter for tenant management operations. Provides REST endpoints for creating and managing
 * tenants. This is an inbound adapter that translates HTTP requests to use case calls.
 */
@RestController
@RequestMapping("/api/v1/tenants")
class TenantWebAdapter(
    private val createTenantUseCase: CreateTenantUseCase,
) {
    @PostMapping
    @PreAuthorize("hasRole('ROLE_SUPERADMIN')")
    fun createTenant(
        @Valid @RequestBody request: CreateTenantRequest,
    ): ResponseEntity<CreateTenantResponse> {
        val command = CreateTenantCommand(request.tenantName, request.initialAdminEmail)
        val result = createTenantUseCase.handle(command)

        val response =
            CreateTenantResponse(
                tenantId = result.tenantId,
                tenantName = result.tenantName,
                tenantAdminUserId = result.tenantAdminUserId,
                tenantAdminEmail = result.tenantAdminEmail,
                invitationDetails = result.invitationDetails,
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}

data class CreateTenantRequest(
    @field:NotBlank(message = "Tenant name cannot be blank") val tenantName: String,
    @field:Email(message = "Initial admin email must be valid")
    @field:NotBlank(message = "Initial admin email cannot be blank")
    val initialAdminEmail: String,
)

data class CreateTenantResponse(
    val tenantId: String,
    val tenantName: String,
    val tenantAdminUserId: String,
    val tenantAdminEmail: String,
    val invitationDetails: String,
)

package com.axians.eaf.iam.application.port.inbound

import com.axians.eaf.core.hexagonal.port.InboundPort

/**
 * Inbound port interface for the Create Tenant use case.
 * This defines what the application can do regarding tenant creation.
 */
interface CreateTenantUseCase : InboundPort<CreateTenantCommand, CreateTenantResult>

/**
 * Command object for creating a new tenant.
 */
data class CreateTenantCommand(
    val tenantName: String,
    val initialAdminEmail: String,
)

/**
 * Result object for tenant creation.
 */
data class CreateTenantResult(
    val tenantId: String,
    val tenantName: String,
    val tenantAdminUserId: String,
    val tenantAdminEmail: String,
    val invitationDetails: String, // Placeholder for email invitation details
)

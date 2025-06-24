package com.axians.eaf.controlplane.domain.model.tenant

import java.time.Instant

/**
 * Domain events for tenant lifecycle operations. These events are published when tenant state
 * changes occur.
 */
sealed class TenantEvent {
    abstract val tenantId: TenantId
    abstract val timestamp: Instant
}

/** Published when a new tenant is created. */
data class TenantCreatedEvent(
    override val tenantId: TenantId,
    val tenantName: String,
    val adminEmail: String,
    val settings: TenantSettings,
    override val timestamp: Instant = Instant.now(),
) : TenantEvent()

/** Published when tenant details are updated. */
data class TenantUpdatedEvent(
    override val tenantId: TenantId,
    val oldName: String,
    val newName: String,
    val oldSettings: TenantSettings,
    val newSettings: TenantSettings,
    override val timestamp: Instant = Instant.now(),
) : TenantEvent()

/** Published when a tenant is suspended. */
data class TenantSuspendedEvent(
    override val tenantId: TenantId,
    val tenantName: String,
    val reason: String? = null,
    override val timestamp: Instant = Instant.now(),
) : TenantEvent()

/** Published when a suspended tenant is reactivated. */
data class TenantReactivatedEvent(
    override val tenantId: TenantId,
    val tenantName: String,
    override val timestamp: Instant = Instant.now(),
) : TenantEvent()

/** Published when a tenant is archived. */
data class TenantArchivedEvent(
    override val tenantId: TenantId,
    val tenantName: String,
    val reason: String? = null,
    override val timestamp: Instant = Instant.now(),
) : TenantEvent()

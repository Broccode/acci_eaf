package com.axians.eaf.core.tenancy.integration

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "eaf.tenancy.integration")
@Validated
data class TenantContextIntegrationProperties(
    val enabled: Boolean = true,
    val filterOrder: Int = TenantContextSynchronizationFilter.FILTER_ORDER,
    val urlPatterns: List<String> = emptyList(),
    val respectExistingContext: Boolean = true,
    val enableHeaderFallback: Boolean = true,
    @field:NotEmpty(
        message = "Tenant headers list cannot be empty when header fallback is enabled",
    )
    val tenantHeaders: List<String> = listOf("X-Tenant-ID", "Tenant-ID"),
    val addResponseHeaders: Boolean = true,
    @field:Min(value = 8, message = "Maximum tenant ID length must be at least 8 characters")
    @field:Max(value = 256, message = "Maximum tenant ID length cannot exceed 256 characters")
    val maxTenantIdLength: Int = 64,
    val enableDetailedLogging: Boolean = false,
)

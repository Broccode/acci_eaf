package com.axians.eaf.controlplane.application.dto.tenant

import com.axians.eaf.controlplane.domain.model.tenant.Tenant
import com.axians.eaf.controlplane.domain.model.tenant.TenantSettings
import com.axians.eaf.controlplane.domain.model.tenant.TenantStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.Instant

/** Request to create a new tenant. */
data class CreateTenantRequest(
    @field:NotBlank(message = "Tenant name cannot be blank")
    @field:Size(max = 100, message = "Tenant name cannot exceed 100 characters")
    val name: String,
    @field:NotBlank(message = "Admin email cannot be blank")
    @field:Email(message = "Admin email must be valid")
    val adminEmail: String,
    @field:Valid val settings: TenantSettingsDto,
    val notifyAdmin: Boolean = true,
)

/** Response after successful tenant creation. */
data class CreateTenantResponse(
    val tenantId: String,
    val name: String,
    val adminUserId: String?,
    val message: String,
) {
    companion object {
        fun success(
            tenantId: String,
            name: String,
            adminUserId: String? = null,
        ): CreateTenantResponse =
            CreateTenantResponse(
                tenantId = tenantId,
                name = name,
                adminUserId = adminUserId,
                message = "Tenant created successfully",
            )

        fun failure(message: String): CreateTenantResponse =
            CreateTenantResponse(
                tenantId = "",
                name = "",
                adminUserId = null,
                message = message,
            )
    }
}

/** Request to update an existing tenant. */
data class UpdateTenantRequest(
    @field:NotBlank(message = "Tenant name cannot be blank")
    @field:Size(max = 100, message = "Tenant name cannot exceed 100 characters")
    val name: String,
    @field:Valid val settings: TenantSettingsDto,
)

/** DTO for tenant settings. */
data class TenantSettingsDto(
    @field:Min(value = 1, message = "Max users must be at least 1")
    @field:Max(value = 10000, message = "Max users cannot exceed 10000")
    val maxUsers: Int,
    @field:NotEmpty(message = "At least one allowed domain must be specified")
    val allowedDomains: List<@NotBlank String>,
    val features: List<String> = emptyList(),
    val customSettings: Map<String, Any> = emptyMap(),
) {
    init {
        require(features.toSet().size == features.size) { "Duplicate features are not allowed" }
        require(features.all { it.isNotBlank() }) { "Feature names cannot be blank" }
        require(allowedDomains.toSet().size == allowedDomains.size) {
            "Duplicate domains are not allowed"
        }
    }

    /** Get unique features as a Set for domain layer processing. */
    fun getUniqueFeatures(): Set<String> = features.toSet()

    /** Get unique allowed domains as a Set for domain layer processing. */
    fun getUniqueAllowedDomains(): Set<String> = allowedDomains.toSet()

    /** Converts DTO to domain model with validation. */
    fun toDomain(): TenantSettings =
        TenantSettings(
            maxUsers = maxUsers,
            allowedDomains = allowedDomains,
            features = features.toSet(),
            customSettings = customSettings,
        )

    companion object {
        /** Creates DTO from domain model. */
        fun fromDomain(settings: TenantSettings): TenantSettingsDto =
            TenantSettingsDto(
                maxUsers = settings.maxUsers,
                allowedDomains = settings.allowedDomains,
                features = settings.features.toList(),
                customSettings = settings.customSettings,
            )
    }
}

/** Detailed tenant information response. */
data class TenantDetailsResponse(
    val tenant: TenantDto,
    val userCount: Int,
    val activeUsers: Int,
    val lastActivity: Instant?,
)

/** Basic tenant information DTO. */
data class TenantDto(
    val id: String,
    val name: String,
    val status: TenantStatus,
    val settings: TenantSettingsDto,
    val createdAt: Instant,
    val lastModified: Instant,
    val archivedAt: Instant? = null,
) {
    companion object {
        /** Creates DTO from domain model. */
        fun fromDomain(tenant: Tenant): TenantDto =
            TenantDto(
                id = tenant.id.value,
                name = tenant.name,
                status = tenant.status,
                settings = TenantSettingsDto.fromDomain(tenant.settings),
                createdAt = tenant.createdAt,
                lastModified = tenant.lastModified,
                archivedAt = tenant.archivedAt,
            )
    }
}

/** Summary tenant information for lists. */
data class TenantSummary(
    val id: String,
    val name: String,
    val status: TenantStatus,
    val userCount: Int,
    val createdAt: Instant,
    val lastActivity: Instant?,
)

/** Response after tenant archival. */
data class ArchiveTenantResponse(
    val tenantId: String,
    val message: String,
    val archivedAt: Instant,
) {
    companion object {
        fun success(
            tenantId: String,
            archivedAt: Instant,
        ): ArchiveTenantResponse =
            ArchiveTenantResponse(
                tenantId = tenantId,
                message = "Tenant archived successfully",
                archivedAt = archivedAt,
            )
    }
}

/** Filter criteria for tenant queries. */
data class TenantFilter(
    val status: TenantStatus? = null,
    val namePattern: String? = null,
    val domainPattern: String? = null,
    val createdAfter: Instant? = null,
    val createdBefore: Instant? = null,
    val page: Int = 0,
    val size: Int = 20,
)

/** Paged response for tenant lists. */
data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun <T> of(
            content: List<T>,
            page: Int,
            size: Int,
            totalElements: Long,
        ): PagedResponse<T> {
            val totalPages = if (size > 0) ((totalElements + size - 1) / size).toInt() else 0
            return PagedResponse(content, page, size, totalElements, totalPages)
        }
    }
}

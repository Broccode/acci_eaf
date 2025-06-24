package com.axians.eaf.controlplane.infrastructure.adapter.input

import java.time.Instant

/**
 * Shared response models for Control Plane endpoints. Provides type-safe communication between
 * frontend and backend.
 */
data class ResponseMetadata(
    val timestamp: Instant,
    val requestId: String,
    val version: String,
)

// Common exception for validation errors
class ValidationException(
    message: String,
) : Exception(message)

// User Info Models

data class UserInfoResponse(
    val success: Boolean,
    val user: UserInfo? = null,
    val permissions: UserPermissions? = null,
    val metadata: ResponseMetadata,
    val error: String? = null,
)

data class UserInfo(
    val userId: String,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val roles: List<String>,
    val tenantId: String,
    val tenantName: String,
    val lastLoginAt: Instant,
    val accountStatus: String,
)

data class UserPermissions(
    val canCreateTenants: Boolean,
    val canManageUsers: Boolean,
    val canViewAuditLogs: Boolean,
    val canModifyConfiguration: Boolean,
)

data class UserPreferencesResponse(
    val success: Boolean,
    val preferences: UserPreferences? = null,
    val metadata: ResponseMetadata,
    val error: String? = null,
)

data class UserPreferences(
    val theme: String,
    val language: String,
    val timezone: String,
    val dateFormat: String,
    val itemsPerPage: Int,
    val enableNotifications: Boolean,
    val dashboardLayout: Map<String, Any>,
)

data class UpdatePreferencesRequest(
    val theme: String? = null,
    val language: String? = null,
    val timezone: String? = null,
    val dateFormat: String? = null,
    val itemsPerPage: Int? = null,
    val enableNotifications: Boolean? = null,
    val dashboardLayout: Map<String, Any>? = null,
)

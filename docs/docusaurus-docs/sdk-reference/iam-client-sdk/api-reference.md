---
sidebar_position: 3
title: API Reference
---

# IAM Client SDK API Reference

Complete API documentation for the EAF IAM Client SDK.

## 🔑 IAMClient

Main interface for IAM operations.

### Methods

#### authenticate(token: String): Authentication

Validates a JWT token and returns authentication information.

#### getServiceToken(): String

Retrieves a service-to-service authentication token.

#### refreshToken(refreshToken: String): TokenResponse

Refreshes an expired access token.

## 🛡️ AuthenticationService

Service for handling authentication operations.

### Methods

#### authenticate(request: AuthenticationRequest): Authentication

Authenticates a user with credentials or token.

#### isAuthenticated(): Boolean

Checks if the current request is authenticated.

## 🔐 AuthorizationService

Service for handling authorization checks.

### Methods

#### hasPermission(permission: String): Boolean

Checks if the current user has a specific permission.

#### hasRole(role: String): Boolean

Checks if the current user has a specific role.

#### requirePermission(permission: String)

Throws exception if permission is not granted.

## 🏢 TenantContext

Service for managing tenant context.

### Methods

#### getCurrentTenant(): TenantId

Gets the current tenant ID from context.

#### setTenant(tenantId: TenantId)

Sets the tenant context for the current request.

## 📋 Data Classes

### Authentication

Contains user authentication information including user ID, roles, and permissions.

### TokenResponse

Response containing access token, refresh token, and expiration information.

### TenantId

Value object representing a tenant identifier.

---

_Complete API reference for the EAF IAM Client SDK._

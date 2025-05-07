# Multi-Tenancy in ACCI EAF

Version: 1.0
Date: 2025-05-07
Status: Published

## Introduction

Multi-tenancy is a core architectural principle of the ACCI EAF, enabling a single instance of an application to serve multiple tenants (e.g., different customers or organizational units) while keeping their data isolated and secure. This document outlines the approach to multi-tenancy within the framework, primarily focusing on Row-Level Security (RLS).

This concept is crucial for SaaS applications and any system requiring logical data separation between different user groups.

## Core Concepts

### 1. Tenant Definition

A **Tenant** represents an isolated group of users and their associated data. Each tenant operates as if they have their own dedicated application instance, even though they share underlying resources.

### 2. Data Isolation Strategy: Row-Level Security (RLS)

ACCI EAF primarily uses RLS to enforce data isolation at the database level.

- **`tenant_id` Column:** A mandatory `tenant_id` column is added to all database tables that store tenant-specific data. This column links each row to a specific tenant.
- **Automated Filtering:** Database queries are automatically filtered based on the current tenant's ID, ensuring that a tenant can only access their own data.

### 3. Tenant Context Propagation

To apply RLS effectively, the application needs to be aware of the current tenant context for every incoming request.

- **Tenant Identification:** The `tenant_id` is typically extracted from an incoming HTTP request. Common sources include:
  - A claim within a JWT (JSON Web Token).
  - A custom HTTP header (e.g., `X-Tenant-ID`).
  - Part of the hostname or URL path (less common in EAF's primary strategy but possible).
- **`AsyncLocalStorage`:** Once identified, the `tenant_id` is stored in an `AsyncLocalStorage` instance. This makes the `tenant_id` available throughout the asynchronous execution flow of a single request, without needing to pass it explicitly through all function calls.
  - A NestJS middleware is responsible for extracting the `tenant_id` and initializing the `AsyncLocalStorage` for the request scope.

### 4. Enforcement with MikroORM Global Filters (ADR-006)

MikroORM, the Object-Relational Mapper used in ACCI EAF, plays a vital role in enforcing RLS:

- **Global Filters:** MikroORM's global filters feature is used to define a tenant filter that is applied to all relevant entities.
- **Dynamic Parameterization:** The filter is configured to dynamically receive the `tenant_id` from the `AsyncLocalStorage`.
- **Automatic Query Modification:** When a query is made for an entity that has the tenant filter enabled, MikroORM automatically appends the necessary SQL `WHERE` clause (e.g., `WHERE tenant_id = :currentTenantId`) before the query is sent to the database.

This approach centralizes the RLS logic and makes it transparent to the application services and repositories, reducing the risk of accidental data exposure.

## Implementation Details (`libs/tenancy`)

The `libs/tenancy` library encapsulates the core multi-tenancy logic:

- **Tenant Context Middleware:** A NestJS middleware to extract `tenant_id` and populate `AsyncLocalStorage`.
- **Tenant Context Service:** Provides access to the current `tenant_id` from `AsyncLocalStorage`.
- **MikroORM Filter Configuration:** Setup and registration of the global tenant filter for MikroORM.

## Benefits

- **Strong Data Isolation:** Prevents tenants from accessing each other's data.
- **Simplified Development:** Developers don't need to manually add `tenant_id` conditions to every query.
- **Scalability:** A single application instance can serve many tenants, optimizing resource utilization.
- **Maintainability:** Centralized tenancy logic is easier to manage and update.

## Considerations

- **Shared Data:** For data that is genuinely shared across all tenants (e.g., system-wide configurations), the `tenant_id` column would not be applicable, or a special NULL/sentinel value might be used, and entities would be configured to bypass the RLS filter.
- **Performance:** While RLS is generally efficient, complex queries on very large multi-tenant tables might require careful indexing on the `tenant_id` column and other frequently queried columns.
- **Cross-Tenant Operations:** Operations that legitimately need to access data from multiple tenants (e.g., by a super-administrator) require special handling, potentially by bypassing RLS filters under strict, audited conditions.

## Related ADRs

- **ADR-006: RLS Enforcement Strategy:** Details the decision to use MikroORM Global Filters for RLS.

## Future Considerations

- Tenant-specific configurations beyond data isolation (e.g., feature flags per tenant).
- Strategies for sharding tenants across multiple databases if a single database becomes a bottleneck (though RLS within a single DB is the primary V1 strategy).

This document provides a foundational understanding of multi-tenancy in ACCI EAF. For specific implementation patterns and usage, refer to the `libs/tenancy` codebase and related ADRs.

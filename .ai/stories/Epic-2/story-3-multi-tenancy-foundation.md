# Story 3: Multi-Tenancy Foundation

**Epic:** [Epic-2: Multi-Tenancy & Control Plane](../epic-2.md)
**Status:** In Progress
**PRD Link:** [PRD Section](../../prd.md#story-3-multi-tenancy-foundation)

## Goal

Implement the core mechanisms for multi-tenancy:

1. Reliable extraction and propagation of the `tenant_id` throughout the request lifecycle.
2. Automatic enforcement of Row-Level Security (RLS) using the `tenant_id` for database queries via MikroORM.

## Implementation Details

- **Tenant Context Propagation (`libs/tenancy`):**
  - Created `TenantContextService` using `AsyncLocalStorage` to store and retrieve the current `tenantId`.
  - Created `TenantContextMiddleware` to extract `tenantId` from JWT claim (`tenant_id`) or `X-Tenant-ID` header (fallback) and run the request within the tenant's context using `TenantContextService`.
  - Created `TenancyModule` to provide and export the service globally.
- **RLS Enforcement (`libs/infrastructure`, `mikro-orm.config.ts`):**
  - Created `TenantFilter` (MikroORM global filter) in `libs/infrastructure/src/lib/persistence/filters/` that uses `TenantContextService` to get the current `tenantId` and apply it as a `WHERE tenantId = ?` condition.
  - Created `TenantFilterInitializer` in the same file and registered it in `InfrastructureModule` to ensure the static service instance used by the filter is initialized.
  - Created `mikro-orm.config.ts` in the project root, defining basic PostgreSQL connection, entity discovery paths (to be refined), migration paths, and **registering the `TenantFilter`**.
  - Added a note in the config that `registerRequestContext: true` must be set when using `MikroOrmModule.forRoot`.
- **Shared Tenant Interface (`libs/shared`):**
  - Created `TenantAware` interface (`libs/shared/src/lib/interfaces/tenant-aware.interface.ts`) defining the `tenantId: string;` property contract.
- **Base Tenant Entity (`libs/core`):**
  - Created abstract `BaseTenantEntity` (`libs/core/src/lib/domain/base-tenant.entity.ts`) implementing `TenantAware` and providing a default UUID `id` and the indexed `tenantId` property.

## Acceptance Criteria

- [x] `TenantContextService` correctly stores and retrieves `tenantId` via `AsyncLocalStorage`.
- [x] `TenantContextMiddleware` extracts `tenantId` from JWT claim or header.
- [x] `TenantContextMiddleware` uses `TenantContextService` to set the context.
- [x] `TenantFilter` is defined and uses `TenantContextService`.
- [x] `TenantFilter` applies the correct `{ tenantId: currentTenantId }` condition.
- [x] `TenantFilter` blocks access ({ tenantId: '**NO_TENANT_ACCESS**' }) if no `tenantId` is in context.
- [x] `TenantFilter` can be disabled via arguments (`args['disabled'] === true`).
- [x] `TenantAware` interface exists.
- [x] `BaseTenantEntity` exists, implements `TenantAware`, and has `id` and `tenantId`.
- [x] Basic `mikro-orm.config.ts` exists and registers `TenantFilter`.
- [ ] `TenantFilterInitializer` is correctly instantiated before the filter is used (verified by module registration).
- [ ] MikroORM configuration is loaded in a NestJS module with `registerRequestContext: true`.
- [ ] An entity inheriting from `BaseTenantEntity` correctly applies the RLS filter during queries.

## Next Steps / Open Points

- Load MikroORM configuration in a NestJS module (`MikroOrmModule.forRoot`).
- Refine entity discovery paths in `mikro-orm.config.ts`.
- Implement actual entities inheriting from `BaseTenantEntity`.
- Write integration tests to verify RLS enforcement.
- Correct the `any` type for `TenantFilter` in `tenant.filter.ts` once MikroORM setup is finalized.

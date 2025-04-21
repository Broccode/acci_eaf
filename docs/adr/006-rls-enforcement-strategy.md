# ADR-006: RLS Enforcement Strategy

* **Status:** Proposed
* **Date:** 2025-04-21
* **Stakeholders:** [Names or roles]

## Context and Problem Statement

For the ACCI EAF's multi-tenancy, Row-Level Security (RLS) using a `tenant_id` column was decided. A reliable and central method is needed to ensure all database queries via MikroORM are automatically and correctly filtered by the `tenant_id` of the current request context.

## Considered Options

1. **Manual Filtering in Repositories:** Each repository method manually adds `.andWhere({ tenantId: ... })`.
    * *Pros:* Explicit.
    * *Cons:* Extremely error-prone (forgetting the filter), lots of boilerplate.
2. **Base Repository Class:** A base class adds the filter in overridden methods (`find`, `findOne`, etc.).
    * *Pros:* Reduces some boilerplate.
    * *Cons:* Doesn't apply to Query Builder or `EntityManager`; requires discipline to use the base class.
3. **MikroORM Filters (Global):** Utilize MikroORM's built-in filter functionality.
    * *Pros:* Centrally defined, automatically applied by the ORM, less boilerplate in repositories, supports all query methods (EM, Repo, QB).
    * *Cons:* Requires correct parameterization per request.

## Decision

We choose **Option 3: MikroORM Filters**. A global filter `@Filter({ name: 'tenant', ... })` will be defined on tenant-aware entities. Parameterization (`tenantId`) will occur per request via `EntityManager.setFilterParams()` using a NestJS Middleware, which reads the value from `AsyncLocalStorage` (provided by `libs/tenancy`).

## Consequences

* Positive: High security against accidentally forgotten filters; central RLS logic; cleaner repository implementations.
* Negative / Risks: Correct implementation of the middleware and MikroORM's `RequestContext` handling is crucial. Performance impact of global filters must be monitored.
* Implications: `libs/tenancy` provides the context, Middleware in `libs/infrastructure` (or app module) sets the filter parameter.

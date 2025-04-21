# Architecture for ACCI EAF

Status: Draft

## Technical Summary

The ACCI EAF (Axians Competence Center Infrastructure Enterprise Application Framework) is designed as a modular, extensible, and secure backend framework for enterprise applications. It leverages Hexagonal Architecture, CQRS/Event Sourcing, and Multi-Tenancy via Row-Level Security (RLS) to ensure scalability, maintainability, and compliance. The framework is built as a monorepo (Nx recommended) with clear separation between core logic (Domain, Application) and infrastructure (Adapters), and supports extensibility through a plugin system. Security, observability, and testability are first-class citizens, laying the groundwork for standards like ISO 27001/SOC2.

## Technology Table

| Technology      | Description                                                      | Justification / ADR Link | Status      |
| -------------- | ---------------------------------------------------------------- | ------------------------ | ----------- |
| TypeScript     | Main language (Strong typing, modern JS features)               |                          | Decided     |
| Node.js        | Runtime environment (V8 performance, large ecosystem)            |                          | Decided     |
| NestJS         | Backend framework (Modularity, DI, Decorators, TS support)       |                          | Decided     |
| PostgreSQL     | Database for event store and read models (Reliable, ACID, JSONB)   |                          | Decided     |
| MikroORM       | ORM with entity discovery and RLS filters (TS-first, UoW, Filters) | ADR-006 (Filters)        | Decided     |
| Redis          | Caching (Fast in-memory cache)                                  |                          | Decided     |
| Nx             | Monorepo management (Code sharing, tooling)                      |                          | Recommended |
| Jest           | Testing framework (Widely used, good ecosystem)                   |                          | Decided     |
| `suites`       | Unit testing utilities for DI components                         |                          | Decided     |
| Testcontainers | Integration testing with real DB/Cache (Reliable tests)          |                          | Decided     |
| `supertest`    | E2E testing HTTP requests                                       |                          | Decided     |
| `@nestjs/testing`| NestJS testing utilities                                        |                          | Decided     |
| `nestjs-i18n`  | Internationalization library (NestJS integration)               |                          | Decided     |
| `casl`         | RBAC/ABAC library (Flexible, TS support)                         | ADR-001                  | Decided     |
| `helmet`       | Security headers middleware (OWASP recommendation)                |                          | Decided     |
| `@nestjs/throttler` | Rate limiting middleware (Basic DoS protection)                 |                          | Decided     |
| `@nestjs/terminus` | Health checks module (Kubernetes readiness/liveness)          |                          | Decided     |
| OpenAPI        | API documentation standard (via NestJS)                           |                          | Decided     |
| `@cyclonedx/cyclonedx-npm` | SBOM generation tool                                          | ADR-002                  | Decided     |
| CycloneDX (JSON) | SBOM format                                                      | ADR-002                  | Decided     |

## Core Architectural Patterns

### Hexagonal Architecture (Ports & Adapters)

* **Goal:** Isolate core logic (domain, application) from external dependencies (UI, DB, APIs) for testability and maintainability.
* **Layers:**
  * **Core (`libs/core`):** Technology-agnostic business logic.
    * `domain`: Aggregates, Entities, Value Objects, Domain Events, Repository *Interfaces* (Ports). No external framework dependencies.
    * `application`: Use Cases, Command/Query Handlers, Application Services, DTOs, Port *Interfaces* for infrastructure. Depends only on `domain`.
  * **Infrastructure (`libs/infrastructure`, `apps/*`):** Implements Ports using specific technologies (NestJS, MikroORM, Redis). Contains Adapters (Controllers, DB Repositories, Cache Clients). Depends on `core`.
* **Dependency Rule:** Dependencies flow inwards (Infrastructure -> Application -> Domain).

### CQRS/ES (Command Query Responsibility Segregation / Event Sourcing)

* **Goal:** Separate write (Commands) and read (Queries) paths; use events as the single source of truth for state changes, enabling traceability and projections.
* **Command Flow:** HTTP Request -> NestJS Controller -> `CommandBus` -> Command Handler (loads Aggregate via Repository) -> Aggregate (validates, creates Domain Event) -> Event Store Adapter (saves Event) -> `EventBus`.
* **Event Flow:** `EventBus` (or polling the store) -> Event Handler / Projector (updates Read Model).
* **Query Flow:** HTTP Request -> NestJS Controller -> `QueryBus` -> Query Handler -> Read Model Repository Adapter (reads from optimized Read Model DB) -> Response.
* **Components:** Commands, Queries, Events (Domain/Integration), Handlers, Aggregate Roots, Event Store, Read Models, Projectors.
* **Key Considerations:**
  * **Event Schema Evolution (ADR-010):** Use explicit versioning in `event_type` (e.g., `UserRegistered.v1`) and implement upcasting functions to handle older versions during event loading or processing.
  * **Idempotent Handlers (ADR-009):** Ensure event handlers can safely process the same event multiple times. Recommended approach: Use a tracking table (`processed_events`) storing `event_id` (and potentially handler name) to skip already processed events.

### Multi-Tenancy (Row-Level Security)

* **Goal:** Securely isolate data for different tenants within the same database instance.
* **Approach:** Utilize a `tenant_id` column in all tenant-specific tables.
* **Context Propagation:** Extract `tenant_id` from request (e.g., JWT claim, header) via Middleware and make it available throughout the request lifecycle using `AsyncLocalStorage` (`libs/tenancy`).
* **Enforcement (ADR-006):** Employ **MikroORM Global Filters** configured globally. These filters automatically append `WHERE tenant_id = :currentTenantId` to relevant SQL queries, parameterized dynamically using the `tenant_id` from `AsyncLocalStorage`.

### Plugin System

* **Goal:** Allow extending EAF functionality without modifying core libraries.
* **Approach:** Define a plugin interface, a loading mechanism (e.g., during app bootstrap), and registration points (e.g., for contributing modules, controllers, entities).
* **Entity Discovery:** Leverage **MikroORM's** entity discovery via glob patterns (`ormconfig.entities`). Plugins can define their MikroORM entities within their own directories, which are then discovered by the central MikroORM configuration.
* **Migrations (ADR-008):** Plugins provide their own MikroORM migration files within their structure. The main application's migration script/process is responsible for discovering and executing these plugin migrations alongside core migrations.
* **Interaction:** Consider plugin interactions with tenancy, RBAC, and licensing (details TBD, likely through shared services/context).

## Architectural Diagrams

*(Diagrams will be added and maintained in this section or linked from here)*

* **Placeholder:** High-Level Component Overview (Monorepo Structure - C4 Container Diagram)
* **Placeholder:** Request Flow - Command (Sequence Diagram)
* **Placeholder:** Request Flow - Query (Sequence Diagram)
* **Placeholder:** Tenant Context Propagation & RLS Filter (Sequence Diagram)
* **Placeholder:** Event Handling with Idempotency Check (Sequence Diagram)
* **Placeholder:** Core Entity Relationships (UML Class Diagram - e.g., User, Tenant, Role)

## Component Overview (Planned)

* **`libs/`:**
  * `core`: Domain logic (Entities, Aggregates, VOs, Domain Events), Application logic (Use Cases/Services, Command/Query Handlers, Ports/Interfaces).
  * `infrastructure`: Adapters implementing Core Ports (Persistence [MikroORM/PG], Cache [Redis], Web [NestJS Controllers/Modules], i18n Setup, Event Bus implementation, etc.).
  * `tenancy`: Tenant context extraction (Middleware), propagation (AsyncLocalStorage), and MikroORM Filter setup.
  * `rbac`: RBAC/ABAC core logic (e.g., `casl` integration), Data Models (User, Role, Permission Entities), Guards, Decorators.
  * `licensing`: Core logic and service interface for license validation.
  * `plugins`: Plugin interface definition and loading/registration mechanism.
  * `testing`: Shared test utilities (Testcontainers setup, base test classes, MikroORM test helpers).
  * `shared`: Common DTOs, constants, simple utility functions, shared interfaces.
* **`apps/`:**
  * `control-plane-api`: NestJS application for system administrators to manage tenants.
  * `sample-app`: NestJS application demonstrating the EAF usage.

## Key Concepts in Detail

### Data Persistence

* **ORM:** MikroORM configured for PostgreSQL.
* **Event Store:** Implemented as a standard PG table (e.g., `events`) with columns: `stream_id` (Aggregate ID), `version` (Event sequence), `event_type` (string identifier, versioned - ADR-010), `payload` (JSONB), `timestamp`, `tenant_id` (for tenant-specific streams).
* **Read Models:** Separate PG tables, potentially denormalized and optimized for specific query use cases. Must include `tenant_id` if data is tenant-specific. Idempotency tracking table (`processed_events`) as per ADR-009.
* **RLS Enforcement (ADR-006):** Global MikroORM Filters automatically apply `tenant_id` constraints based on the current context (`AsyncLocalStorage`).
* **Entity Discovery:** Glob patterns in MikroORM config (`mikro-orm.config.ts`) discover entities from core libs and potentially registered plugins.
* **Unit of Work (UoW):** MikroORM's UoW pattern manages entity state changes and ensures transactional consistency.
* **Migrations:** Central MikroORM CLI manages migrations, including discovering and running those defined within plugins (ADR-008).

### Authentication & Authorization

* **Authentication:** Flexible NestJS Auth module. V1 includes JWT (bearer token) and Local (username/password) strategies. Strategy selection could potentially be tenant-configurable in the future.
* **Authorization (ADR-001):** RBAC combined with basic ABAC (Ownership) using `casl`. Defines permissions (e.g., `manage`, `read`) on subjects (e.g., `Tenant`, `User`).
* **Guards:** Standardized NestJS Guards (e.g., `@UseGuards(JwtAuthGuard, CaslGuard('manage', 'Tenant'))`) protect endpoints/methods.
* **Tenant Admin:** Provide backend services/API endpoints within the tenant's application scope for managing roles and permissions specific to that tenant.

### Observability

* **Logging (V1):** Implement structured logging (e.g., JSON format) via a dedicated logger (e.g., Pino integrated with NestJS). Ensure logs include context like `tenant_id`, `correlationId`.
* **Health Checks (V1):** Utilize `@nestjs/terminus` for standard health endpoints (`/health/live`, `/health/ready`) checking DB connectivity, cache, etc. Crucial for orchestrators like Kubernetes.
* **Roadmap:** Integration of metrics export (Prometheus) and distributed tracing (OpenTelemetry).

### License Validation (ADR-003)

* **Module:** Dedicated `libs/licensing` module/service.
* **Logic:** Implements hybrid validation (Offline file check mandatory, optional online check). Checks constraints like `tenantId`, `expiresAt`, `maxCpuCores`. Invoked on startup and potentially periodically.
* **Security:** Must be robust against simple bypass and tampering.

### Testing Strategy Overview

* **Unit Tests:** Use Jest + `suites` (or similar DI testing tools). Focus on core domain and application logic. Mock infrastructure dependencies (Ports).
* **Integration Tests:** Use Jest + Testcontainers. Test infrastructure adapters (e.g., MikroORM repositories, Redis cache) against real database/cache instances spun up dynamically. Leverage MikroORM's testing utilities.
* **E2E Tests:** Use Jest + `supertest` + `@nestjs/testing`. Test complete API request flows from HTTP entry point to response. Use a dedicated test database seeded via MikroORM.

### SBOM (ADR-002)

* Integrate SBOM generation using `@cyclonedx/cyclonedx-npm` CLI tool into the CI/CD build process for each deployable application (`apps/*`) and potentially core libraries (`libs/*`).
* Output format: CycloneDX JSON.

## Infrastructure

* PostgreSQL: Database server (self-hosted or managed service).
* Redis: Cache server (self-hosted or managed service).
* Node.js Runtime Environment.
* Deployment Environment: Docker recommended, potentially orchestrated by Kubernetes.

## Deployment Considerations

* Applications (`apps/*`) are packaged as Docker containers.
* Nx (or chosen monorepo tool) builds optimized, self-contained artifacts for each application.
* Configuration (DB connection strings, secrets, etc.) should be managed via environment variables or a configuration service, not hardcoded.
* Health Check endpoints (`/health/live`, `/health/ready`) should be configured in orchestration platform (e.g., Kubernetes liveness/readiness probes).
* Graceful shutdown mechanisms in NestJS should be enabled to handle signals (SIGTERM, SIGINT) correctly, allowing in-flight requests to complete before exit.

## Change Log

| Change        | Story ID | Description                                      | ADRs Impacting |
| ------------- | -------- | ------------------------------------------------ | -------------- |
| Initial draft | N/A      | Initial architecture draft                       |                |
| Update        | N/A      | Incorporate decisions from ADRs 001-010        | 001-010        |

# ACCI EAF - Architecture Overview

* **Version:** 1.0 Draft
* **Date:** 2025-04-20
* **Status:** Approved

## Technical Summary

This document describes the high-level architecture of the **ACCI EAF** (Axians Competence Center Infrastructure Enterprise Application Framework). It explains the chosen architectural patterns, core components, technology stack, and fundamental design decisions. This document serves as a technical guide for developers and architects using or evolving the framework.

Detailed functional and non-functional requirements can be found in the separate Product Requirements Document (PRD). Key design decisions are also documented in Architecture Decision Records (ADRs) in the `docs/adr/` directory.

## 2. Architectural Drivers

The architecture of the ACCI EAF is significantly influenced by the following key requirements from the PRD:

* **Multi-Tenancy:** The need to securely manage and isolate multiple tenants within a single application instance (RLS approach).
* **CQRS/ES:** Separation of read and write paths, and Event Sourcing for traceability and as a basis for projections.
* **Hexagonal Architecture:** Strict separation of core logic (Domain, Application) from infrastructure (frameworks, DB, external services) for testability and maintainability.
* **Security & Compliance:** High security requirements, support for certifications (ISO 27001, SOC2), robust AuthN/AuthZ (RBAC/ABAC), Audit Trail (future), SBOM.
* **Licensing:** Support for the Axians business model through integrated license validation.
* **Extensibility:** Enabling the extension of the framework and applications built upon it through a plugin system.
* **Enterprise Readiness:** Providing features for observability, reliability, i18n, configuration management, etc.

## 3. Overall Structure (Monorepo)

The ACCI EAF is developed and managed as a **Monorepo** to promote code sharing, central dependency management, and consistent development processes. The use of **Nx** as a monorepo management tool is recommended. The typical structure includes:

* **`apps/`**: Contains independently deployable applications.
  * `control-plane-api`: The API for central tenant management.
  * `sample-app`: A sample application utilizing the EAF.
* **`libs/`**: Contains reusable libraries forming the core of the EAF and shared functionality.
  * Framework core libraries (e.g., `core`, `infrastructure`, `tenancy`, `rbac`, `licensing`, `plugins`, `testing`).
  * Application-specific libraries for the `sample-app`.

## 4. Core Architectural Patterns

The ACCI EAF is based on a combination of established architectural patterns:

### 4.1. Hexagonal Architecture (Ports & Adapters)

* **Goal:** Isolate core logic from external influences.
* **Layers:**
  * **Core (`libs/core`):** Contains technology-agnostic business and application logic.
    * `domain`: Aggregates, Entities, Value Objects, Domain Events, Repository *Interfaces* (Ports). Contains no dependencies on external frameworks.
    * `application`: Use Cases, Command/Query Handlers, Application Services, DTOs, Port *Interfaces* for infrastructure. Depends only on `domain`.
  * **Infrastructure (`libs/infrastructure`, `apps/*`):** Implements the ports from the Core using specific technologies (NestJS, MikroORM, Redis, etc.). Contains the Adapters (e.g., REST controllers, DB repositories, cache clients). Depends on the `core`.
* **Dependency Rule:** Dependencies always point inwards (Infrastructure -> Application -> Domain).

### 4.2. CQRS/ES (Command Query Responsibility Segregation / Event Sourcing)

* **Goal:** Separation of write (Commands) and read (Queries) operations; using events as the primary source of truth.
* **Command Flow:** HTTP Request -> NestJS Controller -> `CommandBus` -> Command Handler (loads Aggregate via Repository) -> Aggregate (validates, creates Domain Event) -> Event Store Adapter (saves Event) -> `EventBus`.
* **Event Flow:** `EventBus` (or polling the store) -> Event Handler / Projector (updates Read Model).
* **Query Flow:** HTTP Request -> NestJS Controller -> `QueryBus` -> Query Handler -> Read Model Repository Adapter (reads from optimized Read Model DB) -> Response.
* **Components:** Commands, Queries, Events (Domain/Integration), Command/Query/Event Handlers, Aggregate Roots, Event Store, Read Models (optimized database structures), Projectors.

### 4.3. Multi-Tenancy (Row-Level Security)

* **Goal:** Secure data separation of different tenants within the same database.
* **Approach:** Using a `tenant_id` column in all tenant-aware tables.
* **Context:** The current `tenant_id` is extracted from the request (Token/Header) and made available in the request context using `AsyncLocalStorage` (`libs/tenancy`).
* **Enforcement:** **MikroORM Filters** are used to automatically and globally apply the `WHERE tenant_id = ?` filter to all queries for tenant-aware entities. The filters are parameterized with the `tenant_id` from the `AsyncLocalStorage`.

### 4.4. Plugin System

* **Goal:** Extensibility of the EAF and applications built upon it.
* **Approach:** Definition of a plugin interface and a mechanism for loading and registering plugins.
* **Entity Discovery:** Leverages **MikroORM's** capability to discover entities via glob patterns, allowing plugins to define their own database entities managed centrally.
* **Interaction:** Interaction of plugins with Tenancy, RBAC, Licensing must be considered in the design (partially TBD).

## 5. Technology Stack (V1 Selection)

* **Language/Runtime:** TypeScript, Node.js
* **Backend Framework:** NestJS
* **Database:** PostgreSQL
* **ORM:** MikroORM (with glob-pattern Entity Discovery)
* **Cache:** Redis
* **Testing:** Jest, `suites` (Unit), Testcontainers (Integration), `supertest`, `@nestjs/testing`
* **Authorization:** Recommendation: `casl`
* **Internationalization:** `nestjs-i18n`
* **Security Middleware:** `helmet`, `@nestjs/throttler`
* **Observability:** `@nestjs/terminus` (Health Checks), Structured Logging (Implementation TBD)
* **API Documentation:** OpenAPI (Integration via NestJS)
* **Monorepo Management:** Recommendation: Nx
* **SBOM:** Tooling TBD (e.g., `@cyclonedx/bom`), Format TBD (e.g., CycloneDX)

*(Justifications for specific decisions are or will be documented in ADRs)*

## 6. Component Overview (Planned)

* **`libs/`:**
  * `core`: Domain logic, Application Use Cases & Ports.
  * `infrastructure`: Adapters (Persistence [MikroORM/PG], Cache [Redis], Web [NestJS], i18n Setup etc.).
  * `tenancy`: Tenant context handling (AsyncLocalStorage, Middleware).
  * `rbac`: RBAC/ABAC core logic, Guards (Rec.: `casl` integration).
  * `licensing`: Core logic for license validation.
  * `plugins`: Plugin interface and loading mechanism.
  * `testing`: Shared test utilities (Testcontainers Setup etc.).
  * `shared`: Shared DTOs, constants, simple utilities.
* **`apps/`:**
  * `control-plane-api`: NestJS application for tenant management.
  * `sample-app`: NestJS application demonstrating the EAF.

## 7. Key Concepts in Detail

### 7.1. Data Persistence

* MikroORM is configured to communicate with PostgreSQL.
* The Event Store is implemented as a table (e.g., `events`) with columns for `stream_id`, `version`, `event_type`, `payload`, `timestamp`, `tenant_id`.
* Read Models are separate tables, optimized for read access, also containing `tenant_id`.
* RLS is enforced via globally enabled MikroORM Filters, dynamically parameterized with the `tenant_id` from the context.
* Entity Discovery via glob patterns allows plugins to easily contribute their own entities.

### 7.2. Authentication & Authorization

* Flexible AuthN module (V1: JWT, Local). Configurable per tenant (later).
* RBAC + Basic Ownership (ABAC). Recommendation: `casl` for defining and checking permissions.
* Standardized NestJS Guards for easily securing endpoints/methods.
* Provision of backend services/APIs for managing roles/permissions at the tenant level.

### 7.3. Observability

* V1: Ensuring structured logging (format TBD), Health Checks (`/health/live`, `/health/ready`).
* Roadmap: Integration of metrics (Prometheus) and Distributed Tracing (OpenTelemetry).

### 7.4. License Validation

* Dedicated module (`libs/licensing`).
* Validation logic (Online/Offline TBD) is called on startup and/or periodically.
* Must be robust against tampering.

### 7.5. Testing Strategy Overview

* Unit Tests (`suites`, Jest) for Core logic (mocked dependencies).
* Integration Tests (Jest, Testcontainers, MikroORM) for Adapters against real DB/Cache.
* E2E Tests (Jest, `supertest`, `@nestjs/testing`) for complete API flows.

### 7.6. SBOM

* Generation of SBOMs (e.g., CycloneDX) becomes part of the CI/CD build process for all applications and core libraries.

## 8. Deployment Considerations

* Applications are typically packaged as Docker containers.
* Monorepo builds (e.g., with Nx) generate optimized artifacts per application.
* Health Check endpoints support orchestration platforms (Kubernetes).
* Graceful Shutdown is implemented.

## 9. References

* Product Requirements Document (PRD) ACCI EAF V1.0
* Architecture Decision Records (ADRs) in `docs/adr/` directory

## 10. Diagrams (Placeholders)

*Detailed diagrams will be created and maintained as needed. Potential diagram types:*

* C4 Model: System Context, Containers, Components
* UML: Component Diagram, Sequence Diagrams (for CQRS Flows), Class Diagrams (for core entities)
* Free-form Flowcharts

*(Examples could be embedded or linked here:)*

* *High-Level Component Overview (Monorepo)*
* *Request Flow (Command)*
* *Request Flow (Query)*
* *Tenant Context Propagation*

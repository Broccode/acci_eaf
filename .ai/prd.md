# 1. Title: PRD for ACCI EAF

<version>1.1 Updated</version>
<date>2025-04-22</date>
<author>Michael Walloschke</author>

## Status: Approved

## Intro

The ACCI EAF (Axians Competence Center Infrastructure Enterprise Application Framework) is an internal software framework designed to accelerate, standardize, and secure the development of robust, scalable, and maintainable enterprise applications for Axians and its customers. It addresses common pain points in enterprise app development by providing a solid foundation, reusable components, and best practices for multi-tenancy, security, observability, and compliance. It combines modern technologies (TypeScript, NestJS, MikroORM) with proven architectural patterns (Hexagonal Architecture, CQRS/Event Sourcing, Multi-Tenancy via RLS, RBAC/ABAC) and best practices (Observability, Security by Design, Testability, i18n, SBOM) to increase development speed, improve code quality, ensure technical consistency, and establish the prerequisites for industry standards and certifications (e.g., ISO 27001, SOC2).

## Goals

- **Accelerate Development:** Providing a solid foundation, reusable components, and clear patterns.
- **Promote Best Practices:** Implementing Hexagonal Architecture, CQRS/ES, Multi-Tenancy, RBAC/ABAC, i18n, SBOM, Security by Design as standards.
- **Improve Quality:** High test coverage through defined strategies (Unit/Integration/E2E) and tools (`suites`, Testcontainers, MikroORM testing features).
- **Increase Scalability:** Designing for horizontal scalability through CQRS and stateless components where possible.
- **Ensure Traceability:** Through Event Sourcing (with defined evolution strategy - ADR-010) and preparation for a dedicated audit trail.
- **Enhance Maintainability:** Clear separation of concerns, modularity, standardized processes (ADRs).
- **Enable Flexibility:** Through a plugin system (with MikroORM Entity Discovery and defined migration strategy - ADR-008) and configurability (e.g., AuthN methods per tenant).
- **Support the Business Model:** Integrating a mechanism for license validation (Hybrid approach - ADR-003).
- **Support Compliance:** Providing features and documentation that aid in certification processes (ISO 27001, SOC2).

## Features and Requirements

### Scope V1: The Foundation

- **In Scope (V1):**
  - **Core Architecture:** Hexagonal Architecture, CQRS/ES base mechanisms (incl. schema evolution concept - ADR-010, idempotency concept - ADR-009), Plugin System basics (incl. migration strategy - ADR-008).
  - **Multi-Tenancy:** Core functionality via Row-Level Security (RLS) with `tenant_id` column (implemented via MikroORM Global Filters - ADR-006); Tenant Context discovery (Token/Header) and propagation (via `AsyncLocalStorage`).
  - **Control Plane API:** Separate API (in monorepo) for System Administrators to manage tenants (CRUD operations for basic tenant attributes). Bootstrapping defined (ADR-007).
  - **Standard Adapters:** PostgreSQL adapter (MikroORM for Event Store schema & Read Models), Redis adapter (for Caching).
  - **Internationalization (i18n):** Basic integration of `nestjs-i18n` for API responses (errors, validation) and locale detection.
  - **License Validation:** Core mechanism to validate a provided license using a hybrid offline/online approach (ADR-003).
  - **Observability Basics:** Hooks/structure for structured logging; Health Check endpoints (`@nestjs/terminus`).
  - **Security Basics:** Standard Security Headers (`helmet`), Rate Limiting (`@nestjs/throttler`), Base Authentication (JWT validation, local password strategy), Base Authorization (RBAC Core logic and Enforcement Guards using `casl` - ADR-001, Basic Ownership Check for `ownerUserId`), OWASP Top 10 consideration in design.
  - **Reliability Basics:** Hooks for Graceful Shutdown. Idempotent event handlers concept (ADR-009).
  - **Testing Framework:** Defined strategy and tools (`suites` for Unit tests of DI components, Testcontainers for Integration tests with MikroORM, `@nestjs/testing`+`supertest` for E2E tests).
  - **API Standards:** Recommendation and base configuration for using OpenAPI.
  - **SBOM Generation:** Standardized process for generating Software Bill of Materials using `@cyclonedx/cyclonedx-npm` in CycloneDX JSON format (ADR-002).
  - **Development Practices:** Monorepo setup (recommendation: Nx), Use of Architecture Decision Records (ADRs), MikroORM Entity Discovery via glob patterns.
  - **Basic Documentation:** Setup guide, architecture overview, core concepts, ADRs.
  - **Project Template:** Basic template for creating new applications using the EAF.

- **Out of Scope (for V1):**
  - Application-specific business logic or domain implementations.
  - UI Frameworks, Frontend for the Control Plane, Frontend for tenant-specific RBAC administration.
  - Complete, production-ready CI/CD pipeline templates (only basics for EAF internal development).
  - Extensive library of pre-built plugins.
  - **Advanced Observability:** Metric export (Prometheus), Distributed Tracing (OpenTelemetry).
  - **Advanced AuthN:** OIDC, LDAP/AD integration.
  - **Dedicated Audit Trail:** Separate service/module for immutable audit logs.
  - **Advanced Reliability:** Retry mechanisms, Circuit Breaker patterns.
  - **Advanced Configuration:** Dynamic Configuration, Feature Flag system.
  - **Background Job System:** Integration with task queues (e.g., BullMQ).
  - **User Groups:** Support for user groups in the RBAC/ABAC model.
  - **Full ISO 27001 / SOC2 Support Tooling:** Dedicated compliance reports, fully automated control evidence (V1 *enables* this).
  - **CLI Enhancements:** Custom schematics for `nest g ...`.
  - **Admin UIs:** For Control Plane or tenant-specific administration (e.g., RBAC).
  - **Online License Server:** Operation and API definition of the Axians license server (Interaction defined in ADR-003).

### Functional Requirements (FR) - V1 Overview

- **Framework Core (FR-CORE):** Monorepo structure, base libraries, CQRS buses, ES base (incl. evolution/idempotency concepts), plugin loader (incl. migrations).
- **Multi-Tenancy (FR-MT):** `tenant_id` discovery/context, RLS implementation via MikroORM Global Filters.
- **Control Plane API (FR-CP):** Separate API, Tenant CRUD, Admin AuthZ.
- **Persistence Adapters (FR-PA):** MikroORM setup for PG (tenant-aware Entities via Discovery, RLS via Filters), Redis Cache Adapter.
- **Authentication (FR-AUTHN):** AuthN module, JWT strategy, Local strategy (secure), User-Tenant linkage.
- **Authorization (FR-AUTHZ):** RBAC module (`casl` - ADR-001), Data model (MikroORM Entities, tenant-aware), RBAC Guards, Ownership check (`ownerUserId`), Tenant Admin APIs/Services for RBAC.
- **License Validation (FR-LIC):** Validation module/service implementing hybrid approach (ADR-003).
- **Observability (FR-OBS):** Structured logging (hooks/interface), Health Check endpoints.
- **Security (FR-SEC):** `helmet` integration, `throttler` integration.
- **Internationalization (FR-I18N):** `nestjs-i18n` setup, validation translation, error translation (base), service/context provision.
- **API (FR-API):** Standard controller structure, DTO validation (i18n), OpenAPI setup.
- **SBOM (FR-SBOM):** Integration of SBOM generation (`@cyclonedx/cyclonedx-npm`, CycloneDX JSON - ADR-002) into the build process.
- **Deployment (FR-DEPLOY):** CI/CD process to create offline tarball package (incl. `docker save`, scripts). Provision of reference `docker-compose.yml` and `.env.example`. Mechanism for running migrations in the offline setup.

### Non-Functional Requirements (NFR) - V1

| ID     | Category        | Requirement                                                                                                                                                                                             | Measurement/Target (Example)                               | ADR Link (if applicable) |
| :----- | :--------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | :--------------------------------------------------------- | :----------------------- |
| NFR-01 | Performance      | API response times for typical read operations (Queries) should be low. Command processing should be efficient. RLS (via Filters) should not cause significant bottlenecks.                                | P95 Latency < 200ms (Queries), Baseline Command Throughput |                          |
| NFR-02 | Scalability      | Architecture must allow horizontal scaling of read and write paths (within the limits of a VM/Compose environment). Statelessness where possible.                                                        | Scalability tests under load (simulated)                 |                          |
| NFR-03 | Reliability      | Graceful Shutdown must be implemented. Basic error handling in adapters. Event processing for projections should be robust (Idempotency via tracking table). Offline update process should be robust.       | Tests, Code Reviews, Test of update script             | ADR-009                  |
| NFR-04 | Security         | **Must lay groundwork for ISO 27001/SOC2.** RLS implementation (via Filters) must be secure. Protection against common web attacks (OWASP Top 10 consideration). Secure AuthN/AuthZ base components (`casl`). License validation (hybrid) robust. SBOM available. | Security Reviews, Pentest (later), OWASP Mapping Check    | ADR-001, ADR-003, ADR-006 |
| NFR-05 | Maintainability  | Code should follow SOLID principles, be well-documented (code comments, ADRs). High test coverage. Clear module boundaries (Hexagonal). Event schema evolution managed.                                  | Code Coverage > 85% (Core Libs), Static Code Analysis    | ADR-010                  |
| NFR-06 | Testability      | Core logic (Domain/Application) must be testable in isolation. Integration tests must be reliable (Testcontainers + MikroORM). Unit tests easy to write (`suites`).                                      | Test pyramid implemented                                 |                          |
| NFR-07 | Extensibility    | Plugin system must allow extensions without core changes (incl. Entity Discovery, Migrations). Architecture should allow swapping adapters.                                                               | Example plugin implementation, Design Review             | ADR-008                  |
| NFR-08 | Documentation    | Comprehensive documentation: Setup (incl. Offline/Tarball, Bootstrapping), Architecture, Concepts (CQRS, ES, Multi-Tenancy, RBAC, Licensing, SBOM, MikroORM UoW/Filters), How-Tos, API Reference (framework parts), ADRs. | Availability & Quality of Docs                          | ADR-007                  |
| NFR-09 | Developer Exp.   | Intuitive usage, good IDE support (TypeScript), clear error feedback, simple project setup (template), easy testability (`suites`), Monorepo tooling (Nx), easy plugin development.                      | Developer Feedback                                      |                          |
| NFR-10 | i18n Support     | Framework supports translation of API responses (validation, errors) based on locale via `nestjs-i18n`.                                                                                                 | Tests for localized responses                          |                          |
| NFR-11 | Licensing        | The hybrid validation mechanism must be reliable and secure against simple bypass (esp. offline).                                                                                                      | Design Review, Tests                                      | ADR-003                  |
| NFR-12 | Compliance       | Framework must support generation of SBOMs in standard formats (`@cyclonedx/cyclonedx-npm`, CycloneDX JSON). Design considers compliance requirements.                                                    | SBOM generation in build, Design Reviews                | ADR-002                  |
| NFR-13 | Deployment       | The generated tarball package must contain all necessary artifacts for offline installation. Setup/update scripts must be robust.                                                                       | Test of installation/update from tarball              |                          |

## Epic List

### Epic-1: Framework Core & Architecture

### Epic-2: Multi-Tenancy & Control Plane

### Epic-3: Security & Compliance

### Epic-4: Observability & Testing

### Epic-5: Documentation & Developer Experience

### Epic-N: Future Enhancements (Advanced AuthN, Audit Trail, Admin UIs, etc.)

## Epic 1: Story List (High Level - Details in separate .story.md files)

- Story 1: Monorepo and Project Template Setup
  Status: 'Completed'
  Requirements:
  - Set up Nx monorepo
  - Provide project scaffolding for apps/libs

- Story 2: Core Architecture Implementation
  Status: 'Completed'
  Requirements:
  - Implement Hexagonal Architecture base
  - Set up CQRS/ES mechanisms (Buses, Event Store interface, Evolution/Idempotency concepts)
  - Establish plugin system basics (Interface, Loader, Migration handling)

- Story 3: Multi-Tenancy Foundation
  Status: 'Completed'
  Requirements:
  - Implement tenant_id context extraction (Middleware) and propagation (AsyncLocalStorage)
  - Implement RLS via MikroORM Global Filters (ADR-006)

- Story 4: Control Plane API
  Status: 'Completed'
  Requirements:
  - Create API for tenant CRUD (Basic attributes)
  - Admin AuthZ for control plane
  - Implement bootstrapping procedure (ADR-007)

- Story 5: Security & AuthN/AuthZ
  Status: ''
  Requirements:
  - Integrate JWT and local AuthN strategies
  - Implement RBAC/ABAC (`casl` - ADR-001) core logic, guards, basic ownership check (`ownerUserId`)
  - Security headers (helmet), rate limiting (throttler)

- Story 6: Observability & Health Checks
  Status: ''
  Requirements:
  - Structured logging setup (hook/interface)
  - Health endpoints (`@nestjs/terminus`)

- Story 7: License Validation
  Status: ''
  Requirements:
  - Implement license validation module/service interface (Hybrid approach - ADR-003)

- Story 8: Testing Framework
  Status: ''
  Requirements:
  - Set up unit (`suites`), integration (Testcontainers), and E2E testing (`supertest`) infrastructure

- Story 9: Documentation & SBOM
  Status: ''
  Requirements:
  - Provide initial setup and architecture docs
  - Integrate SBOM generation (`@cyclonedx/cyclonedx-npm` - ADR-002) into build process

## Technology Stack

| Technology      | Description                                                      | ADR Decision |
| -------------- | ---------------------------------------------------------------- | ------------ |
| TypeScript     | Main language                                                    |              |
| Node.js        | Runtime environment                                              |              |
| NestJS         | Backend framework                                                |              |
| PostgreSQL     | Database for event store and read models                         |              |
| MikroORM       | ORM with entity discovery and RLS filters                        |              |
| Redis          | Caching                                                          |              |
| Nx             | Monorepo management (recommended)                                |              |
| Jest           | Testing framework                                                |              |
| `suites`       | Unit testing utilities for DI components                         |              |
| Testcontainers | Integration testing with real DB/Cache                           |              |
| `supertest`    | E2E testing HTTP requests                                       |              |
| `@nestjs/testing`| NestJS testing utilities                                        |              |
| `nestjs-i18n`  | Internationalization library                                    |              |
| `casl`         | RBAC/ABAC library                                                | ADR-001      |
| `helmet`       | Security headers middleware                                      |              |
| `@nestjs/throttler` | Rate limiting middleware                                        |              |
| `@nestjs/terminus` | Health checks module                                            |              |
| OpenAPI        | API documentation standard (via NestJS)                           |              |
| `@cyclonedx/cyclonedx-npm` | SBOM generation tool                                          | ADR-002      |
| CycloneDX (JSON) | SBOM format                                                      | ADR-002      |

## Reference

- See docs/PRD.md and docs/ARCH.md for original detailed documentation
- Mermaid diagrams and further visualizations to be added in architecture document (`.ai/arch.md`)
- Architecture Decision Records (ADRs) in `docs/adr/` (Key decisions reflected above)

## Data Models, API Specs, Schemas, etc

- Event Store: `events` table (columns: `stream_id`, `version`, `event_type` [versioned - ADR-010], `payload`, `timestamp`, `tenant_id`)
- Read Models: Tenant-aware tables (e.g., `tenant_id` column) optimized for query needs. Potentially `processed_events` table for idempotency (ADR-009).
- User, Tenant, Role, Permission entities (RBAC/ABAC model, MikroORM entities, tenant-aware where appropriate)
- License schema (Defined in ADR-003)

## Project Structure

```text
apps/
  control-plane-api/    # API for tenant management (NestJS)
  sample-app/           # Sample application using EAF (NestJS)
libs/
  core/                 # Domain logic (Aggregates, Entities, VO, Domain Events), Application logic (Use Cases, Ports) - Technology Agnostic
  infrastructure/       # Adapters (Persistence[MikroORM/PG], Cache[Redis], Web[NestJS], i18n Setup, etc.) - Implements Ports
  tenancy/              # Tenant context handling (AsyncLocalStorage, Middleware)
  rbac/                 # RBAC/ABAC core logic, Guards (e.g., casl integration)
  licensing/            # Core logic for license validation
  plugins/              # Plugin interface and loading mechanism
  testing/              # Shared test utilities (Testcontainers Setup, etc.)
  shared/               # Shared DTOs, constants, simple utilities
```

docs/
  adr/                  # Architecture Decision Records

## Open Questions & Assumptions

- **Resolved via ADRs:**
  - **Licensing Details:** Addressed by ADR-003 (Hybrid model, core attributes defined). Specifics like CPU core measurement method might still need refinement depending on environment/OS. Online server API spec is out of scope for EAF. Policy on network failure depends on configuration (Fail Open/Closed). Secure reporting TBD if needed. Optionality for internal projects TBD.
  - **RLS Enforcement:** Addressed by ADR-006 (MikroORM Global Filters + AsyncLocalStorage).
  - **Control Plane Bootstrapping:** Addressed by ADR-007 (Script/initial admin user).
  - **CQRS/ES:**
    - Event Schema Evolution: Addressed by ADR-010 (Versioning + Upcasting).
    - Idempotent Event Handlers: Addressed by ADR-009 (Tracking table).
  - **Plugins:** Migrations addressed by ADR-008. Interaction specifics TBD based on plugin needs.
  - **Technology Choices:** RBAC (`casl` - ADR-001), SBOM (`@cyclonedx/cyclonedx-npm` - ADR-002) decided.
  - **Deployment:** Structure/content of tarball scripts needs detail, Backup/restore strategy standard Docker practice.

- **Remaining Open Questions / Points for Refinement:**
  - **Shared Data Access:** Exact specification where cross-tenant data resides and how access should be standardized (e.g., via config providers)? (Minor point, might depend on specific needs).
  - **Tenant Attributes:** Which *additional* specific attributes (beyond ADR-003 license/basic ID) does the Control Plane need for tenant management? (Depends on future features).
  - **ISO/SOC2:** Which specific controls require *direct* support through framework features in *future* versions? (Ongoing concern).
  - **CPU Core Measurement:** Practical implementation details for `maxCpuCores` check across different environments (if required by licensing model).

- **Updated Assumptions:**
  - **Confirmed:** EAF provides backend APIs/logic for Tenant RBAC admin; UI is app-specific.
  - **Confirmed:** V1 focuses on the foundation; advanced features follow iteratively.
  - **Confirmed:** RLS uses `tenant_id` column and MikroORM Global Filters (ADR-006).
  - **Confirmed:** MikroORM Entity Discovery configured via glob patterns.
  - **Confirmed:** Primary deployment method for customers is an offline tarball for VMs with Docker Compose.
  - **Confirmed:** RBAC library is `casl` (ADR-001).
  - **Confirmed:** SBOM tool is `@cyclonedx/cyclonedx-npm` (ADR-002).
  - **Confirmed:** License validation uses a hybrid approach (ADR-003).
  - **Confirmed:** Plugin migrations are handled via main application process (ADR-008).
  - **Confirmed:** Event handler idempotency uses a tracking table (ADR-009).
  - **Confirmed:** Event schema evolution uses versioning/upcasting (ADR-010).
  - **Confirmed:** Control plane bootstrapping follows ADR-007.

## Change Log

| Change        | Story ID | Description                                      | ADRs Impacting |
| ------------- | -------- | ------------------------------------------------ | -------------- |
| Initial draft | N/A      | Initial PRD for ACCI EAF                         |                |
| Update        | N/A      | Incorporate decisions from ADRs 001-010        | 001-010        |

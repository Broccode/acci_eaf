# Product Requirements Document: ACCI EAF

* **Version:** 1.0 Draft
* **Date:** 2025-04-20
* **Author:** Michael Walloschke
* **Status:** Draft

## 1. Introduction & Vision

This document describes the requirements for the **ACCI EAF** (Axians Competence Center Infrastructure Enterprise Application Framework). The ACCI EAF is an internal software framework serving as the foundation for developing robust, scalable, secure, multi-tenant, and maintainable enterprise applications developed by Axians and licensed to customers.

It combines modern technologies (TypeScript, NestJS) with proven architectural patterns (Hexagonal Architecture, CQRS/Event Sourcing, Multi-Tenancy via RLS, RBAC/ABAC) and best practices (Observability, Security by Design, Testability, i18n, SBOM) to increase development speed, improve code quality, ensure technical consistency, and establish the prerequisites for industry standards and certifications (e.g., ISO 27001, SOC2). **MikroORM** is used as the ORM.

## 2. Goals & Motivation

Developing enterprise applications for customers poses high demands regarding security, scalability, adaptability, and maintainability. Common challenges include:

* Inconsistent architectures and security levels.
* High development effort for basic functionalities (multi-tenancy, AuthN/AuthZ, auditing, licensing).
* Difficulties in scaling, maintenance, and evolution.
* Lack of testability and observability.
* Missing traceability of data changes and actions.
* Need to support a licensing model.
* Need to support compliance requirements (ISO 27001, SOC2).

The ACCI EAF aims to address these challenges by:

* **Accelerating Development:** Providing a solid foundation, reusable components, and clear patterns.
* **Promoting Best Practices:** Implementing Hexagonal Architecture, CQRS/ES, Multi-Tenancy, RBAC/ABAC, i18n, SBOM, Security by Design as standards.
* **Improving Quality:** High test coverage through defined strategies (Unit/Integration/E2E) and tools (`suites`, Testcontainers, MikroORM testing features).
* **Increasing Scalability:** Designing for horizontal scalability through CQRS and stateless components where possible.
* **Ensuring Traceability:** Through Event Sourcing and preparation for a dedicated audit trail.
* **Enhancing Maintainability:** Clear separation of concerns, modularity, standardized processes (ADRs).
* **Enabling Flexibility:** Through a plugin system (with MikroORM Entity Discovery) and configurability (e.g., AuthN methods per tenant).
* **Supporting the Business Model:** Integrating a mechanism for license validation.
* **Supporting Compliance:** Providing features and documentation that aid in certification processes (ISO 27001, SOC2).

## 3. Target Audience

* **Primary:** Software development teams at Axians developing enterprise applications for customers.
* **Secondary:** Technical architects at Axians designing solutions and defining technical standards. Security & Compliance Officers at Axians.

## 4. Scope

The ACCI EAF will be developed in phases.

### 4.1. Version 1.0 (V1) - The Foundation

* **In Scope (V1):**
  * **Core Architecture:** Hexagonal Architecture, CQRS/ES base mechanisms, Plugin System basics.
  * **Multi-Tenancy:** Core functionality via Row-Level Security (RLS) with `tenant_id` column (implemented via MikroORM Filters); Tenant Context discovery (Token/Header) and propagation (via `AsyncLocalStorage`).
  * **Control Plane API:** Separate API (in monorepo) for System Administrators to manage tenants (CRUD operations for basic tenant attributes).
  * **Standard Adapters:** PostgreSQL adapter (MikroORM for Event Store schema & Read Models), Redis adapter (for Caching).
  * **Internationalization (i18n):** Basic integration of `nestjs-i18n` for API responses (errors, validation) and locale detection.
  * **License Validation:** Core mechanism to validate a provided license (implementation details TBD, see Open Questions).
  * **Observability Basics:** Hooks/structure for structured logging; Health Check endpoints (`@nestjs/terminus`).
  * **Security Basics:** Standard Security Headers (`helmet`), Rate Limiting (`@nestjs/throttler`), Base Authentication (JWT validation, local password strategy), Base Authorization (RBAC Core logic and Enforcement Guards [recommendation: `casl`], Basic Ownership Check for `ownerUserId`), OWASP Top 10 consideration in design.
  * **Reliability Basics:** Hooks for Graceful Shutdown.
  * **Testing Framework:** Defined strategy and tools (`suites` for Unit tests of DI components, Testcontainers for Integration tests with MikroORM, `@nestjs/testing`+`supertest` for E2E tests).
  * **API Standards:** Recommendation and base configuration for using OpenAPI.
  * **SBOM Generation:** Standardized process for generating Software Bill of Materials (Tooling e.g., `@cyclonedx/bom`, Format e.g., CycloneDX).
  * **Development Practices:** Monorepo setup (recommendation: Nx), Use of Architecture Decision Records (ADRs), MikroORM Entity Discovery via glob patterns.
  * **Basic Documentation:** Setup guide, architecture overview, core concepts, ADRs.
  * **Project Template:** Basic template for creating new applications using the EAF.

* **Out of Scope (for V1):**
  * Application-specific business logic or domain implementations.
  * UI Frameworks, Frontend for the Control Plane, Frontend for tenant-specific RBAC administration.
  * Complete, production-ready CI/CD pipeline templates.
  * Extensive library of pre-built plugins.
  * **Advanced Observability:** Metric export (Prometheus), Distributed Tracing (OpenTelemetry).
  * **Advanced AuthN:** OIDC, LDAP/AD integration.
  * **Dedicated Audit Trail:** Separate service/module for immutable audit logs.
  * **Advanced Reliability:** Retry mechanisms, Circuit Breaker patterns.
  * **Advanced Configuration:** Dynamic Configuration, Feature Flag system.
  * **Background Job System:** Integration with task queues (e.g., BullMQ).
  * **User Groups:** Support for user groups in the RBAC/ABAC model.
  * **Full ISO 27001 / SOC2 Support Tooling:** Dedicated compliance reports, fully automated control evidence (V1 *enables* this).
  * **CLI Enhancements:** Custom schematics for `nest g ...`.
  * **Admin UIs:** For Control Plane or tenant-specific administration (e.g., RBAC).

### 4.2. Future Versions (Roadmap)

* Implementation of features listed under "Out of Scope (for V1)" based on prioritization and demand.
* Deepening support for certifications.
* Performance optimizations.
* Enhancement of plugin capabilities.
* Support for additional databases/technologies (optional).

## 5. Functional Requirements (FR) - V1

*(Note: Detailed breakdown follows in separate specification, thematic overview here)*

* **Framework Core (FR-CORE):** Monorepo structure, base libraries, CQRS buses, ES base, plugin loader.
* **Multi-Tenancy (FR-MT):** `tenant_id` discovery/context, RLS implementation via MikroORM Filters.
* **Control Plane API (FR-CP):** Separate API, Tenant CRUD, Admin AuthZ.
* **Persistence Adapters (FR-PA):** MikroORM setup for PG (tenant-aware Entities via Discovery, RLS via Filters), Redis Cache Adapter.
* **Authentication (FR-AUTHN):** AuthN module, JWT strategy, Local strategy (secure), User-Tenant linkage.
* **Authorization (FR-AUTHZ):** RBAC module (Rec.: `casl`), Data model (MikroORM Entities, tenant-aware), RBAC Guards, Ownership check (`ownerUserId`), Tenant Admin APIs/Services for RBAC.
* **License Validation (FR-LIC):** Validation module/service, checking constraints (details TBD).
* **Observability (FR-OBS):** Structured logging (hooks/interface), Health Check endpoints.
* **Security (FR-SEC):** `helmet` integration, `throttler` integration.
* **Internationalization (FR-I18N):** `nestjs-i18n` setup, validation translation, error translation (base), service/context provision.
* **API (FR-API):** Standard controller structure, DTO validation (i18n), OpenAPI setup.
* **SBOM (FR-SBOM):** Integration of SBOM generation (e.g., CycloneDX) into the build process.

## 6. Non-Functional Requirements (NFR) - V1

| ID     | Category        | Requirement                                                                                                                                                                                             | Measurement/Target (Example)                               |
| :----- | :--------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | :--------------------------------------------------------- |
| NFR-01 | Performance      | API response times for typical read operations (Queries) should be low. Command processing should be efficient. RLS (via Filters) should not cause significant bottlenecks.                                | P95 Latency < 200ms (Queries), Baseline Command Throughput |
| NFR-02 | Scalability      | Architecture must allow horizontal scaling of read and write paths. Statelessness where possible.                                                                                                       | Scalability tests under load                               |
| NFR-03 | Reliability      | Graceful Shutdown must be implemented. Basic error handling in adapters. Event processing for projections should be robust (Goal: Idempotency of handlers).                                             | Tests, Code Reviews                                        |
| NFR-04 | Security         | **Must lay groundwork for ISO 27001/SOC2.** RLS implementation (via Filters) must be secure. Protection against common web attacks (OWASP Top 10 consideration). Secure AuthN/AuthZ base components. License validation robust. SBOM available. | Security Reviews, Pentest (later), OWASP Mapping Check    |
| NFR-05 | Maintainability  | Code should follow SOLID principles, be well-documented (code comments, ADRs). High test coverage. Clear module boundaries (Hexagonal).                                                              | Code Coverage > 85% (Core Libs), Static Code Analysis    |
| NFR-06 | Testability      | Core logic (Domain/Application) must be testable in isolation. Integration tests must be reliable (Testcontainers + MikroORM). Unit tests easy to write (`suites`).                                      | Test pyramid implemented                                 |
| NFR-07 | Extensibility    | Plugin system must allow extensions without core changes (incl. Entity Discovery). Architecture should allow swapping adapters.                                                                       | Example plugin implementation, Design Review             |
| NFR-08 | Documentation    | Comprehensive documentation: Setup, Architecture, Concepts (CQRS, ES, Multi-Tenancy, RBAC, Licensing, SBOM, MikroORM UoW/Filters), How-Tos, API Reference (framework parts), ADRs.                       | Availability & Quality of Docs                          |
| NFR-09 | Developer Exp.   | Intuitive usage, good IDE support (TypeScript), clear error feedback, simple project setup (template), easy testability (`suites`), Monorepo tooling (Nx), easy plugin development.                      | Developer Feedback                                      |
| NFR-10 | i18n Support     | Framework supports translation of API responses (validation, errors) based on locale via `nestjs-i18n`.                                                                                                 | Tests for localized responses                          |
| NFR-11 | Licensing        | The validation mechanism must be reliable and secure against simple bypass.                                                                                                                          | Design Review, Tests                                      |
| NFR-12 | Compliance       | Framework must support generation of SBOMs in standard formats (e.g., CycloneDX). Design considers compliance requirements.                                                                              | SBOM generation in build, Design Reviews                |

## 7. Design & Architecture (Overview)

* **Core Technologies:** TypeScript, Node.js, NestJS.
* **Architectural Patterns:** Hexagonal Architecture (Ports & Adapters), CQRS, Event Sourcing, Multi-Tenancy (RLS via MikroORM Filters), RBAC + Basic ABAC (Ownership).
* **Project Structure:** Monorepo (Recommendation: Nx). Clear separation into `apps/` and `libs/`.
* **Database:** PostgreSQL (for Event Store & Read Models in V1).
* **ORM:** **MikroORM** (with Entity Discovery via glob patterns).
* **Cache:** Redis (for Caching in V1).
* **Tenant Context:** Token/Header -> `AsyncLocalStorage`.
* **Internationalization:** `nestjs-i18n`.
* **Testing:** Jest, `suites` (Unit), Testcontainers (Integration with MikroORM), `supertest`, `@nestjs/testing`.
* **Authorization:** Recommendation `casl`.
* **SBOM:** Tooling TBD (e.g., `@cyclonedx/bom`), Format TBD (e.g., CycloneDX).
* **Decisions:** Documented via ADRs in `docs/adr/`.
* **Diagrams:** Use of appropriate diagram types (C4, UML etc.) for visualization.

## 8. Release Criteria (for V1)

* All functional requirements (FR) defined as "In Scope (V1)" are implemented and covered by tests.
* Core NFRs (especially Security, Reliability, Testability, Documentation, Licensing Validation) are demonstrably met.
* Defined code coverage targets are achieved.
* Basic documentation is available and reviewable.
* A functional sample project (`apps/sample-app`) demonstrates core features.
* Successful build and test runs of all components in a reference CI environment.
* Review and approval of the architecture and core components.

## 9. Success Metrics

* Adoption rate by Axians development teams for new projects.
* Reduction in initial development time for projects using the EAF (qualitative/quantitative).
* Developer satisfaction (surveys).
* Consistency and quality of applications built with the EAF (code reviews).
* Successful use in customer projects including licensing.
* Positive feedback regarding compliance/audit support.

## 10. Open Questions & Assumptions

* **Licensing Details:**
  * Exact method for measuring CPU cores in customer environments?
  * Exact validation logic: Online (own Axians license server?) vs. Offline (signed keys)? Frequency? Behavior on failure (grace period, feature degradation, stop)?
  * Need/mechanism for secure metric reporting to Axians?
  * Security aspects of the licensing mechanism? Is it optional for internal/non-licensed projects?
* **RLS Enforcement (MikroORM Filters):** Best practices for configuring and dynamically passing parameters (`tenant_id`) to global filters in the NestJS context?
* **Shared Data Access:** Exact specification where cross-tenant data resides and how access should be standardized (e.g., via config providers)?
* **Control Plane Bootstrapping:** Concrete process for creating the first tenant (if needed) and the first system administrator?
* **Tenant Attributes:** Which specific attributes (beyond name/ID, e.g., for config, license details) does the Control Plane need for tenant management?
* **CQRS/ES:**
  * Long-term strategy for Event Schema Evolution (suggestion needed for V1?).
  * Framework support for idempotent Event Handlers?
* **ISO/SOC2:** Which specific controls require *direct* support through framework features in *future* versions to significantly ease product certification?
* **Plugins:** Exact interaction with Tenancy, Licensing, and RBAC? How are MikroORM entities from plugins reliably discovered and migrations managed?
* **Technology Choices:**
  * Concrete choice of RBAC library (`casl` recommended)? -> ADR required.
  * Concrete choice of SBOM tool and format? -> ADR required.
* **Assumption:** The EAF provides backend APIs/logic for Tenant RBAC admin; the UI is application-specific.
* **Assumption:** V1 focuses on the foundation; advanced enterprise features will follow iteratively according to the roadmap.
* **Assumption:** RLS implementation uses a `tenant_id` column in relevant tables and is enforced via MikroORM Filters.
* **Assumption:** MikroORM Entity Discovery is configured via glob patterns.

## 11. Glossary (Optional)

* **ACCI EAF:** Axians Competence Center Infrastructure Enterprise Application Framework.
* **RLS:** Row-Level Security. Data access restriction at the row level, here using `tenant_id`.
* **RBAC:** Role-Based Access Control. Access based on user roles.
* **ABAC:** Attribute-Based Access Control. Access based on attributes. Here specifically: Ownership.
* **Control Plane:** Separate API for central management of tenants by system administrators.
* **Tenant Context:** Information identifying the current tenant for an operation.
* **ADR:** Architecture Decision Record. Documentation of important architectural decisions.
* **CQRS:** Command Query Responsibility Segregation. Separation of read and write operations.
* **ES:** Event Sourcing. Storing all state changes as a sequence of events.
* **i18n:** Internationalization. Adapting software to different languages and regions.
* **SBOM:** Software Bill of Materials. Inventory list of software components and dependencies.
* **ORM:** Object-Relational Mapper. Here: MikroORM.
* **UoW:** Unit of Work. A design pattern (used by MikroORM) for managing object changes and transactions.

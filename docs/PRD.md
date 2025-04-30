# Product Requirements Document: ACCI EAF

* **Version:** 1.1 Draft
* **Date:** 2025-04-30
* **Author:** Coding Assistant (based on user input)
* **Status:** Draft

## 1. Introduction & Vision

This document describes the requirements for the **ACCI EAF** (Axians Competence Center Infrastructure Enterprise Application Framework). The ACCI EAF is an internal software framework serving as the foundation for developing robust, scalable, secure, multi-tenant, and maintainable enterprise applications developed by Axians and licensed to customers.

It combines modern technologies (TypeScript, NestJS) with proven architectural patterns (Hexagonal Architecture, CQRS/Event Sourcing, Multi-Tenancy via RLS, RBAC/ABAC) and best practices (Observability, Security by Design, Testability, i18n, SBOM) to increase development speed, improve code quality, ensure technical consistency, and establish the prerequisites for industry standards and certifications (e.g., ISO 27001, SOC2). **MikroORM** is used as the ORM (see ADR-001).

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
* **Ensuring Traceability:** Through Event Sourcing and preparation for a dedicated audit trail (Roadmap).
* **Enhancing Maintainability:** Clear separation of concerns, modularity, standardized processes (ADRs).
* **Enabling Flexibility:** Through a plugin system (with MikroORM Entity Discovery) and configurability (e.g., AuthN methods per tenant - Roadmap).
* **Supporting the Business Model:** Integrating a mechanism for license validation (hybrid: offline/online).
* **Supporting Compliance:** Providing features and documentation that aid in certification processes (ISO 27001, SOC2).

## 3. Target Audience

* **Primary:** Software development teams at Axians developing enterprise applications for customers.
* **Secondary:** Technical architects at Axians designing solutions and defining technical standards. Security & Compliance Officers at Axians.

## 4. Scope

The ACCI EAF will be developed in phases.

### 4.1. Version 1.0 (V1) - The Foundation

* **In Scope (V1):**
  * **Core Architecture:** Hexagonal Architecture, CQRS/ES base mechanisms, Plugin System basics.
  * **Multi-Tenancy:** Core functionality via Row-Level Security (RLS) with `tenant_id` column (implemented via MikroORM Filters - see ADR-002, ADR-006); Tenant Context discovery (Token/Header) and propagation (via `AsyncLocalStorage`).
  * **Control Plane API:** Separate API (in monorepo) for System Administrators to manage tenants (CRUD operations for basic tenant attributes).
  * **Standard Adapters:** PostgreSQL adapter (MikroORM for Event Store schema & Read Models - see ADR-001), Redis adapter (for Caching).
  * **Internationalization (i18n):** Basic integration of `nestjs-i18n` for API responses (errors, validation) and locale detection.
  * **License Validation:** Core mechanism to validate a provided license (hybrid: primarily offline-capable via file, optional online check). (Implementation details TBD, see Open Questions).
  * **Observability Basics:** Hooks/structure for structured logging; Health Check endpoints (`@nestjs/terminus`).
  * **Security Basics:** Standard Security Headers (`helmet`), Rate Limiting (`@nestjs/throttler`), Base Authentication (JWT validation, local password strategy), Base Authorization (RBAC Core logic and Enforcement Guards [recommendation: `casl` - see ADR-005], Basic Ownership Check for `ownerUserId`), OWASP Top 10 consideration in design.
  * **Reliability Basics:** Hooks for Graceful Shutdown.
  * **Testing Framework:** Defined strategy and tools (`suites` for Unit tests of DI components - see ADR-004, Testcontainers for Integration tests with MikroORM, `@nestjs/testing`+`supertest` for E2E tests).
  * **API Standards:** Recommendation and base configuration for using OpenAPI.
  * **SBOM Generation:** Standardized process for generating Software Bill of Materials (Tool/Format TBD - see ADR-TBD).
  * **Development Practices:** Monorepo setup (recommendation: Nx - see ADR-003), Use of Architecture Decision Records (ADRs), MikroORM Entity Discovery via glob patterns.
  * **Basic Documentation:** Setup guide, architecture overview, core concepts, ADRs.
  * **Project Template:** Basic template for creating new applications using the EAF.
  * **Deployment Focus:** Support for offline deployment via tarball (incl. Docker Images via `docker save`/`load`, `docker-compose.yml`, scripts) onto customer VMs.

### 4.2. Planned for Future Versions (Roadmap)

*(Replaces "Out of Scope (for V1)" for better clarity)*

* **Advanced Observability:** Metric export (Prometheus), Distributed Tracing (OpenTelemetry).
* **Advanced AuthN:** OIDC, LDAP/AD integration; Tenant-specific AuthN configuration.
* **Dedicated Audit Trail:** Separate service/module for immutable audit logs.
* **Advanced Reliability:** Retry mechanisms, Circuit Breaker patterns.
* **Advanced Configuration:** Dynamic Configuration, Feature Flag system.
* **Background Job System:** Integration with task queues (e.g., BullMQ).
* **User Groups:** Support for user groups in the RBAC/ABAC model.
* **Full ISO 27001 / SOC2 Support Tooling:** Dedicated compliance reports, fully automated control evidence.
* **CLI Enhancements:** Custom schematics for `nest g ...`.
* **Admin UIs:** Frontend for the Control Plane (`react-admin` planned), Frontend for tenant-specific RBAC administration.
* **Online License Server:** Operation and API definition of the Axians license server for optional online checks.
* **Advanced Plugin Features:** Detailed interaction model (Tenancy, RBAC, Licensing), more robust migration handling.
* **Event Schema Evolution:** Implementation of the upcasting pipeline (see ADR-010).
* **Idempotency Support:** Provision of a framework helper (Decorator/Service) for idempotent event handlers (see ADR-009).

## 5. Functional Requirements (FR) - V1

*(Note: Detailed breakdown follows in separate specification, thematic overview here with references to roadmap/ADRs)*

* **Framework Core (FR-CORE):** Monorepo structure, base libraries, CQRS buses, ES base, plugin loader (base).
* **Multi-Tenancy (FR-MT):** `tenant_id` discovery/context, RLS implementation via MikroORM Filters (ADR-002, ADR-006).
* **Control Plane API (FR-CP):** Separate API, Tenant CRUD (base attributes TBD), Admin AuthZ.
* **Persistence Adapters (FR-PA):** MikroORM setup for PG (ADR-001) (tenant-aware Entities via Discovery, RLS via Filters), Redis Cache Adapter.
* **Authentication (FR-AUTHN):** AuthN module, JWT strategy, Local strategy (secure), User-Tenant linkage. (Roadmap: OIDC/LDAP, config per tenant).
* **Authorization (FR-AUTHZ):** RBAC module (Rec.: `casl` - ADR-005), Data model (MikroORM Entities, tenant-aware), RBAC Guards, Ownership check (`ownerUserId`), Backend services/APIs for managing roles/permissions at the tenant level. (Roadmap: User Groups).
* **License Validation (FR-LIC):** Hybrid validation module/service (Offline file primary, optional Online check), checking base constraints (details TBD). Configuration for online mode. (Roadmap: Online license server).
* **Observability (FR-OBS):** Structured logging (hooks/interface), Health Check endpoints. (Roadmap: Metrics, Tracing).
* **Security (FR-SEC):** `helmet` integration, `throttler` integration. (Roadmap: Audit Trail).
* **Internationalization (FR-I18N):** `

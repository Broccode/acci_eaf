# ACCI EAF - Summary of Genesis and Decisions (Status: 2025-04-21)

This document summarizes the conversation flow and key decisions that led to the definition of ACCI EAF V1.

## 1. Initial Request & Technology Stack

* **Goal:** Creation of an Enterprise Application Framework (EAF).
* **Defined Technologies/Patterns:** TypeScript, NestJS, Testcontainers, PostgreSQL, Redis, CQRS/ES, Hexagonal Architecture, Plugin System.
* **Result:** First draft of an architecture and PRD integrating these elements, focusing on layer separation (Hexagonal), CQRS implementation in NestJS, and testability.

## 2. Inspiration & Initial Adjustments

* Discussion about external documents as inspiration (ADRs, detailed NFRs, OWASP, API standards).
* **Decision:** Adoption of best practices:
  * Use of Architecture Decision Records (ADRs).
  * More detailed, measurable Non-Functional Requirements (NFRs).
  * Explicit consideration of OWASP / Security Guidelines.
  * Recommendation to use OpenAPI for APIs.
* **Major Scope Change:** **Multi-Tenancy** becomes a core requirement for V1 (not optional). A **Control Plane API** for tenant management is also required.

## 3. Specification: Name, Multi-Tenancy, Monorepo

* **Project Name:** Defined as **ACCI EAF**.
* **Multi-Tenancy Implementation:**
  * Data Isolation: Decision for **Row-Level Security (RLS)** using a `tenant_id` column.
  * Tenant Context: Discovery from Token/Header, propagation via **`AsyncLocalStorage`**.
* **Control Plane API:** Confirmed as a separate application for Admin Tenant CRUD.
* **Project Structure:** Decision for a **Monorepo** approach to manage the EAF and products based on it (like DPCM). Recommendation for **Nx** as the management tool. Discussion about scalability and CI/CD workflows in the monorepo (confirming feasibility with Nx `affected` commands and caching).

## 4. Tooling Decisions: Testing & ORM

* **Unit Testing:** After clarifying misunderstandings, decision to adopt the **`suites` (`suites.dev`)** framework for simplifying unit tests of DI components in NestJS.
* **ORM (Intensive Discussion):**
  * Initial Recommendation: **Prisma** (due to type safety, DX, RLS middleware).
  * **Problem:** Central `schema.prisma` file conflicts with the requirement for easy **Plugin Entity Discovery** (preference for glob patterns). Prisma ruled out based on user preference.
  * Comparison **TypeORM vs. MikroORM:** Both support glob discovery.
    * TypeORM: Larger ecosystem, RLS implementation more cumbersome.
    * MikroORM: Smaller community, but "Filters" feature identified as a promising solution for RLS.
  * **Final Decision:** **MikroORM** chosen as it meets plugin requirements and offers a good RLS solution. Drizzle ORM was also discussed (re: "Impedance Mismatch") but rejected due to drawbacks in plugin discovery and RLS.
* **CLI:** **`nest-commander`** identified as a suitable tool for implementing CLI commands (like the `setup-admin` command).

## 5. Feature & Requirement Refinement

* **Authorization (RBAC/ABAC):**
  * Requirement for a **full RBAC system** for V1 confirmed.
  * Administration to occur at the **tenant level**.
  * Additional requirement for **ABAC (Ownership)** based on `ownerUserId`.
  * **V1 Scope:** RBAC Core logic + Enforcement Guards + Basic Ownership Check (`ownerUserId`). User Groups moved to roadmap. Recommendation for **`casl`** library.
* **Deployment:**
  * Clarification: Kubernetes rare for customers, VMs are common.
  * Adjustment: Focus on deployment using **Docker Compose** on VMs.
  * New Requirement: **Offline capability** (no internet on customer VM).
  * Final Strategy: Provision of an **Offline Tarball** (contains `docker save`-d images, Compose file, scripts), installation/update via `docker load` and scripts (incl. offline migrations).
* **Licensing:**
  * Clarification: Must be primarily offline-capable (signed file).
  * **Hybrid Approach:** Optional online checks against Axians server if connectivity allowed/configured. "Fail Open" principle recommended for network errors.
* **Internationalization (i18n):** Integration of **`nestjs-i18n`** for API texts/validation added as a requirement.
* **SBOM:** Requirement for automatic generation of **Software Bill of Materials** in the build process added (Tool/Format TBD per ADR).
* **Other "Enterprise Features":** Discussion and inclusion (as V1 basics or roadmap) of requirements/plans for enhanced Observability (Structured Logging, Health Checks V1; Metrics, Tracing later), Reliability (Graceful Shutdown V1; Retry/Circuit Breaker later), Security (Helmet, Rate Limiter V1; Audit Trail, advanced AuthN later), etc.

## 6. Documentation Strategy

* Discussion about **Arc42** as a structural template.
* **Decision:** Stick to the approach with multiple, specific documents (PRD, Architecture Overview, ADRs, Setup Guide, Concept Docs, etc.) rather than adopting a strict Arc42 structure for everything.
* Generation of final PRD and Architecture Overview documents.
* Generation of templates/outlines/drafts for ADRs and other relevant documents.

## 7. Final State & Open Questions

* The result is a detailed definition for **ACCI EAF V1** with a clear scope and a roadmap for future enhancements.
* Core technologies and architectural patterns are defined.
* Important implementation details and some technology choices remain open and need to be clarified and documented in **ADRs** as development progresses (see Section 10 in the final PRD). This particularly affects licensing details, RLS filter implementation, bootstrapping, plugin migrations, idempotency support, RBAC library choice, and SBOM tool choice.

This summary traces the path of our collaborative considerations and the decisions made, leading to the current definition of the ACCI EAF.

**Product Requirements Document (PRD): ACCI EAF (Axians Competence Center Infrastructure -
Enterprise Application Framework)**

**Version: 0.2.0** **Date:** June 4, 2025 **Author(s):** John (PM), Sarah (PO)

## Goal, Objective and Context

The ACCI Enterprise Application Framework (ACCI EAF) is a new, modern foundational platform designed
to replace the outdated DCA framework at Axians. Its **primary goal** is to deliver a stable,
secure, maintainable, and highly usable Enterprise Application Framework that accelerates the
development of high-quality, multi-tenant software products (e.g., DPCM, ZEWSSP, and a planned
combined VMWare/IBM Power management tool).

The current DCA framework suffers from manual ORM, JSON blob storage, lack of multi-tenancy, zero
tests, and difficult code sharing, leading to instability and a poor developer experience. ACCI EAF
aims to resolve these issues, enabling Axians to meet sophisticated customer requirements,
especially those demanding high security, compliance (ISO27001, SOC2, FIPS), and strong data
isolation.

**Key Objectives include:**

- Replacing DCA for new development and modernizing existing products where feasible.
- Implementing robust multi-tenancy and advanced IAM (RBAC/ABAC) as core features.
- Providing excellent Developer Experience (DevEx) through comprehensive tooling, SDKs, and
  documentation, supporting mandatory TDD, DDD, and Hexagonal Architecture.
- Ensuring compatibility with critical infrastructure (`ppc64le`, `corenode` integration) and "less
  cloud-native" customer environments.
- Supporting business needs like integrated License Management and compliance features.
- Establishing a modern architectural foundation based on CQRS/Event Sourcing and an Event-Driven
  Architecture.

The **vision** is for ACCI EAF to be the preferred platform that empowers Axians to efficiently
build innovative, enterprise-grade software, recognized for its stability, feature set, adherence to
best practices, and for making development at Axians a benchmark of excellence.

## Functional Requirements (MVP)

The MVP functional requirements are derived from the `MVP_SCOPE_OUTLINE.md` and encompass the
foundational elements needed to build and validate the EAF. This includes:

- **Foundational Engineering & DevEx Setup:** A configured monorepo (Nx favored) with CI/CD
  (linters, formatters, unit tests - primarily GitLab CI, with GitHub Actions as secondary), TDD
  setups for Kotlin/Spring and the chosen UI framework, and an initial Developer Portal ("The
  Launchpad") structure.
- **Core Eventing Backbone (NATS-based):** A runnable local NATS/JetStream setup, a basic EAF SDK
  for Kotlin/Spring enabling secure, at-least-once event publish/subscribe, patterns for idempotent
  consumers, and basic tenant isolation within NATS.
- **Core Event Store Foundation (Postgres-based ES):** A defined PostgreSQL schema for events, an
  EAF SDK (Kotlin/Spring) for atomic event appends (with optimistic concurrency), event stream
  retrieval, basic snapshot support, and strict tenant data isolation.
- **Kotlin/Spring CQRS/ES Application Framework:** Core SDK abstractions for DDD/CQRS/ES primitives,
  conventions and SDK support for Hexagonal Architecture (reinforced by CLI), basic patterns for
  consistency and error handling in CQRS/ES, and integration with other EAF MVP services.
- **Basic IAM & Multi-Tenancy:** Tenant provisioning (SuperAdmin via Control Plane MVP), streamlined
  single-tenant setup, local user authentication and management per tenant (TenantAdmin via Control
  Plane MVP & EAF SDK), basic RBAC for a pilot application, and core tenant context propagation.
- **Scaffolding CLI ("ACCI EAF CLI"):** Generation for new Kotlin/Spring Backend Services
  (Hexagonal, EAF SDKs, TDD), Core CQRS/ES Components, and basic Frontend Modules.
- **Developer Portal & Documentation ("The Launchpad"):** "Getting Started" guide, "Core Concepts"
  explanations, "MVP SDK & Component Reference," key "Tutorials & How-To Guides," "Examples & Ref
  App links," and Search/Feedback capabilities.
- **Enablement of a Pilot Service/Module:** Validation of the EAF MVP by building a small, real
  service demonstrating core EAF capabilities and principles.

## Non Functional Requirements (MVP)

The ACCI EAF MVP will adhere to the NFRs specified in the Project Brief, with a particular focus on
establishing the groundwork for:

- **Performance & Scalability:** Core services designed for responsiveness (<1-2s UI, <500ms core
  EAF services) under moderate load (dozens of users/tenant, 50-100 tenants initially). KPI
  benchmarking infrastructure will be established.
- **Reliability & Availability:** Core infrastructure (NATS, PostgreSQL) setup for robustness. SDKs
  will include error handling. Target 99.9% uptime for core services.
- **Security (Paramount):** Basic IAM with RBAC and tenant isolation will be functional. Secure
  defaults for SDKs and services. Data protection principles applied from the start.
- **Maintainability & Evolvability (High Priority):** Code quality enforced by CI. Adherence to DDD,
  Hexagonal Architecture, TDD is mandatory and supported by initial tooling/SDKs.
- **Usability / Developer Experience (DevEx - Paramount):** The Scaffolding CLI MVP and Developer
  Portal MVP are key to this. SDKs will be designed for ease of use.
- **Compatibility & Portability:** `ppc64le` considerations addressed in technology choices and CI.
  Deployment patterns for "less cloud-native" environments (Docker Compose) provided.
- **Operational Requirements:** Standardized Docker configurations for local development and basic
  deployment. Structured logging from core components.
- **Internationalization (i18n):** Frontend modules generated by CLI will include basic i18n setup.

## User Interaction and Design Goals

The primary user-facing component directly delivered by the ACCI EAF MVP (besides the Developer
Portal) will be the **"ACCI EAF Control Plane"** application.

- **Overall Vision & Experience:** The Control Plane MVP should be perceived as modern, clean,
  intuitive, and highly functional for its specific administrative tasks. While not a full-fledged
  product UI, it serves as an early example of the EAF's UI capabilities.
- **Key Interaction Paradigms:** Standard web application interactions (forms, tables, navigation)
  for managing tenants, users (within a tenant), and potentially basic license information.
- **Core Screens/Views (Conceptual for Control Plane MVP):**
  - Login Screen (SuperAdmin, TenantAdmin).
  - Dashboard (overview relevant to the logged-in admin type).
  - Tenant Management (SuperAdmin: create/list tenants, assign TenantAdmins).
  - User Management (TenantAdmin: create/list/manage local users for their tenant).
  - (Potentially) Basic License Overview.
- **Accessibility Aspirations:** Adherence to fundamental web accessibility standards (e.g.,
  keyboard navigation, sufficient contrast).
- **Branding Considerations:** Basic Axians branding.
- **Target Devices/Platforms:** Primarily web desktop for administrative tasks. Responsive design
  for usability on tablets is desirable.

This initial vision will guide the Design Architect in creating the UI/UX Specification for the
Control Plane MVP, leveraging the Vaadin/Hilla/React UI Foundation Kit.

## Technical Assumptions

1. **Repository & Service Architecture:** The ACCI EAF will utilize a **Monorepo approach**. The
   overall service architecture will be based on **CQRS (Command Query Responsibility Segregation)
   and Event Sourcing (ES)** principles, within an **Event-Driven Architecture (EDA)**. Backend
   services will primarily be Kotlin/Spring based.
   - _Rationale:_ The Monorepo (managed by tools like Nx) is chosen to facilitate code sharing,
     atomic commits across related components, and consistent tooling across the EAF and products
     built upon it. CQRS/ES and EDA are selected to build scalable, resilient, and maintainable
     systems that can handle complex business logic and evolving requirements, aligning with the
     long-term vision for the EAF.
2. **Core Technology Stack (Preliminary):**
   - Eventing Bus: NATS/JetStream.
   - Event Store Persistence: PostgreSQL.
   - Backend Language/Framework: Kotlin with Spring Boot 3 / Spring Framework 6.
   - CQRS/ES Implementation Aid: Axon Framework (open-source library) is under strong consideration.
   - UI Framework: Vaadin with Hilla (using React).
   - Monorepo Tooling: Nx is a favored candidate.
3. **Development Principles:** DDD, Hexagonal Architecture, and TDD are mandatory and will be
   enforced/supported by the EAF structure, SDKs, and tooling.
4. **Platform Compatibility:** Solutions must be compatible with `ppc64le` and deployable in "less
   cloud-native" environments using Docker and Ansible.
5. **Integration Points:** Key integrations include DPCM's `corenode` (gRPC, Node.js) and external
   Identity Providers (SAML, OIDC, LDAP/AD).

### Testing requirements

- **TDD is Mandatory:** All EAF components and applications built upon it must follow Test-Driven
  Design.
- **Unit Tests:** High unit test coverage (>80%) is targeted for EAF core logic. Kotlin/Spring
  services will use JUnit/MockK (or similar). Frontend modules will use appropriate JS/TS testing
  frameworks.
- **Integration Tests:** To verify interactions between components, especially Hexagonal adapters
  and core services (e.g., service-to-database, service-to-NATS).
- **End-to-End (E2E) Tests:** For critical user flows, particularly for the ACCI EAF Control Plane
  MVP and the pilot application.
- **Performance Tests:** KPI benchmarking for core NFRs will be established, with tests designed to
  be repeatable and versioned to prevent regressions.
- **Security Testing:** Vulnerability scans and adherence to security best practices will be part of
  the CI/CD pipeline.
- The EAF will provide testing utilities and framework guidance to support these requirements.

## [OPTIONAL: For Simplified PM-to-Development Workflow Only] Core Technical Decisions & Application Structure

Given the "Very Technical" workflow context chosen for this PRD, this section outlines foundational
technical decisions and the proposed application structure.

### Technology Stack Selections (MVP Focus)

- **Primary Backend Language/Framework:** Kotlin (latest stable) with Spring Boot 3 / Spring
  Framework 6.
  - _Rationale:_ Modern, concise, strong JVM ecosystem, good `ppc64le` support, excellent Spring
    integration.
- **Primary Frontend Language/Framework (for Control Plane & UI Kit):** Vaadin with Hilla (using
  React and TypeScript).
  - _Rationale:_ Chosen UI Framework. Simplifies client-server, type-safe integration with Kotlin
    backend.
- **Eventing Bus:** NATS (latest stable) with JetStream enabled.
  - _Rationale:_ Performance, simplicity, persistent/ordered delivery, multi-tenancy primitives.
- **Event Store (Persistence Backend):** PostgreSQL (latest stable LTS version).
  - _Rationale:_ Reliability, ACID, familiarity, `ppc64le` support, JSONB capabilities.
- **CQRS/ES Framework Aid (Backend):** Axon Framework (open-source Java/Kotlin library, latest
  stable version compatible with Spring Boot 3).
  - _Rationale:_ Provides proven abstractions for Aggregates, Command/Event handling, Sagas,
    reducing boilerplate for CQRS/ES patterns. To be used without Axon Server.
- **Monorepo Management Tool:** Nx (latest stable).
  - _Rationale:_ Robust support for multi-language projects (Java/Kotlin via Gradle plugin, JS/TS),
    build caching, affected commands, dependency graph visualization.
- **Build Tool (Backend):** Gradle (latest stable) with Kotlin DSL.
  - _Rationale:_ Flexible, powerful, good Kotlin support, integrates well with Nx via `@nx/gradle`
    plugin.
- **Containerization:** Docker.
- **Deployment Orchestration (for "less cloud-native"):** Ansible for EAF infrastructure management
  where applicable. Docker Compose for local development and simpler deployments.
- **IAM Integration Libraries:** Spring Security for core authentication/authorization; relevant
  libraries for SAML, OIDC, LDAP integration.
- **Testing Libraries (Backend):** JUnit 5, MockK (for Kotlin mocking), Spring Boot Test utilities,
  AssertJ, ArchUnit for architectural rule enforcement, Testcontainers for integration testing with
  NATS/Postgres.
- **Logging Framework (Backend):** SLF4J with Logback (default with Spring Boot), configured for
  structured JSON logging.
- **CLI Framework (for ACCI EAF CLI):** Picocli (Java/Kotlin).
- **Developer Portal Platform:** Docusaurus favored.

### Proposed Application Structure (Monorepo managed by Nx)

The monorepo will be structured to support multiple EAF core services, shared libraries, the Control
Plane application, and eventually, product applications.

```
acci-eaf-monorepo/
├── apps/                                # Deployable applications
│   ├── acci-eaf-control-plane/          # Vaadin/Hilla/React + Spring Boot application (Nx project)
│   │   ├── backend/                     # Spring Boot module (Gradle sub-project)
│   │   │   └── src/main/kotlin/com/axians/eaf/controlplane/...
│   │   ├── frontend/                    # React/Hilla frontend code (part of backend Gradle module)
│   │   │   └── src/main/frontend/...
│   │   └── build.gradle.kts
│   ├── iam-service/                     # EAF IAM Core Service (Kotlin/Spring, Nx project, Gradle sub-project)
│   │   └── src/main/kotlin/com/axians/eaf/iam/...
│   │   └── build.gradle.kts
│   ├── license-management-service/      # EAF License Service (Kotlin/Spring, Nx project, Gradle sub-project)
│   │   └── src/main/kotlin/com/axians/eaf/license/...
│   │   └── build.gradle.kts
│   ├── feature-flag-service/            # EAF Feature Flag Service (Kotlin/Spring, Nx project, Gradle sub-project)
│   │   └── src/main/kotlin/com/axians/eaf/featureflag/...
│   │   └── build.gradle.kts
│   └── # ... other EAF core service applications (e.g., Config Service MVP) ...
├── libs/                                # Sharable libraries
│   ├── eaf-core/                        # Common utilities, base classes for EAF (Kotlin, Nx project, Gradle sub-project)
│   │   └── src/main/kotlin/...
│   │   └── build.gradle.kts
│   ├── eaf-eventing-sdk/                # NATS/JetStream integration, event publishing/consuming helpers (Kotlin, Nx project, Gradle sub-project)
│   │   └── src/main/kotlin/...
│   │   └── build.gradle.kts
│   ├── eaf-eventsourcing-sdk/           # Helpers for CQRS/ES, EventStore interaction via PostgreSQL (Kotlin, Nx project, Gradle sub-project)
│   │   └── src/main/kotlin/...
│   │   └── build.gradle.kts
│   ├── eaf-iam-client/                  # Client library for IAM service (Kotlin, Nx project, Gradle sub-project)
│   │   └── src/main/kotlin/...
│   │   └── build.gradle.kts
│   ├── eaf-featureflag-client/          # Client library for Feature Flag service (Kotlin, Nx project, Gradle sub-project)
│   │   └── src/main/kotlin/...
│   │   └── build.gradle.kts
│   ├── eaf-license-client/              # Client library for License service (Kotlin, Nx project, Gradle sub-project)
│   │   └── src/main/kotlin/...
│   │   └── build.gradle.kts
│   ├── shared-domain-kernel/            # Optional: Shared pure domain primitives (Kotlin, Nx project, Gradle sub-project)
│   │   └── src/main/kotlin/com/axians/eaf/shared/kernel/...
│   │   └── build.gradle.kts
│   ├── ui-foundation-kit/               # UI Components (Vaadin/Hilla/React), Storybook (JS/TS, Nx project)
│   │   └── src/                            # Source for components, themes, etc.
│   │   └── storybook/                      # Storybook configuration and stories
│   │   └── package.json
│   └── # ... other shared libraries (e.g., common test utilities) ...
├── tools/
│   └── acci-eaf-cli/                    # ACCI EAF CLI (Kotlin/Picocli, Nx project, Gradle sub-project)
│       └── src/main/kotlin/com/axians/eaf/cli/...
│       └── build.gradle.kts
├── docs/                                # Developer Portal ("The Launchpad") source (Docusaurus)
│   └── package.json
├── infra/
│   ├── docker-compose/                  # Docker Compose files for local dev environments (NATS, Postgres, EAF services)
│   └── ansible/                         # Ansible playbooks for EAF infra setup (if any)
├── gradle/
│   └── wrapper/
├── build.gradle.kts                     # Root Gradle build file (common configs, plugin management for all Gradle projects)
├── settings.gradle.kts                  # Gradle multi-project definitions (includes all backend apps & libs)
├── gradlew
├── gradlew.bat
├── nx.json                              # Nx workspace configuration (plugins, targets defaults)
├── package.json                         # Root package.json for Nx, workspace tools (ESLint, Prettier for JS/TS)
└── tsconfig.base.json                   # Base TypeScript config for the workspace (for UI Kit, docs, etc.)
```

- **Key Modules/Components and Responsibilities (Illustrative for MVP):**
  - **`apps/acci-eaf-control-plane`**: Web application for EAF administration (IAM, tenants,
    licenses). Will use the `eaf-sdk` and `ui-foundation-kit`.
  - **`apps/iam-service`**: Core IAM service implementing RBAC/ABAC, user management, and
    federation.
  - **EAF SDK components (`libs/eaf-core`, `libs/eaf-eventing-sdk`, `libs/eaf-eventsourcing-sdk`,
    etc.)**: Individual Gradle sub-projects providing client SDKs for EAF services and core
    framework functionalities (eventing, event sourcing helpers, core utilities).
  - **`libs/ui-foundation-kit`**: The shared UI component library and Storybook environment
    (Vaadin/Hilla/React).
  - **`tools/acci-eaf-cli`**: The command-line interface for developers.
- **Data Flow Overview (Conceptual for a typical EAF-based application):**
  - UI (Vaadin/Hilla/React) -> `acci-eaf-control-plane` Backend (Kotlin/Spring) / Product App
    Backend
  - Backend Command Handler (Kotlin/Spring, using Axon) -> Event Sourcing SDK -> Event Store
    (Postgres)
  - Events published by Aggregates -> Eventing SDK -> Eventing Bus (NATS/JetStream)
  - Event Listeners/Projectors (Kotlin/Spring, using Axon) <- Eventing SDK <- Eventing Bus
    (NATS/JetStream)
  - Projectors -> Update Read Models (Postgres or other)
  - UI Query -> Backend Query Handler (Kotlin/Spring, using Axon) -> Read Models
  - All services interact with IAM Service (via SDK) for AuthN/AuthZ.
  - All services use Feature Flag Service (via SDK), Config Service (via SDK).

## Epic Overview

The following Epics, derived from the `MVP_SCOPE_OUTLINE.md`, define the core functional areas for
the ACCI EAF MVP. They now include foundational technical stories (prefixed TS) and refined
Acceptance Criteria based on the approved architecture.

---

**Epic 1: Foundational Engineering & DevEx Setup (MVP)**

- **Goal:** Establish the core development environment, CI/CD practices, and quality gates necessary
  for building ACCI EAF and its applications according to mandated principles.
- **User Stories:**
  1. **Story 1.1: Monorepo Initialization & Structure**
     - As a Developer (Michael), I want the ACCI EAF monorepo to be initialized using Nx, with
       clear, standardized project structures defined for Kotlin/Spring backend services (Gradle
       multi-modules) and the chosen Vaadin/Hilla/React UI framework, so that I can easily navigate,
       manage, and contribute code for different EAF modules and product components from day one.
     - **Acceptance Criteria:**
       1. Nx workspace is initialized and configured with `@nx/gradle` and relevant JS/TS plugins
          (e.g., for React).
       2. Root `build.gradle.kts` and `settings.gradle.kts` define common configurations (Kotlin,
          Spring Boot versions via catalog) and initial module structure (e.g., `apps`, `libs`,
          `tools`) for backend projects.
       3. Placeholders for initial EAF services (e.g., `iam-service`) and libraries (e.g.,
          `eaf-sdk`, `ui-foundation-kit`) exist as Gradle sub-projects (for backend) or Nx projects
          (for frontend libs/apps) within the Nx structure.
       4. Basic `project.json` files exist for these initial Nx projects, configured for relevant
          Gradle tasks or JS/TS build/serve/test commands.
       5. Documentation outlines the monorepo structure and how to add new projects/modules.
  2. **Story 1.2 (was PRD Story 1.1.A / TS-DEVEX-01): Configure Nx Monorepo with Gradle Projects &
     Basic CI Workflow**
     - As the EAF Development Team, I want the Nx Monorepo configured with initial Gradle project
       structures for backend services/libraries and a basic CI workflow (build, lint, format, unit
       test affected projects using GitLab CI primarily, and GitHub Actions as a secondary option),
       so that we have a consistent and automated foundation for all EAF development.
     - **Acceptance Criteria:**
       1. CI pipeline (e.g., GitLab CI, with GitHub Actions as an alternative) is defined and
          triggers on pushes/PRs to `main`.
       2. CI pipeline executes `nx affected:build --all` and `nx affected:test --all` (or equivalent
          commands covering both backend and frontend).
       3. ktlint for Kotlin formatting is integrated into the Gradle build and enforced by CI.
       4. ESLint/Prettier (or chosen UI framework equivalents) are configured for frontend code in
          `ui-foundation-kit` and `acci-eaf-control-plane/frontend` and enforced by CI.
       5. Build fails if linting, formatting, or tests fail for any affected project.
       6. Basic build artifacts (e.g., JARs for backend services, static assets for frontend if
          applicable as separate deployable) are produced and archived by CI for successful builds.
  3. **Story 1.3 (was PRD Story 1.1.B / TS-QUALITY-01): Implement Initial ArchUnit Test Suite for
     Core Architectural Rules**
     - As the EAF Development Team, I want an initial ArchUnit test suite integrated into the CI
       pipeline, with core rules for layer dependencies (Hexagonal) and naming conventions, so that
       architectural integrity is programmatically enforced.
     - **Acceptance Criteria:**
       1. ArchUnit dependency is added to the test configurations of relevant backend modules.
       2. An initial ArchUnit test suite is created (e.g., in a shared test utility library or
          within a core EAF module).
       3. Tests verify basic layer dependencies (e.g., domain packages do not depend on
          infrastructure packages like Spring framework).
       4. Tests verify core naming conventions for key architectural components (services, ports,
          adapters).
       5. ArchUnit tests are successfully executed as part of the CI pipeline for relevant backend
          modules.
  4. **Story 1.4 (was PRD Story 1.3): TDD Setup & Examples**
     - As a Developer (Lirika), I want a functional Test-Driven Design (TDD) setup readily available
       for both Kotlin/Spring (JUnit 5, MockK) and the Vaadin/Hilla/React UI framework, complete
       with integrated testing libraries and illustrative example tests for a simple
       component/service, so that I can easily practice and strictly adhere to TDD.
     - **Acceptance Criteria:**
       1. An example Kotlin/Spring service within an EAF module includes a unit test (using JUnit 5
          & MockK) written _before_ its minimal implementation, demonstrating the TDD
          red-green-refactor cycle.
       2. An example React component (within `ui-foundation-kit` or
          `acci-eaf-control-plane/frontend`) includes a unit test (using Vitest/Jest + React Testing
          Library) demonstrating TDD for a UI component.
       3. All necessary testing libraries are correctly configured in `build.gradle.kts` files (for
          backend) and relevant `package.json` files (for frontend).
       4. The "Launchpad" Developer Portal includes a concise guide on the TDD workflow expected for
          ACCI EAF development, referencing these examples.
  5. **Story 1.5 (was PRD Story 1.4): Developer Portal ("The Launchpad") Initial Setup**
     - As an EAF Contributor (Anita), I want the initial structure for the "Launchpad" Developer
       Portal (using Docusaurus) to be established, accessible via a local command, and integrated
       into the monorepo, so that foundational documentation ("Getting Started," architectural
       principles) can be immediately populated and version-controlled.
     - **Acceptance Criteria:**
       1. Docusaurus is set up within the `docs/` directory of the monorepo.
       2. Placeholders for "Getting Started Guide," "Architectural Principles (DDD, Hexagonal,
          CQRS/ES, TDD for EAF)," "Core Services Overview," and "UI Foundation Kit Intro" are
          created.
       3. A local development server can be started for the portal.
       4. The portal is included in the monorepo's version control and basic CI check (e.g., build
          portal).

---

**Epic 2: Core Eventing Backbone (NATS-based) - MVP**

- **Goal:** Provide a reliable and usable eventing system for EDA and CQRS/ES within ACCI EAF.
- **User Stories:**
  1. **Story 2.1 (was PRD Story 2.1 / TS-INFRA-01): Establish NATS/JetStream Core Infrastructure for
     MVP**
     - As an EAF System, I need the NATS/JetStream core infrastructure configured for local
       development (via Docker Compose) and initial MVP deployment considerations, including basic
       multi-account setup for tenant isolation, so that the eventing backbone is operational.
     - **Acceptance Criteria:**
       1. A documented `docker-compose.yml` file starts a NATS server with JetStream enabled and
          basic persistence for local development.
       2. The NATS server configuration within Docker Compose defines at least two distinct tenant
          NATS Accounts (e.g., `TenantA_Account`, `TenantB_Account`) and a system account, each with
          appropriately scoped user credentials and permissions for tenant-prefixed subjects and
          essential JetStream API access.
       3. JetStream is enabled for tenant accounts with basic file-based persistence configured in
          the local setup.
       4. "The Launchpad" contains documentation for running, connecting to (with example
          credentials for different tenant contexts), and basic troubleshooting of the local NATS
          environment.
  2. **Story 2.2 (was PRD Story 2.2 / TS-PATTERNS-01 - Conditional): Implement & Validate
     Transactional Outbox Pattern for NATS Event Publishing**
     - (If confirmed necessary by Architect for MVP reliability based on
       `Axon Framework in Hexagonal EAF` (section 4.a)) As the EAF Eventing SDK, I need to implement
       and validate the Transactional Outbox pattern for publishing events to NATS, so that
       at-least-once delivery and consistency with event store persistence are robustly ensured.
     - **Acceptance Criteria:**
       1. An "outbox" table schema is defined for PostgreSQL.
       2. The EAF Event Sourcing SDK (or a dedicated component) integrates outbox message writes
          into the same database transaction as domain event persistence.
       3. A separate, reliable poller mechanism (e.g., a Spring Boot scheduled task or a dedicated
          process) reads from the outbox table and publishes messages to NATS via the EAF Eventing
          SDK.
       4. The poller handles transient NATS publishing errors with retries and logs persistent
          failures.
       5. Successfully published messages are marked as processed or deleted from the outbox.
       6. Idempotency considerations for message delivery by the poller are addressed.
  3. **Story 2.3 (was PRD Story 2.3): EAF NATS SDK - Event Publishing**
     - As a Backend Developer (Majlinda), I want to use the ACCI EAF SDK (Kotlin/Spring) to easily
       and reliably publish domain events (as JSON payloads) from my service to a NATS JetStream
       stream with an at-least-once delivery guarantee, ensuring that critical business events are
       captured and broadcast.
     - **Acceptance Criteria:**
       1. The `eaf-eventing-sdk` module provides a `NatsEventPublisher` service (or equivalent
          abstraction).
       2. The publisher service connects to NATS using centrally managed EAF application
          configuration and correctly uses tenant-specific credentials or NATS context.
       3. The publisher service accepts a Kotlin domain event object, serializes it to a JSON string
          as per EAF-defined conventions (e.g., including event type, timestamp, payload).
       4. The SDK utilizes JetStream `publish()` method and correctly handles the returned
          `PublishAck` to ensure at-least-once delivery semantics, including configurable retry
          mechanisms for transient publish failures detectable via `PublishAck`.
       5. All published events MUST include `tenant_id` (e.g., in NATS subject or message
          metadata/headers), propagated correctly by the SDK, aligning with the NATS multi-tenancy
          architecture defined by Fred.
       6. (Conditional) If Story 2.2 (Transactional Outbox) is implemented, this SDK publisher
          integrates with it seamlessly.
       7. Comprehensive unit and integration tests (using Testcontainers for NATS) demonstrate
          successful publishing, `PublishAck` handling, tenant context usage, and error scenarios.
  4. **Story 2.4 (was PRD Story 2.4): EAF NATS SDK - Event Consumption & Idempotency**
     - As a Backend Developer (Michael), I want to use the ACCI EAF SDK (Kotlin/Spring) to easily
       subscribe to and consume events in their correct order from a NATS JetStream stream,
       leveraging clear EAF-provided patterns and guidance for implementing idempotent event
       consumers, so that I can build reliable projectors and sagas.
     - **Acceptance Criteria:**
       1. The `eaf-eventing-sdk` provides convenient abstractions (e.g., annotations like
          `@NatsJetStreamListener` on methods, or a configurable listener container factory) for
          consuming messages from a durable JetStream consumer.
       2. The SDK handles JSON deserialization of event payloads from NATS messages to specified
          Kotlin data classes.
       3. The SDK ensures ordered processing of events as delivered by a single JetStream consumer
          instance (respecting JetStream's ordering guarantees).
       4. The EAF NATS SDK consumer MUST provide clear, easy-to-use mechanisms for message
          acknowledgment (`msg.ack()`), negative acknowledgment (`msg.nak()` potentially with a
          configurable delay/backoff strategy), and termination (`msg.term()`) based on the outcome
          of the event processing logic in the listener method.
       5. The EAF NATS SDK MUST allow consumers to be configured with durable names and to bind to
          server-defined JetStream consumer configurations, including specific Delivery Policies
          (e.g., `DeliverAllPolicy`, `DeliverNewPolicy`) and Ack Policies (`AckExplicitPolicy`
          should be the default for reliable processing).
       6. "The Launchpad" Developer Portal documents robust patterns and provides examples for
          writing idempotent event consumer logic using the SDK.
       7. The SDK respects tenant context for subscribing (e.g., subscribes to tenant-specific
          subjects or uses tenant-scoped credentials).
       8. Unit and integration tests (using Testcontainers for NATS) cover event consumption,
          various acknowledgment flows (ack/nak/term), and error handling by the consumer framework.

---

**Epic 2.3: Core Event Store Foundation (Postgres-based ES) - MVP**

- **Goal:** Establish a reliable and secure persistence mechanism for event-sourced aggregates.
- **User Stories:**
  1. **Story 2.3.1 (was PRD Story 2.3.1 / TS-INFRA-02): Implement EAF Event Store SDK - Core
     Persistence Logic for PostgreSQL**
     - As the EAF Development Team, I want to implement the core EAF Event Store SDK (Kotlin/Spring)
       providing logic for atomic event appends (with optimistic concurrency), event stream
       retrieval, basic snapshotting, and tenant data isolation against a PostgreSQL database, so
       that event sourcing capabilities are available to EAF services.
     - **Acceptance Criteria:**
       1. A detailed PostgreSQL schema for the event store (e.g., table `domain_events` with
          columns: `global_sequence_id BIGSERIAL PRIMARY KEY`, `event_id UUID NOT NULL UNIQUE`,
          `stream_id VARCHAR(255) NOT NULL` (e.g., `aggregateType-aggregateId`),
          `aggregate_id VARCHAR(255) NOT NULL`, `aggregate_type VARCHAR(255) NOT NULL`,
          `expected_version BIGINT`, `sequence_number BIGINT NOT NULL`,
          `tenant_id VARCHAR(255) NOT NULL`, `event_type VARCHAR(255) NOT NULL`,
          `payload JSONB NOT NULL`, `metadata JSONB`,
          `timestamp_utc TIMESTAMPTZ NOT NULL DEFAULT (now() AT TIME ZONE 'utc')`,
          `UNIQUE (tenant_id, aggregate_id, sequence_number)`,
          `INDEX idx_stream_id_seq (stream_id, sequence_number)`,
          `INDEX idx_tenant_event_type (tenant_id, event_type)`) is defined, documented, and
          version-controlled (e.g., using Flyway or Liquibase scripts).
       2. A separate table for snapshots (e.g., `aggregate_snapshots`) is similarly defined
          (`aggregate_id`, `tenant_id`, `last_sequence_number`, `snapshot_payload_jsonb`, `version`,
          `timestamp_utc`).
       3. The `eaf-eventsourcing-sdk` module (Kotlin/Spring) provides services/repositories for:
          - Atomically appending one or more domain events for a specific aggregate instance. This
            operation must include a check for optimistic concurrency against the provided
            `expected_version` (last known sequence number for the aggregate) and the current
            `sequence_number` for that `aggregate_id` and `tenant_id` in the database. The new
            events are assigned sequential `sequence_number`s.
          - Efficiently retrieving the complete and correctly ordered event stream (by
            `sequence_number`) for a given `aggregate_id` and `tenant_id`.
          - Storing and retrieving the latest aggregate snapshot for a given `aggregate_id` and
            `tenant_id`.
       4. All SDK database operations strictly enforce `tenant_id` isolation through WHERE clauses
          on `tenant_id` and by ensuring `tenant_id` is part of composite keys or unique constraints
          where appropriate.
       5. Appropriate indexing strategies are implemented on the event store tables to support
          efficient querying by `stream_id` (for rehydration), `tenant_id`, and potentially
          `event_type` or `timestamp_utc` for specific use cases.
       6. Comprehensive unit and integration tests (using Testcontainers for PostgreSQL) validate
          all SDK functionalities, including atomic appends, optimistic concurrency conflict
          scenarios, stream retrieval, snapshot operations, and strict tenant isolation.

---

**Epic 2.4: Kotlin/Spring CQRS/ES Application Framework - MVP**

- **Goal:** Provide developers with the necessary SDKs, base classes, annotations, and patterns to
  build event-sourced services in Kotlin/Spring, adhering to DDD and Hexagonal Architecture.
- **User Stories:**
  1. **Story 2.4.1: SDK Abstractions for Event-Sourced Aggregates**
     - As a Backend Developer (Majlinda), I want to use ACCI EAF's Kotlin/Spring SDK to easily
       define and implement event-sourced Aggregates that seamlessly integrate with the EAF's Event
       Store SDK and Eventing Bus SDK, so that I can focus on my core domain logic rather than
       complex CQRS/ES plumbing.
     - **Acceptance Criteria:**
       1. The `eaf-eventsourcing-sdk` (potentially leveraging parts of Axon Framework open-source
          library if decided, as per `Axon Framework in Hexagonal EAF`) provides base classes,
          interfaces, or annotations (e.g., `@EafAggregate`, `@EafCommandHandler`,
          `@EafEventSourcingHandler`) to simplify the definition of Aggregates in Kotlin.
       2. The SDK facilitates common Aggregate operations: loading from history (events + optional
          snapshot) via the EAF Event Store SDK, applying new events, and having new events
          persisted by the EAF Event Store SDK and published via the EAF Eventing SDK (potentially
          using Unit of Work concepts for consistency).
       3. Aggregates developed using the EAF SDK MUST adhere to Hexagonal Architecture principles,
          ensuring domain logic within the aggregate is isolated from direct infrastructure concerns
          (persistence and event publishing are handled via SDK-provided ports/mechanisms).
       4. Aggregates MUST implement optimistic concurrency control using sequence numbers, with
          conflicts handled and surfaced by the EAF Event Store SDK interactions.
       5. All state changes within aggregates MUST be derived exclusively from domain events applied
          via EAF SDK-provided mechanisms (e.g., methods analogous to `@EventSourcingHandler`).
       6. "The Launchpad" provides clear examples and documentation for defining and testing EAF
          Aggregates using these SDK abstractions.
  2. **Story 2.4.2: SDK Support for Hexagonal Architecture in Services**
     - As a Backend Developer (Michael), I want the ACCI EAF Kotlin/Spring SDK and Scaffolding CLI
       outputs to provide clear structures and base classes/interfaces that naturally guide me in
       building services according to Hexagonal Architecture, ensuring my application and domain
       logic remain well-isolated from infrastructure concerns.
     - **Acceptance Criteria:**
       1. The `eaf-core` SDK module provides interfaces or abstract classes for defining Ports
          (e.g., `InboundPort<C, R>` for command processing, `OutboundPort` for infrastructure
          interactions).
       2. The Scaffolding CLI (`acci-eaf-cli`) when generating new Kotlin/Spring services (Story
          2.6.1) creates a directory structure reflecting Hexagonal layers (e.g., `domain`,
          `application/port/in`, `application/port/out`, `infrastructure/adapter/in/web`,
          `infrastructure/adapter/out/persistence`, `infrastructure/adapter/out/messaging`).
       3. "The Launchpad" documents how to implement Adapters for common infrastructure concerns
          (e.g., REST controllers as inbound adapters using Spring WebMVC/WebFlux, NATS consumers as
          inbound adapters using `eaf-eventing-sdk`, PostgreSQL repositories as outbound adapters
          using Spring Data JPA or JDBC with EAF helpers).
  3. **Story 2.4.3: SDK Support for Idempotent Projectors**
     - As a Backend Developer (Majlinda), I want the ACCI EAF Kotlin/Spring SDK to offer
       straightforward patterns and helper utilities for building idempotent Projectors that consume
       domain events from NATS (via the EAF Eventing SDK) and reliably update read models (e.g., in
       PostgreSQL), so I can efficiently create queryable views of data.
     - **Acceptance Criteria:**
       1. The `eaf-eventing-sdk` or `eaf-eventsourcing-sdk` provides clear patterns or abstractions
          (e.g., base classes, annotations like `@EafProjectorEventHandler`) for creating Projector
          components as Spring beans that listen to specific domain events.
       2. Projectors MUST consume events via the EAF Eventing SDK and rigorously handle potential
          event redeliveries to ensure idempotency in read model updates (e.g., by checking if an
          event ID has already been processed or by using UPSERT database operations on the read
          model, potentially with a `processed_event_id` tracking mechanism).
       3. Projectors, when updating read models (e.g., in PostgreSQL), MUST strictly enforce tenant
          data isolation (e.g., by incorporating `tenant_id` in all read model queries and updates,
          derived from event metadata).
       4. "The Launchpad" provides documented examples of building Projectors, including strategies
          for managing read model schemas, handling errors during projection, and dealing with
          eventual consistency.
  4. **Story 2.4.4: Secure EAF Context Access for Services**
     - As a Backend Developer (Michael, Majlinda), I want Kotlin/Spring services built using the
       ACCI EAF application framework to have a simple and secure way to access essential
       operational context from other EAF services (like tenant/user from IAM, configuration,
       feature flags, license entitlements) via the EAF SDK.
     - **Acceptance Criteria:**
       1. The `eaf-core` SDK module or relevant client SDK modules (e.g., `eaf-iam-client`) provide
          mechanisms (e.g., injectable services, request-scoped beans managed by Spring, utility
          classes for context propagation) for EAF services to securely obtain the current
          operational context.
       2. This context includes at least the validated `tenant_id`, `user_id` (if an authenticated
          user session exists), and associated roles/permissions from the IAM service.
       3. The SDK provides helpers for accessing feature flag evaluations (via
          `eaf-featureflag-client`) and application configurations.
       4. Access to this context is consistently available and propagated correctly across
          asynchronous operations within a request or event processing flow (e.g., within Kotlin
          coroutines using `CoroutineContext` elements, or via message metadata for event handlers).

---

**Epic 2.5: Basic IAM & Multi-Tenancy - MVP**

- **Goal:** Establish core multi-tenancy structure, tenant self-management of local users, basic
  RBAC for a pilot application, and a streamlined single-tenant setup option.
- **User Stories:**
  1. **Story 2.5.1: SuperAdmin Tenant Provisioning via Control Plane MVP**
     - As an ACCI EAF Super Administrator, I want to use the Control Plane MVP to provision a new,
       isolated tenant and create its initial Tenant Administrator account, so that new tenants can
       be onboarded.
     - **Acceptance Criteria:**
       1. The Control Plane UI (designed by Jane) allows an authenticated SuperAdmin to input new
          Tenant Name and initial Tenant Admin email.
       2. The Control Plane Backend API securely calls the IAM Service to create the tenant entity
          and the initial Tenant Admin user entity.
       3. The IAM Service successfully creates these records in its PostgreSQL store, ensuring
          `tenant_id` uniqueness and data isolation for the new tenant.
       4. (Post-MVP integration or as part of a separate technical story) The corresponding NATS
          Account for the new tenant is provisioned dynamically based on the IAM Service action
          (e.g., via an event and a NATS provisioning service, as detailed in
          `Dynamic NATS Multi-Tenancy Provisioning`).
       5. The newly created initial Tenant Admin receives necessary credentials or an invitation
          link to set up their password and access their tenant within the Control Plane.
  2. **Story 2.5.2: Streamlined Single-Tenant EAF Setup**
     - As an Axians Operator deploying ACCI EAF for a customer who initially only needs a
       single-tenant setup, I want a simple, streamlined process (e.g., via deployment scripts or a
       Control Plane first-run wizard accessible to a bootstrap admin) to initialize the EAF with a
       default Superadmin account, a default tenant, and the Superadmin assigned as the Tenant
       Administrator for that tenant, so the system is quickly usable.
     - **Acceptance Criteria:**
       1. A documented script (e.g., shell script, Ansible playbook snippet) or a first-run UI
          wizard in the Control Plane is available.
       2. Upon execution/completion, a default SuperAdmin account is created with secure initial
          credentials.
       3. A default Tenant (e.g., "DefaultTenant") is created in the IAM service.
       4. The created SuperAdmin account is also configured as the initial Tenant Administrator for
          this default tenant.
  3. **Story 2.5.3: TenantAdmin Local User Management via Control Plane MVP**
     - As a Tenant Administrator, I want to use the Control Plane MVP to create and manage local
       users (e.g., set initial passwords via invite/reset flow, activate/deactivate accounts)
       exclusively for my tenant, so I can control user access to applications within my tenant.
     - **Acceptance Criteria:**
       1. A TenantAdmin, when logged into the Control Plane, can view a list of local users
          belonging only to their own tenant.
       2. The UI allows the TenantAdmin to create new local users by providing necessary details
          (e.g., email, name).
       3. The TenantAdmin can trigger actions like "send password reset/invitation" for users.
       4. The TenantAdmin can activate or deactivate user accounts within their tenant.
       5. The IAM Service backend APIs enforce that TenantAdmins can only perform these user
          management actions within their assigned tenant_id scope.
  4. **Story 2.5.4: EAF IAM SDK for Basic Local User Authentication & RBAC**
     - As a Backend Developer (Majlinda), I want to use the ACCI EAF IAM SDK to easily authenticate
       local users (e.g., via username/password or an EAF-issued token) against their specific
       tenant and to enforce basic Role-Based Access Control (based on a few pre-defined application
       roles like "app_user", "app_admin" manageable via Control Plane MVP) for these users, so that
       I can protect application resources within an EAF-based application.
     - **Acceptance Criteria:**
       1. The `eaf-iam-client` SDK provides a clear mechanism for EAF services (acting as OAuth2
          resource servers or using Spring Security) to validate user credentials (e.g., an
          EAF-issued JWT) against the IAM service, ensuring the validation is scoped to a specific
          `tenant_id` present in the request or token.
       2. The SDK provides a simple API (e.g., annotations like
          `@PreAuthorize("hasRole('tenant_app_admin')")` or utility methods like
          `iamContext.hasRole("app_user")`) for checking if an authenticated user possesses specific
          roles.
       3. The IAM service allows SuperAdmins or TenantAdmins (via Control Plane MVP) to define basic
          application-level roles (e.g., "PILOT_APP_USER", "PILOT_APP_ADMIN") and assign these roles
          to users within a tenant.
       4. A protected resource within the pilot application (Epic 2.8) successfully demonstrates
          authentication and RBAC enforcement using the EAF IAM SDK.
  5. **Story 2.5.5 (was PRD Story 2.5.5): Reliable EAF Context Propagation**
     - As an ACCI EAF service/component, I need reliable access to the current `tenant_id` and basic
       user context (like `user_id`, roles) from the IAM system (e.g., via validated tokens or an
       SDK utility), so I can enforce tenant data isolation and make context-aware decisions.
     - **Acceptance Criteria:**
       1. The EAF IAM SDK (e.g., `eaf-iam-client` integrated with Spring Security) MUST provide a
          secure and clearly defined method (e.g., through Spring Security's
          `SecurityContextHolder`, custom `Authentication` object, or request-scoped beans populated
          via a servlet filter/interceptor that validates an incoming EAF JWT) for EAF services to
          reliably obtain the current, authenticated `tenant_id`, `user_id`, and associated roles
          for the request.
       2. This security context, especially the `tenant_id`, MUST be consistently and securely
          propagated by other core EAF SDKs (e.g., the eventing SDK automatically adding `tenant_id`
          to NATS message metadata, the event sourcing SDK ensuring `tenant_id` is recorded with
          persisted events and used in queries) to enable robust multi-tenancy enforcement at the
          data and messaging layers.
       3. Mechanisms for context propagation must function reliably across asynchronous operations
          within a service (e.g., when using Kotlin coroutines, context must be passed to new
          coroutines; when handling events from NATS, the `tenant_id` from message metadata must be
          correctly established in the processing context).

---

**Epic 2.6: Scaffolding CLI ("ACCI EAF CLI") - MVP**

- **Goal:** Accelerate developer setup, promote consistency, and support mandated architectural
  patterns.
- **User Stories:**
  1. **Story 2.6.1: Generate Kotlin/Spring Backend Service via CLI**
     - As a Developer (Michael, Majlinda), I want to use the ACCI EAF CLI to quickly generate a new,
       fully structured Kotlin/Spring backend service module within the Nx monorepo, complete with a
       Hexagonal Architecture layout, TDD setup (JUnit 5, MockK, ArchUnit stubs), integrated core
       EAF SDKs (for logging, context, basic eventing/ES if applicable), default ktlint formatting
       configuration, and boilerplate for local NATS/PostgreSQL connection (via Docker Compose
       references), so I can bypass manual boilerplate and immediately start developing
       domain-specific logic according to EAF best practices.
     - **Acceptance Criteria:**
       1. CLI command `eaf generate service <service-name>` creates a new Gradle sub-project within
          the `apps/` or `libs/` directory of the Nx monorepo, as specified by user or EAF
          convention.
       2. The generated service includes a `build.gradle.kts` with necessary Kotlin, Spring Boot
          (version aligned with EAF), core EAF SDK dependencies (e.g., `eaf-core`,
          `eaf-eventing-sdk`, `eaf-eventsourcing-sdk`), and testing dependencies (JUnit 5, MockK,
          ArchUnit).
       3. The generated directory structure includes `src/main/kotlin` and `src/test/kotlin` with
          placeholders for `domain/model`, `domain/port`, `application/service`,
          `application/port/in`, `application/port/out`, `infrastructure/adapter/in/web` (with a
          sample Hilla endpoint or Spring Controller), `infrastructure/adapter/out/persistence`
          (with a sample repository port implementation).
       4. A basic TDD setup is present: `src/test/kotlin` directory with example unit tests for a
          sample application service and an ArchUnit test class placeholder with example rules.
       5. A main application class for Spring Boot (e.g., `@SpringBootApplication`) is generated.
       6. ktlint configuration is included and applied in the `build.gradle.kts`.
       7. Example `application.yml` or `.properties` files include placeholders and comments for
          connecting to local NATS and PostgreSQL instances (referencing connection details for the
          EAF standard Docker Compose setup).
       8. The new module is correctly registered in the root `settings.gradle.kts` and its
          `project.json` for Nx is generated with basic build/serve/test targets.
  2. **Story 2.6.2: Generate Core CQRS/ES Components within a Service via CLI**
     - As a Backend Developer (Majlinda), I want to use the ACCI EAF CLI to generate boilerplate
       code for common CQRS/ES components (specifically Aggregates, Commands, Domain Events, and
       basic Projector stubs) within an existing EAF Kotlin/Spring service, pre-wired with EAF SDK
       patterns and annotations, so that I can rapidly extend my domain model with consistent,
       EAF-compliant code.
     - **Acceptance Criteria:**
       1. CLI command `eaf generate aggregate <AggregateName> --service=<service-name>` creates:
          - A Kotlin data class for a creation Command (e.g., `Create<AggregateName>Command.kt` in
            an `application/port/in` or `domain/command` package).
          - A Kotlin data class for a creation Event (e.g., `<AggregateName>CreatedEvent.kt` in a
            `domain/event` package).
          - A Kotlin class for the Aggregate root (e.g., `<AggregateName>.kt` in `domain/model`)
            annotated with `@EafAggregate` (or equivalent), including an `@AggregateIdentifier`
            field, a constructor annotated as a command handler for the creation command which
            applies the creation event, and an event sourcing handler method for the creation event.
          - A basic unit test stub (e.g., `<AggregateName>Test.kt`) for the Aggregate using EAF's
            TDD setup.
       2. CLI command `eaf generate command <CommandName> --aggregate=<AggregateName>` adds a new
          Kotlin data class for the command and a placeholder command handler method (annotated
          appropriately) within the specified Aggregate class.
       3. CLI command `eaf generate event <EventName> --aggregate=<AggregateName>` adds a new Kotlin
          data class for the event and a placeholder event sourcing handler method within the
          specified Aggregate class.
       4. CLI command
          `eaf generate projector <ProjectorName> --service=<service-name> --event=<EventName>`
          creates a Kotlin class stub (e.g., `<ProjectorName>.kt` in
          `infrastructure/adapter/in/messaging` or `application/projection`) for a Projector,
          including an event handler method (e.g., using `@NatsJetStreamListener` or equivalent EAF
          SDK annotation) for the specified event type.
       5. Generated components are placed in appropriate packages according to the service's
          Hexagonal Architecture and DDD conventions.
       6. All generated Kotlin code adheres to EAF coding standards and passes ktlint formatting.
  3. **Story 2.6.3: Basic Frontend Module Generation via CLI for Vaadin/Hilla/React**
     - As a Frontend Developer (Lirika), I want to use the ACCI EAF CLI to generate a new frontend
       module structure (for Vaadin/Hilla with React) within an EAF application (e.g., as part of
       the Control Plane or a new product's Hilla application), pre-configured with standard project
       structure, basic i18n setup (e.g., `i18next` with example JSON files), a Storybook setup,
       linting/formatting tools (ESLint/Prettier), and a boilerplate component/view, so I can start
       building UI features quickly and consistently using the UI Foundation Kit.
     - **Acceptance Criteria:**
       1. CLI command `eaf generate frontend-view <ViewName> --app=<hilla-app-name>` (or similar)
          creates a new `.tsx` view file within the specified Hilla application's
          `src/main/frontend/views/` directory.
       2. The generated view includes boilerplate for a React functional component, imports from
          `@vaadin/react-components` and Hilla utilities, and a basic Hilla route configuration
          (`export const config: ViewConfig = {...};`).
       3. CLI command `eaf generate frontend-component <ComponentName> --lib=ui-foundation-kit` (or
          within an app) creates a new `.tsx` component file and a corresponding `.stories.tsx`
          file.
       4. If generating for a new Hilla application for the first time, the CLI ensures the
          `src/main/frontend` directory is correctly structured with `package.json` (including
          dependencies for React, Vaadin components, Hilla, i18next, Storybook), `tsconfig.json`,
          `vite.config.ts`, ESLint/Prettier configs, and basic i18n setup (e.g., `i18n.ts`
          configuration file, `locales/en/translation.json`, `locales/de/translation.json`).
       5. Generated Storybook setup includes configuration to find stories for both UI Foundation
          Kit components and application-specific components.

---

**Epic 2.7: Developer Portal & Documentation - MVP ("The Launchpad" concept)**

- **Goal:** Provide essential learning and reference materials to enable developers to effectively
  use the ACCI EAF MVP.
- **User Stories:**
  1. **Story 2.7.1: "Getting Started" Guide on Launchpad**
     - As a New Developer (Lirika), I want a comprehensive "Getting Started" guide on "The
       Launchpad" Developer Portal that rapidly walks me through setting up my local development
       environment (including NATS, PostgreSQL via Docker Compose, IDE configuration for
       Kotlin/Spring and React/TS) and building/running a simple "hello world" style application
       using the ACCI EAF CLI and core MVP SDKs, so I can quickly become productive and understand
       the basic EAF workflow.
     - **Acceptance Criteria:**
       1. "The Launchpad" (Docusaurus site) hosts a "Getting Started" guide as a primary entry
          point.
       2. The guide includes prerequisites and setup instructions for Kotlin/JDK, Spring Boot,
          Node.js, Docker, Nx, and recommended IDEs (IntelliJ IDEA for backend, VS Code for
          frontend).
       3. Provides step-by-step instructions to use `acci-eaf-cli` to generate a new EAF service and
          a simple frontend view that interacts with it.
       4. Explains how to run the EAF services (including local NATS/PostgreSQL via the EAF-provided
          Docker Compose setup) and the generated application locally from the IDE or command line
          (using Gradle/Nx).
       5. Includes a minimal "hello world" example demonstrating a frontend Vaadin/Hilla/React view
          calling a Hilla endpoint on the Kotlin/Spring backend.
  2. **Story 2.7.2: Core Concepts Documentation on Launchpad**
     - As a Developer (Michael, Majlinda), I want to access well-structured documentation with clear
       explanations and Kotlin/React code examples on "The Launchpad" for core EAF architectural
       concepts (EAF Overview, CQRS/ES principles as applied in EAF, DDD tactical patterns relevant
       to EAF services, Hexagonal Architecture implementation guidelines for EAF, TDD philosophy and
       practices for EAF) and for the APIs/usage of the MVP SDKs (Eventing SDK for NATS, Event
       Sourcing SDK for PostgreSQL, IAM client SDK), so I can effectively build services according
       to EAF best practices.
     - **Acceptance Criteria:**
       1. "The Launchpad" contains dedicated, easily navigable sections for "Core Architectural
          Concepts" and "EAF SDK Reference."
       2. The "Core Architectural Concepts" section clearly explains how DDD (Aggregates, Events,
          Commands), Hexagonal Architecture (Ports, Adapters for common concerns like REST, NATS,
          DB), CQRS/ES, and TDD are expected to be implemented within the ACCI EAF context, with
          illustrative (conceptual) diagrams and code snippets.
       3. The "EAF SDK Reference" provides API documentation (e.g., KDoc generated and presented, or
          manually written guides with code examples) for key public classes, methods, and
          configuration options in the `eaf-eventing-sdk`, `eaf-eventsourcing-sdk`, and
          `eaf-iam-client-sdk`.
       4. Code examples are provided in Kotlin for backend SDK usage and TypeScript/React for
          frontend interactions with Hilla, showing practical application of the SDKs.
  3. **Story 2.7.3: UI Foundation Kit & Storybook Access via Launchpad**
     - As a Frontend Developer (Lirika), I want "The Launchpad" to provide easy access to an
       interactive UI component library (Storybook instance) for the ACCI EAF UI Foundation Kit MVP,
       showcasing available Vaadin and custom React components, their props, variations, and usage
       examples (including Hilla integration), so I can easily discover and reuse standardized UI
       components in my applications.
     - **Acceptance Criteria:**
       1. "The Launchpad" has a clear section or prominent link pointing to the deployed Storybook
          instance for the ACCI EAF UI Foundation Kit.
       2. The Storybook instance includes entries for the initial set of core generic components
          (Buttons, Inputs, Layouts, Tables, Modals, etc.) from the UI Foundation Kit, as defined by
          the Design Architect.
       3. Each Storybook entry provides interactive controls to manipulate component props, clear
          code snippets for usage in React/Hilla, and notes on theming and accessibility.
  4. **Story 2.7.4: Search & Feedback Mechanism on Launchpad**
     - As any User of "The Launchpad" (Anita), I want robust search functionality covering all
       documentation content (guides, API refs, concept explanations) and a simple, clearly visible
       mechanism to provide feedback or ask questions about the documentation (e.g., a link to a
       dedicated feedback channel or issue tracker), so I can efficiently find information and
       contribute to improving the EAF resources.
     - **Acceptance Criteria:**
       1. The Docusaurus search functionality is enabled, configured, and effectively indexes all
          portal content.
       2. A clear "Feedback" or "Report Documentation Issue" link/button is present on all pages or
          in a consistent location (e.g., site footer or navigation bar). This link directs users to
          the appropriate channel (e.g., a designated GitHub Issues repository for documentation, a
          specific email alias, or a feedback form).

---

**Epic 2.8: Enablement of a Pilot Service/Module (Key Outcome of EAF MVP)**

- **Goal:** To validate the entire ACCI EAF MVP by using it to build a real, albeit small, piece of
  software, demonstrating its capabilities and gathering crucial feedback.
- **User Stories:**
  1. **Story 2.8.1: Define and Scope Pilot Service/Module**
     - As the EAF Product Team (John, Fred, Jane, Sarah), I want to clearly define and agree upon
       the scope, functional requirements, and success criteria for a small, representative pilot
       service/module that will be built using the ACCI EAF MVP, so that we have a focused target
       for validating the EAF's capabilities.
     - **Acceptance Criteria:**
       1. A brief Project Brief or scope document for the selected pilot service/module is created
          and approved by stakeholders.
       2. The pilot service scope demonstrably exercises key EAF MVP features: involves at least one
          event-sourced Aggregate, a few Commands and Events, a simple Projector updating a read
          model, basic UI interaction (if applicable, using the UI Foundation Kit), and integration
          with the EAF IAM service for security.
       3. Success criteria for the pilot, specifically from an EAF validation perspective (e.g.,
          "Developer was able to implement X feature using EAF SDK Y within Z timeframe," "TDD
          practices were successfully followed," "Hexagonal structure maintained"), are documented.
  2. **Story 2.8.2: Develop Pilot Service/Module using ACCI EAF MVP**
     - As the Pilot Development Team (e.g., Michael and Lirika, supported by EAF core team), I want
       to successfully develop and build the defined pilot service/module strictly using the ACCI
       EAF MVP (core services accessible via SDKs, `eaf-cli` for scaffolding, "Launchpad" Developer
       Portal for guidance, and adhering to mandated CQRS/ES, DDD, Hexagonal Architecture, TDD
       principles), so that the EAF's usability, SDK effectiveness, and developer experience are
       practically validated.
     - **Acceptance Criteria:**
       1. The pilot service/module is developed following TDD, with unit tests written first and
          achieving >80% coverage for new business logic.
       2. It correctly utilizes the `eaf-eventing-sdk` for publishing domain events relevant to its
          scope to NATS and consuming any necessary events.
       3. It correctly utilizes the `eaf-eventsourcing-sdk` for persisting and rehydrating its
          event-sourced aggregate(s) from the PostgreSQL event store.
       4. It integrates with the EAF IAM service for authentication and basic RBAC using the
          `eaf-iam-client` SDK.
       5. Its internal structure strictly adheres to Hexagonal Architecture principles, with clear
          separation of domain, application, and infrastructure concerns, as scaffolded/guided by
          EAF.
       6. All code passes CI checks, including linting (ktlint), formatting, all unit tests, and
          relevant ArchUnit tests.
       7. If a UI is part of the pilot, it is built using components from the `ui-foundation-kit`
          and Vaadin/Hilla/React.
  3. **Story 2.8.3: Deploy and Test Pilot Service/Module in a Test Environment**
     - As the Pilot Development Team, I want to deploy the pilot service/module to a representative
       test environment (e.g., using the EAF-provided Docker Compose setup for local/dev or a
       similar "less cloud-native" pattern on a shared VM) and perform basic functional and
       integration tests against its exposed APIs or UI, so that its operational viability with EAF
       is demonstrated.
     - **Acceptance Criteria:**
       1. The pilot service/module is successfully deployed to a test environment alongside
          necessary EAF MVP core services (IAM, NATS, Postgres).
       2. Key functionalities of the pilot service (e.g., command processing, event publication,
          query responses, UI interactions if applicable) are tested and verified in this
          environment.
       3. Basic multi-tenancy aspects (e.g., data isolation if the pilot handles tenant-specific
          data) are validated.
       4. Logs from the pilot service and relevant EAF services are accessible and show correct
          operation.
  4. **Story 2.8.4: Collect and Analyze Feedback from Pilot Team**
     - As the ACCI EAF Core Team (John, Fred, Jane, Sarah), I want to systematically collect
       detailed feedback from the pilot development team regarding their experience using the EAF
       MVP's services, SDKs, CLI, documentation ("Launchpad"), and overall development process
       (adherence to DDD, Hexagonal, TDD), so that this feedback can be analyzed to identify EAF
       strengths, weaknesses, and areas for immediate improvement prior to broader rollout.
     - **Acceptance Criteria:**
       1. A feedback collection mechanism (e.g., structured survey, a series of guided interview
          sessions, a dedicated retrospective meeting) is established and communicated to the pilot
          team.
       2. Feedback is actively collected from the pilot team throughout and upon completion of their
          development, focusing on SDK usability, CLI effectiveness, documentation clarity and
          completeness, ease/difficulty of adhering to mandated architectural patterns with EAF
          tooling, and overall Developer Experience.
       3. A summary report of the collected feedback, categorizing findings and including actionable
          insights and specific recommendations for EAF improvement, is produced and reviewed by the
          EAF core team.

---

## Key Reference Documents

- `PROJECT_BRIEF.md` for ACCI EAF
- `MVP_SCOPE_OUTLINE.md` for ACCI EAF
- (Forthcoming) `ACCI_EAF_Architecture.md` (v0.1.0 approved)
- (Forthcoming) `ACCI_EAF_UI_UX_Specification.md` (in progress by Jane)
- (Forthcoming) `ACCI_EAF_Frontend_Architecture.md` (v0.1.0 approved)
- (Anticipated) `ACCI_EAF_Developer_Portal/*` (content to be developed)

## Out of Scope Ideas Post MVP

Refer to "Out of Scope (Initially)" section in `PROJECT_BRIEF.md`. This includes:

- Full migration of all existing DCA-based products.
- Product-specific business logic.
- Provision of underlying cloud/physical infrastructure.
- Development of full end-user product applications (except Control Plane).
- Support for multiple UI frameworks in UI Foundation Kit.
- Built-in SSO across EAF-based apps.
- Integration with external MFA providers (MVP has optional built-in EAF MFA).
- Use of Axon Server (commercial).
- Notification Service and Scheduled Tasks/Job Runner (deferred post-MVP).

## Change Log

| Change                                                          | Date       | Version | Description                                                                                                | Author                                  |
| :-------------------------------------------------------------- | :--------- | :------ | :--------------------------------------------------------------------------------------------------------- | :-------------------------------------- |
| Initial Draft                                                   | 2025-06-03 | 0.1.0   | First draft based on Project Brief & MVP Scope Outline (YOLO mode, Very Technical context)                 | John (PM)                               |
| Technical Story Integration & AC Refinement (Post-Architecture) | 2025-06-04 | 0.2.0   | Integrated architect-identified technical stories and refined ACs for existing stories in MVP Epics 1-2.8. | Sarah (PO) based on Fred's (Arch) input |

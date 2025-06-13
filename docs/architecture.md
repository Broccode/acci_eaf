**{Project Name} Architecture Document** (Project Name: ACCI EAF MVP)

## Introduction / Preamble

This document outlines the overall project architecture for the Minimum Viable Product (MVP) of the
Axians Competence Center Infrastructure - Enterprise Application Framework (ACCI EAF). It details
the technical design, component structures, technology choices, and guiding principles necessary to
build a stable, secure, maintainable, and highly usable foundational platform. The ACCI EAF aims to
accelerate the development of high-quality, multi-tenant software products at Axians, replacing the
legacy DCA framework. This architecture is designed to support the mandatory principles of
CQRS/Event Sourcing (ES), Domain-Driven Design (DDD), Hexagonal Architecture, and Test-Driven Design
(TDD). Its primary goal is to serve as the guiding architectural blueprint for AI-driven development
and human developers alike, ensuring consistency and adherence to chosen patterns and technologies.

**Relationship to Frontend Architecture:** If the project includes a significant user interface
(such as the ACCI EAF Control Plane), a separate Frontend Architecture Document (to be produced by
the Design Architect, Jane) will detail the frontend-specific design and MUST be used in conjunction
with this document. Core technology stack choices documented herein (see "Definitive Tech Stack
Selections") are definitive for the entire project, including any frontend components, unless
specific frontend technologies are detailed in the Frontend Architecture Document.

## Table of Contents

- Introduction / Preamble
- Technical Summary
- High-Level Overview
- Architectural / Design Patterns Adopted
- Component View
- Project Structure
- API Reference (Conceptual for MVP)
- Data Models (Conceptual for MVP)
- Core Workflow / Sequence Diagrams (Conceptual for MVP)
- Definitive Tech Stack Selections
- Infrastructure and Deployment Overview
- Error Handling Strategy
- Coding Standards
- Overall Testing Strategy
- Security Best Practices
- Key Reference Documents
- Change Log
- Prompt for Design Architect (Frontend Architecture Focus) d

## Technical Summary

The ACCI EAF MVP architecture is designed as an **Event-Driven Architecture (EDA)** leveraging
**NATS/JetStream** for its eventing backbone. It fundamentally implements **Command Query
Responsibility Segregation (CQRS) and Event Sourcing (ES)** patterns, with **PostgreSQL** serving as
the persistence layer for the event store, fronted by an EAF-provided logic layer/SDK. Development
will adhere strictly to **Domain-Driven Design (DDD)** for modeling and **Hexagonal Architecture
(Ports and Adapters)** for service structure. The backend services will be developed in **Kotlin
using Spring Boot 3 / Spring Framework 6**, with the **Axon Framework (open-source library)** being
considered to aid CQRS/ES implementation. The entire EAF and its associated applications will be
managed within an **Nx Monorepo** using **Gradle** for backend builds. CI/CD will primarily use
**GitLab CI**, with **GitHub Actions** as a secondary supported option. Key components for the MVP
include core foundational services such as IAM, an Eventing Bus, an Event Store SDK, a scaffolding
CLI, a developer portal, and the ACCI EAF Control Plane. Non-functional requirements like robust
multi-tenancy, high security, maintainability, developer experience, and `ppc64le` compatibility are
paramount.

## High-Level Overview

The ACCI EAF employs a **CQRS/ES architectural style**. Commands are dispatched to aggregates which
validate them and produce events. These events are the source of truth, persisted in the Event Store
(PostgreSQL-based) and published to an Eventing Bus (NATS/JetStream). Query models (read-side
projections) are updated by consuming these events, providing optimized views for querying. This
naturally leads to an **Event-Driven Architecture**.

Services are structured according to the **Hexagonal Architecture**, separating domain logic from
application and infrastructure concerns. Inbound adapters (e.g., REST APIs for the Control Plane,
potentially gRPC for `corenode` integration) receive requests and translate them into commands or
queries. Outbound adapters handle persistence (to the Event Store, read models), event publishing
(to NATS), and interactions with other services.

The entire system is managed within a **Monorepo (Nx)**, containing EAF core services, shared
libraries (SDKs), the Control Plane application, and eventually product applications built on the
EAF.

A typical user interaction flow (e.g., within the Control Plane or an EAF-based application) might
involve:

1. UI (e.g., Hilla/React) sends a request to a backend API (inbound adapter).
2. The API adapter converts this to a Command and dispatches it.
3. A Command Handler (often within an Aggregate) processes the command, performs business logic
   (DDD), and emits Domain Event(s).
4. Events are persisted to the Event Store (PostgreSQL) by the EAF Event Sourcing SDK.
5. Events are published to NATS/JetStream by the EAF Eventing SDK.
6. Projectors (event consumers) subscribe to these events from NATS, update their respective read
   models (e.g., in PostgreSQL).
7. UI sends a Query for data.
8. A Query Handler retrieves data from a read model and returns it to the UI.

```mermaid
graph LR
    subgraph User
        UI[(UI - Vaadin/Hilla/React)]
    end

    subgraph ACCI EAF Backend Service (Hexagonal)
        subgraph Application Core
            CmdHandler[Command Handler / Aggregate]
            DomainLogic{{Domain Logic (DDD)}}
            QueryHandler[Query Handler]
        end
        subgraph Inbound Adapters
            API[API Adapter - REST/Hilla/gRPC]
        end
        subgraph Outbound Adapters
            EventStoreAdapter[Event Store Adapter]
            EventBusAdapter[Event Bus Adapter - Publish]
            ReadModelRepo[Read Model Repository]
            EventConsumerAdapter[Event Bus Adapter - Consume/Projector]
        end
    end

    subgraph Infrastructure
        EventStoreDB[(Event Store - PostgreSQL)]
        EventBusNATS[Eventing Bus - NATS/JetStream]
        ReadModelDB[(Read Models - PostgreSQL)]
    end

    UI -- Command / Query --> API
    API -- Command --> CmdHandler
    CmdHandler -- Uses --> DomainLogic
    CmdHandler -- Emits Events --> EventStoreAdapter
    CmdHandler -- Publishes Events --> EventBusAdapter
    EventStoreAdapter -- Persists --> EventStoreDB
    EventBusAdapter -- Publishes --> EventBusNATS
    EventConsumerAdapter -- Consumes --> EventBusNATS
    EventConsumerAdapter -- Updates --> ReadModelRepo
    ReadModelRepo -- Accesses --> ReadModelDB
    API -- Query --> QueryHandler
    QueryHandler -- Reads --> ReadModelRepo
    EventStoreDB -- Loads Events For --> CmdHandler %% For rehydration
```

## Architectural / Design Patterns Adopted

- **Command Query Responsibility Segregation (CQRS):** Separate models for updating (commands) and
  reading (queries) information.
- **Event Sourcing (ES):** State changes are captured as a sequence of immutable events. The current
  state is derived by replaying these events.
- **Event-Driven Architecture (EDA):** System components communicate primarily through the exchange
  of events, promoting loose coupling and resilience. NATS/JetStream serves as the backbone.
- **Domain-Driven Design (DDD):** Used for modeling the business domain, focusing on bounded
  contexts, aggregates, entities, value objects, and domain events.
- **Hexagonal Architecture (Ports and Adapters):** Isolates the application core (domain and
  application services) from infrastructure concerns (UI, database, messaging).
- **Monorepo:** All EAF code, libraries, and product applications managed in a single repository
  using Nx for tooling.
- **Microservices (for core EAF services):** Core EAF capabilities like IAM, Licensing, and Feature
  Flags will be designed as independent, deployable services, communicating via the event bus or
  well-defined APIs.
- **Software Development Kits (SDKs):** To provide convenient, opinionated access to EAF core
  services and patterns for product developers.
- **Test-Driven Design (TDD):** A mandatory development process.
- **Optimistic Concurrency Control:** For managing concurrent updates to aggregates in the Event
  Store.
- **Transactional Outbox (Consideration for Event Publishing):** To ensure reliable event publishing
  to NATS after database commits for domain events, potentially drawing from research in
  `Axon Framework in Hexagonal EAF` (section 4.a).

## Component View

The ACCI EAF MVP will consist of several key logical components (many will be Kotlin/Spring
services):

- **IAM Service:** Responsible for identity management, authentication (local users per tenant,
  federation via SAML/OIDC/LDAP), and authorization (RBAC/ABAC). Exposes APIs for user/tenant
  management and token validation.
- **EAF Eventing SDK (NATS/JetStream based):** Library for EAF services and product applications to
  publish and consume events reliably using NATS/JetStream. Handles connection, serialization,
  tenant context.
- **EAF Event Store SDK (PostgreSQL based):** Library providing the logic layer for interacting with
  the PostgreSQL event store. Handles event serialization/deserialization, atomic appends with
  optimistic locking, stream reads, and snapshot management.
- **License Management Service:** Manages software licenses, including hardware binding, activation,
  and validation.
- **Feature Flag Management Service:** Central service for defining and evaluating feature flags,
  with SDKs for client applications. (Design based on `Feature Flag Management Best Practices`).
- **Environment & Operational Configuration Management Service (MVP - Basic):** Provides a way for
  services to fetch operational configurations.
- **ACCI EAF Control Plane (Backend):** The Spring Boot backend for the Control Plane web
  application, exposing Hilla endpoints for administrative tasks (tenant management, user management
  within a tenant, license overview).
- **ACCI EAF CLI:** Command-line tool for scaffolding EAF projects and components.
- **"Launchpad" Developer Portal:** Documentation hub (likely Docusaurus).
- **Shared Libraries (`libs/` in Monorepo):**
  - `eaf-core`: Common utilities, base classes for EAF services.
  - `shared-domain-kernel`: Potentially shared, pure domain primitives if applicable across multiple
    bounded contexts.
  - Client libraries for core EAF services (e.g., `eaf-iam-client`).
- **Pilot Application/Service:** A representative application module built using the EAF MVP to
  validate its capabilities.

```mermaid
 C4Context
    title ACCI EAF MVP - Component Overview

    Person(superAdmin, "EAF SuperAdmin", "Manages EAF, Tenants")
    Person(tenantAdmin, "Tenant Admin", "Manages own tenant users/configs")
    Person(devTeam, "Axians Dev Team", "Builds EAF & Products using EAF")
    System_Ext(extIdP, "External IdP", "SAML, OIDC, LDAP/AD")
    System_Ext(corenode, "DPCM CoreNode", "gRPC, Node.js")

    Boundary(bEAF, "ACCI EAF Platform") {
        System(controlPlane, "ACCI EAF Control Plane", "Web App for EAF/Tenant Admin (Vaadin/Hilla/React + Spring Boot Backend)")
        System(iamService, "IAM Service", "Kotlin/Spring, RBAC/ABAC, Federation")
        System(eventingBus, "Eventing Bus (NATS/JetStream)", "Core EDA Backbone")
        System(eventStoreSdk, "Event Store SDK/Logic", "Kotlin/Spring library for PostgreSQL ES")
        System(licenseService, "License Management Service", "Kotlin/Spring")
        System(featureFlagService, "Feature Flag Service", "Kotlin/Spring")
        System(configService, "Config Management Service (MVP)", "Kotlin/Spring")
        System(devPortal, "Developer Portal (Launchpad)", "Docusaurus")
        System(cli, "ACCI EAF CLI", "Scaffolding & Dev Tool")
        SystemDb(pgEventStore, "PostgreSQL Event Store", "Physical DB for events & read models")
        SystemDb(pgIamStore, "PostgreSQL IAM Store", "Physical DB for IAM data")
        SystemDb(pgLicenseStore, "PostgreSQL License Store", "Physical DB for License data")
        System(eafSdkLib, "EAF SDK Libraries", "Shared Kotlin libs for Eventing, ES, IAM Client etc.")
    }

    Boundary(bProduct, "EAF-based Product Application (Pilot)") {
        System(productApp, "Pilot Product/Service", "Built using EAF SDKs & Principles")
    }

    Rel(superAdmin, controlPlane, "Uses, for EAF/Tenant Mgmt")
    Rel(tenantAdmin, controlPlane, "Uses, for own Tenant Mgmt")
    Rel(devTeam, devPortal, "Consumes Documentation")
    Rel(devTeam, cli, "Uses for Scaffolding")
    Rel(devTeam, eafSdkLib, "Uses to Build Products")

    Rel(controlPlane, iamService, "Manages Users/Roles via API")
    Rel(controlPlane, licenseService, "Manages Licenses via API")
    Rel(iamService, extIdP, "Integrates for Federation")
    Rel(iamService, pgIamStore, "Stores/Retrieves IAM data")

    Rel_Back(productApp, eafSdkLib, "Uses SDKs")
    Rel(eafSdkLib, iamService, "Gets AuthN/AuthZ context")
    Rel(eafSdkLib, eventingBus, "Pub/Sub Events")
    Rel(eafSdkLib, eventStoreSdk, "Accesses Event Store")
    Rel(eafSdkLib, featureFlagService, "Evaluates Flags")
    Rel(eafSdkLib, configService, "Gets Config")
    Rel(eafSdkLib, licenseService, "Checks Licenses")

    Rel(eventStoreSdk, pgEventStore, "Stores/Retrieves Events")
    Rel(productApp, eventingBus, "Pub/Sub Events")
    Rel(productApp, pgEventStore, "Updates/Queries Read Models (via its own adapters)")


    Rel_Back(productApp, corenode, "Integrates with (gRPC)")

    %% Generic service interactions
    iamService --|> eafSdkLib
    licenseService --|> eafSdkLib
    featureFlagService --|> eafSdkLib
    configService --|> eafSdkLib
```

## Project Structure

The ACCI EAF will adopt a monorepo structure managed by Nx, containing Gradle multi-module projects
for backend services and libraries, and JS/TS projects for frontend aspects (UI Foundation Kit,
Control Plane frontend).

```plaintext
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

### Key Directory Descriptions

- **`apps/`**: Contains deployable EAF services and the Control Plane application. Each is an Nx
  project, typically corresponding to a Gradle sub-project (or a Hilla project combining backend and
  frontend).
- **`libs/`**: Contains shared libraries.
  - EAF SDK components (`eaf-core`, `eaf-eventing-sdk`, `eaf-eventsourcing-sdk`, etc.): Individual
    Gradle sub-projects providing client SDKs for EAF services and core framework functionalities.
  - `ui-foundation-kit`: Contains the reusable Vaadin/Hilla/React components, themes, and Storybook
    setup.
- **`tools/`**: Contains development tools like the ACCI EAF CLI.
- **`docs/`**: Houses the source for the "Launchpad" Developer Portal.
- **`infra/`**: Contains infrastructure definitions like Docker Compose files for local development
  and Ansible playbooks.
- Root Gradle files (`build.gradle.kts`, `settings.gradle.kts`) manage the Gradle multi-project
  build for all backend components.
- Root Nx files (`nx.json`, `package.json`, `tsconfig.base.json`) manage the overall monorepo, JS/TS
  tooling, and Nx-specific configurations.

## API Reference (Conceptual for MVP)

Detailed API specifications (e.g., OpenAPI for REST, .proto for gRPC) will be developed for each
service. This section outlines conceptual APIs for key MVP services. All EAF APIs must be designed
with multi-tenancy, security (AuthN/AuthZ via IAM service), and versioning in mind.

### External APIs Consumed

- **External IdPs (SAML, OIDC, LDAP/AD):**
  - Purpose: User authentication and attribute sourcing for federation.
  - Interaction: IAM service will act as a client (SP for SAML, RP for OIDC, LDAP client).
  - Link to Official Docs: Specific to each IdP chosen by tenants/Axians.
- **DPCM `corenode` (gRPC):**
  - Purpose: Integration for DPCM product functionality.
  - Interaction: A dedicated EAF service or adapter within a DPCM product application will
    communicate with `corenode` via gRPC. API contracts TBD based on `corenode` interfaces.

### Internal APIs Provided (EAF Core Services - Conceptual)

- **IAM Service API (REST/Hilla):**
  - Purpose: Manage tenants (SuperAdmin), users within tenants (TenantAdmin), roles, permissions.
    Validate tokens.
  - Endpoints (Conceptual):
    - `POST /api/v1/tenants` (SuperAdmin: Create Tenant)
    - `GET /api/v1/tenants/{tenantId}`
    - `POST /api/v1/tenants/{tenantId}/users` (TenantAdmin: Create User in own tenant)
    - `POST /api/v1/auth/token` (Issue EAF token based on credentials or IdP assertion)
    - `GET /api/v1/auth/introspect` (Validate EAF token)
- **License Management Service API (REST/Hilla):**
  - Purpose: Manage license creation, activation, validation.
  - Endpoints (Conceptual):
    - `POST /api/v1/licenses` (SuperAdmin: Create License)
    - `POST /api/v1/tenants/{tenantId}/licenses/{licenseId}/activate`
    - `GET /api/v1/tenants/{tenantId}/licenses/{licenseId}/validate`
- **Feature Flag Service API (REST):**
  - Purpose: For SDKs to fetch flag configurations and for management UI to update flags. (Based on
    `Feature Flag Management Best Practices`).
  - Endpoints (Conceptual):
    - `GET /api/v1/flags?sdkKey=...&context=...` (SDK: Evaluate flags)
    - `POST /api/v1/management/flags` (Admin: Create flag)
- **Eventing SDK (NATS - Not a direct API, but interaction patterns):**
  - Purpose: Publish and subscribe to domain events.
  - Interaction: Via `eaf-eventing-sdk` using NATS client, tenant-scoped subjects.
- **Event Store SDK (Internal - Not a direct API, but interaction patterns):**
  - Purpose: Persist and retrieve event-sourced aggregate events.
  - Interaction: Via `eaf-eventsourcing-sdk`.

## Data Models (Conceptual for MVP)

Core domain objects will be defined using Kotlin data classes, adhering to DDD principles. These are
conceptual and will be refined.

### IAM Service

- **Tenant:** `id (UUID)`, `name (String)`, `status (String)`, `adminEmails (List<String>)`
- **User:** `id (UUID)`, `tenantId (UUID)`, `username (String)`, `email (String)`,
  `hashedPassword (String)`, `roles (List<Role>)`, `status (String)`
- **Role:** `id (UUID)`, `tenantId (UUID)`, `name (String)`, `permissions (List<Permission>)`
- **Permission:** `id (UUID)`, `name (String)` (e.g., `user:create`, `tenant:edit`)

### Event Store (Event Structure)

- **PersistedEvent:** `eventId (UUID)`, `streamId (String)` (e.g., `aggregateType-aggregateId`),
  `aggregateId (String)`, `aggregateType (String)`, `sequenceNumber (Long)`, `tenantId (String)`,
  `eventType (String)`, `eventPayload (JSONB)`, `metadata (JSONB)`, `timestamp (TimestampUTC)`

### License Management Service

- **License:** `id (UUID)`, `tenantId (UUID)`, `productId (String)`, `type (String)`,
  `status (String)`, `expiryDate (Date)`, `activationKey (String)`,
  `hardwareBindingInfo (String/JSONB)`

### Feature Flag Service

- **FeatureFlag:** `key (String)`, `name (String)`, `description (String)`,
  `variations (List<Variation>)`, `defaultOnVariation (String)`, `defaultOffVariation (String)`,
  `rules (List<TargetingRule>)`, `environments (Map<String, EnvironmentConfig>)`
- **TargetingRule:** `conditions (List<Condition>)`, `variationToServe (String)`, `priority (Int)`
- **EnvironmentConfig:** `isEnabled (Boolean)`, `specificRules (List<TargetingRule>)`

## Core Workflow / Sequence Diagrams (Conceptual for MVP)

1. **New Tenant Provisioning (Simplified):**
   - SuperAdmin (via Control Plane UI) -> Control Plane Backend API -> IAM Service (`createTenant`
     command)
   - IAM Service:
     - Validates request.
     - Generates Tenant ID.
     - Creates Tenant entity in IAM DB.
     - Creates initial Tenant Admin User entity in IAM DB.
     - (Optionally) Publishes `TenantCreatedEvent` to NATS.
     - (Future) Calls NATS Provisioning logic to create NATS Account for tenant (as per
       `Dynamic NATS Multi-Tenancy Provisioning`).
   - Control Plane UI receives confirmation.
2. **Basic CQRS Flow (Illustrative - e.g., Product App using EAF):**
   - User (via Product App UI) -> Product App Backend API (`CreateProduct` command with product
     details, tenant context)
   - API Adapter -> Command Gateway (Axon)
   - Command Gateway -> `ProductAggregate` Command Handler (within Product App Backend)
   - `ProductAggregate`: Validates command, applies `ProductCreatedEvent`.
   - EAF Event Sourcing SDK (used by Axon Repository):
     - Persists `ProductCreatedEvent` to PostgreSQL Event Store (with `tenantId`, `aggregateId`,
       `sequenceNumber`).
   - EAF Eventing SDK (hooked into Axon's Unit of Work or via Transactional Outbox):
     - Publishes `ProductCreatedEvent` (JSON) to NATS topic (e.g.,
       `product.tenantId.events.created`).
   - Product Read Model Projector (subscribes to NATS topic via EAF Eventing SDK):
     - Consumes `ProductCreatedEvent`.
     - Updates its PostgreSQL read model table (e.g., `products_view`) with new product details.
   - User (via Product App UI) later queries for product list.
   - Product App Backend API (`ListProducts` query) -> Query Gateway (Axon) -> Query Handler ->
     Reads from `products_view` read model -> Returns data to UI.

## Definitive Tech Stack Selections

These selections are based on the PRD's "Core Technical Decisions & Application Structure" and
further refined for this architectural document. Versions should be pinned during initial project
setup and managed via **centralized dependency version management** in the root `build.gradle.kts`
and `package.json`. See `docs/troubleshooting/dependency-management-guidelines.md` for details.

| Category                        | Technology                                                 | Version / Details                                 | Description / Purpose                                                 | Justification (Primary from PRD/Brief)                                       |
| :------------------------------ | :--------------------------------------------------------- | :------------------------------------------------ | :-------------------------------------------------------------------- | :--------------------------------------------------------------------------- |
| **Languages (Backend)**         | Kotlin                                                     | Latest Stable (e.g., 1.9.x)                       | Primary language for EAF backend services & SDKs                      | Modern, concise, strong JVM ecosystem, Spring integration, `ppc64le` support |
| **Framework (Backend)**         | Spring Boot / Spring Framework                             | Latest Stable (e.g., Boot 3.2.x, Framework 6.1.x) | Core backend application framework                                    | Robust, comprehensive, large ecosystem, good Kotlin support                  |
| **Languages (Frontend)**        | TypeScript                                                 | Latest Stable                                     | Primary language for UI Foundation Kit & Control Plane frontend       | Type safety, aligns with Hilla's generated code                              |
| **Framework (Frontend)**        | Vaadin with Hilla (using React)                            | Latest Stable (e.g., Hilla 2.5.x / Vaadin 24.x.x) | Full-stack framework for Control Plane & UI Kit                       | Chosen UI Framework. Simplifies client-server, type-safe                     |
| **Eventing Bus**                | NATS / JetStream                                           | Latest Stable                                     | Core EDA backbone, persistent event streaming                         | Performance, simplicity, multi-tenancy primitives, `ppc64le` compat.         |
| **Event Store (Persistence)**   | PostgreSQL                                                 | Latest Stable LTS (e.g., 15.x or 16.x)            | Durable storage for events and read models                            | Reliability, ACID, familiarity, JSONB, `ppc64le` support                     |
| **CQRS/ES Aid (Backend)**       | Axon Framework (Open Source Library)                       | Latest Stable (compatible with Spring Boot 3)     | Helper for implementing CQRS/ES patterns (Aggregates, Event Handlers) | Reduces boilerplate for CQRS/ES                                              |
| **Monorepo Management**         | Nx                                                         | Latest Stable                                     | Workspace management, build orchestration, caching                    | Multi-language support, affected commands, dep graph                         |
| **Build Tool (Backend)**        | Gradle                                                     | Latest Stable (via wrapper)                       | Backend project build, dependency management                          | Flexible, Kotlin DSL, Nx integration                                         |
| **Containerization**            | Docker                                                     | Latest Stable                                     | Application packaging and deployment                                  | Standardization, portability                                                 |
| **Infra. Orchestration (LCNE)** | Ansible                                                    | Latest Stable                                     | Infrastructure management in "less cloud-native" environments         | Requirement from Brief                                                       |
| **Security Framework (BE)**     | Spring Security                                            | Pulled by Spring Boot                             | Authentication & Authorization in backend services                    | Standard, robust, integrates with Spring Boot                                |
| **Testing (Backend)**           | JUnit 5, MockK, Spring Boot Test, ArchUnit, Testcontainers | Latest Stable versions                            | Unit, integration, architecture testing                               | TDD support, robust testing                                                  |
| **Logging (Backend)**           | SLF4J with Logback                                         | Pulled by Spring Boot                             | Structured application logging                                        | Standard with Spring Boot, configurable                                      |
| **CLI Framework (EAF CLI)**     | Picocli                                                    | Latest Stable                                     | For building `acci-eaf-cli`                                           | Mature Java/Kotlin CLI framework                                             |
| **Dev Portal Platform**         | Docusaurus                                                 | Latest Stable                                     | For "The Launchpad" Developer Portal                                  | Favored in MVP Scope                                                         |

## Infrastructure and Deployment Overview

- **Cloud Provider(s):** ACCI EAF is designed to be deployable in "less cloud-native" customer
  environments, implying flexibility beyond specific cloud providers. Primary deployment targets are
  customer-managed VMs or infrastructure where Docker and potentially Ansible can be used.
- **Core Services Used (Self-Hosted/Managed):**
  - NATS/JetStream Cluster (for Eventing Bus).
  - PostgreSQL Cluster (for Event Store, Read Models, IAM data, License data, etc.).
  - EAF Core Services (IAM, Licensing, Feature Flags, Control Plane Backend, etc.) deployed as
    Docker containers.
- **Infrastructure as Code (IaC):**
  - Ansible will be used for EAF infrastructure management and setup in target environments where
    applicable.
  - Dockerfiles and Docker Compose files will be provided for all EAF services and for setting up
    local/dev environments.
- **Deployment Strategy:**
  - EAF services will be packaged as Docker images.
  - Deployment to target environments (VMs) will likely involve Docker Compose for simpler setups or
    custom scripts orchestrating Docker container deployments, potentially managed by Ansible.
  - CI/CD pipelines (primarily GitLab CI, with GitHub Actions as a secondary supported option) will
    build Docker images and manage deployment artifacts.
- **Environments:** Standard Development, Staging (or QA), and Production environments are
  anticipated for EAF itself and for products built upon it. Configuration management will handle
  environment-specific settings.
- **Environment Promotion:** Artifacts (Docker images) promoted through environments via CI/CD, with
  appropriate testing and approval gates.
- **Rollback Strategy:** For containerized deployments, rollback typically involves redeploying a
  previous stable Docker image version. Database schema migrations (if any) will require careful
  forward-compatible design and potentially separate rollback scripts/procedures. Event Sourced
  systems have specific considerations for handling "bad" events, possibly involving corrective
  events or re-processing.

## Error Handling Strategy

- **General Approach:** Utilize exceptions for error signaling in Kotlin/Spring backend services. A
  clear hierarchy of custom EAF exceptions (e.g., `EafValidationException`,
  `EafAuthorizationException`, `EafResourceNotFoundException`) extending base Spring/Java exceptions
  will be established. APIs will return standardized error responses (e.g., JSON problem details).
- **Logging (Mandatory):**
  - Library/Method: SLF4J with Logback, configured for structured JSON logging.
  - Format: JSON, including timestamp, severity, service name, thread ID, correlation ID (see
    below), logger name, message, stack trace (for errors), and custom contextual fields (e.g.,
    `tenant_id`, `user_id` where appropriate and safe).
  - Levels: Standard DEBUG, INFO, WARN, ERROR. Consistent usage guidelines will be provided.
  - Context & Correlation IDs: All log messages related to a single request/operation across
    services must include a unique Correlation ID (e.g., propagated via HTTP headers or NATS message
    headers) for distributed tracing.
- **Specific Handling Patterns:**
  - **External API Calls (e.g., to `corenode` or federated IdPs):** Implement resilient client
    patterns:
    - Timeouts (connect and read).
    - Retries with exponential backoff and jitter for transient errors (using libraries like Spring
      Retry or Resilience4j).
    - Circuit Breaker pattern for services prone to unresponsiveness to prevent cascading failures.
    - Clear mapping of external API errors to internal EAF exceptions.
  - **Internal Errors / Business Logic Exceptions:** Custom exceptions will be caught by global
    exception handlers in API layers (e.g., Spring `@ControllerAdvice`) and translated into standard
    API error responses. Sensitive details will not be exposed to clients.
  - **NATS/JetStream Interactions (Event Publishing/Consumption):**
    - Publishing: At-least-once delivery with `PublishAck` handling. Retries for failed publishes.
      Consider Transactional Outbox for critical events if simple retries are insufficient.
    - Consumption: Idempotent consumers are mandatory. Explicit `ack()`, `nak()`, `term()` for
      messages. DLQ strategy for "poison pill" events. Robust error handling within listeners as per
      `NATS Multi-Tenancy JetStream Research` (section 2.4).
  - **Event Store Operations (Optimistic Concurrency):** The EAF Event Store SDK will handle
    optimistic concurrency exceptions during event appends and propagate them clearly for command
    handlers to manage (e.g., by retrying the command with re-read state or failing the operation).
  - **Transaction Management:**
    - Commands in CQRS should typically be processed within a single transaction that appends
      events.
    - Eventual consistency is accepted between the command side (event store) and read models.
    - Projectors updating read models should also manage their updates transactionally where
      appropriate and handle idempotency.

## Coding Standards

These standards are mandatory for all EAF core code and are strongly recommended for product
applications built on EAF. Linting and formatting tools will enforce many of these.

- **Primary Language:** Kotlin (for backend).
- **Style Guide & Linter/Formatter:**
  - Kotlin: Adhere to official Kotlin Coding Conventions.
  - Tooling: Spotless with Ktlint or Kfmt for automated formatting and linting, integrated into CI.
- **Naming Conventions (Kotlin):**
  - Packages: lowercase, dot-separated (e.g., `com.axians.eaf.iam.application.service`).
  - Classes/Interfaces/Objects/Enums/Annotations: PascalCase (e.g., `TenantProvisioningService`).
  - Functions/Methods/Properties (val/var): camelCase (e.g., `provisionNewTenant`).
  - Constants (compile-time `const val`, top-level or object `val` for true constants):
    SCREAMING_SNAKE_CASE (e.g., `DEFAULT_TIMEOUT_MS`).
  - Test Methods: backticks for descriptive names (e.g.,
    ``fun `should provision tenant when valid data is provided`() {}``).
- **File Structure:** Adhere to standard Gradle project layout (`src/main/kotlin`,
  `src/test/kotlin`). Within these, organize by feature or architectural layer (e.g.,
  `domain/model`, `application/port/in`, `infrastructure/adapter/out/persistence`) consistent with
  Hexagonal Architecture.
- **Unit Test File Organization:** Co-located with source files or in a parallel structure within
  `src/test/kotlin` (e.g., `MyService.kt` in `main` has `MyServiceTest.kt` in `test` in the same
  package). Test file names end with `Test`.
- **Asynchronous Operations (Kotlin):** Primarily use Kotlin Coroutines for asynchronous
  programming. Adhere to structured concurrency principles. Avoid blocking threads unnecessarily.
- **Type Safety & Nullability (Kotlin):** Leverage Kotlin's null safety. Avoid platform types where
  possible. Use nullable types (`?`) explicitly. Minimize use of not-null assertion operator (`!!`).
- **Immutability:** Prefer immutable data structures (e.g., `val`, `List`, `Map`, Kotlin data
  classes with `val` properties) where practical, especially for DTOs, Value Objects, and Events.
- **Comments & Documentation:**
  - KDoc for all public APIs (classes, methods, properties).
  - Explain _why_, not _what_, for complex logic. Avoid redundant comments.
  - Module-level READMEs for significant libraries or services.
- **Dependency Management:** Use **centralized dependency version management** in the root
  `build.gradle.kts`. ALL dependency versions are defined centrally and accessed via
  `${rootProject.extra["versionVariable"]}`. **NO** hardcoded versions or version catalogs
  (`libs.versions.toml`) are permitted in module `build.gradle.kts` files. See
  `docs/troubleshooting/dependency-management-guidelines.md` for complete guidelines.
- **Error Handling:** Use exceptions for error states. Define and use specific EAF exceptions. Avoid
  swallowing exceptions or catching generic `Exception` without specific handling or re-throwing.
- **Logging:** Use SLF4J for logging. Log meaningful messages with appropriate context (correlation
  IDs, tenant IDs where applicable and safe). Avoid logging sensitive information.
- **Spring Framework Usage:**
  - Prefer constructor injection for dependencies.
  - Use Spring Boot auto-configuration where possible.
  - Adhere to Spring best practices for defining beans, configurations, and profiles.

### Detailed Kotlin Specifics

- **Functional Constructs:** Utilize Kotlin's functional programming features (lambdas, higher-order
  functions, collection operations like `map`, `filter`, `fold`) where they enhance readability and
  conciseness without sacrificing clarity.
- **Data Classes:** Use extensively for DTOs, Events, Commands, and simple Value Objects.
- **Sealed Classes/Interfaces:** Use for representing restricted hierarchies (e.g., different types
  of Commands for an Aggregate, or states in a state machine).
- **Extension Functions:** Use judiciously to enhance existing classes with utility functions,
  keeping them well-scoped and discoverable.
- **Coroutines:** Follow best practices for launching coroutines (e.g., within `CoroutineScope` tied
  to component lifecycle), error handling (e.g., `CoroutineExceptionHandler`), and context switching
  (`withContext`).

## Overall Testing Strategy

Testing is a cornerstone of ACCI EAF, with TDD being mandatory.

- **Tools (Backend):** JUnit 5, MockK (for Kotlin mocking), Spring Boot Test utilities, AssertJ (for
  fluent assertions), ArchUnit (for architecture rule validation), Testcontainers (for integration
  tests with PostgreSQL, NATS, etc.).
- **Unit Tests:**
  - **Scope:** Test individual Kotlin classes, functions, and small modules in isolation. Focus on
    business logic within Aggregates, Domain Services, Application Services, and utility classes.
  - **Location:** Typically co-located or in a parallel package structure in `src/test/kotlin`.
  - **Mocking/Stubbing:** Use MockK for creating test doubles. Mock all external dependencies (other
    services, database interactions via repository ports, NATS interactions via eventing ports).
  - **Target Coverage:** >80% line/branch coverage for core EAF logic.
- **Integration Tests:**
  - **Scope:** Test interactions between EAF components/services or between services and
    infrastructure (e.g., service layer to Event Store SDK to PostgreSQL; service layer to Eventing
    SDK to NATS). Test Hexagonal adapter integrations.
  - **Location:** Separate source set or directory (e.g., `src/integrationTest/kotlin`).
  - **Environment:** Use Testcontainers to spin up real instances of PostgreSQL and NATS/JetStream
    for reliable integration testing. Spring Boot test slices (e.g., `@DataJpaTest`,
    `@SpringBootTest` with specific components) will be used.
- **Architecture Tests (ArchUnit):**
  - **Scope:** Enforce architectural rules like layer dependencies (Hexagonal), DDD constraints,
    naming conventions, and domain purity (no infrastructure dependencies in domain code).
  - **Location:** Can be part of the main test suite or a dedicated test module.
- **End-to-End (E2E) Tests:**
  - **Scope:** Validate complete user flows or critical paths for applications like the ACCI EAF
    Control Plane MVP and the pilot application.
  - **Tools:** Playwright is mentioned for Hilla. This or similar for frontend E2E. For backend EAF
    service E2E tests, this might involve API-level tests orchestrating multiple service calls.
- **Performance Tests:**
  - **Scope:** Benchmark critical EAF operations and services against defined NFRs (response times,
    throughput).
  - **Tools:** JMeter, k6, or similar.
  - **Process:** Establish baseline benchmarks and run tests regularly (especially in CI) to detect
    performance regressions.
- **Test Data Management:** Strategies for generating and managing test data for various test levels
  will be defined (e.g., test data builders, setup scripts for integration tests).

## Security Best Practices

Security is paramount for ACCI EAF.

- **Authentication & Authorization (IAM Service):**
  - Strong authentication for all users and services (local users per tenant, federation via
    SAML/OIDC/LDAP, optional built-in MFA).
  - Robust RBAC and ABAC mechanisms for fine-grained access control, enforced by the IAM service and
    EAF SDKs.
  - Secure token management (e.g., JWTs) with appropriate lifetimes, signing, and validation.
- **Input Sanitization/Validation:** All external inputs (API requests, event payloads,
  user-provided data) must be rigorously validated at service boundaries (e.g., using Bean
  Validation in Spring, custom validation logic).
- **Output Encoding:** Contextual output encoding for data rendered in UIs (handled by Vaadin/Hilla
  components) and in API responses to prevent XSS.
- **Secrets Management:**
  - No hardcoded secrets. Use environment variables, Docker secrets, or a dedicated secrets
    management solution (e.g., HashiCorp Vault if feasible in target environments) for API keys,
    database credentials, signing keys.
  - EAF services will securely access secrets via a configuration mechanism.
- **Dependency Security:**
  - Regularly scan dependencies (both backend JVM and frontend JS/TS) for known vulnerabilities
    using tools like OWASP Dependency-Check (for scanning) and OWASP Dependency-Track (for ongoing
    monitoring of SBOMs), Snyk, or GitHub Dependabot.
  - Establish a process for timely patching of vulnerable dependencies.
- **Secure Inter-Service Communication:** All communication between EAF services (e.g., via NATS or
  internal APIs) must be over secure channels (e.g., TLS for NATS if configured, HTTPS for REST
  APIs). Authentication between services where necessary (e.g., service-to-service tokens).
- **Data Protection:**
  - Encryption of sensitive data at rest (e.g., PostgreSQL Transparent Data Encryption if
    available/applicable, or application-level encryption for specific fields).
  - Encryption of data in transit (TLS everywhere).
- **Principle of Least Privilege:** Services, users, and API clients should operate with the minimum
  necessary permissions.
- **API Security:**
  - Enforce HTTPS for all APIs.
  - Implement rate limiting and throttling to prevent abuse.
  - Use standard HTTP security headers (CSP, HSTS, X-Frame-Options, etc.) for web applications like
    the Control Plane.
- **Error Handling & Information Disclosure:** Error messages must not leak sensitive internal
  information. Detailed errors logged server-side, generic messages/error IDs to clients.
- **Auditing:** Comprehensive and secure audit trails for all critical administrative, IAM, and
  security-relevant events. Audit logs stored separately and securely.
- **Compliance Enablement:** Design with ISO27001, SOC2, and FIPS requirements in mind, facilitating
  product certification.
- **Regular Security Reviews & Testing:** Incorporate security testing (SAST, DAST, potentially
  penetration testing for mature EAF versions) into the development lifecycle.

## Key Reference Documents

- `PROJECT_BRIEF.md` for ACCI EAF
- `MVP_SCOPE_OUTLINE.md` for ACCI EAF
- `ACCI_EAF_PRD.md` (v0.2.0 - updated by Sarah PO)
- (Forthcoming) `ACCI_EAF_UI_UX_Specification.md` (from Jane, Design Architect)
- (Forthcoming) `ACCI_EAF_Frontend_Architecture.md` (from Jane, Design Architect - Draft 0.1.0
  approved)
- Research Documents:
  - `ArchUnit-Regeln für ACCI EAF-Architektur`
  - `Axon Framework in Hexagonal EAF`
  - `Dynamic NATS Multi-Tenancy Provisioning`
  - `Feature Flag Management Best Practices`
  - `Hilla, Spring Boot, Gradle-Projekt`
  - `NATS Multi-Tenancy JetStream Research`
  - `Nx mit Hilla und Spring Boot`
- "Launchpad" Developer Portal (once populated)

## Change Log

| Change                                              | Date       | Version | Description                                                               | Author      |
| :-------------------------------------------------- | :--------- | :------ | :------------------------------------------------------------------------ | :---------- |
| Initial Draft of ACCI EAF MVP Architecture Document | 2025-06-04 | 0.1.0   | Comprehensive first pass based on PRD, MVP Scope, and technical research. | Fred (Arch) |

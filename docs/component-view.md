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

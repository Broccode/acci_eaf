# Architecture Overview

This document provides a high-level overview of the ACCI EAF architecture.

> **Status:** Draft – sections marked with `TODO`.

## Table of Contents

1. [Hexagonal Architecture](#hexagonal-architecture)
2. [CQRS & Event Sourcing](#cqrs--event-sourcing)
3. [Multi-Tenancy](#multi-tenancy)
4. [Plugin System](#plugin-system)
5. [Security Layer](#security-layer)
6. [Diagrams](#diagrams)

---

## Hexagonal Architecture

The ACCI EAF employs Hexagonal Architecture (also known as Ports and Adapters) to isolate the core application logic from external concerns. This promotes testability, maintainability, and flexibility by allowing different technologies or external services to be swapped out with minimal impact on the core.

- **Core (`libs/core`):** Contains the technology-agnostic business logic.
  - `domain`: Includes Aggregates, Entities, Value Objects, Domain Events, and Repository *Interfaces* (Ports). This layer has no dependencies on external frameworks.
  - `application`: Houses Use Cases, Command/Query Handlers, Application Services, and Data Transfer Objects (DTOs). It defines *Interfaces* (Ports) for infrastructure components and depends only on the `domain` layer.
- **Infrastructure (`libs/infrastructure`, `apps/*`):** Implements the Ports defined in the `core` layer using specific technologies (e.g., NestJS for web controllers, MikroORM for database repositories, Redis for caching). This layer contains Adapters such as controllers, database repository implementations, and cache clients. It depends on the `core` layer.
- **Dependency Rule:** Dependencies strictly flow inwards: Infrastructure → Application → Domain. This ensures the core logic remains independent of specific technology choices.

For more details, refer to the main architecture document (`.ai/arch.md`).

## CQRS & Event Sourcing

Command Query Responsibility Segregation (CQRS) and Event Sourcing (ES) are key patterns in ACCI EAF for managing application state and processing requests.

- **CQRS:** Separates read (Queries) and write (Commands) operations. This allows for optimizing each path independently.
  - **Command Flow:** An incoming HTTP request is typically routed through a NestJS Controller to a `CommandBus`. The `CommandBus` dispatches the Command to its respective Handler. The Handler loads an Aggregate (via a Repository), which validates the command and, if successful, produces one or more Domain Events. These events are then saved by an Event Store Adapter and published via an `EventBus`.
  - **Query Flow:** An HTTP request for data is routed to a `QueryBus` via a Controller. The `QueryBus` dispatches the Query to its Handler, which then reads data from an optimized Read Model Repository (often a denormalized projection) and returns the result.
- **Event Sourcing:** Uses a sequence of events as the single source of truth for an aggregate's state. Instead of storing the current state, all changes are stored as immutable events.
  - **Event Store:** A specialized database (e.g., a PostgreSQL table) that appends events. Key columns include `stream_id` (Aggregate ID), `version`, `event_type` (versioned, e.g., `UserRegistered.v1`), `payload` (JSONB), and `timestamp`.
  - **Projections:** Event Handlers listen to the `EventBus` (or poll the event store) and update Read Models based on the events. These Read Models are optimized for querying.
- **Key Considerations:**
  - **Event Schema Evolution (ADR-010):** Events are versioned (e.g., `UserRegistered.v1`, `UserRegistered.v2`). Upcasting functions are implemented to transform older event versions to the latest schema when events are loaded or processed. This ensures backward compatibility.
  - **Idempotent Handlers (ADR-009):** Event handlers are designed to safely process the same event multiple times without unintended side effects. This is typically achieved using a tracking table (`processed_events`) that stores `event_id` (and potentially handler identifiers) to skip already processed events.

Refer to ADR-009 and ADR-010 for detailed decisions on idempotency and event evolution.

## Multi-Tenancy

ACCI EAF provides multi-tenancy using a Row-Level Security (RLS) approach, ensuring that data for different tenants is securely isolated within the same database instance.

- **Approach:** A `tenant_id` column is included in all tenant-specific database tables.
- **Context Propagation:**
    1. The `tenant_id` is extracted from incoming requests (e.g., from a JWT claim or a custom HTTP header) by a dedicated NestJS Middleware.
    2. This `tenant_id` is then made available throughout the request lifecycle using `AsyncLocalStorage` (provided by `libs/tenancy`).
- **Enforcement (ADR-006):**
  - MikroORM Global Filters are configured centrally.
  - These filters automatically append a `WHERE tenant_id = :currentTenantId` clause to all relevant SQL queries.
  - The `:currentTenantId` parameter is dynamically populated from the `tenant_id` stored in `AsyncLocalStorage`.
    This ensures that data access is automatically and transparently restricted to the current tenant's data.

Details can be found in ADR-006 and the `libs/tenancy` documentation.

## Plugin System

The framework includes a plugin system to allow for extending its functionality without modifying the core libraries. This promotes modularity and makes it easier to add custom features or integrations.

- **Plugin Interface:** A defined interface that plugins must implement.
- **Loading Mechanism:** Plugins are typically discovered and loaded during application bootstrap.
- **Registration Points:** Plugins can register various components like NestJS modules, controllers, services, and MikroORM entities.
- **Entity Discovery:** MikroORM's entity discovery mechanism (via glob patterns in `mikro-orm.config.ts`) is leveraged. Plugins can define their entities within their own directory structure, and these are automatically picked up by the main MikroORM configuration.
- **Migrations (ADR-008):**
  - Plugins are responsible for providing their own MikroORM migration files.
  - The main application's migration process (executed via MikroORM CLI) is configured to discover and run these plugin-specific migrations alongside core application migrations. This ensures that database schemas for plugins are managed consistently.
- **Dynamic Loading:** The system supports dynamic loading of these plugins, enabling a flexible and extensible architecture.

Refer to ADR-008 for the plugin migration strategy.

## Security Layer

Security is a fundamental aspect of ACCI EAF, with various components and strategies in place to protect applications.

- **Authentication (AuthN):**
  - Handled by a flexible NestJS Auth module.
  - Version 1 includes support for JWT (bearer token) and Local (username/password) strategies.
  - The architecture is designed to potentially allow tenant-configurable authentication strategies in the future.
- **Authorization (AuthZ - ADR-001):**
  - Employs a combination of Role-Based Access Control (RBAC) and basic Attribute-Based Access Control (ABAC), primarily for ownership checks.
  - The `casl` library is used for defining and checking permissions (e.g., `manage`, `read`) on various subjects (e.g., `Tenant`, `User`).
- **Guards:** Standard NestJS Guards are used to protect API endpoints and methods (e.g., `@UseGuards(JwtAuthGuard, CaslGuard('manage', 'Tenant'))`).
- **Security Headers:** Integration of `helmet` middleware to apply various HTTP headers that help protect against common web vulnerabilities (e.g., XSS, clickjacking).
- **Rate Limiting:** Basic protection against brute-force and Denial-of-Service (DoS) attacks is provided by `@nestjs/throttler` middleware.

Key decisions regarding authorization are documented in ADR-001.

## Diagrams

This section will contain or link to various diagrams illustrating the ACCI EAF architecture.

- **High-Level Component Overview (Monorepo Structure - C4 Container Diagram):**

    ```mermaid
    graph TD
        A[User] --> B(Browser/Client Application)
        B --> C{ACCI EAF Application (NestJS)}
        C --> D[PostgreSQL Database]
        C --> E[Redis Cache]
        C --> F(External Services / APIs)

        subgraph Monorepo
            direction LR
            subgraph "apps/"
                direction TB
                G["apps/control-plane-api"]
                H["apps/sample-app"]
            end
            subgraph "libs/"
                direction TB
                I["libs/core"]
                J["libs/infrastructure"]
                K["libs/tenancy"]
                L["libs/rbac"]
                M["libs/licensing"]
                N["libs/plugins"]
                O["libs/testing"]
                P["libs/shared"]
            end
        end
        G --> I; G --> J; G --> K; G --> L; G --> M; G --> P;
        H --> I; H --> J; H --> K; H --> L; H --> M; H --> P;
        J --> I;
    end
    ```

- **Request Flow - Command (Sequence Diagram):**

    ```mermaid
    sequenceDiagram
        participant Client
        participant Controller (NestJS)
        participant CommandBus
        participant CommandHandler
        participant Aggregate
        participant EventStoreAdapter
        participant EventBus

        Client->>Controller: HTTP POST /resource
        Controller->>CommandBus: dispatch(CreateResourceCommand)
        CommandBus->>CommandHandler: handle(CreateResourceCommand)
        CommandHandler->>Aggregate: constructor() / load()
        CommandHandler->>Aggregate: executeCommand(data)
        Aggregate->>Aggregate: validate()
        Aggregate->>EventStoreAdapter: save(ResourceCreatedEvent)
        EventStoreAdapter-->>Aggregate: event saved
        Aggregate-->>CommandHandler: success
        CommandHandler->>EventBus: publish(ResourceCreatedEvent)
        CommandHandler-->>Controller: result
        Controller-->>Client: HTTP 201 Created
    end
    ```

- **Request Flow - Query (Sequence Diagram):**

    ```mermaid
    sequenceDiagram
        participant Client
        participant Controller (NestJS)
        participant QueryBus
        participant QueryHandler
        participant ReadModelRepository

        Client->>Controller: HTTP GET /resource/{id}
        Controller->>QueryBus: dispatch(GetResourceQuery)
        QueryBus->>QueryHandler: handle(GetResourceQuery)
        QueryHandler->>ReadModelRepository: findById(id)
        ReadModelRepository-->>QueryHandler: resourceData
        QueryHandler-->>Controller: resourceData
        Controller-->>Client: HTTP 200 OK with resourceData
    end
    ```

- **Tenant Context Propagation & RLS Filter (Simplified Sequence Diagram):**

    ```mermaid
    sequenceDiagram
        participant Client
        participant Middleware (NestJS)
        participant AsyncLocalStorage
        participant Service/Handler
        participant MikroORM_GlobalFilter
        participant Database

        Client->>Middleware: Request with Tenant ID (e.g., JWT/Header)
        Middleware->>AsyncLocalStorage: set('tenantId', extractedTenantId)
        Middleware->>Service/Handler: processRequest()
        Service/Handler->>MikroORM_GlobalFilter: DB Query (e.g., find Entity)
        MikroORM_GlobalFilter->>AsyncLocalStorage: get('tenantId')
        AsyncLocalStorage-->>MikroORM_GlobalFilter: currentTenantId
        MikroORM_GlobalFilter->>Database: SELECT * FROM entity WHERE tenant_id = currentTenantId AND ...
        Database-->>MikroORM_GlobalFilter: Filtered Data
        MikroORM_GlobalFilter-->>Service/Handler: Filtered Data
        Service/Handler-->>Client: Response
    end
    ```

*(More diagrams like Event Handling and Core Entity Relationships will be added as needed based on `.ai/arch.md`)*

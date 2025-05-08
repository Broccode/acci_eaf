# ACCI EAF - Axians Competence Center Infrastructure Enterprise Application Framework

## Project Overview

ACCI EAF is an internal software framework developed by Axians to accelerate, standardize, and secure the development of robust, scalable, and maintainable enterprise applications. The framework addresses common challenges in enterprise application development by providing a solid foundation of reusable components and best practices for multi-tenancy, security, observability, and compliance.

## Core Architecture

The framework is built on several key architectural patterns:

### Hexagonal Architecture (Ports & Adapters)

ACCI EAF implements a clean hexagonal architecture that separates:

- **Core (`libs/core`)**: Technology-agnostic business and application logic
  - `domain`: Aggregates, Entities, Value Objects, Domain Events, Repository Interfaces (Ports)
  - `application`: Use Cases, Command/Query Handlers, Application Services, DTOs, Port Interfaces
- **Infrastructure (`libs/infrastructure`, `apps/*`)**: Implements Ports using specific technologies
  - Controllers, Database Repositories, Cache Clients, etc.

The dependency rule ensures that dependencies always flow inwards (Infrastructure → Application → Domain), making the core business logic independent of external frameworks and technologies.

### CQRS and Event Sourcing

The framework implements Command Query Responsibility Segregation (CQRS) with Event Sourcing:

- **Command Flow**: HTTP Request → Controller → CommandBus → Command Handler → Aggregate → Event Store → EventBus
- **Event Flow**: EventBus → Event Handler/Projector → Read Model Updates
- **Query Flow**: HTTP Request → Controller → QueryBus → Query Handler → Read Model Repository → Response

Key features include:
- Complete audit trails through immutable event records
- Temporal queries for point-in-time reconstruction
- Improved scalability through separate read/write paths
- Clear separation of mutation and retrieval operations
- Event schema evolution (ADR-010) using versioned event types and upcasters
- Idempotent event handlers (ADR-009) using a processed events tracking table

### Multi-Tenancy via Row-Level Security

The framework implements multi-tenancy using Row-Level Security (RLS) as defined in ADR-006:

- All tenant-specific data includes a `tenant_id` column
- Tenant context is extracted from requests (JWT/headers) via middleware
- Context is propagated using `AsyncLocalStorage` for the request lifecycle
- MikroORM Global Filters automatically apply `WHERE tenant_id = :currentTenantId` to all queries
- This ensures strong data isolation between tenants within a single application instance

### Plugin System

ACCI EAF includes a plugin system for extensibility (ADR-008):

- Plugins are implemented as NestJS modules that encapsulate specific functionality
- Plugins can define their own MikroORM entities that are discovered via glob patterns
- Plugins provide their own database migrations within their structure
- The main application's migration process handles executing plugin migrations
- Plugins can interact with core services (tenant context, RBAC, etc.) through well-defined interfaces

## Project Structure

The project follows a monorepo structure managed by Nx:

```
apps/
  control-plane-api/    # API for tenant management
  control-plane-ui/     # UI for tenant management (frontend)
  sample-app/           # Sample application using EAF
libs/
  core/                 # Domain and application logic (tech-agnostic)
  infrastructure/       # Adapters implementing ports
  tenancy/              # Tenant context handling
  rbac/                 # RBAC/ABAC core logic, Guards
  licensing/            # License validation
  plugins/              # Plugin interface and loading
  testing/              # Shared test utilities
  shared/               # Shared DTOs, constants, utilities
docs/
  adr/                  # Architecture Decision Records
  concept/              # Concept documents
  diagrams/             # Architecture diagrams
  setup/                # Setup guides
```

## Technology Stack

ACCI EAF leverages the following key technologies:

| Technology | Description | ADR Decision |
| ---------- | ----------- | ------------ |
| TypeScript | Main language | |
| Node.js | Runtime environment | |
| NestJS | Backend framework | |
| PostgreSQL | Database for event store and read models | |
| MikroORM | ORM with entity discovery and RLS filters | ADR-006 |
| Redis | Caching | |
| Nx | Monorepo management | |
| Jest | Testing framework | |
| `suites` | Unit testing utilities for DI components | |
| Testcontainers | Integration testing with real DB/Cache | |
| `supertest` | E2E testing HTTP requests | |
| `nestjs-i18n` | Internationalization library | |
| `casl` | RBAC/ABAC library | ADR-001 |
| `helmet` | Security headers middleware | |
| `@nestjs/throttler` | Rate limiting middleware | |
| `@nestjs/terminus` | Health checks module | |
| OpenAPI | API documentation standard (via NestJS) | |
| `@cyclonedx/cyclonedx-npm` | SBOM generation tool | ADR-002 |
| CycloneDX (JSON) | SBOM format | ADR-002 |

## Key Features

### Tenant Management

- Control Plane API for system administrators to manage tenants
- Bootstrapping procedure for initial tenant setup (ADR-007)
- Secure isolation of tenant data via RLS with MikroORM Filters
- Tenant-aware commands, events, and queries

### Role-Based Access Control

- Integration with CASL for flexible permission definitions (ADR-001)
- Support for both role-based and attribute-based (ownership) rules
- NestJS guards for securing endpoints and methods
- Tenant-aware permission enforcement

### License Validation

- Hybrid approach with offline file validation and optional online checks (ADR-003)
- Digitally signed license files containing constraints and entitlements
- License attributes include tenant ID, expiration date, feature flags, and usage constraints
- Support for various license models (e.g., expiration dates, feature flags, CPU limits)

### Event Sourcing

- Event-based persistence using MikroORM and PostgreSQL
- Event store schema with stream_id, version, event_type, payload, timestamp, and tenant_id
- Event schema evolution through versioning and upcasting (ADR-010)
- Idempotent event handlers using processed events tracking (ADR-009)

### Observability

- Structured logging for better searchability and analysis
- Health check endpoints for monitoring and orchestration
- Hooks for metrics and distributed tracing

### Testing

- Comprehensive testing strategy (unit, integration, E2E)
- Unit tests with Jest and `suites` for core domain and application logic
- Integration tests with Testcontainers for real database/cache interactions
- E2E tests with supertest and NestJS testing utilities

### SBOM Generation

- Integrated Software Bill of Materials generation using `@cyclonedx/cyclonedx-npm` (ADR-002)
- CycloneDX JSON format for compatibility
- Integration into the build process via npm scripts

## Development and Usage

### Prerequisites

- Node.js (v16+)
- PostgreSQL (v14+)
- Redis (v6+)
- Docker (recommended for local development)

### Setup and Development

```bash
# Install dependencies
npm install

# Setup database (using docker-compose)
npm run db:setup

# Build all packages
npx nx run-many --target=build --all

# Run the sample application
npx nx serve sample-app

# Generate SBOM
npm run generate:sbom
```

### Common Development Commands

```bash
# Generate a new lib
npx nx g @nx/js:lib my-new-lib

# Run tests for a specific library
npx nx test core

# Run tests for all libraries and apps
npx nx test

# Bootstrap the control plane
npm run bootstrap:control-plane

# Run e2e tests for control plane
npm run test:e2e:control-plane
```

## Documentation

Detailed documentation is available in the `docs/` directory:

- **Architecture Overview**: `docs/ARCH.md`
- **Product Requirements**: `docs/PRD.md`
- **Architecture Decision Records**: `docs/adr/`
  - ADR-001: RBAC Library Selection (CASL)
  - ADR-002: SBOM Tool Selection (CycloneDX)
  - ADR-003: License Validation Mechanism (Hybrid Approach)
  - ADR-006: RLS Enforcement Strategy (MikroORM Filters)
  - ADR-007: Control Plane Bootstrapping
  - ADR-008: Plugin Migrations
  - ADR-009: Idempotent Event Handlers
  - ADR-010: Event Schema Evolution
- **Concept Documents**: `docs/concept/`
  - CQRS and Event Sourcing
  - Multi-Tenancy
  - License Validation
  - Plugin System
- **Setup Guides**: `docs/setup/`

## License and Usage

ACCI EAF is for internal use only by Axians and its customers. It is not open source software and all rights are reserved by Axians.
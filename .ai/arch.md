# Architecture for ACCI EAF

Status: Draft

## Technical Summary

The ACCI EAF (Axians Competence Center Infrastructure Enterprise Application Framework) is designed as a modular, extensible, and secure backend framework for enterprise applications. It leverages Hexagonal Architecture, CQRS/Event Sourcing, and Multi-Tenancy via Row-Level Security (RLS) to ensure scalability, maintainability, and compliance. The framework is built as a monorepo (Nx) with clear separation between core logic, infrastructure, and adapters, and supports extensibility through a plugin system. Security, observability, and testability are first-class citizens.

## Technology Table

| Technology      | Description                                                      |
| -------------- | ---------------------------------------------------------------- |
| TypeScript     | Main language                                                    |
| Node.js        | Runtime environment                                              |
| NestJS         | Backend framework                                                |
| PostgreSQL     | Database for event store and read models                         |
| MikroORM       | ORM with entity discovery and RLS filters                        |
| Redis          | Caching                                                          |
| Nx             | Monorepo management                                              |
| Jest           | Testing framework                                                |
| Testcontainers | Integration testing with real DB/Cache                           |
| supertest      | E2E testing                                                      |
| @nestjs/i18n   | Internationalization                                            |
| casl           | RBAC/ABAC                                                        |
| helmet         | Security headers                                                 |
| @nestjs/throttler | Rate limiting                                                 |
| @nestjs/terminus | Health checks                                                  |
| OpenAPI        | API documentation                                                |
| @cyclonedx/bom | SBOM generation (planned)                                        |

## Architectural Diagrams

<!-- Mermaid/C4/UML diagrams to be added as the architecture evolves -->

## Data Models, API Specs, Schemas, etc

- Event Store: `events` table with `stream_id`, `version`, `event_type`, `payload`, `timestamp`, `tenant_id`
- Read Models: tenant-aware tables with `tenant_id`
- User, Tenant, Role, Permission entities (RBAC/ABAC)
- License schema (TBD)

## Project Structure

```text
apps/
  control-plane-api/    # API for tenant management
  sample-app/           # Sample application using EAF
libs/
  core/                 # Domain logic, application use cases
  infrastructure/       # Adapters (DB, cache, web, i18n)
  tenancy/              # Tenant context handling
  rbac/                 # RBAC/ABAC logic, guards
  licensing/            # License validation
  plugins/              # Plugin interface and loader
  testing/              # Shared test utilities
  shared/               # DTOs, constants, utilities
```

docs/
  adr/                  # Architecture Decision Records

## Infrastructure

- PostgreSQL for event store and read models
- Redis for caching
- Nx for monorepo management
- Docker for containerization (recommended)
- Health checks for orchestration (Kubernetes-ready)

## Deployment Plan

- Applications are packaged as Docker containers
- Monorepo builds generate optimized artifacts per app
- Health endpoints support orchestration platforms
- Graceful shutdown implemented

## Change Log

| Change        | Story ID | Description                |
| ------------- | -------- | -------------------------- |
| Initial draft | N/A      | Initial architecture draft |

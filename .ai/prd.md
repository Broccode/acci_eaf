# 1. Title: PRD for ACCI EAF

<version>1.0.0</version>

## Status: Draft

## Intro

The ACCI EAF (Axians Competence Center Infrastructure Enterprise Application Framework) is an internal software framework designed to accelerate, standardize, and secure the development of robust, scalable, and maintainable enterprise applications for Axians and its customers. It addresses common pain points in enterprise app development by providing a solid foundation, reusable components, and best practices for multi-tenancy, security, observability, and compliance.

## Goals

- Accelerate development of enterprise applications
- Promote best practices (Hexagonal Architecture, CQRS/ES, Multi-Tenancy, RBAC/ABAC, i18n, SBOM, Security by Design)
- Improve code quality and maintainability
- Enable compliance with standards (ISO 27001, SOC2)
- Provide a licensing mechanism
- Ensure high test coverage and observability
- Support modularity and extensibility via plugins
- Provide a project template for new applications

## Features and Requirements

- Functional requirements:
  - Core architecture (Hexagonal, CQRS/ES, Plugin System)
  - Multi-Tenancy via RLS (MikroORM Filters)
  - Control Plane API for tenant management
  - PostgreSQL and Redis adapters
  - Internationalization (nestjs-i18n)
  - License validation mechanism
  - Observability (logging, health checks)
  - Security (headers, rate limiting, AuthN/AuthZ, RBAC/ABAC)
  - Graceful shutdown
  - Testing framework (unit, integration, E2E)
  - OpenAPI support
  - SBOM generation
  - Monorepo setup (Nx), ADRs, MikroORM Entity Discovery
  - Basic documentation and project template
- Non-functional requirements:
  - Performance: P95 latency < 200ms (queries)
  - Scalability: horizontal scaling, statelessness
  - Reliability: graceful shutdown, error handling, idempotency
  - Security: RLS, AuthN/AuthZ, license validation, SBOM
  - Maintainability: SOLID, documentation, test coverage >85%
  - Testability: isolation, integration, E2E
  - Extensibility: plugin system, swappable adapters
  - Documentation: setup, architecture, concepts, ADRs
  - Developer experience: intuitive, good IDE support, easy setup
  - i18n: localized API responses
  - Licensing: robust validation
  - Compliance: SBOM, design for certification
- User experience requirements:
  - Clear error feedback, easy onboarding, consistent APIs
- Integration requirements:
  - PostgreSQL, Redis, nestjs-i18n, casl, OpenAPI, SBOM tooling
- Compliance requirements:
  - ISO 27001, SOC2 readiness, SBOM, audit trail (future)

## Epic List

### Epic-1: Framework Core & Architecture

### Epic-2: Multi-Tenancy & Control Plane

### Epic-3: Security & Compliance

### Epic-4: Observability & Testing

### Epic-5: Documentation & Developer Experience

### Epic-N: Future Enhancements (Advanced AuthN, Audit Trail, Admin UIs, etc.)

## Epic 1: Story List

- Story 1: Monorepo and Project Template Setup
  Status: ''
  Requirements:
  - Set up Nx monorepo
  - Provide project scaffolding for apps/libs

- Story 2: Core Architecture Implementation
  Status: ''
  Requirements:
  - Implement Hexagonal Architecture base
  - Set up CQRS/ES mechanisms
  - Establish plugin system basics

- Story 3: Multi-Tenancy Foundation
  Status: ''
  Requirements:
  - Implement tenant_id context and RLS via MikroORM Filters
  - Provide tenant context propagation

- Story 4: Control Plane API
  Status: ''
  Requirements:
  - Create API for tenant CRUD
  - Admin AuthZ for control plane

- Story 5: Security & AuthN/AuthZ
  Status: ''
  Requirements:
  - Integrate JWT and local AuthN
  - Implement RBAC/ABAC (casl)
  - Security headers, rate limiting

- Story 6: Observability & Health Checks
  Status: ''
  Requirements:
  - Structured logging
  - Health endpoints

- Story 7: License Validation
  Status: ''
  Requirements:
  - Implement license validation module

- Story 8: Testing Framework
  Status: ''
  Requirements:
  - Set up unit, integration, and E2E testing

- Story 9: Documentation & SBOM
  Status: ''
  Requirements:
  - Provide setup and architecture docs
  - Integrate SBOM generation

## Technology Stack

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

## Reference

- See docs/PRD.md and docs/ARCH.md for detailed requirements and architecture
- Mermaid diagrams and further visualizations to be added in architecture document

## Data Models, API Specs, Schemas, etc

- Event Store: events table with stream_id, version, event_type, payload, timestamp, tenant_id
- Read Models: tenant-aware tables with tenant_id
- User, Tenant, Role, Permission entities (see RBAC/ABAC)
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

## Change Log

| Change        | Story ID | Description                |
| ------------- | -------- | -------------------------- |
| Initial draft | N/A      | Initial PRD for ACCI EAF   |

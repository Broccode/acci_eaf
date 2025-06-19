# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Project Overview

This is the ACCI Enterprise Application Framework (EAF), a Kotlin/Gradle multi-module monorepo with
Nx workspace management. It combines backend microservices with React frontend components in an
event-driven architecture.

## Architecture

### Multi-Module Structure

- **apps/**: Deployable applications (iam-service, ticket-management-service)
- **libs/**: Shared libraries (eaf-core, eaf-eventing-sdk, eaf-eventsourcing-sdk, eaf-iam-client,
  ui-foundation-kit)
- **tools/**: Development tooling (acci-eaf-cli)
- **docs/**: Docusaurus documentation site
- **infra/**: Docker Compose stack (NATS + PostgreSQL)

### Key Patterns

- **Hexagonal Architecture**: Clear port/adapter separation with inbound/outbound adapters
- **CQRS/Event Sourcing**: Full event sourcing implementation with JDBC event store
- **Multi-Tenancy**: NATS account-based tenant isolation
- **Event-Driven**: NATS JetStream for async communication between services

## Essential Commands

### Build & Run

```bash
# Build specific service
npx nx build iam-service
npx nx build ticket-management-service

# Run services locally
npx nx run iam-service:run
npx nx run ticket-management-service:run

# Build all projects
npx nx run-many -t build

# Build only affected projects
npx nx affected -t build
```

### Testing

```bash
# Test specific service
npx nx test iam-service
npx nx test ticket-management-service

# Run all tests
npx nx run-many -t test

# Test only affected projects
npx nx affected -t test
```

### Frontend Development

```bash
# Component library development with Storybook
npx nx storybook ui-foundation-kit

# Build UI components
npx nx build ui-foundation-kit

# Serve documentation site
npx nx serve docs
```

### Infrastructure

```bash
# Start local development stack
cd infra/docker-compose
docker-compose up -d

# Stop infrastructure
docker-compose down
```

## Technology Stack

### Backend

- **Kotlin 2.0.21** with **Spring Boot 3.3.1**
- **JDK 21** with Gradle Kotlin DSL
- **PostgreSQL 15** for data persistence
- **NATS JetStream 2.17.2** for event streaming
- **Testcontainers** for integration testing

### Frontend

- **React 18.3.1** with **TypeScript 5.6.2**
- **Vaadin 24.4.12** for full-stack integration
- **Vite 5.4.2** for build tooling
- **Storybook 8.6.14** for component development

## Code Organization

### Service Structure (apps/)

Each service follows hexagonal architecture:

- `domain/`: Core business logic and aggregates
- `infrastructure/adapter/inbound/`: Web controllers, event handlers
- `infrastructure/adapter/outbound/`: Repository implementations, external clients
- `application/`: Use cases and application services

### Shared Libraries (libs/)

- **eaf-core**: Base classes, security context, common utilities
- **eaf-eventing-sdk**: NATS integration, event publishing/consuming
- **eaf-eventsourcing-sdk**: CQRS framework, event store, aggregates
- **eaf-iam-client**: Authentication filters, JWT handling
- **ui-foundation-kit**: React component library with design system

## Development Workflow

### Multi-Tenancy

- System uses NATS accounts for tenant isolation
- Event subjects follow pattern: `TENANT_<ID>.<service>.<event>`
- Security context provides tenant information throughout request lifecycle

### Event Sourcing

- Aggregates extend `AbstractAggregateRepository`
- Events stored in PostgreSQL via JDBC event store
- Projectors handle event replay and view materialization

### Testing Strategy

- **Unit tests**: MockK for Kotlin, Jest for TypeScript
- **Integration tests**: Testcontainers for PostgreSQL/NATS
- **Architecture tests**: ArchUnit to enforce patterns
- **Component tests**: Storybook testing addon

### Code Quality

- **Kotlin**: Spotless + KtLint formatting
- **TypeScript**: ESLint + Prettier
- **Git hooks**: Pre-commit formatting enforcement

## Key Documentation References

For comprehensive project understanding, refer to these essential documents:

- **[Product Requirements Document](docs/prd.md)** - Complete functional and non-functional
  requirements, user stories, and MVP scope
- **[Architecture Document](docs/architecture.md)** - Technical architecture, design patterns,
  component structure, and technology stack
- **[Frontend Architecture](docs/frontend-architecture.md)** - Frontend-specific architecture using
  Vaadin/Hilla/React
- **[Project Brief](docs/PROJECT_BRIEF.md)** - High-level project overview, scope, stakeholders, and
  objectives
- **[Operational Guidelines](docs/operational-guidelines.md)** - Coding standards, testing strategy,
  security practices, and development workflows

These documents provide the complete context for architectural decisions, development practices, and
project requirements that guide all development work.

## Workspace Commands

```bash
# View project graph
npx nx graph

# Show all projects
npx nx show projects

# Run specific target across projects
npx nx run-many -t <target>

# Cache management
npx nx reset
```

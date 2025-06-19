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

## Comprehensive Documentation Index

### **Core Project Documents**

- **[Product Requirements Document](docs/prd.md)** - Complete functional and non-functional
  requirements, user stories, and MVP scope
- **[Architecture Document](docs/architecture.md)** - Technical architecture, design patterns,
  component structure, and technology stack
- **[Frontend Architecture](docs/frontend-architecture.md)** - Frontend-specific architecture using
  Vaadin/Hilla/React
- **[Project Brief](docs/PROJECT_BRIEF.md)** - High-level project overview, scope, stakeholders, and
  objectives
- **[MVP Scope Outline](docs/MVP_SCOPE_OUTLINE.md)** - Detailed MVP boundaries, success criteria,
  and core framework components
- **[Operational Guidelines](docs/operational-guidelines.md)** - Coding standards, testing strategy,
  security practices, and development workflows

### **Technical Specifications**

- **[Tech Stack](docs/tech-stack.md)** - Complete technology stack with versions and justifications
- **[Component View](docs/component-view.md)** - System components and their interactions
- **[API Reference](docs/api-reference.md)** - REST APIs, event schemas, and integration patterns
- **[Data Models](docs/data-models.md)** - Domain objects, event store structure, and multi-tenant
  data models

### **Development Setup & Workflow**

- **[System Initialization Guide](docs/system-initialization-guide.md)** - Streamlined single-tenant
  EAF setup and configuration
- **[Formatting Guide](docs/FORMATTING.md)** - Comprehensive formatting setup to prevent CI failures
- **[CI Troubleshooting](docs/CI-TROUBLESHOOTING.md)** - Diagnose and fix CI failures, environment
  differences
- **[Environment Variables](docs/environment-vars.md)** - Environment variable management and
  secrets

### **Pilot Project & Implementation**

- **[Ticket Management Pilot](docs/pilots/ticket-management-brief.md)** - Complete pilot application
  specification and EAF validation
- **[Story Implementation Status](docs/stories/)** - Detailed progress tracking of all MVP user
  stories (74 story files)
  - **Key Success**: Story 2.8.2 - Pilot development completed with 74/74 tests passing
  - **Complete**: Infrastructure, CLI generation, event sourcing, IAM integration

### **Troubleshooting & Best Practices**

- **[Context Propagation Issues](docs/troubleshooting/coroutine-context-propagation-issue.md)** -
  Coroutine context management and solutions
- **[IAM Integration Testing](docs/troubleshooting/iam-integration-test-solution-attempts.md)** -
  Test configuration and Spring Boot integration
- **[MockK Best Practices](docs/troubleshooting/mockk-best-practices.md)** - Comprehensive MockK
  usage guide for Kotlin testing

### **Architecture Deep Dive (Docusaurus)**

The `docs/docusaurus-docs/` directory contains detailed architectural documentation:

- **Architecture Patterns**: DDD, CQRS/Event Sourcing, Hexagonal Architecture, TDD guidelines
- **Core Services**: Context propagation, IAM client SDK, NATS integration, event consumption
- **SDK Reference**: Complete API documentation for eventing, event sourcing, and IAM client SDKs
- **Getting Started**: Prerequisites, local setup, first service creation, troubleshooting
- **Developer Tools**: CLI usage and project scaffolding

## Project Status Summary

### **MVP Completion Status: ✅ PRODUCTION READY**

The ACCI EAF has successfully completed its MVP with a comprehensive pilot project:

**Key Achievements:**

- **✅ Complete Framework**: All core services, SDKs, and infrastructure components delivered
- **✅ Pilot Validation**: Ticket management service with 74/74 tests passing (100% success rate)
- **✅ Production UI**: Modern React frontend with Tailwind CSS and complete CRUD operations
- **✅ Architecture Compliance**: 100% ArchUnit compliance with >90% test coverage
- **✅ SDK Integration**: All EAF SDKs work seamlessly without workarounds
- **✅ Multi-tenancy**: NATS account-based tenant isolation fully implemented
- **✅ Event Sourcing**: Complete CQRS/ES implementation with PostgreSQL event store
- **✅ Developer Experience**: Service scaffolding to working endpoints in <15 minutes

**Core Capabilities Validated:**

- **Event-Driven Architecture**: NATS JetStream messaging with reliable event processing
- **Identity & Access Management**: Multi-tenant IAM with RBAC and federation support
- **Development Tooling**: CLI scaffolding, comprehensive documentation, testing infrastructure
- **UI Foundation**: React component library with Storybook and design system
- **Operational Excellence**: CI/CD pipelines, formatting automation, architectural testing

The framework is ready for production use with proven patterns, comprehensive tooling, and excellent
developer experience.

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

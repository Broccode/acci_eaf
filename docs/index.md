# ACCI EAF Documentation Index

This index provides a central catalog for the sharded documentation of the ACCI Enterprise
Application Framework (EAF).

## I. Project Overview & Planning

- [Project Brief](./PROJECT_BRIEF.md) - Comprehensive project definition, scope, and stakeholder
  analysis for ACCI EAF
- [Project Brief (Post-MVP)](./PROJECT_BRIEF_POST_MVP.md) - Extended project planning beyond the
  initial MVP
- [Product Requirements Document (PRD)](./prd.md) - Complete product requirements and feature
  specifications
- [MVP Scope Outline](./MVP_SCOPE_OUTLINE.md) - Detailed breakdown of MVP deliverables and scope

## II. Project Requirements (Epics from PRD)

- [Epic 1: Foundational Engineering & DevEx Setup](./prd/epic-1.md) - Details the setup of the core
  development environment, CI/CD, and quality gates
- [Epic 2.3: Core Event Store Foundation (Postgres-based ES)](./prd/epic-2.3.md) - Describes the
  PostgreSQL-based event store foundation
- [Epic 2.4: Kotlin/Spring CQRS/ES Application Framework](./prd/epic-2.4.md) - Outlines the
  framework for building event-sourced services in Kotlin/Spring
- [Epic 2.5: Basic IAM & Multi-Tenancy](./prd/epic-2.5.md) - Details core multi-tenancy, IAM, and
  RBAC for the MVP
- [Epic 2.6: Scaffolding CLI ("ACCI EAF CLI")](./prd/epic-2.6.md) - Describes the command-line
  interface for accelerating development
- [Epic 2.7: Developer Portal & Documentation ("The Launchpad")](./prd/epic-2.7.md) - Covers the
  setup and content for the developer portal
- [Epic 2.8: Enablement of a Pilot Service/Module](./prd/epic-2.8.md) - Details the plan for
  validating the EAF MVP with a pilot service

## III. System Architecture

- [Architecture Overview](./architecture.md) - Comprehensive architectural documentation including
  hexagonal architecture, CQRS/ES, and component design
- [Frontend Architecture](./frontend-architecture.md) - Complete frontend architecture specification
  for Vaadin/Hilla/React applications
- [API Reference](./api-reference.md) - Conceptual overview of internal and external APIs for the
  EAF MVP
- [Component View & Design Patterns](./component-view.md) - Describes the architectural design
  patterns adopted and the logical components of the EAF MVP, including C4 diagrams
- [Core Workflows & Sequence Diagrams](./sequence-diagrams.md) - Illustrates key operational flows
  like tenant provisioning and CQRS interactions
- [Data Models](./data-models.md) - Conceptual data models for core services like IAM, Event Store,
  License Management, and Feature Flags
- [Project Structure](./project-structure.md) - Overview of the monorepo structure for the ACCI EAF,
  managed by Nx

## IV. Frontend Architecture & Development

- [API Interaction Layer (Frontend)](./front-end-api-interaction.md) - How the frontend interacts
  with backend Hilla endpoints, including error handling
- [Component Guide (Frontend)](./front-end-component-guide.md) - Details on component naming,
  organization, and a template for component specification
- [Coding Standards (Frontend)](./front-end-coding-standards.md) - Specific coding standards for
  ACCI EAF frontend development using Vaadin/Hilla with React and TypeScript
- [Project Structure (Frontend)](./front-end-project-structure.md) - Detailed directory structure
  for Hilla-based frontend applications and the UI Foundation Kit
- [Routing Strategy (Frontend)](./front-end-routing-strategy.md) - Describes the use of Hilla's
  file-based routing, route definitions, and protection mechanisms
- [State Management (Frontend)](./front-end-state-management.md) - In-depth look at the state
  management philosophy, prioritizing local state and React Context API
- [Styling Guide (Frontend)](./front-end-style-guide.md) - Approach to styling using Vaadin Lumo
  theme customization and CSS Modules
- [Testing Strategy (Frontend)](./front-end-testing-strategy.md) - Specifics of frontend testing,
  including component, feature/flow, and E2E testing

## V. Infrastructure & Operations

- [Infrastructure and Deployment Overview](./infra-deployment.md) - Details on deployment targets,
  IaC, CI/CD strategy, and environments
- [Technology Stack](./tech-stack.md) - Definitive list of technologies, frameworks, and tools
  selected for the EAF
- [Environment Variables](./environment-vars.md) - Guidelines on how environment variables are used
  for configuration and secrets management
- [System Initialization Guide](./system-initialization-guide.md) - Step-by-step guide for
  initializing the EAF system and setting up default configurations
- [Operational Guidelines](./operational-guidelines.md) - Consolidated guidelines covering Error
  Handling, Coding Standards (Backend & Kotlin), Overall Testing Strategy, and Security Best
  Practices

## VI. Development & Quality Assurance

- [Formatting Standards](./FORMATTING.md) - Code formatting rules and standards for consistent code
  quality
- [CI/CD Troubleshooting](./CI-TROUBLESHOOTING.md) - Comprehensive troubleshooting guide for
  continuous integration and deployment issues
- [Key Reference Documents](./key-references.md) - Lists critical documents related to the ACCI EAF
  project

## VII. Implementation Stories

### Epic 1: Foundation Stories

- [Story 1.1: Repository Setup & Initial CI/CD](./stories/1.1.story.md)
- [Story 1.2: Quality Gates & Code Standards](./stories/1.2.story.md)
- [Story 1.3: Monorepo Structure with Nx](./stories/1.3.story.md)
- [Story 1.4: Docker & Local Development](./stories/1.4.story.md)
- [Story 1.5: NATS Infrastructure Setup](./stories/1.5.story.md)

### Epic 2: Core Platform Stories

- [Story 2.1: NATS/JetStream Integration](./stories/2.1.story.md)
- [Story 2.3: Event Store Foundation](./stories/2.3.story.md)
- [Story 2.3.1: Event Store SDK Implementation](./stories/2.3.1.story.md)
- [Story 2.4: CQRS/ES Framework](./stories/2.4.story.md)
- [Story 2.4.1: Command Processing](./stories/2.4.1.story.md)
- [Story 2.4.2: Event Publishing](./stories/2.4.2.story.md)
- [Story 2.4.3: Event Consumption](./stories/2.4.3.story.md)
- [Story 2.4.4: Query Processing](./stories/2.4.4.story.md)
- [Story 2.4.5: Aggregate Repository](./stories/2.4.5.story.md)

### Epic 2.5: IAM & Multi-tenancy Stories

- [Story 2.5.1: SuperAdmin Tenant Provisioning](./stories/2.5.1.story.md)
- [Story 2.5.2: Single-Tenant EAF Setup](./stories/2.5.2.story.md)
- [Story 2.5.3: TenantAdmin User Management](./stories/2.5.3.story.md)
- [Story 2.5.4: EAF IAM SDK for Authentication & RBAC](./stories/2.5.4.story.md)
- [Story 2.5.5: Security Context & Tenant Isolation](./stories/2.5.5.story.md)

### Epic 2.6: CLI Tooling Stories

- [Story 2.6.1: CLI Foundation & Service Generation](./stories/2.6.1.story.md)
- [Story 2.6.2: Advanced CLI Features](./stories/2.6.2.story.md)
- [Story 2.6.3: CLI Integration & Testing](./stories/2.6.3.story.md)

### Epic 2.7: Developer Portal Stories

- [Story 2.7.1: Docusaurus Setup & Structure](./stories/2.7.1.story.md)
- [Story 2.7.2: Documentation Content & Examples](./stories/2.7.2.story.md)
- [Story 2.7.3: Interactive Documentation Features](./stories/2.7.3.story.md)
- [Story 2.7.4: Documentation Automation](./stories/2.7.4.story.md)

### Epic 2.8: Pilot Implementation Stories

- [Story 2.8.1: Pilot Service Architecture](./stories/2.8.1.story.md)
- [Story 2.8.2: Ticket Management Service Implementation](./stories/2.8.2.story.md)
- [Story 2.8.3: Stateless Security & Frontend Integration](./stories/2.8.3.story.md)

### Epic 3: Advanced Features

- [Story 3.1.1: Advanced Multi-tenancy Features](./stories/3.1.1.story.md)
- [Story 3.1.2: Enhanced Security & Federation](./stories/3.1.2.story.md)

## VIII. Pilot Projects

- [Ticket Management Service Brief](./pilots/ticket-management-brief.md) - Overview and requirements
  for the ticket management pilot application

## IX. Troubleshooting & Support

- [IAM Integration Test Solutions](./troubleshooting/iam-integration-test-solution-attempts.md) -
  Comprehensive guide for resolving IAM integration testing issues
- [MockK Best Practices](./troubleshooting/mockk-best-practices.md) - Best practices and common
  patterns for using MockK in Kotlin tests
- [Coroutine Context Propagation Issues](./troubleshooting/coroutine-context-propagation-issue.md) -
  Solutions for context propagation problems in Kotlin coroutines

## X. Developer Portal Setup

- [Docusaurus Favicon Setup](./setup-favicon.md) - Guide for setting up custom favicons in the
  Docusaurus developer portal

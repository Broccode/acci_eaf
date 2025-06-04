# ACCI EAF Documentation Index

This index provides a central catalog for the sharded documentation of the ACCI Enterprise
Application Framework (EAF).

## I. Project Requirements (Epics from PRD)

- [Epic 1: Foundational Engineering & DevEx Setup](./epic-1.md) - Details the setup of the core
  development environment, CI/CD, and quality gates.
- [Epic 2: Core Eventing Backbone (NATS-based)](./epic-2.md) - Covers the NATS-based eventing system
  for EDA and CQRS/ES.
- [Epic 2.3: Core Event Store Foundation (Postgres-based ES)](./epic-2.3.md) - Describes the
  PostgreSQL-based event store foundation.
- [Epic 2.4: Kotlin/Spring CQRS/ES Application Framework](./epic-2.4.md) - Outlines the framework
  for building event-sourced services in Kotlin/Spring.
- [Epic 2.5: Basic IAM & Multi-Tenancy](./epic-2.5.md) - Details core multi-tenancy, IAM, and RBAC
  for the MVP.
- [Epic 2.6: Scaffolding CLI ("ACCI EAF CLI")](./epic-2.6.md) - Describes the command-line interface
  for accelerating development.
- [Epic 2.7: Developer Portal & Documentation ("The Launchpad")](./epic-2.7.md) - Covers the setup
  and content for the developer portal.
- [Epic 2.8: Enablement of a Pilot Service/Module](./epic-2.8.md) - Details the plan for validating
  the EAF MVP with a pilot service.

## II. System Architecture

- [API Reference](./api-reference.md) - Conceptual overview of internal and external APIs for the
  EAF MVP.
- [Component View & Design Patterns](./component-view.md) - Describes the architectural design
  patterns adopted and the logical components of the EAF MVP, including C4 diagrams.
- [Core Workflows & Sequence Diagrams](./sequence-diagrams.md) - Illustrates key operational flows
  like tenant provisioning and CQRS interactions.
- [Data Models](./data-models.md) - Conceptual data models for core services like IAM, Event Store,
  License Management, and Feature Flags.
- [Environment Variables](./environment-vars.md) - Guidelines on how environment variables are used
  for configuration and secrets management.
- [Infrastructure and Deployment Overview](./infra-deployment.md) - Details on deployment targets,
  IaC, CI/CD strategy, and environments.
- [Key Reference Documents](./key-references.md) - Lists critical documents related to the ACCI EAF
  project.
- [Operational Guidelines](./operational-guidelines.md) - Consolidated guidelines covering Error
  Handling, Coding Standards (Backend & Kotlin), Overall Testing Strategy, and Security Best
  Practices.
- [Project Structure](./project-structure.md) - Overview of the monorepo structure for the ACCI EAF,
  managed by Nx.
- [Technology Stack](./tech-stack.md) - Definitive list of technologies, frameworks, and tools
  selected for the EAF.

## III. Front-End Architecture

- [API Interaction Layer (Frontend)](./front-end-api-interaction.md) - How the frontend interacts
  with backend Hilla endpoints, including error handling.
- [Component Guide (Frontend)](./front-end-component-guide.md) - Details on component naming,
  organization, and a template for component specification.
- [Coding Standards (Frontend)](./front-end-coding-standards.md) - Specific coding standards for
  ACCI EAF frontend development using Vaadin/Hilla with React and TypeScript.
- [Project Structure (Frontend)](./front-end-project-structure.md) - Detailed directory structure
  for Hilla-based frontend applications and the UI Foundation Kit.
- [Routing Strategy (Frontend)](./front-end-routing-strategy.md) - Describes the use of Hilla's
  file-based routing, route definitions, and protection mechanisms.
- [State Management (Frontend)](./front-end-state-management.md) - In-depth look at the state
  management philosophy, prioritizing local state and React Context API.
- [Styling Guide (Frontend)](./front-end-style-guide.md) - Approach to styling using Vaadin Lumo
  theme customization and CSS Modules.
- [Testing Strategy (Frontend)](./front-end-testing-strategy.md) - Specifics of frontend testing,
  including component, feature/flow, and E2E testing.

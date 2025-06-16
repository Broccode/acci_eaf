# Project Brief: Simple Ticket Management System (EAF Pilot)

## 1. Problem Statement

To validate the ACCI EAF, we need a simple but representative application that exercises its core
features. Developers need a way to track issues, bugs, or tasks. A simple ticket management system
provides a familiar domain to model, allowing us to focus on validating the EAF's capabilities
rather than getting lost in complex business logic.

This pilot will serve as a practical, hands-on test of the EAF's developer experience, SDK
usability, and architectural enforcement.

## 2. Core Features (MVP Scope)

The pilot will implement the following core features:

- **Ticket Creation**: An authenticated user can create a new support ticket with a title and a
  description.
- **Ticket Assignment**: An authenticated user can assign an open ticket to another registered user.
- **Ticket Closure**: An authenticated user can close a ticket, marking it as resolved.
- **Ticket View**: An authenticated user can view a list of all open tickets.

## 3. EAF Feature Validation

This pilot is designed to touch upon and validate the following key features of the ACCI EAF:

- **Event Sourcing/CQRS (`eaf-eventsourcing-sdk`)**:
  - **Aggregate**: `Ticket` will be the central event-sourced aggregate.
  - **Commands**: `CreateTicket`, `AssignTicket`, `CloseTicket`.
  - **Events**: `TicketCreated`, `TicketAssigned`, `TicketClosed`.
- **Projections**:
  - A `TicketSummaryProjector` will listen to domain events via NATS and update a denormalized
    `ticket_summary` read model table for efficient querying.
- **Event-Driven Architecture (`eaf-eventing-sdk`)**:
  - The `Ticket` aggregate will publish its domain events to NATS.
  - The `TicketSummaryProjector` will consume these events.
- **IAM Integration (`eaf-iam-client`)**:
  - All API endpoints will be secured, requiring an authenticated user.
  - The `userId` of the creator and assignee will be stored, demonstrating context propagation.
- **UI Foundation Kit (`ui-foundation-kit`)**:
  - A simple React/Hilla UI will be built to interact with the service, using components from the
    shared library.
- **Scaffolding (`acci-eaf-cli`)**:
  - The entire backend service will be scaffolded using the `eaf generate service` command to
    validate its effectiveness.

## 4. Success Criteria (EAF Validation)

The success of this pilot will be measured against these EAF-centric criteria:

#### Developer Experience (DevEx)

- **Onboarding Speed**: A developer can scaffold the new service using `eaf generate service` and
  have a running "hello world" endpoint accessible in **under 15 minutes**.
- **Development Velocity**: A developer can implement the full `CreateTicket` flow (Command, Event,
  Aggregate logic) in **under 1 hour**, leveraging the EAF SDKs.

#### Architectural Adherence & Quality

- **Code Generation**: The `acci-eaf-cli` must generate a project structure that is 100% compliant
  with the Hexagonal Architecture rules defined in ArchUnit tests.
- **Architectural Integrity**: The final pilot service code must pass all ArchUnit tests without any
  violations.
- **TDD Enforcement**: TDD must be followed for all new logic. The final code coverage for new
  domain and application logic must be **above 90%**.

#### SDK & Framework Usability

- **Event Sourcing SDK**: The `eaf-eventsourcing-sdk` abstractions (`AbstractAggregateRoot`,
  `AggregateRepository`) must be sufficient to implement the `Ticket` aggregate without requiring
  workarounds.
- **Eventing SDK**: The `eaf-eventing-sdk` must reliably publish all domain events from the
  aggregate to NATS.
- **IAM Client SDK**: The `eaf-iam-client` must secure all API endpoints with minimal configuration.

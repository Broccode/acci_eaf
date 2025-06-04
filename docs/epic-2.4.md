**Epic 2.4: Kotlin/Spring CQRS/ES Application Framework - MVP**

- **Goal:** Provide developers with the necessary SDKs, base classes, annotations, and patterns to
  build event-sourced services in Kotlin/Spring, adhering to DDD and Hexagonal Architecture.
- **User Stories:**
  1. **Story 2.4.1: SDK Abstractions for Event-Sourced Aggregates**
     - As a Backend Developer (Majlinda), I want to use ACCI EAF\'s Kotlin/Spring SDK to easily
       define and implement event-sourced Aggregates that seamlessly integrate with the EAF\'s Event
       Store SDK and Eventing Bus SDK, so that I can focus on my core domain logic rather than
       complex CQRS/ES plumbing.
     - **Acceptance Criteria:**
       1. The `eaf-eventsourcing-sdk` (potentially leveraging parts of Axon Framework open-source
          library if decided, as per `Axon Framework in Hexagonal EAF`) provides base classes,
          interfaces, or annotations (e.g., `@EafAggregate`, `@EafCommandHandler`,
          `@EafEventSourcingHandler`) to simplify the definition of Aggregates in Kotlin.
       2. The SDK facilitates common Aggregate operations: loading from history (events + optional
          snapshot) via the EAF Event Store SDK, applying new events, and having new events
          persisted by the EAF Event Store SDK and published via the EAF Eventing SDK (potentially
          using Unit of Work concepts for consistency).
       3. Aggregates developed using the EAF SDK MUST adhere to Hexagonal Architecture principles,
          ensuring domain logic within the aggregate is isolated from direct infrastructure concerns
          (persistence and event publishing are handled via SDK-provided ports/mechanisms).
       4. Aggregates MUST implement optimistic concurrency control using sequence numbers, with
          conflicts handled and surfaced by the EAF Event Store SDK interactions.
       5. All state changes within aggregates MUST be derived exclusively from domain events applied
          via EAF SDK-provided mechanisms (e.g., methods analogous to `@EventSourcingHandler`).
       6. \"The Launchpad\" provides clear examples and documentation for defining and testing EAF
          Aggregates using these SDK abstractions.
  2. **Story 2.4.2: SDK Support for Hexagonal Architecture in Services**
     - As a Backend Developer (Michael), I want the ACCI EAF Kotlin/Spring SDK and Scaffolding CLI
       outputs to provide clear structures and base classes/interfaces that naturally guide me in
       building services according to Hexagonal Architecture, ensuring my application and domain
       logic remain well-isolated from infrastructure concerns.
     - **Acceptance Criteria:**
       1. The `eaf-core` SDK module provides interfaces or abstract classes for defining Ports
          (e.g., `InboundPort<C, R>` for command processing, `OutboundPort` for infrastructure
          interactions).
       2. The Scaffolding CLI (`acci-eaf-cli`) when generating new Kotlin/Spring services (Story
          2.6.1) creates a directory structure reflecting Hexagonal layers (e.g., `domain`,
          `application/port/in`, `application/port/out`, `infrastructure/adapter/in/web`,
          `infrastructure/adapter/out/persistence`, `infrastructure/adapter/out/messaging`).
       3. \"The Launchpad\" documents how to implement Adapters for common infrastructure concerns
          (e.g., REST controllers as inbound adapters using Spring WebMVC/WebFlux, NATS consumers as
          inbound adapters using `eaf-eventing-sdk`, PostgreSQL repositories as outbound adapters
          using Spring Data JPA or JDBC with EAF helpers).
  3. **Story 2.4.3: SDK Support for Idempotent Projectors**
     - As a Backend Developer (Majlinda), I want the ACCI EAF Kotlin/Spring SDK to offer
       straightforward patterns and helper utilities for building idempotent Projectors that consume
       domain events from NATS (via the EAF Eventing SDK) and reliably update read models (e.g., in
       PostgreSQL), so I can efficiently create queryable views of data.
     - **Acceptance Criteria:**
       1. The `eaf-eventing-sdk` or `eaf-eventsourcing-sdk` provides clear patterns or abstractions
          (e.g., base classes, annotations like `@EafProjectorEventHandler`) for creating Projector
          components as Spring beans that listen to specific domain events.
       2. Projectors MUST consume events via the EAF Eventing SDK and rigorously handle potential
          event redeliveries to ensure idempotency in read model updates (e.g., by checking if an
          event ID has already been processed or by using UPSERT database operations on the read
          model, potentially with a `processed_event_id` tracking mechanism).
       3. Projectors, when updating read models (e.g., in PostgreSQL), MUST strictly enforce tenant
          data isolation (e.g., by incorporating `tenant_id` in all read model queries and updates,
          derived from event metadata).
       4. \"The Launchpad\" provides documented examples of building Projectors, including
          strategies for managing read model schemas, handling errors during projection, and dealing
          with eventual consistency.
  4. **Story 2.4.4: Secure EAF Context Access for Services**
     - As a Backend Developer (Michael, Majlinda), I want Kotlin/Spring services built using the
       ACCI EAF application framework to have a simple and secure way to access essential
       operational context from other EAF services (like tenant/user from IAM, configuration,
       feature flags, license entitlements) via the EAF SDK.
     - **Acceptance Criteria:**
       1. The `eaf-core` SDK module or relevant client SDK modules (e.g., `eaf-iam-client`) provide
          mechanisms (e.g., injectable services, request-scoped beans managed by Spring, utility
          classes for context propagation) for EAF services to securely obtain the current
          operational context.
       2. This context includes at least the validated `tenant_id`, `user_id` (if an authenticated
          user session exists), and associated roles/permissions from the IAM service.
       3. The SDK provides helpers for accessing feature flag evaluations (via
          `eaf-featureflag-client`) and application configurations.
       4. Access to this context is consistently available and propagated correctly across
          asynchronous operations within a request or event processing flow (e.g., within Kotlin
          coroutines using `CoroutineContext` elements, or via message metadata for event handlers).

**Epic 2: Core Eventing Backbone (NATS-based) - MVP**

- **Goal:** Provide a reliable and usable eventing system for EDA and CQRS/ES within ACCI EAF.
- **User Stories:**
  1. **Story 2.1 (was PRD Story 2.1 / TS-INFRA-01): Establish NATS/JetStream Core Infrastructure for
     MVP**
     - As an EAF System, I need the NATS/JetStream core infrastructure configured for local
       development (via Docker Compose) and initial MVP deployment considerations, including basic
       multi-account setup for tenant isolation, so that the eventing backbone is operational.
     - **Acceptance Criteria:**
       1. A documented `docker-compose.yml` file starts a NATS server with JetStream enabled and
          basic persistence for local development.
       2. The NATS server configuration within Docker Compose defines at least two distinct tenant
          NATS Accounts (e.g., `TenantA_Account`, `TenantB_Account`) and a system account, each with
          appropriately scoped user credentials and permissions for tenant-prefixed subjects and
          essential JetStream API access.
       3. JetStream is enabled for tenant accounts with basic file-based persistence configured in
          the local setup.
       4. \"The Launchpad\" contains documentation for running, connecting to (with example
          credentials for different tenant contexts), and basic troubleshooting of the local NATS
          environment.
  2. **Story 2.2 (was PRD Story 2.2 / TS-PATTERNS-01 - Conditional): Implement & Validate
     Transactional Outbox Pattern for NATS Event Publishing**
     - (If confirmed necessary by Architect for MVP reliability based on
       `Axon Framework in Hexagonal EAF` (section 4.a)) As the EAF Eventing SDK, I need to implement
       and validate the Transactional Outbox pattern for publishing events to NATS, so that
       at-least-once delivery and consistency with event store persistence are robustly ensured.
     - **Acceptance Criteria:**
       1. An \"outbox\" table schema is defined for PostgreSQL.
       2. The EAF Event Sourcing SDK (or a dedicated component) integrates outbox message writes
          into the same database transaction as domain event persistence.
       3. A separate, reliable poller mechanism (e.g., a Spring Boot scheduled task or a dedicated
          process) reads from the outbox table and publishes messages to NATS via the EAF Eventing
          SDK.
       4. The poller handles transient NATS publishing errors with retries and logs persistent
          failures.
       5. Successfully published messages are marked as processed or deleted from the outbox.
       6. Idempotency considerations for message delivery by the poller are addressed.
  3. **Story 2.3 (was PRD Story 2.3): EAF NATS SDK - Event Publishing**
     - As a Backend Developer (Majlinda), I want to use the ACCI EAF SDK (Kotlin/Spring) to easily
       and reliably publish domain events (as JSON payloads) from my service to a NATS JetStream
       stream with an at-least-once delivery guarantee, ensuring that critical business events are
       captured and broadcast.
     - **Acceptance Criteria:**
       1. The `eaf-eventing-sdk` module provides a `NatsEventPublisher` service (or equivalent
          abstraction).
       2. The publisher service connects to NATS using centrally managed EAF application
          configuration and correctly uses tenant-specific credentials or NATS context.
       3. The publisher service accepts a Kotlin domain event object, serializes it to a JSON string
          as per EAF-defined conventions (e.g., including event type, timestamp, payload).
       4. The SDK utilizes JetStream `publish()` method and correctly handles the returned
          `PublishAck` to ensure at-least-once delivery semantics, including configurable retry
          mechanisms for transient publish failures detectable via `PublishAck`.
       5. All published events MUST include `tenant_id` (e.g., in NATS subject or message
          metadata/headers), propagated correctly by the SDK, aligning with the NATS multi-tenancy
          architecture defined by Fred.
       6. (Conditional) If Story 2.2 (Transactional Outbox) is implemented, this SDK publisher
          integrates with it seamlessly.
       7. Comprehensive unit and integration tests (using Testcontainers for NATS) demonstrate
          successful publishing, `PublishAck` handling, tenant context usage, and error scenarios.
  4. **Story 2.4 (was PRD Story 2.4): EAF NATS SDK - Event Consumption & Idempotency**
     - As a Backend Developer (Michael), I want to use the ACCI EAF SDK (Kotlin/Spring) to easily
       subscribe to and consume events in their correct order from a NATS JetStream stream,
       leveraging clear EAF-provided patterns and guidance for implementing idempotent event
       consumers, so that I can build reliable projectors and sagas.
     - **Acceptance Criteria:**
       1. The `eaf-eventing-sdk` provides convenient abstractions (e.g., annotations like
          `@NatsJetStreamListener` on methods, or a configurable listener container factory) for
          consuming messages from a durable JetStream consumer.
       2. The SDK handles JSON deserialization of event payloads from NATS messages to specified
          Kotlin data classes.
       3. The SDK ensures ordered processing of events as delivered by a single JetStream consumer
          instance (respecting JetStream\'s ordering guarantees).
       4. The EAF NATS SDK consumer MUST provide clear, easy-to-use mechanisms for message
          acknowledgment (`msg.ack()`), negative acknowledgment (`msg.nak()` potentially with a
          configurable delay/backoff strategy), and termination (`msg.term()`) based on the outcome
          of the event processing logic in the listener method.
       5. The EAF NATS SDK MUST allow consumers to be configured with durable names and to bind to
          server-defined JetStream consumer configurations, including specific Delivery Policies
          (e.g., `DeliverAllPolicy`, `DeliverNewPolicy`) and Ack Policies (`AckExplicitPolicy`
          should be the default for reliable processing).
       6. \"The Launchpad\" Developer Portal documents robust patterns and provides examples for
          writing idempotent event consumer logic using the SDK.
       7. The SDK respects tenant context for subscribing (e.g., subscribes to tenant-specific
          subjects or uses tenant-scoped credentials).
       8. Unit and integration tests (using Testcontainers for NATS) cover event consumption,
          various acknowledgment flows (ack/nak/term), and error handling by the consumer framework.

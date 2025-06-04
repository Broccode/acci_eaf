**Epic 2.3: Core Event Store Foundation (Postgres-based ES) - MVP**

- **Goal:** Establish a reliable and secure persistence mechanism for event-sourced aggregates.
- **User Stories:**
  1. **Story 2.3.1 (was PRD Story 2.3.1 / TS-INFRA-02): Implement EAF Event Store SDK - Core
     Persistence Logic for PostgreSQL**
     - As the EAF Development Team, I want to implement the core EAF Event Store SDK (Kotlin/Spring)
       providing logic for atomic event appends (with optimistic concurrency), event stream
       retrieval, basic snapshotting, and tenant data isolation against a PostgreSQL database, so
       that event sourcing capabilities are available to EAF services.
     - **Acceptance Criteria:**
       1. A detailed PostgreSQL schema for the event store (e.g., table `domain_events` with
          columns: `global_sequence_id BIGSERIAL PRIMARY KEY`, `event_id UUID NOT NULL UNIQUE`,
          `stream_id VARCHAR(255) NOT NULL` (e.g., `aggregateType-aggregateId`),
          `aggregate_id VARCHAR(255) NOT NULL`, `aggregate_type VARCHAR(255) NOT NULL`,
          `expected_version BIGINT`, `sequence_number BIGINT NOT NULL`,
          `tenant_id VARCHAR(255) NOT NULL`, `event_type VARCHAR(255) NOT NULL`,
          `payload JSONB NOT NULL`, `metadata JSONB`,
          `timestamp_utc TIMESTAMPTZ NOT NULL DEFAULT (now() AT TIME ZONE 'utc')`,
          `UNIQUE (tenant_id, aggregate_id, sequence_number)`,
          `INDEX idx_stream_id_seq (stream_id, sequence_number)`,
          `INDEX idx_tenant_event_type (tenant_id, event_type)`) is defined, documented, and
          version-controlled (e.g., using Flyway or Liquibase scripts).
       2. A separate table for snapshots (e.g., `aggregate_snapshots`) is similarly defined
          (`aggregate_id`, `tenant_id`, `last_sequence_number`, `snapshot_payload_jsonb`, `version`,
          `timestamp_utc`).
       3. The `eaf-eventsourcing-sdk` module (Kotlin/Spring) provides services/repositories for:
          - Atomically appending one or more domain events for a specific aggregate instance. This
            operation must include a check for optimistic concurrency against the provided
            `expected_version` (last known sequence number for the aggregate) and the current
            `sequence_number` for that `aggregate_id` and `tenant_id` in the database. The new
            events are assigned sequential `sequence_number`s.
          - Efficiently retrieving the complete and correctly ordered event stream (by
            `sequence_number`) for a given `aggregate_id` and `tenant_id`.
          - Storing and retrieving the latest aggregate snapshot for a given `aggregate_id` and
            `tenant_id`.
       4. All SDK database operations strictly enforce `tenant_id` isolation through WHERE clauses
          on `tenant_id` and by ensuring `tenant_id` is part of composite keys or unique constraints
          where appropriate.
       5. Appropriate indexing strategies are implemented on the event store tables to support
          efficient querying by `stream_id` (for rehydration), `tenant_id`, and potentially
          `event_type` or `timestamp_utc` for specific use cases.
       6. Comprehensive unit and integration tests (using Testcontainers for PostgreSQL) validate
          all SDK functionalities, including atomic appends, optimistic concurrency conflict
          scenarios, stream retrieval, snapshot operations, and strict tenant isolation.

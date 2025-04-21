# ADR-010: Strategy for Event Schema Evolution

* **Status:** Proposed
* **Date:** 2025-04-21
* **Stakeholders:** [Names or roles

## Context and Problem Statement

In long-lived Event Sourced systems, requirements change, and thus the structure of domain events evolves over time. A strategy is needed for how the system can handle older event versions when loading aggregates or building projections.

## Considered Options

1. **No Strategy / Big Bang Migration:** Old events are ignored or require complex, one-off data migrations of the entire event store. Very risky and costly.
2. **Weak Schemas / Tolerant Reader:** Store events as JSON and design handlers/aggregates to tolerate missing/extra fields. Simple initially, but implicit and error-prone for complex changes.
3. **Multiple Event Types:** Introduce a new event type for each change (`UserRegisteredV1`, `UserRegisteredV2`). Handlers/Aggregates need to handle all relevant versions. Can lead to many event types.
4. **Event Versioning and Upcasting:** Events have a version number. When loading old events, "upcaster" functions are applied to transform the old event structure into the current structure *before* processing. (`Event -> Upcaster -> CurrentEvent`). Requires maintaining upcaster code.

## Decision

We choose **Option 4: Event Versioning and Upcasting** as the preferred strategy for ACCI EAF.

1. Every event will receive metadata including `eventId` and `eventVersion`.
2. Payloads will be stored as `JSONB`.
3. The Event Store Adapter will be designed to support an upcasting pipeline (hooks for upcaster functions during event loading).
4. For V1, no complex upcasters will be implemented, but the architecture and event metadata will provide for it. The strategy will be documented.

## Consequences

* Design of the Event Store Adapter must accommodate upcasting.
* Event metadata (`eventVersion`) is required from the start.
* Developers must increment the version and implement upcasters when changing events (process needs definition).
* Provides a robust and explicit method for handling schema changes over time.

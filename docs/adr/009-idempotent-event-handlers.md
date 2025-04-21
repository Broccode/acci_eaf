# ADR-009: Support for Idempotent Event Handlers

* **Status:** Accepted
* **Date:** 2025-04-21
* **Stakeholders:** [Names or roles]

## Context and Problem Statement

In CQRS/ES, especially with "At-Least-Once" delivery guarantees from the event bus or during error/retry scenarios, event handlers (projectors, saga listeners, etc.) can potentially be called multiple times with the same event. This can lead to inconsistencies if the handlers are not idempotent (i.e., produce the same result on multiple executions as on a single execution). The framework should assist developers in writing idempotent handlers.

## Considered Options

1. **Documentation/Convention Only:** Developers are solely responsible for making handlers idempotent (e.g., using UPSERT logic in projections).
    * *Pros:* No framework overhead.
    * *Cons:* Error-prone, no unified solution, hard to enforce.
2. **Optimistic Locking / Versioning of Read Models:** Handlers check read model versions before updating.
    * *Pros:* Solves concurrency issues well.
    * *Cons:* Doesn't directly solve the multiple execution problem, adds overhead to read models.
3. **Tracking Processed Events:** The framework provides a mechanism to store, per handler, which event IDs have already been successfully processed.
    * *Pros:* Direct solution for multiple executions, robust, can be provided centrally.
    * *Cons:* Requires additional storage (DB table), slight performance impact (check before execution).

## Decision

We choose **Option 3: Tracking Processed Events** as a supporting mechanism.

1. Events will standardly receive a unique `eventId`.
2. A central table (`processed_events`) will be used (`handler_name`, `event_id`, `tenant_id`, `processed_at`).
3. The EAF **could** provide a Decorator (`@IdempotentEventHandler`) or a base service that:
    * Atomically checks if the event has already been processed for this handler (+tenant) before execution.
    * Skips execution if already processed.
    * Executes handler logic if not processed. Marks the event ID as processed upon **successful** completion of the handler (ideally within the same transaction as the handler's DB changes). Does not mark on failure.
4. At a minimum, this pattern will be documented and recommended as a best practice.

## Consequences

* Requires an additional DB table and logic for checking/writing.
* Developers need to use the mechanism (decorator) or implement the pattern themselves.
* Significantly increases the reliability of event processing.
* Design of the decorator/service must consider transaction boundaries (mark only after successful handler transaction).

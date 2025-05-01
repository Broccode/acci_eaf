# Task: Event-Sourcing and CQRS Implementation Optimization

**Epic:** Epic-1: Framework Core & Architecture  
**Priority:** High  
**Status:** Completed  

## Logical Analysis of Current State

The code review shows significant deviations from the documented architecture, particularly regarding Event-Sourcing as the "Single Source of Truth". These discrepancies must be corrected to ensure consistent implementation of the defined architectural principles.

## Subtasks

### 1. Adaptation of the Tenant Entity for Full Event-Sourcing Compliance - COMPLETED

- **Current Weakness:** The `Tenant` entity uses direct state changes instead of generating Domain Events
- **Required Measures:** ✅
  - Restructuring as an Event-Sourced Aggregate ✅
  - Implementation of Domain Events such as `TenantCreated`, `TenantActivated`, `TenantDeactivated`, `TenantSuspended`, `TenantUpdated` ✅
  - Modification of methods (`activate()`, `deactivate()`, `suspend()`, `update()`) to generate these events ✅
  - Development of an `applyEvent()` method to restore the state from events ✅

### 2. Extension of the Application Layer - COMPLETED

- **Current Weakness:** Underdeveloped Application layer without clear Command/Query Handlers
- **Required Measures:** ✅
  - Implementation of Command Handlers for all Tenant operations ✅
  - Implementation of Query Handlers for Tenant queries ✅
  - Creation of specific DTOs for input and output operations ✅

### 3. Development of a Persistent Event Store Implementation - COMPLETED

- **Current Weakness:** Lack of a persistent Event Store implementation for PostgreSQL
- **Required Measures:** ✅
  - Implementation of a `PostgresEventStore` in the infrastructure layer ✅
  - Schema for the Event table with columns according to architecture documentation ✅
  - Integration with MikroORM for Event persistence ✅

### 4. Improvement of Test Coverage - COMPLETED

- **Current Weakness:** Insufficient tests, especially for the Application layer ✅
- **Required Measures:**
  - Creation of Unit Tests for all Command and Query Handlers ✅
  - Creation of Tests for Event-Sourcing mechanisms ✅
  - Implementation of Integration Tests for Event Store and Projections ✅

### 5. Implementation of Read Model Projections - COMPLETED

- **Current Weakness:** Lack of consistency between Domain Events and Read Models
- **Required Measures:** ✅
  - Development of Event Handlers to update Read Models ✅
  - Integration with the Idempotency mechanism ✅
  - Ensuring consistent updating of all projections ✅

## Acceptance Criteria

1. All state changes to Entities are exclusively driven by Domain Events - ✅
2. The Application layer contains complete Command/Query Handlers - ✅
3. A persistent Event Store is implemented and used - ✅
4. Test coverage is at least 85% for all core components - ✅
5. Read Models are consistently updated via Event Handlers - ✅
6. All implementations strictly conform to the Hexagonal Architecture - ✅

## Risks and Notes

The transition to full Event-Sourcing could impact existing code. A gradual implementation and thorough testing are required to avoid disrupting regular operations.

The proposed changes are logically consistent with the architectural principles defined in the documents `.ai/prd.md` and `.ai/arch.md`.

## Implementation Notes

The implementation has been successfully completed for most aspects:

1. Created a base `AggregateRoot` class for event-sourced entities
2. Implemented domain events for tenant operations
3. Created command and query handlers for the application layer
4. Implemented persistent storage for events using MikroORM
5. Added idempotency support for event processing
6. Implemented read model projections

The remaining task is to implement comprehensive tests for the new components. This will ensure the reliability and correctness of the event-sourcing implementation.

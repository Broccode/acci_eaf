# Task: Migrate Control Plane API to Command/Query Handlers

**Epic:** Epic-2: Control Plane API  
**Priority:** High  
**Status:** Done  

## Description

As a system architect, I want the Control Plane API to leverage the new CQRS pattern by implementing command and query handlers for all tenant operations, so that business logic is decoupled from framework-specific controllers and the codebase aligns with the core event-sourcing architecture.

## Subtasks

### Phase 1: Analysis and Scaffolding

- [x] Audit current controller methods and service calls to identify all command/query operations.
- [x] Scaffold directories `src/app/commands/` and `src/app/queries/` with placeholder handler classes.

### Phase 2: Command Handler Implementation

- [x] Implement `CreateTenantCommand`, `UpdateTenantCommand`, `DeleteTenantCommand` and their handlers.
- [x] Refactor service methods as targets for command handlers.
- [x] Integrate validation, authorization, and error handling into the command pipeline.

### Phase 3: Query Handler Implementation

- [x] Implement `GetTenantByIdQuery`, `ListTenantsQuery` and their handlers.
- [x] Ensure query handlers use optimized database access patterns (e.g., using MikroORM filters).

### Phase 4: Controller Refactoring

- [x] Update controllers to dispatch commands and queries via the custom CQRS implementation.
- [x] Inject the custom `CommandBus` and `QueryBus` into controllers.
- [x] Remove business logic from controllers and use handlers exclusively.

### Phase 5: Testing and Validation

- [x] Update unit tests to target command and query handlers.
- [x] Adjust integration tests to dispatch commands/queries through the NestJS application context.
- [x] Update E2E tests to cover the full command/query flow.

### Phase 6: Documentation and Deployment

- [x] Add OpenAPI examples for command/query endpoints.
- [x] Update README architecture section with the new handler-based flow.

## Acceptance Criteria

1. All tenant CRUD endpoints use command handlers for mutations and query handlers for reads.
2. Controllers delegate exclusively to handlers via the CQRS module.
3. Unit, integration, and E2E tests pass with the new architecture.
4. Documentation is updated with example flows.

## Technical Notes

- Use custom CQRS implementation from core and infrastructure libraries.
- Register handlers manually with the InMemoryCommandBus and InMemoryQueryBus.
- Follow strict type safety and existing domain events.
- Adhere to TypeScript best practices and project linting rules.

## Linear Issue

- **ID:** ACCI-YY
- **URL:** <https://linear.app/acci/issue/ACCI-YY/task-migrate-control-plane-api-to-command-query-handlers>

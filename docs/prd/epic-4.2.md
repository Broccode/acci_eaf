**Epic 4.2: Security Context Evolution**

- **Goal**: Eliminate the circular dependency by refactoring the security aspect to be event-driven
  and implementing a robust context propagation mechanism.

- **User Stories**:

  - **[4.2.1](./../stories/4.2.1.story.md)**: Implement `SecurityContextCorrelationDataProvider`
  - **[4.2.2](./../stories/4.2.2.story.md)**: Implement Request Context Extraction
  - **[4.2.3](./../stories/4.2.3.story.md)**: Refactor `TenantSecurityAspect`
  - **[4.2.4](./../stories/4.2.4.story.md)**: Create Integration Tests for Security Context

- **Acceptance Criteria**:
  1. The application starts successfully with zero circular dependency exceptions and without
     requiring `@Lazy` workarounds.
  2. All events stored in the event store are verifiably enriched with `tenant_id`, `user_id`, and
     `correlation_id` from the security context.
  3. The refactored `TenantSecurityAspect` correctly enforces all previous security access rules.

**Epic 4.5: Audit Trail Evolution**

- **Goal**: Replace the synchronous audit service with a resilient, event-driven projection that
  creates a comprehensive audit trail from domain events.

- **User Stories**:

  - **[4.5.1](./../stories/4.5.1.story.md)**: Create `AuditTrailProjector` and Schema
  - **[4.5.2](./../stories/4.5.2.story.md)**: Implement Generic Event-to-Audit Mapping
  - **[4.5.3](./../stories/4.5.3.story.md)**: Implement Feature Flag for New Audit System
  - **[4.5.4](./../stories/4.5.4.story.md)**: Refactor Endpoints to Use New Audit Projection

- **Acceptance Criteria**:
  1. The new `audit_trail` table contains an entry for every single domain event that occurs in the
     system.
  2. The new audit data can be queried via existing administrative endpoints.
  3. The new audit system can be toggled on and off using a feature flag, with the system reverting
     to the legacy `AuditService` when disabled.

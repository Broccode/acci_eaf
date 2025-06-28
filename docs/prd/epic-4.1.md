**Epic 4.1: Foundation & Infrastructure Setup**

- **Goal**: Establish the technical foundation for Axon Framework integration. This phase focuses on
  creating the necessary adapters, database schema modifications, and team enablement materials to
  ensure a smooth and successful adoption of Axon.

- **User Stories**:

  - **[4.1.1](./../stories/4.1.1.story.md)**: Implement `EafPostgresEventStorageEngine` Core Logic
  - **[4.1.2](./../stories/4.1.2.story.md)**: Implement `GlobalSequenceTrackingToken`
  - **[4.1.3](./../stories/4.1.3.story.md)**: Implement `TenantContextHolder` for Multi-Tenancy
  - **[4.1.4](./../stories/4.1.4.story.md)**: Unit & Integration Testing for `EventStorageEngine`
  - **[4.1.5](./../stories/4.1.5.story.md)**: Analyze & Script Database Schema Changes
  - **[4.1.6](./../stories/4.1.6.story.md)**: Performance Test Schema
  - **[4.1.7](./../stories/4.1.7.story.md)**: Create Axon Framework Training Materials

- **Acceptance Criteria**:
  1. A custom `EventStorageEngine` is implemented, passing all unit and integration tests.
  2. The database schema is successfully migrated in all environments without data loss.
  3. The development team confirms they can successfully set up their local environments and
     understand the basics of Axon Framework.

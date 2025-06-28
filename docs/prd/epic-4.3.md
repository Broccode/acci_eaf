**Epic 4.3: Event-Driven Core Implementation**

- **Goal**: Migrate core aggregates to Axon with programmatic configuration, establishing the
  primary pattern for event-sourcing in the EAF.

- **User Stories**:

  - **[4.3.1](./../stories/4.3.1.story.md)**: Refactor `User` Entity to Axon `User` Aggregate
  - **[4.3.2](./../stories/4.3.2.story.md)**: Implement `User` Command and Event Handlers
  - **[4.3.3](./../stories/4.3.3.story.md)**: Create Programmatic Axon Configuration
  - **[4.3.4](./../stories/4.3.4.story.md)**: Implement Given-When-Then Tests for `User` Aggregate

- **Acceptance Criteria**:
  1. The `User` aggregate is fully event-sourced and handles all related business commands correctly
     according to Given-When-Then tests.
  2. The `User` domain model and its related command/event classes contain zero Axon Framework or
     Spring annotations.
  3. An API call to create a user correctly results in a `UserCreatedEvent` being persisted in the
     event store with the correct, propagated security metadata.

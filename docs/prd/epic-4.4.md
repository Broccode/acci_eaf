**Epic 4.4: Event Propagation Infrastructure**

- **Goal**: Implement reliable event distribution to external systems using the Transactional Outbox
  pattern, with the Axon Event Store serving as the outbox.

- **User Stories**:

  - **[4.4.1](./../stories/4.4.1.story.md)**: Implement `NatsEventRelayProcessor`
  - **[4.4.2](./../stories/4.4.2.story.md)**: Configure `TrackingEventProcessor` for NATS Relay
  - **[4.4.3](./../stories/4.4.3.story.md)**: Implement Retry and Error Handling for NATS Publishing
  - **[4.4.4](./../stories/4.4.4.story.md)**: Implement Inbound NATS Adapter

- **Acceptance Criteria**:
  1. All events persisted in the event store are published to the correct NATS subject within 5
     seconds.
  2. A temporary NATS outage does not result in any lost events; events are published automatically
     once NATS is restored.
  3. The system can correctly process inbound events from NATS and route them to the appropriate
     internal handlers.

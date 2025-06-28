**Epic 4.6: Production Rollout & Optimization**

- **Goal**: Safely deploy the new event-driven architecture to production, optimize its performance,
  and finalize the migration by removing legacy code.

- **User Stories**:

  - **[4.6.1](./../stories/4.6.1.story.md)**: Create Monitoring Dashboard for Axon Metrics
  - **[4.6.2](./../stories/4.6.2.story.md)**: Conduct and Analyze Load Tests
  - **[4.6.3](./../stories/4.6.3.story.md)**: Implement Chaos Engineering Tests
  - **[4.6.4](./../stories/4.6.4.story.md)**: Final Legacy System Cleanup

- **Acceptance Criteria**:
  1. All new components are monitored in production with clear dashboards and alerts.
  2. The system meets all defined performance targets (latency < 25ms, throughput > 400 RPS) under
     production load.
  3. The legacy `AuditService` and all related components have been completely removed from the
     production codebase.

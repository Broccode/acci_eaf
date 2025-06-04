**Epic 2.8: Enablement of a Pilot Service/Module (Key Outcome of EAF MVP)**

- **Goal:** To validate the entire ACCI EAF MVP by using it to build a real, albeit small, piece of
  software, demonstrating its capabilities and gathering crucial feedback.
- **User Stories:**
  1. **Story 2.8.1: Define and Scope Pilot Service/Module**
     - As the EAF Product Team (John, Fred, Jane, Sarah), I want to clearly define and agree upon
       the scope, functional requirements, and success criteria for a small, representative pilot
       service/module that will be built using the ACCI EAF MVP, so that we have a focused target
       for validating the EAF\'s capabilities.
     - **Acceptance Criteria:**
       1. A brief Project Brief or scope document for the selected pilot service/module is created
          and approved by stakeholders.
       2. The pilot service scope demonstrably exercises key EAF MVP features: involves at least one
          event-sourced Aggregate, a few Commands and Events, a simple Projector updating a read
          model, basic UI interaction (if applicable, using the UI Foundation Kit), and integration
          with the EAF IAM service for security.
       3. Success criteria for the pilot, specifically from an EAF validation perspective (e.g.,
          \"Developer was able to implement X feature using EAF SDK Y within Z timeframe,\" \"TDD
          practices were successfully followed,\" \"Hexagonal structure maintained\"), are
          documented.
  2. **Story 2.8.2: Develop Pilot Service/Module using ACCI EAF MVP**
     - As the Pilot Development Team (e.g., Michael and Lirika, supported by EAF core team), I want
       to successfully develop and build the defined pilot service/module strictly using the ACCI
       EAF MVP (core services accessible via SDKs, `eaf-cli` for scaffolding, \"Launchpad\"
       Developer Portal for guidance, and adhering to mandated CQRS/ES, DDD, Hexagonal Architecture,
       TDD principles), so that the EAF\'s usability, SDK effectiveness, and developer experience
       are practically validated.
     - **Acceptance Criteria:**
       1. The pilot service/module is developed following TDD, with unit tests written first and
          achieving >80% coverage for new business logic.
       2. It correctly utilizes the `eaf-eventing-sdk` for publishing domain events relevant to its
          scope to NATS and consuming any necessary events.
       3. It correctly utilizes the `eaf-eventsourcing-sdk` for persisting and rehydrating its
          event-sourced aggregate(s) from the PostgreSQL event store.
       4. It integrates with the EAF IAM service for authentication and basic RBAC using the
          `eaf-iam-client` SDK.
       5. Its internal structure strictly adheres to Hexagonal Architecture principles, with clear
          separation of domain, application, and infrastructure concerns, as scaffolded/guided by
          EAF.
       6. All code passes CI checks, including linting (Spotless), formatting, all unit tests, and
          relevant ArchUnit tests.
       7. If a UI is part of the pilot, it is built using components from the `ui-foundation-kit`
          and Vaadin/Hilla/React.
  3. **Story 2.8.3: Deploy and Test Pilot Service/Module in a Test Environment**
     - As the Pilot Development Team, I want to deploy the pilot service/module to a representative
       test environment (e.g., using the EAF-provided Docker Compose setup for local/dev or a
       similar \"less cloud-native\" pattern on a shared VM) and perform basic functional and
       integration tests against its exposed APIs or UI, so that its operational viability with EAF
       is demonstrated.
     - **Acceptance Criteria:**
       1. The pilot service/module is successfully deployed to a test environment alongside
          necessary EAF MVP core services (IAM, NATS, Postgres).
       2. Key functionalities of the pilot service (e.g., command processing, event publication,
          query responses, UI interactions if applicable) are tested and verified in this
          environment.
       3. Basic multi-tenancy aspects (e.g., data isolation if the pilot handles tenant-specific
          data) are validated.
       4. Logs from the pilot service and relevant EAF services are accessible and show correct
          operation.
  4. **Story 2.8.4: Collect and Analyze Feedback from Pilot Team**
     - As the ACCI EAF Core Team (John, Fred, Jane, Sarah), I want to systematically collect
       detailed feedback from the pilot development team regarding their experience using the EAF
       MVP\'s services, SDKs, CLI, documentation (\"Launchpad\"), and overall development process
       (adherence to DDD, Hexagonal, TDD), so that this feedback can be analyzed to identify EAF
       strengths, weaknesses, and areas for immediate improvement prior to broader rollout.
     - **Acceptance Criteria:**
       1. A feedback collection mechanism (e.g., structured survey, a series of guided interview
          sessions, a dedicated retrospective meeting) is established and communicated to the pilot
          team.
       2. Feedback is actively collected from the pilot team throughout and upon completion of their
          development, focusing on SDK usability, CLI effectiveness, documentation clarity and
          completeness, ease/difficulty of adhering to mandated architectural patterns with EAF
          tooling, and overall Developer Experience.
       3. A summary report of the collected feedback, categorizing findings and including actionable
          insights and specific recommendations for EAF improvement, is produced and reviewed by the
          EAF core team.

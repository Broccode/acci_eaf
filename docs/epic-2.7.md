**Epic 2.7: Developer Portal & Documentation - MVP (\"The Launchpad\" concept)**

- **Goal:** Provide essential learning and reference materials to enable developers to effectively
  use the ACCI EAF MVP.
- **User Stories:**
  1. **Story 2.7.1: \"Getting Started\" Guide on Launchpad**
     - As a New Developer (Lirika), I want a comprehensive \"Getting Started\" guide on \"The
       Launchpad\" Developer Portal that rapidly walks me through setting up my local development
       environment (including NATS, PostgreSQL via Docker Compose, IDE configuration for
       Kotlin/Spring and React/TS) and building/running a simple \"hello world\" style application
       using the ACCI EAF CLI and core MVP SDKs, so I can quickly become productive and understand
       the basic EAF workflow.
     - **Acceptance Criteria:**
       1. \"The Launchpad\" (Docusaurus site) hosts a \"Getting Started\" guide as a primary entry
          point.
       2. The guide includes prerequisites and setup instructions for Kotlin/JDK, Spring Boot,
          Node.js, Docker, Nx, and recommended IDEs (IntelliJ IDEA for backend, VS Code for
          frontend).
       3. Provides step-by-step instructions to use `acci-eaf-cli` to generate a new EAF service and
          a simple frontend view that interacts with it.
       4. Explains how to run the EAF services (including local NATS/PostgreSQL via the EAF-provided
          Docker Compose setup) and the generated application locally from the IDE or command line
          (using Gradle/Nx).
       5. Includes a minimal \"hello world\" example demonstrating a frontend Vaadin/Hilla/React
          view calling a Hilla endpoint on the Kotlin/Spring backend.
  2. **Story 2.7.2: Core Concepts Documentation on Launchpad**
     - As a Developer (Michael, Majlinda), I want to access well-structured documentation with clear
       explanations and Kotlin/React code examples on \"The Launchpad\" for core EAF architectural
       concepts (EAF Overview, CQRS/ES principles as applied in EAF, DDD tactical patterns relevant
       to EAF services, Hexagonal Architecture implementation guidelines for EAF, TDD philosophy and
       practices for EAF) and for the APIs/usage of the MVP SDKs (Eventing SDK for NATS, Event
       Sourcing SDK for PostgreSQL, IAM client SDK), so I can effectively build services according
       to EAF best practices.
     - **Acceptance Criteria:**
       1. \"The Launchpad\" contains dedicated, easily navigable sections for \"Core Architectural
          Concepts\" and \"EAF SDK Reference.\"
       2. The \"Core Architectural Concepts\" section clearly explains how DDD (Aggregates, Events,
          Commands), Hexagonal Architecture (Ports, Adapters for common concerns like REST, NATS,
          DB), CQRS/ES, and TDD are expected to be implemented within the ACCI EAF context, with
          illustrative (conceptual) diagrams and code snippets.
       3. The \"EAF SDK Reference\" provides API documentation (e.g., KDoc generated and presented,
          or manually written guides with code examples) for key public classes, methods, and
          configuration options in the `eaf-eventing-sdk`, `eaf-eventsourcing-sdk`, and
          `eaf-iam-client-sdk`.
       4. Code examples are provided in Kotlin for backend SDK usage and TypeScript/React for
          frontend interactions with Hilla, showing practical application of the SDKs.
  3. **Story 2.7.3: UI Foundation Kit & Storybook Access via Launchpad**
     - As a Frontend Developer (Lirika), I want \"The Launchpad\" to provide easy access to an
       interactive UI component library (Storybook instance) for the ACCI EAF UI Foundation Kit MVP,
       showcasing available Vaadin and custom React components, their props, variations, and usage
       examples (including Hilla integration), so I can easily discover and reuse standardized UI
       components in my applications.
     - **Acceptance Criteria:**
       1. \"The Launchpad\" has a clear section or prominent link pointing to the deployed Storybook
          instance for the ACCI EAF UI Foundation Kit.
       2. The Storybook instance includes entries for the initial set of core generic components
          (Buttons, Inputs, Layouts, Tables, Modals, etc.) from the UI Foundation Kit, as defined by
          the Design Architect.
       3. Each Storybook entry provides interactive controls to manipulate component props, clear
          code snippets for usage in React/Hilla, and notes on theming and accessibility.
  4. **Story 2.7.4: Search & Feedback Mechanism on Launchpad**
     - As any User of \"The Launchpad\" (Anita), I want robust search functionality covering all
       documentation content (guides, API refs, concept explanations) and a simple, clearly visible
       mechanism to provide feedback or ask questions about the documentation (e.g., a link to a
       dedicated feedback channel or issue tracker), so I can efficiently find information and
       contribute to improving the EAF resources.
     - **Acceptance Criteria:**
       1. The Docusaurus search functionality is enabled, configured, and effectively indexes all
          portal content.
       2. A clear \"Feedback\" or \"Report Documentation Issue\" link/button is present on all pages
          or in a consistent location (e.g., site footer or navigation bar). This link directs users
          to the appropriate channel (e.g., a designated GitHub Issues repository for documentation,
          a specific email alias, or a feedback form).

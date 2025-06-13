**Epic 2.6: Scaffolding CLI (\"ACCI EAF CLI\") - MVP**

- **Goal:** Accelerate developer setup, promote consistency, and support mandated architectural
  patterns.
- **User Stories:**
  1. **Story 2.6.1: Generate Kotlin/Spring Backend Service via CLI**
     - As a Developer (Michael, Majlinda), I want to use the ACCI EAF CLI to quickly generate a new,
       fully structured Kotlin/Spring backend service module within the Nx monorepo, complete with a
       Hexagonal Architecture layout, TDD setup (JUnit 5, MockK, ArchUnit stubs), integrated core
       EAF SDKs (for logging, context, basic eventing/ES if applicable), default Spotless formatting
       configuration, and boilerplate for local NATS/PostgreSQL connection (via Docker Compose
       references), so I can bypass manual boilerplate and immediately start developing
       domain-specific logic according to EAF best practices.
     - **Acceptance Criteria:** 1. CLI command `eaf generate service <service-name>` creates a new
       Gradle sub-project within the `apps/` or `libs/` directory of the Nx monorepo, as specified
       by user or EAF convention. 2. The generated service includes a `build.gradle.kts` with
       necessary Kotlin, Spring Boot dependencies using centralized dependency version management
       (version aligned with EAF), core EAF SDK dependencies (e.g., `eaf-core`, `eaf-eventing-sdk`,
       `eaf-eventsourcing-sdk`), and testing dependencies (JUnit 5, MockK, ArchUnit). 3. The
       generated directory structure includes `src/main/kotlin` and `src/test/kotlin` with
       placeholders for `domain/model`, `domain/port`, `application/service`, `application/port/in`,
       `application/port/out`, `infrastructure/adapter/in/web` (with a sample Hilla endpoint or
       Spring Controller), `infrastructure/adapter/out/persistence` (with a sample repository port
       implementation). 4. A basic TDD setup is present: `src/test/kotlin` directory with example
       unit tests for a sample application service and an ArchUnit test class placeholder with
       example rules. 5. A main application class for Spring Boot (e.g., `@SpringBootApplication`)
       is generated. 6. Spotless configuration is included and applied in the `build.gradle.kts`. 7.
       Example `application.yml` or `.properties` files include placeholders and comments for
       connecting to local NATS and PostgreSQL instances (referencing connection details for the EAF
       standard Docker Compose setup). 8. The new module is correctly registered in the root
       `settings.gradle.kts` and its `project.json` for Nx is generated with basic build/serve/test
       targets.
  2. **Story 2.6.2: Generate Core CQRS/ES Components within a Service via CLI**
     - As a Backend Developer (Majlinda), I want to use the ACCI EAF CLI to generate boilerplate
       code for common CQRS/ES components (specifically Aggregates, Commands, Domain Events, and
       basic Projector stubs) within an existing EAF Kotlin/Spring service, pre-wired with EAF SDK
       patterns and annotations, so that I can rapidly extend my domain model with consistent,
       EAF-compliant code.
     - **Acceptance Criteria:**
       1. CLI command `eaf generate aggregate <AggregateName> --service=<service-name>` creates:
          - A Kotlin data class for a creation Command (e.g., `Create<AggregateName>Command.kt` in
            an `application/port/in` or `domain/command` package).
          - A Kotlin data class for a creation Event (e.g., `<AggregateName>CreatedEvent.kt` in a
            `domain/event` package).
          - A Kotlin class for the Aggregate root (e.g., `<AggregateName>.kt` in `domain/model`)
            annotated with `@EafAggregate` (or equivalent), including an `@AggregateIdentifier`
            field, a constructor annotated as a command handler for the creation command which
            applies the creation event, and an event sourcing handler method for the creation event.
          - A basic unit test stub (e.g., `<AggregateName>Test.kt`) for the Aggregate using EAF\'s
            TDD setup.
       2. CLI command `eaf generate command <CommandName> --aggregate=<AggregateName>` adds a new
          Kotlin data class for the command and a placeholder command handler method (annotated
          appropriately) within the specified Aggregate class.
       3. CLI command `eaf generate event <EventName> --aggregate=<AggregateName>` adds a new Kotlin
          data class for the event and a placeholder event sourcing handler method within the
          specified Aggregate class.
       4. CLI command
          `eaf generate projector <ProjectorName> --service=<service-name> --event=<EventName>`
          creates a Kotlin class stub (e.g., `<ProjectorName>.kt` in
          `infrastructure/adapter/in/messaging` or `application/projection`) for a Projector,
          including an event handler method (e.g., using `@NatsJetStreamListener` or equivalent EAF
          SDK annotation) for the specified event type.
       5. Generated components are placed in appropriate packages according to the service\'s
          Hexagonal Architecture and DDD conventions.
       6. All generated Kotlin code adheres to EAF coding standards and passes Spotless formatting.
  3. **Story 2.6.3: Basic Frontend Module Generation via CLI for Vaadin/Hilla/React**
     - As a Frontend Developer (Lirika), I want to use the ACCI EAF CLI to generate a new frontend
       module structure (for Vaadin/Hilla with React) within an EAF application (e.g., as part of
       the Control Plane or a new product\'s Hilla application), pre-configured with standard
       project structure, basic i18n setup (e.g., `i18next` with example JSON files), a Storybook
       setup, linting/formatting tools (ESLint/Prettier), and a boilerplate component/view, so I can
       start building UI features quickly and consistently using the UI Foundation Kit.
     - **Acceptance Criteria:**
       1. CLI command `eaf generate frontend-view <ViewName> --app=<hilla-app-name>` (or similar)
          creates a new `.tsx` view file within the specified Hilla application\'s
          `src/main/frontend/views/` directory.
       2. The generated view includes boilerplate for a React functional component, imports from
          `@vaadin/react-components` and Hilla utilities, and a basic Hilla route configuration
          (`export const config: ViewConfig = {...};`).
       3. CLI command `eaf generate frontend-component <ComponentName> --lib=ui-foundation-kit` (or
          within an app) creates a new `.tsx` component file and a corresponding `.stories.tsx`
          file.
       4. If generating for a new Hilla application for the first time, the CLI ensures the
          `src/main/frontend` directory is correctly structured with `package.json` (including
          dependencies for React, Vaadin components, Hilla, i18next, Storybook), `tsconfig.json`,
          `vite.config.ts`, ESLint/Prettier configs, and basic i18n setup (e.g., `i18n.ts`
          configuration file, `locales/en/translation.json`, `locales/de/translation.json`).
       5. Generated Storybook setup includes configuration to find stories for both UI Foundation
          Kit components and application-specific components.

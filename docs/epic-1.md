**Epic 1: Foundational Engineering & DevEx Setup (MVP)**

- **Goal:** Establish the core development environment, CI/CD practices, and quality gates necessary
  for building ACCI EAF and its applications according to mandated principles.
- **User Stories:**
  1. **Story 1.1: Monorepo Initialization & Structure**
     - As a Developer (Michael), I want the ACCI EAF monorepo to be initialized using Nx, with
       clear, standardized project structures defined for Kotlin/Spring backend services (Gradle
       multi-modules) and the chosen Vaadin/Hilla/React UI framework, so that I can easily navigate,
       manage, and contribute code for different EAF modules and product components from day one.
     - **Acceptance Criteria:**
       1. Nx workspace is initialized and configured with `@nx/gradle` and relevant JS/TS plugins
          (e.g., for React).
       2. Root `build.gradle.kts` and `settings.gradle.kts` define common configurations (Kotlin,
          Spring Boot versions via catalog) and initial module structure (e.g., `apps`, `libs`,
          `tools`) for backend projects.
       3. Placeholders for initial EAF services (e.g., `iam-service`) and libraries (e.g.,
          `eaf-sdk`, `ui-foundation-kit`) exist as Gradle sub-projects (for backend) or Nx projects
          (for frontend libs/apps) within the Nx structure.
       4. Basic `project.json` files exist for these initial Nx projects, configured for relevant
          Gradle tasks or JS/TS build/serve/test commands.
       5. Documentation outlines the monorepo structure and how to add new projects/modules.
  2. **Story 1.2 (was PRD Story 1.1.A / TS-DEVEX-01): Configure Nx Monorepo with Gradle Projects &
     Basic CI Workflow**
     - As the EAF Development Team, I want the Nx Monorepo configured with initial Gradle project
       structures for backend services/libraries and a basic CI workflow (build, lint, format, unit
       test affected projects, using GitLab CI as primary and GitHub Actions as secondary), so that
       we have a consistent and automated foundation for all EAF development.
     - **Acceptance Criteria:**
       1. CI pipeline (primarily GitLab CI, with GitHub Actions as a secondary option) is defined
          and triggers on pushes/PRs to `main`.
       2. CI pipeline executes `nx affected:build --all` and `nx affected:test --all` (or equivalent
          commands covering both backend and frontend).
       3. Spotless for Kotlin formatting is integrated into the Gradle build and enforced by CI.
       4. ESLint/Prettier (or chosen UI framework equivalents) are configured for frontend code in
          `ui-foundation-kit` and `acci-eaf-control-plane/frontend` and enforced by CI.
       5. Build fails if linting, formatting, or tests fail for any affected project.
       6. Basic build artifacts (e.g., JARs for backend services, static assets for frontend if
          applicable as separate deployable) are produced and archived by CI for successful builds.
  3. **Story 1.3 (was PRD Story 1.1.B / TS-QUALITY-01): Implement Initial ArchUnit Test Suite for
     Core Architectural Rules**
     - As the EAF Development Team, I want an initial ArchUnit test suite integrated into the CI
       pipeline, with core rules for layer dependencies (Hexagonal) and naming conventions, so that
       architectural integrity is programmatically enforced.
     - **Acceptance Criteria:**
       1. ArchUnit dependency is added to the test configurations of relevant backend modules.
       2. An initial ArchUnit test suite is created (e.g., in a shared test utility library or
          within a core EAF module).
       3. Tests verify basic layer dependencies (e.g., domain packages do not depend on
          infrastructure packages like Spring framework).
       4. Tests verify core naming conventions for key architectural components (services, ports,
          adapters).
       5. ArchUnit tests are successfully executed as part of the CI pipeline for relevant backend
          modules.
  4. **Story 1.4 (was PRD Story 1.3): TDD Setup & Examples**
     - As a Developer (Lirika), I want a functional Test-Driven Design (TDD) setup readily available
       for both Kotlin/Spring (JUnit 5, MockK) and the Vaadin/Hilla/React UI framework, complete
       with integrated testing libraries and illustrative example tests for a simple
       component/service, so that I can easily practice and strictly adhere to TDD.
     - **Acceptance Criteria:**
       1. An example Kotlin/Spring service within an EAF module includes a unit test (using JUnit 5
          & MockK) written _before_ its minimal implementation, demonstrating the TDD
          red-green-refactor cycle.
       2. An example React component (within `ui-foundation-kit` or
          `acci-eaf-control-plane/frontend`) includes a unit test (using Vitest/Jest + React Testing
          Library) demonstrating TDD for a UI component.
       3. All necessary testing libraries are correctly configured in `build.gradle.kts` files (for
          backend) and relevant `package.json` files (for frontend).
       4. The \"Launchpad\" Developer Portal includes a concise guide on the TDD workflow expected
          for ACCI EAF development, referencing these examples.
  5. **Story 1.5 (was PRD Story 1.4): Developer Portal (\"The Launchpad\") Initial Setup**
     - As an EAF Contributor (Anita), I want the initial structure for the \"Launchpad\" Developer
       Portal (using Docusaurus) to be established, accessible via a local command, and integrated
       into the monorepo, so that foundational documentation (\"Getting Started,\" architectural
       principles) can be immediately populated and version-controlled.
     - **Acceptance Criteria:**
       1. Docusaurus is set up within the `docs/` directory of the monorepo.
       2. Placeholders for \"Getting Started Guide,\" \"Architectural Principles (DDD, Hexagonal,
          CQRS/ES, TDD for EAF),\" \"Core Services Overview,\" and \"UI Foundation Kit Intro\" are
          created.
       3. A local development server can be started for the portal.
       4. The portal is included in the monorepo\'s version control and basic CI check (e.g., build
          portal).

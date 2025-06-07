## Project Structure

The ACCI EAF will adopt a monorepo structure managed by Nx, containing Gradle multi-module projects
for backend services and libraries, and JS/TS projects for frontend aspects (UI Foundation Kit,
Control Plane frontend).

```plaintext
acci-eaf-monorepo/
├── apps/                                # Deployable applications
│   ├── acci-eaf-control-plane/          # Vaadin/Hilla/React + Spring Boot application (Nx project)
│   │   ├── backend/                     # Spring Boot module (Gradle sub-project)
│   │   │   └── src/main/kotlin/com/axians/eaf/controlplane/...
│   │   ├── frontend/                    # React/Hilla frontend code (part of backend Gradle module)
│   │   │   └── src/main/frontend/...
│   │   └── build.gradle.kts
│   ├── iam-service/                     # EAF IAM Core Service (Kotlin/Spring, Nx project, Gradle sub-project)
│   │   └── src/main/kotlin/com/axians/eaf/iam/...
│   │   └── build.gradle.kts
│   ├── license-management-service/      # EAF License Service (Kotlin/Spring, Nx project, Gradle sub-project)
│   │   └── src/main/kotlin/com/axians/eaf/license/...
│   │   └── build.gradle.kts
│   ├── feature-flag-service/            # EAF Feature Flag Service (Kotlin/Spring, Nx project, Gradle sub-project)
│   │   └── src/main/kotlin/com/axians/eaf/featureflag/...
│   │   └── build.gradle.kts
│   └── # ... other EAF core service applications (e.g., Config Service MVP) ...
├── libs/                                # Sharable libraries
│   ├── eaf-core/                        # Common utilities, base classes for EAF (Kotlin, Nx project, Gradle sub-project)
│   │   └── src/main/kotlin/...
│   │   └── build.gradle.kts
│   ├── eaf-eventing-sdk/                # NATS/JetStream integration, event publishing/consuming helpers (Kotlin, Nx project, Gradle sub-project)
│   │   └── src/main/kotlin/...
│   │   └── build.gradle.kts
│   ├── eaf-eventsourcing-sdk/           # Helpers for CQRS/ES, Axon integration, EventStore interaction via PostgreSQL (Kotlin, Nx project, Gradle sub-project)
│   │   └── src/main/kotlin/...
│   │   └── build.gradle.kts
│   ├── eaf-iam-client/                  # Client library for IAM service (Kotlin, Nx project, Gradle sub-project)
│   │   └── src/main/kotlin/...
│   │   └── build.gradle.kts
│   ├── eaf-featureflag-client/          # Client library for Feature Flag service (Kotlin, Nx project, Gradle sub-project)
│   │   └── src/main/kotlin/...
│   │   └── build.gradle.kts
│   ├── eaf-license-client/              # Client library for License service (Kotlin, Nx project, Gradle sub-project)
│   │   └── src/main/kotlin/...
│   │   └── build.gradle.kts
│   ├── shared-domain-kernel/            # Optional: Shared pure domain primitives (Kotlin, Nx project, Gradle sub-project)
│   │   └── src/main/kotlin/com/axians/eaf/shared/kernel/...
│   │   └── build.gradle.kts
│   ├── ui-foundation-kit/               # UI Components (Vaadin/Hilla/React), Storybook (JS/TS, Nx project)
│   │   └── src/                            # Source for components, themes, etc.
│   │   └── storybook/                      # Storybook configuration and stories
│   │   └── package.json
│   └── # ... other shared libraries (e.g., common test utilities) ...
├── tools/
│   └── acci-eaf-cli/                    # ACCI EAF CLI (Kotlin/Picocli, Nx project, Gradle sub-project)
│       └── src/main/kotlin/com/axians/eaf/cli/...
│       └── build.gradle.kts
├── docs/                                # Developer Portal (\"The Launchpad\") source (Docusaurus)
│   └── package.json
├── infra/
│   ├── docker-compose/                  # Docker Compose files for local dev environments (NATS, Postgres, EAF services)
│   └── ansible/                         # Ansible playbooks for EAF infra setup (if any)
├── gradle/
│   └── wrapper/
├── build.gradle.kts                     # Root Gradle build file (common configs, plugin management for all Gradle projects)
├── settings.gradle.kts                  # Gradle multi-project definitions (includes all backend apps & libs)
├── gradlew
├── gradlew.bat
├── nx.json                              # Nx workspace configuration (plugins, targets defaults)
├── package.json                         # Root package.json for Nx, workspace tools (ESLint, Prettier for JS/TS)
└── tsconfig.base.json                   # Base TypeScript config for the workspace (for UI Kit, docs, etc.)
```

### Key Directory Descriptions

- **`apps/`**: Contains deployable EAF services and the Control Plane application. Each is an Nx
  project, typically corresponding to a Gradle sub-project (or a Hilla project combining backend and
  frontend).
- **`libs/`**: Contains shared libraries.
  - EAF SDK components (`eaf-core`, `eaf-eventing-sdk`, `eaf-eventsourcing-sdk`, etc.): Individual
    Gradle sub-projects providing client SDKs for EAF services and core framework functionalities.
  - `ui-foundation-kit`: Contains the reusable Vaadin/Hilla/React components, themes, and Storybook
    setup.
- **`tools/`**: Contains development tools like the ACCI EAF CLI.
- **`docs/`**: Houses the source for the \"Launchpad\" Developer Portal.
- **`infra/`**: Contains infrastructure definitions like Docker Compose files for local development
  and Ansible playbooks.
- Root Gradle files (`build.gradle.kts`, `settings.gradle.kts`) manage the Gradle multi-project
  build for all backend components.
- Root Nx files (`nx.json`, `package.json`, `tsconfig.base.json`) manage the overall monorepo, JS/TS
  tooling, and Nx-specific configurations.

# ACCI EAF Monorepo

Welcome to the ACCI Enterprise Application Framework (EAF) monorepo. This workspace contains all EAF core services, libraries, and tools organized using Nx for efficient development and build management.

## Prerequisites

- Node.js 18+ and npm 8+
- Java 17+ (for backend services)
- Docker and Docker Compose (for local development)

## Quick Start

```bash
# Install dependencies
npm install

# List all projects
npx nx show projects

# Build all projects
npx nx run-many -t build

# Test all projects
npx nx run-many -t test

# Build a specific project
npx nx build iam-service
npx nx build ui-foundation-kit

# Run a specific service
npx nx run iam-service
```

## Project Structure

```
acci-eaf-monorepo/
├── apps/                    # Deployable applications
│   └── iam-service/         # EAF IAM Core Service
├── libs/                    # Shared libraries
│   ├── eaf-sdk/            # Core EAF SDK (multi-module)
│   └── ui-foundation-kit/   # UI Components & Themes
├── tools/                   # Development tools
├── docs/                    # Documentation
├── infra/                   # Infrastructure definitions
├── nx.json                  # Nx workspace configuration
├── package.json             # Root package.json for workspace
└── build.gradle.kts         # Root Gradle build configuration
```

## Backend Development

Backend services use Kotlin, Spring Boot, and Gradle. All backend projects are managed as Gradle sub-projects.

```bash
# Build backend projects
./gradlew build

# Run tests
./gradlew test

# Run a specific service
./gradlew :apps:iam-service:bootRun
```

## Frontend Development

Frontend projects use TypeScript, React, and Vaadin components.

```bash
# Build UI Foundation Kit
npx nx build ui-foundation-kit

# Run Storybook for UI development
npx nx storybook ui-foundation-kit

# Run tests
npx nx test ui-foundation-kit
```

## Architecture

The EAF follows a microservices architecture with:

- **Core Services**: IAM, License Management, Feature Flags
- **Shared SDK**: Common utilities and client libraries
- **UI Foundation Kit**: Reusable React/Vaadin components
- **Event-Driven Communication**: Using NATS/JetStream
- **CQRS/ES**: Using Axon Framework with PostgreSQL

## Documentation

- [Project Structure](docs/project-structure.md) - Detailed project organization
- [Tech Stack](docs/tech-stack.md) - Technology choices and versions
- [Operational Guidelines](docs/operational-guidelines.md) - Coding standards and practices

## Development Workflow

1. Each feature/service is developed in isolation
2. Shared code goes into the `libs/` directory
3. Use Nx for dependency management and affected builds
4. Follow the coding standards in `docs/operational-guidelines.md`

## Getting Help

- Check the `docs/` directory for detailed documentation
- Use `npx nx --help` for Nx commands
- Use `./gradlew tasks` for available Gradle tasks

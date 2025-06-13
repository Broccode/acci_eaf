---
sidebar_position: 2
title: Getting Started
---

# Getting Started with ACCI EAF

Welcome to the ACCI EAF Getting Started guide. This section will walk you through setting up your
development environment and building your first EAF application.

## Prerequisites

Before you begin, ensure you have the following installed on your development machine:

- **Java Development Kit (JDK)**: Version 17 or higher
- **Kotlin**: Latest stable version
- **Node.js**: Version 18.0 or higher
- **Docker**: For running local infrastructure services
- **Git**: For version control
- **IDE**: IntelliJ IDEA (recommended for backend) or VS Code

## Quick Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd acci-eaf
```

### 2. Install Dependencies

```bash
# Install Node.js dependencies for frontend and tooling
npm install

# Build all backend projects using Nx
nx run-many -t build
```

### 3. Run Your First EAF Service (with Auto Infrastructure)

EAF services are configured with Spring Boot's Docker Compose integration, which automatically
starts required infrastructure services:

```bash
# Start the IAM service - this will automatically start NATS via Docker Compose
nx run iam-service:bootRun
```

**Alternative: Manual Infrastructure Management**

If you prefer to manage infrastructure manually:

```bash
# Start NATS and PostgreSQL using Docker Compose
docker-compose -f infra/docker-compose/docker-compose.yml up -d

# Then start the service
nx run iam-service:bootRun
```

## CI Badges & Example Commands

Embed NX test/build status badges in your repo README:

```markdown
![CI](https://github.com/<org>/acci_eaf/actions/workflows/ci.yml/badge.svg)
```

Essential commands used by CI:

```bash
# Lint all affected projects
nx affected -t lint

# Build + test affected
nx affected -t build,test

# End-to-end documentation site check
npm run build && npm run serve | cat # non-interactive in CI
```

## Next Steps

- [Explore Architectural Principles](/architectural-principles)
- [Learn about Core Services](/core-services)
- [Browse the UI Foundation Kit](/ui-foundation-kit)

_This is a placeholder document. Detailed setup instructions will be added as the EAF development
progresses._

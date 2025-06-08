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

### 3. Start Local Infrastructure

```bash
# Start NATS and PostgreSQL using Docker Compose
docker-compose -f infra/docker-compose/dev.yml up -d
```

### 4. Run Your First EAF Service

```bash
# Start the IAM service using Nx
nx run iam-service:bootRun
```

## Next Steps

- [Explore Architectural Principles](/docs/architectural-principles)
- [Learn about Core Services](/docs/core-services)
- [Browse the UI Foundation Kit](/docs/ui-foundation-kit)

_This is a placeholder document. Detailed setup instructions will be added as the EAF development
progresses._

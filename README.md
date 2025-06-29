# 🚀 ACCI Enterprise Application Framework (EAF)

> **A modern microservices monorepo built with Nx, Kotlin, and React for enterprise applications**

[![CI](https://github.com/Broccode/acci_eaf/actions/workflows/ci.yml/badge.svg)](https://github.com/axians/acci_eaf/actions/workflows/ci.yml)
[![Nx](https://img.shields.io/badge/Nx-143055?style=for-the-badge&logo=nx&logoColor=white)](https://nx.dev/)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![React](https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)

---

## 📋 Table of Contents

- [🚀 Quick Start](#-quick-start)
- [🏗️ Project Structure](#️-project-structure)
- [🛠️ Development](#️-development)
  - [Backend Services](#backend-services)
  - [Frontend Development](#frontend-development)
- [🏛️ Architecture](#️-architecture)
- [📚 Documentation](#-documentation)
- [🔄 Development Workflow](#-development-workflow)
- [🆘 Getting Help](#-getting-help)

---

## 🚀 Quick Start

### Prerequisites

| Technology         | Version | Purpose                       |
| ------------------ | ------- | ----------------------------- |
| **Node.js**        | 18+     | Frontend tooling and Nx       |
| **npm**            | 8+      | Package management            |
| **Java (JDK)**     | 21+     | Backend services              |
| **Docker**         | Latest  | Local development environment |
| **Docker Compose** | Latest  | Service orchestration         |

### Installation & Setup

```bash
# 📦 Install dependencies
npm install

# 🔍 Explore available projects
npx nx show projects

# 🏗️ Build everything
npx nx run-many -t build

# 🧪 Test everything
npx nx run-many -t test
```

### Quick Commands

```bash
# Build specific projects
npx nx build iam-service
npx nx build ui-foundation-kit

# Run backend service
npx nx run iam-service:run

# Start documentation site
npx nx serve docs

# Launch Storybook for UI components
npx nx storybook ui-foundation-kit
```

---

## 🏗️ Project Structure

```
acci-eaf-monorepo/
├── 📱 apps/                    # Deployable applications
│   └── iam-service/           # 🔐 EAF IAM Core Service
├── 📚 libs/                    # Shared libraries
│   ├── eaf-core/              # 🎯 Core EAF utilities
│   ├── eaf-eventing-sdk/      # 📡 Event-driven messaging
│   ├── eaf-eventsourcing-sdk/ # 📝 Event sourcing framework
│   └── ui-foundation-kit/     # 🎨 UI Components & Themes
├── 🛠️ tools/                   # Development tools
├── 📖 docs/                    # Documentation site
├── 🏗️ infra/                   # Infrastructure definitions
│   ├── ansible/               # Configuration management
│   └── docker-compose/        # Local development stack
├── ⚙️ nx.json                  # Nx workspace configuration
├── 📦 package.json             # Root package.json
└── 🏗️ build.gradle.kts         # Root Gradle build
```

---

## 🛠️ Development

### Backend Services

> **Built with**: Kotlin, Spring Boot, Gradle  
> **Architecture**: Microservices with CQRS/Event Sourcing

```bash
# 🏗️ Build a backend service
npx nx build iam-service

# 🧪 Run tests
npx nx test iam-service

# 🚀 Start the service
npx nx run iam-service:run

# 🧹 Clean build artifacts
npx nx clean iam-service
```

**Available Backend Projects:**

- `iam-service` - Identity and Access Management core service

### Frontend Development

> **Built with**: TypeScript, React, Vaadin components

```bash
# 🏗️ Build UI Foundation Kit
npx nx build ui-foundation-kit

# 📖 Run Storybook for component development
npx nx storybook ui-foundation-kit

# 🧪 Run tests
npx nx test ui-foundation-kit

# 📚 Serve documentation site
npx nx serve docs
```

**Available Frontend Projects:**

- `ui-foundation-kit` - Reusable React/Vaadin components and themes
- `docs` - Documentation site built with Docusaurus

---

## 🏛️ Architecture

The EAF follows a **modern microservices architecture** with these key principles:

### 🎯 Core Services

- **🔐 IAM Service** - Identity and Access Management
- **📜 License Management** - Software licensing and entitlements
- **🚩 Feature Flags** - Dynamic feature control

### 🛠️ Shared Libraries

- **🎯 EAF Core** - Common utilities and base components
- **📡 Eventing SDK** - Event-driven communication layer
- **📝 Event Sourcing SDK** - CQRS/ES implementation with Axon Framework
- **🎨 UI Foundation Kit** - Reusable React/Vaadin components

### 🔄 Communication Patterns

- **📡 Event-Driven**: NATS/JetStream for async messaging
- **📝 CQRS/ES**: Axon Framework with PostgreSQL event store
- **🌐 Multi-Tenant**: Account-based isolation for data and events

### 🏗️ Infrastructure

- **🐳 Containerized**: Docker & Docker Compose for local development
- **📊 Monitoring**: NATS monitoring interface and health checks
- **🗃️ Storage**: PostgreSQL for event store and projections

---

## 📚 Documentation

| Document                                                    | Description                     |
| ----------------------------------------------------------- | ------------------------------- |
| [📋 Project Structure](docs/project-structure.md)           | Detailed project organization   |
| [🛠️ Tech Stack](docs/tech-stack.md)                         | Technology choices and versions |
| [📐 Operational Guidelines](docs/operational-guidelines.md) | Coding standards and practices  |
| [🐳 Docker Setup](infra/docker-compose/README.md)           | Local development environment   |

### 🚀 Getting Started Guides

- **Backend Development**: Start with `iam-service` for Kotlin/Spring Boot patterns
- **Frontend Development**: Explore `ui-foundation-kit` Storybook for component library
- **Event Sourcing**: Check `eaf-eventsourcing-sdk` for CQRS/ES examples
- **Multi-Tenancy**: Review NATS configuration in `infra/docker-compose/`

Looking for deeper guidance? Visit the **Launchpad Developer Portal** (Docusaurus-powered) by
running `nx serve docs` locally or browsing the hosted site (coming soon). All dependency versions
live in the [Version Matrix](docs/docusaurus-docs/versions.md) so everyone stays in sync.

---

## 🔄 Development Workflow

1. **🎯 Isolation First** - Each feature/service is developed independently
2. **📚 Shared Libraries** - Common code goes into the `libs/` directory
3. **⚡ Nx Optimization** - Use Nx for dependency management and affected builds
4. **📐 Standards Compliance** - Follow coding standards in operational guidelines
5. **🧪 Test-Driven** - Comprehensive testing at all levels
6. **🐳 Local Environment** - Use Docker Compose for consistent development setup

### 🔧 Useful Commands

```bash
# 🔍 Show project details
npx nx show project <project-name>

# 📊 View dependency graph
npx nx graph

# 🎯 Run affected tests only
npx nx affected -t test

# 🏗️ Build affected projects only
npx nx affected -t build

# 🧹 Reset Nx cache
npx nx reset
```

---

## 🆘 Getting Help

### 📖 Documentation Resources

- 📁 **Local Docs**: Check the `docs/` directory for detailed guides
- 🛠️ **Nx Help**: Use `npx nx --help` for available commands
- 🔍 **Project Info**: Use `npx nx show project <project-name>` for project details

### 🚀 Quick References

- **Nx Workspace**: [Official Nx Documentation](https://nx.dev)
- **Spring Boot**: [Spring Boot Reference](https://spring.io/projects/spring-boot)
- **Kotlin**: [Kotlin Documentation](https://kotlinlang.org/docs/)
- **React**: [React Documentation](https://react.dev)
- **NATS**: [NATS Documentation](https://docs.nats.io)

### 🛠️ Troubleshooting

- **Build Issues**: Check project dependencies with `npx nx graph`
- **Service Issues**: Review Docker Compose logs in `infra/docker-compose/`
- **Test Failures**: Run `npx nx affected -t test` to see what's broken

---

## 🚀 Recent Updates: Axon Framework 5 Migration

**Epic 4.x Program Status**: ✅ **Foundation Complete** | 🔄 **Implementation in Progress**

### Axon 5.0.0-M2 Integration Completed

The EAF platform has been successfully migrated to **Axon Framework 5.0.0-M2**, establishing the
foundation for modern event-sourcing capabilities:

- **✅ Core EventStorageEngine**: PostgreSQL-based implementation compatible with Axon 5 API
- **✅ Multi-tenant Support**: Enhanced tenant isolation with improved context management
- **✅ Async-First Design**: CompletableFuture and MessageStream integration
- **✅ Type-Safe Configuration**: Updated Spring Boot auto-configuration for Axon 5

**Next Steps**: Integration testing, database schema optimization, and full Epic 4.x story
completion.

---

<div align="center">

**Built with ❤️ by the ACCI Engineering Team**

_Empowering enterprise applications with modern architecture_

</div>

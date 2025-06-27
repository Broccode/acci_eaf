---
sidebar_position: 3
title: Local Setup
---

# Local Setup

Now that you have all prerequisites installed, let's get your ACCI EAF development environment up
and running locally.

## 📂 Repository Setup

### Clone the Repository

```bash
# Clone the repository (replace with actual repository URL)
git clone <repository-url> acci-eaf
cd acci-eaf

# Verify you're in the right directory
ls -la
# You should see: build.gradle.kts, nx.json, apps/, libs/, docs/, etc.
```

### Install Dependencies

```bash
# Install Node.js dependencies for frontend tools and documentation
npm install

# Verify Nx is working
nx --version

# Build all projects to verify setup
nx run-many --target=build --all
```

## 🐳 Infrastructure Services

ACCI EAF uses Docker Compose to manage local infrastructure services. Let's start the required
services.

### Start Infrastructure Services

```bash
# Navigate to infrastructure directory
cd infra/docker-compose

# Start NATS and PostgreSQL services
docker compose up -d

# Verify services are running
docker compose ps
```

You should see output similar to:

```
NAME                    COMMAND                  SERVICE   STATUS
nats                    "/nats-server --conf…"   nats      Up
postgres               "docker-entrypoint.s…"   postgres  Up
```

### Service Details

#### NATS Message Broker

- **Port**: 4222
- **Management UI**: `http://localhost:8222`
- **Connection String**: `nats://localhost:4222`

#### PostgreSQL Database

- **Port**: 5432
- **Database**: `eaf_db`
- **Username**: `postgres`
- **Password**: `password`
- **Connection String**: `jdbc:postgresql://localhost:5432/eaf_db`

### Verify Infrastructure

```bash
# Test NATS connection
curl -s http://localhost:8222/varz | grep server_name

# Test PostgreSQL connection (requires psql client)
psql -h localhost -p 5432 -U postgres -d eaf_db -c "SELECT version();"
# Enter password: password
```

## 🚀 Running Your First Service

Let's verify everything works by running the existing IAM service.

### Start the IAM Service

```bash
# Navigate back to project root
cd ../..

# Start the IAM service
nx run iam-service:run

# Alternative: Use bootRun for Spring Boot specific features
nx run iam-service:bootRun
```

### Verify Service is Running

```bash
# In a new terminal, test the health endpoint
curl -s http://localhost:8080/actuator/health

# You should see:
# {"status":"UP"}
```

The service should start successfully and connect to the local infrastructure services
automatically.

## 🔧 IDE Configuration

### IntelliJ IDEA Setup

#### Import Project

1. **File → Open** → Select the `acci-eaf` directory
2. **Trust the Gradle project** when prompted
3. Wait for Gradle sync to complete
4. Verify you see the project structure in the Project view

#### Configure Kotlin

1. **File → Settings → Languages & Frameworks → Kotlin**
2. Ensure **Kotlin version** matches project version
3. Check **Coroutines** support is enabled

#### Configure Spring Boot

1. Install **Spring Boot** plugin (Ultimate edition)
2. **File → Settings → Build → Build Tools → Gradle**
3. Set **Build and run using**: Gradle
4. Set **Run tests using**: Gradle

#### Useful Settings

```
# Enable annotation processing
File → Settings → Build → Compiler → Annotation Processors
✓ Enable annotation processing

# Configure code style
File → Settings → Editor → Code Style → Kotlin
Import scheme: Use Kotlin official style guide

# Configure ktlint
File → Settings → Tools → Actions on Save
✓ Reformat code
✓ Optimize imports
```

### VS Code Setup

#### Open Project

```bash
# In the project root
code .
```

#### Recommended Workspace Settings

Create `.vscode/settings.json`:

```json
{
  "typescript.preferences.importModuleSpecifier": "relative",
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  },
  "files.exclude": {
    "**/node_modules": true,
    "**/dist": true,
    "**/.nx": true
  }
}
```

#### Install Extensions

Use the Extension view (Ctrl+Shift+X) to install:

- ES7+ React/Redux/React-Native snippets
- TypeScript Hero
- Prettier - Code formatter
- ESLint
- GitLens

## 🧪 Verification Tests

Run these commands to ensure everything is working correctly:

### Backend Tests

```bash
# Run all backend tests
nx run-many --target=test --projects=iam-service,eaf-core,eaf-eventing-sdk,eaf-eventsourcing-sdk

# Run specific service tests
nx test iam-service

# Check code quality
nx run-many --target=ktlintCheck --all
```

### Frontend Tests

```bash
# Run UI Foundation Kit tests
nx test ui-foundation-kit

# Run Storybook to see components
nx storybook ui-foundation-kit
# Opens http://localhost:6006
```

### Documentation

```bash
# Start The Launchpad documentation site
nx serve docs
# Opens http://localhost:3000
```

### Full Integration Test

```bash
# Start infrastructure if not running
cd infra/docker-compose && docker compose up -d && cd ../..

# Start IAM service
nx run iam-service:run &

# Wait for service to start, then test
sleep 10
curl -s http://localhost:8080/actuator/health

# Stop the service
pkill -f iam-service
```

## 📁 Understanding the Project Structure

Your workspace should now look like this:

```
acci-eaf/
├── apps/                    # Application services
│   └── iam-service/        # Identity & Access Management service
├── libs/                   # Shared libraries
│   ├── eaf-core/          # Core utilities and interfaces
│   ├── eaf-eventing-sdk/  # NATS event publishing/consuming
│   ├── eaf-eventsourcing-sdk/ # Event sourcing with PostgreSQL
│   ├── eaf-iam-client/    # IAM client SDK
│   └── ui-foundation-kit/ # React component library
├── tools/                  # Development tools
│   └── acci-eaf-cli/      # Code generation CLI
├── docs/                   # Documentation (The Launchpad)
├── infra/                  # Infrastructure as Code
│   └── docker-compose/    # Local development services
├── build.gradle.kts       # Root Gradle build file
├── nx.json                # Nx workspace configuration
└── package.json           # Node.js dependencies
```

## 🌐 Port Reference

Keep track of these ports for local development:

| Service          | Port | URL                                     | Purpose          |
| ---------------- | ---- | --------------------------------------- | ---------------- |
| IAM Service      | 8080 | `http://localhost:8080`                 | Main application |
| NATS             | 4222 | nats://localhost:4222                   | Message broker   |
| NATS Management  | 8222 | `http://localhost:8222`                 | NATS monitoring  |
| PostgreSQL       | 5432 | jdbc:postgresql://localhost:5432/eaf_db | Database         |
| Docs (Launchpad) | 3000 | `http://localhost:3000`                 | Documentation    |
| Storybook        | 6006 | `http://localhost:6006`                 | UI components    |

## 🔧 Troubleshooting

### Common Issues

#### Docker Services Won't Start

```bash
# Check if ports are in use
lsof -i :4222  # NATS
lsof -i :5432  # PostgreSQL

# Stop conflicting services
docker compose down

# Clean Docker system (if needed)
docker system prune -a
```

#### Gradle Build Failures

```bash
# Clean and rebuild
./gradlew clean build

# Check Java version
java -version
# Should be 17+

# Reset Gradle daemon
./gradlew --stop
```

#### Node.js Issues

```bash
# Clear npm cache
npm cache clean --force

# Remove node_modules and reinstall
rm -rf node_modules package-lock.json
npm install
```

#### IDE Issues

- **IntelliJ**: File → Invalidate Caches and Restart
- **VS Code**: Reload window (Ctrl+Shift+P → "Developer: Reload Window")

### Getting Help

If you're stuck:

1. Check the [Troubleshooting](./troubleshooting.md) guide for detailed solutions
2. Verify all prerequisites are correctly installed
3. Ensure Docker services are running: `docker compose ps`
4. Check service logs: `docker compose logs <service-name>`

## 🎉 Success

You should now have:

- ✅ Local repository cloned and dependencies installed
- ✅ Infrastructure services running (NATS, PostgreSQL)
- ✅ IAM service starting successfully
- ✅ IDE configured and ready for development
- ✅ All verification tests passing

## 🚀 Next Steps

Now you're ready to create your first EAF service! Continue to
[Your First Service](./first-service.md) to use the ACCI EAF CLI and learn about hexagonal
architecture.

---

**Great job!** Your development environment is now ready for productive EAF development! 🎯

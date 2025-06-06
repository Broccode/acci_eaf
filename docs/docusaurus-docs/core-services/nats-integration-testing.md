---
sidebar_position: 3
title: NATS SDK Integration Testing
---

# Integration Tests for EAF NATS SDK

## Overview

The integration tests for the EAF NATS SDK are divided into different categories:

1. **Unit Tests**: Run automatically with every build
2. **Basic Integration Tests**: Require a local NATS server
3. **Enhanced Integration Tests**: Use the docker-compose infrastructure
4. **Full Integration Tests**: Use Testcontainers (temporarily disabled)

## Unit Tests

Unit tests run automatically:

```bash
nx test eaf-eventing-sdk
```

## Enhanced Integration Tests (Recommended)

### Docker-Compose NATS Infrastructure

The project includes a complete NATS infrastructure in `infra/docker-compose/`:

- **NATS Server**: With JetStream and persistent storage
- **Multi-Tenant Setup**: Pre-configured accounts (TENANT_A)
- **Monitoring**: HTTP interface on port 8222
- **Persistence**: File-based JetStream storage

### Starting the NATS Server

```bash
# Using the project's docker-compose infrastructure
cd infra/docker-compose
docker-compose up -d nats

# Check status
curl http://localhost:8222/
```

### Running Enhanced Tests

```bash
# Run Enhanced Integration Tests (uses docker-compose setup)
nx test eaf-eventing-sdk --args="-Dnats.integration.enhanced=true"

# With specific NATS URL (if required)
nx test eaf-eventing-sdk --args="-Dnats.integration.enhanced=true -Dnats.url=nats://localhost:4222"
```

### Available Enhanced Tests

- `NatsEventPublisherIntegrationTest`: Comprehensive publisher tests
  - Tenant-specific subject creation
  - Metadata processing
  - Multi-tenant publishing
  - Event envelope validation
  - Large payload handling
  - Docker-compose compatibility
- `NatsJetStreamConsumerIntegrationTest`: Complete consumer tests
  - Stream and consumer creation with file storage
  - Publish/subscribe with persistent storage
  - Message acknowledgment (ack/nak) with retry behavior
  - MessageContext integration
  - Tenant-specific subject patterns (TENANT_A.events.>)

## Basic Integration Tests

### Starting NATS Server (Alternative)

```bash
# With Docker (simple variant)
docker run --rm -p 4222:4222 -p 8222:8222 nats:2.10.22-alpine -js -m 8222

# Or with nats-server binary (if installed)
nats-server -js -m 8222
```

### Running Tests

```bash
# Run Basic Integration Tests
nx test eaf-eventing-sdk --args="-Dnats.integration.enabled=true"
```

### Available Basic Tests

- `NatsJetStreamBasicIntegrationTest`: Basic JetStream functionality
  - Stream and consumer creation
  - Publish/subscribe operations
  - Message acknowledgment (ack/nak)
  - MessageContext functionality

## Test Categories Overview

| Test Type              | Activation                         | Infrastructure | Purpose                       |
| ---------------------- | ---------------------------------- | -------------- | ----------------------------- |
| **Unit Tests**         | Automatic                          | Mocks          | Fast feedback loop            |
| **Basic Integration**  | `-Dnats.integration.enabled=true`  | Local NATS     | Simple JetStream tests        |
| **Enhanced Integration** | `-Dnats.integration.enhanced=true` | Docker-Compose | Complete infrastructure tests |
| **Full Integration**   | Disabled                           | Testcontainers | Isolated container tests      |

## Docker-Compose Configuration

### NATS Server Features

- **JetStream**: Enabled with persistent file storage
- **Accounts**:
  - `SYS_ACCOUNT`: For system administration
  - `TENANT_A`: For tenant-specific tests
- **Permissions**: Tenant-specific publish/subscribe rights
- **Monitoring**: HTTP interface on port 8222
- **Persistence**: Data stored in `./nats-data`

### Configuration Files

- `infra/docker-compose/docker-compose.yml`: Docker-Compose setup
- `infra/docker-compose/nats-server-simple.conf`: NATS server configuration

## Troubleshooting

### NATS Server Not Reachable

```
Failed to connect to NATS server at nats://localhost:4222
```

**Solution**: Ensure the NATS server is running:

```bash
# With docker-compose
cd infra/docker-compose
docker-compose up -d nats

# Check status
curl http://localhost:8222/
docker-compose ps
```

### JetStream Not Enabled

```
JetStream not enabled
```

**Solution**: The docker-compose configuration enables JetStream automatically. Ensure the correct container is running:

```bash
docker-compose logs nats
```

### Deleting Persistent Data

```bash
# Stop NATS and delete data
cd infra/docker-compose
docker-compose down
sudo rm -rf nats-data/

# Restart
docker-compose up -d nats
```

### Port Conflicts

```
Port 4222 already in use
```

**Solution**: Stop other NATS instances:

```bash
# Stop Docker containers
docker ps | grep nats
docker stop <container-id>

# Or all containers
docker-compose down
```

## Development

### Adding New Integration Tests

1. **Enhanced Tests**: Extend existing test classes with `@EnabledIfSystemProperty(named = "nats.integration.enhanced")`
2. **Basic Tests**: Use `@EnabledIfSystemProperty(named = "nats.integration.enabled")`
3. **Use TENANT_A**: Utilize pre-configured tenant structure

### Test Isolation

Each test should:

- Use unique stream names (prefix `TEST_`)
- Perform cleanup in `@AfterEach`
- Have no dependencies on other tests
- For Enhanced Tests: Use file storage (like production)

### Best Practices

- **Enhanced Tests**: Use file storage for realistic tests
- **Basic Tests**: Use memory storage for speed
- **Timeouts**: Set reasonable timeouts (5-10 seconds)
- **Tenant-Awareness**: Test with configured tenants (TENANT_A)
- **Error Scenarios**: Test both positive and negative scenarios

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Integration Tests

jobs:
  integration-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Start NATS Infrastructure
        run: |
          cd infra/docker-compose
          docker-compose up -d nats
          sleep 10
          curl -f http://localhost:8222/ || exit 1

      - name: Run Enhanced Integration Tests
        run: nx test eaf-eventing-sdk --args="-Dnats.integration.enhanced=true"

      - name: Stop NATS Infrastructure
        if: always()
        run: |
          cd infra/docker-compose
          docker-compose down
```

### GitLab CI Example

```yaml
test:integration:
  stage: test
  services:
    - docker:dind
  before_script:
    - cd infra/docker-compose
    - docker-compose up -d nats
    - sleep 10
  script:
    - nx test eaf-eventing-sdk --args="-Dnats.integration.enhanced=true"
  after_script:
    - cd infra/docker-compose
    - docker-compose down
```

## Monitoring and Debugging

### NATS Monitoring Interface

```bash
# General server info
curl http://localhost:8222/varz

# JetStream info
curl http://localhost:8222/jsz

# Connections
curl http://localhost:8222/connz

# Health check
curl http://localhost:8222/healthz
```

### Log Analysis

```bash
# NATS container logs
docker-compose logs nats

# Live logs
docker-compose logs -f nats
```

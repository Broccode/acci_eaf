---
sidebar_position: 1
---

# Running NATS/JetStream for Local Development

This guide explains how to set up and run the NATS/JetStream eventing backbone for ACCI EAF local development, including multi-tenant configuration and basic troubleshooting.

## Overview

The ACCI EAF uses NATS with JetStream as its core eventing backbone, providing:

- **Event-Driven Architecture (EDA)** support
- **Multi-tenant isolation** via NATS accounts
- **Persistent messaging** with JetStream
- **At-least-once delivery** guarantees

## Quick Start

### 1. Navigate to Docker Compose Directory

```bash
cd infra/docker-compose
```

### 2. Start NATS Server

```bash
# Start in detached mode
docker-compose up -d

# Or start with logs visible
docker-compose up
```

### 3. Verify NATS is Running

```bash
# Check service status
docker-compose ps

# Check NATS health
curl http://localhost:8222/healthz
```

Expected output:

```
NAME       COMMAND                  SERVICE   STATUS    PORTS
eaf-nats   "/nats-server --conf…"   nats      running   0.0.0.0:4222->4222/tcp, 0.0.0.0:6222->6222/tcp, 0.0.0.0:8222->8222/tcp
```

## Multi-Tenant Configuration

The NATS server is pre-configured with three accounts for strict tenant isolation:

### System Account (SYS_ACCOUNT)

**Purpose**: EAF internal operations and system management

- **User**: `sys_admin`
- **Password**: `sys_secure_password_123!`
- **Permitted Subjects**: `$SYS.>`, `eaf.system.>`

### Tenant A Account (TENANT_A)

**Regular User**:

- **User**: `tenant_a_user`
- **Password**: `tenant_a_password_456!`
- **Permitted Subjects**: `TENANT_A.>`, `tenant-a.>`

**Admin User** (with JetStream API access):

- **User**: `tenant_a_admin`
- **Password**: `tenant_a_admin_789!`
- **Permitted Subjects**: `TENANT_A.>`, `tenant-a.>`, `$JS.API.>`

### Tenant B Account (TENANT_B)

**Regular User**:

- **User**: `tenant_b_user`  
- **Password**: `tenant_b_password_456!`
- **Permitted Subjects**: `TENANT_B.>`, `tenant-b.>`

**Admin User** (with JetStream API access):

- **User**: `tenant_b_admin`
- **Password**: `tenant_b_admin_789!`
- **Permitted Subjects**: `TENANT_B.>`, `tenant-b.>`, `$JS.API.>`

## Connection Examples

### Using NATS CLI

First, install the NATS CLI:

```bash
# macOS
brew install nats-io/nats-tools/nats

# Linux/Windows - Go install  
go install github.com/nats-io/natscli/nats@latest
```

Create and use tenant contexts:

```bash
# Create Tenant A context
nats context save tenant-a \
  --server nats://tenant_a_user:tenant_a_password_456!@localhost:4222

# Create Tenant B context  
nats context save tenant-b \
  --server nats://tenant_b_user:tenant_b_password_456!@localhost:4222

# Switch to Tenant A and publish a message
nats context select tenant-a
nats pub TENANT_A.events.test "Hello from Tenant A"

# Switch to Tenant B and publish a message
nats context select tenant-b  
nats pub TENANT_B.events.test "Hello from Tenant B"
```

### Using Application Code

#### Kotlin/Spring (EAF Backend Services)

```kotlin
import io.nats.client.Nats
import io.nats.client.Options

// Connect as Tenant A user
val options = Options.Builder()
    .server("nats://localhost:4222")
    .userInfo("tenant_a_user", "tenant_a_password_456!")
    .build()

val connection = Nats.connect(options)

// Publish an event
connection.publish("TENANT_A.events.user.created", eventJson.toByteArray())

// Subscribe to events
val subscription = connection.subscribe("TENANT_A.events.>") { msg ->
    println("Received: ${String(msg.data)}")
}
```

#### TypeScript/JavaScript (Frontend/Node.js)

```typescript
import { connect } from 'nats';

// Connect as Tenant A user
const nc = await connect({
    servers: 'nats://tenant_a_user:tenant_a_password_456!@localhost:4222'
});

// Publish an event
nc.publish('TENANT_A.events.user.login', JSON.stringify({
    userId: '123',
    timestamp: new Date().toISOString()
}));

// Subscribe to events
const sub = nc.subscribe('TENANT_A.events.user.>');
for await (const msg of sub) {
    console.log('Received:', new TextDecoder().decode(msg.data));
}
```

## JetStream Usage Examples

JetStream provides persistent, ordered messaging with at-least-once delivery guarantees.

### Creating a Stream (Admin User Required)

```bash
# Switch to Tenant A admin context
nats context save tenant-a-admin \
  --server nats://tenant_a_admin:tenant_a_admin_789!@localhost:4222

nats context select tenant-a-admin

# Create a stream for user events
nats stream create TENANT_A_USER_EVENTS \
  --subjects "TENANT_A.events.user.>" \
  --storage file \
  --replicas 1 \
  --max-age 7d
```

### Publishing to JetStream

```bash
# Publish persistent messages
nats pub TENANT_A.events.user.created '{"userId":"user-123","email":"user@example.com","timestamp":"2025-01-16T10:00:00Z"}'
nats pub TENANT_A.events.user.updated '{"userId":"user-123","email":"newemail@example.com","timestamp":"2025-01-16T11:00:00Z"}'
```

### Creating Consumers

```bash
# Create a consumer for a user service
nats consumer create TENANT_A_USER_EVENTS user_service \
  --filter "TENANT_A.events.user.>" \
  --deliver all \
  --ack explicit \
  --max-deliver 3
```

### Consuming Messages

```bash
# Consume messages one by one
nats consumer next TENANT_A_USER_EVENTS user_service

# Consume messages continuously  
nats consumer sub TENANT_A_USER_EVENTS user_service
```

## Monitoring and Management

### HTTP Monitoring Interface

Access the NATS monitoring dashboard at: **<http://localhost:8222>**

This provides real-time information about:

- Server connections and subscriptions
- Account usage and permissions
- JetStream streams and consumers
- Performance metrics

### Command Line Monitoring

```bash
# Server information
nats server info

# List streams (as admin user)
nats stream list

# Stream details
nats stream info TENANT_A_USER_EVENTS

# Consumer information
nats consumer list TENANT_A_USER_EVENTS
nats consumer info TENANT_A_USER_EVENTS user_service
```

## Basic Troubleshooting

### Common Issues and Solutions

#### 1. Connection Refused

**Symptoms**: Cannot connect to NATS server

```
Error: nats: no servers available for connection
```

**Solutions**:

```bash
# Check if NATS container is running
docker-compose ps

# Check NATS logs
docker-compose logs nats

# Restart the service
docker-compose restart nats
```

#### 2. Permission Denied

**Symptoms**: Cannot publish/subscribe to subjects

```
Error: nats: Permissions Violation for Publish to "TENANT_B.events.test"
```

**Solutions**:

- Verify you're using the correct tenant credentials
- Check that your subject pattern matches the account's allowed subjects
- Ensure you're not trying to access another tenant's subjects

#### 3. JetStream Not Available

**Symptoms**: Cannot create streams or consumers

```
Error: nats: JetStream not enabled
```

**Solutions**:

```bash
# Check JetStream status
nats server info | grep -i jetstream

# Verify configuration and restart
docker-compose restart nats

# Check for sufficient disk space
df -h
```

#### 4. Data Persistence Issues

**Symptoms**: Messages or streams disappear after restart

**Solutions**:

```bash
# Check data directory permissions
ls -la infra/docker-compose/nats-data/

# Verify volume mounting
docker inspect eaf-nats | grep -A 10 Mounts

# Reset data if corrupted
docker-compose down
rm -rf infra/docker-compose/nats-data/*
docker-compose up -d
```

### Useful Debugging Commands

```bash
# View live NATS logs
docker-compose logs -f nats

# Access NATS server logs inside container  
docker exec -it eaf-nats cat /data/nats-server.log

# Test basic connectivity
nats server check --server nats://localhost:4222

# List all contexts
nats context list

# Check current context
nats context show
```

### Resetting the Environment

To completely reset NATS (remove all streams, consumers, and messages):

```bash
# Stop services
docker-compose down

# Remove all persistent data
rm -rf infra/docker-compose/nats-data/*

# Restart services  
docker-compose up -d
```

## Security Considerations

:::warning Development Only
The credentials shown in this guide are for **local development only**. Never use these passwords in production environments.
:::

For production deployments:

- Use strong, randomly generated passwords
- Enable TLS encryption for all connections
- Implement proper secret management (e.g., environment variables, vault)
- Regularly rotate credentials
- Enable audit logging
- Consider NATS clustering for high availability

## Next Steps

Once you have NATS running locally:

1. **Explore the EAF Eventing SDK** - Learn how to integrate NATS with your Kotlin/Spring services
2. **Review Event-Driven Architecture patterns** - Understand CQRS/ES implementation with NATS
3. **Try the Tutorial Examples** - Follow hands-on examples using the multi-tenant setup
4. **Join the EAF Community** - Connect with other developers building on the EAF platform

## Related Documentation

- [NATS Official Documentation](https://docs.nats.io/)
- [JetStream Concepts](https://docs.nats.io/jetstream)
- [EAF Architecture Overview](../architectural-principles/overview)
- [Docker Compose Reference](../../infra/docker-compose/README.md)

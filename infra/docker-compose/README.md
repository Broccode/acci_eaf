# ACCI EAF Docker Compose Setup

This directory contains the Docker Compose configuration for running the ACCI EAF local development environment.

## Services

### NATS Server with JetStream

- **Image**: `nats:latest`
- **Container Name**: `eaf-nats`
- **Client Port**: 4222
- **HTTP Monitoring**: 8222
- **Routing Port**: 6222 (for future clustering)

## Quick Start

1. **Navigate to the docker-compose directory**:

   ```bash
   cd infra/docker-compose
   ```

2. **Start the services**:

   ```bash
   docker-compose up -d
   ```

3. **Check service status**:

   ```bash
   docker-compose ps
   ```

4. **View logs**:

   ```bash
   # All services
   docker-compose logs -f
   
   # NATS only
   docker-compose logs -f nats
   ```

5. **Stop the services**:

   ```bash
   docker-compose down
   ```

## NATS Multi-Tenant Configuration

> **Current Status**: The active configuration uses `nats-server-simple.conf` which includes System Account and Tenant A only. A full configuration with Tenant B is available in `nats-server.conf` but requires debugging before activation.

The NATS server is currently configured with two accounts for multi-tenant isolation:

### System Account (SYS_ACCOUNT)

- **Purpose**: EAF internal operations and system management
- **User**: `sys_admin`
- **Password**: `sys_secure_password_123!`
- **Subjects**: `$SYS.>`, `eaf.system.>`

### Tenant A Account (TENANT_A) - **ACTIVE**

- **Regular User**:
  - **User**: `tenant_a_user`
  - **Password**: `tenant_a_password_456!`
  - **Subjects**: `TENANT_A.>`, `tenant-a.>`

### Tenant B Account (TENANT_B) - **PLANNED**

> **Note**: Tenant B configuration is available in `nats-server.conf` but not currently active due to startup issues. To enable:
>
> 1. Debug the startup issues in `nats-server.conf`
> 2. Update `docker-compose.yml` to mount `./nats-server.conf:/etc/nats/nats-server.conf:ro`

- **Regular User** (when enabled):
  - **User**: `tenant_b_user`
  - **Password**: `tenant_b_password_456!`
  - **Subjects**: `TENANT_B.>`, `tenant-b.>`

- **Admin User** (when enabled):
  - **User**: `tenant_b_admin`
  - **Password**: `tenant_b_admin_789!`
  - **Subjects**: `TENANT_B.>`, `tenant-b.>`, JetStream API access

## Connection Examples

### Using NATS CLI

1. **Install NATS CLI** (if not already installed):

   ```bash
   # macOS
   brew install nats-io/nats-tools/nats
   
   # Go install
   go install github.com/nats-io/natscli/nats@latest
   ```

2. **Connect as Tenant A User** (currently active):

   ```bash
   nats context save tenant-a \
     --server nats://tenant_a_user:tenant_a_password_456!@localhost:4222
   
   nats context select tenant-a
   nats pub TENANT_A.events.test "Hello from Tenant A"
   ```

3. **Connect as Tenant B User** (available when full config is enabled):

   ```bash
   # Note: This will only work after switching to nats-server.conf
   nats context save tenant-b \
     --server nats://tenant_b_user:tenant_b_password_456!@localhost:4222
   
   nats context select tenant-b
   nats pub TENANT_B.events.test "Hello from Tenant B"
   ```

### Using Application Code

#### Kotlin/Spring Example

```kotlin
// Tenant A connection
val options = Options.Builder()
    .server("nats://localhost:4222")
    .userInfo("tenant_a_user", "tenant_a_password_456!")
    .build()
val connection = Nats.connect(options)
```

#### JavaScript/Node.js Example

```javascript
// Tenant A connection
const { connect } = require('nats');
const nc = await connect({
    servers: 'nats://tenant_a_user:tenant_a_password_456!@localhost:4222'
});
```

## JetStream Usage

### Creating a Stream (Tenant A Admin)

```bash
nats context select tenant-a
nats stream create TENANT_A_EVENTS \
  --subjects "TENANT_A.events.>" \
  --storage file \
  --replicas 1
```

### Publishing to Stream

```bash
nats pub TENANT_A.events.user.created '{"userId":"123","name":"John Doe"}'
```

### Creating a Consumer

```bash
nats consumer create TENANT_A_EVENTS user_service \
  --filter "TENANT_A.events.user.>" \
  --deliver all \
  --ack explicit
```

## Monitoring

### HTTP Monitoring Interface

Access the NATS monitoring interface at: <http://localhost:8222>

### Health Check

```bash
# Manual health check
curl http://localhost:8222/healthz

# Using NATS CLI
nats server check --server nats://localhost:4222
```

## Troubleshooting

### Common Issues

1. **Connection Refused**:
   - Check if the service is running: `docker-compose ps`
   - Verify port 4222 is not in use by another process
   - Check logs: `docker-compose logs nats`

2. **Permission Denied**:
   - Verify you're using the correct credentials for the tenant
   - Check that your subject matches the tenant's allowed patterns
   - Ensure you're connecting to the right account

3. **JetStream Not Available**:
   - Verify JetStream is enabled in the configuration
   - Check that the data directory has proper permissions
   - Ensure sufficient disk space is available

4. **Data Persistence Issues**:
   - Check that the `nats-data` directory exists and is writable
   - Verify Docker volume mounting is working correctly

### Logs and Debugging

```bash
# View NATS server logs
docker-compose logs -f nats

# Access NATS server logs inside container
docker exec -it eaf-nats cat /data/nats-server.log

# Check JetStream status
nats server info --server nats://sys_admin:sys_secure_password_123!@localhost:4222
```

### Resetting Data

To reset all data (streams, consumers, messages):

```bash
docker-compose down
sudo rm -rf nats-data/*
docker-compose up -d
```

## Security Notes

⚠️ **Important**: The passwords in this configuration are for **development only**. In production environments:

1. Use strong, randomly generated passwords
2. Consider using TLS/SSL encryption
3. Implement proper secret management
4. Use environment variables for credentials
5. Enable audit logging
6. Regularly rotate credentials

## Next Steps

This setup provides the foundation for:

- Event-driven architecture implementation
- Multi-tenant event isolation
- JetStream persistent messaging
- Integration with EAF SDK components

For production deployment, consider:

- NATS clustering for high availability
- TLS encryption
- External authentication integration
- Monitoring and alerting setup

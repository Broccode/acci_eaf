# ACCI EAF Control Plane Service

A Spring Boot service providing administrative functionality for the ACCI EAF platform with Hilla
frontend integration and comprehensive EAF SDK integration.

## 🏗️ Architecture

This service follows **Hexagonal Architecture** (Ports & Adapters) with proper separation of
concerns:

```
src/main/kotlin/com/axians/eaf/controlplane/
├── ControlPlaneApplication.kt          # Spring Boot main application
├── domain/                             # Domain layer (entities, aggregates, domain services)
├── application/                        # Application layer (use cases, DTOs, ports)
└── infrastructure/                     # Infrastructure layer (adapters, configuration)
    ├── adapter/
    │   ├── input/                      # Inbound adapters (Hilla endpoints, controllers)
    │   └── outbound/                   # Outbound adapters (repositories, event publishers)
    └── configuration/                  # Spring configuration classes
```

## 🚀 Quick Start

### Prerequisites

- **Java 21** or higher
- **Node.js 18+** (for frontend development)
- **PostgreSQL 15+**
- **NATS Server 2.10+** (for event messaging)
- **EAF IAM Service** (running and accessible)

### Local Development Setup

1. **Clone and navigate to the project:**

   ```bash
   git clone <repository-url>
   cd acci_eaf/apps/acci-eaf-control-plane
   ```

2. **Start dependencies using Docker Compose:**

   ```bash
   # From project root
   cd infra/docker-compose
   docker-compose up -d postgresql nats
   ```

3. **Configure environment variables:**

   ```bash
   export DB_USERNAME=eaf_user
   export DB_PASSWORD=eaf_password
   export EAF_IAM_SERVICE_URL=http://localhost:8081
   export NATS_URL=nats://localhost:4222
   ```

4. **Run the application:**

   ```bash
   # Using Nx (recommended)
   nx serve acci-eaf-control-plane

   # Or using Gradle directly
   ./gradlew :apps:acci-eaf-control-plane:bootRun
   ```

5. **Access the application:**
   - **Web UI:** http://localhost:8080
   - **Health Check:** http://localhost:8080/actuator/health
   - **Metrics:** http://localhost:8080/actuator/prometheus

## 🔧 Configuration

### Environment Variables

| Variable                 | Description           | Default                 | Required |
| ------------------------ | --------------------- | ----------------------- | -------- |
| `DB_USERNAME`            | PostgreSQL username   | `eaf_user`              | Yes      |
| `DB_PASSWORD`            | PostgreSQL password   | `eaf_password`          | Yes      |
| `DB_HOST`                | Database host         | `localhost`             | No       |
| `DB_PORT`                | Database port         | `5432`                  | No       |
| `DB_NAME`                | Database name         | `eaf_control_plane`     | No       |
| `EAF_IAM_SERVICE_URL`    | EAF IAM service URL   | `http://localhost:8081` | Yes      |
| `NATS_URL`               | NATS server URL       | `nats://localhost:4222` | Yes      |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `local`                 | No       |

### Profiles

- **`local`** - Local development with debug logging
- **`test`** - Automated testing with H2 database
- **`staging`** - Staging environment configuration
- **`prod`** - Production environment with security hardening

### Application Configuration

Key configuration sections in `application.yml`:

```yaml
# EAF Integration
eaf:
  iam:
    service-url: ${EAF_IAM_SERVICE_URL}
  eventing:
    nats:
      url: ${NATS_URL}
  control-plane:
    features:
      tenant-creation: true
      user-management: true
      license-overview: true

# Security
security:
  require-ssl: false # true in production
  session-timeout: PT30M
  max-login-attempts: 5

# Monitoring
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

## 🧪 Testing

### Running Tests

```bash
# All tests
nx test acci-eaf-control-plane

# Unit tests only
nx test acci-eaf-control-plane --test-name-pattern="*Unit*"

# Integration tests only
nx test acci-eaf-control-plane --test-name-pattern="*Integration*"

# With coverage
nx test acci-eaf-control-plane --coverage
```

### Test Architecture

Following **Test-Driven Development (TDD)** with 3-layer testing:

1. **Layer 1: Domain Unit Tests** - Pure domain logic with MockK
2. **Layer 2: Integration Tests** - EAF SDK integration with Testcontainers
3. **Layer 3: End-to-End Tests** - Full service integration testing

### Test Categories

- **SDK Connectivity Tests** - EAF SDK connectivity validation
- **Context Propagation Tests** - Tenant/user context flow testing
- **Security Integration Tests** - Authentication and authorization flows
- **Health Check Tests** - Operational monitoring validation
- **Database Integration Tests** - Schema and migration testing
- **Event Flow Tests** - Domain event publishing and consumption
- **Error Handling Tests** - Graceful degradation testing

## 🏗️ Build & Deployment

### Local Build

```bash
# Build application
nx build acci-eaf-control-plane

# Build Docker image
docker build -t eaf/control-plane:latest .

# Run with Docker
docker run -p 8080:8080 \
  -e DB_USERNAME=eaf_user \
  -e DB_PASSWORD=eaf_password \
  -e EAF_IAM_SERVICE_URL=http://localhost:8081 \
  eaf/control-plane:latest
```

### Production Deployment

```bash
# Build production image
docker build --target production -t eaf/control-plane:1.0.0 .

# Deploy with Docker Compose
docker-compose -f docker-compose.prod.yml up -d
```

## 📊 Monitoring & Operations

### Health Checks

The service provides comprehensive health monitoring:

- **Liveness Probe:** `/actuator/health/liveness`
- **Readiness Probe:** `/actuator/health/readiness`
- **Detailed Health:** `/actuator/health`

### Custom Health Indicators

- **EAF IAM Service** - Connectivity to IAM service
- **NATS Messaging** - Message broker connectivity
- **Database** - PostgreSQL connection health
- **Application** - General application status

### Metrics & Monitoring

**Prometheus Metrics:** Available at `/actuator/prometheus`

**Key Business Metrics:**

- `control_plane_tenant_creation_total` - Total tenant creations
- `control_plane_user_management_operations_total` - User management operations
- `control_plane_login_attempts_total` - Login attempt tracking
- `control_plane_admin_actions_total` - Administrative action tracking

**SLA/SLO Targets:**

- **Availability:** 99.9% uptime
- **Response Time:** <200ms for health checks, <2s for admin operations
- **Error Rate:** <0.1% for critical operations

### Alerting

Critical alerts should be configured for:

- Service unavailability (>30 seconds)
- High error rates (>1% over 5 minutes)
- Authentication failures (>10 failures per minute)
- Database connectivity issues
- EAF service dependency outages

### Logging

**Structured JSON Logging** with correlation IDs:

```json
{
  "timestamp": "2024-01-15T10:30:00.000Z",
  "level": "INFO",
  "logger": "com.axians.eaf.controlplane",
  "message": "Tenant created successfully",
  "correlationId": "abc-123-def-456",
  "tenantId": "tenant-789",
  "userId": "user-123",
  "action": "CREATE_TENANT"
}
```

**Log Levels:**

- **ERROR:** System errors, integration failures
- **WARN:** Business rule violations, rate limiting
- **INFO:** Business events, administrative actions
- **DEBUG:** Detailed execution flow (development only)

## 🔒 Security

### Authentication & Authorization

- **EAF IAM Integration** - Centralized authentication
- **Role-Based Access Control** - TENANT_ADMIN, SUPER_ADMIN roles
- **Session Management** - Configurable timeout and security
- **Multi-Factor Authentication** - Via EAF IAM service

### Security Hardening (Production)

- **TLS/SSL Termination** - HTTPS required in production
- **Content Security Policy** - XSS protection
- **CSRF Protection** - Cross-site request forgery prevention
- **Rate Limiting** - API abuse prevention
- **Input Validation** - Comprehensive request validation

### Security Monitoring

- **Authentication Failures** - Tracking and alerting
- **Authorization Violations** - Access attempt monitoring
- **Suspicious Activity** - Pattern detection for admin operations
- **Security Audit Trail** - Complete action logging with context

## 🚨 Troubleshooting

### Common Issues

**1. Application Won't Start**

```bash
# Check configuration
grep -r "ERROR" logs/application.log

# Validate database connectivity
psql -h localhost -U eaf_user -d eaf_control_plane -c "SELECT 1;"

# Check EAF service availability
curl -f http://localhost:8081/actuator/health
```

**2. Frontend Not Loading**

```bash
# Rebuild frontend assets
nx build acci-eaf-control-plane --mode=development

# Check Hilla configuration
grep -r "Hilla" logs/application.log
```

**3. Database Migration Failures**

```bash
# Check migration status
./gradlew :apps:acci-eaf-control-plane:flywayInfo

# Repair failed migrations
./gradlew :apps:acci-eaf-control-plane:flywayRepair
```

**4. EAF SDK Integration Issues**

```bash
# Check EAF service connectivity
curl -f ${EAF_IAM_SERVICE_URL}/actuator/health
nc -zv localhost 4222  # NATS connectivity

# Review EAF SDK logs
grep -r "com.axians.eaf" logs/application.log
```

### Debug Mode

Enable debug logging for troubleshooting:

```yaml
logging:
  level:
    com.axians.eaf: DEBUG
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
```

### Performance Troubleshooting

**Database Performance:**

```sql
-- Check slow queries
SELECT query, calls, total_time, mean_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
```

**JVM Performance:**

```bash
# Memory analysis
jstat -gc <pid>

# Thread dump
jstack <pid> > thread-dump.txt
```

## 🔄 Disaster Recovery

### Backup Procedures

**Database Backup:**

```bash
# Daily backup
pg_dump -h $DB_HOST -U $DB_USERNAME $DB_NAME > backup_$(date +%Y%m%d).sql

# Automated backup with retention
./scripts/backup-database.sh --retention-days=30
```

**Configuration Backup:**

- All configuration is version-controlled in Git
- Environment-specific secrets managed via secure vault
- Docker images tagged and stored in registry

### Recovery Procedures

**Database Recovery:**

```bash
# Restore from backup
psql -h $DB_HOST -U $DB_USERNAME $DB_NAME < backup_20240115.sql

# Verify data integrity
./scripts/verify-data-integrity.sh
```

**Service Recovery:**

```bash
# Quick service restart
docker-compose restart control-plane

# Full environment recovery
./scripts/disaster-recovery.sh --environment=prod
```

## 📝 API Documentation

### Hilla Endpoints

The service exposes type-safe @BrowserCallable endpoints:

**Health Endpoint:**

- `GET /health` - Service health status with detailed information

**User Info Endpoint:**

- `GET /userinfo` - Current user information with tenant context

### REST API (Fallback)

**Health Check:**

- `GET /actuator/health` - Comprehensive health status
- `GET /actuator/health/liveness` - Liveness probe
- `GET /actuator/health/readiness` - Readiness probe

**Metrics:**

- `GET /actuator/prometheus` - Prometheus-compatible metrics
- `GET /actuator/metrics` - Application metrics

## 🏢 Production Operations

### Deployment Checklist

**Pre-Deployment:**

- [ ] Database migration scripts validated
- [ ] Configuration parameters reviewed
- [ ] Security scanning completed
- [ ] Load testing performed
- [ ] Monitoring dashboards configured

**Deployment:**

- [ ] Blue-green deployment strategy
- [ ] Database migrations applied
- [ ] Health checks validated
- [ ] Performance baselines verified
- [ ] Rollback plan prepared

**Post-Deployment:**

- [ ] Monitoring alerts configured
- [ ] Log aggregation verified
- [ ] Security monitoring active
- [ ] Backup procedures tested
- [ ] Documentation updated

### Performance Baselines

**Expected Performance (Production):**

- **Startup Time:** <60 seconds
- **Memory Usage:** <512MB steady state
- **CPU Usage:** <10% under normal load
- **Response Times:** <200ms for health checks, <2s for admin operations
- **Throughput:** 100+ concurrent admin operations

### Capacity Planning

**Resource Requirements:**

- **CPU:** 2 cores minimum, 4 cores recommended
- **Memory:** 1GB minimum, 2GB recommended
- **Storage:** 10GB minimum for logs and temporary files
- **Network:** 1Gbps for high availability deployments

## 🤝 Contributing

### Development Workflow

1. **Feature Development** - Follow TDD approach with domain-first testing
2. **Code Review** - All changes require peer review
3. **Testing** - Maintain >90% test coverage
4. **Documentation** - Update relevant documentation
5. **Security** - Security review for all changes

### Code Standards

- **Kotlin Coding Standards** - Follow project ktlint configuration
- **Architectural Boundaries** - Maintain hexagonal architecture
- **Domain-Driven Design** - Use ubiquitous language
- **Test-Driven Development** - Tests first, implementation second

## 📞 Support

### Internal Support

- **Development Team** - Internal development questions
- **EAF Core Team** - EAF SDK integration issues
- **DevOps Team** - Infrastructure and deployment support
- **Security Team** - Security-related concerns

### Escalation Procedures

- **EAF SDK Issues** - Escalate to EAF core team within 1 business day
- **Security Vulnerabilities** - Immediate escalation to security team
- **Performance Issues** - Escalate to architecture team
- **Integration Failures** - Escalate to DevOps team

---

**Version:** 1.0.0  
**Last Updated:** $(date +%Y-%m-%d)  
**Maintained By:** EAF Development Team

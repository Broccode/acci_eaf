# EAF System Initialization Guide

This guide explains how to use the streamlined single-tenant EAF setup functionality for quick
system initialization.

## Overview

The EAF IAM Service provides an automatic system initialization feature that creates:

- A default tenant (configurable name)
- A default SuperAdmin user (configurable email)
- The SuperAdmin is automatically assigned as the Tenant Administrator for the default tenant

This is ideal for single-tenant deployments or initial system setup.

## Configuration

The system initialization is controlled by the following configuration properties in
`application.properties`:

```properties
# System Initialization Configuration
# Enable automatic creation of default tenant and superadmin user on startup
eaf.system.initialize-default-tenant=true

# Optional: Customize the default tenant name (defaults to "DefaultTenant")
eaf.system.default-tenant-name=MyCompanyTenant

# Optional: Customize the default superadmin email (defaults to "admin@example.com")
eaf.system.default-super-admin-email=admin@mycompany.com
```

## Usage Scenarios

### Scenario 1: Enable Auto-Initialization (Recommended for Single-Tenant)

1. **Configure the IAM Service** by setting the properties in
   `apps/iam-service/src/main/resources/application.properties`:

   ```properties
   eaf.system.initialize-default-tenant=true
   eaf.system.default-tenant-name=MyCompany
   eaf.system.default-super-admin-email=admin@mycompany.com
   ```

2. **Start the IAM Service**:

   ```bash
   # From the project root
   nx serve iam-service
   ```

3. **Verify Initialization** - Check the logs for messages like:

   ```
   INFO  c.a.e.i.i.a.i.DataInitializerRunner : Starting system initialization process...
   INFO  c.a.e.i.a.s.SystemInitializationService : System initialization completed successfully
   INFO  c.a.e.i.a.s.SystemInitializationService : Created default tenant: MyCompany (ID: ...)
   INFO  c.a.e.i.a.s.SystemInitializationService : Created superadmin user: admin@mycompany.com (ID: ...)
   ```

### Scenario 2: Manual Initialization (For Development/Testing)

1. **Disable Auto-Initialization**:

   ```properties
   eaf.system.initialize-default-tenant=false
   ```

2. **Use the SystemInitializationService programmatically** (for testing or custom initialization
   logic):

   ```kotlin
   @Autowired
   private lateinit var systemInitializationService: SystemInitializationService

   fun initializeSystem() {
       val result = systemInitializationService.initializeDefaultTenantIfRequired()
       if (result.wasInitialized) {
           println("System initialized with tenant: ${result.message}")
       } else {
           println("System already initialized: ${result.message}")
       }
   }
   ```

## Deployment Script Example

For automated deployments, you can create a deployment script:

```bash
#!/bin/bash
# deploy-single-tenant-eaf.sh

echo "Deploying Single-Tenant EAF Setup..."

# Set environment variables for configuration
export EAF_SYSTEM_INITIALIZE_DEFAULT_TENANT=true
export EAF_SYSTEM_DEFAULT_TENANT_NAME="${TENANT_NAME:-DefaultTenant}"
export EAF_SYSTEM_DEFAULT_SUPER_ADMIN_EMAIL="${ADMIN_EMAIL:-admin@example.com}"

# Start the IAM service
echo "Starting IAM Service with auto-initialization..."
java -jar iam-service.jar \
  --eaf.system.initialize-default-tenant=${EAF_SYSTEM_INITIALIZE_DEFAULT_TENANT} \
  --eaf.system.default-tenant-name="${EAF_SYSTEM_DEFAULT_TENANT_NAME}" \
  --eaf.system.default-super-admin-email="${EAF_SYSTEM_DEFAULT_SUPER_ADMIN_EMAIL}"

echo "EAF System initialization complete!"
echo "SuperAdmin account created: ${EAF_SYSTEM_DEFAULT_SUPER_ADMIN_EMAIL}"
echo "Default tenant created: ${EAF_SYSTEM_DEFAULT_TENANT_NAME}"
```

Usage:

```bash
# Use defaults
./deploy-single-tenant-eaf.sh

# Or customize
TENANT_NAME="AcmeCorpTenant" ADMIN_EMAIL="admin@acmecorp.com" ./deploy-single-tenant-eaf.sh
```

## Security Considerations

1. **Password Setup**: The SuperAdmin user is created without a password. You'll need to implement a
   password reset/setup flow.

2. **Email Notifications**: Currently, invitation details are logged as placeholders. In production,
   integrate with an email service.

3. **Environment-Specific Configuration**: Use different configuration for different environments:
   - **Development**: Auto-initialization enabled with test data
   - **Production**: Auto-initialization disabled, manual setup preferred

## Idempotency

The initialization process is idempotent:

- If a tenant with the configured name already exists, initialization is skipped
- If a user with the configured email already exists, initialization is skipped
- Multiple application restarts will not create duplicate data

## Troubleshooting

### Common Issues

1. **"System already initialized" message**:

   - This is normal if the system has been initialized before
   - Check the database for existing tenants/users

2. **Database connection errors during initialization**:

   - Ensure PostgreSQL is running and accessible
   - Check database connection configuration

3. **Initialization disabled but expected to run**:
   - Verify `eaf.system.initialize-default-tenant=true` in configuration
   - Check for conflicting property sources

### Logs to Monitor

- `DataInitializerRunner`: Shows when initialization starts
- `SystemInitializationService`: Shows detailed initialization progress
- `TenantPersistenceAdapter`: Shows database operations

## Integration with Control Plane

Once the system is initialized:

1. The SuperAdmin can log into the Control Plane (when implemented)
2. The SuperAdmin has full access to the default tenant
3. Additional tenants can be created via the Control Plane MVP (Story 2.5.1)
4. The SuperAdmin can manage users within the default tenant (Story 2.5.3)

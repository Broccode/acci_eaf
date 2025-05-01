# Story 4: Control Plane API

## Status: Todo

## Description

As a System Administrator, I need a Control Plane API to manage tenants in the system. The API should provide CRUD operations for tenants, implement admin authorization, and offer a bootstrapping mechanism.

## Business Value

The Control Plane API is a central component of the multi-tenant architecture of the ACCI EAF. It allows system administrators to create, configure, and manage tenants without having to access the database directly. This increases security and simplifies system administration.

## Acceptance Criteria

1. A separate NestJS application for the Control Plane API is created
2. CRUD operations for tenants are implemented:
   - Creating new tenants with basic attributes (ID, name, description, etc.)
   - Retrieving tenant information
   - Updating existing tenant data
   - Deactivating/deleting tenants
3. Admin authorization for the Control Plane is implemented
   - Only authorized system administrators can access the API
   - Access token-based authentication is implemented
4. A bootstrapping procedure according to ADR-007 is implemented
   - Initial admin user can be created
   - System configuration can be set during first execution
5. The API is documented with OpenAPI
6. Unit and E2E tests are available

## Tasks

1. Create the Control Plane API NestJS application
2. Implement the tenant module with CRUD operations
3. Create the necessary DTOs and validations
4. Implement admin authorization
5. Create the bootstrapping mechanism
6. Document the API with OpenAPI
7. Write unit and E2E tests

## Technical Notes

- The Control Plane API should be implemented as a separate NestJS application in the monorepo
- It should not apply the multi-tenancy concept as it is a system-wide tool
- The tenant entities should contain the basic attributes necessary for management
- The bootstrapping procedure should follow ADR-007
- The admin authorization should be robust and secure

## References

- [ADR-007: Control Plane Bootstrapping](docs/adr/007-control-plane-bootstrapping.md)
- [ADR-006: Row-Level Security via MikroORM Global Filters](docs/adr/006-row-level-security-mikroorm.md)

## Architecture Plan

The following architecture plan was developed by Commander Spock (Architect) to implement this story.

### Logical Components

1. **NestJS Application Structure**
   - Standalone API service in `apps/control-plane-api/`
   - Consistent separation of Domain, Application, and Infrastructure layers
   - Clear boundary from tenant-aware application code

2. **Tenant Entity Model**
   - Primary key: UUID for tenant ID
   - Core attributes: name, description, status (active/inactive), creation date
   - Extensible attributes: license information, configuration details, admin contacts

3. **Administrator Authentication**
   - JWT-based authentication with system-wide permissions
   - Guards protecting all Control Plane endpoints
   - No dependency on tenant-specific authentication system

4. **Bootstrapping Mechanism**
   - Initialization script according to ADR-007
   - Secure-by-default configuration with initial admin creation
   - Idempotent execution to avoid multiple initializations

### Implementation Phases

#### Phase 1: Basic Infrastructure

1. Create the NestJS application `control-plane-api`
2. Define Tenant entity and related repositories
3. Configure database connection (MikroORM without tenant filter)

#### Phase 2: API Implementation

1. Tenant controller with CRUD operations
2. DTO definitions and validations
3. Service layer with business logic
4. Error handling and response formatting

#### Phase 3: Security Implementation

1. Administrator authentication
2. JWT strategy for the Control Plane
3. Guards for endpoint protection
4. Access-restricting middleware

#### Phase 4: Bootstrapping

1. CLI command for initial setup
2. Admin user creation
3. Secure storage of credentials
4. Verification mechanisms for existing configuration

#### Phase 5: Documentation and Testing

1. OpenAPI documentation
2. Unit tests for services and controllers
3. E2E tests for API endpoints
4. Integration tests for database operations

### Technical Dependencies

1. **Domain Components**
   - Tenant entity in `libs/core/src/lib/domain/`
   - Admin user entity in `libs/core/src/lib/domain/`

2. **Application Components**
   - Tenant services in `apps/control-plane-api/src/app/tenants/`
   - Admin services in `apps/control-plane-api/src/app/admin/`

3. **Infrastructure Components**
   - MikroORM configuration (without tenant filter)
   - JWT Strategy in `apps/control-plane-api/src/app/auth/`
   - Guards in `apps/control-plane-api/src/app/guards/`

4. **Shared Components**
   - DTOs in `libs/shared/src/lib/interfaces/`
   - Validators in `libs/shared/src/lib/validators/`

### Critical Considerations

1. **Data Integrity**
   - Ensure data consistency for tenant CRUD operations
   - Avoid race conditions during concurrent modifications

2. **Security Boundary**
   - Strict separation between Control Plane and tenant-aware applications
   - Minimize access to sensitive tenant configurations

3. **Bootstrapping Security**
   - Protection against unauthorized reconfiguration
   - Secure storage of initial admin credentials

4. **Future Extensibility**
   - Preparation for tenant-specific configuration management
   - Interface for future admin UI integration

## Linear Issue

- **ID:** ACCI-39
- **URL:** <https://linear.app/acci/issue/ACCI-39/story-4-control-plane-api>

# ADR-004: Security, Authentication and Authorization

## Status

Accepted

## Context

The Enterprise Application Framework (EAF) needs robust security mechanisms to protect resources, especially in a multi-tenant environment. We need to enforce:

1. **Authentication**: Securely identify users accessing the system
2. **Authorization**: Control access to resources based on roles and permissions
3. **Basic Security Measures**: Protect against common web vulnerabilities
4. **Rate Limiting**: Prevent abuse and DoS attacks

## Decision

We will implement the following security measures:

### Authentication

- Use JWT (JSON Web Tokens) as the primary authentication mechanism
- Implement the local authentication strategy for username/password authentication
- Use NestJS Passport for strategy integration
- Store JWT secret in environment variables with sensible defaults for development

### Authorization

- Implement RBAC/ABAC (Role-Based/Attribute-Based Access Control) using the CASL library
- Create a dedicated `rbac` library that can be used across all applications
- Define permissions based on:
  - Action (create, read, update, delete, manage)
  - Resource (tenant, user, etc.)
  - Ownership (user can only access their own resources)
  - Tenant context (user can only access resources in their tenant)
- Use Guards and Decorators for controller-level permission enforcement

### Basic Security

- Implement Helmet middleware to set secure HTTP headers
- Configure CORS with appropriate restrictions
- Use rate limiting with ThrottlerModule to prevent abuse

### Multi-Tenancy Security

- Ensure all security mechanisms are tenant-aware
- Authorization decisions consider the tenant context
- Enforce tenant isolation in all data access

## Consequences

### Positive

- Consistent security model across the application
- Flexible permission system that can handle complex authorization scenarios
- Protection against common security vulnerabilities
- Clear separation of concerns between authentication and authorization

### Negative

- Additional complexity in the codebase
- Performance overhead of permission checks (mitigated by efficient implementation)
- Need for careful testing to ensure security boundaries are maintained

### Risks

- Misconfiguration could lead to security vulnerabilities
- Incomplete permission checks could allow unauthorized access
- JWT tokens require proper expiration and refresh mechanisms

## Implementation Details

### Authentication Flow

1. User submits credentials to login endpoint
2. Local strategy validates credentials
3. If valid, JWT token is issued
4. Client includes JWT in Authorization header for subsequent requests
5. JwtAuthGuard validates token and attaches user to request

### Authorization Flow

1. Controller methods are decorated with @CheckPermissions
2. PermissionsGuard intercepts requests
3. UserPermissions are retrieved for the authenticated user
4. CASL ability is built based on user permissions
5. Permission check is executed against the request
6. Access is granted or denied based on result

### Security Headers (Helmet)

- Content-Security-Policy
- X-XSS-Protection
- X-Content-Type-Options
- X-Frame-Options
- Other recommended security headers

### Rate Limiting

- Global rate limits per IP address
- Configurable TTL and limit values
- Exceptions for certain routes as needed

## References

- [NestJS Authentication](https://docs.nestjs.com/security/authentication)
- [CASL Documentation](https://casl.js.org/v6/en/guide/intro)
- [Helmet Security](https://helmetjs.github.io/)
- [ADR-001: RBAC Implementation](./001-rbac-implementation.md)

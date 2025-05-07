# Security in ACCI EAF

Version: 1.0
Date: 2025-05-07
Status: Published

## Introduction

Security is a paramount concern and a first-class citizen in the ACCI EAF. The framework is designed with the OWASP Top 10 in mind and aims to provide a robust foundation for building secure enterprise applications, laying the groundwork for certifications like ISO 27001 and SOC2. This document outlines the core security concepts, mechanisms, and best practices integrated into the EAF.

## Core Security Pillars

### 1. Authentication (AuthN)

Authentication is the process of verifying the identity of a user, system, or service.

- **Mechanism:** ACCI EAF utilizes a flexible authentication module built on NestJS's passport integration.
- **Supported Strategies (V1):**
  - **JWT (JSON Web Token):** Bearer token-based authentication, suitable for stateless API communication. Tokens are typically issued after a successful login and validated on subsequent requests.
  - **Local Strategy:** Username and password-based authentication, often used for initial login to obtain a JWT.
- **Extensibility:** The architecture allows for the addition of other authentication strategies (e.g., OAuth2, OIDC, LDAP) as needed.
- **User-Tenant Linkage:** Authentication is tenant-aware. User identities are typically scoped to a specific tenant.

### 2. Authorization (AuthZ)

Authorization is the process of determining whether an authenticated user, system, or service has the necessary permissions to perform a specific action or access a particular resource.

- **Mechanism (ADR-001):** ACCI EAF employs a combination of Role-Based Access Control (RBAC) and basic Attribute-Based Access Control (ABAC), primarily for ownership checks, using the `casl` library.
  - **RBAC:** Permissions are assigned to roles, and users are assigned to roles. This simplifies permission management.
  - **ABAC (Ownership):** Allows for fine-grained control, such as permitting a user to edit only the resources they own (e.g., via an `ownerUserId` field on an entity).
- **`casl` Library:** A powerful and flexible JavaScript/TypeScript library for managing permissions. It allows defining abilities (permissions) on subjects (entities or concepts) for specific actions (e.g., `create`, `read`, `update`, `delete`, `manage`).
- **Data Model:** Includes MikroORM entities for Users, Roles, and Permissions, which are tenant-aware where appropriate.
- **Enforcement:**
  - **NestJS Guards:** `CaslGuard` (custom or from a library) is used as a declarative way to protect NestJS controllers and resolvers. Example: `@UseGuards(JwtAuthGuard, CaslGuard('update', 'Article'))`.
  - **Programmatic Checks:** Services can also perform imperative permission checks using `casl` abilities for more complex scenarios.
- **Tenant Admin APIs:** The framework provides a basis for tenant administrators to manage roles and permissions within their own tenant scope (FR-AUTHZ).

### 3. Secure Coding Practices & Input Validation

- **DTO Validation:** All incoming data via API requests (DTOs - Data Transfer Objects) is rigorously validated using class-validator and class-transformer, integrated with NestJS pipes. This helps prevent injection attacks and ensures data integrity.
- **Parameterized Queries:** MikroORM, by its nature, uses parameterized queries, which is a primary defense against SQL injection attacks.
- **Output Encoding:** Care is taken to ensure that data sent to clients is appropriately encoded, especially if it might be rendered in HTML contexts (though EAF is primarily backend).

### 4. Common Web Vulnerability Protection

- **Security Headers (`helmet`):** The `helmet` middleware is integrated to set various HTTP security headers (e.g., `X-Content-Type-Options`, `Strict-Transport-Security`, `X-Frame-Options`, `X-XSS-Protection`, `Content-Security-Policy` (basic setup)). These headers instruct browsers to enable protective mechanisms, mitigating risks like XSS and clickjacking.
- **Rate Limiting (`@nestjs/throttler`):** The `@nestjs/throttler` module is used to limit the number of requests an IP address can make to API endpoints within a certain time window. This provides basic protection against brute-force attacks and simple Denial-of-Service (DoS) attacks.
- **CSRF Protection:** While less critical for stateless APIs primarily consumed by non-browser clients, if session-based authentication were to be used or forms served directly, CSRF protection (e.g., `csurf`) would be considered.

### 5. Multi-Tenancy Security (ADR-006)

Data isolation between tenants is a critical security requirement, achieved via Row-Level Security (RLS) as detailed in the Multi-Tenancy concept document and ADR-006.

### 6. License Validation Security (ADR-003)

The license validation mechanism itself is designed to be robust against simple bypass and tampering, as outlined in ADR-003.

### 7. Secrets Management

- Configuration, including sensitive data like database credentials and API keys, MUST be managed via environment variables or a dedicated secrets management service (like HashiCorp Vault or AWS Secrets Manager in production). Secrets should NOT be hardcoded into the application or committed to version control.
- The `.env` file is used for local development and should be included in `.gitignore`.

## Observability for Security

- **Structured Logging:** Capturing relevant security events (e.g., failed login attempts, authorization failures, critical errors) with context (like `tenant_id`, `correlationId`, source IP) is crucial for security monitoring and incident response.
- **Audit Trail (Future V2+):** While a dedicated, immutable audit trail service is out of scope for V1, the Event Sourcing pattern provides a good foundation, as all state changes are recorded as events. This can be leveraged for future audit capabilities.

## Testing for Security

- **Unit and Integration Tests:** Include test cases for authentication, authorization logic (e.g., guard behavior, permission checks), and input validation.
- **E2E Tests:** Cover security-related user flows.
- **Security Reviews & Penetration Testing:** Regular security code reviews and periodic penetration testing (especially before major releases or for sensitive applications) are recommended to proactively identify and mitigate vulnerabilities.

## Key Security-Related ADRs

- **ADR-001: RBAC Library Selection (`casl`)**
- **ADR-003: License Validation**
- **ADR-006: RLS Enforcement Strategy**

## Conclusion

ACCI EAF integrates security at multiple levels, from architectural design to specific library choices and recommended practices. By providing a strong security baseline, the framework helps development teams build more resilient and trustworthy enterprise applications. Continuous attention to security throughout the development lifecycle remains essential.

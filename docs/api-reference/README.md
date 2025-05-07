# ACCI EAF API Reference

Version: 1.0
Date: 2025-05-07
Status: Placeholder

## Introduction

This section provides the API reference documentation for applications built with the ACCI Enterprise Application Framework (EAF), such as the `Control-Plane API` and any `Sample App` or custom applications.

The API documentation details available endpoints, request/response schemas, authentication methods, and status codes.

## Generating API Documentation

ACCI EAF applications typically leverage the `@nestjs/swagger` (or `@nestjs/ OpenAPI`) module to automatically generate an OpenAPI (formerly Swagger) specification from the existing controllers, DTOs, and entity definitions decorated with appropriate decorators (`@ApiProperty()`, `@ApiOperation()`, etc.).

**Typical Generation Process:**

1. **Decorate Code:** Ensure your DTOs, entities, and controller methods are decorated with `@nestjs/swagger` decorators to provide metadata for the OpenAPI specification.
2. **Setup Swagger Module:** In your `main.ts` (or a dedicated Swagger module), configure the `SwaggerModule` to create and serve the OpenAPI specification and the Swagger UI.

    ```typescript
    // Example in main.ts
    async function bootstrap() {
      const app = await NestFactory.create(AppModule);

      // ... other app configurations (pipes, middleware, etc.)

      const config = new DocumentBuilder()
        .setTitle('ACCI EAF Control Plane API')
        .setDescription('API for managing tenants and system settings')
        .setVersion('1.0')
        .addBearerAuth() // If using JWT Bearer tokens
        .build();
      const document = SwaggerModule.createDocument(app, config);
      SwaggerModule.setup('api-docs', app, document); // Serves UI at /api-docs

      await app.listen(3000);
    }
    bootstrap();
    ```

3. **Accessing the Documentation:** Once the application is running, the interactive Swagger UI is typically available at `http://localhost:3000/api-docs` (or the path configured in `SwaggerModule.setup`). The raw OpenAPI JSON specification is often available at `http://localhost:3000/api-docs-json`.

## Key APIs

While the detailed, auto-generated documentation should be consulted, here's a high-level overview of key API areas:

### 1. Control Plane API (`apps/control-plane-api`)

- **Authentication (`/auth`):**
  - `POST /auth/login`: Authenticate and receive JWT.
- **Tenant Management (`/tenants`):**
  - `POST /tenants`: Create a new tenant.
  - `GET /tenants`: List all tenants.
  - `GET /tenants/{id}`: Retrieve a specific tenant.
  - `PUT /tenants/{id}`: Update a tenant.
  - `DELETE /tenants/{id}`: Delete a tenant.
- **Health Checks (`/health`):**
  - `GET /health/live`: Liveness probe.
  - `GET /health/ready`: Readiness probe.
- **(Future) RBAC Management for Control Plane Admins.**

### 2. Sample Application API (`apps/sample-app`)

- Illustrative endpoints demonstrating EAF features within a tenant context.
- Endpoints will be tenant-aware and protected by AuthN/AuthZ as configured.

## Authentication & Authorization

- APIs are typically protected using JWT Bearer token authentication (see `/auth/login`).
- Authorization is enforced using RBAC/ABAC (`casl`) as described in `docs/concept/security.md`.
- Specific permissions required for each endpoint should be detailed in the OpenAPI specification (e.g., via custom decorators or descriptions).

## Rate Limiting & Security Headers

- API endpoints are subject to rate limiting (`@nestjs/throttler`).
- Standard security headers are applied via `helmet`.

---

*This document serves as a placeholder. For the most accurate and detailed API reference, please refer to the live, auto-generated OpenAPI/Swagger documentation served by each application instance.*

# Epic-1 - Story-7

License Validation

**As a** framework developer
**I want** a hybrid license validation mechanism supporting both online and offline scenarios
**so that** the system can enforce licensing requirements even when offline, ensuring security and compliance.

## Status

Completed

## Context

This story is part of Epic-3 (Security & Compliance) and focuses on implementing a hybrid license validation mechanism for the ACCI EAF framework. Online validation will call an external license server API, while offline validation will verify license files or tokens using cryptographic signatures. This ensures that clients can securely validate licenses regardless of network availability.

## Estimation

Story Points: 5

## Tasks

1. - [ ] Define license data models and DTOs
    1. - [ ] Create `LicenseDto` and `LicenseFileDto`
    2. - [ ] Document validation result DTO
2. - [ ] Implement `LicenseValidationService`
    1. - [ ] `validateOnline(licenseKey: string): Promise<boolean>`
    2. - [ ] `validateOffline(licenseFile: Buffer | string): Promise<boolean>`
    3. - [ ] Throw `LicenseValidationError` on invalid license
3. - [ ] Create REST endpoint in `apps/control-plane-api`
    1. - [ ] POST `/licenses/validate`
    2. - [ ] Accept request body `{ licenseKey?: string; licenseFile?: string; }`
    3. - [ ] Return `{ valid: boolean; message?: string }`
4. - [ ] Write unit tests for service methods
    1. - [ ] Mock HTTP client for online validation
    2. - [ ] Use sample license files for offline validation
5. - [ ] Write integration tests
    1. - [ ] Use a mock server or Testcontainers to simulate the external license server
    2. - [ ] Validate both online and offline paths
6. - [ ] Update documentation
    1. - [ ] Add usage examples in README or API docs
    2. - [ ] Document error types and messages

## Constraints

- Use NestJS for controllers and services
- Use the built-in `HttpService` or Axios for online requests
- Use Node.js `crypto` module for offline signature verification
- Ensure multi-tenancy context (`tenant_id`) is applied to license validation

## Data Models / Schema

- License payload JSON:
  - `key`: string
  - `tenantId`: string
  - `expirationDate`: string (ISO 8601)
  - `signature`: string
- Signature algorithm: RSA-SHA256
- License server endpoint: `/api/licenses/validate`

## Dev Notes

- Cache successful online validations with TTL to reduce network calls
- Load public key from secure environment variable for offline verification
- Handle clock skew when checking expiration dates

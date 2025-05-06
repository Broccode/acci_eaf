# Epic-1 - Task-1

Improve Adoption of Testing Framework

**As a** framework maintainer
**I want** to extend the usage of our new testing framework helpers across the codebase
**so that** we increase test coverage and ensure all critical modules benefit from fast, reliable tests.

## Status

Completed

## Context

Story-8 delivered a comprehensive testing framework (unit, integration, E2E helpers). While some modules already leverage these helpers, several important areas still rely on ad-hoc tests or lack proper coverage. This task targets the migration/creation of tests for those modules, using the standardized helpers to ensure consistency and maintainability.

## Estimation

Story Points: 2

## Tasks

1. E2E Tests (NestE2ETestHelper)
   1. - [x] Control-Plane-API – TenantsController basic CRUD flow (`/tenants`)
   2. - [x] Sample-App – Health endpoint (`/health`)

2. Redis Integration Tests (testRedisManager)
   1. - [x] Infrastructure ‑ RedisCacheService store & retrieve
   2. - [x] Verify TTL handling

3. CQRS / Idempotency Integration Tests (MikroOrmTestHelper + testDbManager)
   1. - [x] IdempotencyHandler prevents duplicate processing

4. License Validation Integration Tests
   1. - [x] Validate licence online mode against mock server
   2. - [x] Validate offline mode signature + expiration

5. Authorization Guard Enhancements (NestUnitTestHelper)
   1. - [x] Complex permission scenarios for PermissionsGuard
   2. - [x] Negative cases (unauthenticated / insufficient perms)

## Constraints

- All new tests MUST use the helpers from `@acci/testing`.
- Keep containers isolated per test suite to avoid side effects.
- Aim for >90 % coverage on newly added tests.

## Data Models / Schema

No new schemas. Tests reuse existing entities and DTOs.

## Technical Details

Refer to examples from Story-8 for helper usage. Ensure Testcontainers resources are properly cleaned up in `afterAll` hooks.

## Examples

N/A

## References

- Story-8 Testing Framework implementation
- `@acci/testing` README 📚

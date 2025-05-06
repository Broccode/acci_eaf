<!-- Linear Issue: https://linear.app/acci/issue/ACCI-41/epic-1-story-8-testing-framework -->
# Epic-1 - Story-8

Testing Framework

**As a** framework developer
**I want** a robust testing infrastructure for unit, integration, and E2E testing
**so that** I can ensure high quality, reliability, and maintainability of the framework through comprehensive test coverage.

## Status

Completed

## Context

This story is part of Epic-4 (Observability & Testing) and focuses on establishing a comprehensive testing framework for the ACCI EAF. A well-defined testing strategy is essential for ensuring the quality and reliability of the framework components. This includes setting up infrastructure for unit tests (`suites`), integration tests (Testcontainers), and E2E tests (`supertest`), along with defining testing standards and patterns that will be used across the project.

## Estimation

Story Points: 3

## Tasks

1. - [x] Set up unit testing infrastructure (`suites`)
   1. - [x] Create base test utilities for testing DI components
   2. - [x] Implement test factories for common framework objects
   3. - [x] Define mocking standards and helpers

2. - [x] Set up integration testing infrastructure (Testcontainers)
   1. - [x] Create base Docker container configurations for PostgreSQL and Redis
   2. - [x] Implement MikroORM test helpers for database integration tests
   3. - [x] Create utility for spinning up isolated test environments

3. - [x] Set up E2E testing infrastructure (`supertest`, `@nestjs/testing`)
   1. - [x] Define patterns for creating test applications
   2. - [x] Implement request helpers for API testing
   3. - [x] Create utilities for authentication in tests

4. - [x] Define testing standards and patterns
   1. - [x] Document test organization (unit/integration/E2E directories)
   2. - [x] Define naming conventions for test files and test cases
   3. - [x] Create examples of testing different component types (services, controllers, etc.)

5. - [x] Implement test coverage reporting
   1. - [x] Configure Jest for coverage reporting
   2. - [x] Set up coverage thresholds for core libraries
   3. - [x] Integrate coverage reports with CI pipeline

## Constraints

- Unit tests must be fast and not require external dependencies
- Integration tests must be isolated and reproducible
- E2E tests should cover critical API paths
- Test coverage for core libraries should exceed 85%
- Test utilities must be well-documented and easy to use

## Data Models / Schema

N/A for this story.

## Technical Details

### Unit Testing with `suites`

The `suites` package will be used to simplify testing of NestJS components with dependency injection:

```typescript
import { suite } from '@nestjs/testing/suite';

const { createTestingModule } = suite();

describe('SomeService', () => {
  it('should perform some action', async () => {
    const module = await createTestingModule({
      providers: [SomeService, MockDependency]
    }).compile();
    
    const service = module.get(SomeService);
    expect(service.someMethod()).toBe(expectedResult);
  });
});
```

### Integration Testing with Testcontainers

Example of setting up a PostgreSQL container for integration tests:

```typescript
import { PostgreSqlContainer } from '@testcontainers/postgresql';

describe('Database Integration', () => {
  let container;
  let mikroOrmConfig;

  beforeAll(async () => {
    container = await new PostgreSqlContainer()
      .withDatabase('test-db')
      .withUsername('test-user')
      .withPassword('test-password')
      .start();
    
    mikroOrmConfig = {
      // Configure MikroORM to connect to the test container
      clientUrl: container.getConnectionUri(),
      // other configuration...
    };
  });

  afterAll(async () => {
    await container.stop();
  });

  it('should perform database operations', async () => {
    // Test with real database in the container
  });
});
```

### E2E Testing with `supertest`

Example of E2E test for an API endpoint:

```typescript
import * as request from 'supertest';
import { Test } from '@nestjs/testing';
import { AppModule } from '../src/app.module';
import { INestApplication } from '@nestjs/common';

describe('AppController (e2e)', () => {
  let app: INestApplication;

  beforeAll(async () => {
    const moduleFixture = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    await app.init();
  });

  afterAll(async () => {
    await app.close();
  });

  it('/health (GET)', () => {
    return request(app.getHttpServer())
      .get('/health')
      .expect(200)
      .expect({ status: 'ok' });
  });
});
```

## Examples

N/A for this story.

## References

- [Jest Documentation](https://jestjs.io/docs/getting-started)
- [NestJS Testing](https://docs.nestjs.com/fundamentals/testing)
- [Testcontainers for Node.js](https://node.testcontainers.org/)
- [Supertest](https://github.com/ladjs/supertest)

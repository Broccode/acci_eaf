# Testing Infrastructure

This library provides testing utilities for the ACCI EAF framework. It includes helpers for unit, integration, and E2E testing.

## Installation

This module is included in the ACCI EAF framework by default. No additional installation is required.

## Features

### Unit Testing

- `NestUnitTestHelper`: Simplifies testing NestJS components with dependency injection.
- Provides utilities for creating mock services.

### Integration Testing

- `TestDbManager`: Manages PostgreSQL containers for integration tests.
- `TestRedisManager`: Manages Redis containers for integration tests using @testcontainers/redis.
- `MikroOrmTestHelper`: Configures MikroORM for tests with Testcontainers.

### E2E Testing

- `NestE2ETestHelper`: Simplifies setting up NestJS applications for E2E testing with supertest.
- Provides utilities for authenticated requests.

## Usage

### Unit Testing

```typescript
import { NestUnitTestHelper } from '@acci/testing';

describe('MyService', () => {
  it('should perform some action', async () => {
    // Create a mock dependency
    const mockDependency = {
      someMethod: jest.fn().mockReturnValue('mock result')
    };

    // Set up the testing module
    const { service } = await NestUnitTestHelper.createTestingModule({
      providers: [
        MyService,
        {
          provide: MyDependency,
          useValue: mockDependency
        }
      ]
    }).compile(MyService);

    // Test the service
    expect(service.someMethod()).toBe('expected result');
  });
});
```

### Integration Testing with Database

```typescript
import { MikroOrmTestHelper } from '@acci/testing';

describe('Database Integration', () => {
  let helper: MikroOrmTestHelper;
  let em;

  beforeAll(async () => {
    helper = new MikroOrmTestHelper();
    await helper.setup();
    em = helper.getEntityManager();
  });

  afterAll(async () => {
    await helper.teardown();
  });

  it('should perform database operations', async () => {
    const entity = em.create(SomeEntity, { name: 'Test' });
    await em.persistAndFlush(entity);
    
    const foundEntity = await em.findOne(SomeEntity, { name: 'Test' });
    expect(foundEntity).toBeDefined();
  });
});
```

### Integration Testing with Redis

```typescript
import { testRedisManager } from '@acci/testing';
import { Redis } from 'ioredis';

describe('Redis Integration', () => {
  let redisClient: Redis;

  beforeAll(async () => {
    // Start Redis container
    await testRedisManager.startRedis('redis:7.2');
    
    // Create Redis client
    redisClient = new Redis({
      host: testRedisManager.getHost(),
      port: testRedisManager.getPort()
    });
  });

  afterAll(async () => {
    // Close Redis client
    await redisClient.quit();
    
    // Stop Redis container
    await testRedisManager.stopRedis();
  });

  it('should store and retrieve data from Redis', async () => {
    await redisClient.set('test-key', 'test-value');
    const value = await redisClient.get('test-key');
    expect(value).toBe('test-value');
  });
});
```

### E2E Testing

```typescript
import { NestE2ETestHelper } from '@acci/testing';

describe('AppController (e2e)', () => {
  let testHelper: NestE2ETestHelper;

  beforeAll(async () => {
    testHelper = await NestE2ETestHelper.createTestingApp({
      imports: [AppModule]
    });
  });

  afterAll(async () => {
    await testHelper.close();
  });

  it('/health (GET)', async () => {
    const response = await testHelper.getRequest('/health')
      .expect(200)
      .expect('Content-Type', /json/);
    
    expect(response.body).toEqual({ status: 'ok' });
  });
});
```

## Examples

See the `examples` directory for complete examples of different testing patterns, including:

- Unit testing with mock services
- E2E testing with authentication
- Integration testing with PostgreSQL and Redis containers

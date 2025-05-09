# ACCI EAF How-To Guides

Version: 1.0
Date: 2025-05-07
Status: Draft

Welcome to the How-To Guides for the ACCI Enterprise Application Framework (EAF). This section provides practical, step-by-step instructions for common development tasks and utilizing various framework features.

## Table of Contents

1. [Using the Testing Framework (`@acci/testing`)](#1-using-the-testing-framework-accitesting)
    * [Writing Unit Tests for Services/Handlers](#writing-unit-tests-for-serviceshandlers)
    * [Writing Integration Tests with `MikroOrmTestHelper` and `testDbManager`](#writing-integration-tests-with-mikroormtesthelper-and-testdbmanager)
    * [Writing E2E Tests with `NestE2ETestHelper`](#writing-e2e-tests-with-neste2etesthelper)
    * [Testing Redis Integration with `testRedisManager`](#testing-redis-integration-with-testredismanager)
2. [Implementing a New CQRS Command/Query Handler](#2-implementing-a-new-cqrs-commandquery-handler)
3. [Adding a New Plugin](#3-adding-a-new-plugin)
4. [Configuring and Using Health Check Endpoints](#4-configuring-and-using-health-check-endpoints)
5. [Implementing Custom Authorization with `CaslGuard`](#5-implementing-custom-authorization-with-caslguard)
6. [Working with Multi-Tenancy and RLS](#6-working-with-multi-tenancy-and-rls)
7. [Managing Database Migrations (Core & Plugins)](#7-managing-database-migrations-core--plugins)

---

## 1. Using the Testing Framework (`@acci/testing`)

The `@acci/testing` library provides helpers to streamline writing unit, integration, and E2E tests for applications built with ACCI EAF.

### Writing Unit Tests for Services/Handlers

Unit tests focus on isolated components like services or CQRS handlers. Dependencies are typically mocked.

**Scenario:** Testing a `CreateTenantCommandHandler`.

```typescript
// libs/core/src/lib/application/commands/tenant/create-tenant.handler.spec.ts
import { Test, TestingModule } from '@nestjs/testing';
import { CreateTenantCommandHandler } from './create-tenant.handler';
import { TenantRepository } from '../../../ports/tenant.repository'; // Interface
import { EventBus } from '@nestjs/cqrs';
import { CreateTenantCommand } from './create-tenant.command';
import { Tenant } from '../../../domain/tenant/tenant.aggregate';

// Mock implementations
const mockTenantRepository = {
  save: jest.fn(),
  findById: jest.fn(),
  findByName: jest.fn(),
};

const mockEventBus = {
  publish: jest.fn(),
};

describe('CreateTenantCommandHandler', () => {
  let handler: CreateTenantCommandHandler;
  let repository: TenantRepository;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        CreateTenantCommandHandler,
        { provide: TenantRepository, useValue: mockTenantRepository },
        { provide: EventBus, useValue: mockEventBus },
      ],
    }).compile();

    handler = module.get<CreateTenantCommandHandler>(CreateTenantCommandHandler);
    repository = module.get<TenantRepository>(TenantRepository);
    jest.clearAllMocks();
  });

it('should create a new tenant and publish an event', async () => {
    const command = new CreateTenantCommand({ name: 'New Tenant', ownerId: 'user-123' });
    mockTenantRepository.findByName.mockResolvedValue(null);
    mockTenantRepository.save.mockImplementation(tenant => Promise.resolve(tenant));

    const result = await handler.execute(command);

    expect(result).toBeInstanceOf(Tenant);
    expect(result.name).toBe('New Tenant');
    expect(repository.save).toHaveBeenCalledWith(expect.any(Tenant));
    expect(mockEventBus.publish).toHaveBeenCalledWith(expect.any(Object)); // TenantCreatedEvent
  });

  it('should throw an error if tenant name already exists', async () => {
    const command = new CreateTenantCommand({ name: 'Existing Tenant', ownerId: 'user-123' });
    mockTenantRepository.findByName.mockResolvedValue(new Tenant({ id: 'tenant-456', name: 'Existing Tenant', ownerId: 'user-other' }));

    await expect(handler.execute(command)).rejects.toThrow('Tenant with name Existing Tenant already exists');
  });
});
```

### Writing Integration Tests with `MikroOrmTestHelper` and `testDbManager`

Integration tests verify interactions between components, especially with the database. `@acci/testing` provides `MikroOrmTestHelper` (from `@mikro-orm/nestjs/testing` or a custom wrapper) and `testDbManager` (custom helper for Testcontainers).

**Scenario:** Testing `TenantRepository` (MikroORM implementation).

```typescript
// libs/infrastructure/src/lib/persistence/tenant/tenant.repository.integration-spec.ts
import { Test } from '@nestjs/testing';
import { MikroOrmModule } from '@mikro-orm/nestjs';
import { TsMorphMetadataProvider } from '@mikro-orm/reflection';
import { SqliteDriver } from '@mikro-orm/sqlite'; // Or your actual driver
import { TenantRepositoryImpl } from './tenant.repository';
import { TenantEntity } from '../entities/tenant.entity';
import { Tenant } from '@acci/core'; // Domain aggregate
import { getRepositoryToken } from '@mikro-orm/nestjs';
import { EntityRepository } from '@mikro-orm/core';

// Example using a simplified in-memory SQLite for speed, or use Testcontainers for real DB

describe('TenantRepositoryImpl - Integration', () => {
  let repository: TenantRepositoryImpl;
  let orm: any; // MikroORM instance

  beforeAll(async () => {
    const moduleRef = await Test.createTestingModule({
      imports: [
        MikroOrmModule.forRoot({
          dbName: ':memory:', // Or configure for Testcontainers
          driver: SqliteDriver,
          entities: [TenantEntity],
          metadataProvider: TsMorphMetadataProvider,
          debug: false,
        }),
        MikroOrmModule.forFeature({ entities: [TenantEntity] }),
      ],
      providers: [TenantRepositoryImpl],
    }).compile();

    repository = moduleRef.get(TenantRepositoryImpl);
    orm = moduleRef.get('MikroORM');
    await orm.getSchemaGenerator().updateSchema(); // Create schema
  });

  afterEach(async () => {
    await orm.getSchemaGenerator().refreshDatabase(); // Clean database
  });

  afterAll(async () => {
    await orm.close(true);
  });

  it('should save and find a tenant', async () => {
    const tenant = Tenant.create({ name: 'Test Tenant', ownerId: 'owner-1' });
    // Convert domain to entity if necessary, or use entity directly for repo test
    const tenantEntity = new TenantEntity();
    Object.assign(tenantEntity, { id: tenant.id, name: tenant.name, ownerId: tenant.ownerId, createdAt: new Date(), updatedAt: new Date() });

    await repository.save(tenantEntity as any); // Adapt based on your save method signature

    const found = await repository.findById(tenant.id);
    expect(found).toBeDefined();
    expect(found!.name).toBe('Test Tenant');
  });
});
```

*Note: For real database integration, `testDbManager` would handle starting/stopping Docker containers (e.g., PostgreSQL via Testcontainers) and providing connection details to MikroORM.* Refer to `@acci/testing` examples for `testDbManager` usage.

### Writing E2E Tests with `NestE2ETestHelper`

End-to-end tests validate entire request flows through the API.
`NestE2ETestHelper` (a custom helper or directly using `@nestjs/testing`) sets up the full NestJS application.

**Scenario:** Testing the `/tenants` CRUD endpoints of `ControlPlaneApi`.

```typescript
// apps/control-plane-api-e2e/src/control-plane-api/tenants.e2e-spec.ts
import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication, ValidationPipe } from '@nestjs/common';
import * as request from 'supertest';
import { AppModule } from '@acci/control-plane-api'; // Main app module
// Potentially use MikroOrmTestHelper or testDbManager to set up a test DB

describe('TenantsController (e2e)', () => {
  let app: INestApplication;
  let authToken: string; // For authenticated endpoints

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule], // Import your main application module
    })
    // .overrideProvider(...) for mocks if needed
    .compile();

    app = moduleFixture.createNestApplication();
    app.useGlobalPipes(new ValidationPipe());
    await app.init();

    // TODO: Implement authentication to get authToken if endpoints are protected
    // e.g., const loginResponse = await request(app.getHttpServer()).post('/auth/login').send(...);
    // authToken = loginResponse.body.access_token;
  });

  afterAll(async () => {
    await app.close();
  });

it('/tenants (POST) - should create a new tenant', async () => {
    const createTenantDto = { name: 'E2E Tenant', description: 'A tenant created via E2E test' };
    return request(app.getHttpServer())
      .post('/tenants')
      // .set('Authorization', `Bearer ${authToken}`)
      .send(createTenantDto)
      .expect(201)
      .then(response => {
        expect(response.body.name).toEqual(createTenantDto.name);
        expect(response.body.id).toBeDefined();
      });
  });

  it('/tenants (GET) - should return a list of tenants', async () => {
    // First, create a tenant to ensure the list is not empty
    await request(app.getHttpServer()).post('/tenants').send({ name: 'Tenant For GET Test' });

    return request(app.getHttpServer())
      .get('/tenants')
      // .set('Authorization', `Bearer ${authToken}`)
      .expect(200)
      .then(response => {
        expect(Array.isArray(response.body)).toBe(true);
        expect(response.body.length).toBeGreaterThan(0);
      });
  });

  // Add more tests for GET by ID, PUT, DELETE...
});
```

### Testing Redis Integration with `testRedisManager`

`testRedisManager` (custom helper from `@acci/testing`) would use Testcontainers to spin up a Redis instance for testing services that use Redis (e.g., for caching).

**Scenario:** Testing a `RedisCacheService`.

```typescript
// libs/infrastructure/src/lib/cache/redis-cache.service.integration-spec.ts
// import { testRedisManager } from '@acci/testing'; // Assuming this helper
import { RedisCacheService } from './redis-cache.service';
import { createClient } from 'redis'; // Or ioredis

describe('RedisCacheService - Integration', () => {
  // let redisManager: TestRedisManager; // From @acci/testing
  let cacheService: RedisCacheService;
  let redisClient: any;

  beforeAll(async () => {
    // redisManager = await testRedisManager.start();
    // const redisUrl = await redisManager.getConnectionUrl();
    // For simplicity, connecting to a local/fixed Redis for this example snippet:
    const redisUrl = 'redis://localhost:6379';
    redisClient = createClient({ url: redisUrl });
    await redisClient.connect();

    // Pass the client or connection options to your service
    cacheService = new RedisCacheService(redisClient);
  });

  afterEach(async () => {
    await redisClient.flushDb(); // Clean DB after each test
  });

  afterAll(async () => {
    await redisClient.quit();
    // await redisManager.stop();
  });

it('should store and retrieve a string value', async () => {
    const key = 'test:mykey';
    const value = 'hello world';
    await cacheService.set(key, value, 60); // Set with 60s TTL
    const retrieved = await cacheService.get(key);
    expect(retrieved).toBe(value);
  });

  it('should return null for a non-existent key', async () => {
    const retrieved = await cacheService.get('test:nonexistent');
    expect(retrieved).toBeNull();
  });

  it('should respect TTL', async () => {
    const key = 'test:ttlkey';
    const value = 'expiring value';
    await cacheService.set(key, value, 1); // 1s TTL
    const retrievedBeforeExpiry = await cacheService.get(key);
    expect(retrievedBeforeExpiry).toBe(value);
    await new Promise(resolve => setTimeout(resolve, 1100)); // Wait for TTL to expire
    const retrievedAfterExpiry = await cacheService.get(key);
    expect(retrievedAfterExpiry).toBeNull();
  });
});
```

## 2. Implementing a New CQRS Command/Query Handler

CQRS is central to ACCI EAF. Here's how to add a new command and its handler.

**A. Define the Command (in `libs/core/src/lib/application/commands/...`):**

```typescript
// libs/core/src/lib/application/commands/some-feature/do-something.command.ts
export class DoSomethingCommand {
  constructor(public readonly entityId: string, public readonly someData: string) {}
}
```

**B. Define the Command Handler (in `libs/core/src/lib/application/commands/...`):**

```typescript
// libs/core/src/lib/application/commands/some-feature/do-something.handler.ts
import { CommandHandler, ICommandHandler, EventBus } from '@nestjs/cqrs';
import { DoSomethingCommand } from './do-something.command';
import { SomeAggregateRepository } from '../../../ports/some-aggregate.repository';
// import { SomethingDoneEvent } from '../../../domain/some-feature/events/something-done.event';

@CommandHandler(DoSomethingCommand)
export class DoSomethingCommandHandler implements ICommandHandler<DoSomethingCommand> {
  constructor(
    private readonly repository: SomeAggregateRepository,
    private readonly eventBus: EventBus,
  ) {}

  async execute(command: DoSomethingCommand): Promise<void> {
    const { entityId, someData } = command;
    // const aggregate = await this.repository.findById(entityId);
    // if (!aggregate) { throw new Error('Aggregate not found'); }

    // aggregate.performAction(someData);
    // await this.repository.save(aggregate);

    // this.eventBus.publish(new SomethingDoneEvent(entityId, someData));
    console.log(`Handling DoSomethingCommand for entity ${entityId} with data: ${someData}`);
    // Actual implementation would involve domain logic, repository interaction, and event publishing.
  }
}
```

**C. Register the Handler in a NestJS Module (e.g., in `libs/core` or a feature module):**

```typescript
// libs/core/src/lib/core.module.ts or a feature-specific module
import { Module } from '@nestjs/common';
import { CqrsModule } from '@nestjs/cqrs';
import { DoSomethingCommandHandler } from './application/commands/some-feature/do-something.handler';
// ... other imports and providers

export const CommandHandlers = [DoSomethingCommandHandler /*, other handlers */];

@Module({
  imports: [CqrsModule],
  providers: [
    ...CommandHandlers,
    // ... Repositories, other services
  ],
  exports: [CqrsModule, ...CommandHandlers],
})
export class CoreModule {}
```

**D. Dispatch the Command from a Service or Controller (e.g., in `apps/control-plane-api`):**

```typescript
// apps/control-plane-api/src/app/some-feature/some-feature.service.ts
import { Injectable } from '@nestjs/common';
import { CommandBus } from '@nestjs/cqrs';
import { DoSomethingCommand } from '@acci/core'; // Assuming command is exported from core

@Injectable()
export class SomeFeatureService {
  constructor(private readonly commandBus: CommandBus) {}

  async doSomething(entityId: string, data: string): Promise<void> {
    return this.commandBus.execute(new DoSomethingCommand(entityId, data));
  }
}
```

*Follow a similar pattern for Queries and Query Handlers (using `@QueryHandler` and `IQueryHandler`).*

## 3. Adding a New Plugin

Plugins extend EAF functionality. See `docs/concept/plugin-system.md` for core concepts.

1. **Create Plugin Directory:** Create a new library for your plugin, e.g., `libs/plugins/my-custom-plugin`.

    ```bash
    npx nx generate @nx/nest:library my-custom-plugin --directory=libs/plugins --publishable --importPath=@my-scope/my-custom-plugin
    ```

2. **Develop Plugin Module:** Implement your NestJS module(s), services, controllers, entities, etc., within this library.

    ```typescript
    // libs/plugins/my-custom-plugin/src/lib/my-custom-plugin.module.ts
    import { Module } from '@nestjs/common';
    import { MyPluginService } from './my-plugin.service';

    @Module({
      providers: [MyPluginService],
      exports: [MyPluginService],
    })
    export class MyCustomPluginModule {}
    ```

3. **Define Entities (if any):** Place MikroORM entities in an `entities` subfolder.
4. **Create Migrations (if any):** Initialize migrations for your plugin if it has entities (ADR-008).
    * Update `mikro-orm.config.ts` in your plugin to point to its own migration path.
    * Generate initial migration: `npx mikro-orm migration:create --initial -c ./libs/plugins/my-custom-plugin/src/mikro-orm.config.ts`
5. **Update Main App's MikroORM Config:**
    * Ensure the main application's `mikro-orm.config.ts` can discover entities from your plugin (e.g., add `'libs/plugins/my-custom-plugin/src/lib/entities/**/*.entity.js'` to `entities` glob patterns, adjust for your build output).
    * Ensure the main migration config can discover plugin migrations (e.g., add path to `migrations.path` array or adjust `migrations.glob`).
6. **Import Plugin Module:** Import `MyCustomPluginModule` into your main application module (e.g., `apps/sample-app/src/app.module.ts`).

## 4. Configuring and Using Health Check Endpoints

Health checks are provided by `@nestjs/terminus`. (See `docs/concept/observability.md`).

**A. Install `@nestjs/terminus` (if not already present):**
   `npm install @nestjs/terminus`

**B. Add Health Module to your Application (e.g., `apps/sample-app/src/app/health/health.module.ts`):**

```typescript
// apps/sample-app/src/app/health/health.module.ts
import { Module } from '@nestjs/common';
import { TerminusModule } from '@nestjs/terminus';
import { HttpModule } from '@nestjs/axios'; // For HttpHealthIndicator
import { HealthController } from './health.controller';
import { MikroOrmHealthIndicator } from './mikro-orm.health-indicator'; // Custom for MikroORM

@Module({
  imports: [
    TerminusModule,
    HttpModule, // If you need to check external HTTP services
    // MikroOrmModule, // Ensure MikroORM is available if checking DB
  ],
  controllers: [HealthController],
  providers: [MikroOrmHealthIndicator], // Add your custom indicators
})
export class HealthModule {}
```

**C. Create a Custom Health Indicator (e.g., for MikroORM):**

```typescript
// apps/sample-app/src/app/health/mikro-orm.health-indicator.ts
import { Injectable } from '@nestjs/common';
import { HealthIndicator, HealthIndicatorResult, HealthCheckError } from '@nestjs/terminus';
import { EntityManager } from '@mikro-orm/core'; // Or MikroORM instance

@Injectable()
export class MikroOrmHealthIndicator extends HealthIndicator {
  constructor(private readonly em: EntityManager) { // Inject EntityManager
    super();
  }

  async isHealthy(key: string): Promise<HealthIndicatorResult> {
    try {
      // A simple query to check DB connectivity
      await this.em.getConnection().execute('SELECT 1');
      return this.getStatus(key, true);
    } catch (error) {
      throw new HealthCheckError('MikroORM health check failed', this.getStatus(key, false, { message: error.message }));
    }
  }
}
```

**D. Create Health Controller (e.g., `apps/sample-app/src/app/health/health.controller.ts`):**

```typescript
// apps/sample-app/src/app/health/health.controller.ts
import { Controller, Get } from '@nestjs/common';
import { HealthCheckService, HealthCheck, HttpHealthIndicator, TypeOrmHealthIndicator, MemoryHealthIndicator, DiskHealthIndicator } from '@nestjs/terminus';
import { MikroOrmHealthIndicator } from './mikro-orm.health-indicator';

@Controller('health')
export class HealthController {
  constructor(
    private health: HealthCheckService,
    private http: HttpHealthIndicator, // Example: check external service
    private customDb: MikroOrmHealthIndicator,
    private memory: MemoryHealthIndicator,
    private disk: DiskHealthIndicator,
  ) {}

  @Get('live')
  @HealthCheck()
  checkLiveness() {
    return this.health.check([
      // Basic liveness, no dependencies usually
    ]);
  }

  @Get('ready')
  @HealthCheck()
  checkReadiness() {
    return this.health.check([
      () => this.customDb.isHealthy('database'),
      // Example: Check if an external API is reachable
      // () => this.http.pingCheck('external_api', 'https://api.example.com/ping'),
      // Example: Check memory usage (e.g., RSS < 250MB)
      () => this.memory.checkHeap('memory_heap', 250 * 1024 * 1024),
      () => this.memory.checkRSS('memory_rss', 250 * 1024 * 1024),
      // Example: Check disk space (e.g., path '/' has at least 50% free)
      // () => this.disk.checkStorage('storage', { path: '/', thresholdPercent: 0.5 }),
    ]);
  }
}
```

**E. Import `HealthModule` into your `AppModule`**.

## 5. Implementing Custom Authorization with `CaslGuard`

(See `docs/concept/security.md` and ADR-001).
`casl` is used for RBAC/ABAC. A `CaslGuard` protects routes.

1. **Define Abilities (e.g., in a feature module or `libs/rbac`):**

    ```typescript
    // libs/rbac/src/lib/abilities.decorator.ts or similar
    import { SetMetadata } from '@nestjs/common';
    import { Subject } from './ability.factory'; // Your subject type

    export type Action = 'manage' | 'create' | 'read' | 'update' | 'delete';

    export interface RequiredRule {
      action: Action;
      subject: Subject;
      conditions?: any; // CASL conditions
    }

    export const CHECK_ABILITY = 'check_ability';
    export const CheckAbilities = (...requirements: RequiredRule[]) =>
      SetMetadata(CHECK_ABILITY, requirements);
    ```

2. **Create `CaslGuard` (typically in `libs/rbac/src/lib/guards/`):**

    ```typescript
    // libs/rbac/src/lib/guards/casl.guard.ts
    import { Injectable, CanActivate, ExecutionContext } from '@nestjs/common';
    import { Reflector } from '@nestjs/core';
    import { AbilityFactory, AppAbility } from '../ability.factory'; // Your CASL AbilityFactory
    import { RequiredRule, CHECK_ABILITY } from '../abilities.decorator';
    import { CurrentUser } from '@acci/auth-decorators'; // Assuming you have a way to get current user

    @Injectable()
    export class CaslGuard implements CanActivate {
      constructor(
        private reflector: Reflector,
        private abilityFactory: AbilityFactory,
      ) {}

      async canActivate(context: ExecutionContext): Promise<boolean> {
        const rules = this.reflector.get<RequiredRule[]>(CHECK_ABILITY, context.getHandler()) || [];
        if (!rules.length) {
          return true; // No rules, access granted
        }

        const request = context.switchToHttp().getRequest();
        const user = request.user as CurrentUser; // Adjust to how you get the authenticated user

        if (!user) return false; // No user, access denied

        const ability = this.abilityFactory.createForUser(user);

        // For ownership checks, you might need to load the resource first
        // const resourceId = request.params.id;
        // const resource = await someService.loadResource(resourceId);

        return rules.every(rule => {
          // If rule.subject is a class, CASL can infer from an instance
          // If it's a string, you pass the string directly
          // For conditions, ensure you pass the resource to check against if needed
          return ability.can(rule.action, rule.subject /*, resource for conditions */);
        });
      }
    }
    ```

3. **Apply the Guard to a Controller Method:**

    ```typescript
    // apps/control-plane-api/src/app/tenants/tenants.controller.ts
    import { Controller, Post, UseGuards, Body, Get, Param } from '@nestjs/common';
    import { JwtAuthGuard } from '@acci/auth-guards'; // Your JWT Auth Guard
    import { CaslGuard, CheckAbilities } from '@acci/rbac'; // Assuming guard and decorator are exported
    import { CreateTenantDto } from './dto/create-tenant.dto';
    import { TenantsService } from './tenants.service';

    @Controller('tenants')
    @UseGuards(JwtAuthGuard, CaslGuard) // Apply CaslGuard globally or per method
    export class TenantsController {
      constructor(private readonly tenantsService: TenantsService) {}

      @Post()
      @CheckAbilities({ action: 'create', subject: 'Tenant' }) // 'Tenant' can be a class or string
      create(@Body() createTenantDto: CreateTenantDto) {
        return this.tenantsService.create(createTenantDto);
      }

      @Get(':id')
      @CheckAbilities({ action: 'read', subject: 'Tenant' })
      findOne(@Param('id') id: string) {
        // For conditional checks like 'read own tenant', the guard would need to load the tenant
        // and CASL conditions would compare user.tenantId with resource.tenantId
        return this.tenantsService.findOne(id);
      }
    }
    ```

## 6. Working with Multi-Tenancy and RLS

(See `docs/concept/multi-tenancy.md` and ADR-006).

1. **Ensure `tenant_id` in Entities:** All tenant-specific entities must have a `tenant_id` column.
2. **Tenant Context Propagation:**
    * `TenantMiddleware` (in `libs/tenancy`) extracts `tenant_id` from requests (e.g., JWT or header) and stores it in `AsyncLocalStorage`.
    * Register this middleware globally in your `main.ts` or `AppModule`.
3. **MikroORM Global Filter:**
    * A global filter named `

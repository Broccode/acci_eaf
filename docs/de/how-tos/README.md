# ACCI EAF How-To Anleitungen

Version: 1.0
Datum: 2025-05-07
Status: Entwurf

Willkommen zu den How-To Anleitungen für das ACCI Enterprise Application Framework (EAF). Dieser Abschnitt bietet praktische Schritt-für-Schritt-Anweisungen für gängige Entwicklungsaufgaben und die Nutzung verschiedener Framework-Funktionen.

## Inhaltsverzeichnis

1. [Verwendung des Testing Frameworks (`@acci/testing`)](#1-verwendung-des-testing-frameworks-accitesting)
    * [Schreiben von Unit-Tests für Services/Handler](#schreiben-von-unit-tests-für-serviceshandler)
    * [Schreiben von Integrationstests mit `MikroOrmTestHelper` und `testDbManager`](#schreiben-von-integrationstests-mit-mikroormtesthelper-und-testdbmanager)
    * [Schreiben von E2E-Tests mit `NestE2ETestHelper`](#schreiben-von-e2e-tests-mit-neste2etesthelper)
    * [Testen der Redis-Integration mit `testRedisManager`](#testen-der-redis-integration-mit-testredismanager)
2. [Implementieren eines neuen CQRS Command/Query Handlers](#2-implementieren-eines-neuen-cqrs-commandquery-handlers)
3. [Hinzufügen eines neuen Plugins](#3-hinzufügen-eines-neuen-plugins)
4. [Konfigurieren und Verwenden von Health Check Endpunkten](#4-konfigurieren-und-verwenden-von-health-check-endpunkten)
5. [Implementieren einer benutzerdefinierten Autorisierung mit `CaslGuard`](#5-implementieren-einer-benutzerdefinierten-autorisierung-mit-caslguard)
6. [Arbeiten mit Mandantenfähigkeit und RLS](#6-arbeiten-mit-mandantenfähigkeit-und-rls)
7. [Verwalten von Datenbankmigrationen (Kern & Plugins)](#7-verwalten-von-datenbankmigrationen-kern--plugins)

---

## 1. Verwendung des Testing Frameworks (`@acci/testing`)

Die `@acci/testing`-Bibliothek stellt Hilfsprogramme bereit, um das Schreiben von Unit-, Integrations- und E2E-Tests für Anwendungen, die mit ACCI EAF erstellt wurden, zu optimieren.

### Schreiben von Unit-Tests für Services/Handler

Unit-Tests konzentrieren sich auf isolierte Komponenten wie Services oder CQRS-Handler. Abhängigkeiten werden typischerweise gemockt.

**Szenario:** Testen eines `CreateTenantCommandHandler`.

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

### Schreiben von Integrationstests mit `MikroOrmTestHelper` und `testDbManager`

Integrationstests überprüfen Interaktionen zwischen Komponenten, insbesondere mit der Datenbank. `@acci/testing` stellt `MikroOrmTestHelper` (von `@mikro-orm/nestjs/testing` oder ein benutzerdefinierter Wrapper) und `testDbManager` (benutzerdefinierter Helfer für Testcontainers) bereit.

**Szenario:** Testen von `TenantRepository` (MikroORM-Implementierung).

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

*Hinweis: Für eine echte Datenbankintegration würde `testDbManager` das Starten/Stoppen von Docker-Containern (z.B. PostgreSQL über Testcontainers) und die Bereitstellung von Verbindungsdetails für MikroORM übernehmen.* Beispiele für die Verwendung von `testDbManager` finden Sie in den `@acci/testing`-Beispielen.

### Schreiben von E2E-Tests mit `NestE2ETestHelper`

End-to-End-Tests validieren ganze Anfrageabläufe durch die API.
`NestE2ETestHelper` (ein benutzerdefinierter Helfer oder direkte Verwendung von `@nestjs/testing`) richtet die vollständige NestJS-Anwendung ein.

**Szenario:** Testen der `/tenants` CRUD-Endpunkte von `ControlPlaneApi`.

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

### Testen der Redis-Integration mit `testRedisManager`

`testRedisManager` (benutzerdefinierter Helfer von `@acci/testing`) würde Testcontainers verwenden, um eine Redis-Instanz zum Testen von Diensten, die Redis verwenden (z.B. für Caching), hochzufahren.

**Szenario:** Testen eines `RedisCacheService`.

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

## 2. Implementieren eines neuen CQRS Command/Query Handlers

CQRS ist zentral für ACCI EAF. So fügen Sie einen neuen Befehl und seinen Handler hinzu.

**A. Definieren des Befehls (in `libs/core/src/lib/application/commands/...`):**

```typescript
// libs/core/src/lib/application/commands/some-feature/do-something.command.ts
export class DoSomethingCommand {
  constructor(public readonly entityId: string, public readonly someData: string) {}
}
```

**B. Definieren des Command Handlers (in `libs/core/src/lib/application/commands/...`):**

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
    // Die tatsächliche Implementierung würde Domänenlogik, Repository-Interaktion und Event-Publishing beinhalten.
  }
}
```

**C. Registrieren des Handlers in einem NestJS-Modul (z.B. in `libs/core` oder einem Feature-Modul):**

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

**D. Auslösen des Befehls von einem Service oder Controller (z.B. in `apps/control-plane-api`):**

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

*Folgen Sie einem ähnlichen Muster für Queries und Query Handler (mit `@QueryHandler` und `IQueryHandler`).*

## 3. Hinzufügen eines neuen Plugins

Plugins erweitern die EAF-Funktionalität. Siehe `docs/de/concept/plugin-system.md` für Kernkonzepte.

1. **Plugin-Verzeichnis erstellen:** Erstellen Sie eine neue Bibliothek für Ihr Plugin, z.B. `libs/plugins/my-custom-plugin`.

    ```bash
    npx nx generate @nx/nest:library my-custom-plugin --directory=libs/plugins --publishable --importPath=@my-scope/my-custom-plugin
    ```

2. **Plugin-Modul entwickeln:** Implementieren Sie Ihr(e) NestJS-Modul(e), Services, Controller, Entitäten usw. innerhalb dieser Bibliothek.

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

3. **Entitäten definieren (falls vorhanden):** Platzieren Sie MikroORM-Entitäten in einem `entities`-Unterordner.
4. **Migrationen erstellen (falls vorhanden):** Initialisieren Sie Migrationen für Ihr Plugin, falls es Entitäten hat (ADR-008).
    * Aktualisieren Sie `mikro-orm.config.ts` in Ihrem Plugin, um auf seinen eigenen Migrationspfad zu verweisen.
    * Generieren Sie die initiale Migration: `npx mikro-orm migration:create --initial -c ./libs/plugins/my-custom-plugin/src/mikro-orm.config.ts`
5. **MikroORM-Konfiguration der Haupt-App aktualisieren:**
    * Stellen Sie sicher, dass die `mikro-orm.config.ts` der Hauptanwendung Entitäten aus Ihrem Plugin erkennen kann (z.B. fügen Sie `'libs/plugins/my-custom-plugin/src/lib/entities/**/*.entity.js'` zu den `entities`-Glob-Mustern hinzu, passen Sie es an Ihren Build-Output an).
    * Stellen Sie sicher, dass die Hauptmigrationskonfiguration Plugin-Migrationen erkennen kann (z.B. fügen Sie den Pfad zum `migrations.path`-Array hinzu oder passen Sie `migrations.glob` an).
6. **Plugin-Modul importieren:** Importieren Sie `MyCustomPluginModule` in Ihr Hauptanwendungsmodul (z.B. `apps/sample-app/src/app.module.ts`).

## 4. Konfigurieren und Verwenden von Health Check Endpunkten

Health Checks werden von `@nestjs/terminus` bereitgestellt. (Siehe `docs/de/concept/observability.md`).

**A. Installieren von `@nestjs/terminus` (falls noch nicht vorhanden):**
   `npm install @nestjs/terminus`

**B. Hinzufügen des Health-Moduls zu Ihrer Anwendung (z.B. `apps/sample-app/src/app/health/health.module.ts`):**

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

**C. Erstellen eines benutzerdefinierten Health Indicators (z.B. für MikroORM):**

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

**D. Erstellen des Health Controllers (z.B. `apps/sample-app/src/app/health/health.controller.ts`):**

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

**E. Importieren Sie `HealthModule` in Ihr `AppModule`**.

## 5. Implementieren einer benutzerdefinierten Autorisierung mit `CaslGuard`

(Siehe `docs/de/concept/security.md` und ADR-001).
`casl` wird für RBAC/ABAC verwendet. Ein `CaslGuard` schützt Routen.

1. **Fähigkeiten definieren (z.B. in einem Feature-Modul oder `libs/rbac`):**

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

2. **`CaslGuard` erstellen (typischerweise in `libs/rbac/src/lib/guards/`):**

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

3. **Guard auf eine Controller-Methode anwenden:**

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

## 6. Arbeiten mit Mandantenfähigkeit und RLS

(Siehe `docs/de/concept/multi-tenancy.md` und ADR-006).

1. **Sicherstellen von `tenant_id` in Entitäten:** Alle mandantenspezifischen Entitäten müssen eine `tenant_id`-Spalte haben.
2. **Weitergabe des Mandantenkontexts:**
    * `TenantMiddleware` (in `libs/tenancy`) extrahiert `tenant_id` aus Anfragen (z.B. JWT oder Header) und speichert sie in `AsyncLocalStorage`.
    * Registrieren Sie diese Middleware global in Ihrer `main.ts` oder `AppModule`.
3. **MikroORM Global Filter:**
    * Ein globaler Filter namens `tenantFilter` (o.ä.) ist in `libs/tenancy` oder Ihrer MikroORM-Konfiguration definiert.
    * Dieser Filter fügt automatisch `WHERE tenant_id = :currentTenantId` zu Abfragen für Entitäten hinzu, für die dieser Filter aktiviert ist.
    * Aktivieren Sie diesen Filter für Ihre mandantenspezifischen Entitäten: `@Filter({ name: 'tenantFilter', cond: args => ({ tenantId: args.tenantId }), default: true })`.
    * Die `args.tenantId` wird dynamisch von MikroORM aus Parametern bereitgestellt, die Sie beim Aktivieren von Filtern für eine Anfrage übergeben, typischerweise aus `AsyncLocalStorage`.
4. **Repositories und Services:** Im Allgemeinen müssen Repositories und Services keine `tenant_id`-Bedingungen manuell hinzufügen, wenn der globale Filter korrekt eingerichtet und standardmäßig aktiviert ist.
5. **Zugriff auf `tenant_id`:** Wenn ein Dienst die aktuelle `tenant_id` für Logik benötigt (nicht zum Abfragen), kann er `TenantContextService` (aus `libs/tenancy`) injizieren.

## 7. Verwalten von Datenbankmigrationen (Kern & Plugins)

(Siehe ADR-008 für Plugin-Migrationen).

1. **Kernanwendungsmigrationen:**
    * Stellen Sie sicher, dass `mikro-orm.config.ts` in Ihrer Hauptanwendung (`apps/control-plane-api` oder `apps/sample-app`) korrekt für Migrationen eingerichtet ist (Pfad, Muster, Tabellenname).
    * Wenn Sie eine Kernentität ändern: `npx mikro-orm migration:create -c ./apps/my-app/mikro-orm.config.ts`
    * Um Migrationen auszuführen: `npx mikro-orm migration:up -c ./apps/my-app/mikro-orm.config.ts` (oder verwenden Sie das Nx-Target `nx run my-app:migration:run`).
2. **Plugin-Migrationen:**
    * Jedes Plugin mit Entitäten sollte sein eigenes minimales `mikro-orm.config.ts` haben, das auf seinen lokalen Migrationspfad verweist.
    * Wenn Sie eine Plugin-Entität ändern: `npx mikro-orm migration:create -c ./libs/plugins/my-plugin/mikro-orm.config.ts`
    * **Konfiguration der Haupt-App für Plugin-Migrationen:** Die `mikro-orm.config.ts` der Hauptanwendung muss so konfiguriert werden, dass sie auch diese Plugin-Migrationen erkennt und ausführt. Dies beinhaltet typischerweise:
        * Setzen von `migrations.migrationsList` auf ein Array von Objekten, die jeweils einen Pfad zu einem Satz von Migrationen angeben (Kern und jedes Plugin).
        * Oder Anpassen von `migrations.path` zu einem Array von Pfaden und `migrations.glob`, um Migrationsdateien über all diese Pfade korrekt abzugleichen.

        ```typescript
        // Example in main app's mikro-orm.config.ts (conceptual)
        migrations: {
          tableName: 'mikro_orm_migrations',
          path: ['./dist/apps/my-app/migrations', './dist/libs/plugins/my-plugin/migrations'], // Path to JS files after build
          glob: '!(*.d).{js,ts}',
          transactional: true,
          disableForeignKeys: false,
          allOrNothing: true,
          snapshot: true,
        },
        ```

    * Das Ausführen von `npx mikro-orm migration:up` aus dem Kontext der Haupt-App führt dann alle ausstehenden Kern- und Plugin-Migrationen nacheinander aus (basierend auf ihren Namen/Zeitstempeln).

*Testen Sie die Migrationsgenerierung und -ausführung immer gründlich in einer Entwicklungsumgebung.*

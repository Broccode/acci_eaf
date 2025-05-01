// Module declarations for TypeScript

declare module '@testcontainers/postgresql';

import { Test, TestingModule } from '@nestjs/testing';
import { PostgreSqlContainer } from '@testcontainers/postgresql';
import { MikroOrmModule } from '@mikro-orm/nestjs';
import { EntityManager } from '@mikro-orm/core';
import {
  TenantCreatedEvent,
  TenantActivatedEvent,
  TenantDeactivatedEvent,
  TenantUpdatedEvent,
  TenantStatus
} from 'core';
import { TenantProjector } from '../lib/persistence/tenant/tenant-projector';
import { TenantReadModel } from '../lib/persistence/entities/tenant-read-model.entity';
import { ProcessedEventRecord } from '../lib/persistence/entities/processed-event.entity';
import { InMemoryIdempotencyStore } from '../lib/idempotency/in-memory-idempotency-store';

// Set default timeout for setup and tests to 20 seconds
jest.setTimeout(20000);

/** Suite-based integration test for the TenantProjector */
describe('TenantProjector Integration (Suites + Testcontainers)', () => {
  let container: any;
  let em: EntityManager;
  let projector: TenantProjector;
  let idempotencyStore: InMemoryIdempotencyStore;

  beforeAll(async () => {
    container = await new PostgreSqlContainer('postgres:latest')
      .withDatabase('testdb')
      .start();
    const uri = container.getConnectionUri();

    // Setup NestJS TestingModule for TenantProjector
    const moduleRef: TestingModule = await Test.createTestingModule({
      imports: [
        MikroOrmModule.forRoot({
          clientUrl: uri,
          entities: [TenantReadModel, ProcessedEventRecord],
          autoLoadEntities: true,
          registerRequestContext: true,
          allowGlobalContext: true,
        }),
        MikroOrmModule.forFeature([TenantReadModel]),
      ],
      providers: [
        TenantProjector,
        { provide: 'IdempotencyStore', useClass: InMemoryIdempotencyStore },
      ],
    }).compile();

    em = moduleRef.get<EntityManager>(EntityManager);
    projector = moduleRef.get<TenantProjector>(TenantProjector);
    idempotencyStore = moduleRef.get<InMemoryIdempotencyStore>('IdempotencyStore');

    const driver = em.getDriver();
    const generator = driver.getPlatform().getSchemaGenerator(driver);
    await generator.createSchema();
  });

  afterAll(async () => {
    const driver = em.getDriver();
    const generator = driver.getPlatform().getSchemaGenerator(driver);
    await generator.dropSchema();
    await container.stop();
  });

  function makeEvent<T>(event: T): T {
    return event;
  }

  it('should process TenantCreated and TenantActivated events correctly', async () => {
    const tenantId = '1';
    // Create and process created event
    const createEvt = makeEvent<TenantCreatedEvent>({
      id: 'evt1',
      type: 'TenantCreated',
      version: 1,
      aggregateId: tenantId,
      occurredAt: new Date(),
      payload: { name: 'Name', description: 'Desc', contactEmail: 'email', configuration: {} },
      metadata: {}
    });
    await projector.processEvent(createEvt);

    // Process activated event
    const activateEvt = makeEvent<TenantActivatedEvent>(new TenantActivatedEvent(tenantId, { activatedAt: new Date() }));
    await projector.processEvent(activateEvt);

    const readModel = await em.findOne(TenantReadModel, { id: tenantId });
    expect(readModel).toBeDefined();
    expect(readModel?.status).toBe(TenantStatus.ACTIVE);

    // Check idempotency store
    const processed = await idempotencyStore.hasBeenProcessed(createEvt.id, 'TenantProjector');
    expect(processed).toBe(true);
  });
}); 
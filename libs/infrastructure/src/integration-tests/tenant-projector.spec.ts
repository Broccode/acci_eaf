import { Test } from '@nestjs/testing';
import { MikroOrmModule } from '@mikro-orm/nestjs';
import { EntityManager } from '@mikro-orm/core';
import {
  DomainEvent,
  TenantCreatedEvent,
  TenantActivatedEvent,
  TenantDeactivatedEvent,
  TenantUpdatedEvent,
  TenantStatus
} from 'core';
import { TenantProjector } from '../lib/persistence/tenant/tenant-projector';
import { TenantReadModel } from '../lib/persistence/entities/tenant-read-model.entity';
import { InMemoryIdempotencyStore } from '../lib/idempotency/in-memory-idempotency-store';
import { v4 as uuidv4 } from 'uuid';
import { BetterSqliteDriver } from '@mikro-orm/better-sqlite';

describe('TenantProjector Integration Test', () => {
  let tenantProjector: TenantProjector;
  let entityManager: EntityManager;
  let idempotencyStore: InMemoryIdempotencyStore;
  
  beforeEach(async () => {
    const moduleRef = await Test.createTestingModule({
      imports: [
        MikroOrmModule.forRootAsync({
          useFactory: () => ({
            driver: BetterSqliteDriver,
            dbName: ':memory:',
            entities: [TenantReadModel],
            autoLoadEntities: true,
            registerRequestContext: true,
            allowGlobalContext: true,
          }),
        }),
        MikroOrmModule.forFeature([TenantReadModel]),
      ],
      providers: [
        TenantProjector,
        {
          provide: 'IdempotencyStore',
          useClass: InMemoryIdempotencyStore,
        },
      ],
    }).compile();

    tenantProjector = moduleRef.get<TenantProjector>(TenantProjector);
    entityManager = moduleRef.get<EntityManager>(EntityManager);
    idempotencyStore = moduleRef.get('IdempotencyStore');

    // Create schema for in-memory SQLite database
    const driverInstance = entityManager.getDriver();
    const generator = driverInstance.getPlatform().getSchemaGenerator(driverInstance);
    await generator.createSchema();
  });

  afterEach(async () => {
    // Clean up the database after each test
    const driverInstance = entityManager.getDriver();
    const generator = driverInstance.getPlatform().getSchemaGenerator(driverInstance);
    await generator.dropSchema();
  });

  function createTenantCreatedEvent(tenantId: string): TenantCreatedEvent {
    return {
      id: uuidv4(),
      type: 'TenantCreated',
      version: 1,
      aggregateId: tenantId,
      occurredAt: new Date(),
      payload: {
        name: 'Test Tenant',
        description: 'A test tenant',
        contactEmail: 'test@example.com',
        configuration: { key: 'value' }
      },
      metadata: {}
    };
  }

  function createTenantActivatedEvent(tenantId: string): TenantActivatedEvent {
    return new TenantActivatedEvent(tenantId, { activatedAt: new Date() });
  }

  function createTenantUpdatedEvent(tenantId: string): TenantUpdatedEvent {
    return {
      id: uuidv4(),
      type: 'TenantUpdated',
      version: 1,
      aggregateId: tenantId,
      occurredAt: new Date(),
      payload: {
        name: 'Updated Tenant',
        description: 'An updated tenant',
        contactEmail: 'updated@example.com',
        configuration: { key: 'updated' },
        updatedAt: new Date()
      },
      metadata: {}
    };
  }

  it('should create a tenant read model when processing a TenantCreated event', async () => {
    // Arrange
    const tenantId = uuidv4();
    const event = createTenantCreatedEvent(tenantId);
    
    // Act
    await tenantProjector.processEvent(event);
    
    // Assert
    const readModel = await entityManager.findOne(TenantReadModel, { id: tenantId });
    expect(readModel).toBeDefined();
    expect(readModel?.name).toBe('Test Tenant');
    expect(readModel?.description).toBe('A test tenant');
    expect(readModel?.contactEmail).toBe('test@example.com');
    expect(readModel?.configuration).toEqual({ key: 'value' });
    expect(readModel?.status).toBe(TenantStatus.ACTIVE);
  });

  it('should update the tenant read model when processing update events', async () => {
    // Arrange
    const tenantId = uuidv4();
    const createEvent = createTenantCreatedEvent(tenantId);
    await tenantProjector.processEvent(createEvent);
    
    const updateEvent = createTenantUpdatedEvent(tenantId);
    
    // Act
    await tenantProjector.processEvent(updateEvent);
    
    // Assert
    const readModel = await entityManager.findOne(TenantReadModel, { id: tenantId });
    expect(readModel).toBeDefined();
    expect(readModel?.name).toBe('Updated Tenant');
    expect(readModel?.description).toBe('An updated tenant');
    expect(readModel?.contactEmail).toBe('updated@example.com');
    expect(readModel?.configuration).toEqual({ key: 'updated' });
  });

  it('should update tenant status when processing status change events', async () => {
    // Arrange
    const tenantId = uuidv4();
    await tenantProjector.processEvent(createTenantCreatedEvent(tenantId));
    
    const deactivateEvent = new TenantDeactivatedEvent(tenantId, { deactivatedAt: new Date() });
    
    // Act
    await tenantProjector.processEvent(deactivateEvent);
    
    // Assert
    let readModel = await entityManager.findOne(TenantReadModel, { id: tenantId });
    expect(readModel?.status).toBe(TenantStatus.INACTIVE);
    
    // Reactivate the tenant
    const activateEvent = createTenantActivatedEvent(tenantId);
    await tenantProjector.processEvent(activateEvent);
    
    // Check status after reactivation
    readModel = await entityManager.findOne(TenantReadModel, { id: tenantId });
    expect(readModel?.status).toBe(TenantStatus.ACTIVE);
  });

  it('should not process the same event twice (idempotency)', async () => {
    // Arrange
    const tenantId = uuidv4();
    const event = createTenantCreatedEvent(tenantId);
    
    // Act - process the same event twice
    await tenantProjector.processEvent(event);
    await tenantProjector.processEvent(event);
    
    // Assert - check that only one read model exists with that ID
    const tenants = await entityManager.find(TenantReadModel, {});
    expect(tenants.length).toBe(1);
    
    // Verify the event was marked as processed in the idempotency store
    const wasProcessed = await idempotencyStore.hasBeenProcessed(event.id, 'TenantProjector');
    expect(wasProcessed).toBe(true);
  });

  it('should handle partial updates correctly', async () => {
    // Arrange
    const tenantId = uuidv4();
    await tenantProjector.processEvent(createTenantCreatedEvent(tenantId));
    
    const partialUpdateEvent = new TenantUpdatedEvent(tenantId, { name: 'New Name', updatedAt: new Date() });
    
    // Act
    await tenantProjector.processEvent(partialUpdateEvent);
    
    // Assert
    const readModel = await entityManager.findOne(TenantReadModel, { id: tenantId });
    expect(readModel).toBeDefined();
    expect(readModel?.name).toBe('New Name');
    // Original values should be preserved
    expect(readModel?.description).toBe('A test tenant');
    expect(readModel?.contactEmail).toBe('test@example.com');
    expect(readModel?.configuration).toEqual({ key: 'value' });
  });
}); 
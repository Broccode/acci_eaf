import { Test } from '@nestjs/testing';
import { MikroOrmModule } from '@mikro-orm/nestjs';
import { EntityManager } from '@mikro-orm/core';
import { 
  TenantAggregate,
  TenantRepository,
  EventStore,
  IdempotencyStore,
  TenantStatus
} from 'core';
import { TenantProjector } from '../lib/persistence/tenant/tenant-projector';
import { PostgresTenantRepository } from '../lib/persistence/tenant/postgres-tenant-repository';
import { PostgresEventStore } from '../lib/persistence/postgres-event-store';
import { InMemoryIdempotencyStore } from '../lib/idempotency/in-memory-idempotency-store';
import { TenantReadModel } from '../lib/persistence/entities/tenant-read-model.entity';
import { EventRecord } from '../lib/persistence/entities/event-record.entity';
import { ProcessedEventRecord } from '../lib/persistence/entities/processed-event.entity';
import { v4 as uuidv4 } from 'uuid';
import { BetterSqliteDriver } from '@mikro-orm/better-sqlite';

describe('Tenant End-to-End Integration Test', () => {
  let tenantRepository: TenantRepository;
  let tenantProjector: TenantProjector;
  let eventStore: EventStore;
  let entityManager: EntityManager;
  
  beforeEach(async () => {
    const moduleRef = await Test.createTestingModule({
      imports: [
        MikroOrmModule.forRootAsync({
          useFactory: () => ({
            driver: BetterSqliteDriver,
            dbName: ':memory:',
            entities: [EventRecord, TenantReadModel, ProcessedEventRecord],
            autoLoadEntities: true,
            registerRequestContext: true,
            allowGlobalContext: true,
          }),
        }),
        MikroOrmModule.forFeature([EventRecord, TenantReadModel, ProcessedEventRecord]),
      ],
      providers: [
        PostgresEventStore,
        PostgresTenantRepository,
        TenantProjector,
        {
          provide: 'IdempotencyStore',
          useClass: InMemoryIdempotencyStore,
        },
      ],
    }).compile();

    eventStore = moduleRef.get<EventStore>(PostgresEventStore);
    tenantRepository = moduleRef.get<TenantRepository>(PostgresTenantRepository);
    tenantProjector = moduleRef.get<TenantProjector>(TenantProjector);
    entityManager = moduleRef.get<EntityManager>(EntityManager);

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

  /**
   * Helper to process all events for a tenant through the projector
   */
  async function processEventsForTenant(tenantId: string): Promise<void> {
    const events = await eventStore.loadEvents(tenantId);
    for (const event of events) {
      await tenantProjector.processEvent(event);
    }
  }

  it('should create a tenant and update the read model', async () => {
    // Arrange: create new tenant aggregate with initial data
    const tenant = TenantAggregate.create('Test Tenant', 'This is a test tenant', 'test@example.com', { setting: 'value' });
    const tenantId = tenant.id;
    
    // Act - Create a tenant aggregate
    await tenantRepository.save(tenant);
    
    // Process the events to update the read model
    await processEventsForTenant(tenantId);
    
    // Assert - Check the read model
    const readModel = await entityManager.findOne(TenantReadModel, { id: tenantId });
    
    expect(readModel).toBeDefined();
    expect(readModel?.name).toBe('Test Tenant');
    expect(readModel?.description).toBe('This is a test tenant');
    expect(readModel?.contactEmail).toBe('test@example.com');
    expect(readModel?.configuration).toEqual({ setting: 'value' });
    expect(readModel?.status).toBe(TenantStatus.ACTIVE);
  });

  it('should update tenant status in the read model when tenant status changes', async () => {
    // Arrange: create and save new tenant
    const tenant = TenantAggregate.create('Status Test Tenant', 'Testing status changes');
    const tenantId = tenant.id;
    await tenantRepository.save(tenant);
    await processEventsForTenant(tenantId);
    
    // Act - Deactivate the tenant
    const loadedTenant = await tenantRepository.findById(tenantId);
    expect(loadedTenant).toBeDefined();
    
    loadedTenant?.deactivate();
    await tenantRepository.save(loadedTenant!);
    await processEventsForTenant(tenantId);
    
    // Assert - Check deactivated status in read model
    let readModel = await entityManager.findOne(TenantReadModel, { id: tenantId });
    expect(readModel?.status).toBe(TenantStatus.INACTIVE);
    
    // Act - Suspend the tenant
    const deactivatedTenant = await tenantRepository.findById(tenantId);
    deactivatedTenant?.suspend();
    await tenantRepository.save(deactivatedTenant!);
    await processEventsForTenant(tenantId);
    
    // Assert - Check suspended status in read model
    readModel = await entityManager.findOne(TenantReadModel, { id: tenantId });
    expect(readModel?.status).toBe(TenantStatus.SUSPENDED);
    
    // Act - Reactivate the tenant
    const suspendedTenant = await tenantRepository.findById(tenantId);
    suspendedTenant?.activate();
    await tenantRepository.save(suspendedTenant!);
    await processEventsForTenant(tenantId);
    
    // Assert - Check active status in read model
    readModel = await entityManager.findOne(TenantReadModel, { id: tenantId });
    expect(readModel?.status).toBe(TenantStatus.ACTIVE);
  });

  it('should update tenant properties in the read model when tenant is updated', async () => {
    // Arrange: create and save new tenant
    const tenant = TenantAggregate.create('Original Tenant', 'Original description', 'original@example.com');
    const tenantId = tenant.id;
    await tenantRepository.save(tenant);
    await processEventsForTenant(tenantId);
    
    // Act - Update the tenant
    const loadedTenant = await tenantRepository.findById(tenantId);
    expect(loadedTenant).toBeDefined();
    
    loadedTenant?.update('Updated Tenant', 'Updated description', 'updated@example.com', { config: 'updated' });
    await tenantRepository.save(loadedTenant!);
    await processEventsForTenant(tenantId);
    
    // Assert - Check updated properties in read model
    const readModel = await entityManager.findOne(TenantReadModel, { id: tenantId });
    expect(readModel?.name).toBe('Updated Tenant');
    expect(readModel?.description).toBe('Updated description');
    expect(readModel?.contactEmail).toBe('updated@example.com');
    expect(readModel?.configuration).toEqual({ config: 'updated' });
  });

  it('should handle multiple updates to the same tenant correctly', async () => {
    // Arrange: create and save new tenant
    const tenant = TenantAggregate.create('Multi-Update Tenant', 'Testing multiple updates');
    const tenantId = tenant.id;
    await tenantRepository.save(tenant);
    await processEventsForTenant(tenantId);
    
    // Perform a sequence of operations
    const sequence = async () => {
      // Update 1: Change name
      const t1 = await tenantRepository.findById(tenantId);
      t1?.update('First Update', undefined, undefined, undefined);
      await tenantRepository.save(t1!);
      await processEventsForTenant(tenantId);
      
      // Update 2: Change status
      const t2 = await tenantRepository.findById(tenantId);
      t2?.deactivate();
      await tenantRepository.save(t2!);
      await processEventsForTenant(tenantId);
      
      // Update 3: Change email
      const t3 = await tenantRepository.findById(tenantId);
      t3?.update(undefined, undefined, 'multi@example.com', undefined);
      await tenantRepository.save(t3!);
      await processEventsForTenant(tenantId);
      
      // Update 4: Change config
      const t4 = await tenantRepository.findById(tenantId);
      t4?.update(undefined, undefined, undefined, { complex: { nested: 'value' } });
      await tenantRepository.save(t4!);
      await processEventsForTenant(tenantId);
      
      // Update 5: Reactivate
      const t5 = await tenantRepository.findById(tenantId);
      t5?.activate();
      await tenantRepository.save(t5!);
      await processEventsForTenant(tenantId);
    };
    
    // Act
    await sequence();
    
    // Assert - Final state reflects all changes
    const readModel = await entityManager.findOne(TenantReadModel, { id: tenantId });
    expect(readModel?.name).toBe('First Update');
    expect(readModel?.description).toBe('Testing multiple updates'); // Unchanged
    expect(readModel?.contactEmail).toBe('multi@example.com');
    expect(readModel?.configuration).toEqual({ complex: { nested: 'value' } });
    expect(readModel?.status).toBe(TenantStatus.ACTIVE);
    
    // Verify correct number of events were stored
    const events = await eventStore.loadEvents(tenantId);
    expect(events.length).toBe(6); // Create + 5 updates
  });
}); 
import { Test } from '@nestjs/testing';
import { MikroOrmModule } from '@mikro-orm/nestjs';
import { EntityManager } from '@mikro-orm/core';
import { 
  TenantAggregate, 
  EventStore, 
  TenantCreatedEvent,
  TenantActivatedEvent,
  TenantDeactivatedEvent,
  TenantUpdatedEvent
} from 'core';
import { PostgresEventStore } from '../lib/persistence/postgres-event-store';
import { PostgresTenantRepository } from '../lib/persistence/tenant/postgres-tenant-repository';
import { EventRecord } from '../lib/persistence/entities/event-record.entity';
import { BetterSqliteDriver } from '@mikro-orm/better-sqlite';

describe('PostgresTenantRepository Integration Test', () => {
  let eventStore: EventStore;
  let tenantRepository: PostgresTenantRepository;
  let entityManager: EntityManager;
  
  beforeEach(async () => {
    const moduleRef = await Test.createTestingModule({
      imports: [
        MikroOrmModule.forRoot({
          driver: BetterSqliteDriver,
          dbName: ':memory:',
          entities: [EventRecord],
          autoLoadEntities: true,
          registerRequestContext: true,
          allowGlobalContext: true,
        }),
        MikroOrmModule.forFeature([EventRecord]),
      ],
      providers: [
        PostgresEventStore,
        PostgresTenantRepository,
      ],
    }).compile();

    eventStore = moduleRef.get<EventStore>(PostgresEventStore);
    tenantRepository = moduleRef.get<PostgresTenantRepository>(PostgresTenantRepository);
    entityManager = moduleRef.get<EntityManager>(EntityManager);

    // Create schema for in-memory SQLite database
    const driver = entityManager.getDriver();
    const generator = driver.getPlatform().getSchemaGenerator(driver);
    await generator.createSchema();
  });

  afterEach(async () => {
    // Clean up the database after each test
    const driver = entityManager.getDriver();
    const generator = driver.getPlatform().getSchemaGenerator(driver);
    await generator.dropSchema();
  });

  it('should save and load a tenant aggregate', async () => {
    // Arrange: create new tenant aggregate (status is ACTIVE by creation)
    const tenant = TenantAggregate.create('Test Tenant', 'A test tenant', 'test@example.com');
    const tenantId = tenant.id;

    // Act
    await tenantRepository.save(tenant);

    // Load the tenant back from the repository
    const loadedTenant = await tenantRepository.findById(tenantId);

    // Assert
    expect(loadedTenant).toBeDefined();
    expect(loadedTenant?.id).toBe(tenantId);
    expect(loadedTenant?.name).toBe('Test Tenant');
    expect(loadedTenant?.description).toBe('A test tenant');
    expect(loadedTenant?.contactEmail).toBe('test@example.com');
    expect(loadedTenant?.status).toBe('active');

    // Verify only the creation event has been persisted
    const events = await eventStore.loadEvents(tenantId);
    expect(events.length).toBe(1);
    expect(events[0].type).toBe('TenantCreated');
  });

  it('should handle tenant updates', async () => {
    // Arrange: create and save
    const tenant = TenantAggregate.create('Initial Name', 'Initial Description', 'initial@example.com');
    const tenantId = tenant.id;
    await tenantRepository.save(tenant);
    
    // Act - load, update and save
    const loadedTenant = await tenantRepository.findById(tenantId);
    expect(loadedTenant).toBeDefined();
    
    loadedTenant?.update('Updated Name', 'Updated Description', 'updated@example.com');
    await tenantRepository.save(loadedTenant!);
    
    // Load again to verify changes
    const updatedTenant = await tenantRepository.findById(tenantId);
    
    // Assert
    expect(updatedTenant).toBeDefined();
    expect(updatedTenant?.name).toBe('Updated Name');
    expect(updatedTenant?.description).toBe('Updated Description');
    expect(updatedTenant?.contactEmail).toBe('updated@example.com');
    
    // Verify the events
    const events = await eventStore.loadEvents(tenantId);
    expect(events.length).toBe(2);
    expect(events[0].type).toBe('TenantCreated');
    expect(events[1].type).toBe('TenantUpdated');
  });

  it('should handle tenant status changes', async () => {
    // Arrange: create and save
    const tenant = TenantAggregate.create('Test Tenant', 'Test Description');
    const tenantId = tenant.id;
    await tenantRepository.save(tenant);
    
    // Act - load, update status and save
    const loadedTenant = await tenantRepository.findById(tenantId);
    expect(loadedTenant).toBeDefined();
    
    // Deactivate the tenant
    loadedTenant?.deactivate();
    await tenantRepository.save(loadedTenant!);
    
    // Load again to verify status
    const deactivatedTenant = await tenantRepository.findById(tenantId);
    expect(deactivatedTenant?.status).toBe('inactive');
    
    // Reactivate the tenant
    deactivatedTenant?.activate();
    await tenantRepository.save(deactivatedTenant!);
    
    // Load again to verify status
    const reactivatedTenant = await tenantRepository.findById(tenantId);
    
    // Assert
    expect(reactivatedTenant?.status).toBe('active');
    
    // Verify the events
    const events = await eventStore.loadEvents(tenantId);
    expect(events.length).toBe(3);
    expect(events[0].type).toBe('TenantCreated');
    expect(events[1].type).toBe('TenantDeactivated');
    expect(events[2].type).toBe('TenantActivated');
  });

  it('should return undefined for non-existent tenant', async () => {
    // Act
    const nonExistentTenant = await tenantRepository.findById('non-existent-id');
    
    // Assert
    expect(nonExistentTenant).toBeUndefined();
  });
}); 
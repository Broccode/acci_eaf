import { Test, TestingModule } from '@nestjs/testing';
import { MikroORM } from '@mikro-orm/core';
import { EntityManager } from '@mikro-orm/postgresql'; // Or your specific driver
import { InfrastructureModule } from '../lib/infrastructure.module';
import { TenancyModule, TenantContextService } from 'tenancy';
import { SampleTenantEntity } from '../lib/persistence/entities/sample-tenant.entity';

// Helper function to run code within a tenant context
async function runInTenantContext<T>(
  tenantContextService: TenantContextService,
  tenantId: string,
  fn: () => Promise<T>,
): Promise<T> {
  return tenantContextService.runWithTenant(tenantId, fn);
}

describe('TenantFilter Integration Test', () => {
  let module: TestingModule;
  let orm: MikroORM;
  let em: EntityManager;
  let tenantContextService: TenantContextService;

  const tenant1 = 'tenant-uuid-1';
  const tenant2 = 'tenant-uuid-2';

  beforeAll(async () => {
    module = await Test.createTestingModule({
      imports: [InfrastructureModule], // Imports TenancyModule and MikroOrmModule
    }).compile();

    orm = module.get(MikroORM);
    em = module.get(EntityManager);
    tenantContextService = module.get(TenantContextService);

    // Ensure clean database schema for tests
    const generator = orm.getSchemaGenerator();
    await generator.dropSchema();
    await generator.createSchema();
  });

  afterAll(async () => {
    await orm.close(true);
    await module.close();
  });

  // Clear data before each test within a transaction
  beforeEach(async () => {
    const generator = orm.getSchemaGenerator();
    // Using refreshDatabase() or similar MikroORM testing utils can also work
    await generator.clearDatabase();

    // Seed data OUTSIDE specific tenant context initially
    const emFork = em.fork();
    const entity1Tenant1 = new SampleTenantEntity('Entity 1 Tenant 1', tenant1);
    const entity2Tenant1 = new SampleTenantEntity('Entity 2 Tenant 1', tenant1);
    const entity1Tenant2 = new SampleTenantEntity('Entity 1 Tenant 2', tenant2);
    await emFork.persistAndFlush([entity1Tenant1, entity2Tenant1, entity1Tenant2]);
  });

  it('should only return entities for the current tenant context (tenant1)', async () => {
    const results = await runInTenantContext(tenantContextService, tenant1, async () => {
      // Use a new fork for the request scope simulation
      const scopedEm = em.fork(); 
      return scopedEm.find(SampleTenantEntity, {});
    });

    expect(results).toHaveLength(2);
    expect(results.map(r => r.name).sort()).toEqual([
      'Entity 1 Tenant 1',
      'Entity 2 Tenant 1',
    ].sort());
    results.forEach(r => expect(r.tenantId).toBe(tenant1));
  });

  it('should only return entities for the current tenant context (tenant2)', async () => {
    const results = await runInTenantContext(tenantContextService, tenant2, async () => {
      const scopedEm = em.fork();
      return scopedEm.find(SampleTenantEntity, {});
    });

    expect(results).toHaveLength(1);
    expect(results[0].name).toBe('Entity 1 Tenant 2');
    expect(results[0].tenantId).toBe(tenant2);
  });

  it('should return no entities if no tenant context is set', async () => {
    const scopedEm = em.fork();
    const results = await scopedEm.find(SampleTenantEntity, {});

    // The filter defaults to a blocking condition if no tenantId is found
    expect(results).toHaveLength(0);
  });

  it('should return all entities if the filter is explicitly disabled', async () => {
     // Run within a context, but disable the filter for the query
     const results = await runInTenantContext(tenantContextService, tenant1, async () => {
       const scopedEm = em.fork();
       return scopedEm.find(SampleTenantEntity, {}, { filters: { tenant: false } }); // Disable filter
     });

     expect(results).toHaveLength(3);
     // Check if we got entities from both tenants
     expect(results.some(r => r.tenantId === tenant1)).toBe(true);
     expect(results.some(r => r.tenantId === tenant2)).toBe(true);
  });
}); 
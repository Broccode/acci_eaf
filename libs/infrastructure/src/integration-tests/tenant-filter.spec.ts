import { MikroOrmTestHelper, testDbManager } from 'testing';
import { SampleTenantEntity } from '../lib/persistence/entities/sample-tenant.entity';
import { TenantContextService } from 'tenancy';
import { EntityManager, IDatabaseDriver, Connection } from '@mikro-orm/core';

// Helper function to run code within a tenant context
async function runInTenantContext<T>(
  tenantContextService: TenantContextService,
  tenantId: string,
  em: EntityManager<IDatabaseDriver<Connection>>,
  fn: (em: EntityManager<IDatabaseDriver<Connection>>) => Promise<T>,
): Promise<T> {
  await em.setFilterParams('tenant', { tenantId });
  return tenantContextService.runWithTenant(tenantId, () => fn(em));
}

describe('TenantFilter Integration Test (Testcontainers)', () => {
  let testHelper: MikroOrmTestHelper;
  let em: EntityManager<IDatabaseDriver<Connection>>;
  let tenantContextService: TenantContextService;

  const tenant1 = 'd1b1f1a1-1b1a-4b1a-8b1a-1a1b1a1b1a1a';
  const tenant2 = 'd2b2f2a2-2b2a-4b2a-8b2a-2a2b2a2b2a2a';

  beforeAll(async () => {
    testHelper = new MikroOrmTestHelper();
    await testHelper.setup();
    em = testHelper.getEntityManager();
    tenantContextService = new TenantContextService();
    // TenantFilter global initialisieren
    const { TenantFilterInitializer } = await import('../lib/persistence/filters/tenant.filter');
    new TenantFilterInitializer(tenantContextService);
    // eslint-disable-next-line no-console
    console.log('[Test] ORM and schema initialized (Testcontainers)');
  });

  afterAll(async () => {
    await testHelper.teardown();
    await testDbManager.stopDb();
  });

  beforeEach(async () => {
    // Datenbank leeren (Filter deaktivieren)
    await em.nativeDelete(SampleTenantEntity, {}, { filters: { tenant: false } });
    await em.flush();
    em.clear();
  });

  it('should only return entities for the current tenant context (tenant1)', async () => {
    // Arrange
    const entity1Tenant1 = em.create(SampleTenantEntity, { name: 'Entity 1 Tenant 1', tenantId: tenant1 });
    const entity2Tenant1 = em.create(SampleTenantEntity, { name: 'Entity 2 Tenant 1', tenantId: tenant1 });
    const entity1Tenant2 = em.create(SampleTenantEntity, { name: 'Entity 1 Tenant 2', tenantId: tenant2 });
    await em.persistAndFlush([entity1Tenant1, entity2Tenant1, entity1Tenant2]);
    em.clear();
    // eslint-disable-next-line no-console
    console.log('[Test] Running test for tenant1');
    const results = await runInTenantContext(tenantContextService, tenant1, em, async (em) => {
      return em.find(SampleTenantEntity, {});
    });
    expect(results).toHaveLength(2);
    expect(results.map(r => r.name).sort()).toEqual([
      'Entity 1 Tenant 1',
      'Entity 2 Tenant 1',
    ].sort());
    results.forEach(r => expect(r.tenantId).toBe(tenant1));
  });

  it('should only return entities for the current tenant context (tenant2)', async () => {
    // Arrange
    const entity1Tenant1 = em.create(SampleTenantEntity, { name: 'Entity 1 Tenant 1', tenantId: tenant1 });
    const entity2Tenant1 = em.create(SampleTenantEntity, { name: 'Entity 2 Tenant 1', tenantId: tenant1 });
    const entity1Tenant2 = em.create(SampleTenantEntity, { name: 'Entity 1 Tenant 2', tenantId: tenant2 });
    await em.persistAndFlush([entity1Tenant1, entity2Tenant1, entity1Tenant2]);
    em.clear();
    // eslint-disable-next-line no-console
    console.log('[Test] Running test for tenant2');
    const results = await runInTenantContext(tenantContextService, tenant2, em, async (em) => {
      return em.find(SampleTenantEntity, {});
    });
    expect(results).toHaveLength(1);
    expect(results[0].name).toBe('Entity 1 Tenant 2');
    expect(results[0].tenantId).toBe(tenant2);
  });

  it('should return no entities if no tenant context is set', async () => {
    // Arrange
    const entity1Tenant1 = em.create(SampleTenantEntity, { name: 'Entity 1 Tenant 1', tenantId: tenant1 });
    const entity2Tenant1 = em.create(SampleTenantEntity, { name: 'Entity 2 Tenant 1', tenantId: tenant1 });
    const entity1Tenant2 = em.create(SampleTenantEntity, { name: 'Entity 1 Tenant 2', tenantId: tenant2 });
    await em.persistAndFlush([entity1Tenant1, entity2Tenant1, entity1Tenant2]);
    em.clear();
    // eslint-disable-next-line no-console
    console.log('[Test] Running test for no tenant context');
    const results = await em.find(SampleTenantEntity, {});
    expect(results).toHaveLength(0);
  });

  it('should return all entities if the filter is explicitly disabled', async () => {
    // Arrange
    const entity1Tenant1 = em.create(SampleTenantEntity, { name: 'Entity 1 Tenant 1', tenantId: tenant1 });
    const entity2Tenant1 = em.create(SampleTenantEntity, { name: 'Entity 2 Tenant 1', tenantId: tenant1 });
    const entity1Tenant2 = em.create(SampleTenantEntity, { name: 'Entity 1 Tenant 2', tenantId: tenant2 });
    await em.persistAndFlush([entity1Tenant1, entity2Tenant1, entity1Tenant2]);
    em.clear();
    // eslint-disable-next-line no-console
    console.log('[Test] Running test for filter disabled');
    const results = await runInTenantContext(tenantContextService, tenant1, em, async (em) => {
      return em.find(SampleTenantEntity, {}, { filters: { tenant: false } });
    });
    expect(results).toHaveLength(3);
    expect(results.some(r => r.tenantId === tenant1)).toBe(true);
    expect(results.some(r => r.tenantId === tenant2)).toBe(true);
  });
}); 
import { MikroOrmTestHelper, testDbManager } from 'testing'; // Add testDbManager import
import { TenantContextService } from 'tenancy'; // Assumes 'tenancy' path mapping
import { SampleTenantEntity } from '../entities/sample-tenant.entity'; // Korrigierter Import
import { EntityManager, IDatabaseDriver, Connection } from '@mikro-orm/core';

// Describe the integration test suite for the TenantFilter
describe('TenantFilter Integration Test', () => {
  let testHelper: MikroOrmTestHelper;
  let em: EntityManager<IDatabaseDriver<Connection>>;
  let tenantContext: TenantContextService;

  // Tenant IDs for testing
  const tenant1 = 'd1b1f1a1-1b1a-4b1a-8b1a-1a1b1a1b1a1a';
  const tenant2 = 'd2b2f2a2-2b2a-4b2a-8b2a-2a2b2a2b2a2a';

  const JEST_TIMEOUT_MS = 60000; // 60 seconds

  // Setup: Initialize DB and ORM before all tests
  beforeAll(async () => {
    testHelper = new MikroOrmTestHelper();
    const orm = await testHelper.setup();
    em = testHelper.getEntityManager();
    // Stelle sicher, dass TenantContextService und TenantFilterInitializer korrekt initialisiert werden
    tenantContext = new TenantContextService();
    const { TenantFilterInitializer } = await import('./tenant.filter');
    new TenantFilterInitializer(tenantContext);
    // Debug-Log zur Kontrolle
    // eslint-disable-next-line no-console
    console.log('[Test] TenantContextService instance set:', typeof tenantContext.getTenantId === 'function');
    (global as any).__TENANT_CONTEXT_SERVICE_INSTANCE_SET__ = true;
  }, JEST_TIMEOUT_MS); // Increase timeout for beforeAll

  // Teardown: Close ORM connection after all tests
  afterAll(async () => {
    await testHelper.teardown();
    // Stop DB container explicitly - useful if running only this suite
    await testDbManager.stopDb(); 
  });

  // Cleanup: Clear the ExampleProject table before each test
  beforeEach(async () => {
    // Disable tenant filter for cleanup to delete across all tenants
    await em.nativeDelete(SampleTenantEntity, {}, { filters: { tenant: false } }); 
    await em.flush();
    em.clear(); // Clear identity map
    // Setze explizit die Filter-Parameter für MikroORM
    em.setFilterParams('tenant', {});
  });

  it('should only return entities for the current tenant context', async () => {
    // Arrange: Create entities for different tenants
    const project1_t1 = em.create(SampleTenantEntity, { tenantId: tenant1, name: 'Project 1 Tenant 1' });
    const project2_t1 = em.create(SampleTenantEntity, { tenantId: tenant1, name: 'Project 2 Tenant 1' });
    const project1_t2 = em.create(SampleTenantEntity, { tenantId: tenant2, name: 'Project 1 Tenant 2' });
    await em.persistAndFlush([project1_t1, project2_t1, project1_t2]);
    em.clear();

    // Act: Run query within tenant 1 context
    const resultsTenant1 = await tenantContext.runWithTenant(tenant1, async () => {
        return await em.find(SampleTenantEntity, {});
    });

    // Assert: Only tenant 1 entities should be returned
    expect(resultsTenant1).toHaveLength(2);
    expect(resultsTenant1.map(p => p.id).sort()).toEqual([project1_t1.id, project2_t1.id].sort());
    expect(resultsTenant1.every(p => p.tenantId === tenant1)).toBe(true);
  });

  it('should return an empty array when querying for a different tenant context', async () => {
    // Arrange: Create entity for tenant 1
    const project1_t1 = em.create(SampleTenantEntity, { tenantId: tenant1, name: 'Project 1 Tenant 1' });
    await em.persistAndFlush(project1_t1);
    em.clear();

    // Act: Run query within tenant 2 context
    const resultsTenant2 = await tenantContext.runWithTenant(tenant2, async () => {
        return await em.find(SampleTenantEntity, {});
    });

    // Assert: No entities should be returned
    expect(resultsTenant2).toHaveLength(0);
  });

  it('should return an empty array when querying without a tenant context', async () => {
    // Arrange: Create entities for tenant 1 and tenant 2
    const project1_t1 = em.create(SampleTenantEntity, { tenantId: tenant1, name: 'Project 1 Tenant 1' });
    const project1_t2 = em.create(SampleTenantEntity, { tenantId: tenant2, name: 'Project 1 Tenant 2' });
    await em.persistAndFlush([project1_t1, project1_t2]);
    em.clear();

    // Act: Run query without setting tenant context (filter should block)
    // Note: This assumes the TenantFilter defaults to blocking access without context.
    const resultsNoContext = await em.find(SampleTenantEntity, {});

    // Assert: No entities should be returned
    expect(resultsNoContext).toHaveLength(0);
  });

  it('should allow querying all entities if filter is disabled', async () => {
    // Arrange: Create entities for different tenants
    const project1_t1 = em.create(SampleTenantEntity, { tenantId: tenant1, name: 'Project 1 Tenant 1' });
    const project1_t2 = em.create(SampleTenantEntity, { tenantId: tenant2, name: 'Project 1 Tenant 2' });
    await em.persistAndFlush([project1_t1, project1_t2]);
    em.clear();

    // Act: Run query with the tenant filter explicitly disabled
    const resultsDisabled = await em.find(SampleTenantEntity, {}, { filters: { tenant: false } });
    // Alternative (if using args parameter): 
    // const resultsDisabled = await em.find(SampleTenantEntity, {}, { filters: { tenant: { disabled: true } } });

    // Assert: Both entities should be returned
    expect(resultsDisabled).toHaveLength(2);
    expect(resultsDisabled.map(p => p.id).sort()).toEqual([project1_t1.id, project1_t2.id].sort());
  });

}); 
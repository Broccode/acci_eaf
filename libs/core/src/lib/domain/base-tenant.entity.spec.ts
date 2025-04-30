import { BaseTenantEntity } from './base-tenant.entity';
import { v4 as uuidV4, validate as uuidValidate } from 'uuid';

class TestEntity extends BaseTenantEntity {}

describe('BaseTenantEntity', () => {
  it('should generate a valid uuid for id', () => {
    const entity = new TestEntity();
    expect(uuidValidate(entity.id)).toBe(true);
  });

  it('should allow setting tenantId', () => {
    const entity = new TestEntity();
    const tenantId = uuidV4();
    entity.tenantId = tenantId;
    expect(entity.tenantId).toBe(tenantId);
  });
}); 
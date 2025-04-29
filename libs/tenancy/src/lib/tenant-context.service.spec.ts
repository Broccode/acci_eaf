import { TenantContextService } from './tenant-context.service';

describe('TenantContextService', () => {
  let service: TenantContextService;

  beforeEach(() => {
    service = new TenantContextService();
  });

  it('should set and get tenantId within context', () => {
    const result = service.runWithTenant('tenant-123', () => {
      return service.getTenantId();
    });
    expect(result).toBe('tenant-123');
  });

  it('should return undefined if getTenantId is called outside context', () => {
    expect(service.getTenantId()).toBeUndefined();
  });

  it('should isolate tenantId between contexts', () => {
    let tenantA: string | undefined;
    let tenantB: string | undefined;
    service.runWithTenant('A', () => {
      tenantA = service.getTenantId();
      service.runWithTenant('B', () => {
        tenantB = service.getTenantId();
      });
    });
    expect(tenantA).toBe('A');
    expect(tenantB).toBe('B');
  });
}); 
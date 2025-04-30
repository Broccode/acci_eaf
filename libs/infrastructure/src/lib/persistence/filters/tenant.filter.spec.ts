import { TenantFilterInitializer } from './tenant.filter';

describe('TenantFilter unit tests', () => {
  beforeEach(() => {
    // Reset module state so each test starts fresh
    jest.resetModules();
  });

  it('should block access and log error when no context initialized', () => {
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    const { TenantFilter } = require('./tenant.filter');

    const result = TenantFilter.cond({}, 'read');

    expect(result).toEqual({ tenantId: '__NO_TENANT_CONTEXT__' });
    expect(consoleErrorSpy).toHaveBeenCalledWith('[TenantFilter] TenantContextService not initialized!');

    consoleErrorSpy.mockRestore();
  });

  it('should disable filter when args.disabled is true', () => {
    const { TenantFilter, TenantFilterInitializer } = require('./tenant.filter');
    const mockService = { getTenantId: () => 'ignored' };
    new TenantFilterInitializer(mockService as any);

    const result = TenantFilter.cond({ disabled: true }, 'read');
    expect(result).toEqual({});
  });

  it('should block access when tenantId is falsy', () => {
    const { TenantFilter, TenantFilterInitializer } = require('./tenant.filter');
    const mockService = { getTenantId: () => '' };
    new TenantFilterInitializer(mockService as any);

    const result = TenantFilter.cond({}, 'update');
    expect(result).toEqual({ tenantId: '00000000-0000-0000-0000-000000000000' });
  });

  it('should return correct filter condition when context provides a valid tenantId', () => {
    const { TenantFilter, TenantFilterInitializer } = require('./tenant.filter');
    const tenantId = 'abc-123';
    const mockService = { getTenantId: () => tenantId };
    new TenantFilterInitializer(mockService as any);

    const result = TenantFilter.cond({}, 'delete');
    expect(result).toEqual({ tenantId });
  });
}); 
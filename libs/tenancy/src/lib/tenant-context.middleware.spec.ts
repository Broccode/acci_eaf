import { TenantContextMiddleware } from './tenant-context.middleware';
import { TenantContextService } from './tenant-context.service';
import { Request, Response, NextFunction } from 'express';

describe('TenantContextMiddleware', () => {
  let middleware: TenantContextMiddleware;
  let service: TenantContextService;
  let req: any;
  let res: Partial<Response>;
  let next: jest.Mock;

  beforeEach(() => {
    service = { runWithTenant: jest.fn((tenantId, cb) => cb()) } as any;
    middleware = new TenantContextMiddleware(service);
    req = {};
    res = {};
    next = jest.fn();
  });

  it('should extract tenantId from user and call runWithTenant', () => {
    req.user = { tenant_id: 'user-tenant' };
    middleware.use(req as any, res as any, next);
    expect(service.runWithTenant).toHaveBeenCalledWith('user-tenant', expect.any(Function));
    expect(next).toHaveBeenCalled();
  });

  it('should extract tenantId from x-tenant-id header if not in user', () => {
    req.headers = { 'x-tenant-id': 'header-tenant' };
    middleware.use(req as any, res as any, next);
    expect(service.runWithTenant).toHaveBeenCalledWith('header-tenant', expect.any(Function));
    expect(next).toHaveBeenCalled();
  });

  it('should call next without tenant context if no tenantId found', () => {
    req.headers = {};
    middleware.use(req as any, res as any, next);
    expect(service.runWithTenant).not.toHaveBeenCalled();
    expect(next).toHaveBeenCalled();
  });
});

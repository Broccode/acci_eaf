import { TenantContextMiddleware } from './tenant-context.middleware';
import { TenantContextService } from './tenant-context.service';
import { Request, Response, NextFunction } from 'express';

describe('TenantContextMiddleware', () => {
  let service: TenantContextService;
  let middleware: TenantContextMiddleware;
  let req: any;
  let res: any;
  let next: jest.Mock;

  beforeEach(() => {
    service = new TenantContextService();
    middleware = new TenantContextMiddleware(service);
    next = jest.fn();
    res = {};
    req = { headers: {}, user: {} };
  });

  it('should run next within tenant context when req.user.tenant_id is present', () => {
    req.user = { tenant_id: 'tenant-123' };
    middleware.use(req, res, () => {
      expect(service.getTenantId()).toBe('tenant-123');
      next();
    });
    expect(next).toHaveBeenCalled();
  });

  it('should extract tenantId from header when user not present', () => {
    delete req.user;
    req.headers['x-tenant-id'] = 'header-456';
    middleware.use(req, res, () => {
      expect(service.getTenantId()).toBe('header-456');
      next();
    });
    expect(next).toHaveBeenCalled();
  });

  it('should proceed without context when no tenantId provided', () => {
    middleware.use(req, res, next);
    expect(service.getTenantId()).toBeUndefined();
    expect(next).toHaveBeenCalled();
  });
});

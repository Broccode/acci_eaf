import { Injectable, NestMiddleware } from '@nestjs/common';
import { Request, Response, NextFunction } from 'express';
import { TenantContextService } from './tenant-context.service';

// Define an interface for the expected user structure on the request
// This might need adjustment based on the actual Auth implementation
interface RequestWithUser extends Request {
  user?: {
    tenant_id?: string;
    // other user properties...
  };
}

@Injectable()
export class TenantContextMiddleware implements NestMiddleware {
  constructor(private readonly tenantContextService: TenantContextService) {}

  use(req: RequestWithUser, res: Response, next: NextFunction) {
    // 1. Try extracting tenant_id from JWT payload (assuming request.user is populated)
    let tenantId = req.user?.tenant_id;

    // 2. If not found in JWT, try extracting from 'X-Tenant-ID' header
    if (!tenantId) {
      const headerTenantId = req.headers['x-tenant-id'];
      if (typeof headerTenantId === 'string') {
        tenantId = headerTenantId;
      }
    }

    // 3. If a tenantId is found, run the rest of the request within its context
    if (tenantId) {
      this.tenantContextService.runWithTenant(tenantId, () => {
        // Attach tenantId to request object for potential easier access downstream?
        // (Optional, AsyncLocalStorage is the primary mechanism)
        // (req as any).tenantId = tenantId;
        next();
      });
    } else {
      // If no tenantId is found, proceed without tenant context.
      // Guards or specific route logic should handle cases requiring a tenant.
      next();
    }
  }
} 
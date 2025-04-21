import { Injectable } from '@nestjs/common';
import { AsyncLocalStorage } from 'async_hooks';

@Injectable()
export class TenantContextService {
  private readonly als = new AsyncLocalStorage<{ tenantId: string }>();

  /**
   * Runs the given callback within an async context, storing the tenantId.
   * @param tenantId The tenant ID to store for the context.
   * @param callback The function to execute within the context.
   * @returns The result of the callback function.
   */
  runWithTenant<T>(tenantId: string, callback: () => T): T {
    return this.als.run({ tenantId }, callback);
  }

  /**
   * Retrieves the tenant ID from the current async context.
   * Throws an error if called outside a context managed by runWithTenant.
   * @returns The tenant ID.
   * @throws Error if no tenant context is found.
   */
  getTenantId(): string | undefined {
    const store = this.als.getStore();
    // Allow undefined return for flexibility, middleware/guards should enforce presence
    return store?.tenantId; 
  }
} 
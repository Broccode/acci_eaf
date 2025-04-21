import { FilterQuery } from '@mikro-orm/core';
import { TenantContextService } from 'tenancy'; // Assuming 'tenancy' is mapped in tsconfig
import { Injectable } from '@nestjs/common';

// Define an interface for entities that are tenant-aware
interface TenantAware {
  tenantId: string;
}

// Make the context service statically accessible for the filter if needed
// This is a common pattern when DI is tricky in filter definitions
// Note: Consider if MikroORM's request context (`@RequestContext`) or
// async module loading can provide a cleaner DI solution.
let tenantContextServiceInstance: TenantContextService | undefined;

@Injectable()
export class TenantFilterInitializer {
  constructor(private readonly tenantContext: TenantContextService) {
    tenantContextServiceInstance = this.tenantContext;
  }
}

// Using 'any' temporarily due to uncertainty about the correct Filter definition type
// TODO: Revisit this type once MikroORM setup is finalized.
export const TenantFilter: any = {
  name: 'tenant',
  // TODO: Register this filter in the MikroORM configuration:
  // MikroOrmModule.forRoot({
  //   ...
  //   filters: { tenant: TenantFilter },
  //   ...
  // })
  cond: (args: Record<string, any>, type: 'read' | 'update' | 'delete'): FilterQuery<TenantAware> => {
    // `type` can be 'read' or 'update'/'delete' - might be useful later

    // Check if the entity actually has a tenantId property.
    // MikroORM filters are usually defined on a base entity or interface,
    // so we assume the entity conforms to TenantAware.
    if (!tenantContextServiceInstance) {
      console.error('[TenantFilter] TenantContextService not initialized!');
      // Fail safe: Deny access if context is missing during request
      // Using a condition that is unlikely to be true for a string ID.
      return { tenantId: '__NO_TENANT_CONTEXT__' };
    }

    const tenantId = tenantContextServiceInstance.getTenantId();

    // Allow disabling the filter explicitly for certain operations if args['disabled'] is true
    if (args['disabled'] === true) {
        return {}; // No filter condition
    }

    if (!tenantId) {
      // If no tenantId is in the context (e.g., system operation or error),
      // block access to tenant-specific data by default.
      // Specific operations might need to disable this filter explicitly using em.setFilterParams('tenant', false)
      // Returning a condition that likely won't match any valid tenantId
      return { tenantId: '__NO_TENANT_ACCESS__' };
    }

    // Apply the filter condition using the retrieved tenantId
    // args might contain parameters passed via em.setFilterParams, but we use the context service directly here.
    return { tenantId: tenantId };
  },
  default: true, // Apply this filter by default to entities potentially implementing TenantAware
  // Note: This filter will apply to *all* SELECT queries unless explicitly disabled.
  // Entities MUST have a `tenantId` property for this to work correctly.
}; 
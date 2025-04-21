import { PrimaryKey, Property, Index } from '@mikro-orm/core';
import { v4 } from 'uuid';
import { TenantAware } from 'shared';

/**
 * Abstract base class for all tenant-aware entities.
 * Includes a primary key (UUID) and the tenantId property.
 */
export abstract class BaseTenantEntity implements TenantAware {
  @PrimaryKey({ type: 'uuid' })
  id: string = v4();

  @Index() // Indexing tenantId is crucial for performance
  @Property({ type: 'uuid' }) // Assuming tenant IDs are UUIDs, adjust if different
  tenantId!: string; // Use definite assignment assertion (!) as it must be set by inheriting classes/services

  // Optionally, add common audit fields like createdAt, updatedAt
  // @Property()
  // createdAt: Date = new Date();
  //
  // @Property({ onUpdate: () => new Date() })
  // updatedAt: Date = new Date();
} 
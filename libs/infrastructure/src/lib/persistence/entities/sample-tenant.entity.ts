import { Entity, Property } from '@mikro-orm/core';
import { BaseTenantEntity } from 'libs/core/src/lib/domain/base-tenant.entity'; // Adjust path if needed

@Entity({ tableName: 'sample_tenant_entities' }) // Explicit table name
export class SampleTenantEntity extends BaseTenantEntity {
  @Property()
  name: string;

  // Constructor can be useful for creation
  constructor(name: string, tenantId: string) {
    super(); // Call BaseTenantEntity constructor if it exists/is needed
    this.name = name;
    this.tenantId = tenantId; // Set tenantId explicitly
  }
} 
import { Entity, Property } from '@mikro-orm/core';
import { BaseTenantEntity } from '../../../../../core/src/lib/domain/base-tenant.entity'; // Adjust path as needed

@Entity()
export class ExampleEntity extends BaseTenantEntity {
  @Property()
  name!: string;

  @Property({ nullable: true })
  description?: string;

  // tenantId is inherited from BaseTenantEntity
  // id is inherited from BaseTenantEntity
} 
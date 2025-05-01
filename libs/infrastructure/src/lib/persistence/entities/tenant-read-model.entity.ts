import { Entity, PrimaryKey, Property, Enum } from '@mikro-orm/core';
import { TenantStatus } from 'core';

/**
 * Read model for tenants
 * This is used for querying tenants efficiently
 */
@Entity({ tableName: 'tenant_read_models' })
export class TenantReadModel {
  @PrimaryKey({ type: 'uuid' })
  id!: string;

  @Property({ length: 100 })
  name!: string;

  @Property({ length: 500, nullable: true })
  description?: string;

  @Enum(() => TenantStatus)
  status!: TenantStatus;

  @Property({ type: 'json', nullable: true })
  configuration?: Record<string, unknown>;

  @Property({ length: 255, nullable: true })
  contactEmail?: string;

  @Property({ type: 'date' })
  createdAt!: Date;

  @Property({ type: 'date' })
  updatedAt!: Date;
} 
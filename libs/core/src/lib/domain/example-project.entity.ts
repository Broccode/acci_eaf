import { Entity, Property } from '@mikro-orm/core';
import { BaseTenantEntity } from './base-tenant.entity';

/**
 * EXAMPLE ENTITY
 * 
 * This class demonstrates how to create a tenant-aware entity by extending BaseTenantEntity.
 * It represents a simple project concept.
 * 
 * REMOVE or MODIFY this entity for your actual application domain.
 */
@Entity({ tableName: 'example_projects' }) // Explicit table name for clarity
export class ExampleProject extends BaseTenantEntity {
  @Property()
  name!: string;

  @Property({ type: 'text', nullable: true })
  description?: string;

  // tenantId is inherited from BaseTenantEntity

  /**
   * Example constructor (adapt as needed for your domain logic).
   * Ensures tenantId is set upon creation.
   */
  constructor(tenantId: string, name: string) {
    super(); // Call BaseTenantEntity constructor (although it does nothing currently)
    this.tenantId = tenantId;
    this.name = name;
  }
} 
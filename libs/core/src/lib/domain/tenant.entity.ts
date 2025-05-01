import { Entity, Property, PrimaryKey, Enum } from '@mikro-orm/core';
import { v4 as uuidv4 } from 'uuid';

/**
 * Enum for tenant status
 */
export enum TenantStatus {
  ACTIVE = 'active',
  INACTIVE = 'inactive',
  SUSPENDED = 'suspended',
}

/**
 * Tenant entity for the Control Plane API
 * This entity represents a tenant in the system and is used for tenant management
 * Note: This is NOT a tenant-aware entity itself as it's part of the Control Plane
 */
@Entity({ tableName: 'tenants' })
export class Tenant {
  @PrimaryKey({ type: 'uuid' })
  id: string = uuidv4();

  @Property({ length: 100 })
  name: string;

  @Property({ length: 500, nullable: true })
  description?: string;

  @Enum(() => TenantStatus)
  status: TenantStatus = TenantStatus.ACTIVE;

  @Property({ type: 'json', nullable: true })
  configuration?: Record<string, unknown>;

  @Property({ length: 255, nullable: true })
  contactEmail?: string;

  @Property({ type: 'date' })
  createdAt: Date = new Date();

  @Property({ type: 'date', onUpdate: () => new Date() })
  updatedAt: Date = new Date();

  constructor(
    name: string,
    description?: string,
    contactEmail?: string,
    configuration?: Record<string, unknown>
  ) {
    this.name = name;
    this.description = description;
    this.contactEmail = contactEmail;
    this.configuration = configuration;
  }

  /**
   * Activate the tenant
   */
  activate(): void {
    this.status = TenantStatus.ACTIVE;
  }

  /**
   * Deactivate the tenant
   */
  deactivate(): void {
    this.status = TenantStatus.INACTIVE;
  }

  /**
   * Suspend the tenant
   */
  suspend(): void {
    this.status = TenantStatus.SUSPENDED;
  }

  /**
   * Update tenant information
   */
  update(
    name?: string,
    description?: string,
    contactEmail?: string,
    configuration?: Record<string, unknown>
  ): void {
    if (name) this.name = name;
    if (description !== undefined) this.description = description;
    if (contactEmail !== undefined) this.contactEmail = contactEmail;
    if (configuration !== undefined) this.configuration = configuration;
  }
} 
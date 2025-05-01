import { Entity, PrimaryKey, Property, Enum } from '@mikro-orm/core';
import { v4 as uuidv4 } from 'uuid';

export enum TenantStatus {
  ACTIVE = 'active',
  INACTIVE = 'inactive',
  SUSPENDED = 'suspended',
}

@Entity()
export class Tenant {
  @PrimaryKey()
  id: string = uuidv4();

  @Property()
  name: string;

  @Property({ nullable: true })
  description?: string;

  @Property({ nullable: true })
  contactEmail?: string;

  @Property({ type: 'json', nullable: true })
  configuration?: Record<string, unknown>;

  @Enum(() => TenantStatus)
  status: TenantStatus = TenantStatus.ACTIVE;

  @Property()
  createdAt: Date = new Date();

  @Property({ onUpdate: () => new Date() })
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
} 
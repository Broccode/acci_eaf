import { Entity, PrimaryKey, Property } from '@mikro-orm/core';
import { v4 as uuid } from 'uuid';

@Entity()
export class AuditLog {
  @PrimaryKey()
  id: string = uuid();

  @Property()
  timestamp: Date = new Date();

  @Property()
  actorId!: string;

  @Property()
  action!: string;

  @Property()
  resource!: string;

  @Property()
  resourceId!: string;

  @Property({ type: 'json', nullable: true })
  payload?: Record<string, unknown>;
} 
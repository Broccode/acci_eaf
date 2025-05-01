import { Entity, PrimaryKey, Property } from '@mikro-orm/core';
import { v4 as uuidv4 } from 'uuid';

/**
 * MikroORM entity for storing domain events
 */
@Entity({ tableName: 'events' })
export class EventRecord {
  @PrimaryKey({ type: 'uuid' })
  id: string = uuidv4();

  /**
   * The ID of the aggregate that emitted the event
   */
  @Property({ type: 'uuid' })
  streamId!: string;

  /**
   * The version of the event in the stream (0-indexed)
   */
  @Property()
  version!: number;

  /**
   * The type of the event, including its schema version
   * Format: EventName.vX (e.g., TenantCreated.v1)
   */
  @Property({ length: 100 })
  eventType!: string;

  /**
   * The event payload as a JSON object
   */
  @Property({ type: 'jsonb' })
  payload!: any;

  /**
   * When the event occurred
   */
  @Property({ type: 'date' })
  timestamp!: Date;

  /**
   * Optional tenant ID for tenant-specific events
   */
  @Property({ type: 'uuid', nullable: true })
  tenantId?: string;

  /**
   * Optional metadata for the event
   */
  @Property({ type: 'jsonb', nullable: true })
  metadata?: Record<string, unknown>;
} 
import { Entity, PrimaryKey, Property, Unique } from '@mikro-orm/core';
import { v4 as uuidv4 } from 'uuid';

/**
 * Entity to track processed events for ensuring idempotency
 */
@Entity({ tableName: 'processed_events' })
@Unique({ properties: ['eventId', 'processorId'] })
export class ProcessedEventRecord {
  @PrimaryKey({ type: 'uuid' })
  id: string = uuidv4();

  /**
   * The ID of the processed event
   */
  @Property({ type: 'uuid' })
  eventId!: string;

  /**
   * The ID of the processor that handled the event
   */
  @Property({ length: 100 })
  processorId!: string;

  /**
   * When the event was processed
   */
  @Property({ type: 'date' })
  processedAt: Date = new Date();

  /**
   * Optional metadata about the processing
   */
  @Property({ type: 'jsonb', nullable: true })
  metadata?: Record<string, unknown>;
} 
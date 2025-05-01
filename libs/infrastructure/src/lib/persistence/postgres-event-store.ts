import { EntityManager } from '@mikro-orm/core';
import { Injectable } from '@nestjs/common';
import { DomainEvent, EventStore } from 'core';
import { EventRecord } from './entities/event-record.entity';

/**
 * PostgreSQL implementation of the EventStore using MikroORM
 */
@Injectable()
export class PostgresEventStore implements EventStore {
  constructor(private readonly em: EntityManager) {}

  /**
   * Save a single event to the store
   */
  async save(event: DomainEvent): Promise<void> {
    const eventRecord = this.createEventRecord(event);
    await this.em.persistAndFlush(eventRecord);
  }

  /**
   * Save multiple events to the store in a transaction
   */
  async saveMany(events: DomainEvent[]): Promise<void> {
    const eventRecords = events.map(event => this.createEventRecord(event));
    
    await this.em.transactional(async (em) => {
      for (const record of eventRecords) {
        em.persist(record);
      }
      await em.flush();
    });
  }

  /**
   * Load all events for a specific aggregate
   */
  async loadEvents(aggregateId: string): Promise<DomainEvent[]> {
    const eventRecords = await this.em.find(
      EventRecord,
      { streamId: aggregateId },
      { orderBy: { version: 'ASC' } }
    );

    return eventRecords.map(record => this.mapToDomainEvent(record));
  }

  /**
   * Get the next version number for an aggregate
   */
  async getNextVersion(aggregateId: string): Promise<number> {
    const result = await this.em.find(
      EventRecord,
      { streamId: aggregateId },
      { orderBy: { version: 'DESC' }, limit: 1 }
    );

    return result.length > 0 ? result[0].version + 1 : 0;
  }

  /**
   * Create an EventRecord entity from a DomainEvent
   */
  private createEventRecord(event: DomainEvent): EventRecord {
    const record = new EventRecord();
    record.version = event.version;
    record.streamId = event.aggregateId;
    record.eventType = `${event.type}.v${event.version}`;
    record.payload = event.payload;
    record.timestamp = event.occurredAt;
    record.metadata = event.metadata;
    
    // Set tenant ID if available in metadata
    if (event.metadata && event.metadata['tenantId']) {
      record.tenantId = event.metadata['tenantId'] as string;
    }
    
    return record;
  }

  /**
   * Map an EventRecord entity to a DomainEvent
   */
  private mapToDomainEvent(record: EventRecord): DomainEvent {
    // Extract event type and version from the stored event type string
    const [type, versionStr] = record.eventType.split('.v');
    const version = parseInt(versionStr, 10);

    return {
      id: record.id,
      type,
      version,
      aggregateId: record.streamId,
      occurredAt: record.timestamp,
      payload: record.payload,
      metadata: record.metadata
    };
  }
} 
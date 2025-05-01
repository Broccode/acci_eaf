import { EntityManager } from '@mikro-orm/core';
import { Injectable } from '@nestjs/common';
import { IdempotencyRecord, IdempotencyStore } from 'core';
import { ProcessedEventRecord } from '../persistence/entities/processed-event.entity';

/**
 * PostgreSQL implementation of the IdempotencyStore using MikroORM
 */
@Injectable()
export class PostgresIdempotencyStore implements IdempotencyStore {
  constructor(private readonly em: EntityManager) {}

  /**
   * Mark an event as processed by a specific processor
   */
  async markAsProcessed(
    eventId: string,
    processorId: string,
    metadata?: Record<string, unknown>
  ): Promise<void> {
    const record = new ProcessedEventRecord();
    record.eventId = eventId;
    record.processorId = processorId;
    record.processedAt = new Date();
    
    if (metadata) {
      record.metadata = metadata;
    }

    await this.em.persistAndFlush(record);
  }

  /**
   * Check if an event has already been processed by a specific processor
   */
  async hasBeenProcessed(eventId: string, processorId: string): Promise<boolean> {
    const count = await this.em.count(ProcessedEventRecord, {
      eventId,
      processorId
    });

    return count > 0;
  }

  /**
   * Get the processing record for an event
   */
  async getProcessingRecord(
    eventId: string,
    processorId: string
  ): Promise<IdempotencyRecord | undefined> {
    const record = await this.em.findOne(ProcessedEventRecord, {
      eventId,
      processorId
    });

    if (!record) {
      return undefined;
    }

    return {
      eventId: record.eventId,
      processorId: record.processorId,
      processedAt: record.processedAt,
      metadata: record.metadata
    };
  }
} 
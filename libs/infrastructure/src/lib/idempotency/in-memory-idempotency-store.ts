import { IdempotencyRecord, IdempotencyStore } from '../../../../core/src/lib/cqrs/idempotency';

/**
 * Simple in-memory implementation of IdempotencyStore
 * For production use, this should be replaced with a persistent store
 */
export class InMemoryIdempotencyStore implements IdempotencyStore {
  private records: Map<string, IdempotencyRecord> = new Map();

  /**
   * Create a unique key for event+processor combination
   */
  private createKey(eventId: string, processorId: string): string {
    return `${eventId}:${processorId}`;
  }

  /**
   * Store a record that the event has been processed
   * @param eventId Unique identifier of the event
   * @param processorId Identifier of the processor that handled the event
   * @param metadata Optional metadata about the processing
   */
  async markAsProcessed(
    eventId: string,
    processorId: string,
    metadata?: Record<string, unknown>
  ): Promise<void> {
    const key = this.createKey(eventId, processorId);
    this.records.set(key, {
      eventId,
      processorId,
      processedAt: new Date(),
      metadata
    });
  }

  /**
   * Check if an event has already been processed by a specific processor
   * @param eventId Unique identifier of the event
   * @param processorId Identifier of the processor
   * @returns True if the event has already been processed
   */
  async hasBeenProcessed(eventId: string, processorId: string): Promise<boolean> {
    const key = this.createKey(eventId, processorId);
    return this.records.has(key);
  }

  /**
   * Get the processing record for an event by a specific processor
   * @param eventId Unique identifier of the event
   * @param processorId Identifier of the processor
   * @returns The processing record if found, undefined otherwise
   */
  async getProcessingRecord(
    eventId: string,
    processorId: string
  ): Promise<IdempotencyRecord | undefined> {
    const key = this.createKey(eventId, processorId);
    return this.records.get(key);
  }

  /**
   * Clear all records (useful for testing)
   */
  clear(): void {
    this.records.clear();
  }
} 
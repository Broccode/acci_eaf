/**
 * Record for tracking processed events
 */
export interface IdempotencyRecord {
  /**
   * Unique identifier of the event
   */
  eventId: string;

  /**
   * Identifier of the processor that handled the event
   */
  processorId: string;

  /**
   * When the event was processed
   */
  processedAt: Date;

  /**
   * Optional metadata about the processing
   */
  metadata?: Record<string, unknown>;
}

/**
 * Interface for tracking processed events to ensure idempotency
 */
export interface IdempotencyStore {
  /**
   * Store a record that the event has been processed
   * @param eventId Unique identifier of the event
   * @param processorId Identifier of the processor that handled the event
   * @param metadata Optional metadata about the processing
   */
  markAsProcessed(
    eventId: string,
    processorId: string,
    metadata?: Record<string, unknown>
  ): Promise<void>;

  /**
   * Check if an event has already been processed by a specific processor
   * @param eventId Unique identifier of the event
   * @param processorId Identifier of the processor
   * @returns True if the event has already been processed
   */
  hasBeenProcessed(eventId: string, processorId: string): Promise<boolean>;

  /**
   * Get the processing record for an event by a specific processor
   * @param eventId Unique identifier of the event
   * @param processorId Identifier of the processor
   * @returns The processing record if found, undefined otherwise
   */
  getProcessingRecord(
    eventId: string,
    processorId: string
  ): Promise<IdempotencyRecord | undefined>;
} 
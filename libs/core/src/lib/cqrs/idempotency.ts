import { DomainEvent } from '../domain/domain-event';

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
 * Processes an event with idempotency guarantees
 * @param event The event to process
 * @param idempotencyStore The store for tracking processed events
 * @param processorId Identifier of the processor handling the event
 * @param processor Function that processes the event
 * @returns The result of processing the event
 */
export async function processEventIdempotently<TEvent extends DomainEvent, TResult>(
  event: TEvent,
  idempotencyStore: IdempotencyStore,
  processorId: string,
  processor: (event: TEvent) => Promise<TResult>
): Promise<TResult> {
  // Check if this event has already been processed
  const processed = await idempotencyStore.hasBeenProcessed(event.id, processorId);
  
  if (processed) {
    throw new Error(`Event ${event.id} has already been processed by ${processorId}`);
  }
  
  // Process the event
  const result = await processor(event);
  
  // Mark the event as processed
  await idempotencyStore.markAsProcessed(event.id, processorId, {
    processedAt: new Date(),
    resultType: typeof result
  });
  
  return result;
} 
import { DomainEvent } from '../domain/domain-event';

/**
 * Interface for the event store
 */
export interface EventStore {
  /**
   * Save an event to the store
   * @param event The event to save
   */
  save(event: DomainEvent): Promise<void>;

  /**
   * Save multiple events to the store
   * @param events The events to save
   */
  saveMany(events: DomainEvent[]): Promise<void>;

  /**
   * Load events for a specific aggregate
   * @param aggregateId The aggregate ID
   * @returns Array of domain events
   */
  loadEvents(aggregateId: string): Promise<DomainEvent[]>;

  /**
   * Get the next version number for an aggregate
   * @param aggregateId The aggregate ID
   */
  getNextVersion(aggregateId: string): Promise<number>;
} 
import { EventStore } from '../../../core/src/lib/ports/event-store.interface';
import { DomainEvent } from '../../../core/src/lib/domain/domain-event';

export class InMemoryEventStore implements EventStore {
  private streams = new Map<string, DomainEvent[]>();
  // A tracking table could be added here for idempotency

  // Save a single event to the store
  async save(event: DomainEvent): Promise<void> {
    await this.saveMany([event]);
  }

  // Save multiple events to the store
  async saveMany(events: DomainEvent[]): Promise<void> {
    for (const event of events) {
      const streamId = event.aggregateId;
      if (!this.streams.has(streamId)) {
        this.streams.set(streamId, []);
      }
      this.streams.get(streamId)!.push(event);
    }
  }

  // Load all events for a specific aggregate
  async loadEvents(aggregateId: string): Promise<DomainEvent[]> {
    return this.streams.get(aggregateId) || [];
  }

  // Get the next version number for an aggregate
  async getNextVersion(aggregateId: string): Promise<number> {
    const events = this.streams.get(aggregateId);
    return events ? events.length : 0;
  }

  // Deprecated alias: appendToStream -> implemented by saveMany
  async appendToStream(streamId: string, events: any[]): Promise<void> {
    // Cast events to DomainEvent for internal storage
    await this.saveMany(events as DomainEvent[]);
  }

  // Deprecated alias: readStream -> implemented by loadEvents
  async readStream(streamId: string): Promise<DomainEvent[]> {
    return this.loadEvents(streamId);
  }
} 
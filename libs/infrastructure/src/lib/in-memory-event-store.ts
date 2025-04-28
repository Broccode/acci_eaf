import { EventStore } from '../../../core/src/lib/ports/event-store.interface';

export class InMemoryEventStore implements EventStore {
  private streams = new Map<string, any[]>();
  // A tracking table could be added here for idempotency

  async appendToStream(streamId: string, events: any[]): Promise<void> {
    if (!this.streams.has(streamId)) {
      this.streams.set(streamId, []);
    }
    // Event evolution/upcasting could be applied here during writing/reading
    this.streams.get(streamId)!.push(...events);
  }

  async readStream(streamId: string): Promise<any[]> {
    // Upcasting/versioning could be applied here
    return this.streams.get(streamId) || [];
  }

  // Concept: Methods for event schema evolution (upcasting) and idempotency can be added
} 
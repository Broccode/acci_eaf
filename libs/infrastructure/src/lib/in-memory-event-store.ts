import { EventStore } from '../../../core/src/lib/ports/event-store.interface';

export class InMemoryEventStore implements EventStore {
  private streams = new Map<string, any[]>();
  // Für Idempotency könnte hier eine Tracking-Tabelle ergänzt werden

  async appendToStream(streamId: string, events: any[]): Promise<void> {
    if (!this.streams.has(streamId)) {
      this.streams.set(streamId, []);
    }
    // Event Evolution/Upcasting könnte hier beim Schreiben/Lesen erfolgen
    this.streams.get(streamId)!.push(...events);
  }

  async readStream(streamId: string): Promise<any[]> {
    // Hier könnte Upcasting/Versionierung angewendet werden
    return this.streams.get(streamId) || [];
  }

  // Konzept: Methoden für Event-Schema-Evolution (Upcasting) und Idempotency können ergänzt werden
} 
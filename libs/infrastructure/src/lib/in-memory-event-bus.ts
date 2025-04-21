import { EventBus } from '../../../core/src/lib/ports/event-bus.interface';

export class InMemoryEventBus implements EventBus {
  private handlers = new Map<string, Array<(event: any) => Promise<void>>>();

  register<TEvent>(type: string, handler: (event: TEvent) => Promise<void>): void {
    if (!this.handlers.has(type)) {
      this.handlers.set(type, []);
    }
    this.handlers.get(type)!.push(handler);
  }

  async publish<TEvent extends { type: string }>(event: TEvent): Promise<void> {
    const handlers = this.handlers.get(event.type) || [];
    await Promise.all(handlers.map(h => h(event)));
  }

  async publishAll<TEvent extends { type: string }>(events: TEvent[]): Promise<void> {
    for (const event of events) {
      await this.publish(event);
    }
  }
} 
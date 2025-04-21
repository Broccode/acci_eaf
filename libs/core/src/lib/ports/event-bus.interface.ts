export interface EventBus<TEvent = any> {
  publish(event: TEvent): Promise<void>;
  publishAll?(events: TEvent[]): Promise<void>;
} 
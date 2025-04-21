export interface EventStore<TEvent = any, TStreamId = string> {
  appendToStream(streamId: TStreamId, events: TEvent[]): Promise<void>;
  readStream(streamId: TStreamId): Promise<TEvent[]>;
  // Für Event-Schema-Evolution und Idempotenz können hier weitere Methoden ergänzt werden
} 
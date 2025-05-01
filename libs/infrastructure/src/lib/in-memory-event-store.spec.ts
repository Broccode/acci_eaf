import { InMemoryEventStore } from './in-memory-event-store';

describe('InMemoryEventStore', () => {
  it('should append and read events from a stream', async () => {
    const store = new InMemoryEventStore();
    await store.appendToStream('stream-1', [{ id: '1', aggregateId: 'stream-1', type: 'TestEvent', version: 1, occurredAt: new Date(), payload: {} }]);
    const events = await store.readStream('stream-1');
    expect(events).toHaveLength(1);
    expect(events[0].type).toBe('TestEvent');
  });

  it('should return empty array for unknown stream', async () => {
    const store = new InMemoryEventStore();
    const events = await store.readStream('unknown-stream');
    expect(events).toEqual([]);
  });
}); 
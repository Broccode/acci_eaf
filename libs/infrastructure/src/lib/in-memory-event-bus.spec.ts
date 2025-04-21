import { InMemoryEventBus } from './in-memory-event-bus';

describe('InMemoryEventBus', () => {
  it('should call registered event handlers on publish', async () => {
    const bus = new InMemoryEventBus();
    const handled: any[] = [];
    bus.register('TestEvent', async (event: any): Promise<void> => { handled.push(event.payload); });
    await bus.publish({ type: 'TestEvent', payload: 42 });
    expect(handled).toEqual([42]);
  });

  it('should not fail if no handler registered', async () => {
    const bus = new InMemoryEventBus();
    await expect(bus.publish({ type: 'UnknownEvent' })).resolves.toBeUndefined();
  });

  it('should call handlers for all events in publishAll', async () => {
    const bus = new InMemoryEventBus();
    const handled: any[] = [];
    bus.register('EventA', async (event: any): Promise<void> => { handled.push(event.payload); });
    bus.register('EventB', async (event: any): Promise<void> => { handled.push(event.payload); });

    await bus.publishAll([
      { type: 'EventA', payload: 1 },
      { type: 'EventB', payload: 2 },
    ]);
    expect(handled).toEqual([1, 2]);
  });

  it('should not fail when publishAll is called with empty array', async () => {
    const bus = new InMemoryEventBus();
    await expect(bus.publishAll([])).resolves.toBeUndefined();
  });
}); 
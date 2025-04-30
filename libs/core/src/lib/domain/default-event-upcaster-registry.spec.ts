import { DefaultEventUpcasterRegistry } from './default-event-upcaster-registry';
import { DomainEvent } from './domain-event';

describe('DefaultEventUpcasterRegistry', () => {
  let registry: DefaultEventUpcasterRegistry;
  const baseEvent: DomainEvent = {
    id: 'test-id',
    type: 'TestEvent',
    version: 1,
    occurredAt: new Date(),
    aggregateId: 'agg-1',
    payload: { foo: 'bar' },
  };

  beforeEach(() => {
    registry = new DefaultEventUpcasterRegistry();
  });

  it('returns the original event when no upcasters registered', () => {
    const result = registry.upcast({ ...baseEvent });
    expect(result).toEqual(baseEvent);
  });

  it('registers and applies a single upcaster', () => {
    const upcaster = (evt: DomainEvent): DomainEvent => ({
      ...evt,
      payload: { ...(evt.payload as any), newField: 42 },
    });
    registry.register('TestEvent', 1, 2, upcaster);
    const result = registry.upcast({ ...baseEvent });

    expect(result.version).toBe(2);
    expect((result.payload as any).newField).toBe(42);
    expect(result.metadata?.originalVersion).toBe(1);
  });

  it('applies multiple sequential upcasters', () => {
    const upcaster1 = (evt: DomainEvent): DomainEvent => ({
      ...evt,
      payload: { ...(evt.payload as any), step1: true },
    });
    const upcaster2 = (evt: DomainEvent): DomainEvent => ({
      ...evt,
      payload: { ...(evt.payload as any), step2: true },
    });
    registry.register('TestEvent', 1, 2, upcaster1);
    registry.register('TestEvent', 2, 3, upcaster2);
    const result = registry.upcast({ ...baseEvent });

    expect(result.version).toBe(3);
    const payload = result.payload as any;
    expect(payload.step1).toBe(true);
    expect(payload.step2).toBe(true);
    expect(result.metadata?.originalVersion).toBe(1);
  });

  describe('upcastToVersion', () => {
    it('returns original if version >= target', () => {
      const event = { ...baseEvent, version: 3 };
      const result = registry.upcastToVersion(event, 2);
      expect(result).toEqual(event);
    });

    it('upcasts to a specific intermediate version', () => {
      const upcaster1 = (evt: DomainEvent): DomainEvent => ({ ...evt });
      const upcaster2 = (evt: DomainEvent): DomainEvent => ({ ...evt });
      registry.register('TestEvent', 1, 2, upcaster1);
      registry.register('TestEvent', 2, 3, upcaster2);
      const result2 = registry.upcastToVersion({ ...baseEvent }, 2);

      expect(result2.version).toBe(2);
      expect(result2.metadata?.originalVersion).toBe(1);
    });

    it('does not exceed target version', () => {
      const upcaster1 = (evt: DomainEvent): DomainEvent => ({ ...evt });
      const upcaster2 = (evt: DomainEvent): DomainEvent => ({ ...evt });
      registry.register('TestEvent', 1, 2, upcaster1);
      registry.register('TestEvent', 2, 4, upcaster2);
      const result = registry.upcastToVersion({ ...baseEvent }, 3);
      expect(result.version).toBe(2);
    });
  });
}); 
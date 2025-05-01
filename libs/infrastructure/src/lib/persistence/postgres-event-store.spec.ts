import { EntityManager } from '@mikro-orm/core';
import { PostgresEventStore } from './postgres-event-store';
import { EventRecord } from './entities/event-record.entity';
import { DomainEvent } from 'core';

describe('PostgresEventStore', () => {
  let em: jest.Mocked<EntityManager>;
  let store: PostgresEventStore;

  beforeEach(() => {
    em = {
      persistAndFlush: jest.fn(),
      transactional: jest.fn(),
      find: jest.fn(),
    } as unknown as jest.Mocked<EntityManager>;
    store = new PostgresEventStore(em);
  });

  it('saves a single event', async () => {
    const occurredAt = new Date();
    const event: DomainEvent = {
      id: '1',
      type: 'TestEvent',
      version: 1,
      aggregateId: 'agg1',
      occurredAt,
      payload: { foo: 'bar' },
      metadata: { key: 'value' },
    };
    await store.save(event);

    expect(em.persistAndFlush).toHaveBeenCalledTimes(1);
    const record = (em.persistAndFlush as jest.Mock).mock.calls[0][0] as EventRecord;
    expect(record.streamId).toBe('agg1');
    expect(record.version).toBe(1);
    expect(record.eventType).toBe('TestEvent.v1');
    expect(record.payload).toEqual({ foo: 'bar' });
    expect(record.metadata).toEqual({ key: 'value' });
    expect(record.timestamp).toBe(occurredAt);
  });

  it('saves multiple events in a transaction', async () => {
    const events: DomainEvent[] = [
      { id: '1', type: 'E1', version: 1, aggregateId: 'a', occurredAt: new Date(), payload: {}, metadata: {} },
      { id: '2', type: 'E2', version: 2, aggregateId: 'a', occurredAt: new Date(), payload: {}, metadata: {} },
    ];
    const mockEm = { persist: jest.fn(), flush: jest.fn() };
    (em.transactional as jest.Mock).mockImplementation(async (cb) => {
      await cb(mockEm as any);
    });

    await store.saveMany(events);

    expect(em.transactional).toHaveBeenCalledTimes(1);
    expect(mockEm.persist).toHaveBeenCalledTimes(2);
    expect(mockEm.flush).toHaveBeenCalledTimes(1);
  });

  it('loads events and maps to DomainEvent', async () => {
    const now = new Date();
    const record1 = new EventRecord();
    record1.id = '1';
    record1.streamId = 'agg1';
    record1.eventType = 'E1.v1';
    record1.payload = { a: 1 };
    record1.timestamp = now;
    record1.metadata = { m: 'v' };

    (em.find as jest.Mock).mockResolvedValue([record1]);
    const result = await store.loadEvents('agg1');

    expect(em.find).toHaveBeenCalledWith(
      EventRecord,
      { streamId: 'agg1' },
      { orderBy: { version: 'ASC' } }
    );
    expect(result).toEqual([
      {
        id: '1',
        type: 'E1',
        version: 1,
        aggregateId: 'agg1',
        occurredAt: now,
        payload: { a: 1 },
        metadata: { m: 'v' },
      }
    ]);
  });

  it('getNextVersion returns 0 if no events', async () => {
    (em.find as jest.Mock).mockResolvedValue([]);
    const next = await store.getNextVersion('agg1');

    expect(em.find).toHaveBeenCalledWith(
      EventRecord,
      { streamId: 'agg1' },
      { orderBy: { version: 'DESC' }, limit: 1 }
    );
    expect(next).toBe(0);
  });

  it('getNextVersion returns last version + 1', async () => {
    const rec = { version: 5 } as EventRecord;
    (em.find as jest.Mock).mockResolvedValue([rec]);
    const next = await store.getNextVersion('agg1');
    expect(next).toBe(6);
  });
}); 
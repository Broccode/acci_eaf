import { processEventIdempotently } from './idempotency';
import { DomainEvent } from '../domain/domain-event';
import { IdempotencyStore } from './idempotency';

describe('processEventIdempotently', () => {
  const baseEvent: DomainEvent = {
    id: 'evt-1',
    type: 'TestEvent',
    version: 1,
    occurredAt: new Date(),
    aggregateId: 'agg-1',
    payload: { key: 'value' },
  };

  it('should process event when not already processed', async () => {
    const store: IdempotencyStore = {
      hasBeenProcessed: jest.fn().mockResolvedValue(false),
      markAsProcessed: jest.fn().mockResolvedValue(undefined),
      getProcessingRecord: jest.fn(),
    };
    const processor = jest.fn().mockResolvedValue('processed-result');

    const result = await processEventIdempotently(baseEvent, store, 'proc-1', processor);

    expect(processor).toHaveBeenCalledWith(baseEvent);
    expect(store.hasBeenProcessed).toHaveBeenCalledWith(baseEvent.id, 'proc-1');
    expect(store.markAsProcessed).toHaveBeenCalledWith(
      baseEvent.id,
      'proc-1',
      expect.objectContaining({ resultType: 'string' })
    );
    expect(result).toBe('processed-result');
  });

  it('should throw error when event has been processed already', async () => {
    const store: IdempotencyStore = {
      hasBeenProcessed: jest.fn().mockResolvedValue(true),
      markAsProcessed: jest.fn(),
      getProcessingRecord: jest.fn(),
    };
    const processor = jest.fn();

    await expect(
      processEventIdempotently(baseEvent, store, 'proc-2', processor)
    ).rejects.toThrow(
      `Event ${baseEvent.id} has already been processed by proc-2`
    );

    expect(store.hasBeenProcessed).toHaveBeenCalledWith(baseEvent.id, 'proc-2');
    expect(store.markAsProcessed).not.toHaveBeenCalled();
    expect(processor).not.toHaveBeenCalled();
  });
}); 
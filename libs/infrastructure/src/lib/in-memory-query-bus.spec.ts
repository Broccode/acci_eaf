import { InMemoryQueryBus } from './in-memory-query-bus';

describe('InMemoryQueryBus', () => {
  it('should execute registered query handler', async () => {
    const bus = new InMemoryQueryBus();
    bus.register('TestQuery', async (query: any) => `result: ${query.payload}`);
    const result = await bus.execute({ type: 'TestQuery', payload: 'bar' });
    expect(result).toBe('result: bar');
  });

  it('should throw if no handler registered', async () => {
    const bus = new InMemoryQueryBus();
    await expect(bus.execute({ type: 'UnknownQuery' })).rejects.toThrow();
  });
}); 
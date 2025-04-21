import { InMemoryCommandBus } from './in-memory-command-bus';

describe('InMemoryCommandBus', () => {
  it('should execute registered command handler', async () => {
    const bus = new InMemoryCommandBus();
    bus.register('TestCommand', async (cmd: any) => `handled: ${cmd.payload}`);
    const result = await bus.execute({ type: 'TestCommand', payload: 'foo' });
    expect(result).toBe('handled: foo');
  });

  it('should throw if no handler registered', async () => {
    const bus = new InMemoryCommandBus();
    await expect(bus.execute({ type: 'UnknownCommand' })).rejects.toThrow();
  });
}); 
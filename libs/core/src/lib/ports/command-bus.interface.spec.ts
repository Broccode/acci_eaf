import { CommandBus } from './command-bus.interface';

describe('CommandBus', () => {
  it('should define an execute method', () => {
    const bus: CommandBus = {
      execute: jest.fn(async (command) => 'result'),
    };
    expect(typeof bus.execute).toBe('function');
  });
}); 
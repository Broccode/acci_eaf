import { CommandBus } from '../../../core/src/lib/ports/command-bus.interface';

export class InMemoryCommandBus implements CommandBus {
  private handlers = new Map<string, (command: any) => Promise<any>>();

  register<TCommand>(type: string, handler: (command: TCommand) => Promise<any>): void {
    this.handlers.set(type, handler);
  }

  async execute<TCommand, TResult = any>(command: TCommand & { type: string }): Promise<TResult> {
    const handler = this.handlers.get(command.type);
    if (!handler) throw new Error(`No handler registered for command type: ${command.type}`);
    return handler(command);
  }
} 
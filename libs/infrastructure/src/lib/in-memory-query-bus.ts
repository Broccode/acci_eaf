import { QueryBus } from '../../../core/src/lib/ports/query-bus.interface';

export class InMemoryQueryBus implements QueryBus {
  private handlers = new Map<string, (query: any) => Promise<any>>();

  register<TQuery>(type: string, handler: (query: TQuery) => Promise<any>): void {
    this.handlers.set(type, handler);
  }

  async execute<TQuery, TResult = any>(query: TQuery & { type: string }): Promise<TResult> {
    const handler = this.handlers.get(query.type);
    if (!handler) throw new Error(`No handler registered for query type: ${query.type}`);
    return handler(query);
  }
} 
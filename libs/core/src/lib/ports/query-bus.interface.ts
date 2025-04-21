export interface QueryBus<TQuery = any, TResult = any> {
  execute(query: TQuery): Promise<TResult>;
} 
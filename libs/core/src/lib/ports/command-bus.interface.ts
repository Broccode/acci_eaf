export interface CommandBus<TCommand = any, TResult = any> {
  execute(command: TCommand): Promise<TResult>;
} 
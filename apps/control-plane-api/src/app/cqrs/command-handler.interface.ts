// Defined as a separate interface instead of extending core to avoid type conflicts
/**
 * Command handler interface with support for return values
 */
export interface CommandHandler<TCommand, TResult = any> {
  /**
   * Execute the command with a specific return type
   * @param command The command to execute
   */
  execute(command: TCommand): Promise<TResult>;
} 
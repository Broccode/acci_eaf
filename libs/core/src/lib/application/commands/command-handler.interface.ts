/**
 * Interface for command handlers
 */
export interface CommandHandler<TCommand> {
  /**
   * Execute the command
   * @param command The command to execute
   */
  execute(command: TCommand): Promise<void>;
} 
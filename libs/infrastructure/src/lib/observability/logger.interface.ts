/**
 * Interface for structured logging
 */
export interface Logger {
  log(message: string, context?: string, meta?: Record<string, any>): void;
  error(message: string, trace?: string, context?: string, meta?: Record<string, any>): void;
  warn(message: string, context?: string, meta?: Record<string, any>): void;
  debug(message: string, context?: string, meta?: Record<string, any>): void;
  verbose(message: string, context?: string, meta?: Record<string, any>): void;
}

/**
 * Logger factory interface for creating loggers
 */
export interface LoggerFactory {
  /**
   * Creates a logger instance with the given context
   * @param context The context for the logger
   */
  createLogger(context: string): Logger;
} 
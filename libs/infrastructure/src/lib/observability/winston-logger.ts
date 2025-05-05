import { Logger as NestLogger, LoggerService } from '@nestjs/common';
import { Logger, LoggerFactory } from './logger.interface';
import { createLogger, format, transports, Logger as WinstonLogger } from 'winston';

export class WinstonLoggerService implements Logger, LoggerService {
  private readonly logger: WinstonLogger;
  private readonly context?: string;

  constructor(context?: string, options?: any) {
    this.context = context;

    // Default options for Winston logger
    const defaultOptions = {
      format: format.combine(
        format.timestamp(),
        format.json()
      ),
      transports: [
        new transports.Console({
          format: format.combine(
            format.colorize(),
            format.timestamp(),
            format.printf(({ timestamp, level, message, context, ...meta }) => {
              return `${timestamp} [${level}] [${context || 'Application'}]: ${message} ${Object.keys(meta).length ? JSON.stringify(meta) : ''}`;
            })
          )
        })
      ]
    };

    this.logger = createLogger({ ...defaultOptions, ...options });
  }

  log(message: string, context?: string, meta: Record<string, any> = {}): void {
    this.logger.info(message, { context: context || this.context, ...meta });
  }

  error(message: string, trace?: string, context?: string, meta: Record<string, any> = {}): void {
    this.logger.error(message, { 
      context: context || this.context, 
      trace,
      ...meta 
    });
  }

  warn(message: string, context?: string, meta: Record<string, any> = {}): void {
    this.logger.warn(message, { context: context || this.context, ...meta });
  }

  debug(message: string, context?: string, meta: Record<string, any> = {}): void {
    this.logger.debug(message, { context: context || this.context, ...meta });
  }

  verbose(message: string, context?: string, meta: Record<string, any> = {}): void {
    this.logger.verbose(message, { context: context || this.context, ...meta });
  }
}

export class WinstonLoggerFactory implements LoggerFactory {
  private options: any;

  constructor(options?: any) {
    this.options = options;
  }

  createLogger(context: string): Logger {
    return new WinstonLoggerService(context, this.options);
  }
} 
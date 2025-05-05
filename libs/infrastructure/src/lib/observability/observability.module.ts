import { DynamicModule, Global, Module, Provider } from '@nestjs/common';
import { WinstonModule, utilities as nestWinstonUtilities } from 'nest-winston';
import * as winston from 'winston';
import { WinstonLoggerFactory } from './winston-logger';
import { LoggerFactory } from './logger.interface';

export const LOGGER_FACTORY = 'LOGGER_FACTORY';

export interface ObservabilityModuleOptions {
  logLevel?: string;
  logFormat?: 'json' | 'console';
  additionalTransports?: winston.transport[];
}

@Global()
@Module({})
export class ObservabilityModule {
  static forRoot(options: ObservabilityModuleOptions = {}): DynamicModule {
    const { logLevel = 'info', logFormat = 'console', additionalTransports = [] } = options;

    // Configure Winston format based on environment
    let winstonFormat: winston.Logform.Format;
    if (logFormat === 'json') {
      winstonFormat = winston.format.combine(
        winston.format.timestamp(),
        winston.format.json()
      );
    } else {
      winstonFormat = winston.format.combine(
        winston.format.timestamp(),
        winston.format.ms(),
        nestWinstonUtilities.format.nestLike('ACCI-EAF', {
          prettyPrint: true,
          colors: true,
        })
      );
    }

    // Create Winston logger configuration
    const winstonConfig = {
      level: logLevel,
      format: winstonFormat,
      transports: [
        new winston.transports.Console(),
        ...additionalTransports
      ]
    };

    // Create providers
    const loggerFactoryProvider: Provider = {
      provide: LOGGER_FACTORY,
      useFactory: () => {
        return new WinstonLoggerFactory(winstonConfig);
      }
    };

    return {
      module: ObservabilityModule,
      imports: [
        WinstonModule.forRoot(winstonConfig)
      ],
      providers: [
        loggerFactoryProvider,
      ],
      exports: [
        WinstonModule,
        loggerFactoryProvider,
      ]
    };
  }
} 
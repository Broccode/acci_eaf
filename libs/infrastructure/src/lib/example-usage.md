# Using Observability and Health Check Modules

This document demonstrates how to use the Observability and Health Check modules in a NestJS application.

## Basic Setup

```typescript
// app.module.ts
import { Module } from '@nestjs/common';
import { InfrastructureModule } from '@acci/infrastructure';
import { AppController } from './app.controller';
import { AppService } from './app.service';

@Module({
  imports: [
    InfrastructureModule.forRoot({
      // Configure Observability module
      observability: {
        logLevel: process.env.LOG_LEVEL || 'info',
        logFormat: (process.env.NODE_ENV === 'production') ? 'json' : 'console',
      },
      // Configure Health module
      health: {
        path: 'health',
        enableDefaultChecks: true,
      }
    }),
    // Other modules...
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
```

## Using the Logger in a Service

```typescript
// app.service.ts
import { Injectable, Inject } from '@nestjs/common';
import { Logger, LoggerFactory, LOGGER_FACTORY } from '@acci/infrastructure';

@Injectable()
export class AppService {
  private readonly logger: Logger;

  constructor(
    @Inject(LOGGER_FACTORY) loggerFactory: LoggerFactory,
  ) {
    this.logger = loggerFactory.createLogger(AppService.name);
  }

  getHello(): string {
    this.logger.log('getHello method called', undefined, { 
      additionalInfo: 'This is additional metadata for structured logging'
    });
    return 'Hello World!';
  }

  performComplexOperation(data: any): void {
    try {
      this.logger.debug('Starting complex operation', undefined, { data });
      // Operation logic...
      this.logger.log('Complex operation completed successfully');
    } catch (error) {
      this.logger.error('Error during complex operation', error.stack, undefined, { 
        errorCode: error.code,
        data 
      });
      throw error;
    }
  }
}
```

## Adding Custom Health Indicators

If you need to add custom health indicators (e.g., for database connections, external APIs, etc.), you can create a custom health module:

```typescript
// custom-health.module.ts
import { Module } from '@nestjs/common';
import { HealthModule, TerminusModule } from '@acci/infrastructure';
import { CustomHealthIndicator } from './custom-health.indicator';
import { CustomHealthController } from './custom-health.controller';

@Module({
  imports: [
    TerminusModule,
  ],
  controllers: [CustomHealthController],
  providers: [CustomHealthIndicator],
})
export class CustomHealthModule {}
```

```typescript
// custom-health.indicator.ts
import { Injectable } from '@nestjs/common';
import { HealthIndicator, HealthIndicatorResult, HealthCheckError } from '@nestjs/terminus';

@Injectable()
export class CustomHealthIndicator extends HealthIndicator {
  constructor() {
    super();
  }

  async isHealthy(key: string): Promise<HealthIndicatorResult> {
    // Check some service or condition
    const isHealthy = true; // Replace with actual check
    
    const result = this.getStatus(key, isHealthy);

    if (isHealthy) {
      return result;
    }
    
    throw new HealthCheckError('Custom check failed', result);
  }
}
```

```typescript
// custom-health.controller.ts
import { Controller, Get } from '@nestjs/common';
import { HealthCheck, HealthCheckService } from '@nestjs/terminus';
import { CustomHealthIndicator } from './custom-health.indicator';

@Controller('custom-health')
export class CustomHealthController {
  constructor(
    private health: HealthCheckService,
    private customHealthIndicator: CustomHealthIndicator,
  ) {}

  @Get()
  @HealthCheck()
  check() {
    return this.health.check([
      () => this.customHealthIndicator.isHealthy('custom-service'),
    ]);
  }
}
```

## Advanced Configuration

For more advanced configurations, including custom Winston transports:

```typescript
import { InfrastructureModule } from '@acci/infrastructure';
import * as winston from 'winston';
import 'winston-daily-rotate-file';

@Module({
  imports: [
    InfrastructureModule.forRoot({
      observability: {
        logLevel: 'debug',
        logFormat: 'json',
        additionalTransports: [
          new winston.transports.DailyRotateFile({
            filename: 'logs/application-%DATE%.log',
            datePattern: 'YYYY-MM-DD',
            zippedArchive: true,
            maxSize: '20m',
            maxFiles: '14d',
          }),
        ],
      },
    }),
  ],
})
export class AppModule {}
```

## Testing with Structured Logging

```typescript
// app.service.spec.ts
import { Test, TestingModule } from '@nestjs/testing';
import { AppService } from './app.service';
import { LOGGER_FACTORY, WinstonLoggerFactory } from '@acci/infrastructure';

describe('AppService', () => {
  let service: AppService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AppService,
        {
          provide: LOGGER_FACTORY,
          useValue: {
            createLogger: jest.fn().mockReturnValue({
              log: jest.fn(),
              error: jest.fn(),
              warn: jest.fn(),
              debug: jest.fn(),
              verbose: jest.fn(),
            }),
          },
        },
      ],
    }).compile();

    service = module.get<AppService>(AppService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  // Other tests...
});
```

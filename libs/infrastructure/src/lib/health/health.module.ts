import { DynamicModule, Module } from '@nestjs/common';
import { TerminusModule } from '@nestjs/terminus';
import { HealthController } from './health.controller';
import { HttpModule } from '@nestjs/axios';

export interface HealthModuleOptions {
  path?: string;
  enableDefaultChecks?: boolean;
  disableHealthEndpoint?: boolean;
}

@Module({})
export class HealthModule {
  static forRoot(options: HealthModuleOptions = {}): DynamicModule {
    const {
      path = 'health',
      enableDefaultChecks = true,
      disableHealthEndpoint = false,
    } = options;

    const controllers = disableHealthEndpoint ? [] : [HealthController];

    return {
      module: HealthModule,
      imports: [
        TerminusModule,
        HttpModule,
      ],
      controllers,
      providers: [
        {
          provide: 'HEALTH_MODULE_OPTIONS',
          useValue: {
            path,
            enableDefaultChecks,
          },
        },
      ],
      exports: [TerminusModule],
    };
  }
} 
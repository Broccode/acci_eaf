import { Global, Module, DynamicModule } from '@nestjs/common';
import { MikroOrmModule } from '@mikro-orm/nestjs';
import { TenancyModule } from 'tenancy'; // Assuming 'tenancy' path mapping
import { TenantFilterInitializer } from './persistence/filters/tenant.filter';
import mikroOrmConfig from '../../../../mikro-orm.config'; // Adjust path as needed
import { SampleTenantEntity } from './persistence/entities/sample-tenant.entity';
import { ConfigType } from '@nestjs/config';
import { CommandBus, CqrsModule, EventBus, QueryBus } from '@nestjs/cqrs';
import { InMemoryCommandBus } from './in-memory-command-bus';
import { InMemoryEventBus } from './in-memory-event-bus';
import { InMemoryQueryBus } from './in-memory-query-bus';
import { ObservabilityModule, ObservabilityModuleOptions } from './observability';
import { HealthModule, HealthModuleOptions } from './health';

// Making it Global might simplify dependency injection, but exporting is often cleaner.
// Decide based on project needs. Let's export for now.
export interface InfrastructureModuleOptions<T extends Record<string, any> = Record<string, any>> {
  appConfig?: T;
  observability?: ObservabilityModuleOptions;
  health?: HealthModuleOptions;
}

@Module({})
export class InfrastructureModule {
  static forRoot<T extends Record<string, any> = Record<string, any>>(
    options: InfrastructureModuleOptions<T> = {},
  ): DynamicModule {
    const { 
      appConfig = {},
      observability = {},
      health = {},
    } = options;

    return {
      module: InfrastructureModule,
      global: true,
      imports: [
        TenancyModule, // Import TenancyModule to access TenantContextService
        MikroOrmModule.forRootAsync({
          // Load configuration asynchronously
          useFactory: (): any => { // Return type any to satisfy linter for now
            // Clone the config to avoid modifying the original import
            const finalConfig = { ...mikroOrmConfig };

            // Ensure registerRequestContext is true for AsyncLocalStorage context
            // This property is not in the base Options, needs to be added for NestJS module
            (finalConfig as any).registerRequestContext = true;

            return finalConfig;
          },
          // registerRequestContext: true, // THIS IS WRONG for forRootAsync
        }),
        MikroOrmModule.forFeature([SampleTenantEntity]),
        CqrsModule,
        ObservabilityModule.forRoot(observability),
        HealthModule.forRoot(health),
      ],
      controllers: [],
      providers: [
        TenantFilterInitializer, // Register the initializer
        {
          provide: 'APP_CONFIG',
          useValue: appConfig,
        },
        {
          provide: CommandBus,
          useFactory: () => {
            return new InMemoryCommandBus();
          },
        },
        {
          provide: EventBus,
          useFactory: () => {
            return new InMemoryEventBus();
          },
        },
        {
          provide: QueryBus,
          useFactory: () => {
            return new InMemoryQueryBus();
          },
        },
        // Other infrastructure providers...
      ],
      exports: [
        MikroOrmModule, // Export MikroOrmModule to make repositories injectable
        MikroOrmModule.forFeature([SampleTenantEntity]),
        CqrsModule,
        'APP_CONFIG',
        // Export other necessary providers...
      ],
    };
  }
}

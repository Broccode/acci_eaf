import { Global, Module } from '@nestjs/common';
import { MikroOrmModule } from '@mikro-orm/nestjs';
import { TenancyModule } from 'tenancy'; // Assuming 'tenancy' path mapping
import { TenantFilterInitializer } from './persistence/filters/tenant.filter';
import mikroOrmConfig from '../../../../mikro-orm.config'; // Adjust path as needed
import { SampleTenantEntity } from './persistence/entities/sample-tenant.entity';

// Making it Global might simplify dependency injection, but exporting is often cleaner.
// Decide based on project needs. Let's export for now.
@Module({
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
  ],
  controllers: [],
  providers: [
    TenantFilterInitializer, // Register the initializer
    // Other infrastructure providers...
  ],
  exports: [
    MikroOrmModule, // Export MikroOrmModule to make repositories injectable
    MikroOrmModule.forFeature([SampleTenantEntity]),
    // Export other necessary providers...
  ],
})
export class InfrastructureModule {}

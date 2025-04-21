import { Module } from '@nestjs/common';
import { TenancyModule } from 'tenancy'; // Assuming 'tenancy' path mapping
import { TenantFilterInitializer } from './persistence/filters/tenant.filter';

@Module({
  imports: [TenancyModule], // Import TenancyModule to access TenantContextService
  controllers: [],
  providers: [
    TenantFilterInitializer, // Register the initializer
    // Other infrastructure providers...
  ],
  exports: [
    // Export necessary providers...
  ],
})
export class InfrastructureModule {}

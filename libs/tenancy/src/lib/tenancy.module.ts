import { Module, Global } from '@nestjs/common';
import { TenantContextService } from './tenant-context.service';

@Global() // Make the service globally available
@Module({
  controllers: [],
  providers: [TenantContextService],
  exports: [TenantContextService], // Export the service for use in other modules
})
export class TenancyModule {}

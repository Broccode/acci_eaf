import { Module } from '@nestjs/common';
import { MikroOrmModule } from '@mikro-orm/nestjs';
import { TenantsController } from './tenants.controller';
import { TenantsService } from './tenants.service';
import { TenantRepository } from './entities/tenant.repository';
import { Tenant } from './entities/tenant.entity';

@Module({
  imports: [
    MikroOrmModule.forFeature([Tenant])
  ],
  controllers: [TenantsController],
  providers: [TenantsService, TenantRepository],
  exports: [TenantsService]
})
export class TenantsModule {} 
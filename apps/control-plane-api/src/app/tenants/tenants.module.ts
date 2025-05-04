import { Module } from '@nestjs/common';
import { MikroOrmModule } from '@mikro-orm/nestjs';
import { TenantsController } from './tenants.controller';
import { TenantsService } from './tenants.service';
import { TenantRepository } from './entities/tenant.repository';
import { Tenant } from './entities/tenant.entity';
import { CreateTenantHandler } from '../commands/handlers/create-tenant.handler';
import { UpdateTenantHandler } from '../commands/handlers/update-tenant.handler';
import { DeleteTenantHandler } from '../commands/handlers/delete-tenant.handler';
import { GetTenantByIdHandler } from '../queries/handlers/get-tenant-by-id.handler';
import { ListTenantsHandler } from '../queries/handlers/list-tenants.handler';

@Module({
  imports: [
    MikroOrmModule.forFeature([Tenant])
  ],
  controllers: [TenantsController],
  providers: [
    TenantsService,
    TenantRepository,
    CreateTenantHandler,
    UpdateTenantHandler,
    DeleteTenantHandler,
    GetTenantByIdHandler,
    ListTenantsHandler,
  ],
  exports: [TenantsService]
})
export class TenantsModule {} 
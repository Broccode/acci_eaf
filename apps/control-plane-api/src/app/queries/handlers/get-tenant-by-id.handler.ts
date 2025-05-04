import { Injectable } from '@nestjs/common';
import { QueryHandler } from '../../cqrs';
import { GetTenantByIdQuery } from '../impl/get-tenant-by-id.query';
import { TenantsService } from '../../tenants/tenants.service';
import { Tenant } from '../../tenants/entities/tenant.entity';

@Injectable()
export class GetTenantByIdHandler implements QueryHandler<GetTenantByIdQuery, Tenant> {
  constructor(private readonly tenantsService: TenantsService) {}

  async execute(query: GetTenantByIdQuery): Promise<Tenant> {
    return this.tenantsService.findOne(query.id);
  }
} 
import { Injectable } from '@nestjs/common';
import { QueryHandler } from '../../cqrs';
import { ListTenantsQuery } from '../impl/list-tenants.query';
import { TenantsService } from '../../tenants/tenants.service';
import { Tenant } from '../../tenants/entities/tenant.entity';

@Injectable()
export class ListTenantsHandler implements QueryHandler<ListTenantsQuery, Tenant[]> {
  constructor(private readonly tenantsService: TenantsService) {}

  async execute(query: ListTenantsQuery): Promise<Tenant[]> {
    return query.activeOnly
      ? this.tenantsService.findActive()
      : this.tenantsService.findAll();
  }
} 
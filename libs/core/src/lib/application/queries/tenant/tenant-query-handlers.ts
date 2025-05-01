import { QueryHandler } from '../query-handler.interface';
import { 
  GetAllTenantsQuery, 
  GetTenantByIdQuery, 
  GetTenantByNameQuery 
} from './tenant-queries';
import { PaginatedTenantsResponse, TenantDto } from './tenant-dto';
import { TenantRepository } from '../../../domain/tenant/tenant-repository.interface';
import { TenantAggregate } from '../../../domain/tenant/tenant-aggregate';

/**
 * Handler for getting a tenant by ID
 */
export class GetTenantByIdQueryHandler implements QueryHandler<GetTenantByIdQuery, TenantDto | undefined> {
  constructor(private readonly tenantRepository: TenantRepository) {}

  async execute(query: GetTenantByIdQuery): Promise<TenantDto | undefined> {
    const tenant = await this.tenantRepository.findById(query.id);
    if (!tenant) {
      return undefined;
    }

    return this.mapToDto(tenant);
  }

  private mapToDto(tenant: TenantAggregate): TenantDto {
    return {
      id: tenant.id,
      name: tenant.name,
      description: tenant.description,
      status: tenant.status,
      configuration: tenant.configuration,
      contactEmail: tenant.contactEmail,
      createdAt: tenant.createdAt,
      updatedAt: tenant.updatedAt
    };
  }
}

/**
 * Handler for getting a tenant by name
 */
export class GetTenantByNameQueryHandler implements QueryHandler<GetTenantByNameQuery, TenantDto | undefined> {
  constructor(private readonly tenantRepository: TenantRepository) {}

  async execute(query: GetTenantByNameQuery): Promise<TenantDto | undefined> {
    const tenant = await this.tenantRepository.findByName(query.name);
    if (!tenant) {
      return undefined;
    }

    return this.mapToDto(tenant);
  }

  private mapToDto(tenant: TenantAggregate): TenantDto {
    return {
      id: tenant.id,
      name: tenant.name,
      description: tenant.description,
      status: tenant.status,
      configuration: tenant.configuration,
      contactEmail: tenant.contactEmail,
      createdAt: tenant.createdAt,
      updatedAt: tenant.updatedAt
    };
  }
}

/**
 * Handler for getting all tenants with pagination
 */
export class GetAllTenantsQueryHandler implements QueryHandler<GetAllTenantsQuery, PaginatedTenantsResponse> {
  constructor(private readonly tenantRepository: TenantRepository) {}

  async execute(query: GetAllTenantsQuery): Promise<PaginatedTenantsResponse> {
    const allTenants = await this.tenantRepository.findAll();
    const total = allTenants.length;
    
    // Simple pagination logic (in a real implementation, this would be done at the repository level)
    const startIndex = (query.page - 1) * query.limit;
    const endIndex = Math.min(startIndex + query.limit, total);
    const paginatedTenants = allTenants.slice(startIndex, endIndex);

    return {
      items: paginatedTenants.map(tenant => this.mapToDto(tenant)),
      total,
      page: query.page,
      limit: query.limit,
      pageCount: Math.ceil(total / query.limit)
    };
  }

  private mapToDto(tenant: TenantAggregate): TenantDto {
    return {
      id: tenant.id,
      name: tenant.name,
      description: tenant.description,
      status: tenant.status,
      configuration: tenant.configuration,
      contactEmail: tenant.contactEmail,
      createdAt: tenant.createdAt,
      updatedAt: tenant.updatedAt
    };
  }
} 
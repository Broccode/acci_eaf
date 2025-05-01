import { Injectable } from '@nestjs/common';
import { EntityManager } from '@mikro-orm/core';
import { Tenant, TenantStatus } from './tenant.entity';

/**
 * Repository for handling Tenant entity persistence
 */
@Injectable()
export class TenantRepository {
  constructor(private readonly em: EntityManager) {}

  async findAllTenants(): Promise<Tenant[]> {
    return this.em.find(Tenant, {});
  }

  async findActiveTenants(): Promise<Tenant[]> {
    return this.em.find(Tenant, { status: TenantStatus.ACTIVE });
  }

  async findTenantById(id: string): Promise<Tenant | null> {
    return this.em.findOne(Tenant, { id });
  }

  async createTenant(
    name: string,
    description?: string,
    contactEmail?: string,
    configuration?: Record<string, unknown>
  ): Promise<Tenant> {
    const tenant = new Tenant(name, description, contactEmail, configuration);
    await this.em.persistAndFlush(tenant);
    return tenant;
  }

  async updateTenantStatus(id: string, status: TenantStatus): Promise<Tenant | null> {
    const tenant = await this.findTenantById(id);
    if (tenant) {
      tenant.status = status;
      await this.em.flush();
    }
    return tenant;
  }

  /**
   * Find tenant by name (exact match)
   */
  findByName(name: string): Promise<Tenant | null> {
    return this.em.findOne(Tenant, { name });
  }

  /**
   * Check if a tenant with the given name exists
   */
  async nameExists(name: string): Promise<boolean> {
    const count = await this.em.count(Tenant, { name });
    return count > 0;
  }

  /**
   * Search tenants by name pattern
   */
  searchByName(namePattern: string): Promise<Tenant[]> {
    return this.em.find(Tenant, {
      name: { $like: `%${namePattern}%` },
    });
  }
} 
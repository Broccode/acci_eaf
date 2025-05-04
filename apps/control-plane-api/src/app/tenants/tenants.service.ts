import { BadRequestException, Injectable, NotFoundException, Inject } from '@nestjs/common';
import { EntityManager } from '@mikro-orm/core';
import { TenantRepository } from './entities/tenant.repository';
import { CreateTenantDto } from './dto/create-tenant.dto';
import { UpdateTenantDto } from './dto/update-tenant.dto';
import { Tenant, TenantStatus } from './entities/tenant.entity';

/**
 * Service for handling tenant-related operations
 */
@Injectable()
export class TenantsService {
  constructor(
    @Inject(TenantRepository)
    private readonly tenantRepository: TenantRepository,
    private readonly em: EntityManager,
  ) {}

  /**
   * Create a new tenant
   */
  async create(createTenantDto: CreateTenantDto): Promise<Tenant> {
    return this.tenantRepository.createTenant(
      createTenantDto.name,
      createTenantDto.description,
      createTenantDto.contactEmail,
      createTenantDto.configuration
    );
  }

  /**
   * Get all tenants
   */
  async findAll(): Promise<Tenant[]> {
    return this.tenantRepository.findAllTenants();
  }

  /**
   * Get only active tenants
   */
  async findActive(): Promise<Tenant[]> {
    return this.tenantRepository.findActiveTenants();
  }

  /**
   * Find a tenant by ID
   */
  async findOne(id: string): Promise<Tenant> {
    const tenant = await this.tenantRepository.findTenantById(id);
    if (!tenant) {
      throw new NotFoundException(`Tenant with ID ${id} not found`);
    }
    return tenant;
  }

  /**
   * Search for tenants by name
   */
  async search(namePattern: string): Promise<Tenant[]> {
    return this.tenantRepository.searchByName(namePattern);
  }

  /**
   * Update a tenant by ID
   */
  async update(id: string, updateTenantDto: UpdateTenantDto): Promise<Tenant> {
    const tenant = await this.findOne(id);
    
    if (updateTenantDto.name !== undefined) {
      tenant.name = updateTenantDto.name;
    }
    
    if (updateTenantDto.description !== undefined) {
      tenant.description = updateTenantDto.description;
    }
    
    if (updateTenantDto.contactEmail !== undefined) {
      tenant.contactEmail = updateTenantDto.contactEmail;
    }
    
    if (updateTenantDto.configuration !== undefined) {
      tenant.configuration = updateTenantDto.configuration;
    }
    
    await this.em.flush();
    return tenant;
  }

  /**
   * Activate a tenant
   */
  async activate(id: string): Promise<Tenant> {
    const tenant = await this.tenantRepository.updateTenantStatus(id, TenantStatus.ACTIVE);
    if (!tenant) {
      throw new NotFoundException(`Tenant with ID ${id} not found`);
    }
    return tenant;
  }

  /**
   * Deactivate a tenant
   */
  async deactivate(id: string): Promise<Tenant> {
    const tenant = await this.tenantRepository.updateTenantStatus(id, TenantStatus.INACTIVE);
    if (!tenant) {
      throw new NotFoundException(`Tenant with ID ${id} not found`);
    }
    return tenant;
  }

  /**
   * Suspend a tenant
   */
  async suspend(id: string): Promise<Tenant> {
    const tenant = await this.tenantRepository.updateTenantStatus(id, TenantStatus.SUSPENDED);
    if (!tenant) {
      throw new NotFoundException(`Tenant with ID ${id} not found`);
    }
    return tenant;
  }

  /**
   * Remove a tenant by ID (soft delete via status change)
   */
  async remove(id: string): Promise<void> {
    const tenant = await this.findOne(id);
    await this.em.removeAndFlush(tenant);
  }
} 
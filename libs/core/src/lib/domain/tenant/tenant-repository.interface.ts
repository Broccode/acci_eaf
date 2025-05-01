import { TenantAggregate } from './tenant-aggregate';

/**
 * Repository interface for accessing and persisting Tenant aggregates
 */
export interface TenantRepository {
  /**
   * Find a tenant by ID
   * @param id The tenant ID
   * @returns The tenant aggregate if found, undefined otherwise
   */
  findById(id: string): Promise<TenantAggregate | undefined>;

  /**
   * Find a tenant by name
   * @param name The tenant name
   * @returns The tenant aggregate if found, undefined otherwise
   */
  findByName(name: string): Promise<TenantAggregate | undefined>;
  
  /**
   * Find all tenants
   * @returns Array of tenant aggregates
   */
  findAll(): Promise<TenantAggregate[]>;
  
  /**
   * Save a tenant aggregate
   * This will persist all uncommitted events and clear them from the aggregate
   * @param tenant The tenant aggregate to save
   */
  save(tenant: TenantAggregate): Promise<void>;
  
  /**
   * Delete a tenant
   * @param id The tenant ID
   */
  delete(id: string): Promise<void>;
} 
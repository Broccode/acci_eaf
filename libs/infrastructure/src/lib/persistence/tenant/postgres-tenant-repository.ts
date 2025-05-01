import { EntityManager } from '@mikro-orm/core';
import { Injectable } from '@nestjs/common';
import { TenantAggregate, TenantRepository } from 'core';
import { PostgresEventStore } from '../postgres-event-store';

/**
 * PostgreSQL implementation of the TenantRepository using MikroORM and Event Sourcing
 */
@Injectable()
export class PostgresTenantRepository implements TenantRepository {
  constructor(
    private readonly em: EntityManager,
    private readonly eventStore: PostgresEventStore
  ) {}

  /**
   * Find a tenant by ID
   */
  async findById(id: string): Promise<TenantAggregate | undefined> {
    const events = await this.eventStore.loadEvents(id);
    
    if (events.length === 0) {
      return undefined;
    }

    const tenant = new TenantAggregate(id);
    tenant.loadFromHistory(events);
    
    return tenant;
  }

  /**
   * Find a tenant by name
   * Note: This is a naive implementation that loads all tenants and filters in memory
   * In a real implementation, you would use a read model for this
   */
  async findByName(name: string): Promise<TenantAggregate | undefined> {
    // In a real implementation, we would use a read model/projection for this lookup
    // This is just a placeholder implementation
    const allTenants = await this.findAll();
    return allTenants.find(tenant => tenant.name === name);
  }

  /**
   * Find all tenants
   * Note: This is a naive implementation that loads all event streams for tenants
   * In a real implementation, you would use a read model for this
   */
  async findAll(): Promise<TenantAggregate[]> {
    // In a real implementation, we would use a read model/projection to get all tenant IDs
    // and then load each tenant by ID
    // This is just a placeholder implementation that assumes we have a way to get all tenant IDs
    
    // Mock implementation - in a real system, you would query a read model
    const tenantIds: string[] = await this.getTenantIdsFromReadModel();
    
    const tenants: TenantAggregate[] = [];
    for (const id of tenantIds) {
      const tenant = await this.findById(id);
      if (tenant) {
        tenants.push(tenant);
      }
    }
    
    return tenants;
  }

  /**
   * Save a tenant aggregate
   * This persists all uncommitted events from the aggregate to the event store
   */
  async save(tenant: TenantAggregate): Promise<void> {
    const uncommittedEvents = tenant.getUncommittedEvents();
    
    if (uncommittedEvents.length === 0) {
      return;
    }
    
    await this.eventStore.saveMany(uncommittedEvents);
    
    // Clear uncommitted events after they've been saved
    tenant.clearUncommittedEvents();
  }

  /**
   * Delete a tenant
   * In an event-sourced system, this typically means adding a "TenantDeleted" event
   * rather than actually removing data
   */
  async delete(id: string): Promise<void> {
    // In a real implementation, this would likely create and persist a TenantDeleted event
    // For simplicity, we're not implementing actual deletion logic here
    throw new Error('Not implemented - deleting tenants is not supported in this version');
  }

  /**
   * Helper method to get all tenant IDs from a read model
   * This is a placeholder for a real implementation
   */
  private async getTenantIdsFromReadModel(): Promise<string[]> {
    // In a real implementation, this would query a read model
    // For now, we're returning an empty array
    return [];
  }
} 
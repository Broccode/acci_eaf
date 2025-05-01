import { TenantRepository } from '../../../domain/tenant/tenant-repository.interface';
import { TenantAggregate } from '../../../domain/tenant/tenant-aggregate';
import { CommandHandler } from '../command-handler.interface';
import {
  ActivateTenantCommand,
  CreateTenantCommand,
  DeactivateTenantCommand,
  DeleteTenantCommand,
  SuspendTenantCommand,
  UpdateTenantCommand
} from './tenant-commands';

/**
 * Error thrown when an entity is not found
 */
export class EntityNotFoundError extends Error {
  constructor(entityType: string, id: string) {
    super(`${entityType} with ID ${id} not found`);
    this.name = 'EntityNotFoundError';
  }
}

/**
 * Command handler for creating tenants
 */
export class CreateTenantCommandHandler implements CommandHandler<CreateTenantCommand> {
  constructor(private readonly tenantRepository: TenantRepository) {}

  async execute(command: CreateTenantCommand): Promise<void> {
    // Check if tenant with same name already exists
    const existingTenant = await this.tenantRepository.findByName(command.name);
    if (existingTenant) {
      throw new Error(`Tenant with name ${command.name} already exists`);
    }

    // Create new tenant
    const tenant = TenantAggregate.create(
      command.name,
      command.description,
      command.contactEmail,
      command.configuration
    );

    // Save tenant (which persists events)
    await this.tenantRepository.save(tenant);
  }
}

/**
 * Command handler for activating tenants
 */
export class ActivateTenantCommandHandler implements CommandHandler<ActivateTenantCommand> {
  constructor(private readonly tenantRepository: TenantRepository) {}

  async execute(command: ActivateTenantCommand): Promise<void> {
    const tenant = await this.tenantRepository.findById(command.tenantId);
    if (!tenant) {
      throw new EntityNotFoundError('Tenant', command.tenantId);
    }

    tenant.activate();
    await this.tenantRepository.save(tenant);
  }
}

/**
 * Command handler for deactivating tenants
 */
export class DeactivateTenantCommandHandler implements CommandHandler<DeactivateTenantCommand> {
  constructor(private readonly tenantRepository: TenantRepository) {}

  async execute(command: DeactivateTenantCommand): Promise<void> {
    const tenant = await this.tenantRepository.findById(command.tenantId);
    if (!tenant) {
      throw new EntityNotFoundError('Tenant', command.tenantId);
    }

    tenant.deactivate();
    await this.tenantRepository.save(tenant);
  }
}

/**
 * Command handler for suspending tenants
 */
export class SuspendTenantCommandHandler implements CommandHandler<SuspendTenantCommand> {
  constructor(private readonly tenantRepository: TenantRepository) {}

  async execute(command: SuspendTenantCommand): Promise<void> {
    const tenant = await this.tenantRepository.findById(command.tenantId);
    if (!tenant) {
      throw new EntityNotFoundError('Tenant', command.tenantId);
    }

    tenant.suspend();
    await this.tenantRepository.save(tenant);
  }
}

/**
 * Command handler for updating tenants
 */
export class UpdateTenantCommandHandler implements CommandHandler<UpdateTenantCommand> {
  constructor(private readonly tenantRepository: TenantRepository) {}

  async execute(command: UpdateTenantCommand): Promise<void> {
    const tenant = await this.tenantRepository.findById(command.tenantId);
    if (!tenant) {
      throw new EntityNotFoundError('Tenant', command.tenantId);
    }

    tenant.update(
      command.name,
      command.description,
      command.contactEmail,
      command.configuration
    );

    await this.tenantRepository.save(tenant);
  }
}

/**
 * Command handler for deleting tenants
 */
export class DeleteTenantCommandHandler implements CommandHandler<DeleteTenantCommand> {
  constructor(private readonly tenantRepository: TenantRepository) {}

  async execute(command: DeleteTenantCommand): Promise<void> {
    const tenant = await this.tenantRepository.findById(command.tenantId);
    if (!tenant) {
      throw new EntityNotFoundError('Tenant', command.tenantId);
    }

    await this.tenantRepository.delete(command.tenantId);
  }
} 
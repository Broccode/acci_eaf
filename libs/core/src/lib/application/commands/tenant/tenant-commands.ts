/**
 * Command to create a new tenant
 */
export class CreateTenantCommand {
  constructor(
    public readonly name: string,
    public readonly description?: string,
    public readonly contactEmail?: string,
    public readonly configuration?: Record<string, unknown>
  ) {}
}

/**
 * Command to activate a tenant
 */
export class ActivateTenantCommand {
  constructor(
    public readonly tenantId: string
  ) {}
}

/**
 * Command to deactivate a tenant
 */
export class DeactivateTenantCommand {
  constructor(
    public readonly tenantId: string
  ) {}
}

/**
 * Command to suspend a tenant
 */
export class SuspendTenantCommand {
  constructor(
    public readonly tenantId: string
  ) {}
}

/**
 * Command to update tenant information
 */
export class UpdateTenantCommand {
  constructor(
    public readonly tenantId: string,
    public readonly name?: string,
    public readonly description?: string,
    public readonly contactEmail?: string,
    public readonly configuration?: Record<string, unknown>
  ) {}
}

/**
 * Command to delete a tenant
 */
export class DeleteTenantCommand {
  constructor(
    public readonly tenantId: string
  ) {}
} 
import { 
  ActivateTenantCommandHandler,
  CreateTenantCommandHandler, 
  DeactivateTenantCommandHandler,
  DeleteTenantCommandHandler,
  EntityNotFoundError,
  SuspendTenantCommandHandler,
  UpdateTenantCommandHandler
} from './tenant-command-handlers';
import { 
  ActivateTenantCommand,
  CreateTenantCommand,
  DeactivateTenantCommand,
  DeleteTenantCommand,
  SuspendTenantCommand,
  UpdateTenantCommand
} from './tenant-commands';
import { TenantRepository } from '../../../domain/tenant/tenant-repository.interface';
import { TenantAggregate } from '../../../domain/tenant/tenant-aggregate';
import { TenantStatus } from '../../../domain/tenant/tenant-status.enum';

// Mocking the TenantRepository
class MockTenantRepository implements TenantRepository {
  private tenants: Map<string, TenantAggregate> = new Map();
  private tenantsByName: Map<string, TenantAggregate> = new Map();
  
  // Test helper to set up mock data
  mockTenantByName(name: string, tenant: TenantAggregate): void {
    this.tenantsByName.set(name, tenant);
  }
  
  mockTenantById(id: string, tenant: TenantAggregate): void {
    this.tenants.set(id, tenant);
    this.tenantsByName.set(tenant.name, tenant);
  }

  // Test helper to verify delete was called
  wasDeleted(id: string): boolean {
    return !this.tenants.has(id);
  }
  
  async findById(id: string): Promise<TenantAggregate | undefined> {
    return this.tenants.get(id);
  }
  
  async findByName(name: string): Promise<TenantAggregate | undefined> {
    return this.tenantsByName.get(name);
  }
  
  async findAll(): Promise<TenantAggregate[]> {
    return Array.from(this.tenants.values());
  }
  
  async save(tenant: TenantAggregate): Promise<void> {
    this.tenants.set(tenant.id, tenant);
    this.tenantsByName.set(tenant.name, tenant);
    tenant.clearUncommittedEvents();
  }
  
  async delete(id: string): Promise<void> {
    const tenant = this.tenants.get(id);
    if (tenant) {
      this.tenantsByName.delete(tenant.name);
      this.tenants.delete(id);
    }
  }
}

describe('CreateTenantCommandHandler', () => {
  let repository: MockTenantRepository;
  let handler: CreateTenantCommandHandler;
  
  beforeEach(() => {
    repository = new MockTenantRepository();
    handler = new CreateTenantCommandHandler(repository);
  });
  
  it('should create a new tenant successfully', async () => {
    // Arrange
    const command = new CreateTenantCommand(
      'Test Tenant',
      'Test Description',
      'test@example.com',
      { key: 'value' }
    );
    
    // Act
    await handler.execute(command);
    
    // Assert
    const createdTenant = await repository.findByName('Test Tenant');
    expect(createdTenant).toBeDefined();
    expect(createdTenant?.name).toBe('Test Tenant');
    expect(createdTenant?.description).toBe('Test Description');
    expect(createdTenant?.contactEmail).toBe('test@example.com');
    expect(createdTenant?.configuration).toEqual({ key: 'value' });
    expect(createdTenant?.status).toBe(TenantStatus.ACTIVE);
    
    // Should have cleared uncommitted events after saving
    expect(createdTenant?.getUncommittedEvents().length).toBe(0);
  });
  
  it('should throw an error when a tenant with the same name already exists', async () => {
    // Arrange
    const existingTenant = TenantAggregate.create('Existing Tenant');
    repository.mockTenantByName('Existing Tenant', existingTenant);
    
    const command = new CreateTenantCommand('Existing Tenant');
    
    // Act & Assert
    await expect(handler.execute(command)).rejects.toThrow(
      'Tenant with name Existing Tenant already exists'
    );
  });
});

describe('ActivateTenantCommandHandler', () => {
  let repository: MockTenantRepository;
  let handler: ActivateTenantCommandHandler;
  let tenant: TenantAggregate;
  const tenantId = 'test-tenant-id';
  
  beforeEach(() => {
    repository = new MockTenantRepository();
    handler = new ActivateTenantCommandHandler(repository);
    
    // Create an inactive tenant for testing
    tenant = TenantAggregate.create('Test Tenant');
    tenant.clearUncommittedEvents();
    tenant.deactivate();
    tenant.clearUncommittedEvents();
    
    // Mock repository to return this tenant
    repository.mockTenantById(tenantId, tenant);
  });
  
  it('should activate a tenant successfully', async () => {
    // Arrange
    const command = new ActivateTenantCommand(tenantId);
    
    // Act
    await handler.execute(command);
    
    // Assert
    const activatedTenant = await repository.findById(tenantId);
    expect(activatedTenant?.status).toBe(TenantStatus.ACTIVE);
  });
  
  it('should throw error when tenant does not exist', async () => {
    // Arrange
    const command = new ActivateTenantCommand('non-existent-id');
    
    // Act & Assert
    await expect(handler.execute(command)).rejects.toThrow(EntityNotFoundError);
    await expect(handler.execute(command)).rejects.toThrow(
      'Tenant with ID non-existent-id not found'
    );
  });
});

describe('DeactivateTenantCommandHandler', () => {
  let repository: MockTenantRepository;
  let handler: DeactivateTenantCommandHandler;
  let tenant: TenantAggregate;
  const tenantId = 'test-tenant-id';
  
  beforeEach(() => {
    repository = new MockTenantRepository();
    handler = new DeactivateTenantCommandHandler(repository);
    
    // Create an active tenant for testing
    tenant = TenantAggregate.create('Test Tenant');
    tenant.clearUncommittedEvents();
    
    // Mock repository to return this tenant
    repository.mockTenantById(tenantId, tenant);
  });
  
  it('should deactivate a tenant successfully', async () => {
    // Arrange
    const command = new DeactivateTenantCommand(tenantId);
    
    // Act
    await handler.execute(command);
    
    // Assert
    const deactivatedTenant = await repository.findById(tenantId);
    expect(deactivatedTenant?.status).toBe(TenantStatus.INACTIVE);
  });
  
  it('should throw error when tenant does not exist', async () => {
    // Arrange
    const command = new DeactivateTenantCommand('non-existent-id');
    
    // Act & Assert
    await expect(handler.execute(command)).rejects.toThrow(EntityNotFoundError);
  });
});

describe('SuspendTenantCommandHandler', () => {
  let repository: MockTenantRepository;
  let handler: SuspendTenantCommandHandler;
  let tenant: TenantAggregate;
  const tenantId = 'test-tenant-id';
  
  beforeEach(() => {
    repository = new MockTenantRepository();
    handler = new SuspendTenantCommandHandler(repository);
    
    // Create an active tenant for testing
    tenant = TenantAggregate.create('Test Tenant');
    tenant.clearUncommittedEvents();
    
    // Mock repository to return this tenant
    repository.mockTenantById(tenantId, tenant);
  });
  
  it('should suspend a tenant successfully', async () => {
    // Arrange
    const command = new SuspendTenantCommand(tenantId);
    
    // Act
    await handler.execute(command);
    
    // Assert
    const suspendedTenant = await repository.findById(tenantId);
    expect(suspendedTenant?.status).toBe(TenantStatus.SUSPENDED);
  });
  
  it('should throw error when tenant does not exist', async () => {
    // Arrange
    const command = new SuspendTenantCommand('non-existent-id');
    
    // Act & Assert
    await expect(handler.execute(command)).rejects.toThrow(EntityNotFoundError);
  });
});

describe('UpdateTenantCommandHandler', () => {
  let repository: MockTenantRepository;
  let handler: UpdateTenantCommandHandler;
  let tenant: TenantAggregate;
  const tenantId = 'test-tenant-id';
  
  beforeEach(() => {
    repository = new MockTenantRepository();
    handler = new UpdateTenantCommandHandler(repository);
    
    // Create a tenant with initial values for testing
    tenant = TenantAggregate.create(
      'Initial Name', 
      'Initial Description', 
      'initial@example.com', 
      { setting: 'initial' }
    );
    tenant.clearUncommittedEvents();
    
    // Mock repository to return this tenant
    repository.mockTenantById(tenantId, tenant);
  });
  
  it('should update tenant properties successfully', async () => {
    // Arrange
    const command = new UpdateTenantCommand(
      tenantId,
      'Updated Name',
      'Updated Description',
      'updated@example.com',
      { setting: 'updated' }
    );
    
    // Act
    await handler.execute(command);
    
    // Assert
    const updatedTenant = await repository.findById(tenantId);
    expect(updatedTenant?.name).toBe('Updated Name');
    expect(updatedTenant?.description).toBe('Updated Description');
    expect(updatedTenant?.contactEmail).toBe('updated@example.com');
    expect(updatedTenant?.configuration).toEqual({ setting: 'updated' });
  });
  
  it('should update only specified properties', async () => {
    // Arrange - only update name and email
    const command = new UpdateTenantCommand(
      tenantId,
      'Updated Name',
      undefined,
      'updated@example.com'
    );
    
    // Act
    await handler.execute(command);
    
    // Assert
    const updatedTenant = await repository.findById(tenantId);
    expect(updatedTenant?.name).toBe('Updated Name');
    expect(updatedTenant?.description).toBe('Initial Description'); // Unchanged
    expect(updatedTenant?.contactEmail).toBe('updated@example.com');
    expect(updatedTenant?.configuration).toEqual({ setting: 'initial' }); // Unchanged
  });
  
  it('should throw error when tenant does not exist', async () => {
    // Arrange
    const command = new UpdateTenantCommand('non-existent-id', 'New Name');
    
    // Act & Assert
    await expect(handler.execute(command)).rejects.toThrow(EntityNotFoundError);
  });
});

describe('DeleteTenantCommandHandler', () => {
  let repository: MockTenantRepository;
  let handler: DeleteTenantCommandHandler;
  let tenant: TenantAggregate;
  const tenantId = 'test-tenant-id';
  
  beforeEach(() => {
    repository = new MockTenantRepository();
    handler = new DeleteTenantCommandHandler(repository);
    
    // Create a tenant for testing
    tenant = TenantAggregate.create('Test Tenant');
    tenant.clearUncommittedEvents();
    
    // Mock repository to return this tenant
    repository.mockTenantById(tenantId, tenant);
  });
  
  it('should delete tenant successfully', async () => {
    // Arrange
    const command = new DeleteTenantCommand(tenantId);
    
    // Act
    await handler.execute(command);
    
    // Assert
    const deletedTenant = await repository.findById(tenantId);
    expect(deletedTenant).toBeUndefined();
    expect(repository.wasDeleted(tenantId)).toBe(true);
  });
  
  it('should throw error when tenant does not exist', async () => {
    // Arrange
    const command = new DeleteTenantCommand('non-existent-id');
    
    // Act & Assert
    await expect(handler.execute(command)).rejects.toThrow(EntityNotFoundError);
  });
}); 
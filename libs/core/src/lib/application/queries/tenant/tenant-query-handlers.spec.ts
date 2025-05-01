import { 
  GetAllTenantsQueryHandler, 
  GetTenantByIdQueryHandler, 
  GetTenantByNameQueryHandler 
} from './tenant-query-handlers';
import { 
  GetAllTenantsQuery, 
  GetTenantByIdQuery, 
  GetTenantByNameQuery 
} from './tenant-queries';
import { TenantRepository } from '../../../domain/tenant/tenant-repository.interface';
import { TenantAggregate } from '../../../domain/tenant/tenant-aggregate';
import { TenantStatus } from '../../../domain/tenant/tenant-status.enum';
import { PaginatedTenantsResponse, TenantDto } from './tenant-dto';

// Mocking the TenantRepository
class MockTenantRepository implements TenantRepository {
  private tenants: Map<string, TenantAggregate> = new Map();
  private tenantsByName: Map<string, TenantAggregate> = new Map();

  // Test helper to add mock data
  mockTenantById(id: string, tenant: TenantAggregate): void {
    this.tenants.set(id, tenant);
    this.tenantsByName.set(tenant.name, tenant);
  }

  // Test helper to add multiple tenants
  mockMultipleTenants(tenants: TenantAggregate[]): void {
    tenants.forEach(tenant => {
      this.tenants.set(tenant.id, tenant);
      this.tenantsByName.set(tenant.name, tenant);
    });
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

describe('GetTenantByIdQueryHandler', () => {
  let repository: MockTenantRepository;
  let handler: GetTenantByIdQueryHandler;
  let tenant: TenantAggregate;

  beforeEach(() => {
    repository = new MockTenantRepository();
    handler = new GetTenantByIdQueryHandler(repository);

    // Create a tenant for testing
    tenant = TenantAggregate.create(
      'Test Tenant',
      'Test Description',
      'test@example.com',
      { setting: 'value' }
    );
    
    // Mock the repository to return this tenant using its generated ID
    repository.mockTenantById(tenant.id, tenant);
  });

  it('should return the tenant DTO when found', async () => {
    // Arrange
    const query = new GetTenantByIdQuery(tenant.id);

    // Act
    const result = await handler.execute(query);

    // Assert
    expect(result).toBeDefined();
    expect(result?.id).toBe(tenant.id); // Use the generated ID
    expect(result?.name).toBe('Test Tenant');
    expect(result?.description).toBe('Test Description');
    expect(result?.contactEmail).toBe('test@example.com');
    expect(result?.configuration).toEqual({ setting: 'value' });
    expect(result?.status).toBe(TenantStatus.ACTIVE);
  });

  it('should return undefined when tenant not found', async () => {
    // Arrange
    const query = new GetTenantByIdQuery('non-existent-id');

    // Act
    const result = await handler.execute(query);

    // Assert
    expect(result).toBeUndefined();
  });
});

describe('GetTenantByNameQueryHandler', () => {
  let repository: MockTenantRepository;
  let handler: GetTenantByNameQueryHandler;
  let tenant: TenantAggregate;
  const tenantName = 'Test Tenant';

  beforeEach(() => {
    repository = new MockTenantRepository();
    handler = new GetTenantByNameQueryHandler(repository);

    // Create a tenant for testing
    tenant = TenantAggregate.create(
      tenantName,
      'Test Description',
      'test@example.com',
      { setting: 'value' }
    );
    
    // Mock the repository to return this tenant
    repository.mockTenantById(tenant.id, tenant);
  });

  it('should return the tenant DTO when found by name', async () => {
    // Arrange
    const query = new GetTenantByNameQuery(tenantName);

    // Act
    const result = await handler.execute(query);

    // Assert
    expect(result).toBeDefined();
    expect(result?.id).toBe(tenant.id);
    expect(result?.name).toBe(tenantName);
    expect(result?.description).toBe('Test Description');
    expect(result?.status).toBe(TenantStatus.ACTIVE);
  });

  it('should return undefined when tenant not found by name', async () => {
    // Arrange
    const query = new GetTenantByNameQuery('Non-Existent Tenant');

    // Act
    const result = await handler.execute(query);

    // Assert
    expect(result).toBeUndefined();
  });
});

describe('GetAllTenantsQueryHandler', () => {
  let repository: MockTenantRepository;
  let handler: GetAllTenantsQueryHandler;
  let tenants: TenantAggregate[];

  beforeEach(() => {
    repository = new MockTenantRepository();
    handler = new GetAllTenantsQueryHandler(repository);

    // Create multiple tenants for testing
    tenants = [
      TenantAggregate.create('Tenant 1', 'Description 1'),
      TenantAggregate.create('Tenant 2', 'Description 2'),
      TenantAggregate.create('Tenant 3', 'Description 3'),
      TenantAggregate.create('Tenant 4', 'Description 4'),
      TenantAggregate.create('Tenant 5', 'Description 5'),
    ];
    
    // Mock the repository to return these tenants
    repository.mockMultipleTenants(tenants);
  });

  it('should return paginated response with default pagination', async () => {
    // Arrange
    const query = new GetAllTenantsQuery(); // Default page 1, limit 10

    // Act
    const result = await handler.execute(query);

    // Assert
    expect(result).toBeDefined();
    expect(result.items.length).toBe(5); // All 5 tenants
    expect(result.total).toBe(5);
    expect(result.page).toBe(1);
    expect(result.limit).toBe(10);
    expect(result.pageCount).toBe(1);

    // Check if returned items are correctly mapped to DTOs
    expect(result.items[0].name).toBe('Tenant 1');
    expect(result.items[1].name).toBe('Tenant 2');
  });

  it('should return paginated response with custom pagination', async () => {
    // Arrange - request page 1 with limit 2
    const query = new GetAllTenantsQuery(1, 2);

    // Act
    const result = await handler.execute(query);

    // Assert
    expect(result).toBeDefined();
    expect(result.items.length).toBe(2); // Only first 2 tenants
    expect(result.total).toBe(5);
    expect(result.page).toBe(1);
    expect(result.limit).toBe(2);
    expect(result.pageCount).toBe(3); // Ceil(5/2) = 3 pages

    // Check if returned items are correctly mapped to DTOs
    expect(result.items[0].name).toBe('Tenant 1');
    expect(result.items[1].name).toBe('Tenant 2');
  });

  it('should return paginated response for second page', async () => {
    // Arrange - request page 2 with limit 2
    const query = new GetAllTenantsQuery(2, 2);

    // Act
    const result = await handler.execute(query);

    // Assert
    expect(result).toBeDefined();
    expect(result.items.length).toBe(2); // Only tenants 3-4
    expect(result.total).toBe(5);
    expect(result.page).toBe(2);
    expect(result.limit).toBe(2);
    expect(result.pageCount).toBe(3);

    // Check if returned items are correctly mapped to DTOs
    expect(result.items[0].name).toBe('Tenant 3');
    expect(result.items[1].name).toBe('Tenant 4');
  });

  it('should return last page with fewer items', async () => {
    // Arrange - request page 3 with limit 2
    const query = new GetAllTenantsQuery(3, 2);

    // Act
    const result = await handler.execute(query);

    // Assert
    expect(result).toBeDefined();
    expect(result.items.length).toBe(1); // Only tenant 5
    expect(result.page).toBe(3);
    expect(result.limit).toBe(2);
    expect(result.pageCount).toBe(3);

    // Check if returned items are correctly mapped to DTOs
    expect(result.items[0].name).toBe('Tenant 5');
  });

  it('should return empty items array when page is out of bounds', async () => {
    // Arrange - request page 4 with limit 2 (out of bounds)
    const query = new GetAllTenantsQuery(4, 2);

    // Act
    const result = await handler.execute(query);

    // Assert
    expect(result).toBeDefined();
    expect(result.items.length).toBe(0);
    expect(result.total).toBe(5);
    expect(result.page).toBe(4);
    expect(result.limit).toBe(2);
    expect(result.pageCount).toBe(3);
  });
}); 
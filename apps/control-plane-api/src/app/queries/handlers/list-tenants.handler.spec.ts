import { TestBed } from '@suites/unit';
import { Mocked } from '@suites/doubles.jest';
import { ListTenantsHandler } from './list-tenants.handler';
import { TenantsService } from '../../tenants/tenants.service';
import { ListTenantsQuery } from '../impl/list-tenants.query';
import { Tenant, TenantStatus } from '../../tenants/entities/tenant.entity';

describe('ListTenantsHandler (Suites)', () => {
  let underTest: ListTenantsHandler;
  let tenantsService: Mocked<TenantsService>;

  beforeAll(async () => {
    // Solitary test für den Handler mit gemocktem Service
    const { unit, unitRef } = await TestBed.solitary<ListTenantsHandler>(ListTenantsHandler).compile();
    
    underTest = unit;
    tenantsService = unitRef.get(TenantsService);
  });

  beforeEach(() => {
    // Reset mocks before each test
    jest.clearAllMocks();
  });

  it('should call tenantsService.findAll when activeOnly is false', async () => {
    // Arrange
    const mockTenants: Tenant[] = [
      {
        id: 'tenant-1',
        name: 'Tenant 1',
        description: 'First tenant',
        contactEmail: 'tenant1@example.com',
        configuration: {},
        status: undefined,
        createdAt: new Date(),
        updatedAt: new Date(),
      } as Tenant,
      {
        id: 'tenant-2',
        name: 'Tenant 2',
        description: 'Second tenant',
        contactEmail: 'tenant2@example.com',
        configuration: {},
        status: undefined,
        createdAt: new Date(),
        updatedAt: new Date(),
      } as Tenant,
    ];
    
    tenantsService.findAll.mockResolvedValue(mockTenants);
    
    const query = new ListTenantsQuery(false);
    
    // Act
    const result = await underTest.execute(query);
    
    // Assert
    expect(tenantsService.findAll).toHaveBeenCalled();
    expect(tenantsService.findActive).not.toHaveBeenCalled();
    expect(result).toEqual(mockTenants);
  });

  it('should call tenantsService.findActive when activeOnly is true', async () => {
    // Arrange
    const mockActiveTenants: Tenant[] = [
      {
        id: 'tenant-1',
        name: 'Active Tenant',
        description: 'An active tenant',
        contactEmail: 'active@example.com',
        configuration: {},
        status: TenantStatus.ACTIVE,
        createdAt: new Date(),
        updatedAt: new Date(),
      } as Tenant,
    ];
    
    tenantsService.findActive.mockResolvedValue(mockActiveTenants);
    
    const query = new ListTenantsQuery(true);
    
    // Act
    const result = await underTest.execute(query);
    
    // Assert
    expect(tenantsService.findActive).toHaveBeenCalledTimes(1);
    expect(result).toEqual(mockActiveTenants);
  });
}); 
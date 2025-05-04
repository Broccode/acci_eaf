import { TestBed } from '@suites/unit';
import { Mocked } from '@suites/doubles.jest';
import { GetTenantByIdHandler } from './get-tenant-by-id.handler';
import { TenantsService } from '../../tenants/tenants.service';
import { GetTenantByIdQuery } from '../impl/get-tenant-by-id.query';
import { Tenant } from '../../tenants/entities/tenant.entity';

describe('GetTenantByIdHandler (Suites)', () => {
  let underTest: GetTenantByIdHandler;
  let tenantsService: Mocked<TenantsService>;

  beforeAll(async () => {
    // Solitary test for the handler with a mocked service
    const { unit, unitRef } = await TestBed.solitary<GetTenantByIdHandler>(GetTenantByIdHandler).compile();
    
    underTest = unit;
    tenantsService = unitRef.get(TenantsService);
  });

  it('should call tenantsService.findOne with correct id and return the tenant', async () => {
    // Arrange
    const mockTenant: Tenant = {
      id: 'test-uuid',
      name: 'TestTenant',
      description: 'TestDescription',
      contactEmail: 'test@example.com',
      configuration: {},
      status: undefined,
      createdAt: new Date(),
      updatedAt: new Date(),
    } as Tenant;
    
    tenantsService.findOne.mockResolvedValue(mockTenant);
    
    const query = new GetTenantByIdQuery('test-uuid');
    
    // Act
    const result = await underTest.execute(query);
    
    // Assert
    expect(tenantsService.findOne).toHaveBeenCalledWith('test-uuid');
    expect(result).toEqual(mockTenant);
  });
}); 
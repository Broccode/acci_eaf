import { TestBed } from '@suites/unit';
import { Mocked } from '@suites/doubles.jest';
import { CreateTenantHandler } from './create-tenant.handler';
import { TenantsService } from '../../tenants/tenants.service';
import { CreateTenantCommand } from '../impl/create-tenant.command';
import { Tenant } from '../../tenants/entities/tenant.entity';

describe('CreateTenantHandler (Suites)', () => {
  let underTest: CreateTenantHandler;
  let tenantsService: Mocked<TenantsService>;

  beforeAll(async () => {
    // Automatisches Mocking aller Dependencies mit solitary Test
    const { unit, unitRef } = await TestBed.solitary<CreateTenantHandler>(CreateTenantHandler).compile();
    
    underTest = unit;
    tenantsService = unitRef.get(TenantsService);
  });

  it('should call tenantsService.create with correct params and return the created tenant', async () => {
    // Arrange
    const mockTenant: Tenant = {
      id: 'uuid',
      name: 'TestTenant',
      description: 'TestDescription',
      contactEmail: 'test@example.com',
      configuration: {},
      status: undefined,
      createdAt: new Date(),
      updatedAt: new Date(),
    } as Tenant;
    
    tenantsService.create.mockResolvedValue(mockTenant);
    
    const command = new CreateTenantCommand('TestTenant', 'TestDescription', 'test@example.com', {});
    
    // Act
    const result = await underTest.execute(command);
    
    // Assert
    expect(tenantsService.create).toHaveBeenCalledWith({
      name: 'TestTenant',
      description: 'TestDescription',
      contactEmail: 'test@example.com',
      configuration: {},
    });
    expect(result).toEqual(mockTenant);
  });
}); 
import { TestBed } from '@suites/unit';
import { Mocked } from '@suites/doubles.jest';
import { UpdateTenantHandler } from './update-tenant.handler';
import { TenantsService } from '../../tenants/tenants.service';
import { UpdateTenantCommand } from '../impl/update-tenant.command';
import { Tenant } from '../../tenants/entities/tenant.entity';

describe('UpdateTenantHandler (Suites)', () => {
  let underTest: UpdateTenantHandler;
  let tenantsService: Mocked<TenantsService>;

  beforeAll(async () => {
    // Automatisches Mocking aller Dependencies mit solitary Test
    const { unit, unitRef } = await TestBed.solitary<UpdateTenantHandler>(UpdateTenantHandler).compile();
    
    underTest = unit;
    tenantsService = unitRef.get(TenantsService);
  });

  it('should call tenantsService.update with correct params and return the updated tenant', async () => {
    // Arrange
    const mockTenant: Tenant = {
      id: 'uuid',
      name: 'UpdatedName',
      description: 'UpdatedDesc',
      contactEmail: 'updated@example.com',
      configuration: {},
      status: undefined,
      createdAt: new Date(),
      updatedAt: new Date(),
    } as Tenant;
    
    tenantsService.update.mockResolvedValue(mockTenant);
    
    const command = new UpdateTenantCommand(
      'uuid',
      'UpdatedName',
      'UpdatedDesc',
      'updated@example.com',
      {},
    );
    
    // Act
    const result = await underTest.execute(command);
    
    // Assert
    expect(tenantsService.update).toHaveBeenCalledWith('uuid', {
      name: 'UpdatedName',
      description: 'UpdatedDesc',
      contactEmail: 'updated@example.com',
      configuration: {},
    });
    expect(result).toEqual(mockTenant);
  });
}); 
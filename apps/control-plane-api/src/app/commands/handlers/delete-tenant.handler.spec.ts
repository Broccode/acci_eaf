import { TestBed } from '@suites/unit';
import { Mocked } from '@suites/doubles.jest';
import { DeleteTenantHandler } from './delete-tenant.handler';
import { TenantsService } from '../../tenants/tenants.service';
import { DeleteTenantCommand } from '../impl/delete-tenant.command';

describe('DeleteTenantHandler (Suites)', () => {
  let underTest: DeleteTenantHandler;
  let tenantsService: Mocked<TenantsService>;

  beforeAll(async () => {
    // Automatisches Mocking aller Dependencies mit solitary Test
    const { unit, unitRef } = await TestBed.solitary<DeleteTenantHandler>(DeleteTenantHandler).compile();
    
    underTest = unit;
    tenantsService = unitRef.get(TenantsService);
  });

  it('should call tenantsService.remove with correct id and return void', async () => {
    // Arrange
    tenantsService.remove.mockResolvedValue(undefined);
    const command = new DeleteTenantCommand('uuid');
    
    // Act
    const result = await underTest.execute(command);
    
    // Assert
    expect(tenantsService.remove).toHaveBeenCalledWith('uuid');
    expect(result).toBeUndefined();
  });
}); 
import { TestBed } from '@suites/unit';
import { UserPermissionsService } from './user-permissions.service';
import { Action } from '../types';

describe('UserPermissionsService', () => {
  let underTest: UserPermissionsService;

  beforeAll(async () => {
    // Erstelle den Service als Unit
    const { unit } = await TestBed.solitary(UserPermissionsService).compile();
    underTest = unit;
  });

  it('should be defined', () => {
    expect(underTest).toBeDefined();
  });

  describe('getUserPermissions', () => {
    it('should return user permissions with roles', async () => {
      // Arrange - use the default roles created in the service constructor

      // Act
      const adminPermissions = await underTest.getUserPermissions('admin-user');
      const managerPermissions = await underTest.getUserPermissions('manager-user');
      const userPermissions = await underTest.getUserPermissions('regular-user');

      // Assert
      expect(adminPermissions.roles.length).toBe(1);
      expect(adminPermissions.roles[0].name).toBe('Administrator');
      expect(adminPermissions.roles[0].permissions.length).toBe(1);
      expect(adminPermissions.roles[0].permissions[0].action).toBe(Action.Manage);
      expect(adminPermissions.roles[0].permissions[0].resource).toBe('all');

      expect(managerPermissions.roles.length).toBe(1);
      expect(managerPermissions.roles[0].name).toBe('Tenant Manager');
      expect(managerPermissions.roles[0].permissions.length).toBe(3);
      
      expect(userPermissions.roles.length).toBe(1);
      expect(userPermissions.roles[0].name).toBe('User');
      expect(userPermissions.roles[0].permissions.length).toBe(2);
    });

    it('should filter roles by tenant ID when specified', async () => {
      // Arrange
      const userId = 'multi-tenant-user';
      const tenantId1 = 'tenant-1';
      const tenantId2 = 'tenant-2';
      
      // Manually assign role for testing
      await underTest.assignRoleToUser(userId, 'tenant-manager');
      
      // Act
      const permissionsWithoutTenant = await underTest.getUserPermissions(userId);
      const permissionsWithTenant1 = await underTest.getUserPermissions(userId, tenantId1);
      const permissionsWithTenant2 = await underTest.getUserPermissions(userId, tenantId2);

      // Assert
      expect(permissionsWithoutTenant.roles.length).toBe(1);
      
      // Since the default roles don't have tenant IDs, they should be included
      // when no tenant filter is applied, but not when a specific tenant is requested
      expect(permissionsWithTenant1.roles.length).toBe(0);
      expect(permissionsWithTenant2.roles.length).toBe(0);
    });

    it('should return empty arrays for non-existing users', async () => {
      // Act
      const permissions = await underTest.getUserPermissions('non-existing-user');

      // Assert
      expect(permissions.roles).toEqual([]);
      expect(permissions.directPermissions).toEqual([]);
    });
  });

  describe('assignRoleToUser', () => {
    it('should assign role to user', async () => {
      // Arrange
      const userId = 'test-user';

      // Act
      await underTest.assignRoleToUser(userId, 'admin');
      const permissions = await underTest.getUserPermissions(userId);

      // Assert
      expect(permissions.roles.length).toBe(1);
      expect(permissions.roles[0].id).toBe('admin');
    });

    it('should not duplicate role assignments', async () => {
      // Arrange
      const userId = 'test-user-2';

      // Act
      await underTest.assignRoleToUser(userId, 'user');
      await underTest.assignRoleToUser(userId, 'user'); // Assign same role again
      const permissions = await underTest.getUserPermissions(userId);

      // Assert
      expect(permissions.roles.length).toBe(1);
      expect(permissions.roles[0].id).toBe('user');
    });

    it('should throw error for non-existing role', async () => {
      // Arrange
      const userId = 'test-user-3';
      const nonExistingRoleId = 'non-existing-role';

      // Act & Assert
      await expect(underTest.assignRoleToUser(userId, nonExistingRoleId))
        .rejects
        .toThrow(`Role with ID ${nonExistingRoleId} not found`);
    });
  });
}); 
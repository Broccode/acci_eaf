import { Injectable } from '@nestjs/common';
import { Role, UserPermissions, Permission, Action, Resource } from '../types';

/**
 * Service for retrieving user permissions
 * In a real application, this would interact with a database
 */
@Injectable()
export class UserPermissionsService {
  // For demonstration, we're using in-memory storage
  // In a real application, this would use MikroORM or another data source
  private readonly roles: Map<string, Role> = new Map();
  private readonly userRoles: Map<string, string[]> = new Map();
  private readonly userPermissions: Map<string, Permission[]> = new Map();

  constructor() {
    // Initialize with some default roles and permissions
    this.initDefaultRoles();
  }

  /**
   * Get all permissions for a user
   * 
   * @param userId The ID of the user
   * @param tenantId The ID of the tenant
   * @returns The user's permissions, including roles and direct permissions
   */
  async getUserPermissions(userId: string, tenantId?: string): Promise<UserPermissions> {
    // Get role IDs assigned to the user
    const roleIds = this.userRoles.get(userId) || [];
    
    // Get the actual role objects
    const roles: Role[] = roleIds
      .map(id => this.roles.get(id))
      .filter(role => role !== undefined && (!tenantId || role.tenantId === tenantId)) as Role[];
    
    // Get direct permissions assigned to the user
    const directPermissions = this.userPermissions.get(userId) || [];
    
    return {
      userId,
      tenantId,
      roles,
      directPermissions,
    };
  }

  /**
   * Assign a role to a user
   * 
   * @param userId The ID of the user
   * @param roleId The ID of the role
   */
  async assignRoleToUser(userId: string, roleId: string): Promise<void> {
    // Check if the role exists
    if (!this.roles.has(roleId)) {
      throw new Error(`Role with ID ${roleId} not found`);
    }
    
    // Get or initialize user's roles
    if (!this.userRoles.has(userId)) {
      this.userRoles.set(userId, []);
    }
    
    // Add the role if not already assigned
    const userRoles = this.userRoles.get(userId) as string[];
    if (!userRoles.includes(roleId)) {
      userRoles.push(roleId);
    }
  }

  /**
   * Initialize default roles and permissions
   * In a real application, these would be loaded from a database
   */
  private initDefaultRoles(): void {
    // Admin role
    const adminRole: Role = {
      id: 'admin',
      name: 'Administrator',
      description: 'Full system access',
      permissions: [
        { action: Action.Manage, resource: 'all' }
      ],
    };
    
    // Tenant Manager role
    const tenantManagerRole: Role = {
      id: 'tenant-manager',
      name: 'Tenant Manager',
      description: 'Can manage tenant settings',
      permissions: [
        { action: Action.Read, resource: 'tenant' },
        { action: Action.Update, resource: 'tenant' },
        { action: Action.Read, resource: 'user' },
      ],
    };
    
    // User role
    const userRole: Role = {
      id: 'user',
      name: 'User',
      description: 'Basic user access',
      permissions: [
        { action: Action.Read, resource: 'tenant' },
        { 
          action: Action.Read, 
          resource: 'user',
          condition: { id: '${user.id}' } // Can only read own user data
        },
      ],
    };
    
    // Store the roles
    this.roles.set(adminRole.id as string, adminRole);
    this.roles.set(tenantManagerRole.id as string, tenantManagerRole);
    this.roles.set(userRole.id as string, userRole);
    
    // Assign admin role to a test admin user
    this.userRoles.set('admin-user', ['admin']);
    
    // Assign tenant manager role to a test manager user
    this.userRoles.set('manager-user', ['tenant-manager']);
    
    // Assign user role to a test regular user
    this.userRoles.set('regular-user', ['user']);
  }
} 
import { Plugin, PluginInfo } from 'core';
import { PluginMigration } from 'infrastructure';

/**
 * Represents a role-based permission
 */
export interface Permission {
  /**
   * Unique identifier for the permission
   */
  id: string;
  
  /**
   * Human-readable name of the permission
   */
  name: string;
  
  /**
   * Description of what the permission allows
   */
  description: string;
  
  /**
   * Resource that the permission applies to (e.g., 'users', 'invoices')
   */
  resource: string;
  
  /**
   * Action that the permission allows (e.g., 'read', 'write', 'delete')
   */
  action: string;
}

/**
 * Authorization service exposed by the plugin
 */
export interface AuthorizationService {
  /**
   * Check if a user has a specific permission
   * @param userId User identifier
   * @param permissionId Permission identifier
   * @returns Promise resolving to true if the user has the permission
   */
  hasPermission(userId: string, permissionId: string): Promise<boolean>;
  
  /**
   * Grant a permission to a user
   * @param userId User identifier
   * @param permissionId Permission identifier
   */
  grantPermission(userId: string, permissionId: string): Promise<void>;
  
  /**
   * Revoke a permission from a user
   * @param userId User identifier
   * @param permissionId Permission identifier
   */
  revokePermission(userId: string, permissionId: string): Promise<void>;
  
  /**
   * Register a new permission
   * @param permission Permission to register
   */
  registerPermission(permission: Permission): Promise<void>;
  
  /**
   * Get all available permissions
   * @returns List of all registered permissions
   */
  getPermissions(): Promise<Permission[]>;
}

/**
 * Implementation of the authorization service
 * This is a simplified in-memory implementation for demonstration purposes
 */
class InMemoryAuthorizationService implements AuthorizationService {
  private permissions: Map<string, Permission> = new Map();
  private userPermissions: Map<string, Set<string>> = new Map();
  
  async hasPermission(userId: string, permissionId: string): Promise<boolean> {
    const userPerms = this.userPermissions.get(userId);
    return userPerms?.has(permissionId) || false;
  }
  
  async grantPermission(userId: string, permissionId: string): Promise<void> {
    // Ensure the permission exists
    if (!this.permissions.has(permissionId)) {
      throw new Error(`Permission "${permissionId}" does not exist`);
    }
    
    // Ensure user permissions set exists
    if (!this.userPermissions.has(userId)) {
      this.userPermissions.set(userId, new Set());
    }
    
    // Grant the permission
    this.userPermissions.get(userId)!.add(permissionId);
  }
  
  async revokePermission(userId: string, permissionId: string): Promise<void> {
    const userPerms = this.userPermissions.get(userId);
    if (userPerms) {
      userPerms.delete(permissionId);
    }
  }
  
  async registerPermission(permission: Permission): Promise<void> {
    if (this.permissions.has(permission.id)) {
      throw new Error(`Permission "${permission.id}" already exists`);
    }
    
    this.permissions.set(permission.id, permission);
  }
  
  async getPermissions(): Promise<Permission[]> {
    return Array.from(this.permissions.values());
  }
}

/**
 * Migration to create initial permissions
 */
const createInitialPermissionsV1: PluginMigration = {
  version: '1.0.0',
  async up() {
    console.log('Running initial permissions migration');
    // In a real implementation, this would create permissions in a database
  },
  
  async down() {
    console.log('Rolling back initial permissions migration');
    // In a real implementation, this would remove the permissions from the database
  }
};

/**
 * Example Authorization Plugin that provides role-based access control
 */
export class AuthorizationPlugin implements Plugin {
  name = 'authorization-plugin';
  version = '1.0.0';
  description = 'Provides role-based access control for the application';
  
  // Service instance that will be created on register
  private authService: AuthorizationService | null = null;
  
  // Migrations to run when the plugin is loaded
  public readonly migrations: PluginMigration[] = [
    createInitialPermissionsV1
  ];
  
  /**
   * Register the plugin and initialize the authorization service
   */
  async register(): Promise<void> {
    console.log(`Registering ${this.name} v${this.version}`);
    
    // Create the authorization service
    this.authService = new InMemoryAuthorizationService();
    
    // Register some default permissions
    await this.registerDefaultPermissions();
    
    console.log(`${this.name} registered successfully`);
  }
  
  /**
   * Clean up resources when the plugin is unloaded
   */
  async unregister(): Promise<void> {
    console.log(`Unregistering ${this.name}`);
    // Clean up resources if needed
    this.authService = null;
  }
  
  /**
   * Check if the plugin is compatible with the given framework version
   */
  isCompatible(frameworkVersion: string): boolean {
    // This plugin works with framework versions 1.x
    return frameworkVersion.startsWith('1.');
  }
  
  /**
   * Get the authorization service provided by this plugin
   */
  getAuthorizationService(): AuthorizationService {
    if (!this.authService) {
      throw new Error('Authorization service not initialized. Plugin not registered.');
    }
    return this.authService;
  }
  
  /**
   * Get additional information about the plugin
   */
  getInfo(): PluginInfo {
    return {
      author: 'ACCI EAF Team',
      license: 'MIT',
      homepage: 'https://github.com/acci-org/eaf',
      keywords: ['authorization', 'security', 'rbac'],
      dependencies: {
        'core': '>=1.0.0'
      }
    };
  }
  
  /**
   * Register default permissions
   * This is called during plugin registration
   */
  private async registerDefaultPermissions(): Promise<void> {
    if (!this.authService) return;
    
    // Register some basic permissions
    const defaultPermissions: Permission[] = [
      {
        id: 'user:read',
        name: 'Read Users',
        description: 'Allows reading user data',
        resource: 'user',
        action: 'read'
      },
      {
        id: 'user:write',
        name: 'Write Users',
        description: 'Allows creating and updating users',
        resource: 'user',
        action: 'write'
      },
      {
        id: 'user:delete',
        name: 'Delete Users',
        description: 'Allows deleting users',
        resource: 'user',
        action: 'delete'
      }
    ];
    
    // Register each permission
    for (const permission of defaultPermissions) {
      await this.authService.registerPermission(permission);
    }
  }
} 
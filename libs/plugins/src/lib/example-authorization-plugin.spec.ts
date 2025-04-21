import { AuthorizationPlugin, Permission } from './example-authorization-plugin';

describe('AuthorizationPlugin', () => {
  let plugin: AuthorizationPlugin;

  beforeEach(() => {
    plugin = new AuthorizationPlugin();
  });

  it('should have correct metadata', () => {
    expect(plugin.name).toBe('authorization-plugin');
    expect(plugin.version).toBe('1.0.0');
    expect(plugin.description).toBeDefined();
  });

  it('should initialize with migrations', () => {
    expect(plugin.migrations).toBeDefined();
    expect(plugin.migrations.length).toBeGreaterThan(0);
    expect(plugin.migrations[0].version).toBe('1.0.0');
  });

  it('should be compatible with framework version 1.x', () => {
    expect(plugin.isCompatible('1.0.0')).toBe(true);
    expect(plugin.isCompatible('1.5.2')).toBe(true);
    expect(plugin.isCompatible('2.0.0')).toBe(false);
  });

  it('should return plugin info', () => {
    const info = plugin.getInfo();
    expect(info.author).toBeDefined();
    expect(info.license).toBeDefined();
    expect(info.keywords).toContain('authorization');
  });

  it('should throw error when trying to get service before registration', () => {
    expect(() => plugin.getAuthorizationService()).toThrow();
  });

  describe('after registration', () => {
    beforeEach(async () => {
      // Register the plugin
      await plugin.register();
    });

    it('should provide an authorization service', () => {
      const service = plugin.getAuthorizationService();
      expect(service).toBeDefined();
    });

    it('should have registered default permissions', async () => {
      const service = plugin.getAuthorizationService();
      const permissions = await service.getPermissions();
      
      expect(permissions.length).toBeGreaterThan(0);
      expect(permissions.some(p => p.id === 'user:read')).toBe(true);
      expect(permissions.some(p => p.id === 'user:write')).toBe(true);
      expect(permissions.some(p => p.id === 'user:delete')).toBe(true);
    });

    it('should allow granting and checking permissions', async () => {
      const service = plugin.getAuthorizationService();
      const userId = 'test-user-1';
      
      // Initially user should have no permissions
      expect(await service.hasPermission(userId, 'user:read')).toBe(false);
      
      // Grant permission
      await service.grantPermission(userId, 'user:read');
      
      // Now user should have the permission
      expect(await service.hasPermission(userId, 'user:read')).toBe(true);
      
      // But not other permissions
      expect(await service.hasPermission(userId, 'user:write')).toBe(false);
    });

    it('should allow revoking permissions', async () => {
      const service = plugin.getAuthorizationService();
      const userId = 'test-user-2';
      
      // Grant permissions
      await service.grantPermission(userId, 'user:read');
      await service.grantPermission(userId, 'user:write');
      
      // User should have both permissions
      expect(await service.hasPermission(userId, 'user:read')).toBe(true);
      expect(await service.hasPermission(userId, 'user:write')).toBe(true);
      
      // Revoke one permission
      await service.revokePermission(userId, 'user:read');
      
      // User should only have the other permission
      expect(await service.hasPermission(userId, 'user:read')).toBe(false);
      expect(await service.hasPermission(userId, 'user:write')).toBe(true);
    });

    it('should allow registering new permissions', async () => {
      const service = plugin.getAuthorizationService();
      
      // Register a new permission
      const newPermission: Permission = {
        id: 'report:generate',
        name: 'Generate Reports',
        description: 'Allows generating reports',
        resource: 'report',
        action: 'generate'
      };
      
      await service.registerPermission(newPermission);
      
      // The new permission should be in the list
      const permissions = await service.getPermissions();
      const foundPermission = permissions.find(p => p.id === 'report:generate');
      
      expect(foundPermission).toBeDefined();
      expect(foundPermission?.name).toBe('Generate Reports');
    });
  });
}); 
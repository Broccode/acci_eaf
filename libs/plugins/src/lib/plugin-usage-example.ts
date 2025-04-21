import { PluginLoader } from 'infrastructure';
import { AuthorizationPlugin } from './example-authorization-plugin';

/**
 * This example demonstrates how to use the plugin system with the
 * AuthorizationPlugin for role-based access control
 */
export async function demoPluginUsage(): Promise<void> {
  console.log('=== Plugin System Demo ===');
  
  // Create a plugin loader with the framework version
  const pluginLoader = new PluginLoader('1.0.0');
  
  // Register for events to see what's happening
  pluginLoader.onEvent((event) => {
    if (event.type === 'plugin-loaded') {
      console.log(`Plugin loaded: ${event.plugin.name} v${event.plugin.version}`);
    } else if (event.type === 'migration-applied') {
      console.log(`Migration applied for plugin: ${event.plugin.name}`);
    }
  });
  
  // Create and load the authorization plugin
  const authPlugin = new AuthorizationPlugin();
  await pluginLoader.load(authPlugin);
  
  // Now we can use the authorization service provided by the plugin
  const authService = authPlugin.getAuthorizationService();
  
  // List all available permissions
  const permissions = await authService.getPermissions();
  console.log('Available permissions:');
  permissions.forEach(p => {
    console.log(`- ${p.name} (${p.id}): ${p.description}`);
  });
  
  // Grant permissions to a user
  const userId = 'user-123';
  await authService.grantPermission(userId, 'user:read');
  await authService.grantPermission(userId, 'user:write');
  
  // Check user permissions
  const canReadUsers = await authService.hasPermission(userId, 'user:read');
  const canDeleteUsers = await authService.hasPermission(userId, 'user:delete');
  
  console.log(`\nUser ${userId} permissions:`);
  console.log(`- Can read users: ${canReadUsers}`);
  console.log(`- Can delete users: ${canDeleteUsers}`);
  
  // Example of how to use the authorization in a hypothetical API endpoint
  console.log('\nExample authorization checks:');
  
  const exampleUserRequest = async (requestUserId: string, action: string) => {
    const permissionId = `user:${action}`;
    const hasPermission = await authService.hasPermission(requestUserId, permissionId);
    
    if (hasPermission) {
      console.log(`User ${requestUserId} authorized to ${action} users`);
      return true;
    } else {
      console.log(`User ${requestUserId} not authorized to ${action} users`);
      return false;
    }
  };
  
  // These should succeed as we granted these permissions
  await exampleUserRequest(userId, 'read');
  await exampleUserRequest(userId, 'write');
  
  // This should fail as we didn't grant delete permission
  await exampleUserRequest(userId, 'delete');
  
  // Unload the plugin when done
  await pluginLoader.unload(authPlugin.name);
  console.log(`\nPlugin ${authPlugin.name} unloaded`);
}

// This would be called from the app's entry point
export async function runDemo(): Promise<void> {
  try {
    await demoPluginUsage();
  } catch (error) {
    console.error('Error in plugin demo:', error);
  }
} 
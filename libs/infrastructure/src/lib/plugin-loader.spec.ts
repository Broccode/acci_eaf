import { PluginLoader, PluginLoadError, PluginLoaderEvent } from './plugin-loader';
import { Plugin } from 'core';
import { PluginMigration } from './plugin-migration.interface';

describe('PluginLoader', () => {
  it('should load a plugin and run migrations if present', async () => {
    const migrationsRun: string[] = [];
    const plugin: Plugin & { migrations: PluginMigration[] } = {
      name: 'TestPlugin',
      version: '1.0.0',
      async register() { migrationsRun.push('register'); },
      migrations: [
        { 
          version: '1.0.0', 
          up: async () => { 
            migrationsRun.push('migration-up');
          }
        },
      ],
    };
    const loader = new PluginLoader();
    await loader.load(plugin);
    expect(migrationsRun).toEqual(['migration-up', 'register']);
    expect(loader.getLoadedPlugins()).toHaveLength(1);
  });
  
  it('should emit events during plugin loading', async () => {
    // Arrange
    const plugin: Plugin = {
      name: 'EventTestPlugin',
      version: '1.0.0',
      async register() { /* */ },
    };
    
    const loader = new PluginLoader();
    const events: PluginLoaderEvent[] = [];
    
    loader.onEvent(event => {
      events.push(event);
    });
    
    // Act
    await loader.load(plugin);
    
    // Assert
    expect(events).toHaveLength(1);
    expect(events[0].type).toBe('plugin-loaded');
    expect((events[0] as {type: 'plugin-loaded', plugin: Plugin}).plugin).toBe(plugin);
  });
  
  it('should validate plugins before loading', async () => {
    // Arrange
    const invalidPlugin: Partial<Plugin> = {
      name: '', // Invalid: empty name
      version: '1.0.0',
      async register() { /* */ },
    };
    
    const loader = new PluginLoader();
    
    // Act & Assert
    await expect(loader.load(invalidPlugin as Plugin)).rejects.toThrow(PluginLoadError);
  });
  
  it('should prevent loading plugins with duplicate names', async () => {
    // Arrange
    const plugin1: Plugin = {
      name: 'DuplicatePlugin',
      version: '1.0.0',
      async register() { /* */ },
    };
    
    const plugin2: Plugin = {
      name: 'DuplicatePlugin', // Same name as plugin1
      version: '1.1.0',
      async register() { /* */ },
    };
    
    const loader = new PluginLoader();
    
    // Act
    await loader.load(plugin1);
    
    // Assert
    await expect(loader.load(plugin2)).rejects.toThrow(PluginLoadError);
    expect(loader.getLoadedPlugins()).toHaveLength(1);
  });
  
  it('should unload a plugin', async () => {
    // Arrange
    const unregisterCalled = jest.fn();
    const plugin: Plugin = {
      name: 'UnloadablePlugin',
      version: '1.0.0',
      async register() { /* */ },
      async unregister() { unregisterCalled(); },
    };
    
    const loader = new PluginLoader();
    await loader.load(plugin);
    
    // Act
    await loader.unload('UnloadablePlugin');
    
    // Assert
    expect(unregisterCalled).toHaveBeenCalled();
    expect(loader.getLoadedPlugins()).toHaveLength(0);
  });
  
  it('should check plugin compatibility with framework version', async () => {
    // Arrange
    const compatiblePlugin: Plugin = {
      name: 'CompatiblePlugin',
      version: '1.0.0',
      async register() { /* */ },
      isCompatible(version) { return version === '2.0.0'; },
    };
    
    const loader = new PluginLoader('2.0.0');
    const incompatibleLoader = new PluginLoader('1.0.0');
    
    // Act & Assert
    await expect(loader.load(compatiblePlugin)).resolves.toBeUndefined();
    await expect(incompatibleLoader.load(compatiblePlugin)).rejects.toThrow(PluginLoadError);
  });
  
  it('should allow retrieving a plugin by name', async () => {
    // Arrange
    const plugin: Plugin = {
      name: 'FindablePlugin',
      version: '1.0.0',
      async register() { /* */ },
    };
    
    const loader = new PluginLoader();
    await loader.load(plugin);
    
    // Act
    const foundPlugin = loader.getPlugin('FindablePlugin');
    const nonExistentPlugin = loader.getPlugin('NonExistentPlugin');
    
    // Assert
    expect(foundPlugin).toBe(plugin);
    expect(nonExistentPlugin).toBeUndefined();
  });

  // New tests for better coverage

  it('should emit error events when plugin loading fails', async () => {
    // Arrange
    const plugin: Plugin = {
      name: 'FailingPlugin',
      version: '1.0.0',
      async register() { 
        throw new Error('Registration failed');
      },
    };
    
    const loader = new PluginLoader();
    const events: PluginLoaderEvent[] = [];
    
    loader.onEvent(event => {
      events.push(event);
    });
    
    // Act
    try {
      await loader.load(plugin);
    } catch (error) {
      // We expect this to throw
    }
    
    // Assert
    expect(events.length).toBeGreaterThan(0);
    expect(events.some(e => e.type === 'error')).toBe(true);
    
    const errorEvent = events.find(e => e.type === 'error') as { type: 'error', error: Error };
    expect(errorEvent?.error).toBeInstanceOf(PluginLoadError);
    
    const pluginLoadError = errorEvent.error as PluginLoadError;
    expect(pluginLoadError.cause).toBeInstanceOf(Error);
    expect(pluginLoadError.cause?.message).toBe('Registration failed');
  });

  it('should emit events when migrations are applied', async () => {
    // Arrange
    const plugin: Plugin & { migrations: PluginMigration[] } = {
      name: 'MigrationEventPlugin',
      version: '1.0.0',
      async register() { /* */ },
      migrations: [
        { 
          version: '1.0.0', 
          up: async () => { /* */ }
        },
        { 
          version: '1.1.0', 
          up: async () => { /* */ }
        },
      ],
    };
    
    const loader = new PluginLoader();
    const events: PluginLoaderEvent[] = [];
    
    loader.onEvent(event => {
      events.push(event);
    });
    
    // Act
    await loader.load(plugin);
    
    // Assert
    const migrationEvents = events.filter(e => e.type === 'migration-applied');
    expect(migrationEvents).toHaveLength(2);

    // Type assertions for the events
    const typedEvent1 = migrationEvents[0] as {
      type: 'migration-applied'; 
      plugin: Plugin; 
      migration: PluginMigration
    };
    const typedEvent2 = migrationEvents[1] as {
      type: 'migration-applied'; 
      plugin: Plugin; 
      migration: PluginMigration
    };
    
    expect(typedEvent1.plugin).toBe(plugin);
    expect(typedEvent1.migration).toBe(plugin.migrations[0]);
    expect(typedEvent2.migration).toBe(plugin.migrations[1]);
  });

  it('should handle errors in event handlers gracefully', async () => {
    // Arrange
    const plugin: Plugin = {
      name: 'EventErrorPlugin',
      version: '1.0.0',
      async register() { /* */ },
    };
    
    const loader = new PluginLoader();
    const errorHandlerCalled = jest.fn();
    
    // First handler throws an error
    loader.onEvent(() => {
      throw new Error('Event handler error');
    });
    
    // Second handler should still be called
    loader.onEvent(() => {
      errorHandlerCalled();
    });
    
    // Mock console.error to not pollute test output
    const originalConsoleError = console.error;
    console.error = jest.fn();
    
    // Act
    await loader.load(plugin);
    
    // Restore console.error
    console.error = originalConsoleError;
    
    // Assert - second handler should still be called despite first handler error
    expect(errorHandlerCalled).toHaveBeenCalled();
  });

  it('should register and unregister event handlers', () => {
    // Arrange
    const loader = new PluginLoader();
    const handler1Called = jest.fn();
    const handler2Called = jest.fn();
    
    // Act
    const unregisterHandler1 = loader.onEvent(() => handler1Called());
    const unregisterHandler2 = loader.onEvent(() => handler2Called());
    
    // Unregister the first handler
    unregisterHandler1();
    
    // Trigger an event by loading a plugin
    const plugin: Plugin = {
      name: 'EventHandlerTestPlugin',
      version: '1.0.0',
      async register() { /* */ },
    };
    
    // Act & Assert
    return loader.load(plugin).then(() => {
      // Handler1 should not be called, handler2 should be called
      expect(handler1Called).not.toHaveBeenCalled();
      expect(handler2Called).toHaveBeenCalled();
    });
  });

  it('should throw when unloading a non-existent plugin', async () => {
    // Arrange
    const loader = new PluginLoader();
    
    // Act & Assert
    await expect(loader.unload('NonExistentPlugin')).rejects.toThrow(
      'Plugin "NonExistentPlugin" is not loaded'
    );
  });

  it('should emit error event when unload fails', async () => {
    // Arrange
    const plugin: Plugin = {
      name: 'UnloadFailPlugin',
      version: '1.0.0',
      async register() { /* */ },
      async unregister() { 
        throw new Error('Unload failed');
      },
    };
    
    const loader = new PluginLoader();
    await loader.load(plugin);
    
    const events: PluginLoaderEvent[] = [];
    loader.onEvent(event => {
      events.push(event);
    });
    
    // Act
    try {
      await loader.unload('UnloadFailPlugin');
    } catch (error) {
      // We expect this to throw
    }
    
    // Assert
    expect(events.some(e => e.type === 'error')).toBe(true);
    
    const errorEvent = events.find(e => e.type === 'error') as { type: 'error', error: Error };
    expect(errorEvent?.error.message).toContain('Failed to unload plugin');
    expect(errorEvent?.error.message).toContain('Unload failed');
  });

  it('should validate plugin has required register method', async () => {
    // Arrange
    const invalidPlugin: Partial<Plugin> = {
      name: 'InvalidPlugin',
      version: '1.0.0',
      // Missing register method
    };
    
    const loader = new PluginLoader();
    
    // Act & Assert
    await expect(loader.load(invalidPlugin as Plugin)).rejects.toThrow(
      'Plugin register method is required'
    );
  });

  it('should validate plugin version is required', async () => {
    // Arrange
    const invalidPlugin: Partial<Plugin> = {
      name: 'InvalidPlugin',
      // Missing version
      async register() { /* */ },
    };
    
    const loader = new PluginLoader();
    
    // Act & Assert
    await expect(loader.load(invalidPlugin as Plugin)).rejects.toThrow(
      'Plugin version is required'
    );
  });

  it('should support a full plugin lifecycle with multiple plugins', async () => {
    // Arrange - Create multiple plugins with different properties
    let pluginAInitialized = false;
    let pluginACleanedUp = false;
    let pluginBInitialized = false;
    
    const pluginA: Plugin = {
      name: 'PluginA',
      version: '1.0.0',
      description: 'Test Plugin A',
      async register() {
        pluginAInitialized = true;
      },
      async unregister() {
        pluginACleanedUp = true;
      },
      isCompatible(version) {
        return version.startsWith('1.');
      },
      getInfo() {
        return {
          author: 'Test Author',
          license: 'MIT'
        };
      }
    };
    
    const pluginB: Plugin = {
      name: 'PluginB',
      version: '2.0.0',
      description: 'Test Plugin B that depends on Plugin A',
      async register() {
        pluginBInitialized = true;
      }
    };
    
    // Erstelle einen Plugin-Loader und registriere Event-Handler
    const loader = new PluginLoader('1.0.0');
    const events: PluginLoaderEvent[] = [];
    
    loader.onEvent(event => {
      events.push(event);
    });
    
    // Act - Perform various operations on the plugin loader
    
    // 1. Load Plugin A
    await loader.load(pluginA);
    expect(pluginAInitialized).toBe(true);
    expect(loader.getLoadedPlugins()).toHaveLength(1);
    
    // 2. Hole Plugin-Info
    const pluginAFromLoader = loader.getPlugin('PluginA');
    expect(pluginAFromLoader).toBe(pluginA);
    
    if (pluginAFromLoader?.getInfo) {
      const info = pluginAFromLoader.getInfo();
      expect(info.author).toBe('Test Author');
    } else {
      fail('PluginA should have getInfo method');
    }
    
    // 3. Lade Plugin B
    await loader.load(pluginB);
    expect(pluginBInitialized).toBe(true);
    expect(loader.getLoadedPlugins()).toHaveLength(2);
    
    // 4. Entlade Plugin A (simuliert Plugin-Cleanup/Shutdown)
    await loader.unload('PluginA');
    expect(pluginACleanedUp).toBe(true);
    expect(loader.getLoadedPlugins()).toHaveLength(1);
    expect(loader.getPlugin('PluginA')).toBeUndefined();
    
    // 5. Entlade Plugin B
    await loader.unload('PluginB');
    expect(loader.getLoadedPlugins()).toHaveLength(0);
    
    // Assert - Check if the right events were triggered
    const pluginLoadedEvents = events.filter(e => e.type === 'plugin-loaded');
    const pluginUnloadedEvents = events.filter(e => e.type === 'plugin-unloaded');
    
    expect(pluginLoadedEvents).toHaveLength(2);
    expect(pluginUnloadedEvents).toHaveLength(2);
    
    // Check if the events were triggered in the right order
    const findPluginLoadedEvent = (pluginName: string) => events.findIndex(e => 
      e.type === 'plugin-loaded' && 
      ((e as {type: 'plugin-loaded', plugin: Plugin}).plugin).name === pluginName
    );
    
    const findPluginUnloadedEvent = (pluginName: string) => events.findIndex(e => 
      e.type === 'plugin-unloaded' && 
      ((e as {type: 'plugin-unloaded', plugin: Plugin}).plugin).name === pluginName
    );
    
    expect(findPluginLoadedEvent('PluginA')).toBeLessThan(findPluginLoadedEvent('PluginB'));
    expect(findPluginUnloadedEvent('PluginA')).toBeLessThan(findPluginUnloadedEvent('PluginB'));
  });
}); 
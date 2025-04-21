import { Plugin } from '../../../core/src/lib/ports/plugin.interface';
import { PluginMigration } from './plugin-migration.interface';

/**
 * Error thrown when a plugin fails to load
 */
export class PluginLoadError extends Error {
  constructor(
    public readonly pluginName: string, 
    message: string, 
    public readonly cause?: Error
  ) {
    super(`Failed to load plugin "${pluginName}": ${message}`);
    this.name = 'PluginLoadError';
  }
}

/**
 * Type of event emitted by the plugin loader
 */
export type PluginLoaderEvent = 
  | { type: 'plugin-loaded'; plugin: Plugin } 
  | { type: 'plugin-unloaded'; plugin: Plugin }
  | { type: 'migration-applied'; plugin: Plugin; migration: PluginMigration }
  | { type: 'error'; error: Error };

/**
 * Handler for plugin loader events
 */
export type PluginLoaderEventHandler = (event: PluginLoaderEvent) => void;

/**
 * Class responsible for loading, validating, and managing plugins
 */
export class PluginLoader {
  private plugins: Map<string, Plugin> = new Map();
  private eventHandlers: PluginLoaderEventHandler[] = [];
  private frameworkVersion: string;

  /**
   * Create a new PluginLoader instance
   * @param frameworkVersion The current framework version
   */
  constructor(frameworkVersion: string = '1.0.0') {
    this.frameworkVersion = frameworkVersion;
  }

  /**
   * Register an event handler to receive plugin loader events
   * @param handler Function to be called when events occur
   * @returns Function to unregister the handler
   */
  onEvent(handler: PluginLoaderEventHandler): () => void {
    this.eventHandlers.push(handler);
    return () => {
      this.eventHandlers = this.eventHandlers.filter(h => h !== handler);
    };
  }

  /**
   * Emit an event to all registered handlers
   * @param event Event to emit
   */
  private emitEvent(event: PluginLoaderEvent): void {
    for (const handler of this.eventHandlers) {
      try {
        handler(event);
      } catch (error) {
        console.error('Error in plugin loader event handler:', error);
      }
    }
  }

  /**
   * Validate a plugin before loading it
   * @param plugin The plugin to validate
   * @throws PluginLoadError if validation fails
   */
  private validatePlugin(plugin: Plugin): void {
    if (!plugin.name) {
      throw new PluginLoadError(plugin.name || 'unknown', 'Plugin name is required');
    }

    if (!plugin.version) {
      throw new PluginLoadError(plugin.name, 'Plugin version is required');
    }

    if (typeof plugin.register !== 'function') {
      throw new PluginLoadError(plugin.name, 'Plugin register method is required');
    }

    // Check if a plugin with the same name is already loaded
    if (this.plugins.has(plugin.name)) {
      throw new PluginLoadError(
        plugin.name,
        `A plugin with name "${plugin.name}" is already loaded`
      );
    }

    // Check compatibility if the plugin implements isCompatible
    if (plugin.isCompatible && !plugin.isCompatible(this.frameworkVersion)) {
      throw new PluginLoadError(
        plugin.name,
        `Plugin is not compatible with framework version ${this.frameworkVersion}`
      );
    }
  }

  /**
   * Load a plugin into the system
   * @param plugin The plugin to load
   * @throws PluginLoadError if loading fails
   */
  async load(plugin: Plugin): Promise<void> {
    try {
      // Validate the plugin
      this.validatePlugin(plugin);

      // Run migrations if present
      if ('migrations' in plugin && Array.isArray((plugin as any).migrations)) {
        for (const migration of (plugin as any).migrations as PluginMigration[]) {
          await migration.up();
          this.emitEvent({ 
            type: 'migration-applied', 
            plugin, 
            migration 
          });
        }
      }

      // Register the plugin
      await plugin.register();
      
      // Store the plugin
      this.plugins.set(plugin.name, plugin);
      
      // Emit loaded event
      this.emitEvent({ type: 'plugin-loaded', plugin });
    } catch (error) {
      const loadError = error instanceof PluginLoadError 
        ? error 
        : new PluginLoadError(
            plugin.name || 'unknown', 
            'Failed to load plugin', 
            error instanceof Error ? error : undefined
          );
      
      this.emitEvent({ type: 'error', error: loadError });
      throw loadError;
    }
  }

  /**
   * Unload a plugin from the system
   * @param pluginName Name of the plugin to unload
   * @throws Error if the plugin is not loaded or fails to unload
   */
  async unload(pluginName: string): Promise<void> {
    const plugin = this.plugins.get(pluginName);
    
    if (!plugin) {
      throw new Error(`Plugin "${pluginName}" is not loaded`);
    }
    
    try {
      // Call unregister if it exists
      if (plugin.unregister) {
        await plugin.unregister();
      }
      
      // Remove the plugin
      this.plugins.delete(pluginName);
      
      // Emit unloaded event
      this.emitEvent({ type: 'plugin-unloaded', plugin });
    } catch (error) {
      const unloadError = new Error(
        `Failed to unload plugin "${pluginName}": ${error instanceof Error ? error.message : String(error)}`
      );
      this.emitEvent({ type: 'error', error: unloadError });
      throw unloadError;
    }
  }

  /**
   * Get a plugin by name
   * @param name Name of the plugin to get
   * @returns The plugin or undefined if not found
   */
  getPlugin(name: string): Plugin | undefined {
    return this.plugins.get(name);
  }

  /**
   * Get all loaded plugins
   * @returns Array of loaded plugins
   */
  getLoadedPlugins(): Plugin[] {
    return Array.from(this.plugins.values());
  }
} 
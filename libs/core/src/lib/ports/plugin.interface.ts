/**
 * Interface for a plugin in the framework
 * Plugins provide a way to extend the framework with new functionality
 * without modifying the core
 */
export interface Plugin {
  /**
   * Unique name of the plugin
   */
  name: string;
  
  /**
   * Version of the plugin in semver format
   */
  version: string;
  
  /**
   * Description of the plugin
   */
  description?: string;
  
  /**
   * Initialization method called when the plugin is loaded
   */
  register(): Promise<void>;
  
  /**
   * Optional method to clean up resources when the plugin is unloaded
   */
  unregister?(): Promise<void>;
  
  /**
   * Optional method to check if the plugin is compatible with the current framework
   * @param frameworkVersion Version of the framework
   * @returns True if the plugin is compatible with the given framework version
   */
  isCompatible?(frameworkVersion: string): boolean;
  
  /**
   * Optional method to get extended info about the plugin
   */
  getInfo?(): PluginInfo;
}

/**
 * Extended information about a plugin
 */
export interface PluginInfo {
  /**
   * Author of the plugin
   */
  author?: string;
  
  /**
   * License of the plugin
   */
  license?: string;
  
  /**
   * Homepage URL of the plugin
   */
  homepage?: string;
  
  /**
   * Repository URL of the plugin
   */
  repository?: string;
  
  /**
   * Keywords to describe the plugin
   */
  keywords?: string[];
  
  /**
   * Dependencies required by the plugin
   */
  dependencies?: Record<string, string>;
  
  /**
   * Any additional custom metadata
   */
  [key: string]: unknown;
} 
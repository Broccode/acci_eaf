export interface PluginMigration {
  version: string;
  up(): Promise<void>;
  down?(): Promise<void>;
}

// Plugins can optionally provide migrations to handle schema/data changes 
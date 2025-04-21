import { Plugin } from 'core';
import { PluginMigration } from 'infrastructure';

export class ExamplePlugin implements Plugin {
  name = 'ExamplePlugin';
  version = '1.0.0';

  migrations: PluginMigration[] = [
    {
      version: '1.0.0',
      async up() {
        // Migration logic here
      },
    },
  ];

  async register(): Promise<void> {
    // Plugin-Initialisierung, z.B. Registrieren von Event-Handlern
    console.log(`[Plugin] ${this.name} v${this.version} registered`);
  }
} 
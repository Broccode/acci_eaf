import { Options } from '@mikro-orm/core';
import { TsMorphMetadataProvider } from '@mikro-orm/reflection';
import { PostgreSqlDriver } from '@mikro-orm/postgresql';
import { Logger } from '@nestjs/common';

// Assuming you have a way to import the filter definition
// Adjust the path based on your tsconfig paths or relative location
import { TenantFilter } from './libs/infrastructure/src/lib/persistence/filters/tenant.filter';

// Basic logger for MikroORM
const logger = new Logger('MikroORM');

const config: Options = {
  // --- Core --- 
  metadataProvider: TsMorphMetadataProvider,
  // NOTE: registerRequestContext: true is crucial for AsyncLocalStorage and
  // should be set in MikroOrmModule.forRoot({ ... registerRequestContext: true }) in your NestJS module.
  // It is not part of the base Options type here.
  debug: process.env['NODE_ENV'] !== 'production',
  logger: logger.log.bind(logger),

  // --- Database Connection (PostgreSQL) --- 
  driver: PostgreSqlDriver,
  host: process.env['DB_HOST'] || 'localhost',
  port: parseInt(process.env['DB_PORT'] || '5432', 10),
  user: process.env['DB_USER'] || 'postgres',
  password: process.env['DB_PASSWORD'] || 'password',
  dbName: process.env['DB_NAME'] || 'acci_eaf_db',
  // schema: 'public', // Optional: Specify schema if needed
  // pool: { min: 2, max: 10 }, // Optional: Configure connection pool

  // --- Entity Discovery --- 
  // Define specific paths where entities are expected.
  // Adjust these if your entity locations differ.
  entities: [
    'dist/libs/core/src/lib/domain/**/*.entity.js',
    'dist/libs/infrastructure/src/lib/persistence/entities/**/*.entity.js',
    'dist/libs/rbac/src/lib/**/*.entity.js',
    'dist/libs/tenancy/src/lib/**/*.entity.js',
    // Add paths for plugin entities if applicable, e.g., 'dist/libs/plugins/**/entities/**/*.entity.js'
  ],
  entitiesTs: [
    'libs/core/src/lib/domain/**/*.entity.ts',
    'libs/infrastructure/src/lib/persistence/entities/**/*.entity.ts',
    'libs/rbac/src/lib/**/*.entity.ts',
    'libs/tenancy/src/lib/**/*.entity.ts',
    // Add paths for plugin entities if applicable, e.g., 'libs/plugins/**/entities/**/*.entity.ts'
  ],
  // Example paths - REVIEW AND ADJUST:
  // entities: ['dist/libs/core/src/lib/domain/**/*.entity.js', 'dist/libs/infrastructure/src/lib/persistence/entities/**/*.entity.js'],
  // entitiesTs: ['libs/core/src/lib/domain/**/*.entity.ts', 'libs/infrastructure/src/lib/persistence/entities/**/*.entity.ts'],

  // --- Migrations --- 
  migrations: {
    path: './db/migrations', // Where to save migration files
    pathTs: './db/migrations', // Where to save TS migration files (if using TS migrations)
    glob: '!(*.d).{js,ts}', // How to find migration files (ignore .d.ts)
    transactional: true, // Run each migration in a transaction
    disableForeignKeys: false, // Try to disable FKs during migration execution
    allOrNothing: true, // Wrap all migrations in master transaction
    dropTables: true, // Allow migrations to drop tables
    safe: false, // Allows running risky migrations (e.g., dropping tables)
    emit: 'ts', // Generate migrations in TypeScript
  },

  // --- Filters --- 
  filters: {
    tenant: { // The key here MUST match the name property in the filter definition
      cond: TenantFilter.cond, // Reference the condition function
      default: TenantFilter.default, // Reference the default setting
      // Note: If the filter definition object itself is exported directly, you could potentially do:
      // tenant: TenantFilter,
      // But referencing cond/default explicitly might be safer depending on imports/exports.
    },
    // Add other global filters here if needed
  },

  // --- Seeding --- 
  // seeder: {
  //   path: './db/seeders',
  //   pathTs: './db/seeders',
  //   defaultSeeder: 'DatabaseSeeder',
  //   glob: '!(*.d).{js,ts}',
  //   emit: 'ts',
  // },

  // --- TypeScript specific --- 
  // tsNode: process.env.NODE_ENV !== 'production', // Automatically enable ts-node in dev
};

export default config; 
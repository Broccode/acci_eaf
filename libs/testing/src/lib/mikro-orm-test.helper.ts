import { MikroORM, Options, IDatabaseDriver, Connection } from '@mikro-orm/core';
import mikroOrmConfig from '../../../../mikro-orm.config'; // Adjust path to root config
import { testDbManager } from './test-db-manager';
import { SampleTenantEntity } from '../../../infrastructure/src/lib/persistence/entities/sample-tenant.entity';

/**
 * Initializes MikroORM for integration testing.
 * Connects to a test database managed by Testcontainers.
 * Runs migrations before tests.
 * Cleans up the database schema after tests.
 */
export class MikroOrmTestHelper {
  orm: MikroORM<IDatabaseDriver<Connection>> | null = null;

  async setup(): Promise<MikroORM<IDatabaseDriver<Connection>>> {
    // Start the test database
    await testDbManager.startDb();

    // Override connection settings in the config with dynamic values from Testcontainers
    const testConfig: Partial<Options> = {
      ...mikroOrmConfig,
      host: testDbManager.getHost(),
      port: testDbManager.getPort(),
      user: testDbManager.getUsername(),
      password: testDbManager.getPassword(),
      dbName: testDbManager.getDatabase(),
      // Override entity discovery for this test to ensure SampleTenantEntity is found
      entities: [SampleTenantEntity], // Explicitly provide the entity class
      entitiesTs: [], // Clear TS discovery path as we provide entity directly
      allowGlobalContext: true, // Allows ORM to work without NestJS DI context in tests
      // Ensure debug logging is off for tests unless explicitly needed
      debug: false, 
      logger: () => { /* Disable logger in tests */ }, 
      // Ensure schema generation isn't used; rely on migrations
      ensureIndexes: false,
      ensureDatabase: false,
      // registerRequestContext is NOT part of Options, it's for MikroOrmModule.forRoot
    };

    // Initialize MikroORM
    this.orm = await MikroORM.init(testConfig as Options);

    // Generate schema for all registered entities
    const generator = this.orm.getSchemaGenerator();
    await generator.dropSchema();
    await generator.createSchema();

    // Run migrations to set up the schema (optional, falls vorhanden)
    const migrator = this.orm.getMigrator();
    await migrator.up();

    return this.orm;
  }

  async teardown(): Promise<void> {
    if (this.orm) {
      // Optional: Clean the database schema (useful for isolation between test suites)
      // const generator = this.orm.getSchemaGenerator();
      // await generator.dropSchema(); // Careful, this drops everything!
      
      // Close the ORM connection
      await this.orm.close(true);
      this.orm = null;
    }

    // Stop the test database container
    // Consider doing this globally (e.g., Jest globalTeardown) 
    // if multiple test suites share the same container instance.
    // await testDbManager.stopDb(); 
  }

  /**
   * Helper to get the initialized ORM instance.
   * Throws error if setup() wasn't called.
   */
  getOrm(): MikroORM<IDatabaseDriver<Connection>> {
    if (!this.orm) {
      throw new Error('MikroORM not initialized. Call setup() first.');
    }
    return this.orm;
  }

  /**
   * Helper to get the EntityManager.
   */
  getEntityManager() {
      return this.getOrm().em.fork(); // Fork EM for isolation
  }
} 
import { StartedTestContainer, GenericContainer } from 'testcontainers';
import { PostgreSqlContainer } from '@testcontainers/postgresql';

// Default credentials used by PostgreSqlContainer unless overridden
const DEFAULT_DB = 'test';
const DEFAULT_USER = 'test';
const DEFAULT_PASSWORD = 'test';

export class TestDbManager {
  private container: StartedTestContainer | null = null;
  // Store the config used, not the instance itself, as methods might be on StartedTestContainer
  private dbConfig = {
      database: DEFAULT_DB,
      username: DEFAULT_USER,
      password: DEFAULT_PASSWORD
  };

  async startDb(): Promise<StartedTestContainer> {
    if (this.container) {
      return this.container;
    }
    console.log('Starting PostgreSQL container for testing...');
    // Explicitly set user/pw/db if needed, otherwise defaults are used.
    const pgContainer = new PostgreSqlContainer('postgres:15')
        .withDatabase(this.dbConfig.database)
        .withUsername(this.dbConfig.username)
        .withPassword(this.dbConfig.password);
        
    this.container = await pgContainer.start();
    console.log('PostgreSQL container started.');
    return this.container;
  }

  async stopDb(): Promise<void> {
    if (this.container) {
      console.log('Stopping PostgreSQL container...');
      // stop() should be available on StartedTestContainer
      await this.container.stop(); 
      this.container = null;
      console.log('PostgreSQL container stopped.');
    }
  }

  getHost(): string {
    if (!this.container) throw new Error('Database container not started!');
    return this.container.getHost();
  }

  getPort(): number {
    if (!this.container) throw new Error('Database container not started!');
    return this.container.getMappedPort(5432);
  }

  // Return the configured or default values
  getDatabase(): string {
    return this.dbConfig.database;
  }

  getUsername(): string {
    return this.dbConfig.username;
  }

  getPassword(): string {
    return this.dbConfig.password;
  }
  
  getConnectionUri(): string {
    return `postgresql://${this.getUsername()}:${this.getPassword()}@${this.getHost()}:${this.getPort()}/${this.getDatabase()}`;
  }
}

export const testDbManager = new TestDbManager(); 
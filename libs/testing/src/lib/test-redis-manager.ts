import { RedisContainer } from '@testcontainers/redis';

/**
 * Manages a Redis container for integration testing.
 * Provides methods to start, stop and access the Redis container.
 */
export class TestRedisManager {
  private container: any = null;

  /**
   * Starts a Redis container for testing.
   * @param version Optional Redis version (default: "redis:alpine")
   * @returns The started container
   */
  async startRedis(version: string = "redis:alpine"): Promise<any> {
    if (this.container) {
      return this.container;
    }
    console.log('Starting Redis container for testing...');
    
    // Use the specialized RedisContainer from @testcontainers/redis
    const redisContainer = new RedisContainer(version);
        
    this.container = await redisContainer.start();
    console.log('Redis container started.');
    return this.container;
  }

  /**
   * Stops the Redis container.
   */
  async stopRedis(): Promise<void> {
    if (this.container) {
      console.log('Stopping Redis container...');
      await this.container.stop(); 
      this.container = null;
      console.log('Redis container stopped.');
    }
  }

  /**
   * Gets the host of the Redis container.
   * @returns The host
   */
  getHost(): string {
    if (!this.container) throw new Error('Redis container not started!');
    return this.container.getHost();
  }

  /**
   * Gets the mapped port of the Redis container.
   * @returns The port
   */
  getPort(): number {
    if (!this.container) throw new Error('Redis container not started!');
    return this.container.getMappedPort(6379); // Redis default port
  }
  
  /**
   * Gets the connection string for the Redis container.
   * @returns The connection string in the format redis://host:port
   */
  getConnectionString(): string {
    return `redis://${this.getHost()}:${this.getPort()}`;
  }
}

// Export a singleton instance
export const testRedisManager = new TestRedisManager(); 
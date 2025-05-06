import { Injectable } from '@nestjs/common';
import { MikroOrmTestHelper, testDbManager, testRedisManager } from '..';
import { EntityManager } from '@mikro-orm/core';
import { Entity, PrimaryKey, Property } from '@mikro-orm/core';

/**
 * Example entity for demonstration
 */
@Entity({ tableName: 'test_users' })
class TestUser {
  @PrimaryKey()
  id!: number;

  @Property()
  name!: string;

  @Property()
  email!: string;

  @Property({ nullable: true })
  tenantId?: string;
}

/**
 * Example service that uses both database and Redis
 */
@Injectable()
class UserCacheService {
  constructor(
    private readonly em: EntityManager,
    private readonly redisClient: any
  ) {}

  async createUser(name: string, email: string): Promise<TestUser> {
    const user = new TestUser();
    user.name = name;
    user.email = email;
    
    await this.em.persistAndFlush(user);
    
    // Cache user in Redis
    await this.redisClient.set(`user:${user.id}`, JSON.stringify(user));
    
    return user;
  }

  async getUserById(id: number): Promise<TestUser | null> {
    // Try to get from cache first
    const cachedUser = await this.redisClient.get(`user:${id}`);
    if (cachedUser) {
      return JSON.parse(cachedUser);
    }
    
    // Fall back to database
    return this.em.findOne(TestUser, { id });
  }

  async clearCache(userId: number): Promise<void> {
    await this.redisClient.del(`user:${userId}`);
  }
}

/**
 * Example integration test for UserCacheService
 * This is just for demonstration purposes and won't be executed
 */
describe('UserCacheService Integration', () => {
  let mikroOrmHelper: MikroOrmTestHelper;
  let em: EntityManager;
  let redisClient: any;
  let userService: UserCacheService;

  beforeAll(async () => {
    // Start PostgreSQL container
    await testDbManager.startDb();
    
    // Start Redis container
    await testRedisManager.startRedis();
    
    // Set up MikroORM
    mikroOrmHelper = new MikroOrmTestHelper();
    const orm = await mikroOrmHelper.setup();
    
    // Create schema
    const generator = orm.getSchemaGenerator();
    await generator.createSchema();
    
    // Set up Redis client
    const Redis = require('ioredis');
    redisClient = new Redis({
      host: testRedisManager.getHost(),
      port: testRedisManager.getPort()
    });
    
    // Create fork of EntityManager for test isolation
    em = orm.em.fork();
    
    // Create service instance
    userService = new UserCacheService(em, redisClient);
  });

  afterAll(async () => {
    // Close Redis client
    if (redisClient) {
      await redisClient.quit();
    }
    
    // Clean up MikroORM
    await mikroOrmHelper.teardown();
    
    // Stop Redis container
    await testRedisManager.stopRedis();
    
    // Stop PostgreSQL container
    await testDbManager.stopDb();
  });

  beforeEach(async () => {
    // Clear database between tests
    await em.nativeDelete(TestUser, {});
    // Clear Redis cache between tests
    await redisClient.flushall();
  });

  it('should create a user and store in both database and cache', async () => {
    // Act
    const user = await userService.createUser('John Doe', 'john@example.com');
    
    // Assert - Check database
    const savedUser = await em.findOne(TestUser, { id: user.id });
    expect(savedUser).toBeDefined();
    expect(savedUser?.name).toBe('John Doe');
    
    // Assert - Check Redis cache
    const cachedUser = await redisClient.get(`user:${user.id}`);
    expect(cachedUser).toBeDefined();
    
    const parsedCachedUser = JSON.parse(cachedUser);
    expect(parsedCachedUser.name).toBe('John Doe');
  });

  it('should retrieve user from cache without database query', async () => {
    // Arrange
    const user = await userService.createUser('Jane Doe', 'jane@example.com');
    
    // Mock EntityManager to track database calls
    const findOneSpy = jest.spyOn(em, 'findOne');
    
    // Act
    const retrievedUser = await userService.getUserById(user.id);
    
    // Assert
    expect(retrievedUser).toBeDefined();
    expect(retrievedUser?.name).toBe('Jane Doe');
    
    // Should not have called database
    expect(findOneSpy).not.toHaveBeenCalled();
  });

  it('should fall back to database when cache is cleared', async () => {
    // Arrange
    const user = await userService.createUser('Bob Smith', 'bob@example.com');
    await userService.clearCache(user.id);
    
    // Mock EntityManager to track database calls
    const findOneSpy = jest.spyOn(em, 'findOne');
    
    // Act
    const retrievedUser = await userService.getUserById(user.id);
    
    // Assert
    expect(retrievedUser).toBeDefined();
    expect(retrievedUser?.name).toBe('Bob Smith');
    
    // Should have called database since cache was cleared
    expect(findOneSpy).toHaveBeenCalledWith(TestUser, { id: user.id });
  });
}); 
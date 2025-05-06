import { RedisCacheService } from '../lib/cache/redis-cache.service';
import { testRedisManager } from 'testing';
import Redis from 'ioredis';

describe('RedisCacheService Integration', () => {
  let redisClient: Redis;
  let service: RedisCacheService;

  beforeAll(async () => {
    // Start real Redis container via Testcontainers
    await testRedisManager.startRedis('redis:7.2');
    redisClient = new Redis({
      host: testRedisManager.getHost(),
      port: testRedisManager.getPort(),
    });

    service = new RedisCacheService(redisClient);
  });

  afterAll(async () => {
    if (redisClient) {
      await redisClient.quit();
    }
    await testRedisManager.stopRedis();
  });

  beforeEach(async () => {
    await redisClient.flushall();
  });

  it('should store and retrieve JSON-serialised value', async () => {
    const key = 'test-key';
    const value = { foo: 'bar' };

    await service.set(key, value);
    const retrieved = await service.get<typeof value>(key);

    expect(retrieved).toEqual(value);
  });

  it('should respect TTL and expire keys', async () => {
    jest.setTimeout(10000); // safety
    const key = 'ttl-key';

    await service.set(key, 'temp', 1); // 1 second TTL
    const immediate = await service.get<string>(key);
    expect(immediate).toBe('temp');

    // Wait >1s for key expiry
    await new Promise((res) => setTimeout(res, 1500));

    const expired = await service.get<string>(key);
    expect(expired).toBeUndefined();
  });
}); 
import { Injectable } from '@nestjs/common';
import Redis from 'ioredis';

/**
 * Simple wrapper around ioredis providing typed get/set with JSON serialization
 * and optional TTL support. Intended for use by framework infrastructure and
 * easily mockable in tests.
 */
@Injectable()
export class RedisCacheService {
  constructor(private readonly client: Redis) {}

  /**
   * Store a value under the given key. Automatically serialises to JSON. If
   * ttlSeconds is provided, the key will expire after that many seconds.
   */
  async set<T = any>(key: string, value: T, ttlSeconds?: number): Promise<void> {
    const payload = JSON.stringify(value);
    if (ttlSeconds && ttlSeconds > 0) {
      await this.client.set(key, payload, 'EX', ttlSeconds);
    } else {
      await this.client.set(key, payload);
    }
  }

  /**
   * Retrieve a value by key. Returns `undefined` if the key does not exist.
   */
  async get<T = any>(key: string): Promise<T | undefined> {
    const data = await this.client.get(key);
    return data ? (JSON.parse(data) as T) : undefined;
  }

  /**
   * Delete a key from redis.
   */
  async del(key: string): Promise<void> {
    await this.client.del(key);
  }
}

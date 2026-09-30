import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TtlCache, TokenBucketRateLimiter } from '../src/cache.js';

describe('TtlCache', () => {
  let cache: TtlCache<string>;

  beforeEach(() => {
    cache = new TtlCache<string>(1); // 1 second default TTL
  });

  it('stores and retrieves cached values within TTL', () => {
    cache.set('key1', 'value1');
    expect(cache.get('key1')).toBe('value1');
  });

  it('returns null for non-existent keys', () => {
    expect(cache.get('unknown')).toBeNull();
  });

  it('expires entries after TTL elapsed', async () => {
    cache.set('expiring', 'temp', 0.05); // 50ms TTL
    expect(cache.get('expiring')).toBe('temp');

    await new Promise((resolve) => setTimeout(resolve, 80));
    expect(cache.get('expiring')).toBeNull();
  });

  it('deletes entries correctly', () => {
    cache.set('toDelete', 'value');
    expect(cache.get('toDelete')).toBe('value');
    cache.delete('toDelete');
    expect(cache.get('toDelete')).toBeNull();
  });

  it('clears all cached entries', () => {
    cache.set('k1', 'v1');
    cache.set('k2', 'v2');
    cache.clear();
    expect(cache.get('k1')).toBeNull();
    expect(cache.get('k2')).toBeNull();
  });
});

describe('TokenBucketRateLimiter', () => {
  it('allows requests within capacity', () => {
    const limiter = new TokenBucketRateLimiter(5);
    const key = 'test-client';

    for (let i = 0; i < 5; i++) {
      const result = limiter.tryConsume(key);
      expect(result.allowed).toBe(true);
      expect(result.remaining).toBe(4 - i);
    }
  });

  it('blocks requests once token capacity is exhausted', () => {
    const limiter = new TokenBucketRateLimiter(3);
    const key = 'limited-client';

    expect(limiter.tryConsume(key).allowed).toBe(true);
    expect(limiter.tryConsume(key).allowed).toBe(true);
    expect(limiter.tryConsume(key).allowed).toBe(true);

    const blocked = limiter.tryConsume(key);
    expect(blocked.allowed).toBe(false);
    expect(blocked.remaining).toBe(0);
    expect(blocked.retryAfterSeconds).toBeGreaterThan(0);
  });

  it('isolates buckets across different keys', () => {
    const limiter = new TokenBucketRateLimiter(2);
    const clientA = 'client-a';
    const clientB = 'client-b';

    expect(limiter.tryConsume(clientA).allowed).toBe(true);
    expect(limiter.tryConsume(clientA).allowed).toBe(true);
    expect(limiter.tryConsume(clientA).allowed).toBe(false);

    // Client B still has its own full quota
    expect(limiter.tryConsume(clientB).allowed).toBe(true);
    expect(limiter.tryConsume(clientB).allowed).toBe(true);
  });

  it('resets key rate limit correctly', () => {
    const limiter = new TokenBucketRateLimiter(1);
    const key = 'reset-client';

    expect(limiter.tryConsume(key).allowed).toBe(true);
    expect(limiter.tryConsume(key).allowed).toBe(false);

    limiter.reset(key);
    expect(limiter.tryConsume(key).allowed).toBe(true);
  });
});

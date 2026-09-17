/**
 * In-memory TTL Cache and Token-Bucket Rate Limiter
 * Provides micro-caching for API Key authentication and read queries,
 * and guards against runaway AI agent loops (60 requests/min per key).
 */

interface CacheEntry<T> {
  value: T;
  expiresAt: number;
}

export class TtlCache<T> {
  private store = new Map<string, CacheEntry<T>>();
  private defaultTtlMs: number;

  constructor(defaultTtlSeconds: number = 30) {
    this.defaultTtlMs = defaultTtlSeconds * 1000;
  }

  get(key: string): T | null {
    const entry = this.store.get(key);
    if (!entry) return null;

    if (Date.now() > entry.expiresAt) {
      this.store.delete(key);
      return null;
    }
    return entry.value;
  }

  set(key: string, value: T, ttlSeconds?: number): void {
    const ttlMs = ttlSeconds !== undefined ? ttlSeconds * 1000 : this.defaultTtlMs;
    this.store.set(key, {
      value,
      expiresAt: Date.now() + ttlMs,
    });

    // Simple house-keeping: cap cache at 500 items
    if (this.store.size > 500) {
      const oldestKey = this.store.keys().next().value;
      if (oldestKey) this.store.delete(oldestKey);
    }
  }

  delete(key: string): void {
    this.store.delete(key);
  }

  clear(): void {
    this.store.clear();
  }
}

interface Bucket {
  tokens: number;
  lastRefill: number;
}

export class TokenBucketRateLimiter {
  private buckets = new Map<string, Bucket>();
  private readonly capacity: number;
  private readonly refillRatePerSec: number;

  /**
   * @param requestsPerMinute Maximum allowed requests in a 60-second window (default: 60)
   */
  constructor(requestsPerMinute: number = 60) {
    this.capacity = requestsPerMinute;
    this.refillRatePerSec = requestsPerMinute / 60;
  }

  /**
   * Checks if an action is allowed for the given identifier. Consumes 1 token if allowed.
   */
  tryConsume(key: string, tokens: number = 1): { allowed: boolean; remaining: number } {
    const now = Date.now();
    let bucket = this.buckets.get(key);

    if (!bucket) {
      bucket = { tokens: this.capacity, lastRefill: now };
      this.buckets.set(key, bucket);
    } else {
      // Refill tokens based on elapsed time
      const elapsedSec = (now - bucket.lastRefill) / 1000;
      const addedTokens = elapsedSec * this.refillRatePerSec;
      bucket.tokens = Math.min(this.capacity, bucket.tokens + addedTokens);
      bucket.lastRefill = now;
    }

    if (bucket.tokens >= tokens) {
      bucket.tokens -= tokens;
      return { allowed: true, remaining: Math.floor(bucket.tokens) };
    }

    return { allowed: false, remaining: 0 };
  }
}

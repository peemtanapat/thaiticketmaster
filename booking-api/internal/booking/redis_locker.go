package booking

import (
	"context"
	"fmt"
	"time"

	"github.com/go-redis/redis/v8"
)

// RedisLocker implements Locker using Redis
type RedisLocker struct {
	client *redis.Client
}

// NewRedisLocker creates a new Redis-based distributed locker
func NewRedisLocker(client *redis.Client) *RedisLocker {
	return &RedisLocker{client: client}
}

// AcquireLock attempts to acquire a distributed lock
func (l *RedisLocker) AcquireLock(ctx context.Context, key string, ttl time.Duration) error {
	// Use SET with NX (set if not exists) and EX (expiration)
	result, err := l.client.SetNX(ctx, key, "locked", ttl).Result()
	if err != nil {
		return fmt.Errorf("failed to acquire lock: %w", err)
	}

	if !result {
		return fmt.Errorf("lock already held by another process")
	}

	return nil
}

// ReleaseLock releases a distributed lock
func (l *RedisLocker) ReleaseLock(ctx context.Context, key string) error {
	err := l.client.Del(ctx, key).Err()
	if err != nil {
		return fmt.Errorf("failed to release lock: %w", err)
	}
	return nil
}

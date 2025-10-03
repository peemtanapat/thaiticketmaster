package booking

import (
	"context"
	"time"
)

// noOpLocker is a no-op locker for testing
type noOpLocker struct{}

func (l *noOpLocker) AcquireLock(ctx context.Context, key string, ttl time.Duration) error {
	return nil
}

func (l *noOpLocker) ReleaseLock(ctx context.Context, key string) error {
	return nil
}

// noOpTxManager is a no-op transaction manager for testing
type noOpTxManager struct{}

func (m *noOpTxManager) BeginTx(ctx context.Context) (Transaction, error) {
	return &noOpTx{}, nil
}

// noOpTx is a no-op transaction for testing
type noOpTx struct{}

func (t *noOpTx) Commit() error {
	return nil
}

func (t *noOpTx) Rollback() error {
	return nil
}

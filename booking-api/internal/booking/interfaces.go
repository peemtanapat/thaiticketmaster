package booking

import (
	"context"
	"time"
)

// Locker defines the interface for distributed locking
type Locker interface {
	AcquireLock(ctx context.Context, key string, ttl time.Duration) error
	ReleaseLock(ctx context.Context, key string) error
}

// EventAPIClient defines the interface for interacting with event-api
type EventAPIClient interface {
	GetEvent(ctx context.Context, eventID string) (*Event, error)
}

// TransactionManager defines the interface for database transaction management
type TransactionManager interface {
	BeginTx(ctx context.Context) (Transaction, error)
}

// Transaction defines the interface for a database transaction
type Transaction interface {
	Commit() error
	Rollback() error
	// Add more methods as needed (e.g., Exec, Query)
}

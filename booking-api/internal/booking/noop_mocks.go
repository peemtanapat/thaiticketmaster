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

// noOpRepository is a no-op repository for testing
type noOpRepository struct{}

func (r *noOpRepository) CreateBooking(ctx context.Context, tx Transaction, booking *Booking) error {
	return nil
}

func (r *noOpRepository) GetBookingByID(ctx context.Context, bookingID string) (*Booking, error) {
	return nil, nil
}

func (r *noOpRepository) GetBookingsByUserID(ctx context.Context, userID string) ([]*Booking, error) {
	return nil, nil
}

func (r *noOpRepository) UpdateBookingStatus(ctx context.Context, bookingID string, status string) error {
	return nil
}

func (r *noOpRepository) DeleteBooking(ctx context.Context, bookingID string) error {
	return nil
}

func (r *noOpRepository) CheckSeatsAvailability(ctx context.Context, tx Transaction, eventID string, showtime time.Time, seatIDs []string) ([]string, error) {
	return []string{}, nil // Return empty list (all seats available) for testing
}

func (r *noOpRepository) UpdateEventSeatsStatus(ctx context.Context, tx Transaction, eventID string, showtime time.Time, seatIDs []string, bookingID string, status string) error {
	return nil // No-op for testing
}

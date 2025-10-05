package booking

import (
	"context"
	"database/sql"
	"fmt"
	"time"
)

// BookingRequest represents a ticket booking request
type BookingRequest struct {
	EventID  string    `json:"event_id"`
	UserID   string    `json:"user_id"`
	Showtime time.Time `json:"showtime"`
	Quantity int       `json:"quantity"`
	SeatIDs  []string  `json:"seat_ids"`
}

// BookingService handles ticket booking operations
type BookingService struct {
	locker      Locker
	txManager   TransactionManager
	eventClient EventAPIClient
	repository  BookingRepository
	lockTTL     time.Duration
}

// NewBookingService creates a new booking service with proper dependencies
func NewBookingService(locker Locker, txManager TransactionManager, eventClient EventAPIClient, repository BookingRepository) *BookingService {
	return &BookingService{
		locker:      locker,
		txManager:   txManager,
		eventClient: eventClient,
		repository:  repository,
		lockTTL:     30 * time.Second, // Default lock TTL
	}
}

// NewBookingServiceWithDefaults creates a booking service with default implementations (for testing)
func NewBookingServiceWithDefaults(db *sql.DB, redisClient interface{}, eventAPIURL string) *BookingService {
	var locker Locker = &noOpLocker{}
	var txManager TransactionManager = &noOpTxManager{}
	var eventClient EventAPIClient = NewHTTPEventAPIClient(eventAPIURL)
	var repository BookingRepository = &noOpRepository{}

	if db != nil {
		txManager = NewSQLTransactionManager(db)
		repository = NewPostgresBookingRepository(db)
	}

	return NewBookingService(locker, txManager, eventClient, repository)
}

// BookTickets implements the ticket booking flow with:
// 1. Acquire distributed lock
// 2. Start DB transaction
// 3. Validate booking rules (check event exists and showtime matches)
// 4. Commit transaction on success
func (s *BookingService) BookTickets(ctx context.Context, req BookingRequest) error {
	// Step 1: Acquire distributed lock
	lockKey := fmt.Sprintf("booking:lock:%s", req.EventID)
	if err := s.locker.AcquireLock(ctx, lockKey, s.lockTTL); err != nil {
		return fmt.Errorf("failed to acquire lock: %w", err)
	}
	defer s.locker.ReleaseLock(ctx, lockKey)

	// Step 2: Start DB transaction
	tx, err := s.txManager.BeginTx(ctx)
	if err != nil {
		return fmt.Errorf("failed to start transaction: %w", err)
	}
	defer tx.Rollback() // Rollback if not committed

	// Step 3: Validate booking rules - check event exists and showtime matches
	event, err := s.eventClient.GetEvent(ctx, req.EventID)
	if err != nil {
		return fmt.Errorf("failed to get event: %w", err)
	}

	if err := s.validateShowtimeInEvent(req.Showtime, event.ShowDateTimes); err != nil {
		return err
	}

	// Additional validation
	if err := s.validateBookingRequest(req); err != nil {
		return err
	}

	// Step 4: Check seat availability (prevent duplicate bookings)
	bookedSeats, err := s.repository.CheckSeatsAvailability(ctx, tx, req.EventID, req.Showtime, req.SeatIDs)
	if err != nil {
		return fmt.Errorf("failed to check seat availability: %w", err)
	}

	if len(bookedSeats) > 0 {
		return fmt.Errorf("seats already booked: %v", bookedSeats)
	}

	// Step 5: Save booking to database
	booking := &Booking{
		EventID:  req.EventID,
		UserID:   req.UserID,
		Showtime: req.Showtime,
		Quantity: req.Quantity,
		SeatIDs:  req.SeatIDs,
		Status:   "CONFIRMED",
	}

	if err := s.repository.CreateBooking(ctx, tx, booking); err != nil {
		return fmt.Errorf("failed to create booking: %w", err)
	}

	// Step 6: Commit transaction on success
	if err := tx.Commit(); err != nil {
		return fmt.Errorf("failed to commit transaction: %w", err)
	}

	return nil
}

// Event represents the event data from event-api
type Event struct {
	ID            int64          `json:"id"`
	Name          string         `json:"name"`
	ShowDateTimes []FlexibleTime `json:"showDateTimes"`
	Location      string         `json:"location"`
}

// validateShowtimeInEvent checks if the booking showtime exists in the event's showtimes
func (s *BookingService) validateShowtimeInEvent(bookingShowtime time.Time, eventShowtimes []FlexibleTime) error {
	if len(eventShowtimes) == 0 {
		return fmt.Errorf("event has no showtimes available")
	}

	// Truncate to seconds to avoid microsecond differences
	bookingTime := bookingShowtime.Truncate(time.Second)

	for _, eventShowtime := range eventShowtimes {
		if bookingTime.Equal(eventShowtime.Time.Truncate(time.Second)) {
			return nil // Found matching showtime
		}
	}

	// No matching showtime found
	return fmt.Errorf("showtime %s not available for this event",
		bookingShowtime.Format(time.RFC3339))
}

// validateBookingRequest validates the booking request
func (s *BookingService) validateBookingRequest(req BookingRequest) error {
	if req.EventID == "" {
		return fmt.Errorf("event_id is required")
	}
	if req.UserID == "" {
		return fmt.Errorf("user_id is required")
	}
	if req.Quantity <= 0 {
		return fmt.Errorf("quantity must be positive")
	}
	if len(req.SeatIDs) != req.Quantity {
		return fmt.Errorf("number of seats must match quantity")
	}
	return nil
}

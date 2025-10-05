package booking

import (
	"context"
	"database/sql"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/lib/pq"
)

// BookingRepository handles database operations for bookings
type BookingRepository interface {
	CreateBooking(ctx context.Context, tx Transaction, booking *Booking) error
	GetBookingByID(ctx context.Context, bookingID string) (*Booking, error)
	GetBookingsByUserID(ctx context.Context, userID string) ([]*Booking, error)
	UpdateBookingStatus(ctx context.Context, bookingID string, status string) error
	DeleteBooking(ctx context.Context, bookingID string) error
	CheckSeatsAvailability(ctx context.Context, tx Transaction, eventID string, showtime time.Time, seatIDs []string) ([]string, error)
}

// Booking represents a booking record in the database
type Booking struct {
	ID        int       `json:"id"`
	BookingID string    `json:"booking_id"`
	EventID   string    `json:"event_id"`
	UserID    string    `json:"user_id"`
	Showtime  time.Time `json:"showtime"`
	Quantity  int       `json:"quantity"`
	Status    string    `json:"status"`
	SeatIDs   []string  `json:"seat_ids"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}

// PostgresBookingRepository implements BookingRepository for PostgreSQL
type PostgresBookingRepository struct {
	db *sql.DB
}

// NewPostgresBookingRepository creates a new PostgreSQL booking repository
func NewPostgresBookingRepository(db *sql.DB) *PostgresBookingRepository {
	return &PostgresBookingRepository{db: db}
}

// CreateBooking creates a new booking and its associated seats in the database
func (r *PostgresBookingRepository) CreateBooking(ctx context.Context, tx Transaction, booking *Booking) error {
	// Generate unique booking ID if not provided
	if booking.BookingID == "" {
		booking.BookingID = generateBookingID()
	}

	// Set default status if not provided
	if booking.Status == "" {
		booking.Status = "CONFIRMED"
	}

	// Get the actual *sql.Tx from the Transaction interface
	sqlTx, err := r.getSQLTx(tx)
	if err != nil {
		return err
	}

	// Insert booking record
	query := `
		INSERT INTO bookings (booking_id, event_id, user_id, showtime, quantity, status, created_at, updated_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
		RETURNING id, created_at, updated_at
	`

	now := time.Now()
	err = sqlTx.QueryRowContext(
		ctx,
		query,
		booking.BookingID,
		booking.EventID,
		booking.UserID,
		booking.Showtime,
		booking.Quantity,
		booking.Status,
		now,
		now,
	).Scan(&booking.ID, &booking.CreatedAt, &booking.UpdatedAt)

	if err != nil {
		return fmt.Errorf("failed to insert booking: %w", err)
	}

	// Insert booking seats
	if len(booking.SeatIDs) > 0 {
		seatQuery := `
			INSERT INTO booking_seats (booking_id, seat_id, created_at)
			VALUES ($1, $2, $3)
		`

		for _, seatID := range booking.SeatIDs {
			_, err := sqlTx.ExecContext(ctx, seatQuery, booking.BookingID, seatID, now)
			if err != nil {
				return fmt.Errorf("failed to insert seat %s: %w", seatID, err)
			}
		}
	}

	return nil
}

// GetBookingByID retrieves a booking by its booking ID
func (r *PostgresBookingRepository) GetBookingByID(ctx context.Context, bookingID string) (*Booking, error) {
	booking := &Booking{}

	query := `
		SELECT id, booking_id, event_id, user_id, showtime, quantity, status, created_at, updated_at
		FROM bookings
		WHERE booking_id = $1
	`

	err := r.db.QueryRowContext(ctx, query, bookingID).Scan(
		&booking.ID,
		&booking.BookingID,
		&booking.EventID,
		&booking.UserID,
		&booking.Showtime,
		&booking.Quantity,
		&booking.Status,
		&booking.CreatedAt,
		&booking.UpdatedAt,
	)

	if err == sql.ErrNoRows {
		return nil, fmt.Errorf("booking not found")
	}
	if err != nil {
		return nil, fmt.Errorf("failed to get booking: %w", err)
	}

	// Fetch associated seats
	seatQuery := `
		SELECT seat_id
		FROM booking_seats
		WHERE booking_id = $1
		ORDER BY id
	`

	rows, err := r.db.QueryContext(ctx, seatQuery, bookingID)
	if err != nil {
		return nil, fmt.Errorf("failed to get seats: %w", err)
	}
	defer rows.Close()

	booking.SeatIDs = []string{}
	for rows.Next() {
		var seatID string
		if err := rows.Scan(&seatID); err != nil {
			return nil, fmt.Errorf("failed to scan seat: %w", err)
		}
		booking.SeatIDs = append(booking.SeatIDs, seatID)
	}

	return booking, nil
}

// GetBookingsByUserID retrieves all bookings for a user
func (r *PostgresBookingRepository) GetBookingsByUserID(ctx context.Context, userID string) ([]*Booking, error) {
	query := `
		SELECT id, booking_id, event_id, user_id, showtime, quantity, status, created_at, updated_at
		FROM bookings
		WHERE user_id = $1
		ORDER BY created_at DESC
	`

	rows, err := r.db.QueryContext(ctx, query, userID)
	if err != nil {
		return nil, fmt.Errorf("failed to get bookings: %w", err)
	}
	defer rows.Close()

	bookings := []*Booking{}
	for rows.Next() {
		booking := &Booking{}
		err := rows.Scan(
			&booking.ID,
			&booking.BookingID,
			&booking.EventID,
			&booking.UserID,
			&booking.Showtime,
			&booking.Quantity,
			&booking.Status,
			&booking.CreatedAt,
			&booking.UpdatedAt,
		)
		if err != nil {
			return nil, fmt.Errorf("failed to scan booking: %w", err)
		}

		// Fetch seats for this booking
		booking.SeatIDs, err = r.getSeatsForBooking(ctx, booking.BookingID)
		if err != nil {
			return nil, err
		}

		bookings = append(bookings, booking)
	}

	return bookings, nil
}

// UpdateBookingStatus updates the status of a booking
func (r *PostgresBookingRepository) UpdateBookingStatus(ctx context.Context, bookingID string, status string) error {
	query := `
		UPDATE bookings
		SET status = $1, updated_at = $2
		WHERE booking_id = $3
	`

	result, err := r.db.ExecContext(ctx, query, status, time.Now(), bookingID)
	if err != nil {
		return fmt.Errorf("failed to update booking status: %w", err)
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return fmt.Errorf("failed to get rows affected: %w", err)
	}

	if rowsAffected == 0 {
		return fmt.Errorf("booking not found")
	}

	return nil
}

// DeleteBooking soft deletes a booking (updates status to CANCELLED)
func (r *PostgresBookingRepository) DeleteBooking(ctx context.Context, bookingID string) error {
	return r.UpdateBookingStatus(ctx, bookingID, "CANCELLED")
}

// Helper methods

func (r *PostgresBookingRepository) getSQLTx(tx Transaction) (*sql.Tx, error) {
	if sqlTx, ok := tx.(*sqlTransaction); ok {
		return sqlTx.GetSQLTx(), nil
	}
	return nil, fmt.Errorf("tx is not a *sqlTransaction")
}

func (r *PostgresBookingRepository) getSeatsForBooking(ctx context.Context, bookingID string) ([]string, error) {
	query := `
		SELECT seat_id
		FROM booking_seats
		WHERE booking_id = $1
		ORDER BY id
	`

	rows, err := r.db.QueryContext(ctx, query, bookingID)
	if err != nil {
		return nil, fmt.Errorf("failed to get seats: %w", err)
	}
	defer rows.Close()

	seatIDs := []string{}
	for rows.Next() {
		var seatID string
		if err := rows.Scan(&seatID); err != nil {
			return nil, fmt.Errorf("failed to scan seat: %w", err)
		}
		seatIDs = append(seatIDs, seatID)
	}

	return seatIDs, nil
}

// CheckSeatsAvailability checks if the requested seats are available for booking
// Returns a list of already booked seats (empty list means all seats are available)
func (r *PostgresBookingRepository) CheckSeatsAvailability(ctx context.Context, tx Transaction, eventID string, showtime time.Time, seatIDs []string) ([]string, error) {
	if len(seatIDs) == 0 {
		return []string{}, nil
	}

	// Get the actual *sql.Tx from the Transaction interface
	sqlTx, err := r.getSQLTx(tx)
	if err != nil {
		return nil, err
	}

	// Build query to check for already booked seats
	// We check for CONFIRMED bookings only (not CANCELLED)
	query := `
		SELECT DISTINCT bs.seat_id
		FROM booking_seats bs
		INNER JOIN bookings b ON bs.booking_id = b.booking_id
		WHERE b.event_id = $1
		  AND b.showtime = $2
		  AND b.status = 'CONFIRMED'
		  AND bs.seat_id = ANY($3)
	`

	rows, err := sqlTx.QueryContext(ctx, query, eventID, showtime, pq.Array(seatIDs))
	if err != nil {
		return nil, fmt.Errorf("failed to check seat availability: %w", err)
	}
	defer rows.Close()

	bookedSeats := []string{}
	for rows.Next() {
		var seatID string
		if err := rows.Scan(&seatID); err != nil {
			return nil, fmt.Errorf("failed to scan booked seat: %w", err)
		}
		bookedSeats = append(bookedSeats, seatID)
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("error iterating booked seats: %w", err)
	}

	return bookedSeats, nil
}

// generateBookingID generates a unique booking ID
func generateBookingID() string {
	return fmt.Sprintf("BK-%s", uuid.New().String())
}

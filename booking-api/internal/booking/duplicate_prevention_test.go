package booking

import (
	"context"
	"database/sql"
	"testing"
	"time"

	_ "github.com/lib/pq"
	"github.com/stretchr/testify/assert"
)

// TestCheckSeatsAvailability_Integration tests seat availability checking with real database
func TestCheckSeatsAvailability_Integration(t *testing.T) {
	if testing.Short() {
		t.Skip("Skipping integration test in short mode")
	}

	// Setup test database connection
	db, err := sql.Open("postgres", "host=localhost port=5432 user=admin password=admin123 dbname=booking_db sslmode=disable")
	if err != nil {
		t.Skipf("Skipping test: cannot connect to database: %v", err)
		return
	}
	defer db.Close()

	if err := db.Ping(); err != nil {
		t.Skipf("Skipping test: database not available: %v", err)
		return
	}

	// Create repository
	repo := NewPostgresBookingRepository(db)
	txManager := NewSQLTransactionManager(db)
	ctx := context.Background()

	// Clean up any existing test data
	_, _ = db.Exec("DELETE FROM booking_seats WHERE booking_id LIKE 'TEST-%'")
	_, _ = db.Exec("DELETE FROM bookings WHERE booking_id LIKE 'TEST-%'")

	t.Run("No booked seats - all available", func(t *testing.T) {
		// Arrange
		tx, err := txManager.BeginTx(ctx)
		assert.NoError(t, err)
		defer tx.Rollback()

		eventID := "test-event-1"
		showtime := time.Date(2025, 12, 25, 19, 0, 0, 0, time.UTC)
		requestedSeats := []string{"A1", "A2", "A3"}

		// Act
		bookedSeats, err := repo.CheckSeatsAvailability(ctx, tx, eventID, showtime, requestedSeats)

		// Assert
		assert.NoError(t, err)
		assert.Empty(t, bookedSeats, "All seats should be available")
	})

	t.Run("Some seats already booked", func(t *testing.T) {
		// Arrange - Create a booking with some seats
		tx1, err := txManager.BeginTx(ctx)
		assert.NoError(t, err)

		eventID := "test-event-2"
		showtime := time.Date(2025, 12, 26, 20, 0, 0, 0, time.UTC)

		existingBooking := &Booking{
			BookingID: "TEST-EXISTING-001",
			EventID:   eventID,
			UserID:    "user-1",
			Showtime:  showtime,
			Quantity:  2,
			SeatIDs:   []string{"B1", "B2"},
			Status:    "CONFIRMED",
		}

		err = repo.CreateBooking(ctx, tx1, existingBooking)
		assert.NoError(t, err)
		err = tx1.Commit()
		assert.NoError(t, err)

		// Arrange - Prepare to check availability for overlapping seats
		tx2, err := txManager.BeginTx(ctx)
		assert.NoError(t, err)
		defer tx2.Rollback()

		requestedSeats := []string{"B1", "B2", "B3", "B4"}

		// Act
		bookedSeats, err := repo.CheckSeatsAvailability(ctx, tx2, eventID, showtime, requestedSeats)

		// Assert
		assert.NoError(t, err)
		assert.ElementsMatch(t, []string{"B1", "B2"}, bookedSeats, "B1 and B2 should be already booked")

		// Clean up
		_, _ = db.Exec("DELETE FROM booking_seats WHERE booking_id = 'TEST-EXISTING-001'")
		_, _ = db.Exec("DELETE FROM bookings WHERE booking_id = 'TEST-EXISTING-001'")
	})

	t.Run("All requested seats already booked", func(t *testing.T) {
		// Arrange - Create a booking with all requested seats
		tx1, err := txManager.BeginTx(ctx)
		assert.NoError(t, err)

		eventID := "test-event-3"
		showtime := time.Date(2025, 12, 27, 21, 0, 0, 0, time.UTC)

		existingBooking := &Booking{
			BookingID: "TEST-EXISTING-002",
			EventID:   eventID,
			UserID:    "user-2",
			Showtime:  showtime,
			Quantity:  3,
			SeatIDs:   []string{"C1", "C2", "C3"},
			Status:    "CONFIRMED",
		}

		err = repo.CreateBooking(ctx, tx1, existingBooking)
		assert.NoError(t, err)
		err = tx1.Commit()
		assert.NoError(t, err)

		// Arrange - Prepare to check the same seats
		tx2, err := txManager.BeginTx(ctx)
		assert.NoError(t, err)
		defer tx2.Rollback()

		requestedSeats := []string{"C1", "C2", "C3"}

		// Act
		bookedSeats, err := repo.CheckSeatsAvailability(ctx, tx2, eventID, showtime, requestedSeats)

		// Assert
		assert.NoError(t, err)
		assert.ElementsMatch(t, []string{"C1", "C2", "C3"}, bookedSeats, "All seats should be already booked")

		// Clean up
		_, _ = db.Exec("DELETE FROM booking_seats WHERE booking_id = 'TEST-EXISTING-002'")
		_, _ = db.Exec("DELETE FROM bookings WHERE booking_id = 'TEST-EXISTING-002'")
	})

	t.Run("Cancelled bookings don't block seats", func(t *testing.T) {
		// Arrange - Create a cancelled booking
		tx1, err := txManager.BeginTx(ctx)
		assert.NoError(t, err)

		eventID := "test-event-4"
		showtime := time.Date(2025, 12, 28, 19, 0, 0, 0, time.UTC)

		cancelledBooking := &Booking{
			BookingID: "TEST-CANCELLED-001",
			EventID:   eventID,
			UserID:    "user-3",
			Showtime:  showtime,
			Quantity:  2,
			SeatIDs:   []string{"D1", "D2"},
			Status:    "CANCELLED", // Cancelled status
		}

		err = repo.CreateBooking(ctx, tx1, cancelledBooking)
		assert.NoError(t, err)
		err = tx1.Commit()
		assert.NoError(t, err)

		// Arrange - Prepare to check if seats are available
		tx2, err := txManager.BeginTx(ctx)
		assert.NoError(t, err)
		defer tx2.Rollback()

		requestedSeats := []string{"D1", "D2"}

		// Act
		bookedSeats, err := repo.CheckSeatsAvailability(ctx, tx2, eventID, showtime, requestedSeats)

		// Assert
		assert.NoError(t, err)
		assert.Empty(t, bookedSeats, "Cancelled bookings should not block seats")

		// Clean up
		_, _ = db.Exec("DELETE FROM booking_seats WHERE booking_id = 'TEST-CANCELLED-001'")
		_, _ = db.Exec("DELETE FROM bookings WHERE booking_id = 'TEST-CANCELLED-001'")
	})

	t.Run("Different showtimes don't conflict", func(t *testing.T) {
		// Arrange - Create a booking for one showtime
		tx1, err := txManager.BeginTx(ctx)
		assert.NoError(t, err)

		eventID := "test-event-5"
		showtime1 := time.Date(2025, 12, 29, 19, 0, 0, 0, time.UTC)
		showtime2 := time.Date(2025, 12, 29, 21, 0, 0, 0, time.UTC)

		booking1 := &Booking{
			BookingID: "TEST-SHOWTIME-001",
			EventID:   eventID,
			UserID:    "user-4",
			Showtime:  showtime1,
			Quantity:  2,
			SeatIDs:   []string{"E1", "E2"},
			Status:    "CONFIRMED",
		}

		err = repo.CreateBooking(ctx, tx1, booking1)
		assert.NoError(t, err)
		err = tx1.Commit()
		assert.NoError(t, err)

		// Arrange - Prepare to check availability for different showtime (same seats, different time)
		tx2, err := txManager.BeginTx(ctx)
		assert.NoError(t, err)
		defer tx2.Rollback()

		requestedSeats := []string{"E1", "E2"}

		// Act
		bookedSeats, err := repo.CheckSeatsAvailability(ctx, tx2, eventID, showtime2, requestedSeats)

		// Assert
		assert.NoError(t, err)
		assert.Empty(t, bookedSeats, "Same seats at different showtime should be available")

		// Clean up
		_, _ = db.Exec("DELETE FROM booking_seats WHERE booking_id = 'TEST-SHOWTIME-001'")
		_, _ = db.Exec("DELETE FROM bookings WHERE booking_id = 'TEST-SHOWTIME-001'")
	})
}

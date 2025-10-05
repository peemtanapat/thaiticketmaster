package booking

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// TestBookTickets_Success tests the happy path of booking tickets
// Following TDD: This test will FAIL because BookTickets doesn't exist yet
func TestBookTickets_Success(t *testing.T) {
	// Arrange: Set up test data and mock event-api
	ctx := context.Background()

	// Mock event-api response
	eventAPIServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Validate the request
		assert.Equal(t, "/api/v1/events/event-123", r.URL.Path)
		assert.Equal(t, "GET", r.Method)

		// Return a valid event with showtime (matching event-api format)
		event := map[string]interface{}{
			"id":            123,
			"name":          "Concert 2025",
			"showDateTimes": []string{"2025-10-10T19:00:00Z"},
			"location":      "Stadium",
		}
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		json.NewEncoder(w).Encode(event)
	}))
	defer eventAPIServer.Close()

	// Create booking request
	bookingReq := BookingRequest{
		EventID:  "event-123",
		UserID:   "user-456",
		Showtime: mustParseTime(t, "2025-10-10T19:00:00Z"),
		Quantity: 2,
		SeatIDs:  []string{"A1", "A2"},
	}

	// Create service with mock dependencies
	service := NewBookingServiceWithDefaults(nil, nil, eventAPIServer.URL)

	// Act: Call BookTickets
	err := service.BookTickets(ctx, bookingReq)

	// Assert: Should succeed with no error
	require.NoError(t, err, "BookTickets should succeed when event exists and showtime matches")
}

// TestBookTickets_EventNotFound tests when event-api returns 404
func TestBookTickets_EventNotFound(t *testing.T) {
	// Arrange
	ctx := context.Background()

	// Mock event-api to return 404
	eventAPIServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{
			"error": "Event not found",
		})
	}))
	defer eventAPIServer.Close()

	bookingReq := BookingRequest{
		EventID:  "non-existent",
		UserID:   "user-456",
		Showtime: mustParseTime(t, "2025-10-10T19:00:00Z"),
		Quantity: 2,
		SeatIDs:  []string{"A1", "A2"},
	}

	service := NewBookingServiceWithDefaults(nil, nil, eventAPIServer.URL)

	// Act
	err := service.BookTickets(ctx, bookingReq)

	// Assert
	require.Error(t, err, "BookTickets should fail when event not found")
	assert.Contains(t, err.Error(), "event not found")
}

// TestBookTickets_ShowtimeMismatch tests when requested showtime doesn't match event schedule
func TestBookTickets_ShowtimeMismatch(t *testing.T) {
	// Arrange
	ctx := context.Background()

	// Mock event-api to return 200 but with different showtime
	eventAPIServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"id":           "evt-789",
			"name":         "Test Event",
			"showtimes":    []string{"2025-11-11T20:00:00Z"},
			"totalSeats":   100,
			"soldOutSeats": []string{},
		})
	}))
	defer eventAPIServer.Close()

	bookingReq := BookingRequest{
		EventID:  "evt-789",
		UserID:   "user-789",
		Showtime: mustParseTime(t, "2025-12-25T18:00:00Z"), // Wrong showtime!
		Quantity: 1,
		SeatIDs:  []string{"B5"},
	}

	service := NewBookingServiceWithDefaults(nil, nil, eventAPIServer.URL)

	// Act
	err := service.BookTickets(ctx, bookingReq)

	// Assert
	require.Error(t, err, "BookTickets should fail when showtime doesn't exist")
	assert.Contains(t, err.Error(), "not available")
}

// TestBookTickets_WithTransaction tests that DB transaction is used
func TestBookTickets_WithTransaction(t *testing.T) {
	// This test will verify transaction handling
	// We'll implement this with actual testcontainers in the GREEN phase
	t.Skip("Will implement with testcontainers in GREEN phase")
}

// TestBookTickets_WithDistributedLock tests that distributed locking works
func TestBookTickets_WithDistributedLock(t *testing.T) {
	// This test will verify distributed locking with Redis
	// We'll implement this with testcontainers-redis in the GREEN phase
	t.Skip("Will implement with testcontainers in GREEN phase")
}

// Helper functions

func mustParseTime(t *testing.T, timeStr string) time.Time {
	parsedTime, err := time.Parse(time.RFC3339, timeStr)
	require.NoError(t, err)
	return parsedTime
}

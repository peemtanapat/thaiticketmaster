package booking

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestBookingHandler_CreateBooking_Success(t *testing.T) {
	// Arrange: Setup mock event API
	eventAPI := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		event := map[string]interface{}{
			"id":            123,
			"name":          "Test Concert",
			"showDateTimes": []string{"2025-10-10T19:00:00Z"},
			"location":      "Stadium",
		}
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(event)
	}))
	defer eventAPI.Close()

	// Create service and handler
	service := NewBookingServiceWithDefaults(nil, nil, eventAPI.URL)
	handler := NewBookingHandler(service)

	// Create request
	reqBody := CreateBookingRequest{
		EventID:  "event-123",
		UserID:   "user-456",
		Showtime: mustParseFlexibleTime(t, "2025-10-10T19:00:00Z"),
		Quantity: 2,
		SeatIDs:  []string{"A1", "A2"},
	}
	body, _ := json.Marshal(reqBody)
	req := httptest.NewRequest(http.MethodPost, "/api/v1/bookings", bytes.NewReader(body))
	rec := httptest.NewRecorder()

	// Act
	handler.CreateBooking(rec, req)

	// Assert
	assert.Equal(t, http.StatusCreated, rec.Code)

	var response CreateBookingResponse
	err := json.NewDecoder(rec.Body).Decode(&response)
	require.NoError(t, err)

	assert.True(t, response.Success)
	assert.Equal(t, "Booking created successfully", response.Message)
	assert.Equal(t, "event-123", response.Data.EventID)
	assert.Equal(t, "user-456", response.Data.UserID)
	assert.Equal(t, 2, response.Data.Quantity)
}

func TestBookingHandler_CreateBooking_InvalidJSON(t *testing.T) {
	// Arrange
	service := NewBookingServiceWithDefaults(nil, nil, "http://localhost:8080")
	handler := NewBookingHandler(service)

	req := httptest.NewRequest(http.MethodPost, "/api/v1/bookings", bytes.NewReader([]byte("invalid json")))
	rec := httptest.NewRecorder()

	// Act
	handler.CreateBooking(rec, req)

	// Assert
	assert.Equal(t, http.StatusBadRequest, rec.Code)

	var response ErrorResponse
	err := json.NewDecoder(rec.Body).Decode(&response)
	require.NoError(t, err)

	assert.False(t, response.Success)
	assert.Contains(t, response.Error, "Invalid request body")
}

func TestBookingHandler_CreateBooking_MissingEventID(t *testing.T) {
	// Arrange
	service := NewBookingServiceWithDefaults(nil, nil, "http://localhost:8080")
	handler := NewBookingHandler(service)

	reqBody := CreateBookingRequest{
		// EventID missing
		UserID:   "user-456",
		Showtime: FlexibleTime{Time: time.Now()},
		Quantity: 2,
		SeatIDs:  []string{"A1", "A2"},
	}
	body, _ := json.Marshal(reqBody)
	req := httptest.NewRequest(http.MethodPost, "/api/v1/bookings", bytes.NewReader(body))
	rec := httptest.NewRecorder()

	// Act
	handler.CreateBooking(rec, req)

	// Assert
	assert.Equal(t, http.StatusBadRequest, rec.Code)

	var response ErrorResponse
	err := json.NewDecoder(rec.Body).Decode(&response)
	require.NoError(t, err)

	assert.False(t, response.Success)
	assert.Contains(t, response.Error, "eventId is required")
}

func TestBookingHandler_CreateBooking_InvalidQuantity(t *testing.T) {
	// Arrange
	service := NewBookingServiceWithDefaults(nil, nil, "http://localhost:8080")
	handler := NewBookingHandler(service)

	reqBody := CreateBookingRequest{
		EventID:  "event-123",
		UserID:   "user-456",
		Showtime: FlexibleTime{Time: time.Now()},
		Quantity: 0, // Invalid
		SeatIDs:  []string{},
	}
	body, _ := json.Marshal(reqBody)
	req := httptest.NewRequest(http.MethodPost, "/api/v1/bookings", bytes.NewReader(body))
	rec := httptest.NewRecorder()

	// Act
	handler.CreateBooking(rec, req)

	// Assert
	assert.Equal(t, http.StatusBadRequest, rec.Code)

	var response ErrorResponse
	err := json.NewDecoder(rec.Body).Decode(&response)
	require.NoError(t, err)

	assert.False(t, response.Success)
	assert.Contains(t, response.Error, "quantity must be positive")
}

func TestBookingHandler_CreateBooking_SeatCountMismatch(t *testing.T) {
	// Arrange
	service := NewBookingServiceWithDefaults(nil, nil, "http://localhost:8080")
	handler := NewBookingHandler(service)

	reqBody := CreateBookingRequest{
		EventID:  "event-123",
		UserID:   "user-456",
		Showtime: FlexibleTime{Time: time.Now()},
		Quantity: 2,
		SeatIDs:  []string{"A1", "A2", "A3"}, // Mismatch
	}
	body, _ := json.Marshal(reqBody)
	req := httptest.NewRequest(http.MethodPost, "/api/v1/bookings", bytes.NewReader(body))
	rec := httptest.NewRecorder()

	// Act
	handler.CreateBooking(rec, req)

	// Assert
	assert.Equal(t, http.StatusBadRequest, rec.Code)

	var response ErrorResponse
	err := json.NewDecoder(rec.Body).Decode(&response)
	require.NoError(t, err)

	assert.False(t, response.Success)
	assert.Contains(t, response.Error, "number of seats must match quantity")
}

func TestBookingHandler_CreateBooking_EventNotFound(t *testing.T) {
	// Arrange: Mock event API returns 404
	eventAPI := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "Event not found"})
	}))
	defer eventAPI.Close()

	service := NewBookingServiceWithDefaults(nil, nil, eventAPI.URL)
	handler := NewBookingHandler(service)

	reqBody := CreateBookingRequest{
		EventID:  "non-existent",
		UserID:   "user-456",
		Showtime: mustParseFlexibleTime(t, "2025-10-10T19:00:00Z"),
		Quantity: 2,
		SeatIDs:  []string{"A1", "A2"},
	}
	body, _ := json.Marshal(reqBody)
	req := httptest.NewRequest(http.MethodPost, "/api/v1/bookings", bytes.NewReader(body))
	rec := httptest.NewRecorder()

	// Act
	handler.CreateBooking(rec, req)

	// Assert
	assert.Equal(t, http.StatusNotFound, rec.Code)

	var response ErrorResponse
	err := json.NewDecoder(rec.Body).Decode(&response)
	require.NoError(t, err)

	assert.False(t, response.Success)
	assert.Contains(t, response.Error, "event not found")
}

func TestBookingHandler_CreateBooking_ShowtimeMismatch(t *testing.T) {
	// Arrange: Mock event API with different showtime
	eventAPI := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		event := map[string]interface{}{
			"id":            123,
			"name":          "Test Concert",
			"showDateTimes": []string{"2025-10-10T19:00:00Z"},
			"location":      "Stadium",
		}
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(event)
	}))
	defer eventAPI.Close()

	service := NewBookingServiceWithDefaults(nil, nil, eventAPI.URL)
	handler := NewBookingHandler(service)

	reqBody := CreateBookingRequest{
		EventID:  "event-123",
		UserID:   "user-456",
		Showtime: mustParseFlexibleTime(t, "2025-10-11T19:00:00Z"), // Different showtime
		Quantity: 2,
		SeatIDs:  []string{"A1", "A2"},
	}
	body, _ := json.Marshal(reqBody)
	req := httptest.NewRequest(http.MethodPost, "/api/v1/bookings", bytes.NewReader(body))
	rec := httptest.NewRecorder()

	// Act
	handler.CreateBooking(rec, req)

	// Assert
	assert.Equal(t, http.StatusBadRequest, rec.Code)

	var response ErrorResponse
	err := json.NewDecoder(rec.Body).Decode(&response)
	require.NoError(t, err)

	assert.False(t, response.Success)
	assert.Contains(t, response.Error, "showtime")
	assert.Contains(t, response.Error, "not available")
}

func TestBookingHandler_CreateBooking_MethodNotAllowed(t *testing.T) {
	// Arrange
	service := NewBookingServiceWithDefaults(nil, nil, "http://localhost:8080")
	handler := NewBookingHandler(service)

	req := httptest.NewRequest(http.MethodGet, "/api/v1/bookings", nil)
	rec := httptest.NewRecorder()

	// Act
	handler.CreateBooking(rec, req)

	// Assert
	assert.Equal(t, http.StatusMethodNotAllowed, rec.Code)

	var response ErrorResponse
	err := json.NewDecoder(rec.Body).Decode(&response)
	require.NoError(t, err)

	assert.False(t, response.Success)
	assert.Contains(t, response.Error, "Method not allowed")
}

func TestBookingHandler_GetBooking_NotImplemented(t *testing.T) {
	// Arrange
	service := NewBookingServiceWithDefaults(nil, nil, "http://localhost:8080")
	handler := NewBookingHandler(service)

	req := httptest.NewRequest(http.MethodGet, "/api/v1/bookings/123", nil)
	rec := httptest.NewRecorder()

	// Act
	handler.GetBooking(rec, req)

	// Assert
	assert.Equal(t, http.StatusNotImplemented, rec.Code)
}

func TestBookingHandler_CancelBooking_NotImplemented(t *testing.T) {
	// Arrange
	service := NewBookingServiceWithDefaults(nil, nil, "http://localhost:8080")
	handler := NewBookingHandler(service)

	req := httptest.NewRequest(http.MethodDelete, "/api/v1/bookings/123", nil)
	rec := httptest.NewRecorder()

	// Act
	handler.CancelBooking(rec, req)

	// Assert
	assert.Equal(t, http.StatusNotImplemented, rec.Code)
}

// Helper function to create FlexibleTime from time.Time
func mustParseFlexibleTime(t *testing.T, timeStr string) FlexibleTime {
	parsedTime, err := time.Parse(time.RFC3339, timeStr)
	require.NoError(t, err)
	return FlexibleTime{Time: parsedTime}
}

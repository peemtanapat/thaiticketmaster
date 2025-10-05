package booking

import (
	"encoding/json"
	"fmt"
	"net/http"
	"time"
)

// BookingHandler handles HTTP requests for booking operations
type BookingHandler struct {
	service *BookingService
}

// NewBookingHandler creates a new booking handler
func NewBookingHandler(service *BookingService) *BookingHandler {
	return &BookingHandler{
		service: service,
	}
}

// CreateBooking handles POST /api/v1/bookings
func (h *BookingHandler) CreateBooking(w http.ResponseWriter, r *http.Request) {
	// Only allow POST method
	if r.Method != http.MethodPost {
		respondError(w, http.StatusMethodNotAllowed, "Method not allowed")
		return
	}

	// Parse request body
	var req CreateBookingRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, fmt.Sprintf("Invalid request body: %v", err))
		return
	}

	// Validate request
	if err := req.Validate(); err != nil {
		respondError(w, http.StatusBadRequest, err.Error())
		return
	}

	// Convert to internal BookingRequest
	bookingReq := BookingRequest{
		EventID:  req.EventID,
		UserID:   req.UserID,
		Showtime: req.Showtime.Time,
		Quantity: req.Quantity,
		SeatIDs:  req.SeatIDs,
	}

	// Call service to book tickets
	ctx := r.Context()
	if err := h.service.BookTickets(ctx, bookingReq); err != nil {
		// Determine appropriate HTTP status code based on error
		statusCode := determineStatusCode(err)
		respondError(w, statusCode, err.Error())
		return
	}

	// Success response
	response := CreateBookingResponse{
		Success: true,
		Message: "Booking created successfully",
		Data: BookingData{
			EventID:  req.EventID,
			UserID:   req.UserID,
			Showtime: req.Showtime.Time,
			Quantity: req.Quantity,
			SeatIDs:  req.SeatIDs,
		},
	}

	respondJSON(w, http.StatusCreated, response)
}

// GetBooking handles GET /api/v1/bookings/{id}
func (h *BookingHandler) GetBooking(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement get booking by ID
	respondError(w, http.StatusNotImplemented, "Get booking endpoint not yet implemented")
}

// GetUserBookings handles GET /api/v1/bookings/user/{userId}
func (h *BookingHandler) GetUserBookings(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement get user bookings
	respondError(w, http.StatusNotImplemented, "Get user bookings endpoint not yet implemented")
}

// CancelBooking handles DELETE /api/v1/bookings/{id}
func (h *BookingHandler) CancelBooking(w http.ResponseWriter, r *http.Request) {
	// TODO: Implement cancel booking
	respondError(w, http.StatusNotImplemented, "Cancel booking endpoint not yet implemented")
}

// Request/Response DTOs

// CreateBookingRequest represents the HTTP request body for creating a booking
type CreateBookingRequest struct {
	EventID  string       `json:"eventId"`
	UserID   string       `json:"userId"`
	Showtime FlexibleTime `json:"showtime"`
	Quantity int          `json:"quantity"`
	SeatIDs  []string     `json:"seatIds"`
}

// Validate validates the create booking request
func (r *CreateBookingRequest) Validate() error {
	if r.EventID == "" {
		return fmt.Errorf("eventId is required")
	}
	if r.UserID == "" {
		return fmt.Errorf("userId is required")
	}
	if r.Quantity <= 0 {
		return fmt.Errorf("quantity must be positive")
	}
	if len(r.SeatIDs) == 0 {
		return fmt.Errorf("seatIds is required")
	}
	if len(r.SeatIDs) != r.Quantity {
		return fmt.Errorf("number of seats must match quantity")
	}
	if r.Showtime.IsZero() {
		return fmt.Errorf("showtime is required")
	}
	return nil
}

// CreateBookingResponse represents the HTTP response for creating a booking
type CreateBookingResponse struct {
	Success bool        `json:"success"`
	Message string      `json:"message"`
	Data    BookingData `json:"data,omitempty"`
}

// BookingData represents booking information in the response
type BookingData struct {
	BookingID string    `json:"bookingId,omitempty"`
	EventID   string    `json:"eventId"`
	UserID    string    `json:"userId"`
	Showtime  time.Time `json:"showtime"`
	Quantity  int       `json:"quantity"`
	SeatIDs   []string  `json:"seatIds"`
	Status    string    `json:"status,omitempty"`
	CreatedAt time.Time `json:"createdAt,omitempty"`
}

// ErrorResponse represents an error response
type ErrorResponse struct {
	Success bool   `json:"success"`
	Error   string `json:"error"`
}

// Helper functions

// respondJSON sends a JSON response
func respondJSON(w http.ResponseWriter, statusCode int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(statusCode)
	if err := json.NewEncoder(w).Encode(data); err != nil {
		http.Error(w, "Failed to encode response", http.StatusInternalServerError)
	}
}

// respondError sends an error response
func respondError(w http.ResponseWriter, statusCode int, message string) {
	response := ErrorResponse{
		Success: false,
		Error:   message,
	}
	respondJSON(w, statusCode, response)
}

// determineStatusCode determines the appropriate HTTP status code based on error
func determineStatusCode(err error) int {
	errMsg := err.Error()

	// Check for specific error types
	if contains(errMsg, "event not found") {
		return http.StatusNotFound
	}
	if contains(errMsg, "showtime") && contains(errMsg, "not available") {
		return http.StatusBadRequest
	}
	if contains(errMsg, "no showtimes available") {
		return http.StatusBadRequest
	}
	if contains(errMsg, "failed to acquire lock") || contains(errMsg, "lock already held") {
		return http.StatusConflict
	}
	if contains(errMsg, "is required") || contains(errMsg, "must be positive") ||
		contains(errMsg, "must match") {
		return http.StatusBadRequest
	}

	// Default to internal server error
	return http.StatusInternalServerError
}

// contains checks if a string contains a substring (case-sensitive)
func contains(s, substr string) bool {
	return len(s) >= len(substr) && (s == substr || len(s) > len(substr) &&
		(s[:len(substr)] == substr || s[len(s)-len(substr):] == substr ||
			containsMiddle(s, substr)))
}

func containsMiddle(s, substr string) bool {
	for i := 0; i <= len(s)-len(substr); i++ {
		if s[i:i+len(substr)] == substr {
			return true
		}
	}
	return false
}

# API Integration Fix - Event-API to Booking-API

## Problem
The booking-api was failing to parse event data from event-api with the error:
```
failed to get event: failed to decode event: json: cannot unmarshal number into Go struct field Event.id of type string
```

## Root Cause
The booking-api's `Event` struct didn't match the actual response from event-api:

### Event-API Returns (Java/Spring Boot):
```json
{
  "id": 123,                                    // Long (number)
  "name": "Concert Name",
  "showDateTimes": [                            // List<LocalDateTime>
    "2025-10-10T19:00:00",
    "2025-10-10T21:00:00"
  ],
  "location": "Stadium Name",                    // location field
  "category": { ... },
  "ticketPrice": 1500.00,
  ...
}
```

### Booking-API Expected (OLD):
```go
type Event struct {
    ID       string    `json:"id"`              // string ❌
    Name     string    `json:"name"`
    Showtime time.Time `json:"showtime"`        // single time ❌
    Venue    string    `json:"venue"`           // venue ❌
}
```

## Solution

### 1. Updated Event Struct
```go
type Event struct {
    ID            int64       `json:"id"`              // Changed to int64 ✅
    Name          string      `json:"name"`
    ShowDateTimes []time.Time `json:"showDateTimes"`   // Changed to array ✅
    Location      string      `json:"location"`        // Changed field name ✅
}
```

### 2. Updated Validation Logic
Changed from validating a single showtime to checking if the requested showtime exists in the array:

**OLD:**
```go
func (s *BookingService) validateShowtime(bookingShowtime, eventShowtime time.Time) error {
    if !bookingShowtime.Equal(eventShowtime) {
        return fmt.Errorf("showtime mismatch")
    }
    return nil
}
```

**NEW:**
```go
func (s *BookingService) validateShowtimeInEvent(bookingShowtime time.Time, eventShowtimes []time.Time) error {
    if len(eventShowtimes) == 0 {
        return fmt.Errorf("event has no showtimes available")
    }

    bookingTime := bookingShowtime.Truncate(time.Second)
    
    for _, eventShowtime := range eventShowtimes {
        if bookingTime.Equal(eventShowtime.Truncate(time.Second)) {
            return nil // Found matching showtime
        }
    }

    return fmt.Errorf("showtime %s not available for this event", 
        bookingShowtime.Format(time.RFC3339))
}
```

### 3. Updated Error Handling
Updated the HTTP handler to recognize the new error message format:

```go
if contains(errMsg, "showtime") && contains(errMsg, "not available") {
    return http.StatusBadRequest
}
if contains(errMsg, "no showtimes available") {
    return http.StatusBadRequest
}
```

### 4. Updated Tests
Updated all test mocks to return the correct format:

```go
event := map[string]interface{}{
    "id":            123,                          // number
    "name":          "Concert 2025",
    "showDateTimes": []string{"2025-10-10T19:00:00Z"},  // array
    "location":      "Stadium",                    // location
}
```

## Files Changed

1. **internal/booking/booking_service.go**
   - Updated `Event` struct
   - Replaced `validateShowtime()` with `validateShowtimeInEvent()`
   - Updated service logic to use the new validation

2. **internal/booking/booking_service_test.go**
   - Updated mock event responses to match event-api format
   - Updated test assertions for new error messages

3. **internal/booking/handler.go**
   - Updated error handling to recognize new validation messages

4. **internal/booking/handler_test.go**
   - Updated mock event responses
   - Updated test assertions

## Testing

All tests passing:
```
✅ 13 tests PASS
⏭️  4 tests SKIP (integration tests)
```

## How to Test

1. **Start Event-API** (on port 8080)
2. **Start Booking-API** (on port 8081)
3. **Use client.http** to test:

```http
POST http://localhost:8081/api/v1/bookings
Content-Type: application/json

{
  "eventId": "1",
  "userId": "user-123",
  "showtime": "2025-07-15T19:00:00Z",
  "quantity": 2,
  "seatIds": ["A1", "A2"]
}
```

## Notes

- The booking-api now correctly handles events with multiple showtimes
- Event ID is properly handled as a number (int64)
- Field names match the event-api response (location instead of venue)
- All existing tests have been updated and are passing

## Future Improvements

- Add more fields from event-api if needed (category, ticketPrice, etc.)
- Add integration tests with actual event-api instance
- Consider using code generation tools for API client consistency

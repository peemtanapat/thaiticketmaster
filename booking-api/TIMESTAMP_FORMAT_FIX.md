# Timestamp Format Compatibility Fix

## Problem
The booking-api was rejecting requests with Java LocalDateTime format timestamps:
```
"Invalid request body: parsing time \"2025-07-15T19:00:00\" as \"2006-01-02T15:04:05Z07:00\": cannot parse \"\" as \"Z07:00\""
```

## Root Cause
Go's default `time.Time` JSON unmarshaler only accepts RFC3339 format (with timezone):
- ✅ Accepts: `"2025-07-15T19:00:00Z"` or `"2025-07-15T19:00:00+07:00"`
- ❌ Rejects: `"2025-07-15T19:00:00"` (no timezone)

However, Java's `LocalDateTime` serializes **without timezone information**, causing incompatibility between:
- Client requests (may use LocalDateTime format)
- Event-API responses (uses LocalDateTime format from Spring Boot)

## Solution

### Created FlexibleTime Type
Implemented a custom time type that accepts multiple timestamp formats:

```go
type FlexibleTime struct {
    time.Time
}

func (ft *FlexibleTime) UnmarshalJSON(b []byte) error {
    formats := []string{
        time.RFC3339,                 // "2006-01-02T15:04:05Z07:00"
        "2006-01-02T15:04:05",        // Java LocalDateTime format
        time.RFC3339Nano,             // with nanoseconds + timezone
        "2006-01-02T15:04:05.999999", // with nanoseconds, no timezone
    }
    
    // Try each format until one succeeds
    for _, format := range formats {
        t, err := time.Parse(format, s)
        if err == nil {
            ft.Time = t
            return nil
        }
    }
    return error
}
```

### Updated Request DTO
```go
type CreateBookingRequest struct {
    EventID  string       `json:"eventId"`
    UserID   string       `json:"userId"`
    Showtime FlexibleTime `json:"showtime"`  // Changed from time.Time
    Quantity int          `json:"quantity"`
    SeatIDs  []string     `json:"seatIds"`
}
```

### Updated Handler Logic
```go
bookingReq := BookingRequest{
    Showtime: req.Showtime.Time,  // Access embedded time.Time
    // ... other fields
}
```

## Supported Timestamp Formats

The API now accepts **all** of these formats:

1. **RFC3339 with UTC**: `"2025-07-15T19:00:00Z"`
2. **RFC3339 with timezone**: `"2025-07-15T19:00:00+07:00"`
3. **LocalDateTime (no timezone)**: `"2025-07-15T19:00:00"` ✨ NEW
4. **With nanoseconds + timezone**: `"2025-07-15T19:00:00.123456789Z"`
5. **With nanoseconds, no timezone**: `"2025-07-15T19:00:00.123456"` ✨ NEW

## Files Changed

1. **internal/booking/handler.go**
   - Added `FlexibleTime` type with custom JSON unmarshaling
   - Updated `CreateBookingRequest.Showtime` to use `FlexibleTime`
   - Updated handler logic to use `req.Showtime.Time`

2. **internal/booking/handler_test.go**
   - Added `mustParseFlexibleTime()` helper function
   - Updated all test cases to use `FlexibleTime`

3. **client.http**
   - Added documentation about supported timestamp formats
   - Added example showing LocalDateTime format

## Testing

All tests passing:
```
✅ 13 tests PASS
⏭️  4 tests SKIP (integration tests)
```

## Examples

### Valid Request (RFC3339 with timezone)
```json
POST /api/v1/bookings
{
  "eventId": "1",
  "userId": "user-123",
  "showtime": "2025-07-15T19:00:00Z",
  "quantity": 2,
  "seatIds": ["A1", "A2"]
}
```

### Valid Request (LocalDateTime format - NEW!)
```json
POST /api/v1/bookings
{
  "eventId": "1",
  "userId": "user-123",
  "showtime": "2025-07-15T19:00:00",
  "quantity": 2,
  "seatIds": ["A1", "A2"]
}
```

Both formats now work! ✅

## Benefits

1. **Better compatibility** with Java/Spring Boot backends
2. **More flexible** - accepts multiple timestamp formats
3. **No breaking changes** - still accepts original RFC3339 format
4. **Client-friendly** - clients can use simpler format without timezone
5. **Event-API compatible** - matches the LocalDateTime format from event-api

## Technical Notes

- `FlexibleTime` embeds `time.Time`, so it inherits all `time.Time` methods
- The parser tries formats in order, returning on first match
- Always outputs as RFC3339 when marshaling to JSON (for consistency)
- Timezone-less timestamps are parsed as UTC

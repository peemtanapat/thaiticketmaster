# PostgreSQL Array Parameter Fix

## Issue

When checking seat availability, the application threw an error:

```
failed to check seat availability: sql: converting argument $3 type: 
unsupported type []string, a slice of string
```

## Root Cause

The PostgreSQL driver (`github.com/lib/pq`) doesn't directly support Go slices as query parameters. When passing `[]string` to a query with `ANY($3)`, the driver cannot automatically convert it to PostgreSQL's array type.

## Solution

Use `pq.Array()` to wrap the slice before passing it to the query:

### Before (Error)
```go
rows, err := sqlTx.QueryContext(ctx, query, eventID, showtime, seatIDs)
```

### After (Fixed)
```go
import "github.com/lib/pq"

rows, err := sqlTx.QueryContext(ctx, query, eventID, showtime, pq.Array(seatIDs))
```

## Code Changes

**File:** `internal/booking/repository.go`

1. **Added import:**
   ```go
   import (
       // ... existing imports
       "github.com/lib/pq"
   )
   ```

2. **Updated CheckSeatsAvailability method:**
   ```go
   func (r *PostgresBookingRepository) CheckSeatsAvailability(
       ctx context.Context, 
       tx Transaction, 
       eventID string, 
       showtime time.Time, 
       seatIDs []string,
   ) ([]string, error) {
       // ... validation and setup
       
       query := `
           SELECT DISTINCT bs.seat_id
           FROM booking_seats bs
           INNER JOIN bookings b ON bs.booking_id = b.booking_id
           WHERE b.event_id = $1
             AND b.showtime = $2
             AND b.status = 'CONFIRMED'
             AND bs.seat_id = ANY($3)  -- PostgreSQL array operator
       `
       
       // Use pq.Array() to convert Go slice to PostgreSQL array
       rows, err := sqlTx.QueryContext(ctx, query, eventID, showtime, pq.Array(seatIDs))
       // ...
   }
   ```

## How pq.Array() Works

The `pq.Array()` function creates a wrapper that:
1. Implements the `driver.Valuer` interface
2. Converts Go slices to PostgreSQL array literals
3. Properly escapes and formats the array syntax

**Example conversion:**
```go
Go:         []string{"A1", "A2", "A3"}
            ↓ pq.Array()
PostgreSQL: ARRAY['A1', 'A2', 'A3']
```

## Testing

### Build
```bash
make build
```
**Result:** ✅ Success

### Unit Tests
```bash
make test
```
**Result:** ✅ All 12 tests passing

### Manual Test
```bash
# Start booking-api
make run

# Create first booking (should succeed)
curl -X POST http://localhost:8081/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "1",
    "userId": "user-123",
    "showtime": "2025-07-15T19:00:00",
    "quantity": 2,
    "seatIds": ["A1", "A2"]
  }'

# Try duplicate (should fail with proper error)
curl -X POST http://localhost:8081/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "1",
    "userId": "user-456",
    "showtime": "2025-07-15T19:00:00",
    "quantity": 2,
    "seatIds": ["A1", "A2"]
  }'
```

**Expected:**
- First request: `200 OK` ✅
- Second request: `400 Bad Request` with error message `"seats already booked: [A1 A2]"` ❌

## Similar Issues to Watch For

Whenever working with PostgreSQL arrays in Go queries, always use `pq.Array()`:

```go
// ❌ WRONG - Will fail
db.Query("SELECT * FROM table WHERE col = ANY($1)", []string{"a", "b"})

// ✅ CORRECT
db.Query("SELECT * FROM table WHERE col = ANY($1)", pq.Array([]string{"a", "b"}))
```

## PostgreSQL Array Operators

Common PostgreSQL array operators that require `pq.Array()`:

- `= ANY($1)` - Value equals any array element
- `!= ALL($1)` - Value not equal to all elements
- `@>` - Array contains
- `<@` - Array is contained by
- `&&` - Arrays overlap

## Documentation Updated

- ✅ Code fixed in `repository.go`
- ✅ Import added for `github.com/lib/pq`
- ✅ All tests passing
- ✅ Build successful

---

**The seat availability check now works correctly with PostgreSQL arrays! 🎉**

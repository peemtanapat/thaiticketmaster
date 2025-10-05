# Duplicate Booking Prevention

## Overview

The booking-api now prevents duplicate bookings by checking seat availability **before** creating a booking. This ensures that confirmed seats cannot be booked again for the same event and showtime.

## How It Works

### 1. Seat Availability Check

Before creating a booking, the system checks if any of the requested seats are already booked:

```go
// Check seat availability within transaction
bookedSeats, err := repository.CheckSeatsAvailability(ctx, tx, eventID, showtime, seatIDs)
if len(bookedSeats) > 0 {
    return fmt.Errorf("seats already booked: %v", bookedSeats)
}
```

### 2. Query Logic

The `CheckSeatsAvailability` method queries the database for existing bookings:

```sql
SELECT DISTINCT bs.seat_id
FROM booking_seats bs
INNER JOIN bookings b ON bs.booking_id = b.booking_id
WHERE b.event_id = $1
  AND b.showtime = $2
  AND b.status = 'CONFIRMED'  -- Only check CONFIRMED bookings
  AND bs.seat_id = ANY($3)
```

**Key Points:**
- Only checks `CONFIRMED` bookings (not `CANCELLED`)
- Checks exact event ID and showtime match
- Uses transaction to ensure consistency
- Returns list of already-booked seats

### 3. Transaction Safety

The check happens **within the same transaction** as the booking creation:

```
1. Start transaction
2. Check seat availability (within transaction)
3. If seats available: Create booking
4. If seats taken: Return error, rollback
5. Commit transaction
```

This prevents race conditions where two concurrent requests try to book the same seats.

## Booking Flow with Prevention

```
Client Request
    ↓
Acquire Lock (Redis)
    ↓
Start Transaction
    ↓
Validate Event
    ↓
Validate Showtime
    ↓
✨ Check Seat Availability (NEW!)
    ↓
    ├─ If seats available → Create Booking → Commit
    └─ If seats taken → Return Error → Rollback
```

## API Behavior

### Successful Booking (Seats Available)

**Request:**
```http
POST http://localhost:8081/api/v1/bookings
Content-Type: application/json

{
  "eventId": "1",
  "userId": "user-123",
  "showtime": "2025-07-15T19:00:00",
  "quantity": 2,
  "seatIds": ["A1", "A2"]
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Booking created successfully",
  "data": {
    "eventId": "1",
    "userId": "user-123",
    "showtime": "2025-07-15T19:00:00Z",
    "quantity": 2,
    "seatIds": ["A1", "A2"]
  }
}
```

### Failed Booking (Seats Already Taken)

**Request:**
```http
POST http://localhost:8081/api/v1/bookings
Content-Type: application/json

{
  "eventId": "1",
  "userId": "user-456",
  "showtime": "2025-07-15T19:00:00",
  "quantity": 2,
  "seatIds": ["A1", "A2"]  // Same seats!
}
```

**Response:** `400 Bad Request`
```json
{
  "error": "seats already booked: [A1 A2]"
}
```

### Partial Conflict

If only some seats are taken:

**Request:**
```http
POST http://localhost:8081/api/v1/bookings
Content-Type: application/json

{
  "eventId": "1",
  "userId": "user-789",
  "showtime": "2025-07-15T19:00:00",
  "quantity": 3,
  "seatIds": ["A1", "A3", "A4"]  // A1 is taken, A3 and A4 are free
}
```

**Response:** `400 Bad Request`
```json
{
  "error": "seats already booked: [A1]"
}
```

**Important:** The entire booking fails if **any** seat is unavailable. This is an all-or-nothing operation.

## Edge Cases Handled

### 1. Cancelled Bookings Don't Block Seats

```sql
WHERE b.status = 'CONFIRMED'  -- Only checks CONFIRMED status
```

If a booking is `CANCELLED`, those seats become available again.

**Example:**
1. User A books seats A1, A2 → Status: CONFIRMED
2. User A cancels → Status: CANCELLED
3. User B can now book A1, A2 ✅

### 2. Different Showtimes Don't Conflict

The check includes showtime:

```sql
WHERE b.showtime = $2
```

Same seats can be booked for different showtimes of the same event.

**Example:**
- Event: "Concert"
- Showtime 1: 2025-12-25 19:00 → Seats A1, A2 booked by User A
- Showtime 2: 2025-12-25 21:00 → Seats A1, A2 available for User B ✅

### 3. Different Events Don't Conflict

The check includes event ID:

```sql
WHERE b.event_id = $1
```

Same seat IDs in different events don't conflict.

**Example:**
- Event 1: "Concert" → Seat A1 booked
- Event 2: "Theater" → Seat A1 available ✅

### 4. Concurrent Bookings (Race Condition Prevention)

**Scenario:** Two users try to book the same seats simultaneously.

**Protection:**
1. Redis distributed lock (per event)
2. Database transaction isolation
3. Seat availability check within transaction

**Result:** Only one booking succeeds, the other gets "seats already booked" error.

## Implementation Details

### New Repository Method

```go
// CheckSeatsAvailability checks if requested seats are available
// Returns list of already-booked seats (empty = all available)
func (r *PostgresBookingRepository) CheckSeatsAvailability(
    ctx context.Context, 
    tx Transaction, 
    eventID string, 
    showtime time.Time, 
    seatIDs []string,
) ([]string, error) {
    // Query for already-booked seats
    // Returns []string of booked seat IDs
}
```

### Updated BookingService Flow

```go
func (s *BookingService) BookTickets(ctx context.Context, req BookingRequest) error {
    // 1. Lock
    // 2. Transaction
    // 3. Validate event
    // 4. Validate request
    
    // 5. Check seat availability (NEW!)
    bookedSeats, err := s.repository.CheckSeatsAvailability(
        ctx, tx, req.EventID, req.Showtime, req.SeatIDs)
    if err != nil {
        return fmt.Errorf("failed to check seat availability: %w", err)
    }
    if len(bookedSeats) > 0 {
        return fmt.Errorf("seats already booked: %v", bookedSeats)
    }
    
    // 6. Create booking
    // 7. Commit
}
```

## Testing

### Unit Tests

All existing tests still pass:
```
✅ TestBookTickets_Success
✅ TestBookTickets_EventNotFound
✅ TestBookTickets_ShowtimeMismatch
✅ All handler tests
```

### Integration Tests

New integration test file: `duplicate_prevention_test.go`

Tests included:
1. **No booked seats** - All available
2. **Some seats booked** - Partial conflict detection
3. **All seats booked** - Full conflict detection
4. **Cancelled bookings** - Don't block seats
5. **Different showtimes** - No conflict

Run integration tests:
```bash
# Requires PostgreSQL running
go test -v ./internal/booking/duplicate_prevention_test.go
```

### Manual Testing

#### Test 1: Create First Booking
```bash
curl -X POST http://localhost:8081/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "1",
    "userId": "user-123",
    "showtime": "2025-07-15T19:00:00",
    "quantity": 2,
    "seatIds": ["A1", "A2"]
  }'
```

**Expected:** Success ✅

#### Test 2: Try Duplicate Booking
```bash
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

**Expected:** Error - "seats already booked: [A1 A2]" ❌

#### Test 3: Partial Overlap
```bash
curl -X POST http://localhost:8081/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "1",
    "userId": "user-789",
    "showtime": "2025-07-15T19:00:00",
    "quantity": 3,
    "seatIds": ["A1", "A3", "A4"]
  }'
```

**Expected:** Error - "seats already booked: [A1]" ❌

#### Test 4: Different Showtime (Should Work)
```bash
curl -X POST http://localhost:8081/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "1",
    "userId": "user-999",
    "showtime": "2025-07-15T21:00:00",
    "quantity": 2,
    "seatIds": ["A1", "A2"]
  }'
```

**Expected:** Success ✅ (different showtime)

### Verify in Database

```bash
# Connect to database
docker exec -it booking-postgres psql -U admin -d booking_db

# View all bookings with seats
SELECT b.booking_id, b.event_id, b.user_id, b.showtime, b.status,
       array_agg(bs.seat_id) as seats
FROM bookings b
LEFT JOIN booking_seats bs ON b.booking_id = bs.booking_id
GROUP BY b.id
ORDER BY b.created_at DESC;
```

**Expected Output:**
```
              booking_id               | event_id | user_id  |      showtime       |  status   |   seats
---------------------------------------+----------+----------+---------------------+-----------+----------
 BK-xxx-1                              |    1     | user-123 | 2025-07-15 19:00:00 | CONFIRMED | {A1,A2}
 BK-xxx-2                              |    1     | user-999 | 2025-07-15 21:00:00 | CONFIRMED | {A1,A2}
```

Notice: Same seats (A1, A2) can be booked for different showtimes.

## Performance Considerations

### Database Query Optimization

The availability check uses:
- **Indexes:** `idx_bookings_event_id`, `idx_booking_seats_booking_id`
- **JOIN:** Efficiently combines bookings and seats
- **Array operator:** `seat_id = ANY($3)` for multi-seat check

### Transaction Overhead

- Minimal: One additional SELECT query per booking
- Query is fast due to indexes
- Happens within existing transaction (no extra connection)

### Lock Duration

- Redis lock held during entire booking process
- Prevents concurrent bookings for same event
- Lock released after commit/rollback

## Security

### SQL Injection Prevention

All queries use parameterized statements:
```go
rows, err := sqlTx.QueryContext(ctx, query, eventID, showtime, seatIDs)
```

### Race Condition Prevention

1. **Redis Lock** - Prevents concurrent bookings for same event
2. **Transaction** - Ensures atomic operations
3. **Database Check** - Validates seat availability within transaction

## Error Messages

| Scenario | Error Message | HTTP Status |
|----------|--------------|-------------|
| Seats available | N/A (Success) | 200 |
| All seats taken | `seats already booked: [A1 A2]` | 400 |
| Some seats taken | `seats already booked: [A1]` | 400 |
| Database error | `failed to check seat availability: ...` | 500 |

## Summary

### ✅ What's Implemented

- [x] Seat availability check before booking
- [x] Query checks CONFIRMED bookings only
- [x] Transaction-safe checking (prevents race conditions)
- [x] Clear error messages with seat details
- [x] Edge cases handled (cancelled bookings, different showtimes)
- [x] Integration tests for duplicate prevention
- [x] No performance impact (uses existing indexes)

### 🎯 Key Benefits

1. **Prevents Double Booking** - Same seats can't be booked twice
2. **Transaction Safety** - Check happens within transaction
3. **Clear Errors** - Users know which seats are unavailable
4. **Flexible** - Cancelled bookings release seats
5. **Efficient** - Uses database indexes, minimal overhead

### 📝 Usage

The feature works automatically - no code changes needed in client:
```
POST /api/v1/bookings → Automatically checks seat availability
```

If seats are taken, API returns error with list of unavailable seats.

---

**The booking-api now fully prevents duplicate bookings! 🎉**

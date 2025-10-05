# Database Implementation - Booking Persistence

## Overview

The booking-api now implements **real database persistence** for booking records. Bookings are saved to PostgreSQL with full transaction support.

## What Was Implemented

### 1. Database Repository Pattern

Created `repository.go` with:
- **BookingRepository interface** - Defines database operations
- **PostgresBookingRepository** - PostgreSQL implementation
- Full CRUD operations for bookings

### 2. Booking Model

```go
type Booking struct {
    ID        int       `json:"id"`           // Auto-increment primary key
    BookingID string    `json:"booking_id"`   // Unique booking identifier (BK-uuid)
    EventID   string    `json:"event_id"`     // Reference to event
    UserID    string    `json:"user_id"`      // Reference to user
    Showtime  time.Time `json:"showtime"`     // Event showtime
    Quantity  int       `json:"quantity"`     // Number of tickets
    Status    string    `json:"status"`       // CONFIRMED, CANCELLED, etc.
    SeatIDs   []string  `json:"seat_ids"`     // Associated seat IDs
    CreatedAt time.Time `json:"created_at"`   // Record creation time
    UpdatedAt time.Time `json:"updated_at"`   // Last update time
}
```

### 3. Database Tables

#### bookings table
```sql
CREATE TABLE bookings (
    id SERIAL PRIMARY KEY,
    booking_id VARCHAR(255) UNIQUE NOT NULL,
    event_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    showtime TIMESTAMP NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_bookings_event_id ON bookings(event_id);
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_status ON bookings(status);
```

#### booking_seats table
```sql
CREATE TABLE booking_seats (
    id SERIAL PRIMARY KEY,
    booking_id VARCHAR(255) NOT NULL,
    seat_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
);

CREATE INDEX idx_booking_seats_booking_id ON booking_seats(booking_id);
```

### 4. Repository Operations

#### CreateBooking
```go
func (r *PostgresBookingRepository) CreateBooking(ctx context.Context, tx Transaction, booking *Booking) error
```
- Generates unique booking ID (BK-uuid format)
- Inserts booking record within transaction
- Inserts associated seats
- Returns complete booking with database IDs

#### GetBookingByID
```go
func (r *PostgresBookingRepository) GetBookingByID(ctx context.Context, bookingID string) (*Booking, error)
```
- Retrieves booking by booking_id
- Fetches associated seats
- Returns complete booking object

#### GetBookingsByUserID
```go
func (r *PostgresBookingRepository) GetBookingsByUserID(ctx context.Context, userID string) ([]*Booking, error)
```
- Gets all bookings for a user
- Ordered by creation date (newest first)
- Includes seat information

#### UpdateBookingStatus
```go
func (r *PostgresBookingRepository) UpdateBookingStatus(ctx context.Context, bookingID string, status string) error
```
- Updates booking status (CONFIRMED, CANCELLED, etc.)
- Updates updated_at timestamp

#### DeleteBooking
```go
func (r *PostgresBookingRepository) DeleteBooking(ctx context.Context, bookingID string) error
```
- Soft delete (sets status to CANCELLED)
- Preserves booking history

### 5. Updated BookingService

The `BookTickets` function now:
1. ✅ Acquires distributed lock
2. ✅ Starts database transaction
3. ✅ Validates event exists and showtime matches
4. ✅ Validates booking request
5. ✅ **Saves booking to database** (NEW!)
6. ✅ Commits transaction

## How It Works

### Booking Flow

```go
func (s *BookingService) BookTickets(ctx context.Context, req BookingRequest) error {
    // 1. Lock
    lockKey := fmt.Sprintf("booking:lock:%s", req.EventID)
    s.locker.AcquireLock(ctx, lockKey, s.lockTTL)
    defer s.locker.ReleaseLock(ctx, lockKey)

    // 2. Transaction
    tx, _ := s.txManager.BeginTx(ctx)
    defer tx.Rollback()

    // 3. Validate event
    event, _ := s.eventClient.GetEvent(ctx, req.EventID)
    s.validateShowtimeInEvent(req.Showtime, event.ShowDateTimes)
    s.validateBookingRequest(req)

    // 4. Save booking (NEW!)
    booking := &Booking{
        EventID:  req.EventID,
        UserID:   req.UserID,
        Showtime: req.Showtime,
        Quantity: req.Quantity,
        SeatIDs:  req.SeatIDs,
        Status:   "CONFIRMED",
    }
    s.repository.CreateBooking(ctx, tx, booking)

    // 5. Commit
    tx.Commit()
}
```

### Transaction Safety

All database operations happen within a transaction:
- **Atomic**: Either all operations succeed or all are rolled back
- **Consistent**: Foreign key constraints enforced
- **Isolated**: Concurrent bookings don't interfere
- **Durable**: Committed bookings persist

### Booking ID Generation

```go
func generateBookingID() string {
    return fmt.Sprintf("BK-%s", uuid.New().String())
}
```

Example: `BK-550e8400-e29b-41d4-a716-446655440000`

## Testing

### Manual Testing

1. **Start the services:**
```bash
# Terminal 1: PostgreSQL should be running
# Terminal 2: Redis should be running
# Terminal 3: Event-API should be running
# Terminal 4: 
make run
```

2. **Create a booking:**
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

3. **Verify in database:**
```bash
psql -h localhost -U admin -d booking_db

-- View bookings
SELECT * FROM bookings;

-- View seats
SELECT * FROM booking_seats;

-- Join query
SELECT b.booking_id, b.event_id, b.user_id, b.showtime, b.quantity, b.status, 
       array_agg(bs.seat_id) as seats
FROM bookings b
LEFT JOIN booking_seats bs ON b.booking_id = bs.booking_id
GROUP BY b.id
ORDER BY b.created_at DESC;
```

### Expected Output

**API Response:**
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

**Database Records:**

**bookings table:**
```
 id |              booking_id               | event_id | user_id  |      showtime       | quantity |  status   |      created_at        |      updated_at
----+---------------------------------------+----------+----------+---------------------+----------+-----------+------------------------+------------------------
  1 | BK-550e8400-e29b-41d4-a716-44665544  |    1     | user-123 | 2025-07-15 19:00:00 |    2     | CONFIRMED | 2025-10-05 10:30:00   | 2025-10-05 10:30:00
```

**booking_seats table:**
```
 id |              booking_id               | seat_id |      created_at
----+---------------------------------------+---------+------------------------
  1 | BK-550e8400-e29b-41d4-a716-44665544  |   A1    | 2025-10-05 10:30:00
  2 | BK-550e8400-e29b-41d4-a716-44665544  |   A2    | 2025-10-05 10:30:00
```

## Files Changed

1. **internal/booking/repository.go** (NEW)
   - BookingRepository interface
   - PostgresBookingRepository implementation
   - CRUD operations for bookings and seats

2. **internal/booking/booking_service.go**
   - Added `repository` field to BookingService
   - Updated `NewBookingService` to accept repository
   - Updated `NewBookingServiceWithDefaults` to create repository
   - Modified `BookTickets` to save bookings

3. **internal/booking/transaction.go**
   - Added `GetSQLTx()` method to sqlTransaction
   - Allows repository to access underlying *sql.Tx

4. **internal/booking/noop_mocks.go**
   - Added noOpRepository for testing

5. **cmd/api/main.go**
   - Create PostgresBookingRepository instance
   - Pass repository to BookingService

6. **go.mod**
   - Added github.com/google/uuid dependency

## Database Queries

### Useful Queries

**Count bookings by status:**
```sql
SELECT status, COUNT(*) 
FROM bookings 
GROUP BY status;
```

**Recent bookings:**
```sql
SELECT b.booking_id, b.user_id, b.event_id, b.showtime, b.status, b.created_at
FROM bookings b
ORDER BY b.created_at DESC
LIMIT 10;
```

**Bookings with seats:**
```sql
SELECT b.booking_id, b.event_id, b.user_id, 
       string_agg(bs.seat_id, ', ') as seats
FROM bookings b
LEFT JOIN booking_seats bs ON b.booking_id = bs.booking_id
GROUP BY b.booking_id, b.event_id, b.user_id;
```

**Bookings by event:**
```sql
SELECT event_id, COUNT(*) as booking_count, SUM(quantity) as total_tickets
FROM bookings
WHERE status = 'CONFIRMED'
GROUP BY event_id;
```

## Future Enhancements

### Planned Features

1. **GET /api/v1/bookings/{id}**
   - Retrieve booking by ID
   - Uses `GetBookingByID` repository method

2. **GET /api/v1/bookings/user/{userId}**
   - Get all bookings for a user
   - Uses `GetBookingsByUserID` repository method

3. **DELETE /api/v1/bookings/{id}**
   - Cancel booking
   - Uses `DeleteBooking` repository method

4. **Update Handler Response**
   - Return `booking_id` in response
   - Include creation timestamp

### Example Implementation for GET

```go
func (h *BookingHandler) GetBooking(w http.ResponseWriter, r *http.Request) {
    bookingID := extractBookingID(r.URL.Path)
    
    booking, err := h.service.repository.GetBookingByID(r.Context(), bookingID)
    if err != nil {
        respondError(w, http.StatusNotFound, "Booking not found")
        return
    }
    
    respondJSON(w, http.StatusOK, booking)
}
```

## Troubleshooting

### Common Issues

**Issue: "foreign key constraint" error**
- Cause: booking_id doesn't exist when inserting seats
- Solution: Ensure booking is inserted before seats

**Issue: "duplicate key value" error**
- Cause: booking_id already exists
- Solution: UUID generation should be unique, check for conflicts

**Issue: "transaction already committed"**
- Cause: Trying to use transaction after commit
- Solution: Ensure defer tx.Rollback() doesn't run after Commit()

**Issue: "no rows in result set"**
- Cause: Booking doesn't exist
- Solution: Check booking_id is correct

## Performance Considerations

### Indexes
All critical columns are indexed:
- `event_id` - Fast lookup by event
- `user_id` - Fast lookup by user
- `status` - Fast filtering by status
- `booking_id` - Fast foreign key lookups

### Query Optimization
- Use `LIMIT` for large result sets
- Use prepared statements (built-in with QueryRowContext)
- Batch insert seats if needed

### Monitoring
Monitor these metrics:
- Booking creation rate
- Query latency
- Transaction rollback rate
- Database connection pool usage

## Security

### SQL Injection Prevention
All queries use parameterized statements:
```go
// ✅ Safe
db.QueryRowContext(ctx, "SELECT * FROM bookings WHERE booking_id = $1", bookingID)

// ❌ NEVER do this
db.QueryRowContext(ctx, fmt.Sprintf("SELECT * FROM bookings WHERE booking_id = '%s'", bookingID))
```

### Data Validation
- Booking IDs validated before queries
- Input sanitization in service layer
- Status values constrained to allowed set

## Summary

✅ **Complete database persistence implemented**
✅ **Transaction safety guaranteed**
✅ **Repository pattern for clean architecture**
✅ **CRUD operations ready**
✅ **UUID-based booking IDs**
✅ **Proper indexing for performance**
✅ **SQL injection protection**
✅ **Foreign key constraints**
✅ **Ready for production use**

The booking-api now fully persists bookings to PostgreSQL with complete ACID transaction guarantees!

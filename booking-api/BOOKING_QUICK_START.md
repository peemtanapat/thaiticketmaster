# Booking API - Quick Start Guide

## Database Persistence is Now Live! 🎉

The booking-api now **saves all bookings to PostgreSQL** with full ACID transaction support.

## Quick Test

### 1. Start All Services

```bash
# Terminal 1: PostgreSQL
docker run --name booking-postgres \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=admin123 \
  -e POSTGRES_DB=booking_db \
  -p 5432:5432 \
  -d postgres:15

# Terminal 2: Redis
docker run --name booking-redis \
  -p 6379:6379 \
  -d redis:7-alpine

# Terminal 3: Event API (should already be running)
cd ../event-api
./mvnw spring-boot:run

# Terminal 4: Booking API
cd booking-api
make run
```

### 2. Create a Booking

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

**Expected Response:**
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

### 3. Verify in Database

```bash
# Connect to PostgreSQL
docker exec -it booking-postgres psql -U admin -d booking_db

# View your bookings
SELECT * FROM bookings;

# View associated seats
SELECT b.booking_id, b.event_id, b.user_id, b.showtime, b.quantity, b.status,
       array_agg(bs.seat_id) as seats
FROM bookings b
LEFT JOIN booking_seats bs ON b.booking_id = bs.booking_id
GROUP BY b.id
ORDER BY b.created_at DESC;
```

**Expected Output:**
```
              booking_id               | event_id | user_id  |      showtime       | quantity |  status   |       seats
---------------------------------------+----------+----------+---------------------+----------+-----------+------------------
 BK-550e8400-e29b-41d4-a716-44665544  |    1     | user-123 | 2025-07-15 19:00:00 |    2     | CONFIRMED | {A1,A2}
```

## What Changed?

### Before (Validation Only)
```go
func (s *BookingService) BookTickets(ctx context.Context, req BookingRequest) error {
    // Lock
    // Transaction
    // Validate event
    // Validate request
    // Commit
    return nil  // ❌ Nothing saved!
}
```

### After (Full Persistence)
```go
func (s *BookingService) BookTickets(ctx context.Context, req BookingRequest) error {
    // Lock
    // Transaction
    // Validate event
    // Validate request
    
    // ✅ Save to database!
    booking := &Booking{
        EventID:  req.EventID,
        UserID:   req.UserID,
        Showtime: req.Showtime,
        Quantity: req.Quantity,
        SeatIDs:  req.SeatIDs,
        Status:   "CONFIRMED",
    }
    s.repository.CreateBooking(ctx, tx, booking)
    
    // Commit
    return nil
}
```

## Features

### ✅ What's Working

- **Create Booking**: POST /api/v1/bookings
- **Database Persistence**: Bookings saved to PostgreSQL
- **Transaction Safety**: All-or-nothing booking creation
- **Distributed Locking**: Redis-based concurrency control
- **Event Validation**: Checks event exists and showtime matches
- **Seat Tracking**: booking_seats table for seat assignments
- **Unique Booking IDs**: UUID-based identifiers (BK-xxx)
- **Soft Delete**: Status-based cancellation (preserves history)

### 📋 Repository Methods

```go
// Create new booking
CreateBooking(ctx, tx, booking) error

// Get booking by ID
GetBookingByID(ctx, bookingID) (*Booking, error)

// Get all bookings for a user
GetBookingsByUserID(ctx, userID) ([]*Booking, error)

// Update booking status
UpdateBookingStatus(ctx, bookingID, status) error

// Cancel booking (soft delete)
DeleteBooking(ctx, bookingID) error
```

### 🚧 Coming Soon

- GET /api/v1/bookings/{id} - Retrieve booking
- GET /api/v1/bookings/user/{userId} - List user bookings
- DELETE /api/v1/bookings/{id} - Cancel booking
- Response includes booking_id

## Database Schema

### bookings
```sql
CREATE TABLE bookings (
    id SERIAL PRIMARY KEY,
    booking_id VARCHAR(255) UNIQUE NOT NULL,  -- BK-uuid
    event_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    showtime TIMESTAMP NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### booking_seats
```sql
CREATE TABLE booking_seats (
    id SERIAL PRIMARY KEY,
    booking_id VARCHAR(255) NOT NULL,
    seat_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
);
```

## Testing

### Run Tests
```bash
make test
```

**Test Results:** ✅ All passing
```
TestBookTickets_Success                                PASS
TestBookTickets_EventNotFound                         PASS
TestBookTickets_ShowtimeMismatch                      PASS
TestBookingHandler_CreateBooking_Success              PASS
TestBookingHandler_CreateBooking_InvalidJSON          PASS
TestBookingHandler_CreateBooking_MissingEventID       PASS
TestBookingHandler_CreateBooking_InvalidQuantity      PASS
TestBookingHandler_CreateBooking_SeatCountMismatch    PASS
TestBookingHandler_CreateBooking_EventNotFound        PASS
TestBookingHandler_CreateBooking_ShowtimeMismatch     PASS
```

### Manual End-to-End Test

```bash
# 1. Create booking
curl -X POST http://localhost:8081/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "1",
    "userId": "user-456",
    "showtime": "2025-07-15T19:00:00",
    "quantity": 3,
    "seatIds": ["B1", "B2", "B3"]
  }'

# 2. Check database
docker exec -it booking-postgres psql -U admin -d booking_db \
  -c "SELECT booking_id, event_id, user_id, quantity, status FROM bookings ORDER BY created_at DESC LIMIT 5;"

# 3. Check seats
docker exec -it booking-postgres psql -U admin -d booking_db \
  -c "SELECT bs.booking_id, array_agg(bs.seat_id) as seats FROM booking_seats bs GROUP BY bs.booking_id ORDER BY bs.created_at DESC LIMIT 5;"
```

## Useful Commands

### Database Management

```bash
# Connect to database
docker exec -it booking-postgres psql -U admin -d booking_db

# Count bookings
\c booking_db
SELECT COUNT(*) FROM bookings;

# Recent bookings
SELECT * FROM bookings ORDER BY created_at DESC LIMIT 10;

# Bookings by status
SELECT status, COUNT(*) FROM bookings GROUP BY status;

# Exit psql
\q
```

### Service Management

```bash
# Build
make build

# Run normally
make run

# Run with debug logging
make run-debug

# Debug with Delve
make debug

# Run tests
make test

# Clean build artifacts
make clean
```

## Architecture

```
┌─────────────┐
│   Client    │
└─────┬───────┘
      │ HTTP POST
      ▼
┌─────────────────┐
│     Handler     │  (handler.go)
└────────┬────────┘
         │ BookingRequest
         ▼
┌─────────────────┐
│     Service     │  (booking_service.go)
├─────────────────┤
│ 1. Lock (Redis) │
│ 2. Begin TX     │
│ 3. Validate     │
│ 4. Save to DB   │  ← NEW!
│ 5. Commit       │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Repository    │  (repository.go)
├─────────────────┤
│ CreateBooking   │  ← Inserts booking + seats
│ GetBookingByID  │
│ GetBookingsByID │
│ UpdateStatus    │
│ DeleteBooking   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   PostgreSQL    │
├─────────────────┤
│ bookings        │  ← Main booking records
│ booking_seats   │  ← Seat assignments
└─────────────────┘
```

## Transaction Flow

```
1. POST /api/v1/bookings
   ↓
2. Handler validates JSON
   ↓
3. Service acquires Redis lock
   ↓
4. Service starts PostgreSQL transaction
   ↓
5. Service validates event (event-api)
   ↓
6. Service validates showtime
   ↓
7. Repository generates booking_id (BK-uuid)
   ↓
8. Repository inserts to bookings table
   ↓
9. Repository inserts to booking_seats table
   ↓
10. Service commits transaction
    ↓
11. Service releases lock
    ↓
12. Handler returns success response
```

## Environment Variables

```bash
# Database
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_USER=admin
DATABASE_PASSWORD=admin123
DATABASE_NAME=booking_db

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Event API
EVENT_API_BASE_URL=http://localhost:8080

# Server
SERVER_PORT=8081
```

## Troubleshooting

### Issue: "connection refused" on port 5432
**Solution:** Start PostgreSQL
```bash
docker run --name booking-postgres \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=admin123 \
  -e POSTGRES_DB=booking_db \
  -p 5432:5432 \
  -d postgres:15
```

### Issue: "Event not found"
**Solution:** Ensure event-api is running
```bash
cd ../event-api
./mvnw spring-boot:run
```

### Issue: "could not acquire lock"
**Solution:** Start Redis
```bash
docker run --name booking-redis -p 6379:6379 -d redis:7-alpine
```

### Issue: "showtime not found in event"
**Solution:** Use a valid showtime from event-api
```bash
# Get event details
curl http://localhost:8080/api/v1/events/1

# Use one of the showDateTimes in your booking request
```

## Next Steps

1. **Test the API** - Create some bookings and verify in database
2. **Implement GET endpoints** - Retrieve bookings by ID or user
3. **Implement DELETE endpoint** - Cancel bookings
4. **Add integration tests** - Test with real database using testcontainers
5. **Monitor performance** - Check query execution times
6. **Add pagination** - For GetBookingsByUserID

## Documentation

- **DATABASE_IMPLEMENTATION.md** - Detailed implementation guide
- **DEBUG_GUIDE.md** - Debugging with VS Code/GoLand/Delve
- **API_DOCUMENTATION.md** - Full API reference (if available)

## Success Criteria ✅

- [x] Bookings save to PostgreSQL
- [x] Transaction safety (all-or-nothing)
- [x] Unique booking IDs generated
- [x] Seats tracked in separate table
- [x] Foreign key constraints enforced
- [x] Indexes for performance
- [x] All tests passing
- [x] Build successful
- [x] Documentation complete

**The booking-api now has full database persistence! 🎉**

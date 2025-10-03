# Booking API - TDD Implementation Guide

## Overview

This project demonstrates **Test-Driven Development (TDD)** using the **Red-Green-Refactor** technique to build a modern Go microservice for ticket booking.

## TDD Process - Red-Green-Refactor

### 🔴 RED Phase: Write Failing Tests

We started by writing comprehensive tests **before** implementing any code:

**File**: `internal/booking/booking_service_test.go`

```go
func TestBookTickets_Success(t *testing.T) {
    // Test expects BookTickets to:
    // 1. Acquire distributed lock
    // 2. Start DB transaction
    // 3. Call event-api to validate event exists
    // 4. Validate showtime matches
    // 5. Commit transaction
}
```

**Tests Written**:
- ✅ `TestBookTickets_Success` - Happy path
- ✅ `TestBookTickets_EventNotFound` - Handle 404 from event-api
- ✅ `TestBookTickets_ShowtimeMismatch` - Validate showtime validation

**Result**: All tests FAILED initially (as expected) ✅

### 🟢 GREEN Phase: Minimal Implementation

We implemented just enough code to make all tests pass:

**File**: `internal/booking/booking_service.go`

```go
func (s *BookingService) BookTickets(ctx context.Context, req BookingRequest) error {
    // 1. Acquire distributed lock
    lockKey := fmt.Sprintf("booking:lock:%s", req.EventID)
    if err := s.locker.AcquireLock(ctx, lockKey, s.lockTTL); err != nil {
        return fmt.Errorf("failed to acquire lock: %w", err)
    }
    defer s.locker.ReleaseLock(ctx, lockKey)

    // 2. Start DB transaction
    tx, err := s.txManager.BeginTx(ctx)
    if err != nil {
        return fmt.Errorf("failed to start transaction: %w", err)
    }
    defer tx.Rollback()

    // 3. Validate booking rules
    event, err := s.eventClient.GetEvent(ctx, req.EventID)
    if err != nil {
        return fmt.Errorf("failed to get event: %w", err)
    }

    if err := s.validateShowtime(req.Showtime, event.Showtime); err != nil {
        return err
    }

    // 4. Commit transaction
    if err := tx.Commit(); err != nil {
        return fmt.Errorf("failed to commit transaction: %w", err)
    }

    return nil
}
```

**Result**: All tests PASSED ✅

### 🔵 REFACTOR Phase: Improve Code Quality

We refactored the code to follow best practices while keeping tests green:

**Improvements Made**:

1. **Interface Segregation**:
   ```go
   type Locker interface {
       AcquireLock(ctx context.Context, key string, ttl time.Duration) error
       ReleaseLock(ctx context.Context, key string) error
   }

   type EventAPIClient interface {
       GetEvent(ctx context.Context, eventID string) (*Event, error)
   }

   type TransactionManager interface {
       BeginTx(ctx context.Context) (Transaction, error)
   }
   ```

2. **Separate Implementations**:
   - `redis_locker.go` - Redis-based distributed locking
   - `event_client.go` - HTTP client for event-api
   - `transaction.go` - SQL transaction management
   - `noop_mocks.go` - No-op implementations for testing

3. **Better Error Handling**:
   - Wrapped errors with context
   - Proper defer cleanup
   - Transaction rollback safety

**Result**: All tests still PASSED with improved code structure ✅

## Code Coverage

```bash
make test-coverage
```

**Current Coverage**: 59.7%

## Project Structure

```
booking-api/
├── cmd/
│   └── api/
│       └── main.go              # Application entry point
├── internal/
│   └── booking/
│       ├── booking_service.go   # Main service implementation
│       ├── booking_service_test.go  # TDD tests
│       ├── interfaces.go        # Interface definitions
│       ├── event_client.go      # Event API HTTP client
│       ├── redis_locker.go      # Redis distributed locking
│       ├── transaction.go       # SQL transaction management
│       └── noop_mocks.go        # Test mocks
├── Dockerfile                   # Multi-stage Docker build
├── Makefile                     # Build automation
├── go.mod                       # Go module dependencies
└── README.md                    # Documentation
```

## Key Features Implemented

### 1. Distributed Locking
- Uses Redis for distributed locks
- Prevents concurrent bookings for the same event
- Automatic lock expiration (TTL)

### 2. Database Transactions
- ACID compliant transactions
- Automatic rollback on errors
- PostgreSQL support

### 3. Event API Integration
- HTTP client with proper timeouts
- Error handling for 404 and other status codes
- JSON parsing of event data

### 4. Validation Rules
- Event existence check
- Showtime matching validation
- Booking request validation (quantity, seats, etc.)

## Running Tests

```bash
# Run all tests
make test

# Run tests with coverage
make test-coverage

# Run specific test
go test -v -run TestBookTickets_Success ./internal/booking
```

## Building & Running

```bash
# Build the application
make build

# Run locally
make run

# Build Docker image
make docker-build

# Run in Docker
make docker-run
```

## Dependencies

- **Testing**: `testify` for assertions
- **Database**: `lib/pq` for PostgreSQL
- **Cache**: `go-redis/redis/v8` for Redis
- **Future**: `testcontainers-go` for integration tests

## Configuration

Environment variables (see `.env.example`):

- `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME` - Database config
- `REDIS_HOST`, `REDIS_PORT` - Redis config
- `EVENT_API_URL` - Event API service URL
- `SERVER_PORT` - HTTP server port

## TDD Benefits Demonstrated

1. ✅ **Clear Requirements**: Tests define expected behavior upfront
2. ✅ **Faster Feedback**: Immediate validation of changes
3. ✅ **Better Design**: Interfaces emerged naturally from testing needs
4. ✅ **Refactoring Safety**: Tests ensure behavior remains correct
5. ✅ **Documentation**: Tests serve as usage examples

## Next Steps

To complete the TDD cycle with real infrastructure:

1. Implement integration tests with testcontainers:
   - PostgreSQL container for transaction tests
   - Redis container for distributed lock tests
   
2. Add more booking scenarios:
   - Concurrent booking attempts
   - Seat availability checks
   - Booking rollback on errors

3. Add HTTP endpoints:
   - POST `/api/v1/bookings` - Create booking
   - GET `/api/v1/bookings/{id}` - Get booking details

## Conclusion

This project successfully demonstrates the **Red-Green-Refactor TDD technique** in Go:

- 🔴 **RED**: Wrote failing tests first
- 🟢 **GREEN**: Implemented minimal code to pass
- 🔵 **REFACTOR**: Improved design while keeping tests green

The result is a well-tested, maintainable microservice with clean architecture and proper separation of concerns.

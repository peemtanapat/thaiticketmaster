# Booking API - REST Endpoints Documentation

## Overview

The Booking API provides RESTful endpoints for managing ticket bookings with distributed locking and transaction support.

**Base URL**: `http://localhost:8081`

---

## Authentication

🚧 **Not Yet Implemented** - All endpoints are currently open for testing purposes.

---

## Endpoints

### 1. Health Check

Check if the service is running and healthy.

**Endpoint**: `GET /health`

**Response**: `200 OK`

```json
{
  "status": "healthy"
}
```

**Example**:
```bash
curl http://localhost:8081/health
```

---

### 2. Create Booking

Create a new ticket booking with distributed locking and transaction management.

**Endpoint**: `POST /api/v1/bookings`

**Request Headers**:
- `Content-Type: application/json`

**Request Body**:

```json
{
  "eventId": "string",      // Required: Event ID from event-api
  "userId": "string",       // Required: User ID making the booking
  "showtime": "datetime",   // Required: ISO 8601 format (e.g., "2025-10-10T19:00:00Z")
  "quantity": integer,      // Required: Number of tickets (must be > 0)
  "seatIds": ["string"]     // Required: Array of seat IDs (length must match quantity)
}
```

**Success Response**: `201 Created`

```json
{
  "success": true,
  "message": "Booking created successfully",
  "data": {
    "eventId": "event-123",
    "userId": "user-456",
    "showtime": "2025-10-10T19:00:00Z",
    "quantity": 2,
    "seatIds": ["A1", "A2"]
  }
}
```

**Error Responses**:

| Status Code | Description | Example Error |
|-------------|-------------|---------------|
| `400 Bad Request` | Invalid request body or validation failure | `"eventId is required"` |
| `404 Not Found` | Event not found in event-api | `"event not found"` |
| `409 Conflict` | Failed to acquire distributed lock | `"lock already held by another process"` |
| `500 Internal Server Error` | Server error | `"failed to start transaction"` |

**Example - Success**:
```bash
curl -X POST http://localhost:8081/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "1",
    "userId": "user-123",
    "showtime": "2025-07-15T19:00:00Z",
    "quantity": 2,
    "seatIds": ["A1", "A2"]
  }'
```

**Example - Event Not Found**:
```bash
curl -X POST http://localhost:8081/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "non-existent",
    "userId": "user-123",
    "showtime": "2025-07-15T19:00:00Z",
    "quantity": 2,
    "seatIds": ["A1", "A2"]
  }'
```

Response:
```json
{
  "success": false,
  "error": "failed to get event: event not found"
}
```

**Example - Showtime Mismatch**:
```bash
curl -X POST http://localhost:8081/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "1",
    "userId": "user-123",
    "showtime": "2025-07-16T19:00:00Z",
    "quantity": 2,
    "seatIds": ["A1", "A2"]
  }'
```

Response:
```json
{
  "success": false,
  "error": "showtime mismatch: booking=2025-07-16T19:00:00Z, event=2025-07-15T19:00:00Z"
}
```

**Validation Rules**:

1. ✅ `eventId` must not be empty
2. ✅ `userId` must not be empty
3. ✅ `quantity` must be positive (> 0)
4. ✅ `seatIds` must not be empty
5. ✅ Number of `seatIds` must match `quantity`
6. ✅ `showtime` must not be zero/empty
7. ✅ Event must exist in event-api
8. ✅ Booking `showtime` must match event's showtime

---

### 3. Get Booking (Future)

Retrieve details of a specific booking.

**Endpoint**: `GET /api/v1/bookings/{id}`

**Status**: `501 Not Implemented`

**Planned Response**: `200 OK`

```json
{
  "success": true,
  "data": {
    "bookingId": "booking-123",
    "eventId": "event-123",
    "userId": "user-456",
    "showtime": "2025-10-10T19:00:00Z",
    "quantity": 2,
    "seatIds": ["A1", "A2"],
    "status": "CONFIRMED",
    "createdAt": "2025-10-03T10:30:00Z"
  }
}
```

---

### 4. Get User Bookings (Future)

Retrieve all bookings for a specific user.

**Endpoint**: `GET /api/v1/bookings/user/{userId}`

**Status**: `501 Not Implemented`

**Planned Response**: `200 OK`

```json
{
  "success": true,
  "data": [
    {
      "bookingId": "booking-123",
      "eventId": "event-123",
      "showtime": "2025-10-10T19:00:00Z",
      "quantity": 2,
      "status": "CONFIRMED"
    }
  ]
}
```

---

### 5. Cancel Booking (Future)

Cancel an existing booking.

**Endpoint**: `DELETE /api/v1/bookings/{id}`

**Status**: `501 Not Implemented`

**Planned Response**: `200 OK`

```json
{
  "success": true,
  "message": "Booking cancelled successfully"
}
```

---

## Implementation Details

### Booking Flow (TDD Implementation)

When creating a booking, the following steps are executed:

1. **Acquire Distributed Lock** (Redis)
   - Prevents concurrent bookings for the same event
   - Lock key: `booking:lock:{eventId}`
   - TTL: 30 seconds

2. **Start Database Transaction** (PostgreSQL)
   - ACID compliant
   - Auto-rollback on errors

3. **Validate Event**
   - HTTP call to event-api: `GET /api/v1/events/{eventId}`
   - Verify event exists (200 OK)
   - Extract event showtime

4. **Validate Showtime**
   - Compare booking showtime with event showtime
   - Must match exactly

5. **Validate Booking Request**
   - Check all required fields
   - Verify quantity and seat count match

6. **Commit Transaction**
   - Save booking to database
   - Release distributed lock

7. **Return Response**
   - Success: 201 Created
   - Failure: Appropriate error code

---

## Error Handling

All errors return a consistent format:

```json
{
  "success": false,
  "error": "Error message describing what went wrong"
}
```

### Common Error Scenarios

| Error | Status | Description |
|-------|--------|-------------|
| Invalid JSON | 400 | Request body is not valid JSON |
| Missing required field | 400 | Required field is empty |
| Validation failure | 400 | Data doesn't meet validation rules |
| Event not found | 404 | Event doesn't exist in event-api |
| Lock conflict | 409 | Another booking is in progress |
| Server error | 500 | Internal server error |

---

## Testing with curl

### Successful Booking
```bash
curl -v -X POST http://localhost:8081/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "1",
    "userId": "user-123",
    "showtime": "2025-07-15T19:00:00Z",
    "quantity": 2,
    "seatIds": ["A1", "A2"]
  }'
```

### Invalid Quantity
```bash
curl -v -X POST http://localhost:8081/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "1",
    "userId": "user-123",
    "showtime": "2025-07-15T19:00:00Z",
    "quantity": 0,
    "seatIds": []
  }'
```

### Seat Count Mismatch
```bash
curl -v -X POST http://localhost:8081/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "1",
    "userId": "user-123",
    "showtime": "2025-07-15T19:00:00Z",
    "quantity": 2,
    "seatIds": ["A1", "A2", "A3"]
  }'
```

---

## Testing with HTTP Client (VS Code)

Use the provided `client.http` file with the REST Client extension:

```http
### Create booking
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

---

## Integration with Event API

The booking service integrates with the Event API to validate events:

**Event API Base URL**: `http://localhost:8080`

**Endpoint Used**: `GET /api/v1/events/{eventId}`

**Expected Response** (200 OK):
```json
{
  "id": "1",
  "name": "Summer Music Festival 2025",
  "showtime": "2025-07-15T19:00:00",
  "location": "Central Stadium, Bangkok",
  "ticketPrice": 1500.00,
  "eventStatus": "ON_SALE"
}
```

---

## Prerequisites

Before using the API, ensure these services are running:

1. **PostgreSQL** - `localhost:5432`
   - Database: `booking_db`
   - User: `postgres`
   - Password: `postgres`

2. **Redis** - `localhost:6379`
   - Used for distributed locking

3. **Event API** - `localhost:8080`
   - Provides event information

---

## Configuration

Environment variables:

```bash
DB_HOST=localhost
DB_PORT=5432
DB_USER=postgres
DB_PASSWORD=postgres
DB_NAME=booking_db

REDIS_HOST=localhost
REDIS_PORT=6379

EVENT_API_URL=http://localhost:8080
SERVER_PORT=8081
```

---

## Test Coverage

Current test coverage: **68.4%**

**Tests Implemented**:
- ✅ Service layer (booking_service_test.go) - 5 tests
- ✅ Handler layer (handler_test.go) - 10 tests
- ✅ Happy path scenarios
- ✅ Error scenarios
- ✅ Validation scenarios

Run tests:
```bash
make test              # Run all tests
make test-coverage     # Generate coverage report
```

---

## Performance Considerations

### Distributed Locking
- **Lock TTL**: 30 seconds (configurable)
- **Lock Scope**: Per event
- **Implementation**: Redis SETNX

### Database Transactions
- **Isolation Level**: Default (Read Committed)
- **Timeout**: Inherits from context
- **Auto-rollback**: Yes, on errors

### HTTP Timeouts
- **Event API Client**: 10 seconds
- **Server Read**: 15 seconds
- **Server Write**: 15 seconds
- **Server Idle**: 60 seconds

---

## Future Enhancements

- [ ] Implement GET /api/v1/bookings/{id}
- [ ] Implement GET /api/v1/bookings/user/{userId}
- [ ] Implement DELETE /api/v1/bookings/{id}
- [ ] Add authentication/authorization
- [ ] Add rate limiting
- [ ] Add request ID tracking
- [ ] Add structured logging
- [ ] Add Prometheus metrics
- [ ] Add OpenAPI/Swagger documentation

---

## Support

For issues or questions, refer to:
- **Quick Start**: `QUICK_START.md`
- **TDD Guide**: `TDD_GUIDE.md`
- **Project Summary**: `TDD_COMPLETION_SUMMARY.md`

---

**Built with TDD principles - Test coverage: 68.4%** ✅

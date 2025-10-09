# 🧪 Testing Guide - Seat Validation & Rollback

## Quick Test Commands

### Prerequisites
```bash
# Make sure database is running
docker-compose up -d postgres

# Run migrations
cd booking-api
make db-migrate
make db-migrate-timestamps
```

---

## Test 1: Valid Seats Booking (Should Succeed ✅)

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "event_id": "event_1",
    "showtime": "2025-12-25T19:00:00+00:00",
    "seat_ids": ["A1", "A2"],
    "user_id": "user_123",
    "quantity": 2
  }'
```

**Expected Result**:
```json
{
  "booking_id": "BK-...",
  "status": "CONFIRMED",
  "seat_ids": ["A1", "A2"]
}
```

**Status Code**: `201 Created` ✅

---

## Test 2: Non-existent Seats (Should Fail & Rollback ❌)

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "event_id": "event_1",
    "showtime": "2025-12-25T19:00:00+00:00",
    "seat_ids": ["Z999", "XYZ"],
    "user_id": "user_123",
    "quantity": 2
  }'
```

**Expected Result**:
```json
{
  "error": "seats do not exist for this event/showtime: [Z999, XYZ]"
}
```

**Status Code**: `400 Bad Request` ❌

**Database Check** (should be empty):
```sql
-- No booking should be created
SELECT * FROM bookings WHERE user_id = 'user_123' ORDER BY created_at DESC LIMIT 1;
-- Expected: 0 rows (transaction rolled back)

-- No booking_seats should be created
SELECT * FROM booking_seats WHERE seat_id IN ('Z999', 'XYZ');
-- Expected: 0 rows (transaction rolled back)
```

---

## Test 3: Mixed Valid/Invalid Seats (Should Fail & Rollback ❌)

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "event_id": "event_1",
    "showtime": "2025-12-25T19:00:00+00:00",
    "seat_ids": ["A1", "Z999"],
    "user_id": "user_123",
    "quantity": 2
  }'
```

**Expected Result**:
```json
{
  "error": "seats do not exist for this event/showtime: [Z999]"
}
```

**Status Code**: `400 Bad Request` ❌

**Database Check**:
```sql
-- A1 should NOT be booked (entire transaction rolled back)
SELECT status FROM event_seats 
WHERE event_id = 'event_1' 
AND showtime = '2025-12-25 19:00:00+00' 
AND seat_id = 'A1';
-- Expected: status = 'AVAILABLE' (not RESERVED or SOLD)
```

---

## Test 4: Wrong Event ID (Should Fail ❌)

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "event_id": "nonexistent_event",
    "showtime": "2025-12-25T19:00:00+00:00",
    "seat_ids": ["A1", "A2"],
    "user_id": "user_123",
    "quantity": 2
  }'
```

**Expected Result**:
```json
{
  "error": "seats do not exist for this event/showtime: [A1, A2]"
}
```

**Status Code**: `400 Bad Request` ❌

---

## Test 5: Wrong Showtime (Should Fail ❌)

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "event_id": "event_1",
    "showtime": "2099-12-31T23:59:59+00:00",
    "seat_ids": ["A1", "A2"],
    "user_id": "user_123",
    "quantity": 2
  }'
```

**Expected Result**:
```json
{
  "error": "seats do not exist for this event/showtime: [A1, A2]"
}
```

**Status Code**: `400 Bad Request` ❌

---

## Test 6: Already Booked Seats (Should Fail ❌)

```bash
# First booking (should succeed)
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "event_id": "event_1",
    "showtime": "2025-12-25T19:00:00+00:00",
    "seat_ids": ["B1"],
    "user_id": "user_123",
    "quantity": 1
  }'

# Second booking (should fail - seat already taken)
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "event_id": "event_1",
    "showtime": "2025-12-25T19:00:00+00:00",
    "seat_ids": ["B1"],
    "user_id": "user_456",
    "quantity": 1
  }'
```

**Expected Result** (second request):
```json
{
  "error": "seats are already booked: [B1]"
}
```

**Status Code**: `400 Bad Request` ❌

---

## Performance Test

### Benchmark Seat Validation Query

```sql
-- Enable timing
\timing on

-- Test validation query
EXPLAIN ANALYZE
SELECT seat_id
FROM event_seats
WHERE event_id = 'event_1'
  AND showtime = '2025-12-25 19:00:00+00'
  AND seat_id = ANY(ARRAY['A1', 'A2', 'A3', 'A4', 'A5']);
```

**Expected**:
- Execution time: < 5ms
- Uses index: `idx_event_seats_event_showtime`
- Plan: Index Scan (not Seq Scan)

---

## Database Verification Queries

### Check Available Seats
```sql
SELECT seat_id, status, booking_id
FROM event_seats
WHERE event_id = 'event_1'
  AND showtime = '2025-12-25 19:00:00+00'
  AND status = 'AVAILABLE'
ORDER BY seat_id;
```

### Check Booked Seats
```sql
SELECT seat_id, status, booking_id
FROM event_seats
WHERE event_id = 'event_1'
  AND showtime = '2025-12-25 19:00:00+00'
  AND status IN ('RESERVED', 'SOLD')
ORDER BY seat_id;
```

### Check Recent Bookings
```sql
SELECT 
    b.booking_id,
    b.event_id,
    b.showtime,
    b.status,
    ARRAY_AGG(bs.seat_id ORDER BY bs.seat_id) as seats,
    b.created_at
FROM bookings b
LEFT JOIN booking_seats bs ON b.booking_id = bs.booking_id
WHERE b.created_at > NOW() - INTERVAL '1 hour'
GROUP BY b.booking_id, b.event_id, b.showtime, b.status, b.created_at
ORDER BY b.created_at DESC;
```

### Verify No Orphaned Records (After Failed Transaction)
```sql
-- Should return 0 for failed bookings
SELECT COUNT(*) FROM bookings 
WHERE booking_id NOT IN (SELECT DISTINCT booking_id FROM booking_seats);
```

---

## Load Test

### Test Concurrent Bookings

```bash
# Install hey (HTTP load testing tool)
go install github.com/rakyll/hey@latest

# Run load test
hey -n 100 -c 10 -m POST \
  -H "Content-Type: application/json" \
  -d '{
    "event_id": "event_1",
    "showtime": "2025-12-25T19:00:00+00:00",
    "seat_ids": ["C1"],
    "user_id": "user_loadtest",
    "quantity": 1
  }' \
  http://localhost:8080/api/bookings
```

**Expected**:
- Only 1 request succeeds (first one)
- 99 requests fail (seat already booked)
- No race conditions
- No duplicate bookings

---

## Rollback Verification

### Manual Transaction Test

```sql
-- Start transaction
BEGIN;

-- Try to book non-existent seat
INSERT INTO bookings (booking_id, event_id, user_id, showtime, quantity, status, created_at, updated_at)
VALUES ('BK-TEST-001', 'event_1', 'user_test', '2025-12-25 19:00:00+00', 1, 'CONFIRMED', NOW(), NOW());

-- Check if booking exists (should see 1 row)
SELECT * FROM bookings WHERE booking_id = 'BK-TEST-001';

-- Rollback
ROLLBACK;

-- Check again (should see 0 rows)
SELECT * FROM bookings WHERE booking_id = 'BK-TEST-001';
```

---

## Test Checklist

- [ ] Test 1: Valid seats booking succeeds
- [ ] Test 2: Non-existent seats fail with proper error
- [ ] Test 3: Mixed valid/invalid seats fail completely (no partial booking)
- [ ] Test 4: Wrong event ID fails
- [ ] Test 5: Wrong showtime fails
- [ ] Test 6: Already booked seats fail
- [ ] Performance test shows < 5ms query time
- [ ] No orphaned records after failed transactions
- [ ] Concurrent bookings handled correctly
- [ ] Transaction rollback works properly

---

## Monitoring Commands

### Watch Recent Errors
```bash
# In booking-api logs
tail -f logs/app.log | grep "seats do not exist"
```

### Monitor Transaction Rollbacks
```sql
-- PostgreSQL stats
SELECT 
    datname,
    xact_commit,
    xact_rollback,
    ROUND(100.0 * xact_rollback / NULLIF(xact_commit + xact_rollback, 0), 2) as rollback_ratio
FROM pg_stat_database
WHERE datname = 'booking_db';
```

### Check Index Usage
```sql
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE tablename = 'event_seats'
ORDER BY idx_scan DESC;
```

---

## Success Criteria

✅ Valid bookings complete successfully
✅ Invalid bookings fail immediately
✅ No partial bookings (all-or-nothing)
✅ Clear error messages
✅ Query performance < 5ms
✅ No database inconsistencies
✅ Transaction rollback works correctly
✅ Concurrent requests handled safely

---

## Troubleshooting

### Issue: All Bookings Failing
```sql
-- Check if event_seats table has data
SELECT COUNT(*) FROM event_seats WHERE event_id = 'event_1';
-- If 0: Run migration 001 to populate seats
```

### Issue: Validation Not Working
```sql
-- Check if event_seats table exists
SELECT EXISTS (
    SELECT FROM information_schema.tables 
    WHERE table_name = 'event_seats'
);
-- If false: Run migration 001
```

### Issue: Slow Performance
```sql
-- Check if indexes exist
SELECT indexname FROM pg_indexes WHERE tablename = 'event_seats';
-- Should see: idx_event_seats_event_showtime
```

---

Run these tests to verify the seat validation and rollback functionality! 🧪

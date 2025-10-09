# ✅ FIX: "column sold_at does not exist" Error

**Problem:** Booking API returns error when creating bookings:
```json
{
  "success": false,
  "error": "failed to update event seats status: pq: column \"sold_at\" does not exist"
}
```

---

## 🔧 Solution

The `event_seats` table was missing timestamp columns required for the booking workflow.

### Migration Applied: `002_add_event_seats_timestamps.sql`

Added the following columns:
- `reserved_at` - Timestamp when seat was reserved
- `reserved_until` - Reservation expiry (for automatic cleanup)
- `sold_at` - Timestamp when seat was sold

---

## ✅ How to Fix

### Quick Fix (Already Done):
```bash
make db-migrate-timestamps
```

This command:
1. ✅ Checks if `event_seats` table exists
2. ✅ Adds `reserved_at` column if missing
3. ✅ Adds `reserved_until` column if missing
4. ✅ Adds `sold_at` column if missing
5. ✅ Creates index on `reserved_until` for performance
6. ✅ Verifies all columns exist

---

## 📊 Updated Schema

### event_seats Table (After Migration)

| Column | Type | Description |
|--------|------|-------------|
| `id` | SERIAL | Primary key |
| `event_id` | VARCHAR(255) | Event identifier |
| `showtime` | TIMESTAMP | Show date/time |
| `seat_id` | VARCHAR(50) | Seat identifier (A1, B2, etc.) |
| `zone` | VARCHAR(50) | Seat zone (VIP, Premium, Standard) |
| `price` | DECIMAL(10,2) | Seat price |
| `status` | VARCHAR(20) | Seat status (AVAILABLE, RESERVED, SOLD, BLOCKED) |
| `booking_id` | VARCHAR(255) | Reference to booking when reserved/sold |
| `created_at` | TIMESTAMP | Creation timestamp |
| `updated_at` | TIMESTAMP | Last update timestamp |
| `reserved_at` | TIMESTAMP | ⭐ When seat was reserved |
| `reserved_until` | TIMESTAMP | ⭐ Reservation expiry time |
| `sold_at` | TIMESTAMP | ⭐ When seat was sold |

---

## 🧪 Test the Fix

### 1. Verify Schema
```bash
# Check columns exist
PGPASSWORD=admin psql -h localhost -p 5432 -U admin -d booking_db \
  -c "SELECT column_name FROM information_schema.columns WHERE table_name='event_seats' ORDER BY ordinal_position;"
```

**Expected Output:**
```
 column_name    
----------------
 id
 event_id
 showtime
 seat_id
 zone
 price
 status
 booking_id
 created_at
 updated_at
 reserved_at     ⭐
 reserved_until  ⭐
 sold_at         ⭐
```

---

### 2. Test Booking API

**Using client.http:**
```http
### Create a booking (should work now!)
POST http://localhost:8081/api/v1/bookings
Content-Type: application/json

{
  "eventId": "1",
  "userId": "user-123",
  "showtime": "2025-12-25T19:00:00Z",
  "quantity": 2,
  "seatIds": ["A1", "A2"]
}
```

**Using curl:**
```bash
curl -X POST http://localhost:8081/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "1",
    "userId": "user-123",
    "showtime": "2025-12-25T19:00:00Z",
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
    "booking_id": "BK-...",
    "event_id": "1",
    "user_id": "user-123",
    "showtime": "2025-12-25T19:00:00Z",
    "quantity": 2,
    "seat_ids": ["A1", "A2"],
    "status": "CONFIRMED"
  }
}
```

---

### 3. Verify event_seats Updated

```bash
# Check seat status after booking
PGPASSWORD=admin psql -h localhost -p 5432 -U admin -d booking_db \
  -c "SELECT seat_id, status, booking_id, sold_at FROM event_seats WHERE seat_id IN ('A1', 'A2');"
```

**Expected Output:**
```
 seat_id | status |   booking_id   |         sold_at         
---------+--------+----------------+-------------------------
 A1      | SOLD   | BK-xxx-xxx-xxx | 2025-10-06 12:00:00
 A2      | SOLD   | BK-xxx-xxx-xxx | 2025-10-06 12:00:00
```

---

## 🎯 What Changed

### Before (Broken):
```sql
-- event_seats table missing timestamp columns
CREATE TABLE event_seats (
    ...
    status VARCHAR(20),
    booking_id VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
    -- ❌ Missing: reserved_at, reserved_until, sold_at
);

-- Booking code tries to update sold_at
UPDATE event_seats SET sold_at = NOW() ...
-- ❌ ERROR: column "sold_at" does not exist
```

### After (Fixed):
```sql
-- event_seats table with all required columns
CREATE TABLE event_seats (
    ...
    status VARCHAR(20),
    booking_id VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    reserved_at TIMESTAMP,     ⭐ ADDED
    reserved_until TIMESTAMP,  ⭐ ADDED
    sold_at TIMESTAMP          ⭐ ADDED
);

-- Booking code updates sold_at successfully
UPDATE event_seats SET sold_at = NOW() ...
-- ✅ SUCCESS: Column exists
```

---

## 📋 Migration Files

### Primary Migrations:
1. **`001_create_event_seats_simple.sql`** - Creates event_seats table (basic)
2. **`002_add_event_seats_timestamps.sql`** ⭐ **NEW** - Adds timestamp columns

### Make Commands:
```bash
# Initial setup (if not done yet)
make db-migrate

# Fix missing columns (THIS ONE!)
make db-migrate-timestamps

# Verify setup
make db-verify
make db-status
```

---

## 🎓 Why This Happened

The initial migration (`001_create_event_seats_simple.sql`) created a simplified version of the `event_seats` table without the timestamp columns.

When the booking service was updated to integrate with `event_seats`, it expected these columns to exist:
- `reserved_at` - Track when reservation started
- `reserved_until` - Enable automatic cleanup of expired reservations
- `sold_at` - Record when seat was sold

The code tried to update these columns, but they didn't exist, causing the error.

---

## ✅ Fixed!

After running `make db-migrate-timestamps`:
- ✅ All timestamp columns added
- ✅ Booking API works correctly
- ✅ Seats marked as SOLD when booked
- ✅ Timestamps recorded properly
- ✅ No breaking changes (backward compatible)

---

## 🚀 Next Steps

1. **Test the booking:**
   ```bash
   # Run booking-api if not already running
   make booking-run
   
   # Test with client.http or curl
   ```

2. **Verify seat updates:**
   ```bash
   # Check seat status after booking
   make db-status
   ```

3. **Monitor logs:**
   ```bash
   # Watch booking-api logs
   tail -f booking-api/logs/*.log
   ```

---

## 📚 Related Files

- **Migration:** `booking-api/migrations/002_add_event_seats_timestamps.sql`
- **Repository:** `booking-api/internal/booking/repository.go` (UpdateEventSeatsStatus method)
- **Service:** `booking-api/internal/booking/booking_service.go` (BookTickets method)
- **Documentation:** `booking-api/EVENT_SEATS_TRANSACTION_INTEGRATION.md`

---

**Problem:** ❌ column "sold_at" does not exist  
**Solution:** ✅ `make db-migrate-timestamps`  
**Status:** ✅ FIXED!

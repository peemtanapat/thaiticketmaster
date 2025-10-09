# Database Migrations - Complete Schema with TIMESTAMPTZ

## Overview

This directory contains SQL migration scripts for the Thai Ticket Master booking system with **full timezone support (TIMESTAMPTZ)** for all timestamp columns.

## 📌 Current Migration Structure (v2.0 - Consolidated)

### ✨ NEW: `001_create_complete_schema.sql` (RECOMMENDED)
**Version:** v2.0 (Consolidated from old 001, 002, 003)  
**Status:** ✅ Production-Ready

**Features:**
- ✅ Creates all 3 tables: `bookings`, `booking_seats`, `event_seats`
- ✅ **ALL timestamp columns use TIMESTAMPTZ** (timezone-aware)
- ✅ Automatic seat generation using PostgreSQL functions
- ✅ Pre-populates 40 seats for 2 events
- ✅ Performance-optimized indexes (including composite indexes)
- ✅ Helper functions for seat management
- ✅ Idempotent - safe to run multiple times
- ✅ Built-in verification and status reporting

**Timestamp Columns (ALL with timezone):**
- `bookings`: showtime, created_at, updated_at
- `booking_seats`: created_at
- `event_seats`: showtime, reserved_at, reserved_until, sold_at, created_at, updated_at

**Use this when:** You want a clean, single-migration deployment with the latest schema.

---

## 📦 Archived Migrations (Reference Only)

The following files have been archived and are **NOT required** for new deployments:

### `001_create_event_seats.sql.old`
- Original version with partial TIMESTAMPTZ support
- Created `event_seats` table only
- **Issue:** Mixed TIMESTAMP and TIMESTAMPTZ columns

### `002_add_event_seats_timestamps.sql.old`
- Added reserved_at, reserved_until, sold_at as TIMESTAMPTZ
- Incremental migration approach
- **Superseded by:** Consolidated 001

### `003_alter_showtime_timestamptz.sql.old`
- Converted existing TIMESTAMP to TIMESTAMPTZ
- **Superseded by:** Consolidated 001

### `001_create_event_seats_simple.sql`
- Simplified version for testing
- **Note:** Does not include booking tables

---

## Pre-populated Data

### Event 1 - Christmas Concert
- **Event ID:** `1`
- **Showtime:** `2025-12-25 19:00:00`
- **Total Seats:** 20

| Zone | Seats | Price | Description |
|------|-------|-------|-------------|
| VIP | A1-A5 | ฿2,000 | Front row seats |
| Premium | B1-B5 | ฿1,500 | Second row |
| Standard | C1-C5, D1-D5 | ฿1,000 | Back rows |

---

### Event 2 - New Year Show
- **Event ID:** `2`
- **Showtime:** `2025-12-31 20:00:00`
- **Total Seats:** 20

| Zone | Seats | Price | Description |
|------|-------|-------|-------------|
| VIP | A1-A5 | ฿2,500 | Front row seats |
| Premium | B1-B5 | ฿1,800 | Second row |
| Standard | C1-C5, D1-D5 | ฿1,200 | Back rows |

---

## 🚀 How to Run

### ⚡ Recommended: Run Consolidated Migration

```bash
# Using psql (Command Line)
psql -h localhost -p 5432 -U postgres -d booking_db -f migrations/001_create_complete_schema.sql

# Using Docker
docker cp migrations/001_create_complete_schema.sql postgresql-container:/tmp/
docker exec -it postgresql-container psql -U postgres -d booking_db -f /tmp/001_create_complete_schema.sql
```

### 🔄 For Fresh Database Setup

```bash
# 1. Drop existing database (CAUTION: This deletes all data!)
psql -h localhost -p 5432 -U postgres -d postgres -c "DROP DATABASE IF EXISTS booking_db;"

# 2. Create new database
psql -h localhost -p 5432 -U postgres -d postgres -c "CREATE DATABASE booking_db;"

# 3. Run consolidated migration
psql -h localhost -p 5432 -U postgres -d booking_db -f migrations/001_create_complete_schema.sql
```

### 🛠️ Using Database Clients

**DBeaver / pgAdmin / TablePlus:**
1. Connect to your `booking_api` database
2. Open `001_create_complete_schema.sql`
3. Execute the SQL script

---

## 📊 Table Schemas

### 1. `bookings` Table

```sql
CREATE TABLE bookings (
    id SERIAL PRIMARY KEY,
    booking_id VARCHAR(255) UNIQUE NOT NULL,
    event_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    showtime TIMESTAMPTZ NOT NULL,          -- ⏰ With timezone
    quantity INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,  -- ⏰ With timezone
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP   -- ⏰ With timezone
);
```

### 2. `booking_seats` Table

```sql
CREATE TABLE booking_seats (
    id SERIAL PRIMARY KEY,
    booking_id VARCHAR(255) NOT NULL,
    seat_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,  -- ⏰ With timezone
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
);
```

### 3. `event_seats` Table

```sql
CREATE TABLE event_seats (
    id SERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    showtime TIMESTAMPTZ NOT NULL,          -- ⏰ With timezone
    seat_id VARCHAR(50) NOT NULL,
    zone VARCHAR(50) NOT NULL DEFAULT 'Standard',
    row_number VARCHAR(10),
    seat_number INTEGER,
    price DECIMAL(10, 2) NOT NULL DEFAULT 1000.00,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    booking_id VARCHAR(255),
    
    -- All timestamp columns with timezone support
    reserved_at TIMESTAMPTZ,                -- ⏰ With timezone
    reserved_until TIMESTAMPTZ,             -- ⏰ With timezone
    sold_at TIMESTAMPTZ,                    -- ⏰ With timezone
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,  -- ⏰ With timezone
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,  -- ⏰ With timezone
    
    CONSTRAINT unique_seat_per_showtime UNIQUE (event_id, showtime, seat_id),
    CONSTRAINT valid_status CHECK (status IN ('AVAILABLE', 'RESERVED', 'SOLD', 'BLOCKED')),
    CONSTRAINT valid_zone CHECK (zone IN ('VIP', 'Premium', 'Standard', 'Balcony', 'Standing'))
);
```

### Status Values

| Status | Description | Workflow |
|--------|-------------|----------|
| `AVAILABLE` | Seat is available for booking | Initial state |
| `RESERVED` | Temporarily held (5-15 min) | During checkout |
| `SOLD` | Successfully booked | After payment |
| `BLOCKED` | Unavailable (maintenance, etc.) | Admin action |

---

## Verification Queries

### Check if seats were created

```sql
SELECT 
    event_id,
    TO_CHAR(showtime, 'YYYY-MM-DD HH24:MI') as showtime,
    COUNT(*) as total_seats
FROM event_seats
WHERE event_id IN ('1', '2')
GROUP BY event_id, showtime
ORDER BY event_id;
```

**Expected Output:**
```
 event_id |     showtime     | total_seats 
----------+------------------+-------------
 1        | 2025-12-25 19:00 |          20
 2        | 2025-12-31 20:00 |          20
```

---

### View all seats for Event 1

```sql
SELECT 
    seat_id,
    zone,
    price,
    status
FROM event_seats
WHERE event_id = '1'
ORDER BY seat_id;
```

---

### Check seat distribution by zone

```sql
SELECT 
    event_id,
    zone,
    COUNT(*) as seat_count,
    SUM(price) as total_value
FROM event_seats
WHERE event_id IN ('1', '2')
GROUP BY event_id, zone
ORDER BY event_id, zone;
```

---

### View available seats only

```sql
SELECT 
    event_id,
    seat_id,
    zone,
    price
FROM event_seats
WHERE event_id = '1' 
AND status = 'AVAILABLE'
ORDER BY seat_id;
```

---

## Next Steps

After running the migration, you'll need to update your Go code:

### 1. Update Repository Methods

Add methods to check seat availability against `event_seats` table:

```go
// Check if seat exists and is available
func (r *PostgresRepository) CheckSeatAvailable(ctx context.Context, eventID string, showtime time.Time, seatID string) (bool, error)

// Reserve seats (mark as RESERVED)
func (r *PostgresRepository) ReserveSeats(ctx context.Context, bookingID string, seats []string) error

// Mark seats as SOLD
func (r *PostgresRepository) SellSeats(ctx context.Context, bookingID string) error

// Release reserved seats back to AVAILABLE
func (r *PostgresRepository) ReleaseSeats(ctx context.Context, bookingID string) error
```

### 2. Update Booking Logic

Before creating a booking:
1. Check if seats exist in `event_seats`
2. Check if seats are `AVAILABLE`
3. Mark seats as `RESERVED` (temporary hold)
4. Create booking
5. After payment: Mark as `SOLD`
6. If payment fails: Mark back as `AVAILABLE`

### 3. Add Seat Expiration Job

Create a background job to release expired reservations:

```go
// Release seats that have been RESERVED for more than 15 minutes
UPDATE event_seats 
SET status = 'AVAILABLE', booking_id = NULL, reserved_until = NULL
WHERE status = 'RESERVED' 
AND reserved_until < NOW()
```

---

## Rollback

If you need to remove the event_seats table:

```sql
-- Drop table and all data
DROP TABLE IF EXISTS event_seats CASCADE;

-- Drop helper views (full version only)
DROP VIEW IF EXISTS v_available_seats;
DROP VIEW IF EXISTS v_seat_inventory;

-- Drop functions (full version only)
DROP FUNCTION IF EXISTS seats_exist_for_event(VARCHAR, TIMESTAMP);
DROP FUNCTION IF EXISTS create_seats_for_event(VARCHAR, TIMESTAMP, INTEGER);
DROP FUNCTION IF EXISTS update_event_seats_timestamp();
```

---

## Safety Features

Both migration files are **idempotent**, meaning:

✅ Safe to run multiple times  
✅ Won't create duplicate seats  
✅ Won't overwrite existing data  
✅ Checks for existing records before inserting  

---

## Support

For questions or issues:
1. Check the main documentation: `SEAT_MANAGEMENT_ANALYSIS.md`
2. Review migration guide: `MIGRATION_TO_PRESEATED.md`
3. Test in development environment first
4. Backup your database before running in production

---

## Migration Checklist

- [ ] Backup database
- [ ] Review migration SQL file
- [ ] Test in development environment
- [ ] Run migration script
- [ ] Verify seats were created (run verification queries)
- [ ] Update Go repository code
- [ ] Update booking service logic
- [ ] Add seat reservation workflow
- [ ] Test booking with pre-created seats
- [ ] Deploy to production

---

**Last Updated:** October 6, 2025  
**Version:** 1.0.0

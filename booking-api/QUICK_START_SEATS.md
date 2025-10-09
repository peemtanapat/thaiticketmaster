# 🎫 Pre-Created Seats Migration - Quick Start Guide

## What This Migration Does

✅ Creates `event_seats` table in your database  
✅ Pre-creates **40 seat tickets** (20 for Event 1, 20 for Event 2)  
✅ Sets up zone-based pricing (VIP, Premium, Standard)  
✅ All seats start with `status = 'AVAILABLE'`  
✅ Automatic duplicate prevention - safe to run multiple times  

---

## 🚀 Quick Start (3 Steps)

### Step 1: Ensure Database is Running

```bash
# Check if PostgreSQL is running
psql -h localhost -p 5432 -U postgres -d booking_db -c "SELECT version();"
```

### Step 2: Run Migration

```bash
# Option A: Using Make (Recommended)
make db-migrate

# Option B: Using psql directly
cd booking-api
psql -h localhost -p 5432 -U postgres -d booking_db -f migrations/001_create_event_seats_simple.sql
```

### Step 3: Verify Seats Were Created

```bash
# Show seat summary
make db-verify

# Show detailed inventory
make db-status
```

**Expected Output:**
```
 event_id |     showtime     | total_seats 
----------+------------------+-------------
 1        | 2025-12-25 19:00 |          20
 2        | 2025-12-31 20:00 |          20
```

---

## 📊 Pre-Created Seats Overview

### Event 1: Christmas Concert (Dec 25, 2025 @ 7:00 PM)

| Zone | Seats | Price | Total Value |
|------|-------|-------|-------------|
| **VIP** | A1, A2, A3, A4, A5 | ฿2,000 | ฿10,000 |
| **Premium** | B1, B2, B3, B4, B5 | ฿1,500 | ฿7,500 |
| **Standard** | C1-C5, D1-D5 | ฿1,000 | ฿10,000 |
| **TOTAL** | **20 seats** | | **฿27,500** |

### Event 2: New Year Show (Dec 31, 2025 @ 8:00 PM)

| Zone | Seats | Price | Total Value |
|------|-------|-------|-------------|
| **VIP** | A1, A2, A3, A4, A5 | ฿2,500 | ฿12,500 |
| **Premium** | B1, B2, B3, B4, B5 | ฿1,800 | ฿9,000 |
| **Standard** | C1-C5, D1-D5 | ฿1,200 | ฿12,000 |
| **TOTAL** | **20 seats** | | **฿33,500** |

---

## 🎯 Seat Layout Visualization

```
Event 1: Christmas Concert
┌─────────────────────────────────────┐
│  🎤 STAGE                           │
└─────────────────────────────────────┘

Row A (VIP - ฿2,000)
[A1] [A2] [A3] [A4] [A5]

Row B (Premium - ฿1,500)
[B1] [B2] [B3] [B4] [B5]

Row C (Standard - ฿1,000)
[C1] [C2] [C3] [C4] [C5]

Row D (Standard - ฿1,000)
[D1] [D2] [D3] [D4] [D5]
```

---

## 🛠️ Useful Make Commands

```bash
# Run migration
make db-migrate              # Simple version
make db-migrate-full         # Full version with functions

# Verify & check status
make db-verify              # Quick verification
make db-status              # Detailed inventory
make db-available           # Show only available seats

# View specific events
make db-show-event1         # All seats for Event 1
make db-show-event2         # All seats for Event 2

# Database connection
make db-connect             # Interactive psql session

# Cleanup (CAUTION!)
make db-clean-seats         # Remove event_seats table
```

---

## 🔍 Verification Queries

### Check total seats by event

```sql
SELECT 
    event_id,
    COUNT(*) as total_seats
FROM event_seats
GROUP BY event_id;
```

### Check seat distribution by zone

```sql
SELECT 
    event_id,
    zone,
    COUNT(*) as seat_count,
    MIN(price) as min_price,
    MAX(price) as max_price
FROM event_seats
GROUP BY event_id, zone
ORDER BY event_id, zone;
```

### Show all available seats

```sql
SELECT 
    event_id,
    seat_id,
    zone,
    price,
    status
FROM event_seats
WHERE status = 'AVAILABLE'
ORDER BY event_id, seat_id;
```

---

## 🔄 Seat Status Workflow

```
┌─────────────┐
│  AVAILABLE  │ ← Initial state (all 40 seats)
└──────┬──────┘
       │ User clicks "Book"
       ↓
┌─────────────┐
│  RESERVED   │ ← Temporary hold (5-15 minutes)
└──────┬──────┘
       │ Payment success
       ↓
┌─────────────┐
│    SOLD     │ ← Final state (seat is booked)
└─────────────┘
```

---

## 🧪 Testing the Migration

### 1. Check if table was created

```bash
psql -h localhost -U postgres -d booking_db -c "\dt event_seats"
```

### 2. Count total seats

```bash
psql -h localhost -U postgres -d booking_db -c "SELECT COUNT(*) FROM event_seats;"
```

**Expected:** `40`

### 3. Check seat distribution

```bash
make db-status
```

### 4. View sample seats

```bash
psql -h localhost -U postgres -d booking_db -c "SELECT * FROM event_seats LIMIT 10;"
```

---

## ❓ Troubleshooting

### Problem: "relation event_seats already exists"

**Solution:** The table already exists. This is normal if you ran the migration before.

```bash
# Verify seats exist
make db-verify
```

### Problem: "ERROR: duplicate key value violates unique constraint"

**Solution:** Seats were already inserted. The migration is idempotent - it skips existing data.

### Problem: No seats showing up

**Solution:** Check if the INSERT statement ran successfully:

```bash
psql -h localhost -U postgres -d booking_db -c "SELECT COUNT(*) FROM event_seats WHERE event_id IN ('1', '2');"
```

### Problem: Can't connect to database

**Solution:** Check database connection settings:

```bash
# Test connection
psql -h localhost -p 5432 -U postgres -d booking_db -c "SELECT 1;"

# Or update Makefile variables
export DB_HOST=your_host
export DB_PORT=5432
export DB_USER=postgres
export DB_PASSWORD=postgres
export DB_NAME=booking_db
```

---

## 🎓 Next Steps

After successful migration:

### 1. Update Go Repository

Add methods to interact with `event_seats`:

```go
// internal/booking/repository.go

// CheckSeatExists verifies seat exists in event_seats table
func (r *PostgresRepository) CheckSeatExists(ctx context.Context, 
    eventID string, showtime time.Time, seatID string) (bool, error)

// GetAvailableSeats returns all AVAILABLE seats for an event
func (r *PostgresRepository) GetAvailableSeats(ctx context.Context, 
    eventID string, showtime time.Time) ([]Seat, error)

// ReserveSeats marks seats as RESERVED
func (r *PostgresRepository) ReserveSeats(ctx context.Context, 
    bookingID string, seats []string, expiresAt time.Time) error

// ConfirmSeats marks RESERVED seats as SOLD
func (r *PostgresRepository) ConfirmSeats(ctx context.Context, 
    bookingID string) error

// ReleaseSeats marks RESERVED seats back to AVAILABLE
func (r *PostgresRepository) ReleaseSeats(ctx context.Context, 
    bookingID string) error
```

### 2. Update Booking Service

Validate against `event_seats` before booking:

```go
// Before creating booking, check seats exist and are available
availableSeats, err := repo.GetAvailableSeats(ctx, eventID, showtime)
if err != nil {
    return err
}

// Validate requested seats are in available list
for _, seatID := range requestedSeats {
    if !contains(availableSeats, seatID) {
        return fmt.Errorf("seat %s is not available", seatID)
    }
}

// Reserve seats (temporary hold)
err = repo.ReserveSeats(ctx, bookingID, requestedSeats, time.Now().Add(15*time.Minute))
```

### 3. Add Background Job

Release expired reservations:

```go
// Release seats reserved for more than 15 minutes
func ReleaseExpiredReservations(ctx context.Context, repo Repository) error {
    query := `
        UPDATE event_seats 
        SET status = 'AVAILABLE', 
            booking_id = NULL, 
            reserved_until = NULL
        WHERE status = 'RESERVED' 
        AND reserved_until < NOW()
    `
    _, err := repo.db.ExecContext(ctx, query)
    return err
}

// Run every 1 minute
ticker := time.NewTicker(1 * time.Minute)
go func() {
    for range ticker.C {
        ReleaseExpiredReservations(ctx, repo)
    }
}()
```

---

## 📚 Additional Resources

- **Full Documentation:** `migrations/README.md`
- **Migration Scripts:**
  - Simple version: `migrations/001_create_event_seats_simple.sql`
  - Full version: `migrations/001_create_event_seats.sql`
- **Architecture Guide:** `SEAT_MANAGEMENT_ANALYSIS.md`
- **Implementation Guide:** `MIGRATION_TO_PRESEATED.md`

---

## ✅ Migration Checklist

- [ ] Database is running and accessible
- [ ] Ran `make db-migrate` successfully
- [ ] Verified 40 seats were created (`make db-verify`)
- [ ] Checked seat inventory (`make db-status`)
- [ ] Tested seat queries
- [ ] Updated Go repository code (if needed)
- [ ] Updated booking service logic (if needed)
- [ ] Added seat reservation workflow (if needed)
- [ ] Tested booking with pre-created seats

---

## 🎉 Success!

If you can see 40 seats (20 per event) when running `make db-verify`, your migration was successful!

Your system now has:
- ✅ Pre-created seat inventory
- ✅ Zone-based pricing
- ✅ Seat status management
- ✅ Industry-standard ticketing architecture

**You're ready to build a professional ticketing system!** 🚀

---

**Questions?** Check the full documentation in `migrations/README.md`

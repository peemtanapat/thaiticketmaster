# 🚀 Quick Migration Commands

Run these commands to apply the TIMESTAMPTZ migration.

---

## event-api (Java/Spring Boot)

### Run Migration
```bash
cd event-api

# Clean and build
./mvnw clean install

# Start application (Flyway runs V3 migration automatically)
./mvnw spring-boot:run
```

### Verify Migration
```bash
# Check if migration ran
psql -h localhost -U your_user -d event_api_db -c "
SELECT version, description, installed_on 
FROM flyway_schema_history 
WHERE version = '3';
"

# Verify column type
psql -h localhost -U your_user -d event_api_db -c "
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'event_show_times' 
AND column_name = 'show_datetime';
"
# Expected: timestamp with time zone
```

---

## booking-api (Go)

### Run All Migrations
```bash
cd booking-api

# Run migration 001 (creates event_seats with TIMESTAMPTZ)
make db-migrate

# Run migration 002 (adds timestamp columns)
make db-migrate-timestamps

# Migration 003 runs automatically when needed
```

### Verify Migrations
```bash
# Check bookings table
PGPASSWORD=admin psql -h localhost -p 5432 -U admin -d booking_db -c "
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'bookings' 
AND column_name IN ('showtime', 'created_at', 'updated_at');
"

# Check event_seats table
PGPASSWORD=admin psql -h localhost -p 5432 -U admin -d booking_db -c "
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'event_seats' 
AND column_name IN ('showtime', 'reserved_at', 'reserved_until', 'sold_at');
"

# All should return: timestamp with time zone
```

---

## Full Reset (if needed)

### event-api
```bash
cd event-api

# Drop and recreate database
psql -h localhost -U your_user -d postgres -c "DROP DATABASE IF EXISTS event_api_db;"
psql -h localhost -U your_user -d postgres -c "CREATE DATABASE event_api_db;"

# Run application (creates schema + runs all migrations)
./mvnw spring-boot:run
```

### booking-api
```bash
cd booking-api

# Drop and recreate database
PGPASSWORD=admin psql -h localhost -p 5432 -U admin -d postgres -c "DROP DATABASE IF EXISTS booking_db;"
PGPASSWORD=admin psql -h localhost -p 5432 -U admin -d postgres -c "CREATE DATABASE booking_db;"

# Run all migrations
make db-migrate
make db-migrate-timestamps
```

---

## Test Commands

### event-api Tests
```bash
cd event-api
./mvnw test
```

### booking-api Tests
```bash
cd booking-api
make test
# or
make test-coverage
```

---

## Check Data

### event-api Sample Data
```bash
psql -h localhost -U your_user -d event_api_db -c "
SELECT e.name, est.show_datetime 
FROM events e 
JOIN event_show_times est ON e.id = est.event_id 
LIMIT 5;
"
```

### booking-api Sample Data
```bash
PGPASSWORD=admin psql -h localhost -p 5432 -U admin -d booking_db -c "
SELECT event_id, seat_id, showtime, status, created_at 
FROM event_seats 
LIMIT 10;
"
```

---

## One-Line Complete Migration

```bash
# event-api
cd event-api && ./mvnw clean spring-boot:run

# booking-api
cd booking-api && make db-migrate && make db-migrate-timestamps && make test
```

---

## Verification Script

```bash
#!/bin/bash
echo "==================================="
echo "Verifying TIMESTAMPTZ Migration"
echo "==================================="

echo ""
echo "📊 event-api (event_show_times.show_datetime):"
psql -h localhost -U your_user -d event_api_db -c "
SELECT data_type FROM information_schema.columns 
WHERE table_name = 'event_show_times' AND column_name = 'show_datetime';
" -t

echo ""
echo "📊 booking-api (bookings.showtime):"
PGPASSWORD=admin psql -h localhost -p 5432 -U admin -d booking_db -c "
SELECT data_type FROM information_schema.columns 
WHERE table_name = 'bookings' AND column_name = 'showtime';
" -t

echo ""
echo "📊 booking-api (event_seats.showtime):"
PGPASSWORD=admin psql -h localhost -p 5432 -U admin -d booking_db -c "
SELECT data_type FROM information_schema.columns 
WHERE table_name = 'event_seats' AND column_name = 'showtime';
" -t

echo ""
echo "✅ If all show 'timestamp with time zone', migration is complete!"
```

Save as `verify_migration.sh`, make executable: `chmod +x verify_migration.sh`, then run: `./verify_migration.sh`

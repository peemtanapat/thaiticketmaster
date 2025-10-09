# Database Setup - Booking API

## Overview

The booking-api automatically creates and initializes its PostgreSQL database on startup. The application will:

1. ✅ Check if the database exists
2. ✅ Create the database if it doesn't exist
3. ✅ Create all necessary tables and indexes
4. ✅ Verify the schema is up-to-date

---

## Automatic Database Creation

### How It Works

When the application starts, it executes the following steps:

```go
1. Connect to PostgreSQL 'postgres' database (default)
2. Check if 'booking_db' exists
3. If not exists, create 'booking_db'
4. Connect to 'booking_db'
5. Create/verify tables and indexes
6. Start the application
```

### Code Location

- **Database Functions**: `cmd/api/database.go`
  - `ensureDatabaseExists()` - Creates database if not exists
  - `createBookingSchema()` - Creates tables and indexes

- **Main Application**: `cmd/api/main.go`
  - Calls database functions on startup

---

## Database Schema

### Tables Created

#### 1. **bookings** Table

Stores booking information.

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
```

**Columns**:
- `id` - Auto-incrementing primary key
- `booking_id` - Unique booking identifier (UUID)
- `event_id` - Reference to event from event-api
- `user_id` - User who made the booking
- `showtime` - Event showtime
- `quantity` - Number of tickets
- `status` - Booking status (CONFIRMED, CANCELLED, etc.)
- `created_at` - Timestamp when booking was created
- `updated_at` - Timestamp when booking was last updated

**Indexes**:
- `idx_bookings_event_id` - Fast lookups by event
- `idx_bookings_user_id` - Fast lookups by user
- `idx_bookings_status` - Fast lookups by status

#### 2. **booking_seats** Table

Stores seat assignments for each booking.

```sql
CREATE TABLE booking_seats (
    id SERIAL PRIMARY KEY,
    booking_id VARCHAR(255) NOT NULL,
    seat_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
);
```

**Columns**:
- `id` - Auto-incrementing primary key
- `booking_id` - Reference to booking (foreign key)
- `seat_id` - Seat identifier (e.g., "A1", "B2")
- `created_at` - Timestamp when seat was assigned

**Indexes**:
- `idx_booking_seats_booking_id` - Fast lookups by booking

**Relationships**:
- Foreign key to `bookings.booking_id` with CASCADE delete

---

## Configuration

### Environment Variables

```bash
# Database Configuration
DB_HOST=localhost        # PostgreSQL host
DB_PORT=5432            # PostgreSQL port
DB_USER=postgres        # PostgreSQL user (must have CREATE DATABASE permission)
DB_PASSWORD=postgres    # PostgreSQL password
DB_NAME=booking_db      # Database name to create/use
```

### Required PostgreSQL Permissions

The database user must have:
- ✅ **CREATE DATABASE** - To create the database
- ✅ **CREATE TABLE** - To create tables
- ✅ **CREATE INDEX** - To create indexes

---

## Running the Application

### First Time Setup

```bash
# 1. Ensure PostgreSQL is running
# 2. Create a PostgreSQL user with appropriate permissions (if needed)

# 3. Start the application
cd booking-api
make run

# The application will automatically:
# - Create 'booking_db' database
# - Create 'bookings' and 'booking_seats' tables
# - Create all necessary indexes
```

**Console Output**:
```
Checking database existence...
Database 'booking_db' does not exist. Creating...
Successfully created database 'booking_db'
Successfully connected to database
Creating/verifying database schema...
Successfully created/verified database schema
Registered endpoints:
  GET  /health
  POST /api/v1/bookings
  ...
Starting booking-api server on port 8081
```

### Subsequent Runs

On subsequent runs, the application will:
- ✅ Detect that the database exists
- ✅ Skip database creation
- ✅ Verify/update schema if needed

**Console Output**:
```
Checking database existence...
Database 'booking_db' already exists
Successfully connected to database
Creating/verifying database schema...
Successfully created/verified database schema
...
```

---

## Manual Database Setup (Optional)

If you prefer to create the database manually:

### Using psql

```bash
# Connect to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE booking_db;

# Connect to the database
\c booking_db

# Create tables (or let the app do it)
# The app will create tables automatically even if you create the DB manually
```

### Using SQL Script

```sql
-- Create database
CREATE DATABASE booking_db;

-- Connect to booking_db
\c booking_db

-- Tables and indexes will be created automatically by the application
```

---

## Verification

### Check Database Exists

```bash
psql -U postgres -c "\l" | grep booking_db
```

### Check Tables Exist

```bash
psql -U postgres -d booking_db -c "\dt"
```

Expected output:
```
             List of relations
 Schema |     Name      | Type  | Owner 
--------+---------------+-------+-------
 public | bookings      | table | admin
 public | booking_seats | table | admin
```

### Check Schema Details

```bash
# View bookings table structure
psql -U postgres -d booking_db -c "\d bookings"

# View booking_seats table structure
psql -U postgres -d booking_db -c "\d booking_seats"

# View indexes
psql -U postgres -d booking_db -c "\di"
```

---

## Troubleshooting

### Issue: Permission Denied

**Error**: `permission denied to create database`

**Solution**: Grant CREATE DATABASE permission to the user:
```sql
ALTER USER admin CREATEDB;
```

### Issue: Database Already Exists but Schema is Missing

**Solution**: The application will automatically create the schema on startup. If tables are missing, restart the application.

### Issue: Connection Refused

**Error**: `connection refused`

**Solution**:
1. Ensure PostgreSQL is running: `pg_isready`
2. Check PostgreSQL is listening on correct host/port
3. Verify firewall rules allow connection

### Issue: Authentication Failed

**Error**: `password authentication failed`

**Solution**:
1. Verify credentials in environment variables
2. Check `pg_hba.conf` authentication method
3. Reset password if needed: `ALTER USER admin PASSWORD 'newpassword';`

---

## Testing

### Run Database Tests

```bash
# Run integration tests (requires PostgreSQL)
cd booking-api
go test -v ./cmd/api -run TestEnsureDatabaseExists
go test -v ./cmd/api -run TestCreateBookingSchema
```

**Note**: Integration tests create and drop test databases:
- `booking_api_test`
- `booking_api_schema_test`

### Skip Integration Tests

```bash
# Skip tests that require PostgreSQL
go test -v -short ./cmd/api
```

---

## Database Migrations

### Current Approach

The application uses **auto-migration** on startup:
- ✅ Simple and automatic
- ✅ No manual migration steps
- ✅ Idempotent (safe to run multiple times)
- ✅ Uses `CREATE TABLE IF NOT EXISTS`

### Future: Migration Tool

For production, consider using a migration tool:
- **golang-migrate** - Popular Go migration tool
- **Flyway** - Java-based migration tool
- **Liquibase** - Database-independent migrations

---

## Best Practices

### Development

✅ **Let the app create the database** - Easiest for development  
✅ **Use environment variables** - Keep credentials out of code  
✅ **Run tests in isolation** - Use separate test databases  

### Production

✅ **Pre-create the database** - Don't rely on auto-creation  
✅ **Use migration tools** - For version control of schema  
✅ **Backup regularly** - Before schema changes  
✅ **Use connection pooling** - For better performance  
✅ **Monitor database size** - Set up alerts  

---

## Database Diagram

```
┌─────────────────────────────────────┐
│           bookings                  │
├─────────────────────────────────────┤
│ id (PK)                             │
│ booking_id (UNIQUE) ◄───────────┐   │
│ event_id                         │   │
│ user_id                          │   │
│ showtime                         │   │
│ quantity                         │   │
│ status                           │   │
│ created_at                       │   │
│ updated_at                       │   │
└─────────────────────────────────────┘
                                    │
                                    │ FK
                                    │
┌─────────────────────────────────────┐
│         booking_seats               │
├─────────────────────────────────────┤
│ id (PK)                             │
│ booking_id (FK) ────────────────────┘
│ seat_id                             │
│ created_at                          │
└─────────────────────────────────────┘
```

---

## Connection Pooling

The application uses Go's `database/sql` package which includes built-in connection pooling.

### Default Settings

```go
// Adjust these in main.go if needed
db.SetMaxOpenConns(25)      // Maximum open connections
db.SetMaxIdleConns(5)       // Maximum idle connections
db.SetConnMaxLifetime(5*time.Minute) // Connection lifetime
```

---

## Monitoring

### Check Connection Status

```sql
-- View active connections
SELECT * FROM pg_stat_activity 
WHERE datname = 'booking_db';

-- Count connections
SELECT count(*) FROM pg_stat_activity 
WHERE datname = 'booking_db';
```

### Check Table Sizes

```sql
-- View table sizes
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

---

## Security

### Recommendations

✅ **Use strong passwords** - Never use default passwords in production  
✅ **Limit permissions** - Grant only necessary privileges  
✅ **Use SSL/TLS** - Enable `sslmode=require` in production  
✅ **Rotate credentials** - Change passwords periodically  
✅ **Audit access** - Monitor database access logs  

### Connection String (Production)

```go
// Development (sslmode=disable)
dsn := "host=localhost port=5432 user=admin password=admin dbname=booking_db sslmode=disable"

// Production (sslmode=require)
dsn := "host=prod-db.example.com port=5432 user=app_user password=strong_password dbname=booking_db sslmode=require"
```

---

## Summary

✅ **Automatic Database Creation** - No manual setup required  
✅ **Auto Schema Management** - Tables and indexes created automatically  
✅ **Idempotent Operations** - Safe to run multiple times  
✅ **Well-Tested** - Integration tests verify functionality  
✅ **Production Ready** - Proper error handling and logging  

**The booking-api handles all database setup automatically!** 🎉

# Database Credentials Update Summary

## ✅ Changes Applied

All database credentials in the `booking-api` project have been updated from:

### Old Credentials
```
Username: admin
Password: admin
Database: booking_api
```

### New Credentials
```
Username: postgres
Password: postgres
Database: booking_db
```

---

## 📝 Files Updated

### 1. Migration Documentation (4 files)
- ✅ `migrations/README.md`
- ✅ `migrations/MIGRATION_CONSOLIDATION.md`
- ✅ `migrations/CONSOLIDATION_SUMMARY.md`
- ✅ `migrations/001_create_complete_schema.sql` (showtimes also updated)

### 2. Main Documentation (5 files)
- ✅ `QUICK_START.md`
- ✅ `DATABASE_SETUP.md`
- ✅ `DEBUG_GUIDE.md`
- ✅ `VSCODE_DEBUG_GUIDE.md`
- ✅ `QUICK_START_SEATS.md`
- ✅ `SEATS_IMPLEMENTATION_SUMMARY.md`

### 3. Test Files (1 file)
- ✅ `cmd/api/database_test.go`

### 4. Database Names Updated
- ✅ `booking_api` → `booking_db` (production)
- ✅ `booking_api_test` → `booking_db_test` (test database)
- ✅ `booking_api_schema_test` → `booking_db_schema_test` (schema test)

---

## 🔄 Additional Updates

### Event Showtimes Synchronized

Updated `001_create_complete_schema.sql` to match event-api showtimes:

| Event ID | Old Showtime | New Showtime | Event Name |
|----------|--------------|--------------|------------|
| event_1 | 2025-12-25 19:00:00+00 | **2025-10-04 19:00:00+07:00** | ONE Fight Night 36 🥊 |
| event_2 | 2025-12-31 20:00:00+00 | **2025-10-11 18:00:00+07:00** | MARIAH CAREY 🎤 |

**Benefits:**
- ✅ Consistent showtimes across event-api and booking-api
- ✅ Proper Bangkok timezone (UTC+7) instead of UTC
- ✅ Matches real event data from event-api/data.sql

---

## 🚀 Quick Start Commands (Updated)

### Create Fresh Database

```bash
# Drop existing database (if any)
psql -h localhost -U postgres -d postgres \
  -c "DROP DATABASE IF EXISTS booking_db;"

# Create new database
psql -h localhost -U postgres -d postgres \
  -c "CREATE DATABASE booking_db;"

# Run migration
psql -h localhost -U postgres -d booking_db \
  -f booking-api/migrations/001_create_complete_schema.sql
```

### Verify Setup

```bash
# Check database exists
psql -h localhost -U postgres -d booking_db -c "SELECT version();"

# Count seats (should return 40)
psql -h localhost -U postgres -d booking_db \
  -c "SELECT COUNT(*) FROM event_seats;"

# View event showtimes
psql -h localhost -U postgres -d booking_db \
  -c "SELECT event_id, showtime, COUNT(*) as seats FROM event_seats GROUP BY event_id, showtime;"
```

**Expected Output:**
```
 event_id |         showtime          | seats
----------+---------------------------+-------
 event_1  | 2025-10-04 19:00:00+07    |    20
 event_2  | 2025-10-11 18:00:00+07    |    20
```

---

## 🔧 Environment Variables

### Update Your `.env` File

```bash
# Old
DB_USER=admin
DB_PASSWORD=admin
DB_NAME=booking_api

# New (update to this)
DB_USER=postgres
DB_PASSWORD=postgres
DB_NAME=booking_db
```

### Update VS Code Launch Configuration

If you're using VS Code debugging, update `.vscode/launch.json`:

```json
{
  "env": {
    "DB_USER": "postgres",
    "DB_PASSWORD": "postgres",
    "DB_NAME": "booking_db"
  }
}
```

---

## 📋 Testing Checklist

After updating:

- [ ] Update `.env` file with new credentials
- [ ] Drop old `booking_api` database (if exists)
- [ ] Create new `booking_db` database
- [ ] Run migration script
- [ ] Verify 40 seats created
- [ ] Test application connection
- [ ] Verify showtimes match event-api

---

## 🎯 Summary

| Change Type | Count | Status |
|-------------|-------|--------|
| Documentation files | 10 | ✅ Updated |
| Test files | 1 | ✅ Updated |
| Migration files | 1 | ✅ Updated (+ showtimes) |
| Database names | 3 | ✅ Updated |
| Credentials | All | ✅ Standardized to postgres/postgres |
| Showtimes | 2 events | ✅ Synchronized with event-api |

---

## ✨ Benefits

1. **Standard Credentials** - Using default PostgreSQL credentials
2. **Consistent Naming** - `booking_db` matches naming convention
3. **Timezone Accuracy** - Bangkok time (UTC+7) for all events
4. **Synchronized Data** - Showtimes match between microservices
5. **Complete Documentation** - All docs updated with correct info

---

**All credential updates complete!** 🎉

The booking-api project now uses standard PostgreSQL credentials and has synchronized showtimes with the event-api service.

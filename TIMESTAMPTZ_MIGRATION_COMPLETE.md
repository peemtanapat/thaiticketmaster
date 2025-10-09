# 🕐 TIMESTAMPTZ Migration Complete

Complete migration of all showtime fields to **TIMESTAMP WITH TIME ZONE** in both event-api and booking-api.

**Date:** October 6, 2025  
**Status:** ✅ Complete

---

## 📋 Summary

### What Changed
All timestamp fields related to showtimes have been migrated from `TIMESTAMP WITHOUT TIME ZONE` to `TIMESTAMP WITH TIME ZONE (TIMESTAMPTZ)` to properly handle timezone-aware timestamps across the system.

### Why This Matters
- ✅ **Timezone Awareness**: Events can be scheduled in different timezones (Bangkok, UTC, etc.)
- ✅ **Data Accuracy**: No more timezone ambiguity or conversion errors
- ✅ **International Support**: System ready for global events
- ✅ **DST Handling**: Automatic daylight saving time adjustments

---

## 🎯 Changes Made

### 1. **event-api (Java/Spring Boot)**

#### Database Schema
- ✅ **Migration V3**: `event_show_times.show_datetime` → `TIMESTAMPTZ`
- ✅ **data.sql**: Sample data updated with explicit timezones (+07:00 for Bangkok)

#### Java Code
- ✅ **Event.java**: `List<LocalDateTime>` → `List<OffsetDateTime>` for showDateTimes
- ✅ **EventDTO.java**: `List<LocalDateTime>` → `List<OffsetDateTime>` for showDateTimes
- ✅ **Column Definition**: Added `columnDefinition = "TIMESTAMPTZ"` to entity mapping

**Files Modified:**
```
event-api/src/main/java/dev/peemtanapat/thaiticketmaster/event_api/event/
├── Event.java              (showDateTimes field + getter/setter + constructor)
├── EventDTO.java           (showDateTimes field + getter/setter + constructor)

event-api/src/main/resources/
├── data.sql                (show_datetime values with +07:00 timezone)
└── db/migration/
    └── V3__alter_event_show_times_timestamptz.sql  (already existed)
```

---

### 2. **booking-api (Go)**

#### Database Schema
- ✅ **database.go**: `bookings.showtime` → `TIMESTAMPTZ`
- ✅ **database.go**: All `created_at`, `updated_at` → `TIMESTAMPTZ`
- ✅ **Migration 001**: `event_seats.showtime` → `TIMESTAMPTZ`
- ✅ **Migration 001**: Function signatures updated to use `TIMESTAMPTZ`
- ✅ **Migration 001**: Sample data updated with explicit timezone (+00 for UTC)
- ✅ **Migration 002**: `reserved_at`, `reserved_until`, `sold_at` → `TIMESTAMPTZ`
- ✅ **Migration 003**: Conversion migration (already existed)

#### Go Code
- ✅ **No changes needed**: Go's `time.Time` already supports timezones natively
- ✅ **PostgreSQL driver**: `pq` driver handles TIMESTAMPTZ correctly
- ✅ **JSON serialization**: RFC3339 format includes timezone automatically

**Files Modified:**
```
booking-api/
├── cmd/api/
│   └── database.go                                 (TIMESTAMP → TIMESTAMPTZ)
└── migrations/
    ├── 001_create_event_seats.sql                  (showtime + functions + sample data)
    ├── 002_add_event_seats_timestamps.sql          (reserved_at, reserved_until, sold_at)
    └── 003_alter_showtime_timestamptz.sql          (already existed)
```

---

## 🗄️ Database Schema Changes

### event-api Database

#### Before:
```sql
CREATE TABLE event_show_times (
    event_id BIGINT,
    show_datetime TIMESTAMP WITHOUT TIME ZONE  -- ❌ No timezone
);
```

#### After:
```sql
CREATE TABLE event_show_times (
    event_id BIGINT,
    show_datetime TIMESTAMPTZ  -- ✅ With timezone
);
```

---

### booking-api Database

#### Before:
```sql
CREATE TABLE bookings (
    showtime TIMESTAMP NOT NULL,            -- ❌ No timezone
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE event_seats (
    showtime TIMESTAMP NOT NULL,            -- ❌ No timezone
    reserved_at TIMESTAMP,
    reserved_until TIMESTAMP,
    sold_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

#### After:
```sql
CREATE TABLE bookings (
    showtime TIMESTAMPTZ NOT NULL,          -- ✅ With timezone
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE event_seats (
    showtime TIMESTAMPTZ NOT NULL,          -- ✅ With timezone
    reserved_at TIMESTAMPTZ,
    reserved_until TIMESTAMPTZ,
    sold_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

---

## 📝 Code Changes

### Java (event-api)

#### Before:
```java
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "event_show_times", joinColumns = @JoinColumn(name = "event_id"))
@Column(name = "show_datetime", nullable = false)
private List<LocalDateTime> showDateTimes = new ArrayList<>();

public List<LocalDateTime> getShowDateTimes() {
    return showDateTimes;
}

public void setShowDateTimes(List<LocalDateTime> showDateTimes) {
    this.showDateTimes = showDateTimes;
}
```

#### After:
```java
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "event_show_times", joinColumns = @JoinColumn(name = "event_id"))
@Column(name = "show_datetime", nullable = false, columnDefinition = "TIMESTAMPTZ")
private List<OffsetDateTime> showDateTimes = new ArrayList<>();

public List<OffsetDateTime> getShowDateTimes() {
    return showDateTimes;
}

public void setShowDateTimes(List<OffsetDateTime> showDateTimes) {
    this.showDateTimes = showDateTimes;
}
```

**Key Changes:**
- ✅ `LocalDateTime` → `OffsetDateTime` (timezone-aware)
- ✅ Added `columnDefinition = "TIMESTAMPTZ"`
- ✅ Updated constructors, getters, setters
- ✅ Updated EventDTO to match

---

### Go (booking-api)

**No code changes needed!** Go's `time.Time` already supports timezones:

```go
type Booking struct {
    Showtime  time.Time `json:"showtime"`  // ✅ Already timezone-aware
}
```

**Why?**
- Go's `time.Time` stores timezone information internally
- PostgreSQL `pq` driver converts TIMESTAMPTZ ↔ time.Time correctly
- JSON serialization uses RFC3339 format (includes timezone)

---

## 🧪 Testing

### Run Migrations

#### event-api:
```bash
cd event-api
./mvnw spring-boot:run
# Flyway will automatically run V3__alter_event_show_times_timestamptz.sql
```

#### booking-api:
```bash
cd booking-api
make db-migrate              # Runs all migrations
make db-migrate-timestamps   # Specifically migration 002
```

---

### Verify Schema

#### event-api (PostgreSQL):
```sql
-- Connect to event-api database
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'event_show_times' 
AND column_name = 'show_datetime';

-- Expected: show_datetime | timestamp with time zone
```

#### booking-api (PostgreSQL):
```sql
-- Connect to booking_db
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'bookings' 
AND column_name = 'showtime';

SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'event_seats' 
AND column_name IN ('showtime', 'reserved_at', 'reserved_until', 'sold_at');

-- Expected: All should be "timestamp with time zone"
```

---

### Test Data

#### event-api Sample Query:
```sql
SELECT event_id, show_datetime 
FROM event_show_times 
LIMIT 5;

-- Expected format: 2025-10-04 19:00:00+07 (Bangkok time)
```

#### booking-api Sample Query:
```sql
SELECT seat_id, showtime, created_at 
FROM event_seats 
WHERE event_id = '1' 
LIMIT 5;

-- Expected format: 2025-12-25 19:00:00+00 (UTC)
```

---

## 🔄 API Response Changes

### event-api Response (JSON)

#### Before:
```json
{
  "showDateTimes": [
    "2025-10-04T19:00:00"  // ❌ No timezone info
  ]
}
```

#### After:
```json
{
  "showDateTimes": [
    "2025-10-04T19:00:00+07:00"  // ✅ Bangkok timezone
  ]
}
```

---

### booking-api Response (JSON)

#### Before:
```json
{
  "showtime": "2025-12-25T19:00:00Z"  // ✅ Already had timezone (Go default)
}
```

#### After:
```json
{
  "showtime": "2025-12-25T19:00:00Z"  // ✅ No change (Go handles it)
}
```

---

## ⚠️ Breaking Changes

### For event-api Consumers

**Impact:** API responses now include timezone offsets in showtime fields

**Before:**
```json
"showDateTimes": ["2025-10-04T19:00:00"]
```

**After:**
```json
"showDateTimes": ["2025-10-04T19:00:00+07:00"]
```

**Action Required:**
- ✅ Update clients to parse ISO 8601 with timezone offsets
- ✅ Use timezone-aware date parsers (JavaScript `Date`, Java `OffsetDateTime`, etc.)
- ✅ Test timezone conversions in client code

---

### For booking-api Consumers

**Impact:** Minimal - Go already used timezone-aware timestamps

**Action Required:**
- ✅ Verify existing date parsing still works
- ✅ Test that timezone information is preserved

---

## 📊 Timezone Handling

### Bangkok Time (Asia/Bangkok = UTC+7)
```
Event time: 2025-10-04 19:00:00 Bangkok time
Database:   2025-10-04 19:00:00+07:00
UTC equiv:  2025-10-04 12:00:00+00:00 (same instant)
```

### UTC Time
```
Event time: 2025-12-25 19:00:00 UTC
Database:   2025-12-25 19:00:00+00:00
Bangkok:    2025-12-26 02:00:00+07:00 (same instant)
```

---

## 🚀 Deployment Steps

### 1. **Backup Databases**
```bash
# event-api database
pg_dump -h localhost -U your_user event_api_db > event_api_backup.sql

# booking-api database
pg_dump -h localhost -U admin booking_db > booking_db_backup.sql
```

### 2. **Run Migrations**

#### event-api (automatic via Flyway):
```bash
cd event-api
./mvnw clean install
./mvnw spring-boot:run
```

#### booking-api (manual):
```bash
cd booking-api
make db-migrate
```

### 3. **Verify Data Integrity**
```sql
-- Check that no data was lost
SELECT COUNT(*) FROM event_show_times;
SELECT COUNT(*) FROM bookings;
SELECT COUNT(*) FROM event_seats;
```

### 4. **Test Application**
```bash
# Start event-api
cd event-api && ./mvnw spring-boot:run

# Start booking-api
cd booking-api && make run

# Run tests
cd event-api && ./mvnw test
cd booking-api && make test
```

---

## 🐛 Troubleshooting

### Issue: "column show_datetime does not exist"
**Cause:** Migration not run yet  
**Solution:** 
```bash
cd event-api && ./mvnw spring-boot:run  # Runs Flyway migrations
```

---

### Issue: "Type mismatch in Java tests"
**Cause:** Tests still use `LocalDateTime`  
**Solution:** Update test files to use `OffsetDateTime`:
```java
// Before
List<LocalDateTime> showTimes = Arrays.asList(LocalDateTime.now());

// After
List<OffsetDateTime> showTimes = Arrays.asList(OffsetDateTime.now());
```

---

### Issue: "Data appears in wrong timezone"
**Cause:** Client not handling timezone offset  
**Solution:** Use timezone-aware parsing:
```javascript
// JavaScript
const date = new Date("2025-10-04T19:00:00+07:00");

// Java
OffsetDateTime date = OffsetDateTime.parse("2025-10-04T19:00:00+07:00");
```

---

## ✅ Verification Checklist

### Database
- [ ] Run migration V3 in event-api
- [ ] Run migrations 001, 002, 003 in booking-api
- [ ] Verify all showtime columns are TIMESTAMPTZ
- [ ] Check sample data has timezone offsets

### Code
- [ ] Event.java uses OffsetDateTime
- [ ] EventDTO.java uses OffsetDateTime
- [ ] Go code compiles without errors
- [ ] All tests pass

### API
- [ ] event-api returns timestamps with timezone offsets
- [ ] booking-api handles timezone-aware timestamps
- [ ] Client applications can parse new format

### Testing
- [ ] Create test booking with timezone-aware showtime
- [ ] Query shows correct timezone in response
- [ ] Timezone conversions work correctly

---

## 📚 References

### PostgreSQL
- [PostgreSQL Date/Time Types](https://www.postgresql.org/docs/current/datatype-datetime.html)
- [TIMESTAMP vs TIMESTAMPTZ](https://www.postgresql.org/docs/current/datatype-datetime.html#DATATYPE-TIMEZONES)

### Java
- [OffsetDateTime JavaDoc](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/OffsetDateTime.html)
- [LocalDateTime vs OffsetDateTime](https://www.baeldung.com/java-8-date-time-intro)

### Go
- [time.Time Package](https://pkg.go.dev/time)
- [PostgreSQL pq Driver](https://pkg.go.dev/github.com/lib/pq)

---

## 🎉 Migration Complete!

All showtime fields now use **TIMESTAMP WITH TIME ZONE** for proper timezone handling.

**Benefits:**
- ✅ Timezone-aware event scheduling
- ✅ Accurate time conversions
- ✅ International support
- ✅ DST compatibility

**Next Steps:**
1. Run migrations in both projects
2. Update client applications to handle timezone offsets
3. Test booking flows with different timezones
4. Monitor for any timezone-related issues

---

**For questions or issues, refer to the migration files in:**
- `event-api/src/main/resources/db/migration/V3__alter_event_show_times_timestamptz.sql`
- `booking-api/migrations/003_alter_showtime_timestamptz.sql`

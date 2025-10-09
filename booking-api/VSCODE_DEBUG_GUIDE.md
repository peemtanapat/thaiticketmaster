# 🐛 VS Code Breakpoint Debugging Guide - Booking API

Complete guide to debugging the booking-api in VS Code with visual breakpoints.

---

## ⚡ Quick Start (3 Steps)

### 1. **Stop Running Server**
```bash
# Press Ctrl+C in terminal, or:
pkill -f "booking-api"
```

### 2. **Set Breakpoints**
- Open any `.go` file in `booking-api/`
- Click in the **left gutter** (left of line numbers) to add red dots 🔴
- Recommended files:
  - `internal/booking/handler.go` (line ~45)
  - `internal/booking/booking_service.go` (line ~80)
  - `internal/booking/repository.go` (line ~360)

### 3. **Start Debugging**
**Method A:** Use Run and Debug panel
1. Click "Run and Debug" icon (▶️🐛) in left sidebar
2. Select **"Debug Booking API"** from dropdown
3. Click green play button ▶️

**Method B:** Use keyboard
1. Press `F5`
2. Select **"Debug Booking API"**

**Method C:** Use Command Palette
1. Press `Cmd+Shift+P` (Mac) or `Ctrl+Shift+P` (Windows)
2. Type: "Debug: Select and Start Debugging"
3. Choose **"Debug Booking API"**

✅ **Server starts in debug mode!**

---

## 🎯 Test Your Breakpoints

### Send a test request (using `client.http`):
```http
POST http://localhost:8081/api/v1/bookings
Content-Type: application/json

{
  "eventId": "1",
  "userId": "user-123",
  "showtime": "2025-10-05T02:00:00Z",
  "quantity": 2,
  "seatIds": ["A1", "A3"]
}
```

**🎉 VS Code will pause at your breakpoint!**

---

## 🎮 Debug Controls

### Keyboard Shortcuts (Mac / Windows)

| Action | Mac | Windows/Linux | Icon | Description |
|--------|-----|---------------|------|-------------|
| **Continue** | `F5` | `F5` | ▶️ | Resume until next breakpoint |
| **Step Over** | `F10` | `F10` | ⤵️ | Execute line, don't enter functions |
| **Step Into** | `F11` | `F11` | ⬇️ | Go inside function calls |
| **Step Out** | `Shift+F11` | `Shift+F11` | ⬆️ | Exit current function |
| **Restart** | `Cmd+Shift+F5` | `Ctrl+Shift+F5` | 🔄 | Restart debug session |
| **Stop** | `Shift+F5` | `Shift+F5` | ⏹️ | Stop debugging |

### Debug Toolbar (appears when paused)
```
▶️ Continue | ⤵️ Step Over | ⬇️ Step Into | ⬆️ Step Out | 🔄 Restart | ⏹️ Stop
```

---

## 🔍 Inspection Panels

### 1. **Variables** (Shows automatically when paused)
- **Local variables**: All variables in current function
- **Function arguments**: Parameters passed to function
- **Expand** objects to see nested fields

Example when paused in `CreateBooking`:
```
▼ Variables
  ▼ req
    eventId: "1"
    userId: "user-123"
    quantity: 2
    ▼ seatIds
      [0]: "A1"
      [1]: "A3"
    ▼ showtime
      time.Time: 2025-10-05T02:00:00Z
  err: <nil>
```

### 2. **Watch** (Add custom expressions)
Click "+ Add Expression":
```
booking.BookingID
len(booking.SeatIDs)
status == "SOLD"
err != nil
```

### 3. **Call Stack** (Shows function call chain)
```
CreateBooking → handler.go:45
ServeHTTP → http/server.go:2123
Handle → mux.go:86
```

### 4. **Debug Console** (Evaluate expressions)
Type Go code at bottom panel:
```go
> booking.EventID
"1"

> len(bookedSeats)
0

> fmt.Sprintf("Status: %s", status)
"Status: SOLD"
```

---

## 📍 Strategic Breakpoint Locations

### 🎯 **Level 1: Request Handler** (Start here!)

**File:** `internal/booking/handler.go`

```go
func (h *BookingHandler) CreateBooking(w http.ResponseWriter, r *http.Request) {
    var req CreateBookingRequest
    
    // 🔴 BREAKPOINT 1: Check incoming request
    if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
        // 🔴 BREAKPOINT 2: JSON parsing error
        ...
    }
    
    // 🔴 BREAKPOINT 3: After validation
    if err := validateCreateBookingRequest(&req); err != nil {
        ...
    }
}
```

**Inspect:**
- ✅ `r.Method` - Is it POST?
- ✅ `r.URL.Path` - Correct endpoint?
- ✅ `req.EventID`, `req.SeatIDs` - Parsed correctly?

---

### 🎯 **Level 2: Business Logic**

**File:** `internal/booking/booking_service.go`

```go
func (s *BookingService) BookTickets(ctx context.Context, req *BookTicketsRequest) (*Booking, error) {
    // 🔴 BREAKPOINT 4: Start of booking workflow
    lockKey := fmt.Sprintf("booking:lock:%s:%s", req.EventID, req.Showtime.Format(time.RFC3339))
    
    // 🔴 BREAKPOINT 5: Check lock acquisition
    acquired, err := s.locker.AcquireLock(ctx, lockKey, 30*time.Second)
    if !acquired {
        return nil, fmt.Errorf("could not acquire lock")
    }
    
    // 🔴 BREAKPOINT 6: Transaction started
    tx, err := s.transactionManager.BeginTransaction(ctx)
    
    // 🔴 BREAKPOINT 7: Event validation
    event, err := s.eventClient.GetEvent(ctx, req.EventID)
    
    // 🔴 BREAKPOINT 8: Check seat availability
    bookedSeats, err := s.repository.CheckSeatsAvailability(ctx, tx, req.EventID, req.Showtime, req.SeatIDs)
    
    // 🔴 BREAKPOINT 9: Seats already booked?
    if len(bookedSeats) > 0 {
        return nil, fmt.Errorf("seats already booked: %v", bookedSeats)
    }
}
```

**Inspect:**
- ✅ `acquired` - Did we get the lock?
- ✅ `event` - Event data from event-api
- ✅ `bookedSeats` - Which seats are taken?

---

### 🎯 **Level 3: Database Operations**

**File:** `internal/booking/repository.go`

```go
func (r *PostgresBookingRepository) CreateBooking(ctx context.Context, tx Transaction, booking *Booking) error {
    // 🔴 BREAKPOINT 10: Before insert
    if booking.BookingID == "" {
        booking.BookingID = generateBookingID()
    }
    
    // 🔴 BREAKPOINT 11: Execute INSERT
    err = sqlTx.QueryRowContext(ctx, query, ...).Scan(...)
    if err != nil {
        // 🔴 BREAKPOINT 12: Database error
        return fmt.Errorf("failed to insert booking: %w", err)
    }
    
    // 🔴 BREAKPOINT 13: Insert seats
    for _, seatID := range booking.SeatIDs {
        _, err := sqlTx.ExecContext(ctx, seatQuery, booking.BookingID, seatID, now)
    }
}
```

**Inspect:**
- ✅ `booking.BookingID` - Generated correctly?
- ✅ `err` - Any SQL errors?
- ✅ `seatID` - Which seat is being processed?

---

### 🎯 **Level 4: Seat Status Update** (Where the bug was!)

**File:** `internal/booking/repository.go`

```go
func (r *PostgresBookingRepository) UpdateEventSeatsStatus(...) error {
    // 🔴 BREAKPOINT 14: Before SQL execution
    if status == "SOLD" {
        // 🔴 BREAKPOINT 15: SOLD path
        updateQuery = `... sold_at = NOW() ...`
    } else {
        // 🔴 BREAKPOINT 16: Other status path
        updateQuery = `... (no sold_at) ...`
    }
    
    // 🔴 BREAKPOINT 17: Execute UPDATE
    result, err := sqlTx.ExecContext(ctx, updateQuery, args...)
    if err != nil {
        // 🔴 BREAKPOINT 18: SQL error (type inconsistency was here!)
        return fmt.Errorf("failed to update event seats: %w", err)
    }
    
    // 🔴 BREAKPOINT 19: Check rows affected
    rowsAffected, err := result.RowsAffected()
}
```

**Inspect:**
- ✅ `status` - What status are we setting?
- ✅ `updateQuery` - Which SQL path?
- ✅ `rowsAffected` - How many seats updated?

---

## 🧪 Debug Scenarios

### Scenario 1: **Test Complete Booking Flow**

**Setup:**
```go
// Set breakpoints at:
// 1. handler.go:45 (request received)
// 2. booking_service.go:80 (acquire lock)
// 3. repository.go:340 (check availability)
// 4. repository.go:360 (update seats)
```

**Steps:**
1. Start debug session (`F5`)
2. Send booking request from `client.http`
3. Press `F10` to step through each line
4. Watch Variables panel update
5. Press `F5` to continue to next breakpoint

**Expected Flow:**
```
🔴 Handler → Validate Input
   ↓ F5 (Continue)
🔴 Service → Acquire Lock ✅
   ↓ F5
🔴 Repository → Check Availability → []empty
   ↓ F5
🔴 Repository → Update Seats → 2 rows affected
   ↓ F5
✅ Booking Created!
```

---

### Scenario 2: **Debug Seat Already Booked**

**Setup:**
```go
// Set breakpoint at:
// booking_service.go after CheckSeatsAvailability
```

**Steps:**
1. Send first booking: A1, A2 → Success
2. Send second booking: A1, A2 again
3. Pause at breakpoint
4. **Inspect:** `bookedSeats = ["A1", "A2"]`
5. Step through to see error handling

---

### Scenario 3: **Debug Type Inconsistency Error** (The bug we fixed!)

**Setup:**
```go
// Set breakpoint at:
// repository.go:365 (if status == "SOLD")
```

**Steps:**
1. Send booking with status "SOLD"
2. Pause at breakpoint
3. **Inspect:** `status = "SOLD"`
4. **Inspect:** `updateQuery` → Should use separate query (not CASE)
5. Step over SQL execution
6. **Verify:** No error! (Previously would error here)

---

## 🎓 Advanced Features

### 1. **Conditional Breakpoints**
Right-click breakpoint → "Edit Breakpoint" → "Expression"

Examples:
```go
seatID == "A1"              // Break only for seat A1
err != nil                   // Break only on error
len(bookedSeats) > 0        // Break when seats are booked
status == "SOLD"            // Break only for SOLD status
req.Quantity > 5            // Break for large bookings
```

### 2. **Logpoints** (Print without stopping)
Right-click gutter → "Add Logpoint"

Examples:
```
Processing seat: {seatID}
Booking ID: {booking.BookingID}, Status: {booking.Status}
Error: {err}
Seats booked: {len(bookedSeats)}
```

### 3. **Hit Count**
Right-click breakpoint → "Edit Breakpoint" → "Hit Count"

Examples:
```
= 3          // Break on 3rd hit
> 5          // Break after 5 hits
% 10         // Break every 10th hit
```

### 4. **Hover Inspection**
Hover over any variable in code to see its value (when paused)

### 5. **Copy Value**
Right-click variable in Variables panel → "Copy Value"

---

## 🚨 Common Debug Scenarios

### ❌ Error: "column sold_at does not exist"

**Set breakpoint:** `repository.go:320` (check table exists)

**Inspect:**
```go
tableExists  // Should be true
```

**If false:** Run `make db-migrate-timestamps`

---

### ❌ Error: "inconsistent types for parameter $1"

**Set breakpoint:** `repository.go:365`

**Inspect:**
```go
status           // Check value
updateQuery      // Should be separate query for SOLD vs others
```

**Verify:** Using `if status == "SOLD"` not `CASE WHEN $1`

---

### ❌ Error: "seats not available"

**Set breakpoint:** After `CheckSeatsAvailability`

**Inspect:**
```go
bookedSeats      // ["A1", "A2"]
req.SeatIDs      // ["A1", "A2"]
```

**Cause:** Seats already booked by another booking

---

### ❌ Error: "event not found"

**Set breakpoint:** After `s.eventClient.GetEvent`

**Inspect:**
```go
event            // nil
err              // "404 Not Found"
req.EventID      // "non-existent"
```

**Cause:** Event doesn't exist in event-api

---

## 🔧 Debug Configuration

Your configuration (already added to `.vscode/launch.json`):

```json
{
  "name": "Debug Booking API",
  "type": "go",
  "request": "launch",
  "mode": "debug",
  "program": "${workspaceFolder}/booking-api/cmd/api",
  "cwd": "${workspaceFolder}/booking-api",
  "env": {
    "DB_HOST": "localhost",
    "DB_PORT": "5432",
    "DB_USER": "postgres",
    "DB_PASSWORD": "postgres",
    "DB_NAME": "booking_db",
    "REDIS_HOST": "localhost",
    "REDIS_PORT": "6379",
    "EVENT_API_URL": "http://localhost:8080",
    "SERVER_PORT": "8081"
  }
}
```

**Important Notes:**
- ⚠️ `program` points to the **directory** `cmd/api`, not `cmd/api/main.go`
- This is required because the package has multiple files (`main.go`, `database.go`)
- Go debugger needs to build the entire package, not just one file

**Customize:**
- Change `SERVER_PORT` to run on different port
- Add more environment variables as needed

---

## 📝 Complete Example Walkthrough

### Goal: Debug a complete booking request

**1. Set Breakpoints:**
```go
// handler.go:45
var req CreateBookingRequest  // 👈 BREAKPOINT

// booking_service.go:80  
acquired, err := s.locker.AcquireLock(...)  // 👈 BREAKPOINT

// repository.go:340
bookedSeats, err := s.repository.CheckSeatsAvailability(...)  // 👈 BREAKPOINT

// repository.go:365
if status == "SOLD" {  // 👈 BREAKPOINT
```

**2. Start Debug:**
- Press `F5`
- Select "Debug Booking API"
- Wait for "Starting booking-api server on port 8081"

**3. Send Request:**
```http
POST http://localhost:8081/api/v1/bookings
{
  "eventId": "1",
  "userId": "user-123",
  "showtime": "2025-10-05T02:00:00Z",
  "quantity": 2,
  "seatIds": ["A1", "A3"]
}
```

**4. Debug Flow:**

```
🔴 BREAKPOINT 1: handler.go:45
   Variables Panel Shows:
   ├─ r.Method: "POST"
   ├─ r.URL.Path: "/api/v1/bookings"
   └─ req
      ├─ eventId: "1"
      ├─ userId: "user-123"
      ├─ quantity: 2
      └─ seatIds: ["A1", "A3"]
   
   Actions:
   - Press F10 (Step Over) to validate input
   - Verify req is parsed correctly
   - Press F5 (Continue)

🔴 BREAKPOINT 2: booking_service.go:80
   Variables Panel Shows:
   ├─ lockKey: "booking:lock:1:2025-10-05T02:00:00Z"
   ├─ acquired: true ✅
   └─ err: <nil>
   
   Actions:
   - Press F11 (Step Into) to see lock implementation
   - Or F5 (Continue)

🔴 BREAKPOINT 3: repository.go:340
   Variables Panel Shows:
   ├─ eventID: "1"
   ├─ seatIDs: ["A1", "A3"]
   ├─ bookedSeats: [] (empty - seats available! ✅)
   └─ err: <nil>
   
   Actions:
   - If bookedSeats is empty → seats available
   - Press F5 (Continue)

🔴 BREAKPOINT 4: repository.go:365
   Variables Panel Shows:
   ├─ status: "SOLD"
   ├─ updateQuery: "UPDATE event_seats SET ... sold_at = NOW() ..."
   └─ args: ["SOLD", "BK-xxx", "1", "2025-10-05...", ["A1","A3"]]
   
   Actions:
   - Press F10 (Step Over) to execute SQL
   - Check Variables: rowsAffected: 2 ✅
   - Press F5 (Continue to completion)
```

**5. Verify Success:**
```bash
make db-status
# A1 | SOLD | BK-xxx | 2025-10-06 13:xx:xx ✅
# A3 | SOLD | BK-xxx | 2025-10-06 13:xx:xx ✅
```

---

## 🎉 You're Ready!

### Quick Reference Card:

| Task | Action |
|------|--------|
| **Start Debug** | Press `F5` → Select "Debug Booking API" |
| **Set Breakpoint** | Click in gutter (left of line numbers) |
| **Remove Breakpoint** | Click red dot again |
| **Continue** | `F5` |
| **Step Over** | `F10` |
| **Step Into** | `F11` |
| **Stop Debug** | `Shift+F5` |

### Files to Set Breakpoints:
- ✅ `internal/booking/handler.go` - Request entry
- ✅ `internal/booking/booking_service.go` - Business logic
- ✅ `internal/booking/repository.go` - Database operations

### What to Inspect:
- ✅ Request data (`req.EventID`, `req.SeatIDs`)
- ✅ Lock acquisition (`acquired`)
- ✅ Seat availability (`bookedSeats`)
- ✅ SQL queries (`updateQuery`, `args`)
- ✅ Errors (`err`)

---

**Happy Debugging!** 🐛🔍✨

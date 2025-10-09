# 🎯 Quick Start: Debug Booking API with Breakpoints

## 3 Simple Steps

### Step 1: Set a Breakpoint 🔴
1. Open `booking-api/internal/booking/handler.go`
2. Find line ~45: `var req CreateBookingRequest`
3. **Click in the left gutter** (left of the line number)
4. A **red dot 🔴** appears = Breakpoint set!

```go
func (h *BookingHandler) CreateBooking(w http.ResponseWriter, r *http.Request) {
🔴  var req CreateBookingRequest  // 👈 Click here in the gutter!
    
    if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
        ...
    }
}
```

### Step 2: Start Debug Session 🚀
**Option A:** Keyboard shortcut
- Press **`F5`**
- Select **"Debug Booking API"** from the menu

**Option B:** UI
1. Click **"Run and Debug"** icon in left sidebar (▶️🐛)
2. Select **"Debug Booking API"** from dropdown
3. Click green **play button ▶️**

You'll see in terminal:
```
Starting booking-api server on port 8081
```

### Step 3: Send Request 📨
1. Open `booking-api/client.http`
2. Find the POST request:
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
3. Click **"Send Request"** above the POST line

**🎉 VS Code pauses at your breakpoint!**

---

## What You'll See When Paused

### Debug Toolbar (top of screen)
```
▶️ Continue | ⤵️ Step Over | ⬇️ Step Into | ⬆️ Step Out | 🔄 Restart | ⏹️ Stop
```

### Variables Panel (left sidebar)
```
▼ Local
  ▼ req
    eventId: "1"
    userId: "user-123"
    quantity: 2
    ▼ seatIds
      [0]: "A1"
      [1]: "A3"
  err: <nil>
  w: http.ResponseWriter
  r: *http.Request
```

### Current Line (highlighted in yellow)
```go
→   var req CreateBookingRequest  // ← You are here!
    
    if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
```

---

## Basic Controls

| Key | Action | What It Does |
|-----|--------|--------------|
| `F5` | **Continue** | Run until next breakpoint |
| `F10` | **Step Over** | Execute current line, move to next |
| `F11` | **Step Into** | Go inside function calls |
| `Shift+F11` | **Step Out** | Exit current function |
| `Shift+F5` | **Stop** | Stop debugging |

---

## Try It Now!

### 1. Set breakpoint in handler.go (line ~45)
### 2. Press F5 → Select "Debug Booking API"
### 3. Send POST request from client.http
### 4. VS Code pauses! ✅
### 5. Press F10 to step through code line by line
### 6. Watch Variables panel update with each step

---

## More Breakpoint Locations

Add more breakpoints to follow the complete flow:

```go
// File: internal/booking/handler.go
var req CreateBookingRequest  // 🔴 Request entry

// File: internal/booking/booking_service.go
acquired, err := s.locker.AcquireLock(...)  // 🔴 Lock check

// File: internal/booking/repository.go
bookedSeats, err := s.repository.CheckSeatsAvailability(...)  // 🔴 Seat check

// File: internal/booking/repository.go
if status == "SOLD" {  // 🔴 Update seats
```

---

## Tips

✅ **Add Multiple Breakpoints** - Click multiple gutters to set multiple breakpoints
✅ **Remove Breakpoint** - Click the red dot again to remove it
✅ **Hover Variables** - Hover over any variable to see its value
✅ **Debug Console** - Type expressions at the bottom to evaluate them

---

## Full Documentation

For detailed guide with examples and scenarios:
📖 See `VSCODE_DEBUG_GUIDE.md`

---

**Ready to debug!** 🐛🔍

Press `F5` and start exploring! 🚀

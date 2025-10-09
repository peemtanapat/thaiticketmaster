# 🔧 Debug Troubleshooting - Booking API

Common errors when debugging and how to fix them.

---

## ❌ Error: "undefined: ensureDatabaseExists" / "undefined: createBookingSchema"

### Full Error Message:
```
Build Error: go build -o /path/__debug_bin -gcflags all=-N -l ./main.go
# command-line-arguments
./main.go:35:12: undefined: ensureDatabaseExists
./main.go:57:12: undefined: createBookingSchema (exit status 1)
```

### 🔍 Root Cause:
The debugger is trying to build only `main.go` file instead of the entire package. Since `ensureDatabaseExists` and `createBookingSchema` are defined in `database.go`, they're not found when building only `main.go`.

### ✅ Solution:
In `.vscode/launch.json`, the `program` path should point to the **directory**, not the file:

**❌ Wrong:**
```json
"program": "${workspaceFolder}/booking-api/cmd/api/main.go"
```

**✅ Correct:**
```json
"program": "${workspaceFolder}/booking-api/cmd/api"
```

### Why?
When you have multiple Go files in a package (`main.go`, `database.go`, etc.), the debugger needs to build the entire package directory, not individual files.

---

## ❌ Error: "could not launch process: decoding dwarf section info"

### Solution:
This usually happens when the debug binary is corrupted.

1. **Clean debug binaries:**
```bash
cd booking-api
rm -rf cmd/api/__debug_bin*
```

2. **Restart VS Code** (sometimes needed)

3. **Try debugging again** (`F5`)

---

## ❌ Error: "Failed to continue: Check the debug console for details"

### Solution 1: Check Port Conflicts
Another instance might be running on port 8081:

```bash
# Kill existing process
pkill -f "booking-api"

# Or find and kill by port
lsof -ti:8081 | xargs kill -9
```

### Solution 2: Check Database Connection
Ensure PostgreSQL is running:

```bash
# Check if PostgreSQL is running
psql -h localhost -p 5432 -U admin -d booking_db -c "SELECT 1;"

# If not running, start it:
# (depends on your installation method)
brew services start postgresql@14
```

### Solution 3: Check Redis Connection
Ensure Redis is running:

```bash
# Test Redis connection
redis-cli ping
# Should return: PONG

# If not running:
brew services start redis
```

---

## ❌ Error: "Breakpoint set but never triggered"

### Possible Causes & Solutions:

#### 1. **Code Not Reached**
- Request didn't reach that code path
- Check if endpoint was called correctly
- Verify request URL and method

#### 2. **Optimization Stripped Code**
Debug configuration should have:
```json
"mode": "debug"  // NOT "exec" or "test"
```

#### 3. **Breakpoint in Wrong File**
- Make sure you're editing files in `booking-api/` directory
- Not files in `vendor/` or other dependencies

#### 4. **File Not Saved**
- Save the file (`Cmd+S` / `Ctrl+S`) before setting breakpoint
- Red dot should appear solid (not hollow)

---

## ❌ Error: "listen tcp :8081: bind: address already in use"

### Solution:
Port 8081 is already in use. Stop the other process:

```bash
# Find process using port 8081
lsof -ti:8081

# Kill it
lsof -ti:8081 | xargs kill -9

# Or use make command
cd booking-api
make kill
```

Then try debugging again (`F5`).

---

## ❌ Debugger Stops at Wrong Location

### Possible Causes:

#### 1. **Stale Debug Binary**
Clean and rebuild:
```bash
cd booking-api
rm -rf cmd/api/__debug_bin*
make clean
make build
```

Then start debug session again.

#### 2. **Multiple Breakpoints**
Check Debug panel (left sidebar) → Breakpoints section
- Remove unwanted breakpoints
- Click red dot to toggle off

#### 3. **Code Changed Since Build**
- Stop debugger (`Shift+F5`)
- Save all files (`Cmd+K S` / `Ctrl+K S`)
- Restart debugger (`F5`)

---

## ❌ Variables Panel Shows "Optimized Out"

### Cause:
Variables optimized away by compiler.

### Solution:
Ensure debug configuration has:
```json
"mode": "debug",
"buildFlags": "-gcflags=all=-N -l"  // Disable optimizations
```

Already configured in "Debug Booking API"! ✅

---

## ❌ "Cannot find package" errors

### Solution:
Go modules not synced. Run:

```bash
cd booking-api
go mod tidy
go mod download
```

Then restart VS Code and try debugging again.

---

## ❌ Debug Console Shows Nothing

### Solutions:

#### 1. **Enable Verbose Logging**
In `.vscode/launch.json`, ensure:
```json
"showLog": true,
"trace": "verbose"
```

#### 2. **Check Output Panel**
- View → Output (or `Cmd+Shift+U` / `Ctrl+Shift+U`)
- Select "Go" from dropdown
- Look for error messages

#### 3. **Check Debug Console**
- View → Debug Console (or `Cmd+Shift+Y` / `Ctrl+Shift+Y`)
- Should show debugger output

---

## ❌ Event API Integration Fails

### Error in Variables:
```
event: <nil>
err: "Get http://localhost:8080/api/v1/events/1: connection refused"
```

### Solution:
Event API is not running. Start it:

**Option 1: Start in VS Code**
1. Press `F5`
2. Select "EventApiApplication"

**Option 2: Start in Terminal**
```bash
cd event-api
./mvnw spring-boot:run
```

Verify it's running:
```bash
curl http://localhost:8080/health
# Should return 200 OK
```

---

## ✅ Verify Debug Setup

### Quick Checklist:

```bash
# 1. Check launch.json is correct
cat .vscode/launch.json | grep "Debug Booking API" -A 15

# Should show:
# "program": "${workspaceFolder}/booking-api/cmd/api"  ✅

# 2. Check dependencies are running
psql -h localhost -p 5432 -U admin -d booking_db -c "SELECT 1;"  # PostgreSQL ✅
redis-cli ping  # Should return PONG ✅
curl http://localhost:8080/health  # Event API ✅

# 3. Check no process on port 8081
lsof -ti:8081  # Should return empty ✅

# 4. Build successfully
cd booking-api
go build ./cmd/api  # Should succeed ✅
```

All checks passed? Try `F5` again! 🚀

---

## 🆘 Still Not Working?

### Nuclear Option: Fresh Start

```bash
# 1. Kill all processes
pkill -f "booking-api"
pkill -f "event-api"

# 2. Clean everything
cd booking-api
make clean
rm -rf cmd/api/__debug_bin*

# 3. Rebuild
make build

# 4. Restart VS Code
# Close VS Code completely
# Open it again

# 5. Try debugging
# Press F5 → Select "Debug Booking API"
```

---

## 📚 Additional Resources

- **Quick Start:** `QUICK_DEBUG.md`
- **Full Guide:** `VSCODE_DEBUG_GUIDE.md`
- **Launch Config:** `.vscode/launch.json`

---

**Most Common Fix:** ⚠️
Make sure `program` in launch.json points to **`cmd/api`** (directory), not **`cmd/api/main.go`** (file)!

This fixes 90% of debug issues! 🎯

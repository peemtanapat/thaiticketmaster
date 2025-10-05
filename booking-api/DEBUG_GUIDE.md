# Debug Mode Guide for Booking-API

## Quick Start

### Option 1: Simple Verbose Logging (Easiest)
```bash
make run-debug
```
This runs the application with verbose logging enabled via `LOG_LEVEL=debug`.

### Option 2: Delve Debugger (Full Debugging)

#### Install Delve (first time only):
```bash
make install-delve
```

#### Start debug server:
```bash
make debug
```

This will:
- Start the application with Delve debugger
- Listen on port 2345 for debugger connections
- Wait for a debugger to attach before running

## Debugging Options

### 1. Command Line Debug Mode

Set environment variables for more verbose output:

```bash
LOG_LEVEL=debug \
DB_HOST=localhost \
DB_PORT=5432 \
DB_USER=admin \
DB_PASSWORD=admin \
DB_NAME=booking_db \
REDIS_HOST=localhost \
REDIS_PORT=6379 \
EVENT_API_URL=http://localhost:8080 \
SERVER_PORT=8081 \
go run ./cmd/api
```

### 2. VS Code Debug Configuration

Create or update `.vscode/launch.json`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "Debug Booking API",
      "type": "go",
      "request": "launch",
      "mode": "debug",
      "program": "${workspaceFolder}/booking-api/cmd/api",
      "env": {
        "LOG_LEVEL": "debug",
        "DB_HOST": "localhost",
        "DB_PORT": "5432",
        "DB_USER": "admin",
        "DB_PASSWORD": "admin",
        "DB_NAME": "booking_db",
        "REDIS_HOST": "localhost",
        "REDIS_PORT": "6379",
        "EVENT_API_URL": "http://localhost:8080",
        "SERVER_PORT": "8081"
      },
      "showLog": true
    },
    {
      "name": "Attach to Debug Server",
      "type": "go",
      "request": "attach",
      "mode": "remote",
      "remotePath": "${workspaceFolder}/booking-api",
      "port": 2345,
      "host": "localhost",
      "showLog": true
    },
    {
      "name": "Debug Current Test",
      "type": "go",
      "request": "launch",
      "mode": "test",
      "program": "${fileDirname}",
      "showLog": true
    }
  ]
}
```

**Usage:**
1. Press `F5` or click "Run and Debug" in VS Code
2. Select "Debug Booking API"
3. Set breakpoints by clicking left of line numbers
4. Use debug toolbar to step through code

### 3. GoLand / IntelliJ IDEA

1. Right-click on `cmd/api/main.go`
2. Select "Debug 'go build main.go'"
3. Or create a Run Configuration:
   - Go to Run → Edit Configurations
   - Add "Go Build"
   - Set:
     - Package path: `./cmd/api`
     - Working directory: `booking-api`
     - Environment: Add all required env vars

### 4. Attach Debugger to Running Process

Start debug server:
```bash
make debug
```

Then in VS Code, use "Attach to Debug Server" configuration, or connect manually:
```bash
dlv connect localhost:2345
```

### 5. Debug Tests

Debug specific test file:
```bash
dlv test ./internal/booking -- -test.run TestBookTickets_Success
```

Or with Makefile:
```bash
make debug-test
```

## Makefile Commands

```bash
make help              # Show all available commands
make run               # Run normally
make run-debug         # Run with debug logging
make debug             # Run with Delve debugger (port 2345)
make debug-test        # Debug tests with Delve
make install-delve     # Install Delve debugger
```

## Environment Variables for Debugging

You can set these before running:

```bash
# Database
export DB_HOST=localhost
export DB_PORT=5432
export DB_USER=admin
export DB_PASSWORD=admin
export DB_NAME=booking_db

# Redis
export REDIS_HOST=localhost
export REDIS_PORT=6379

# Services
export EVENT_API_URL=http://localhost:8080
export SERVER_PORT=8081

# Logging
export LOG_LEVEL=debug    # debug, info, warn, error
```

Then run:
```bash
make run
```

## Debugging Tips

### 1. Add Temporary Debug Logs

In your code:
```go
import "log"

log.Printf("DEBUG: Variable value: %+v", myVariable)
log.Printf("DEBUG: Request: %s %s", r.Method, r.URL.Path)
```

### 2. Pretty Print JSON

```go
import "encoding/json"

data, _ := json.MarshalIndent(myStruct, "", "  ")
log.Printf("DEBUG: %s", string(data))
```

### 3. Check Database Queries

Connect to PostgreSQL:
```bash
psql -h localhost -U admin -d booking_db
```

View tables:
```sql
\dt
SELECT * FROM bookings;
SELECT * FROM booking_seats;
```

### 4. Check Redis

Connect to Redis:
```bash
redis-cli -h localhost -p 6379
```

View all keys:
```
KEYS *
GET booking:lock:event-123:2025-07-15T19:00:00Z
```

### 5. Test API with curl

```bash
# Health check
curl http://localhost:8081/health

# Create booking (verbose)
curl -v -X POST http://localhost:8081/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "1",
    "userId": "user-123",
    "showtime": "2025-07-15T19:00:00",
    "quantity": 2,
    "seatIds": ["A1", "A2"]
  }'
```

### 6. Monitor Logs in Real-time

If running in another terminal:
```bash
# Follow application logs
tail -f /path/to/logfile

# Or use journalctl if running as service
journalctl -u booking-api -f
```

## Delve Commands

When connected to Delve debugger:

```
break main.go:50       # Set breakpoint at line 50
break BookTickets      # Set breakpoint at function
continue (or c)        # Continue execution
next (or n)           # Step over
step (or s)           # Step into
print myVar           # Print variable value
locals                # Show all local variables
goroutines            # List all goroutines
help                  # Show all commands
quit                  # Exit debugger
```

## Common Debug Scenarios

### Scenario 1: Debug Request Handler

1. Set breakpoint in `internal/booking/handler.go` at `CreateBooking` function
2. Run with debugger
3. Send request with `client.http` or curl
4. Step through code to see request processing

### Scenario 2: Debug Event API Integration

1. Set breakpoint in `internal/booking/event_client.go` at `GetEvent` function
2. Run with debugger
3. Create booking request
4. See how event data is fetched and parsed

### Scenario 3: Debug Validation Logic

1. Set breakpoint in `internal/booking/booking_service.go` at `validateShowtimeInEvent`
2. Run with debugger
3. Create booking with specific showtime
4. Step through to see validation logic

### Scenario 4: Debug Database Operations

1. Set breakpoint in `internal/booking/transaction.go`
2. Run with debugger
3. Create booking
4. See transaction begin/commit/rollback

### Scenario 5: Debug Redis Locking

1. Set breakpoint in `internal/booking/redis_locker.go`
2. Run with debugger
3. Create concurrent bookings
4. See lock acquisition and release

## Remote Debugging (Production)

**⚠️ WARNING: Only for testing/staging, not production!**

1. Build with debug symbols:
```bash
go build -gcflags="all=-N -l" -o bin/booking-api ./cmd/api
```

2. Run with Delve on remote server:
```bash
dlv exec ./bin/booking-api --headless --listen=:2345 --api-version=2
```

3. Forward port via SSH:
```bash
ssh -L 2345:localhost:2345 user@remote-server
```

4. Attach local debugger to `localhost:2345`

## Performance Profiling

### CPU Profile
```bash
go run ./cmd/api -cpuprofile=cpu.prof
go tool pprof cpu.prof
```

### Memory Profile
```bash
go run ./cmd/api -memprofile=mem.prof
go tool pprof mem.prof
```

### Live Profiling
Add to main.go:
```go
import _ "net/http/pprof"

go func() {
    log.Println(http.ListenAndServe("localhost:6060", nil))
}()
```

Then access:
- http://localhost:6060/debug/pprof/
- http://localhost:6060/debug/pprof/goroutine
- http://localhost:6060/debug/pprof/heap

## Troubleshooting

### "dlv: command not found"
```bash
make install-delve
# Or manually:
go install github.com/go-delve/delve/cmd/dlv@latest
```

### Can't connect to debugger
- Check port 2345 is not in use: `lsof -i :2345`
- Check firewall settings
- Try restarting debug server

### Breakpoints not hitting
- Ensure you're running debug build, not release
- Check file paths match
- Rebuild: `make clean && make build`

### Application too slow in debug mode
- This is normal - debugger adds overhead
- Use `continue` to skip to breakpoints
- Consider logging instead of stepping

## Additional Resources

- [Delve Documentation](https://github.com/go-delve/delve/tree/master/Documentation)
- [VS Code Go Debugging](https://github.com/golang/vscode-go/wiki/debugging)
- [GoLand Debugging](https://www.jetbrains.com/help/go/debugging-code.html)

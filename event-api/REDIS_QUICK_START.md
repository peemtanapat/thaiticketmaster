# Redis Cache Integration - Quick Start Guide

## Prerequisites
- Redis is deployed in Kubernetes (`event-redis` namespace)
- Event API is configured with Redis connection
- PostgreSQL database is running

## Setup and Testing

### 1. Start Redis Port Forward (for local development)
```bash
kubectl port-forward -n event-redis svc/redis 6379:6379
```

### 2. Start the Event API
```bash
cd event-api
./mvnw spring-boot:run
```

### 3. Monitor Redis Activity
Open a new terminal and run:
```bash
redis-cli MONITOR
```

## Testing Write-Through Cache

### Test 1: Create Event (Write-Through)
```bash
# Create a new event
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Concert",
    "categoryId": 1,
    "showDateTimes": ["2025-12-31T20:00:00"],
    "location": "Bangkok Arena",
    "onSaleDateTime": "2025-10-15T09:00:00",
    "ticketPrice": 1500.00,
    "detail": "Amazing concert",
    "condition": "18+ only",
    "eventStatus": "ON_SALE",
    "gateOpen": "1 hour before"
  }'
```

**Expected Behavior:**
1. Event saved to PostgreSQL
2. Event cached in Redis with key `event:{id}`
3. List caches (`events:all`, `events:onsale`) invalidated
4. You should see `SET event:1` in Redis MONITOR

### Test 2: Read Event (Cache Hit)
```bash
# First call - Cache miss, loads from DB
curl http://localhost:8080/api/events/1

# Second call - Cache hit, faster response
curl http://localhost:8080/api/events/1
```

**Expected Behavior:**
- First call: Query PostgreSQL → Cache result → Return (slower)
- Second call: Read from Redis → Return (faster)
- See `GET event:1` in Redis MONITOR

### Test 3: Update Event (Write-Through)
```bash
# Update the event
curl -X PUT http://localhost:8080/api/events/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Concert Name",
    "ticketPrice": 1800.00
  }'
```

**Expected Behavior:**
1. Event updated in PostgreSQL
2. Cache updated with new data (key `event:1`)
3. List caches invalidated
4. See `SET event:1` in Redis MONITOR

### Test 4: Delete Event (Write-Through)
```bash
# Delete the event
curl -X DELETE http://localhost:8080/api/events/1
```

**Expected Behavior:**
1. Event deleted from PostgreSQL
2. Cache evicted (key `event:1` removed)
3. List caches invalidated
4. See `DEL event:1` in Redis MONITOR

### Test 5: List Events (Cache List)
```bash
# First call - loads from DB and caches
curl http://localhost:8080/api/events

# Second call - served from cache
curl http://localhost:8080/api/events
```

**Expected Behavior:**
- First call: Query all from PostgreSQL → Cache → Return
- Second call: Return from cache
- See `GET events:all` in Redis MONITOR

### Test 6: On-Sale Events (Time-Sensitive Cache)
```bash
# Get events currently on sale
curl http://localhost:8080/api/events/on-sale
```

**Expected Behavior:**
- Cached for only 15 minutes (shorter TTL)
- Automatically expires after 15 minutes
- See `GET events:onsale` in Redis MONITOR

## Verify Cache in Redis CLI

```bash
# Connect to Redis
redis-cli

# List all cached keys
KEYS *

# Check specific event cache
GET event:1

# Check list caches
GET events:all
GET events:onsale

# Check TTL (time to live)
TTL event:1
TTL events:all
TTL events:onsale

# Verify cache content (pretty print)
GET event:1 | python -m json.tool

# Clear all cache (for testing)
FLUSHALL

# Get cache statistics
INFO stats
```

## Performance Comparison

### Without Cache (Direct DB Query)
```bash
time curl http://localhost:8080/api/events/1
# Expected: ~50-100ms (database query time)
```

### With Cache (Redis)
```bash
# First call (cache miss)
time curl http://localhost:8080/api/events/1

# Second call (cache hit)
time curl http://localhost:8080/api/events/1
# Expected: ~5-20ms (memory access time)
```

## Cache Invalidation Test

```bash
# 1. Get event (cache it)
curl http://localhost:8080/api/events/1

# 2. Verify it's cached
redis-cli GET event:1

# 3. Update event (should update cache)
curl -X PUT http://localhost:8080/api/events/1 \
  -H "Content-Type: application/json" \
  -d '{"ticketPrice": 2000.00}'

# 4. Get event again (should return updated data from cache)
curl http://localhost:8080/api/events/1

# 5. Verify cache has updated data
redis-cli GET event:1
```

## Troubleshooting

### Redis Connection Failed
```bash
# Check if Redis is running
kubectl get pods -n event-redis

# Check Redis logs
kubectl logs -n event-redis deployment/redis

# Verify port forward is active
netstat -an | grep 6379
```

### Cache Not Working
```bash
# Check application logs
tail -f event-api/logs/spring.log

# Verify Redis configuration
redis-cli PING
# Should return: PONG

# Check cache statistics
redis-cli INFO stats
```

### Clear Cache for Testing
```bash
# Clear all cache
redis-cli FLUSHALL

# Clear specific keys
redis-cli DEL event:1
redis-cli DEL events:all
redis-cli DEL events:onsale
```

## Kubernetes Deployment

When deploying to Kubernetes, the application will automatically connect to:
```
redis.event-redis.svc.cluster.local:6379
```

No need for port forwarding in Kubernetes environment.

## Next Steps

1. ✅ Redis caching is now integrated with write-through strategy
2. Monitor cache hit/miss ratios in production
3. Consider adding cache warming for popular events
4. Implement cache metrics using Spring Boot Actuator
5. Setup Redis high availability (Sentinel/Cluster) for production

## Performance Metrics to Monitor

- **Cache Hit Ratio**: Percentage of requests served from cache
- **Average Response Time**: Compare with/without cache
- **Redis Memory Usage**: Monitor memory consumption
- **Cache Eviction Rate**: How often keys are evicted
- **Database Load**: Should decrease with effective caching

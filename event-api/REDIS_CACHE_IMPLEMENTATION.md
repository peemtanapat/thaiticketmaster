# Redis Cache Implementation - Write-Through Strategy

## Overview
This document describes the Redis caching implementation for the Event API using a **write-through caching strategy**.

## What is Write-Through Caching?

Write-through caching is a caching strategy where:
1. **On READ**: Check cache first. If data exists (cache hit), return it. If not (cache miss), fetch from database and store in cache before returning.
2. **On WRITE (Create/Update)**: Write to database first, then update the cache with the new data.
3. **On DELETE**: Delete from database first, then remove from cache.

This ensures that the cache is always consistent with the database, making it highly reliable for applications that require data accuracy.

## Architecture

```
Client Request
     ↓
EventService
     ↓
  ┌──────────────┐
  │ Redis Cache  │ ← Check first on read
  │ (In-Memory)  │ ← Update on write
  └──────────────┘
     ↓
  ┌──────────────┐
  │ PostgreSQL   │ ← Source of truth
  │  (Database)  │
  └──────────────┘
```

## Implementation Details

### 1. Dependencies Added (`pom.xml`)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>
```

### 2. Configuration

#### Local Environment (`application.yml`)
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
      jedis:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
```

#### Kubernetes Environment (`application-k8s.yml`)
```yaml
spring:
  data:
    redis:
      host: redis.event-redis.svc.cluster.local
      port: 6379
      timeout: 2000ms
      jedis:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
```

### 3. Cache Keys Strategy

| Operation | Cache Key | TTL |
|-----------|-----------|-----|
| Single Event | `event:{id}` | 1 hour |
| All Events | `events:all` | 1 hour |
| On-Sale Events | `events:onsale` | 15 minutes |

### 4. Caching Operations

#### Read Operations
- **getEventById(id)**: Cache-aside pattern
  1. Check Redis with key `event:{id}`
  2. If found → return cached data
  3. If not found → query database, cache result, return data

- **getAllEvents()**: Cache-aside pattern
  1. Check Redis with key `events:all`
  2. If found → return cached list
  3. If not found → query database, cache result, return list

- **getOnSaleEvents()**: Cache-aside pattern with shorter TTL
  1. Check Redis with key `events:onsale`
  2. If found → return cached list
  3. If not found → query database, cache result (15 min TTL), return list

#### Write Operations
- **createEvent(request)**: Write-through pattern
  1. Write to PostgreSQL database
  2. Cache the new event with key `event:{id}`
  3. Invalidate list caches (`events:all`, `events:onsale`)
  4. Return created event

- **updateEvent(id, request)**: Write-through pattern
  1. Update in PostgreSQL database
  2. Update cache with key `event:{id}`
  3. Invalidate list caches (`events:all`, `events:onsale`)
  4. Return updated event

- **deleteEvent(id)**: Write-through pattern
  1. Delete from PostgreSQL database
  2. Remove from cache (key `event:{id}`)
  3. Invalidate list caches (`events:all`, `events:onsale`)

### 5. Serialization

All DTOs and entities are made `Serializable` for Redis storage:
- `Event` entity
- `Category` entity
- `EventDTO` class
- `EventDTO.CategoryDTO` inner class

Redis uses Jackson JSON serialization with support for Java 8 date/time types (`LocalDateTime`).

## Benefits

1. **Improved Performance**: Frequently accessed data is served from memory (Redis) instead of database
2. **Reduced Database Load**: Less queries to PostgreSQL database
3. **Data Consistency**: Write-through ensures cache is always consistent with database
4. **Scalability**: Redis can handle high read throughput
5. **Time-sensitive Caching**: On-sale events have shorter TTL (15 min) to reflect real-time availability

## Cache Invalidation Strategy

### Automatic Invalidation
- List caches (`events:all`, `events:onsale`) are invalidated on any write operation (create/update/delete)
- Individual event caches are invalidated/updated on their specific operations

### TTL-based Expiration
- Single events: 1 hour
- Event lists: 1 hour
- On-sale events: 15 minutes (more frequent updates needed)

## Testing the Implementation

### Local Testing
1. Start Redis: `kubectl port-forward -n event-redis svc/redis 6379:6379`
2. Start Event API
3. Monitor Redis: `redis-cli MONITOR`

### Verify Caching
```bash
# Create an event - should cache it
curl -X POST http://localhost:8080/api/events -H "Content-Type: application/json" -d '{...}'

# Get event by ID - first call: DB query + cache
# Second call: cache hit (faster response)
curl http://localhost:8080/api/events/1

# Check Redis
redis-cli
> KEYS event:*
> GET event:1
```

## Monitoring

### Redis Commands for Monitoring
```bash
# Connect to Redis
redis-cli

# List all keys
KEYS *

# Check specific event
GET event:1

# Check list caches
GET events:all
GET events:onsale

# Check TTL
TTL event:1

# Clear all cache (for testing)
FLUSHALL
```

## Production Considerations

1. **Redis High Availability**: Use Redis Sentinel or Redis Cluster in production
2. **Connection Pooling**: Configured with Jedis pool (max 8 connections)
3. **Cache Warming**: Consider pre-loading popular events on application startup
4. **Cache Metrics**: Monitor cache hit/miss ratios using Spring Boot Actuator
5. **Error Handling**: Application continues to work if Redis is unavailable (falls back to database)

## Future Enhancements

1. **Cache Warming**: Pre-populate cache with popular events on startup
2. **Cache Tags**: Implement cache tags for better invalidation (e.g., by category)
3. **Distributed Caching**: For multi-instance deployments
4. **Cache Statistics**: Add metrics for cache hit/miss rates
5. **Conditional Caching**: Cache only frequently accessed events

## Related Files

- Configuration: `src/main/java/dev/peemtanapat/thaiticketmaster/event_api/config/RedisConfig.java`
- Service: `src/main/java/dev/peemtanapat/thaiticketmaster/event_api/event/EventService.java`
- Entities: `src/main/java/dev/peemtanapat/thaiticketmaster/event_api/event/Event.java`
- DTOs: `src/main/java/dev/peemtanapat/thaiticketmaster/event_api/event/EventDTO.java`

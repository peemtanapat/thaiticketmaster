# Fix: Hibernate Lazy Initialization Exception with Redis Serialization

## Problem
When caching `EventDTO` objects to Redis, the application threw:
```
SerializationException: Could not read JSON: failed to lazily initialize a collection: 
could not initialize proxy - no Session (through reference chain: 
dev.peemtanapat.thaiticketmaster.event_api.event.EventDTO["showDateTimes"])
```

## Root Cause
Even though the `Event.showDateTimes` collection was marked with `@ElementCollection(fetch = FetchType.EAGER)`, the Jackson serialization to Redis was encountering Hibernate proxy objects. When Jackson tried to serialize these objects, the Hibernate session had already closed, causing the lazy initialization exception.

## Solution Applied

### 1. Added Hibernate Jackson Module Dependency
**File**: `pom.xml`
```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-hibernate6</artifactId>
</dependency>
```

This module teaches Jackson how to handle Hibernate-specific objects like lazy-loaded proxies.

### 2. Configured Hibernate6Module in RedisConfig
**File**: `RedisConfig.java`

Added Hibernate6Module to both ObjectMapper instances:
```java
Hibernate6Module hibernate6Module = new Hibernate6Module();
hibernate6Module.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);
hibernate6Module.configure(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true);
objectMapper.registerModule(hibernate6Module);
```

**Configuration Explained:**
- `FORCE_LAZY_LOADING = false`: Don't force loading of lazy collections (prevents unnecessary DB queries)
- `SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS = true`: For unloaded lazy objects, serialize just their ID instead of trying to load them

### 3. Defensive Copy in EventDTO Constructor
**File**: `EventDTO.java`

Changed from direct assignment to creating a new ArrayList:
```java
// Before (potential proxy issue)
this.showDateTimes = event.getShowDateTimes();

// After (defensive copy)
this.showDateTimes = event.getShowDateTimes() != null ? 
    new java.util.ArrayList<>(event.getShowDateTimes()) : new java.util.ArrayList<>();
```

This ensures we're working with a plain ArrayList, not a Hibernate PersistentBag proxy.

## How It Works Now

1. **Within Transaction**: When `EventDTO` is created from `Event`, the EAGER-loaded `showDateTimes` collection is accessed and copied to a plain ArrayList.

2. **Serialization**: When Jackson serializes the DTO to JSON for Redis:
   - The Hibernate6Module detects any Hibernate proxies
   - For already-loaded collections (like showDateTimes), it serializes them normally
   - For unloaded lazy objects (if any), it only serializes their IDs
   - No Hibernate session is needed during serialization

3. **Deserialization**: When reading from Redis:
   - Jackson deserializes the JSON back to plain POJOs
   - No Hibernate proxies are involved
   - Data is returned directly from cache

## Benefits of This Approach

1. **No More Lazy Initialization Errors**: Hibernate6Module handles all Hibernate-specific objects gracefully
2. **Better Performance**: Doesn't force loading of lazy collections unnecessarily
3. **Safer Serialization**: Defensive copy ensures no proxy objects leak into the cache
4. **Clean Separation**: DTOs in cache are plain POJOs, not Hibernate entities

## Testing the Fix

```bash
# Start the application
./mvnw spring-boot:run

# Test creating an event (will cache it)
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Event",
    "categoryId": 1,
    "showDateTimes": ["2025-12-31T20:00:00", "2025-12-31T22:00:00"],
    "location": "Test Location",
    "onSaleDateTime": "2025-10-15T09:00:00",
    "ticketPrice": 1500.00,
    "eventStatus": "ON_SALE"
  }'

# Get the event (should work without errors)
curl http://localhost:8080/api/events/1

# Verify in Redis
redis-cli
> GET event:1
```

## Alternative Solutions Considered

### Option 1: Use @JsonIgnoreProperties (Not Chosen)
```java
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Event { ... }
```
**Why not**: Only ignores specific proxy fields, doesn't solve the core serialization issue.

### Option 2: Use DTO Projection in Repository (Not Chosen)
```java
@Query("SELECT new EventDTO(...) FROM Event e WHERE e.id = :id")
Optional<EventDTO> findEventDTOById(Long id);
```
**Why not**: More complex, requires maintaining parallel query methods.

### Option 3: Use @Transactional with Extended Session (Not Chosen)
```java
@Transactional(readOnly = true)
public EventDTO getEventById(Long id) {
    // Open session in view pattern
}
```
**Why not**: Opens door to N+1 queries, performance issues, and anti-pattern.

### ✅ Option 4: Hibernate6Module + Defensive Copy (CHOSEN)
**Why**: Clean, efficient, handles all cases, best practice for DTO serialization.

## Related Files Modified

1. `pom.xml` - Added jackson-datatype-hibernate6 dependency
2. `RedisConfig.java` - Configured Hibernate6Module for both ObjectMappers
3. `EventDTO.java` - Added defensive copy in constructor

## References

- [Jackson Hibernate Module Documentation](https://github.com/FasterXML/jackson-datatype-hibernate)
- [Spring Data Redis Documentation](https://docs.spring.io/spring-data/redis/reference/)
- [Hibernate Lazy Loading Best Practices](https://vladmihalcea.com/hibernate-facts-the-importance-of-fetch-strategy/)

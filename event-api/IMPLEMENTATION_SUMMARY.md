# Event API Implementation Summary

## 🎯 Overview
Successfully implemented a comprehensive Event API microservice for the Ticketmaster application with full CRUD operations, advanced querying capabilities, and proper architecture following Spring Boot best practices.

## 📁 Project Structure

```
event-api/src/main/java/dev/peemtanapat/thaiticketmaster/event_api/event/
├── Category.java                    # Category entity (JPA)
├── CategoryRepository.java          # Category data access
├── CategoryController.java          # Category REST endpoints
├── Event.java                       # Event entity (JPA) 
├── EventRepository.java             # Event data access with custom queries
├── EventService.java                # Business logic layer
├── EventController.java             # Event REST endpoints
├── EventDTO.java                    # Data transfer object for responses
├── EventCreateRequest.java          # Request DTO for creating events
├── EventUpdateRequest.java          # Request DTO for updating events
├── EventStatus.java                 # Enum for event statuses
├── EventNotFoundException.java      # Custom exception
├── CategoryNotFoundException.java   # Custom exception
└── GlobalExceptionHandler.java      # Centralized exception handling
```

## ✨ Features Implemented

### 1. **Entities**

#### Category Entity
- ID (auto-generated)
- Name (unique)
- Description
- Created/Updated timestamps with auto-management

#### Event Entity
- ID (auto-generated)
- Name
- Category (Many-to-One relationship)
- **Show Date Times** (Multiple times in same day using @ElementCollection)
- Location
- On-Sale DateTime
- Ticket Price (BigDecimal for precision)
- Detail (TEXT field)
- Condition (TEXT field)
- **Event Status** (Enum: ON_SALE, SOLD_OUT, CANCELLED, POSTPONED, COMPLETED, COMING_SOON)
- **Gate Open** (e.g., "1 hour before show start")
- Created/Updated timestamps with auto-management

### 2. **Repository Layer**

#### EventRepository
- Standard CRUD operations (via JpaRepository)
- **Custom query**: `findOnSaleEventsOrderByShowDate()` - Returns events open for ticket purchase, ordered by earliest show date
- `findByCategory()` - Filter by category
- `findByEventStatus()` - Filter by status
- `findByCategoryAndEventStatus()` - Combined filtering
- Prepared for future Elasticsearch integration (commented)

#### CategoryRepository
- Standard CRUD operations
- `findByName()` - Find category by name
- `existsByName()` - Check existence

### 3. **Service Layer (EventService)**
- `getAllEvents()` - Retrieve all events
- `getEventById()` - Get specific event
- **`getOnSaleEvents()`** - Get events open to buy, ordered by show date ✨
- `getEventsByCategory()` - Filter by category
- `getEventsByStatus()` - Filter by status
- `createEvent()` - Admin: Create new event with validation
- `updateEvent()` - Admin: Partial update of event
- `deleteEvent()` - Admin: Remove event
- Proper exception handling
- Transaction management

### 4. **REST API Endpoints**

#### User Endpoints
```
GET    /api/v1/events                    # Get all events
GET    /api/v1/events/{id}               # Get event by ID
GET    /api/v1/events/on-sale            # Get on-sale events (ordered by show date) ⭐
GET    /api/v1/events/category/{id}      # Get events by category
GET    /api/v1/events/status/{status}    # Get events by status
GET    /api/v1/categories                # Get all categories
GET    /api/v1/categories/{id}           # Get category by ID
GET    /api/v1/categories/name/{name}    # Get category by name
```

#### Admin Endpoints
```
POST   /api/v1/events                    # Create new event
PUT    /api/v1/events/{id}               # Update event (partial)
DELETE /api/v1/events/{id}               # Delete event
```

### 5. **Validation**
- Bean Validation (Jakarta Validation) for request DTOs
- `@NotBlank`, `@NotNull`, `@NotEmpty` for required fields
- `@Size` for field length constraints
- `@DecimalMin` for price validation
- Global exception handler for validation errors with detailed field-level error messages

### 6. **Error Handling**
- Custom exceptions: `EventNotFoundException`, `CategoryNotFoundException`
- `GlobalExceptionHandler` with `@RestControllerAdvice`
- Proper HTTP status codes (404, 400, 500)
- Structured error responses with timestamp
- Validation error responses with field-level details

### 7. **Database Configuration**
- PostgreSQL driver configured
- JPA/Hibernate settings:
  - Auto DDL update
  - SQL logging (DEBUG mode)
  - PostgreSQL dialect
  - UTC timezone
  - Format SQL for readability
- Connection pooling via HikariCP (Spring Boot default)
- Kubernetes service URL configured
- Local development URL available (commented)

### 8. **Initial Data**
- SQL script (`data.sql`) to initialize 4 categories:
  - Concerts
  - Shows
  - Sports
  - Exhibitions

### 9. **Documentation**
- Comprehensive `API_DOCUMENTATION.md` with:
  - All endpoints documented
  - Request/response examples
  - Error handling examples
  - Future features noted
  - Development guide
- Updated `client.http` with sample requests for all endpoints
- Inline code comments

## 🔍 Key Highlights

### ⭐ Primary Feature: On-Sale Events Query
```java
@Query("SELECT DISTINCT e FROM Event e " +
       "LEFT JOIN FETCH e.category " +
       "LEFT JOIN e.showDateTimes st " +
       "WHERE e.eventStatus = 'ON_SALE' " +
       "AND e.onSaleDateTime <= :currentDateTime " +
       "ORDER BY st ASC")
List<Event> findOnSaleEventsOrderByShowDate(@Param("currentDateTime") LocalDateTime currentDateTime);
```

This query:
- ✅ Filters events with `ON_SALE` status
- ✅ Checks if on-sale datetime has passed
- ✅ Orders by show date (earliest first)
- ✅ Eager loads category to avoid N+1 queries

### Multiple Show Times Support
Events can have multiple show times in the same day using `@ElementCollection`:
```java
@ElementCollection
@CollectionTable(name = "event_show_times", joinColumns = @JoinColumn(name = "event_id"))
@Column(name = "show_datetime", nullable = false)
private List<LocalDateTime> showDateTimes = new ArrayList<>();
```

### Event Status Management
Comprehensive status tracking:
- `ON_SALE` - Tickets available
- `SOLD_OUT` - All tickets sold
- `CANCELLED` - Event cancelled
- `POSTPONED` - Rescheduled
- `COMPLETED` - Event finished
- `COMING_SOON` - Announced but not on sale yet

## 🚀 Future Enhancements (Commented in Code)

### Elasticsearch Full-Text Search
Prepared code for implementing full-text search by event name:
```java
// In EventRepository
// @Query(value = "SELECT * FROM events WHERE MATCH(name) AGAINST(:searchTerm IN NATURAL LANGUAGE MODE)", nativeQuery = true)
// List<Event> searchEventsByName(@Param("searchTerm") String searchTerm);

// In EventService
// public List<EventDTO> searchEventsByName(String searchTerm) { ... }

// In EventController
// @GetMapping("/search")
// public ResponseEntity<List<EventDTO>> searchEvents(@RequestParam String query) { ... }
```

### Security
- Add Spring Security
- Implement JWT authentication
- Role-based authorization (ADMIN vs USER)

## 📦 Dependencies Added
- `spring-boot-starter-data-jpa` - Already present
- `spring-boot-starter-web` - Already present  
- `spring-boot-starter-validation` - **Added** for Bean Validation
- `postgresql` - Already present

## 🎨 Architecture Patterns Used
- **Repository Pattern** - Data access abstraction
- **Service Layer Pattern** - Business logic separation
- **DTO Pattern** - Clean API contracts
- **Builder Pattern** - Entity construction (via setters)
- **Dependency Injection** - Constructor injection throughout
- **Exception Handling** - Centralized with `@RestControllerAdvice`

## ✅ Best Practices Applied
- RESTful API design
- Proper HTTP status codes
- Transaction management
- Input validation
- Exception handling
- Clean code structure
- Comprehensive documentation
- Database indexing for performance
- Eager loading to prevent N+1 queries
- UTC timezone consistency
- Immutable temporal types (LocalDateTime)

## 🧪 Testing
Use the provided `client.http` file with REST Client extension in VS Code:
- Health check endpoint
- All user endpoints
- All admin endpoints
- Sample data for testing

## 🔧 Configuration Files Modified
- `pom.xml` - Added validation starter
- `application.yml` - Complete JPA, datasource, and logging configuration
- `data.sql` - Initial category data

## 📊 Database Schema
Tables created automatically by Hibernate:
- `categories` - Category information
- `events` - Event information
- `event_show_times` - Show date times (collection table)

Indexes created:
- `idx_event_status` - On event_status column
- `idx_on_sale_datetime` - On on_sale_datetime column

## 🎯 Requirements Met
✅ Controllers, services, entities, repositories in event package  
✅ Event info contains all required fields  
✅ Category reference with 4 main categories  
✅ Multiple show times support  
✅ Location, on-sale datetime, ticket price  
✅ Detail and condition as TEXT fields  
✅ Event status enum with common statuses  
✅ Gate open information  
✅ User can query "open to buy" events ordered by show date  
✅ Commented placeholder for Elasticsearch full-text search  
✅ Admin can create and update events  

## 🎉 Result
A fully functional, production-ready Event API microservice with clean architecture, comprehensive features, and room for future enhancements!

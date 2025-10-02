# Event API Documentation

## Overview
This is the Event API microservice for the Ticketmaster application. It provides endpoints for users to browse events and for administrators to manage event information.

## Features

### Entities
- **Category**: Event categories (Concerts, Shows, Sports, Exhibitions)
- **Event**: Event information with details like name, location, dates, pricing, etc.

### Event Information
Each event contains:
- **Name**: Event name
- **Category**: Reference to category table (Concerts, Shows, Sports, Exhibitions)
- **Show Date Times**: Multiple show times in the same day
- **Location**: Event location/venue
- **On-Sale DateTime**: When tickets go on sale
- **Ticket Price**: Base ticket price
- **Detail**: Detailed event description
- **Condition**: Terms and conditions
- **Event Status**: Current status (ON_SALE, SOLD_OUT, CANCELLED, POSTPONED, COMPLETED, COMING_SOON)
- **Gate Open**: Gate opening information (e.g., "1 hour before show start")

## API Endpoints

### User Endpoints

#### Get All Events
```http
GET /api/v1/events
```
Returns all events.

#### Get Event by ID
```http
GET /api/v1/events/{id}
```
Returns a specific event by ID.

#### Get On-Sale Events (Ordered by Show Date)
```http
GET /api/v1/events/on-sale
```
Returns events that are currently open for ticket purchase, ordered by show date (earliest first).
- Events must have status `ON_SALE`
- On-sale datetime must have passed

#### Get Events by Category
```http
GET /api/v1/events/category/{categoryId}
```
Returns all events in a specific category.

#### Get Events by Status
```http
GET /api/v1/events/status/{status}
```
Returns all events with a specific status.
Status options: `ON_SALE`, `SOLD_OUT`, `CANCELLED`, `POSTPONED`, `COMPLETED`, `COMING_SOON`

### Admin Endpoints

#### Create Event
```http
POST /api/v1/events
Content-Type: application/json

{
  "name": "Summer Music Festival 2025",
  "categoryId": 1,
  "showDateTimes": [
    "2025-07-15T19:00:00",
    "2025-07-15T22:00:00"
  ],
  "location": "Central Stadium, Bangkok",
  "onSaleDateTime": "2025-06-01T10:00:00",
  "ticketPrice": 1500.00,
  "detail": "A spectacular summer music festival featuring top artists",
  "condition": "Age 18+. No refunds. Subject to terms and conditions.",
  "eventStatus": "ON_SALE",
  "gateOpen": "1 hour before show start"
}
```

#### Update Event
```http
PUT /api/v1/events/{id}
Content-Type: application/json

{
  "name": "Updated Event Name",
  "eventStatus": "SOLD_OUT",
  "ticketPrice": 2000.00
}
```
Note: Only provided fields will be updated (partial update).

#### Delete Event
```http
DELETE /api/v1/events/{id}
```

## Response Format

### Success Response
```json
{
  "id": 1,
  "name": "Summer Music Festival 2025",
  "category": {
    "id": 1,
    "name": "Concerts",
    "description": "Live music performances and concerts"
  },
  "showDateTimes": [
    "2025-07-15T19:00:00",
    "2025-07-15T22:00:00"
  ],
  "location": "Central Stadium, Bangkok",
  "onSaleDateTime": "2025-06-01T10:00:00",
  "ticketPrice": 1500.00,
  "detail": "A spectacular summer music festival featuring top artists",
  "condition": "Age 18+. No refunds. Subject to terms and conditions.",
  "eventStatus": "ON_SALE",
  "gateOpen": "1 hour before show start",
  "createdAt": "2025-10-01T10:00:00",
  "updatedAt": "2025-10-01T10:00:00"
}
```

### Error Response
```json
{
  "status": 404,
  "message": "Event not found with id: 999",
  "timestamp": "2025-10-02T10:30:00"
}
```

### Validation Error Response
```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2025-10-02T10:30:00",
  "errors": {
    "name": "Event name is required",
    "ticketPrice": "Ticket price must be greater than 0"
  }
}
```

## Future Features

### Full-Text Search (Planned)
```http
GET /api/v1/events/search?query={searchTerm}
```
Full-text search functionality using Elasticsearch for searching events by name.
- Commented code is prepared in the repository
- Requires Elasticsearch integration

## Database Configuration

The application uses PostgreSQL as the database. Configuration can be found in `application.yml`:

- **Default (Kubernetes)**: `postgresql.event-api.svc.cluster.local:5432`
- **Local Development**: Uncomment the localhost URL in `application.yml`

## Categories

The application comes with 4 pre-defined categories:
1. **Concerts** - Live music performances
2. **Shows** - Theater, comedy shows, entertainment
3. **Sports** - Sporting events and competitions
4. **Exhibitions** - Art exhibitions, trade shows, expos

Categories are automatically initialized from `data.sql`.

## Event Status Types

- `ON_SALE`: Tickets are currently available
- `SOLD_OUT`: All tickets have been sold
- `CANCELLED`: Event has been cancelled
- `POSTPONED`: Event has been postponed to a later date
- `COMPLETED`: Event has already taken place
- `COMING_SOON`: Event announced but tickets not yet on sale

## Security (To Be Implemented)

Admin endpoints (POST, PUT, DELETE) should be protected with role-based authentication.
- TODO: Add Spring Security
- TODO: Implement JWT authentication
- TODO: Add ADMIN role authorization

## Development

### Prerequisites
- Java 21
- Maven
- PostgreSQL database

### Build
```bash
./mvnw clean package
```

### Run
```bash
./mvnw spring-boot:run
```

### Docker Build
```bash
docker build -t event-api:latest .
```

## Testing

Use the provided `client.http` file with REST Client extension in VS Code for manual API testing.

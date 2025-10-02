# Event API - Quick Start Guide

## 🚀 Getting Started

### Prerequisites
- Java 21
- Maven
- PostgreSQL database running
- (Optional) Docker for containerization

### Setup Steps

#### 1. Database Setup
Make sure PostgreSQL is running and accessible. The application expects:
- **Database name**: `event-db`
- **Username**: `eventuser`
- **Password**: `eventpass123`
- **Port**: `5432`

For Kubernetes deployment:
```bash
# Apply PostgreSQL manifests (from event-db directory)
kubectl apply -f event-db/
```

For local development:
```bash
# Create database and user in PostgreSQL
psql -U postgres
CREATE DATABASE event_db;
CREATE USER eventuser WITH PASSWORD 'eventpass123';
GRANT ALL PRIVILEGES ON DATABASE event_db TO eventuser;
```

Then update `application.yml`:
```yaml
spring:
  datasource:
    # Comment out Kubernetes URL
    # url: jdbc:postgresql://postgresql.event-api.svc.cluster.local:5432/event-db
    # Uncomment local URL
    url: jdbc:postgresql://localhost:5432/event_db
```

#### 2. Build the Application
```bash
cd event-api
./mvnw clean install
```

#### 3. Run the Application
```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

#### 4. Verify Setup
```bash
# Check health
curl http://localhost:8080/api/health

# Get categories (should return 4 categories)
curl http://localhost:8080/api/v1/categories
```

### Testing the API

#### Using client.http (Recommended)
1. Open `client.http` in VS Code
2. Install "REST Client" extension
3. Click "Send Request" above any endpoint

#### Using curl

**Get all events:**
```bash
curl http://localhost:8080/api/v1/events
```

**Get on-sale events (ordered by show date):**
```bash
curl http://localhost:8080/api/v1/events/on-sale
```

**Create an event:**
```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Summer Music Festival 2025",
    "categoryId": 1,
    "showDateTimes": ["2025-07-15T19:00:00", "2025-07-15T22:00:00"],
    "location": "Central Stadium, Bangkok",
    "onSaleDateTime": "2025-06-01T10:00:00",
    "ticketPrice": 1500.00,
    "detail": "A spectacular summer music festival",
    "condition": "Age 18+. No refunds.",
    "eventStatus": "ON_SALE",
    "gateOpen": "1 hour before show start"
  }'
```

**Get events by category (Concerts):**
```bash
curl http://localhost:8080/api/v1/events/category/1
```

**Update event status:**
```bash
curl -X PUT http://localhost:8080/api/v1/events/1 \
  -H "Content-Type: application/json" \
  -d '{"eventStatus": "SOLD_OUT"}'
```

### Docker Deployment

**Build Docker image:**
```bash
docker build -t event-api:latest .
```

**Run with Docker:**
```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/event_db \
  event-api:latest
```

### Kubernetes Deployment

```bash
# Apply namespace
kubectl apply -f event-api.namespace.yaml

# Apply deployment and service
kubectl apply -f event-api.deployment.yaml
kubectl apply -f event-api.service.yaml
```

## 📝 Sample Data

### Categories (Auto-created)
1. **Concerts** - Live music performances
2. **Shows** - Theater, comedy shows
3. **Sports** - Sporting events
4. **Exhibitions** - Art exhibitions, expos

### Create Sample Events

Use the requests in `client.http` to create sample events or use these curl commands:

**Concert Event:**
```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Rock Band Concert",
    "categoryId": 1,
    "showDateTimes": ["2025-11-20T20:00:00"],
    "location": "Impact Arena, Bangkok",
    "onSaleDateTime": "2025-10-01T10:00:00",
    "ticketPrice": 2500.00,
    "detail": "International rock band tour",
    "condition": "No refunds. Age 15+",
    "eventStatus": "ON_SALE",
    "gateOpen": "1.5 hours before show"
  }'
```

**Sports Event:**
```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Basketball Championship Finals",
    "categoryId": 3,
    "showDateTimes": ["2025-12-10T18:00:00"],
    "location": "Thailand Convention Center",
    "onSaleDateTime": "2025-11-01T09:00:00",
    "ticketPrice": 1200.00,
    "detail": "National championship finals",
    "condition": "All ages welcome",
    "eventStatus": "COMING_SOON",
    "gateOpen": "2 hours before match"
  }'
```

## 🔍 Common Issues

### Database Connection Error
**Error:** `Connection refused` or `Could not connect to database`

**Solution:**
1. Verify PostgreSQL is running: `pg_isready`
2. Check connection details in `application.yml`
3. Ensure database and user exist
4. Check firewall/network settings

### Port Already in Use
**Error:** `Port 8080 is already in use`

**Solution:**
1. Change port in `application.yml`:
   ```yaml
   server:
     port: 8081
   ```
2. Or kill the process using port 8080

### Maven Build Errors
**Solution:**
```bash
./mvnw clean install -U  # Force update dependencies
```

### JPA/Hibernate Errors
The application will auto-create tables on first run. If issues occur:
1. Check database permissions
2. Verify Hibernate ddl-auto setting
3. Check logs for SQL errors

## 📚 Additional Resources

- **API Documentation**: `API_DOCUMENTATION.md`
- **Implementation Details**: `IMPLEMENTATION_SUMMARY.md`
- **Test Requests**: `client.http`

## 🎯 Next Steps

1. ✅ Set up database
2. ✅ Build and run application
3. ✅ Test with sample requests
4. 🔄 Add authentication/authorization
5. 🔄 Implement Elasticsearch for search
6. 🔄 Add caching with Redis
7. 🔄 Add metrics and monitoring
8. 🔄 Write unit and integration tests

## 💡 Tips

- Use `client.http` for easy API testing
- Check logs in console for SQL queries (DEBUG mode enabled)
- Categories are auto-created on first run
- Event timestamps (createdAt, updatedAt) are managed automatically
- Use the `/on-sale` endpoint for the main user-facing event listing

## 🐛 Troubleshooting

**Check application logs:**
```bash
# Console output shows SQL queries and debug info
# Look for ERROR or WARN messages
```

**Test database connection:**
```bash
psql -h localhost -U eventuser -d event_db
```

**Verify categories were created:**
```bash
curl http://localhost:8080/api/v1/categories
# Should return 4 categories
```

Happy coding! 🚀

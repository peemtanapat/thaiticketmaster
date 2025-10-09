.PHONY: help install-all install-traefik deploy-event-api clean clean-all status

# Database configuration
DB_HOST ?= localhost
DB_PORT ?= 5432
DB_USER ?= admin
DB_PASSWORD ?= admin
DB_NAME ?= booking_db

# Default target
help:
	@echo "Available targets:"
	@echo ""
	@echo "Booking API (Local Development):"
	@echo "  booking-run       - Run booking-api locally"
	@echo "  booking-build     - Build booking-api binary"
	@echo "  booking-test      - Run booking-api tests"
	@echo "  booking-test-all  - Run all booking-api tests (including integration)"
	@echo "  booking-coverage  - Generate test coverage report"
	@echo "  booking-deps      - Download booking-api dependencies"
	@echo "  booking-clean     - Clean booking-api build artifacts"
	@echo ""
	@echo "Kubernetes Deployment:"
	@echo "  install-all       - Install Traefik, and deploy Event API"
	@echo "  install-traefik   - Install Traefik ingress controller"
	@echo "  deploy-event-api  - Deploy the Event API application"
	@echo "  status            - Check status of all deployments"
	@echo "  clean             - Remove Event API deployment"
	@echo "  clean-all         - Remove all deployments (Event API, Traefik)"
	@echo "  restart-event-api - Restart Event API pods"
	@echo "  logs-event-api    - Tail Event API logs"
	@echo ""
	@echo "Database Migration:"
	@echo "  db-migrate            - Run event seats migration (simple version)"
	@echo "  db-migrate-timestamps - Add timestamp columns to event_seats (FIX for booking)"
	@echo "  db-migrate-full       - Run event seats migration (full version with functions)"
	@echo "  db-verify             - Verify event seats were created"
	@echo "  db-status             - Show seat inventory status"
	@echo "  db-clean-seats        - Remove event_seats table (CAUTION!)"

# Install everything in order
install-all: install-traefik deploy-event-api
	@echo "✅ All components installed successfully!"

# Install Traefik
install-traefik:
	@echo "🚀 Installing Traefik..."
	kubectl apply -f treafik/traefik.namespace.yaml
	helm repo add traefik https://traefik.github.io/charts
	helm repo update
	helm install traefik traefik/traefik \
		--namespace traefik \
		--values treafik/traefik-values.yaml
	@echo "⏳ Waiting for Traefik to be ready..."
	kubectl wait --namespace traefik \
		--for=condition=ready pod \
		--selector=app.kubernetes.io/name=traefik \
		--timeout=90s
	@echo "✅ Traefik installed successfully"

# Deploy Event API
deploy-event-api:
	@echo "🚀 Deploying Event API..."
	kubectl apply -f event-api/event-api.namespace.yaml
	kubectl apply -f event-api/event-api.deployment.yaml
	kubectl apply -f event-api/event-api.service.yaml
	kubectl apply -f event-api/event-api.ingressroute.yaml
	@echo "⏳ Waiting for Event API to be ready..."
	kubectl wait --namespace event-api \
		--for=condition=ready pod \
		--selector=app=event-api \
		--timeout=90s
	@echo "✅ Event API deployed successfully"

# Check status of all components
status:
	@echo ""
	@echo "📊 Checking Traefik status..."
	@kubectl get pods -n traefik 2>/dev/null || echo "Traefik not installed"
	@kubectl get svc -n traefik 2>/dev/null || true
	@echo ""
	@echo "📊 Checking Event API status..."
	@kubectl get pods -n event-api 2>/dev/null || echo "Event API not deployed"
	@kubectl get svc -n event-api 2>/dev/null || true
	@kubectl get ingressroute -n event-api 2>/dev/null || true

# Restart Event API pods
restart-event-api:
	@echo "🔄 Restarting Event API pods..."
	kubectl rollout restart deployment -n event-api
	kubectl rollout status deployment -n event-api

# View Event API logs
logs-event-api:
	@echo "📜 Tailing Event API logs..."
	kubectl logs -n event-api -l app=event-api --tail=100 -f

# Clean Event API only
clean:
	@echo "🧹 Removing Event API..."
	kubectl delete -f event-api/event-api.ingressroute.yaml --ignore-not-found
	kubectl delete -f event-api/event-api.service.yaml --ignore-not-found
	kubectl delete -f event-api/event-api.deployment.yaml --ignore-not-found
	kubectl delete -f event-api/event-api.namespace.yaml --ignore-not-found
	@echo "✅ Event API removed"

# Clean everything
clean-all: clean
	@echo "🧹 Removing Traefik..."
	helm uninstall traefik -n traefik 2>/dev/null || true
	kubectl delete namespace traefik --ignore-not-found
	@echo "✅ All components removed"

# Development helpers
dev-rebuild:
	@echo "🔨 Rebuilding and redeploying Event API..."
	kubectl delete -f event-api/event-api.deployment.yaml --ignore-not-found
	kubectl apply -f event-api/event-api.deployment.yaml
	kubectl wait --namespace event-api \
		--for=condition=ready pod \
		--selector=app=event-api \
		--timeout=90s

# Show ingress information
show-ingress:
	@echo "🌐 Ingress Information:"
	@kubectl get svc -n traefik traefik -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "No LoadBalancer IP assigned yet"

# ============================================================================
# Database Migration Targets
# ============================================================================

# Run simple event seats migration
db-migrate:
	@echo "🗄️  Running Event Seats Migration (Simple Version)..."
	@echo "Database: $(DB_NAME) on $(DB_HOST):$(DB_PORT)"
	@PGPASSWORD=$(DB_PASSWORD) psql -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d $(DB_NAME) \
		-f booking-api/migrations/001_create_event_seats.sql
	@echo "✅ Migration completed!"

# Add timestamp columns to existing event_seats table
db-migrate-timestamps:
	@echo "🗄️  Adding timestamp columns to event_seats table..."
	@echo "Database: $(DB_NAME) on $(DB_HOST):$(DB_PORT)"
	@PGPASSWORD=$(DB_PASSWORD) psql -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d $(DB_NAME) \
		-f booking-api/migrations/003_alter_showtime_timestamptz.sql
	@echo "✅ Timestamp columns added!"

# Run full event seats migration with functions
db-migrate-full:
	@echo "🗄️  Running Event Seats Migration (Full Version with Functions)..."
	@echo "Database: $(DB_NAME) on $(DB_HOST):$(DB_PORT)"
	@PGPASSWORD=$(DB_PASSWORD) psql -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d $(DB_NAME) \
		-f booking-api/migrations/001_create_event_seats.sql
	@echo "✅ Migration completed!"

# Verify seats were created
db-verify:
	@echo "🔍 Verifying Event Seats..."
	@PGPASSWORD=$(DB_PASSWORD) psql -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d $(DB_NAME) \
		-c "SELECT event_id, TO_CHAR(showtime, 'YYYY-MM-DD HH24:MI') as showtime, COUNT(*) as total_seats FROM event_seats WHERE event_id IN ('1', '2') GROUP BY event_id, showtime ORDER BY event_id;"

# Show seat inventory status
db-status:
	@echo "📊 Event Seats Inventory:"
	@echo ""
	@PGPASSWORD=$(DB_PASSWORD) psql -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d $(DB_NAME) \
		-c "SELECT event_id, zone, COUNT(*) as total, COUNT(*) FILTER (WHERE status='AVAILABLE') as available, COUNT(*) FILTER (WHERE status='RESERVED') as reserved, COUNT(*) FILTER (WHERE status='SOLD') as sold, MIN(price) as min_price, MAX(price) as max_price FROM event_seats WHERE event_id IN ('1', '2') GROUP BY event_id, zone ORDER BY event_id, zone;"

# Show all seats for Event 1
db-show-event1:
	@echo "🎫 Event 1 - All Seats:"
	@PGPASSWORD=$(DB_PASSWORD) psql -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d $(DB_NAME) \
		-c "SELECT seat_id, zone, price, status FROM event_seats WHERE event_id = '1' ORDER BY seat_id;"

# Show all seats for Event 2
db-show-event2:
	@echo "🎫 Event 2 - All Seats:"
	@PGPASSWORD=$(DB_PASSWORD) psql -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d $(DB_NAME) \
		-c "SELECT seat_id, zone, price, status FROM event_seats WHERE event_id = '2' ORDER BY seat_id;"

# Show only available seats
db-available:
	@echo "✅ Available Seats:"
	@PGPASSWORD=$(DB_PASSWORD) psql -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d $(DB_NAME) \
		-c "SELECT event_id, seat_id, zone, price FROM event_seats WHERE status = 'AVAILABLE' AND event_id IN ('1', '2') ORDER BY event_id, seat_id;"

# Clean event_seats table (CAUTION!)
db-clean-seats:
	@echo "⚠️  WARNING: This will DELETE the event_seats table and ALL seat data!"
	@echo "Press Ctrl+C to cancel, or Enter to continue..."
	@read confirm
	@PGPASSWORD=$(DB_PASSWORD) psql -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d $(DB_NAME) \
		-c "DROP TABLE IF EXISTS event_seats CASCADE;"
	@echo "🗑️  event_seats table removed"

# Connect to database (interactive)
db-connect:
	@echo "🔌 Connecting to database: $(DB_NAME)"
	@PGPASSWORD=$(DB_PASSWORD) psql -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d $(DB_NAME)

# ============================================================================
# Booking API - Local Development Targets
# ============================================================================

# Run booking-api locally
booking-run:
	@echo "🚀 Running Booking API locally..."
	@echo "Database: $(DB_NAME) on $(DB_HOST):$(DB_PORT)"
	@cd booking-api && GOTOOLCHAIN=local go run ./cmd/api

# Build booking-api binary
booking-build:
	@echo "🔨 Building Booking API..."
	@cd booking-api && GOTOOLCHAIN=local go build -o bin/booking-api ./cmd/api
	@echo "✅ Binary built: booking-api/bin/booking-api"

# Run booking-api tests (short mode, skips integration tests)
booking-test:
	@echo "🧪 Running Booking API tests (short mode)..."
	@cd booking-api && GOTOOLCHAIN=local go test -v -short -race ./...

# Run all booking-api tests including integration tests
booking-test-all:
	@echo "🧪 Running ALL Booking API tests (including integration)..."
	@echo "Note: Requires PostgreSQL and Redis to be running"
	@cd booking-api && GOTOOLCHAIN=local go test -v -race ./...

# Run tests with coverage report
booking-coverage:
	@echo "📊 Generating test coverage report..."
	@cd booking-api && GOTOOLCHAIN=local go test -v -short -race -coverprofile=coverage.out ./...
	@cd booking-api && GOTOOLCHAIN=local go tool cover -html=coverage.out -o coverage.html
	@echo "✅ Coverage report generated: booking-api/coverage.html"

# Download booking-api dependencies
booking-deps:
	@echo "📦 Downloading Booking API dependencies..."
	@cd booking-api && GOTOOLCHAIN=local go mod download
	@cd booking-api && GOTOOLCHAIN=local go mod tidy
	@echo "✅ Dependencies updated"

# Clean booking-api build artifacts
booking-clean:
	@echo "🧹 Cleaning Booking API build artifacts..."
	@rm -rf booking-api/bin
	@rm -f booking-api/coverage.out
	@rm -f booking-api/coverage.html
	@rm -f booking-api/__debug_bin*
	@echo "✅ Clean complete"

# Build and run booking-api
booking-dev: booking-build
	@echo "🚀 Starting Booking API..."
	@./booking-api/bin/booking-api

# Watch and reload booking-api (requires air or similar)
booking-watch:
	@echo "👀 Watching Booking API for changes..."
	@cd booking-api && air || (echo "❌ 'air' not installed. Install with: go install github.com/cosmtrek/air@latest" && exit 1)

.PHONY: help test build run docker-build docker-run clean coverage lint

# Variables
APP_NAME=booking-api
DOCKER_IMAGE=$(APP_NAME):latest
GO=GOTOOLCHAIN=local go

help: ## Display this help screen
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

deps: ## Download dependencies
	$(GO) mod download
	$(GO) mod tidy

test: ## Run tests (short mode, skips integration tests)
	$(GO) test -v -short -race ./...

test-all: ## Run all tests including integration tests (requires PostgreSQL)
	$(GO) test -v -race ./...

test-coverage: ## Run tests with coverage
	$(GO) test -v -short -race -coverprofile=coverage.out ./...
	$(GO) tool cover -html=coverage.out -o coverage.html
	@echo "Coverage report generated: coverage.html"

build: ## Build the application
	$(GO) build -o bin/$(APP_NAME) ./cmd/api

run: ## Run the application locally
	$(GO) run ./cmd/api

run-debug: ## Run the application with verbose logging
	@echo "Starting booking-api in debug mode..."
	@echo "Setting LOG_LEVEL=debug"
	LOG_LEVEL=debug $(GO) run ./cmd/api

debug: ## Run the application with Delve debugger (install: go install github.com/go-delve/delve/cmd/dlv@latest)
	@echo "Starting Delve debugger on port 2345..."
	@echo "Connect your IDE debugger to localhost:2345"
	dlv debug ./cmd/api --headless --listen=:2345 --api-version=2 --accept-multiclient

debug-test: ## Run tests with Delve debugger
	dlv test ./internal/booking --headless --listen=:2345 --api-version=2

install-delve: ## Install Delve debugger
	$(GO) install github.com/go-delve/delve/cmd/dlv@latest
	@echo "Delve installed successfully!"

docker-build: ## Build Docker image
	docker build -t $(DOCKER_IMAGE) .

docker-run: ## Run Docker container
	docker run -p 8081:8081 \
		-e DB_HOST=host.docker.internal \
		-e REDIS_HOST=host.docker.internal \
		-e EVENT_API_URL=http://host.docker.internal:8080 \
		$(DOCKER_IMAGE)

clean: ## Clean build artifacts
	rm -rf bin/
	rm -f coverage.out coverage.html
	$(GO) clean

lint: ## Run linters
	$(GO) fmt ./...
	$(GO) vet ./...

mod-verify: ## Verify dependencies
	$(GO) mod verify

all: clean deps lint test build ## Run all tasks

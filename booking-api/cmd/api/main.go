package main

import (
	"context"
	"database/sql"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/go-redis/redis/v8"
	_ "github.com/lib/pq"
	"github.com/peemtanapat/thaiticketmaster/booking-api/internal/booking"
)

func main() {
	// Load configuration from environment variables
	dbHost := getEnv("DB_HOST", "localhost")
	dbPort := getEnv("DB_PORT", "5432")
	dbUser := getEnv("DB_USER", "admin")
	dbPassword := getEnv("DB_PASSWORD", "admin")
	dbName := getEnv("DB_NAME", "booking_db")

	redisHost := getEnv("REDIS_HOST", "localhost")
	redisPort := getEnv("REDIS_PORT", "6379")

	eventAPIURL := getEnv("EVENT_API_URL", "http://localhost:8080")
	serverPort := getEnv("SERVER_PORT", "8081")

	// Ensure database exists (create if not exists)
	log.Println("Checking database existence...")
	if err := ensureDatabaseExists(dbHost, dbPort, dbUser, dbPassword, dbName); err != nil {
		log.Fatalf("Failed to ensure database exists: %v", err)
	}

	// Initialize database connection
	dsn := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=disable",
		dbHost, dbPort, dbUser, dbPassword, dbName)

	db, err := sql.Open("postgres", dsn)
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}
	defer db.Close()

	// Test database connection
	if err := db.Ping(); err != nil {
		log.Fatalf("Failed to ping database: %v", err)
	}
	log.Println("Successfully connected to database")

	// Create/verify database schema (tables, indexes)
	log.Println("Creating/verifying database schema...")
	if err := createBookingSchema(db); err != nil {
		log.Fatalf("Failed to create database schema: %v", err)
	}

	// Initialize Redis client
	redisClient := redis.NewClient(&redis.Options{
		Addr:     fmt.Sprintf("%s:%s", redisHost, redisPort),
		Password: "", // No password by default
		DB:       0,  // Default DB
	})
	defer redisClient.Close()

	// Test Redis connection
	ctx := context.Background()
	if err := redisClient.Ping(ctx).Err(); err != nil {
		log.Printf("Warning: Failed to ping Redis: %v", err)
	} else {
		log.Println("Successfully connected to Redis")
	}

	// Initialize booking service components
	locker := booking.NewRedisLocker(redisClient)
	txManager := booking.NewSQLTransactionManager(db)
	eventClient := booking.NewHTTPEventAPIClient(eventAPIURL)
	bookingRepository := booking.NewPostgresBookingRepository(db)

	// Initialize booking service
	bookingService := booking.NewBookingService(locker, txManager, eventClient, bookingRepository)

	// Setup HTTP server
	mux := http.NewServeMux()

	// Health check endpoint
	mux.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"status":"healthy"}`))
	})

	// Initialize booking handler
	bookingHandler := booking.NewBookingHandler(bookingService)

	// Booking endpoints
	mux.HandleFunc("/api/v1/bookings", bookingHandler.CreateBooking)
	mux.HandleFunc("/api/v1/bookings/", func(w http.ResponseWriter, r *http.Request) {
		// Route to appropriate handler based on method
		switch r.Method {
		case http.MethodGet:
			bookingHandler.GetBooking(w, r)
		case http.MethodDelete:
			bookingHandler.CancelBooking(w, r)
		default:
			http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		}
	})

	log.Println("Registered endpoints:")
	log.Println("  GET  /health")
	log.Println("  POST /api/v1/bookings")
	log.Println("  GET  /api/v1/bookings/{id}")
	log.Println("  DELETE /api/v1/bookings/{id}")

	server := &http.Server{
		Addr:         ":" + serverPort,
		Handler:      mux,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	// Start server in a goroutine
	go func() {
		log.Printf("Starting booking-api server on port %s", serverPort)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Failed to start server: %v", err)
		}
	}()

	// Wait for interrupt signal to gracefully shutdown
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	log.Println("Shutting down server...")
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	if err := server.Shutdown(ctx); err != nil {
		log.Fatalf("Server forced to shutdown: %v", err)
	}

	log.Println("Server exited")
}

func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}

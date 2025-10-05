package main

import (
	"database/sql"
	"fmt"
	"os"
	"testing"

	_ "github.com/lib/pq"
)

// Helper function to get environment variable with fallback
func getEnvOrDefault(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}

// TestEnsureDatabaseExists tests the database creation logic
// Note: This requires a running PostgreSQL instance
func TestEnsureDatabaseExists(t *testing.T) {
	if testing.Short() {
		t.Skip("Skipping integration test in short mode")
	}

	// Arrange
	host := getEnvOrDefault("DB_HOST", "localhost")
	port := getEnvOrDefault("DB_PORT", "5432")
	user := getEnvOrDefault("DB_USER", "admin")
	password := getEnvOrDefault("DB_PASSWORD", "admin")
	testDBName := "booking_api_test"

	// Clean up: drop test database if exists
	defer func() {
		dropTestDatabase(host, port, user, password, testDBName)
	}()

	// Act
	err := ensureDatabaseExists(host, port, user, password, testDBName)
	if err != nil {
		t.Fatalf("Failed to ensure database exists: %v", err)
	}

	// Assert
	exists, err := databaseExists(host, port, user, password, testDBName)
	if err != nil {
		t.Fatalf("Failed to check if database exists: %v", err)
	}

	if !exists {
		t.Errorf("Database should exist but doesn't")
	}

	// Test idempotency - calling again should not error
	err = ensureDatabaseExists(host, port, user, password, testDBName)
	if err != nil {
		t.Fatalf("Second call to ensureDatabaseExists should not error: %v", err)
	}
}

// TestCreateBookingSchema tests the schema creation
func TestCreateBookingSchema(t *testing.T) {
	if testing.Short() {
		t.Skip("Skipping integration test in short mode")
	}

	// Arrange
	testDBName := "booking_api_schema_test"

	host := getEnvOrDefault("DB_HOST", "localhost")
	port := getEnvOrDefault("DB_PORT", "5432")
	user := getEnvOrDefault("DB_USER", "admin")
	password := getEnvOrDefault("DB_PASSWORD", "admin")

	// Clean up
	defer func() {
		dropTestDatabase(host, port, user, password, testDBName)
	}()

	// Create test database
	if err := ensureDatabaseExists(host, port, user, password, testDBName); err != nil {
		t.Fatalf("Failed to create test database: %v", err)
	}

	// Connect to test database
	dsn := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=disable",
		host, port, user, password, testDBName)

	db, err := sql.Open("postgres", dsn)
	if err != nil {
		t.Fatalf("Failed to connect to test database: %v", err)
	}
	defer db.Close()

	// Act
	err = createBookingSchema(db)
	if err != nil {
		t.Fatalf("Failed to create schema: %v", err)
	}

	// Assert
	tables := []string{"bookings", "booking_seats"}
	for _, table := range tables {
		var exists bool
		query := `SELECT EXISTS (
			SELECT FROM information_schema.tables 
			WHERE table_name = $1
		)`
		err := db.QueryRow(query, table).Scan(&exists)
		if err != nil {
			t.Fatalf("Failed to check if table %s exists: %v", table, err)
		}
		if !exists {
			t.Errorf("Table %s should exist but doesn't", table)
		}
	}

	// Test idempotency - calling again should not error
	err = createBookingSchema(db)
	if err != nil {
		t.Fatalf("Second call to createBookingSchema should not error: %v", err)
	}
}

// Helper functions for tests

func databaseExists(host, port, user, password, dbName string) (bool, error) {
	postgresDB := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=postgres sslmode=disable",
		host, port, user, password)

	db, err := sql.Open("postgres", postgresDB)
	if err != nil {
		return false, err
	}
	defer db.Close()

	var exists bool
	query := `SELECT EXISTS(SELECT 1 FROM pg_database WHERE datname = $1)`
	err = db.QueryRow(query, dbName).Scan(&exists)
	return exists, err
}

func dropTestDatabase(host, port, user, password, dbName string) error {
	postgresDB := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=postgres sslmode=disable",
		host, port, user, password)

	db, err := sql.Open("postgres", postgresDB)
	if err != nil {
		return err
	}
	defer db.Close()

	// Terminate existing connections
	_, _ = db.Exec(fmt.Sprintf(`
		SELECT pg_terminate_backend(pg_stat_activity.pid)
		FROM pg_stat_activity
		WHERE pg_stat_activity.datname = '%s'
		AND pid <> pg_backend_pid()
	`, dbName))

	// Drop database
	_, err = db.Exec(fmt.Sprintf("DROP DATABASE IF EXISTS %s", dbName))
	return err
}

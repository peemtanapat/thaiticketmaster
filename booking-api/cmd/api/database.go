package main

import (
	"database/sql"
	"fmt"
	"log"

	_ "github.com/lib/pq"
)

// ensureDatabaseExists checks if the database exists and creates it if it doesn't
func ensureDatabaseExists(host, port, user, password, dbName string) error {
	// Connect to postgres database (default database)
	postgresDB := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=postgres sslmode=disable",
		host, port, user, password)

	db, err := sql.Open("postgres", postgresDB)
	if err != nil {
		return fmt.Errorf("failed to connect to postgres database: %w", err)
	}
	defer db.Close()

	// Test connection
	if err := db.Ping(); err != nil {
		return fmt.Errorf("failed to ping postgres database: %w", err)
	}

	// Check if database exists
	var exists bool
	query := `SELECT EXISTS(SELECT 1 FROM pg_database WHERE datname = $1)`
	err = db.QueryRow(query, dbName).Scan(&exists)
	if err != nil {
		return fmt.Errorf("failed to check if database exists: %w", err)
	}

	if exists {
		log.Printf("Database '%s' already exists", dbName)
		return nil
	}

	// Create database
	log.Printf("Database '%s' does not exist. Creating...", dbName)
	createQuery := fmt.Sprintf("CREATE DATABASE %s", dbName)
	_, err = db.Exec(createQuery)
	if err != nil {
		return fmt.Errorf("failed to create database '%s': %w", dbName, err)
	}

	log.Printf("Successfully created database '%s'", dbName)
	return nil
}

// createBookingSchema creates the necessary tables for booking-api
func createBookingSchema(db *sql.DB) error {
	schema := `
	-- Create bookings table if not exists
	CREATE TABLE IF NOT EXISTS bookings (
		id SERIAL PRIMARY KEY,
		booking_id VARCHAR(255) UNIQUE NOT NULL,
		event_id VARCHAR(255) NOT NULL,
		user_id VARCHAR(255) NOT NULL,
		showtime TIMESTAMPTZ NOT NULL,
		quantity INTEGER NOT NULL,
		status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
		created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
		updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
	);

	-- Create booking_seats table if not exists
	CREATE TABLE IF NOT EXISTS booking_seats (
		id SERIAL PRIMARY KEY,
		booking_id VARCHAR(255) NOT NULL,
		seat_id VARCHAR(255) NOT NULL,
		created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
		FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
	);

	-- Create indexes for better performance
	CREATE INDEX IF NOT EXISTS idx_bookings_event_id ON bookings(event_id);
	CREATE INDEX IF NOT EXISTS idx_bookings_user_id ON bookings(user_id);
	CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);
	CREATE INDEX IF NOT EXISTS idx_booking_seats_booking_id ON booking_seats(booking_id);
	`

	_, err := db.Exec(schema)
	if err != nil {
		return fmt.Errorf("failed to create schema: %w", err)
	}

	log.Println("Successfully created/verified database schema")
	return nil
}

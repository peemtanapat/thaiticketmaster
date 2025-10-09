-- ============================================================================
-- Migration 001: Create Complete Schema with Latest Updates
-- Description: Creates all tables with TIMESTAMPTZ columns (timezone-aware)
--              Includes: bookings, booking_seats, event_seats with all features
-- Version: v2.0 (Consolidated from 001, 002, 003)
-- Created: 2025-10-07
-- ============================================================================

-- ============================================================================
-- PART 1: Create bookings table
-- ============================================================================
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

-- Create indexes for bookings table
CREATE INDEX IF NOT EXISTS idx_bookings_event_id ON bookings(event_id);
CREATE INDEX IF NOT EXISTS idx_bookings_user_id ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_bookings_showtime ON bookings(showtime);
CREATE INDEX IF NOT EXISTS idx_bookings_event_showtime ON bookings(event_id, showtime);

-- ============================================================================
-- PART 2: Create booking_seats table
-- ============================================================================
CREATE TABLE IF NOT EXISTS booking_seats (
    id SERIAL PRIMARY KEY,
    booking_id VARCHAR(255) NOT NULL,
    seat_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
);

-- Create indexes for booking_seats table
CREATE INDEX IF NOT EXISTS idx_booking_seats_booking_id ON booking_seats(booking_id);
CREATE INDEX IF NOT EXISTS idx_booking_seats_seat_id ON booking_seats(seat_id);

-- ============================================================================
-- PART 3: Create event_seats table (with all timestamp columns)
-- ============================================================================
CREATE TABLE IF NOT EXISTS event_seats (
    id SERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    showtime TIMESTAMPTZ NOT NULL,
    seat_id VARCHAR(50) NOT NULL,
    zone VARCHAR(50) NOT NULL DEFAULT 'Standard',
    row_number VARCHAR(10),
    seat_number INTEGER,
    price DECIMAL(10, 2) NOT NULL DEFAULT 1000.00,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    booking_id VARCHAR(255),
    
    -- Timestamp columns (all with timezone)
    reserved_at TIMESTAMPTZ,
    reserved_until TIMESTAMPTZ,
    sold_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT unique_seat_per_showtime UNIQUE (event_id, showtime, seat_id),
    CONSTRAINT valid_status CHECK (status IN ('AVAILABLE', 'RESERVED', 'SOLD', 'BLOCKED')),
    CONSTRAINT valid_zone CHECK (zone IN ('VIP', 'Premium', 'Standard', 'Balcony', 'Standing'))
);

-- Create indexes for event_seats table
CREATE INDEX IF NOT EXISTS idx_event_seats_event_showtime 
    ON event_seats(event_id, showtime);

CREATE INDEX IF NOT EXISTS idx_event_seats_status 
    ON event_seats(status);

CREATE INDEX IF NOT EXISTS idx_event_seats_booking_id 
    ON event_seats(booking_id) 
    WHERE booking_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_event_seats_zone 
    ON event_seats(zone);

CREATE INDEX IF NOT EXISTS idx_event_seats_reserved_until 
    ON event_seats(reserved_until) 
    WHERE status = 'RESERVED';

-- Composite index for availability checks (performance optimization)
CREATE INDEX IF NOT EXISTS idx_event_seats_availability 
    ON event_seats(event_id, showtime, status, seat_id);

-- ============================================================================
-- PART 4: Create helper functions
-- ============================================================================

-- Function to check if seats exist for an event
CREATE OR REPLACE FUNCTION seats_exist_for_event(p_event_id VARCHAR, p_showtime TIMESTAMPTZ)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM event_seats 
        WHERE event_id = p_event_id 
        AND showtime = p_showtime
    );
END;
$$ LANGUAGE plpgsql;

-- Function to create seats for an event
CREATE OR REPLACE FUNCTION create_seats_for_event(
    p_event_id VARCHAR,
    p_showtime TIMESTAMPTZ,
    p_total_seats INTEGER DEFAULT 20
)
RETURNS INTEGER AS $$
DECLARE
    v_row_letter CHAR(1);
    v_seat_num INTEGER;
    v_zone VARCHAR(50);
    v_price DECIMAL(10, 2);
    v_inserted_count INTEGER := 0;
BEGIN
    -- Check if seats already exist for this event and showtime
    IF seats_exist_for_event(p_event_id, p_showtime) THEN
        RAISE NOTICE 'Seats already exist for event % at showtime %. Skipping insertion.', 
            p_event_id, p_showtime;
        RETURN 0;
    END IF;

    -- Insert seats based on row and zone logic
    FOR i IN 1..p_total_seats LOOP
        -- Determine row letter (A, B, C, D...)
        v_row_letter := CHR(65 + ((i - 1) / 5)); -- A=65, 5 seats per row
        v_seat_num := ((i - 1) % 5) + 1;

        -- Determine zone and price based on row
        CASE v_row_letter
            WHEN 'A' THEN 
                v_zone := 'VIP';
                v_price := 2000.00;
            WHEN 'B' THEN 
                v_zone := 'Premium';
                v_price := 1500.00;
            WHEN 'C' THEN 
                v_zone := 'Standard';
                v_price := 1000.00;
            ELSE 
                v_zone := 'Balcony';
                v_price := 800.00;
        END CASE;

        -- Insert seat
        INSERT INTO event_seats (
            event_id, 
            showtime, 
            seat_id, 
            zone, 
            row_number, 
            seat_number, 
            price, 
            status,
            created_at,
            updated_at
        ) VALUES (
            p_event_id,
            p_showtime,
            v_row_letter || v_seat_num,
            v_zone,
            v_row_letter,
            v_seat_num,
            v_price,
            'AVAILABLE',
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );

        v_inserted_count := v_inserted_count + 1;
    END LOOP;

    RAISE NOTICE 'Successfully inserted % seats for event % at showtime %', 
        v_inserted_count, p_event_id, p_showtime;
    
    RETURN v_inserted_count;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- PART 5: Populate sample data
-- ============================================================================

-- Insert seats for event_1 (ONE Fight Night 36)
-- Show date: Saturday 4 October 2025, 19:00 Bangkok time (UTC+7)
SELECT create_seats_for_event('event_1', '2025-10-04 19:00:00+07:00'::TIMESTAMPTZ, 20);

-- Insert seats for event_2 (MARIAH CAREY The Celebration of Mimi)
-- Show date: Saturday 11 October 2025, 18:00 Bangkok time (UTC+7)
SELECT create_seats_for_event('event_2', '2025-10-11 18:00:00+07:00'::TIMESTAMPTZ, 20);

-- ============================================================================
-- PART 6: Verification
-- ============================================================================

-- Verify table structures
DO $$
DECLARE
    v_bookings_exists BOOLEAN;
    v_booking_seats_exists BOOLEAN;
    v_event_seats_exists BOOLEAN;
    v_event_seats_count INTEGER;
BEGIN
    -- Check if tables exist
    SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'bookings') INTO v_bookings_exists;
    SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'booking_seats') INTO v_booking_seats_exists;
    SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'event_seats') INTO v_event_seats_exists;
    
    IF v_bookings_exists AND v_booking_seats_exists AND v_event_seats_exists THEN
        RAISE NOTICE '✅ All tables created successfully!';
    ELSE
        RAISE WARNING '⚠️  Some tables are missing!';
    END IF;
    
    -- Count seats
    SELECT COUNT(*) INTO v_event_seats_count FROM event_seats;
    RAISE NOTICE 'Total seats created: %', v_event_seats_count;
END $$;

-- Show schema summary
SELECT 
    table_name,
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns
WHERE table_name IN ('bookings', 'booking_seats', 'event_seats')
  AND column_name LIKE '%time%'
ORDER BY table_name, ordinal_position;

-- ============================================================================
-- Migration Complete! 🎉
-- ============================================================================
-- 
-- Summary:
-- ✅ Created bookings table with TIMESTAMPTZ columns
-- ✅ Created booking_seats table
-- ✅ Created event_seats table with all timestamp columns (TIMESTAMPTZ)
-- ✅ Created all necessary indexes for performance
-- ✅ Created helper functions for seat management
-- ✅ Populated sample data (40 seats across 2 events)
-- 
-- All timestamp columns use TIMESTAMPTZ (timezone-aware):
-- - bookings: showtime, created_at, updated_at
-- - booking_seats: created_at
-- - event_seats: showtime, reserved_at, reserved_until, sold_at, created_at, updated_at
-- 
-- ============================================================================

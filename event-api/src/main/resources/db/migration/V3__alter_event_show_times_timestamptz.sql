-- ============================================================================
-- Migration: Convert event_show_times.show_datetime to TIMESTAMPTZ
-- Description: Alters the element collection table to use timezone-aware
--              timestamps. Assumes existing naive timestamps are UTC.
-- Created: 2025-10-06
-- ============================================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'event_show_times'
          AND column_name = 'show_datetime'
          AND data_type = 'timestamp without time zone'
    ) THEN
        RAISE NOTICE 'Altering event_show_times.show_datetime to TIMESTAMPTZ...';
        ALTER TABLE event_show_times
            ALTER COLUMN show_datetime TYPE TIMESTAMPTZ
            USING (show_datetime AT TIME ZONE 'UTC');
        RAISE NOTICE 'event_show_times.show_datetime altered.';
    ELSE
        RAISE NOTICE 'event_show_times.show_datetime already TIMESTAMPTZ or column missing.';
    END IF;
END $$;

-- Optional verification (comment out in prod):
-- SELECT column_name, data_type FROM information_schema.columns
--   WHERE table_name = 'event_show_times' AND column_name = 'show_datetime';

-- ============================================================================
-- Migration Complete
-- ============================================================================

-- Rollback for V3__user_time_zone.sql (manual, run with the previous build).
ALTER TABLE user_settings
    DROP COLUMN timezone_offset_minutes;

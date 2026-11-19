-- =============================================================================
-- 羽鲸个人工具 0.1.0 — remember the client time zone offset (M3)
--
-- Reminder generation needs the user's local wall clock even when the user is
-- offline (plan 4.2, 6.3 "接口传偏移量"). Each authenticated write refreshes the
-- last offset; the hourly maintenance job then uses it to materialise future
-- reminders at the right local hour.
--
-- Rollback path (documented, manual):
--   1. Stop the server and deploy the previous application version.
--   2. Run db/rollback/V3__user_time_zone_rollback.sql against the database.
--   3. DELETE FROM flyway_schema_history WHERE version = '3';
-- =============================================================================

ALTER TABLE user_settings
    ADD COLUMN timezone_offset_minutes INT NOT NULL DEFAULT 0
        COMMENT 'minutes east of UTC reported by the most recent client request';

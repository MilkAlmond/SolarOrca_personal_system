-- =============================================================================
-- Manual rollback for V1__baseline.sql
--
-- Usage:
--   mysql -u <user> -p <database> < V1__baseline_rollback.sql
--   DELETE FROM flyway_schema_history WHERE version = '1';
--
-- This drops every table created by the 0.1.0 baseline. Take a backup first.
-- =============================================================================

DROP TABLE IF EXISTS push_subscriptions;
DROP TABLE IF EXISTS devices;
DROP TABLE IF EXISTS reminder_deliveries;
DROP TABLE IF EXISTS reminders;
DROP TABLE IF EXISTS notes;
DROP TABLE IF EXISTS task_milestones;
DROP TABLE IF EXISTS task_checkins;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS birthdays;
DROP TABLE IF EXISTS user_settings;
DROP TABLE IF EXISTS contacts;
DROP TABLE IF EXISTS honors;
DROP TABLE IF EXISTS profile_rows;
DROP TABLE IF EXISTS public_profile;
DROP TABLE IF EXISTS export_jobs;
DROP TABLE IF EXISTS sync_tombstones;
DROP TABLE IF EXISTS files;
DROP TABLE IF EXISTS sessions;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS app_versions;
DROP TABLE IF EXISTS users;

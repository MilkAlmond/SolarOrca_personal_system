-- =============================================================================
-- 羽鲸个人工具 0.1.0 — baseline schema (M1)
--
-- Rollback path (documented, manual):
--   1. Stop the server.
--   2. Run db/rollback/V1__baseline_rollback.sql against the same database.
--   3. Delete the applied history row:
--        DELETE FROM flyway_schema_history WHERE version = '1';
--   4. Start the previous application version.
--   This migration creates the complete 0.1.0 schema; it cannot be rolled back
--   in-place without dropping all business tables, so take a backup first.
-- =============================================================================

CREATE TABLE users (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    username              VARCHAR(20)  NOT NULL,
    password_hash         VARCHAR(100) NOT NULL,
    display_name          VARCHAR(40)  NOT NULL,
    role                  VARCHAR(16)  NOT NULL DEFAULT 'user',
    status                VARCHAR(16)  NOT NULL DEFAULT 'active',
    failed_login_count    INT          NOT NULL DEFAULT 0,
    locked_until          DATETIME(6)  NULL,
    deletion_requested_at DATETIME(6)  NULL,
    deletion_scheduled_at DATETIME(6)  NULL,
    last_login_at         DATETIME(6)  NULL,
    version               BIGINT       NOT NULL DEFAULT 0,
    created_at            DATETIME(6)  NOT NULL,
    updated_at            DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    KEY idx_users_status (status),
    KEY idx_users_deletion_due (deletion_scheduled_at),
    CONSTRAINT chk_users_role CHECK (role IN ('user', 'admin')),
    CONSTRAINT chk_users_status CHECK (status IN ('active', 'disabled'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE sessions (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    user_id       BIGINT      NOT NULL,
    token_hash    CHAR(64)    CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    device_id     VARCHAR(64) NOT NULL,
    platform      VARCHAR(16) NOT NULL DEFAULT 'unknown',
    app_version   VARCHAR(20) NULL,
    created_at    DATETIME(6) NOT NULL,
    last_seen_at  DATETIME(6) NOT NULL,
    revoked_at    DATETIME(6) NULL,
    revoke_reason VARCHAR(32) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sessions_token (token_hash),
    KEY idx_sessions_user_active (user_id, revoked_at),
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE files (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    owner_id     BIGINT       NULL,
    purpose      VARCHAR(16)  NOT NULL,
    storage_path VARCHAR(255) NOT NULL,
    content_type VARCHAR(32)  NOT NULL,
    size_bytes   BIGINT       NOT NULL,
    sha256       CHAR(64)     CHARACTER SET ascii COLLATE ascii_bin NULL,
    created_at   DATETIME(6)  NOT NULL,
    deleted_at   DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_files_storage_path (storage_path),
    KEY idx_files_owner_created (owner_id, created_at),
    KEY idx_files_deleted (deleted_at),
    CONSTRAINT fk_files_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_files_purpose CHECK (purpose IN ('avatar', 'bio', 'export'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Public page content is a global singleton (5.3): only admin may write it.
CREATE TABLE public_profile (
    id             TINYINT       NOT NULL DEFAULT 1,
    avatar_file_id BIGINT        NULL,
    title          VARCHAR(40)   NULL,
    subtitle       VARCHAR(60)   NULL,
    bio            VARCHAR(2000) NULL,
    version        BIGINT        NOT NULL DEFAULT 0,
    updated_by     BIGINT        NULL,
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_public_profile_singleton CHECK (id = 1),
    CONSTRAINT fk_public_profile_avatar FOREIGN KEY (avatar_file_id) REFERENCES files (id) ON DELETE SET NULL,
    CONSTRAINT fk_public_profile_updated_by FOREIGN KEY (updated_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE profile_rows (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    label      VARCHAR(40)  NULL,
    value      VARCHAR(200) NOT NULL,
    sort_order INT          NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_profile_rows_sort (sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE honors (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    name       VARCHAR(60) NOT NULL,
    awarded_on DATE        NOT NULL,
    sort_order INT         NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_honors_sort (sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE contacts (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    type         VARCHAR(16)  NOT NULL,
    display_text VARCHAR(60)  NOT NULL,
    target       VARCHAR(300) NULL,
    is_public    BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order   INT          NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_contacts_sort (sort_order),
    CONSTRAINT chk_contacts_type CHECK (type IN ('email', 'social', 'link'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE user_settings (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    owner_id           BIGINT      NOT NULL,
    reminders_enabled  BOOLEAN     NOT NULL DEFAULT TRUE,
    version            BIGINT      NOT NULL DEFAULT 0,
    created_at         DATETIME(6) NOT NULL,
    updated_at         DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_settings_owner (owner_id),
    KEY idx_user_settings_owner_updated (owner_id, updated_at),
    CONSTRAINT fk_user_settings_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE birthdays (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    owner_id     BIGINT       NOT NULL,
    name         VARCHAR(40)  NOT NULL,
    category     VARCHAR(16)  NOT NULL,
    calendar_type VARCHAR(8)  NOT NULL,
    solar_date   DATE         NULL,
    lunar_month  TINYINT      NULL,
    lunar_day    TINYINT      NULL,
    lunar_leap   BOOLEAN      NOT NULL DEFAULT FALSE,
    relation_tag VARCHAR(20)  NULL,
    advance_days TINYINT      NOT NULL DEFAULT 0,
    note         VARCHAR(200) NULL,
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_birthdays_owner_updated (owner_id, updated_at),
    CONSTRAINT fk_birthdays_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_birthdays_category CHECK (category IN ('birthday', 'anniversary')),
    CONSTRAINT chk_birthdays_calendar CHECK (calendar_type IN ('solar', 'lunar')),
    CONSTRAINT chk_birthdays_advance CHECK (advance_days BETWEEN 0 AND 30),
    CONSTRAINT chk_birthdays_date CHECK (
        (calendar_type = 'solar' AND solar_date IS NOT NULL AND lunar_month IS NULL AND lunar_day IS NULL)
        OR (calendar_type = 'lunar' AND solar_date IS NULL AND lunar_month BETWEEN 1 AND 12 AND lunar_day BETWEEN 1 AND 30)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE courses (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    owner_id        BIGINT      NOT NULL,
    name            VARCHAR(40) NOT NULL,
    teacher         VARCHAR(20) NULL,
    location        VARCHAR(40) NULL,
    weekday         TINYINT     NOT NULL,
    start_time      TIME        NOT NULL,
    end_time        TIME        NOT NULL,
    repeat_type     VARCHAR(8)  NOT NULL,
    course_date     DATE        NULL,
    effective_from  DATE        NULL,
    effective_to    DATE        NULL,
    remind_minutes  INT         NOT NULL DEFAULT 10,
    version         BIGINT      NOT NULL DEFAULT 0,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_courses_owner_updated (owner_id, updated_at),
    KEY idx_courses_owner_weekday (owner_id, weekday, start_time),
    CONSTRAINT fk_courses_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_courses_weekday CHECK (weekday BETWEEN 1 AND 7),
    CONSTRAINT chk_courses_time CHECK (end_time > start_time),
    CONSTRAINT chk_courses_repeat CHECK (repeat_type IN ('once', 'weekly')),
    CONSTRAINT chk_courses_once_date CHECK (repeat_type = 'weekly' OR course_date IS NOT NULL),
    CONSTRAINT chk_courses_remind CHECK (remind_minutes IN (0, 5, 10, 15, 30))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE tasks (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    owner_id      BIGINT      NOT NULL,
    name          VARCHAR(40) NOT NULL,
    period        VARCHAR(8)  NOT NULL,
    weekly_target TINYINT     NULL,
    start_date    DATE        NOT NULL,
    remind_time   TIME        NULL,
    enabled       BOOLEAN     NOT NULL DEFAULT TRUE,
    version       BIGINT      NOT NULL DEFAULT 0,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_tasks_owner_updated (owner_id, updated_at),
    CONSTRAINT fk_tasks_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_tasks_period CHECK (period IN ('daily', 'weekly')),
    CONSTRAINT chk_tasks_target CHECK (
        (period = 'daily' AND weekly_target IS NULL)
        OR (period = 'weekly' AND weekly_target BETWEEN 1 AND 7)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE task_checkins (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    owner_id     BIGINT      NOT NULL,
    task_id      BIGINT      NOT NULL,
    checkin_date DATE        NOT NULL,
    version      BIGINT      NOT NULL DEFAULT 0,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_checkins_task_date (task_id, checkin_date),
    KEY idx_task_checkins_owner_date (owner_id, checkin_date),
    CONSTRAINT fk_task_checkins_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_task_checkins_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE task_milestones (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    owner_id        BIGINT      NOT NULL,
    task_id         BIGINT      NOT NULL,
    milestone_days  INT         NOT NULL,
    reached_on      DATE        NOT NULL,
    version         BIGINT      NOT NULL DEFAULT 0,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_milestones_task_days (task_id, milestone_days),
    KEY idx_task_milestones_owner_updated (owner_id, updated_at),
    CONSTRAINT fk_task_milestones_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_task_milestones_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    CONSTRAINT chk_task_milestones_days CHECK (milestone_days IN (7, 30, 100))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE notes (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    owner_id   BIGINT       NOT NULL,
    title      VARCHAR(60)  NOT NULL,
    body       TEXT         NULL,
    category   VARCHAR(20)  NULL,
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notes_owner_updated (owner_id, updated_at),
    CONSTRAINT fk_notes_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE reminders (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    owner_id        BIGINT       NOT NULL,
    source          VARCHAR(16)  NOT NULL,
    source_id       BIGINT       NULL,
    title           VARCHAR(80)  NOT NULL,
    trigger_at      DATETIME(6)  NOT NULL,
    advance_minutes INT          NOT NULL DEFAULT 0,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    delivery_status VARCHAR(16)  NOT NULL DEFAULT 'pending',
    dedupe_key      VARCHAR(160) NOT NULL,
    sent_at         DATETIME(6)  NULL,
    failed_at       DATETIME(6)  NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reminders_owner_dedupe (owner_id, dedupe_key),
    KEY idx_reminders_owner_updated (owner_id, updated_at),
    KEY idx_reminders_due (delivery_status, trigger_at),
    CONSTRAINT fk_reminders_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_reminders_source CHECK (source IN ('course', 'birthday', 'task', 'manual')),
    CONSTRAINT chk_reminders_status CHECK (delivery_status IN ('pending', 'sent', 'failed'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE reminder_deliveries (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    owner_id      BIGINT      NOT NULL,
    reminder_id   BIGINT      NOT NULL,
    device_id     VARCHAR(64) NOT NULL,
    attempted_at  DATETIME(6) NOT NULL,
    result        VARCHAR(8)  NOT NULL,
    attempt_count INT         NOT NULL DEFAULT 1,
    version       BIGINT      NOT NULL DEFAULT 0,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reminder_deliveries_reminder_device (reminder_id, device_id),
    KEY idx_reminder_deliveries_owner_updated (owner_id, updated_at),
    CONSTRAINT fk_reminder_deliveries_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_reminder_deliveries_reminder FOREIGN KEY (reminder_id) REFERENCES reminders (id) ON DELETE CASCADE,
    CONSTRAINT chk_reminder_deliveries_result CHECK (result IN ('sent', 'failed'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE devices (
    id                    BIGINT      NOT NULL AUTO_INCREMENT,
    owner_id              BIGINT      NOT NULL,
    device_id             VARCHAR(64) NOT NULL,
    platform              VARCHAR(16) NOT NULL,
    app_version           VARCHAR(20) NULL,
    notifications_enabled BOOLEAN     NOT NULL DEFAULT TRUE,
    last_seen_at          DATETIME(6) NOT NULL,
    version               BIGINT      NOT NULL DEFAULT 0,
    created_at            DATETIME(6) NOT NULL,
    updated_at            DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_devices_owner_device (owner_id, device_id),
    KEY idx_devices_owner_updated (owner_id, updated_at),
    CONSTRAINT fk_devices_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE push_subscriptions (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    owner_id   BIGINT       NOT NULL,
    device_id  VARCHAR(64)  NOT NULL,
    endpoint   VARCHAR(500) NOT NULL,
    p256dh     VARCHAR(200) NOT NULL,
    auth_key   VARCHAR(100) NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_push_subscriptions_owner_device (owner_id, device_id),
    KEY idx_push_subscriptions_owner_updated (owner_id, updated_at),
    CONSTRAINT fk_push_subscriptions_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Separate 30-day tombstones: business rows are physically deleted (5.3), while
-- stale offline writes are still rejected during the retention window (5.4, 6.3).
CREATE TABLE sync_tombstones (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    owner_id      BIGINT      NOT NULL,
    entity_type   VARCHAR(24) NOT NULL,
    entity_id     BIGINT      NOT NULL,
    entity_version BIGINT     NOT NULL,
    deleted_at    DATETIME(6) NOT NULL,
    version       BIGINT     NOT NULL DEFAULT 0,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sync_tombstones_owner_entity (owner_id, entity_type, entity_id),
    KEY idx_sync_tombstones_deleted (deleted_at),
    CONSTRAINT fk_sync_tombstones_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE app_versions (
    platform              VARCHAR(16)  NOT NULL,
    latest_version        VARCHAR(20)  NOT NULL,
    min_supported_version VARCHAR(20)  NOT NULL,
    download_url          VARCHAR(500) NULL,
    notes                 VARCHAR(500) NULL,
    updated_at            DATETIME(6)  NOT NULL,
    PRIMARY KEY (platform),
    CONSTRAINT chk_app_versions_platform CHECK (platform IN ('web', 'windows', 'linux', 'android'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO app_versions (platform, latest_version, min_supported_version, download_url, notes, updated_at)
VALUES ('web', '0.1.0', '0.1.0', NULL, NULL, UTC_TIMESTAMP(6)),
       ('windows', '0.1.0', '0.1.0', NULL, NULL, UTC_TIMESTAMP(6)),
       ('linux', '0.1.0', '0.1.0', NULL, NULL, UTC_TIMESTAMP(6)),
       ('android', '0.1.0', '0.1.0', NULL, NULL, UTC_TIMESTAMP(6));

CREATE TABLE audit_logs (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT       NULL,
    action        VARCHAR(32)  NOT NULL,
    target        VARCHAR(120) NULL,
    result        VARCHAR(16)  NOT NULL,
    ip            VARCHAR(45)  NULL,
    detail        VARCHAR(255) NULL,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_audit_logs_created (created_at),
    KEY idx_audit_logs_actor_created (actor_user_id, created_at),
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE export_jobs (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    owner_id            BIGINT      NOT NULL,
    status              VARCHAR(16) NOT NULL DEFAULT 'pending',
    file_id             BIGINT      NULL,
    download_token_hash CHAR(64)    CHARACTER SET ascii COLLATE ascii_bin NULL,
    expires_at          DATETIME(6) NULL,
    created_at          DATETIME(6) NOT NULL,
    completed_at        DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_export_jobs_token (download_token_hash),
    KEY idx_export_jobs_owner_created (owner_id, created_at),
    CONSTRAINT fk_export_jobs_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_export_jobs_file FOREIGN KEY (file_id) REFERENCES files (id) ON DELETE SET NULL,
    CONSTRAINT chk_export_jobs_status CHECK (status IN ('pending', 'ready', 'failed', 'expired'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

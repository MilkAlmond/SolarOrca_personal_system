-- V2 adds the ordered gallery for the public profile (F1 supports 0-6 PNG images).
--
-- Rollback: run db/rollback/V2__public_profile_images_rollback.sql, then delete
-- the version '2' row from flyway_schema_history.

CREATE TABLE profile_images (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    file_id    BIGINT      NOT NULL,
    sort_order INT         NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_profile_images_file (file_id),
    KEY idx_profile_images_sort (sort_order),
    CONSTRAINT fk_profile_images_file FOREIGN KEY (file_id) REFERENCES files (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

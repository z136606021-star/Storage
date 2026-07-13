-- Fail fast when normalized emails are duplicated; do not auto-modify existing accounts.
CREATE TEMPORARY TABLE sys_user_duplicate_email_guard (
    must_be_empty INT NOT NULL,
    CONSTRAINT chk_no_duplicate_user_emails CHECK (must_be_empty <> 1)
);

INSERT INTO sys_user_duplicate_email_guard (must_be_empty)
SELECT 1
FROM sys_user
WHERE email IS NOT NULL
GROUP BY email
HAVING COUNT(*) > 1
LIMIT 1;

DROP TEMPORARY TABLE sys_user_duplicate_email_guard;

SET @index_count := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_user'
      AND INDEX_NAME = 'uk_sys_user_email'
);

SET @sql := IF(
    @index_count = 0,
    'ALTER TABLE sys_user ADD CONSTRAINT uk_sys_user_email UNIQUE (email)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

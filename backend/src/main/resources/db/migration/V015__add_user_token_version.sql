SET @token_version_exists := (

    SELECT COUNT(*)

    FROM INFORMATION_SCHEMA.COLUMNS

    WHERE TABLE_SCHEMA = DATABASE()

      AND TABLE_NAME = 'sys_user'

      AND COLUMN_NAME = 'token_version'

);



SET @sql := IF(

    @token_version_exists = 0,

    'ALTER TABLE sys_user ADD COLUMN token_version INT NOT NULL DEFAULT 0 COMMENT ''JWT token version'' AFTER password_hash',

    'SELECT 1'

);

PREPARE stmt FROM @sql;

EXECUTE stmt;

DEALLOCATE PREPARE stmt;



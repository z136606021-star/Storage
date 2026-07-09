SET @col_nullable := (
    SELECT IS_NULLABLE
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'warehouse_bin'
      AND COLUMN_NAME = 'col_no'
);

SET @sql := IF(
    @col_nullable = 'NO',
    'ALTER TABLE warehouse_bin MODIFY col_no INT NULL COMMENT ''列''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @level_nullable := (
    SELECT IS_NULLABLE
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'warehouse_bin'
      AND COLUMN_NAME = 'level_no'
);

SET @sql := IF(
    @level_nullable = 'NO',
    'ALTER TABLE warehouse_bin MODIFY level_no INT NULL COMMENT ''层''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_count := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'warehouse_bin'
      AND INDEX_NAME = 'uk_row_col_level'
);

SET @sql := IF(
    @index_count > 0,
    'ALTER TABLE warehouse_bin DROP INDEX uk_row_col_level',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

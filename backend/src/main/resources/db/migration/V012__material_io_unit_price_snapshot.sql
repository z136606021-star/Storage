SET @unit_price_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'material_io_record'
      AND COLUMN_NAME = 'unit_price'
);

SET @sql := IF(
    @unit_price_exists = 0,
    'ALTER TABLE material_io_record ADD COLUMN unit_price DECIMAL(10, 2) NULL COMMENT ''单价快照'' AFTER quantity',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

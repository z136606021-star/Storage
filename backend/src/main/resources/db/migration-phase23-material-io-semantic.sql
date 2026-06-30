SET @ddl := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE material_io_record ADD COLUMN purpose VARCHAR(32) NULL COMMENT ''用途码'' AFTER remark',
        'SELECT 1')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material_io_record' AND COLUMN_NAME = 'purpose'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
    SELECT IF(COUNT(*) = 0,
        'CREATE INDEX idx_material_io_purpose ON material_io_record (purpose)',
        'SELECT 1')
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material_io_record' AND INDEX_NAME = 'idx_material_io_purpose'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

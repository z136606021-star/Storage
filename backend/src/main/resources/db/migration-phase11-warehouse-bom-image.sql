SET @ddl := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE warehouse_bom ADD COLUMN image_object_key VARCHAR(512) NULL COMMENT ''MinIO 对象键''',
        'SELECT 1')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'warehouse_bom' AND COLUMN_NAME = 'image_object_key'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

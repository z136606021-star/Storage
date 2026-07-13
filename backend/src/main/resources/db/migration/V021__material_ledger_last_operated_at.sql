SET @last_operated_at_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'material_ledger'
      AND COLUMN_NAME = 'last_operated_at'
);

SET @sql := IF(
    @last_operated_at_exists = 0,
    'ALTER TABLE material_ledger ADD COLUMN last_operated_at DATETIME NULL COMMENT ''最后一次出入库操作时间'' AFTER unit_price',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE material_ledger ml
INNER JOIN (
    SELECT material_ledger_id, MAX(operated_at) AS max_operated_at
    FROM material_io_record
    GROUP BY material_ledger_id
) io ON ml.id = io.material_ledger_id
SET ml.last_operated_at = io.max_operated_at;

SET @index_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'material_ledger'
      AND INDEX_NAME = 'idx_material_ledger_last_operated_at'
);

SET @sql := IF(
    @index_exists = 0,
    'CREATE INDEX idx_material_ledger_last_operated_at ON material_ledger (last_operated_at, id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

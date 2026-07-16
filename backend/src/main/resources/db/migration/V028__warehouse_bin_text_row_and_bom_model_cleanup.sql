-- Bin 位的“排”允许使用“铁柜”等文本；物料清单不再维护型号/规格。

SET @row_no_is_varchar := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'warehouse_bin'
      AND COLUMN_NAME = 'row_no'
      AND DATA_TYPE = 'varchar'
);

SET @sql := IF(
    @row_no_is_varchar = 0,
    'ALTER TABLE warehouse_bin MODIFY row_no VARCHAR(32) NOT NULL COMMENT ''排（支持文本）''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @bom_model_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'warehouse_bom'
      AND COLUMN_NAME = 'model'
);

SET @sql := IF(
    @bom_model_exists > 0,
    'ALTER TABLE warehouse_bom DROP COLUMN model',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

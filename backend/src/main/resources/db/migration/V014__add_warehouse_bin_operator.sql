SET @operator_user_id_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'warehouse_bin'
      AND COLUMN_NAME = 'operator_user_id'
);

SET @sql := IF(
    @operator_user_id_exists = 0,
    'ALTER TABLE warehouse_bin ADD COLUMN operator_user_id BIGINT NULL COMMENT ''操作人用户 ID'' AFTER remark',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @operator_name_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'warehouse_bin'
      AND COLUMN_NAME = 'operator_name'
);

SET @sql := IF(
    @operator_name_exists = 0,
    'ALTER TABLE warehouse_bin ADD COLUMN operator_name VARCHAR(64) NULL COMMENT ''操作人快照'' AFTER operator_user_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'warehouse_bin'
      AND CONSTRAINT_NAME = 'fk_warehouse_bin_operator'
);

SET @sql := IF(
    @fk_exists = 0,
    'ALTER TABLE warehouse_bin ADD CONSTRAINT fk_warehouse_bin_operator FOREIGN KEY (operator_user_id) REFERENCES sys_user (id) ON DELETE SET NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

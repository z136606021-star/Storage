-- 第三十六期：动态菜单路由组件 Key
SET @column_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_menu'
      AND COLUMN_NAME = 'component_key'
);

SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE sys_menu ADD COLUMN component_key VARCHAR(128) NULL COMMENT ''前端组件 Key'' AFTER path',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_menu SET component_key = 'ShellPlaceholder' WHERE id IN (10, 21, 22, 23, 121, 122, 123, 150, 161, 162, 163, 170, 181, 182, 183) AND component_key IS NULL;
UPDATE sys_menu SET component_key = 'MaterialLedger' WHERE id = 111 AND component_key IS NULL;
UPDATE sys_menu SET component_key = 'MaterialIo' WHERE id = 112 AND component_key IS NULL;
UPDATE sys_menu SET component_key = 'SafetyStock' WHERE id = 113 AND component_key IS NULL;
UPDATE sys_menu SET component_key = 'InventoryStats' WHERE id = 117 AND component_key IS NULL;
UPDATE sys_menu SET component_key = 'BinManage' WHERE id = 115 AND component_key IS NULL;
UPDATE sys_menu SET component_key = 'BomManage' WHERE id = 116 AND component_key IS NULL;
UPDATE sys_menu SET component_key = 'SystemManageLayout' WHERE id = 201 AND component_key IS NULL;
UPDATE sys_menu SET component_key = 'RoleManagePanel' WHERE id = 202 AND component_key IS NULL;
UPDATE sys_menu SET component_key = 'MenuManagePanel' WHERE id = 203 AND component_key IS NULL;
UPDATE sys_menu SET component_key = 'CustomerManage' WHERE id = 204 AND component_key IS NULL;

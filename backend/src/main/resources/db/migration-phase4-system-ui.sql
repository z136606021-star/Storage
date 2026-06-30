-- 第四期系统管理 UI 改版：用户扩展字段、角色状态、侧栏菜单调整
SET NAMES utf8mb4;

SET @ddl := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE sys_user ADD COLUMN email VARCHAR(128) NULL COMMENT ''邮箱'' AFTER display_name',
        'SELECT 1')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'email'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE sys_user ADD COLUMN phone VARCHAR(32) NULL COMMENT ''手机号'' AFTER email',
        'SELECT 1')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'phone'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE sys_role ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT ''1启用 0停用'' AFTER name',
        'SELECT 1')
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_role' AND COLUMN_NAME = 'status'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_menu SET visible = 0 WHERE id IN (202, 203);

INSERT IGNORE INTO sys_menu (id, parent_id, menu_type, name, permission, path, icon, visible, sort_order) VALUES
(204, 200, 'MENU', '客户管理', 'system:customer:read', '/system/customers', NULL, 1, 20);

INSERT IGNORE INTO sys_menu (id, parent_id, menu_type, name, permission, path, icon, visible, sort_order) VALUES
(244, NULL, 'MENU', '客户写', 'system:customer:write', NULL, NULL, 0, 244);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 204),
(1, 244);

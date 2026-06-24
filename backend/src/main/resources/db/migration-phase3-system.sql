-- 第三期系统管理：菜单树扩展、USER 角色、系统管理权限
USE storage;

SET NAMES utf8mb4;

ALTER TABLE sys_menu
    ADD COLUMN menu_type VARCHAR(16) NOT NULL DEFAULT 'MENU' COMMENT 'CATALOG/MENU' AFTER parent_id;

ALTER TABLE sys_menu
    ADD COLUMN icon VARCHAR(64) NULL COMMENT 'Ant Design 图标名' AFTER path;

ALTER TABLE sys_menu
    ADD COLUMN visible TINYINT NOT NULL DEFAULT 1 COMMENT '1显示 0隐藏' AFTER icon;

ALTER TABLE sys_menu
    MODIFY COLUMN permission VARCHAR(128) NULL COMMENT '权限标识，目录可为空';

INSERT IGNORE INTO sys_role (id, code, name) VALUES
(2, 'USER', '普通用户');

UPDATE sys_menu SET visible = 0, menu_type = 'MENU', permission = NULL WHERE id IN (1, 2, 3);
UPDATE sys_menu SET permission = 'warehouse:material-ledger:write', visible = 0 WHERE id = 2;
UPDATE sys_menu SET permission = 'platform:file:upload', visible = 0 WHERE id = 3;
DELETE FROM sys_role_menu WHERE menu_id = 1;

INSERT IGNORE INTO sys_menu (id, parent_id, menu_type, name, permission, path, icon, visible, sort_order) VALUES
(100, NULL, 'CATALOG', '资源管理', NULL, NULL, 'DatabaseOutlined', 1, 10),
(110, 100, 'CATALOG', '仓库管理', NULL, NULL, NULL, 1, 10),
(111, 110, 'MENU', '物料台账', 'warehouse:material-ledger:read', '/warehouse/material-ledger', NULL, 1, 10),
(112, 110, 'MENU', '物料出入库', 'warehouse:material-io:read', NULL, NULL, 0, 20),
(113, 110, 'MENU', '安全库存管理', 'warehouse:safety-stock:read', NULL, NULL, 0, 30),
(200, NULL, 'CATALOG', '系统管理', NULL, NULL, 'SettingOutlined', 1, 90),
(201, 200, 'MENU', '用户管理', 'system:user:read', '/system/users', NULL, 1, 10),
(202, 200, 'MENU', '角色管理', 'system:role:read', '/system/roles', NULL, 1, 20),
(203, 200, 'MENU', '菜单管理', 'system:menu:read', '/system/menus', NULL, 1, 30);

INSERT IGNORE INTO sys_menu (id, parent_id, menu_type, name, permission, path, icon, visible, sort_order) VALUES
(214, NULL, 'MENU', '用户写', 'system:user:write', NULL, NULL, 0, 214),
(224, NULL, 'MENU', '角色写', 'system:role:write', NULL, NULL, 0, 224),
(234, NULL, 'MENU', '菜单写', 'system:menu:write', NULL, NULL, 0, 234);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 100), (1, 110), (1, 111), (1, 112), (1, 113),
(1, 200), (1, 201), (1, 202), (1, 203),
(1, 2), (1, 3), (1, 214), (1, 224), (1, 234);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(2, 111);

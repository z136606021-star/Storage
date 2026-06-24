-- 第七期：壳层菜单补全 path，支持侧栏跳转与 Tab 同步
USE storage;

SET NAMES utf8mb4;

UPDATE sys_menu SET path = '/platform/personal' WHERE id = 10;
UPDATE sys_menu SET path = '/platform/project/create' WHERE id = 21;
UPDATE sys_menu SET path = '/platform/project/set' WHERE id = 22;
UPDATE sys_menu SET path = '/platform/procurement' WHERE id = 121;
UPDATE sys_menu SET path = '/platform/procurement/create' WHERE id = 122;
UPDATE sys_menu SET path = '/platform/procurement/mine' WHERE id = 123;
UPDATE sys_menu SET path = '/platform/design' WHERE id = 150;
UPDATE sys_menu SET path = '/platform/skill/matrix' WHERE id = 161;
UPDATE sys_menu SET path = '/platform/skill/talent' WHERE id = 162;
UPDATE sys_menu SET path = '/platform/skill/training' WHERE id = 163;
UPDATE sys_menu SET path = '/platform/experience' WHERE id = 170;
UPDATE sys_menu SET path = '/platform/finance/dashboard' WHERE id = 181;
UPDATE sys_menu SET path = '/platform/finance/settlement' WHERE id = 182;
UPDATE sys_menu SET path = '/platform/finance/cost' WHERE id = 183;

-- 项目中心 Tab 默认页（隐藏菜单，仅权限 + 路由）
INSERT IGNORE INTO sys_menu (id, parent_id, menu_type, name, permission, path, icon, visible, sort_order) VALUES
(23, NULL, 'MENU', '项目中心读', 'platform:project:read', '/platform/project', NULL, 0, 23);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 23);

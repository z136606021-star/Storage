-- 第六期：完整平台壳层导航 + 仓库配置管理菜单
USE storage;

SET NAMES utf8mb4;

-- 平台壳层（ADMIN 可见，无 path 的 MENU 点击仍提示开发中）
INSERT IGNORE INTO sys_menu (id, parent_id, menu_type, name, permission, path, icon, visible, sort_order) VALUES
(10, NULL, 'MENU', '个人中心', 'platform:personal:read', NULL, 'HomeOutlined', 1, 1),
(20, NULL, 'CATALOG', '项目管理中心', NULL, NULL, 'ProjectOutlined', 1, 2),
(21, 20, 'MENU', '新建项目', 'platform:project:create', NULL, NULL, 1, 10),
(22, 20, 'MENU', '项目集', 'platform:project:set', NULL, NULL, 1, 20),
(120, 100, 'CATALOG', '采购管理', NULL, NULL, NULL, 1, 5),
(121, 120, 'MENU', '采购管理', 'procurement:read', NULL, NULL, 1, 10),
(122, 120, 'MENU', '新增采购需求', 'procurement:create', NULL, NULL, 1, 20),
(123, 120, 'MENU', '我的采购需求', 'procurement:mine', NULL, NULL, 1, 30),
(114, 110, 'CATALOG', '配置管理', NULL, NULL, NULL, 1, 40),
(115, 114, 'MENU', 'Bin位管理', 'warehouse:bin:read', '/warehouse/config/bin', NULL, 1, 10),
(116, 114, 'MENU', '物料清单管理', 'warehouse:bom:read', '/warehouse/config/bom', NULL, 1, 20),
(150, NULL, 'MENU', '设计指引', 'platform:design:read', NULL, 'BulbOutlined', 1, 3),
(160, NULL, 'CATALOG', '技能中心', NULL, NULL, 'ToolOutlined', 1, 4),
(161, 160, 'MENU', '技能矩阵', 'skill:matrix:read', NULL, NULL, 1, 10),
(162, 160, 'MENU', '人才画像', 'skill:talent:read', NULL, NULL, 1, 20),
(163, 160, 'MENU', '人才培训计划', 'skill:training:read', NULL, NULL, 1, 30),
(170, NULL, 'MENU', '经验库', 'platform:experience:read', NULL, 'BookOutlined', 1, 5),
(180, NULL, 'CATALOG', '财务中心', NULL, NULL, 'ShoppingOutlined', 1, 6),
(181, 180, 'MENU', '业务分析看板', 'finance:dashboard:read', NULL, NULL, 1, 10),
(182, 180, 'MENU', '财务结算中心', 'finance:settlement:read', NULL, NULL, 1, 20),
(183, 180, 'MENU', '成本分析中心', 'finance:cost:read', NULL, NULL, 1, 30);

-- 仓库既有项：显示并补全路由
UPDATE sys_menu SET visible = 1, path = '/warehouse/material-io' WHERE id = 112;
UPDATE sys_menu SET visible = 1, path = '/warehouse/safety-stock' WHERE id = 113;

-- 隐藏写权限
INSERT IGNORE INTO sys_menu (id, parent_id, menu_type, name, permission, path, icon, visible, sort_order) VALUES
(254, NULL, 'MENU', 'Bin位写', 'warehouse:bin:write', NULL, NULL, 0, 254),
(255, NULL, 'MENU', '物料清单写', 'warehouse:bom:write', NULL, NULL, 0, 255);

-- ADMIN 角色授权
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 10), (1, 20), (1, 21), (1, 22),
(1, 120), (1, 121), (1, 122), (1, 123),
(1, 114), (1, 115), (1, 116),
(1, 150), (1, 160), (1, 161), (1, 162), (1, 163),
(1, 170), (1, 180), (1, 181), (1, 182), (1, 183),
(1, 254), (1, 255);

-- 出入库项目编号（轻量关联，待项目管理模块上线后可对接）
-- MySQL 8 不支持 ADD COLUMN IF NOT EXISTS；依赖 spring.sql.init.continue-on-error 幂等重跑
ALTER TABLE material_io_record
    ADD COLUMN project_ref VARCHAR(128) NULL COMMENT '项目编号/名称（项目领用）' AFTER purpose;

CREATE INDEX idx_material_io_project_ref ON material_io_record (project_ref);

-- 库存统计菜单
INSERT IGNORE INTO sys_menu (id, parent_id, menu_type, name, permission, path, icon, visible, sort_order) VALUES
(117, 110, 'MENU', '库存统计', 'warehouse:stats:read', '/warehouse/inventory-stats', NULL, 1, 25);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 117);

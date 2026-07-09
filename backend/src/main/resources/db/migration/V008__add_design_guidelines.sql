CREATE TABLE design_product_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_code VARCHAR(32) NOT NULL COMMENT '产品类型编号',
    type_name VARCHAR(64) NOT NULL COMMENT '产品类型',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    operator_user_id BIGINT NULL COMMENT '操作人用户 ID',
    operator_name VARCHAR(64) NULL COMMENT '操作人快照',
    operated_at DATETIME NULL COMMENT '操作时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_design_product_type_code (type_code),
    INDEX idx_design_product_type_name (type_name),
    CONSTRAINT fk_design_product_type_operator FOREIGN KEY (operator_user_id) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE design_stage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sort_order INT NOT NULL COMMENT '顺序',
    stage_name VARCHAR(64) NOT NULL COMMENT '阶段',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    operator_user_id BIGINT NULL COMMENT '操作人用户 ID',
    operator_name VARCHAR(64) NULL COMMENT '操作人快照',
    operated_at DATETIME NULL COMMENT '操作时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_design_stage_sort_order (sort_order),
    INDEX idx_design_stage_name (stage_name),
    CONSTRAINT fk_design_stage_operator FOREIGN KEY (operator_user_id) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE design_guide (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_type_id BIGINT NOT NULL COMMENT '产品类型 ID',
    product_type_code VARCHAR(32) NOT NULL COMMENT '产品类型编号快照',
    product_type_name VARCHAR(64) NOT NULL COMMENT '产品类型快照',
    stage_id BIGINT NOT NULL COMMENT '项目阶段 ID',
    stage_name VARCHAR(64) NOT NULL COMMENT '阶段快照',
    scope VARCHAR(64) NOT NULL COMMENT '适用范围',
    check_item VARCHAR(500) NOT NULL COMMENT '检查项',
    remark VARCHAR(500) NULL COMMENT '备注',
    recorder_user_id BIGINT NULL COMMENT '记录人用户 ID',
    recorder_name VARCHAR(64) NULL COMMENT '记录人快照',
    recorded_at DATETIME NULL COMMENT '记录时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_design_guide_natural (product_type_id, stage_id, scope, check_item),
    INDEX idx_design_guide_product_type (product_type_id),
    INDEX idx_design_guide_stage (stage_id),
    INDEX idx_design_guide_scope (scope),
    CONSTRAINT fk_design_guide_product_type FOREIGN KEY (product_type_id) REFERENCES design_product_type (id),
    CONSTRAINT fk_design_guide_stage FOREIGN KEY (stage_id) REFERENCES design_stage (id),
    CONSTRAINT fk_design_guide_recorder FOREIGN KEY (recorder_user_id) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE sys_menu
SET component_key = 'views/design/DesignGuideView.vue'
WHERE id = 150 OR permission = 'platform:design:read';

DELETE FROM sys_role_menu WHERE role_id = 2 AND menu_id = 150;

INSERT INTO sys_menu (parent_id, menu_type, name, permission, path, component_key, icon, visible, sort_order)
SELECT NULL, 'MENU', '设计指引写', 'platform:design:write', NULL, NULL, NULL, 0, 258
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'platform:design:write');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE permission IN ('platform:design:read', 'platform:design:write')
AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.id
);

INSERT INTO design_product_type (id, type_code, type_name, enabled, operator_user_id, operator_name, operated_at) VALUES
(1, 'A01', '机器人', 1, 1, 'Mandy Liu', '2026-01-12 00:00:00'),
(2, 'A02', '传送带', 1, 1, 'Mandy Liu', '2026-01-12 00:00:00'),
(3, 'A03', '防护栏', 0, 1, 'Mandy Liu', '2026-01-12 00:00:00'),
(4, 'A04', '电柜', 1, 1, 'Mandy Liu', '2026-01-12 00:00:00'),
(5, 'A05', '中转台', 1, 1, 'Mandy Liu', '2026-01-12 00:00:00'),
(6, 'A06', '控制面板', 1, 1, 'Mandy Liu', '2026-01-12 00:00:00'),
(7, 'A07', 'CCD影像显示', 1, 1, 'Mandy Liu', '2026-01-12 00:00:00'),
(8, 'A08', '扫描显示器', 1, 1, 'Mandy Liu', '2026-01-12 00:00:00'),
(9, 'A09', 'MES数据处理', 1, 1, 'Mandy Liu', '2026-01-12 00:00:00'),
(10, 'A10', 'MES显示器', 1, 1, 'Mandy Liu', '2026-01-12 00:00:00');

INSERT INTO design_stage (id, sort_order, stage_name, enabled, operator_user_id, operator_name, operated_at) VALUES
(1, 1, '设计', 1, 1, 'Mandy Liu', '2026-01-12 00:00:00'),
(2, 2, '选型', 1, 1, 'Mandy Liu', '2026-01-12 00:00:00'),
(3, 3, '配置', 0, 1, 'Mandy Liu', '2026-01-12 00:00:00'),
(4, 4, '安装', 1, 1, 'Mandy Liu', '2026-01-12 00:00:00'),
(5, 5, '调试', 1, 1, 'Mandy Liu', '2026-01-12 00:00:00');

INSERT INTO design_guide (
    product_type_id, product_type_code, product_type_name, stage_id, stage_name,
    scope, check_item, remark, recorder_user_id, recorder_name, recorded_at
) VALUES
(1, 'A01', '机器人', 1, '设计', 'Common', '设计需要考虑机器人控制柜线缆的摆放位置', NULL, 1, 'Ben Zou', '2025-08-20 00:00:00'),
(2, 'A02', '传送带', 2, '选型', 'Common', '手掌重量及负重分析', NULL, 1, 'Ben Zou', '2025-08-20 00:00:00'),
(3, 'A03', '防护栏', 2, '选型', 'Xpeng', '机器人臂展覆盖要求', NULL, 1, 'Ben Zou', '2025-08-20 00:00:00'),
(4, 'A04', '电柜', 3, '配置', 'IC', '手掌延伸臂长度要求', NULL, 1, 'Ben Zou', '2025-08-20 00:00:00'),
(5, 'A05', '中转台', 3, '配置', 'Xpeng', '手掌旋转干涉确认', NULL, 1, 'Ben Zou', '2025-08-20 00:00:00'),
(6, 'A06', '控制面板', 4, '安装', 'Valeo', '载荷保险系数+30%', NULL, 1, 'Ben Zou', '2025-08-20 00:00:00'),
(7, 'A07', 'CCD影像显示', 4, '安装', 'Valeo', 'Layout场地制约性确认', NULL, 1, 'Ben Zou', '2025-08-20 00:00:00'),
(8, 'A08', '扫描显示器', 5, '调试', 'Valeo', '成本制约性确认', NULL, 1, 'Ben Zou', '2025-08-20 00:00:00'),
(9, 'A09', 'MES数据处理', 5, '调试', 'Valeo', '硬件Recycle制约性确认', NULL, 1, 'Ben Zou', '2025-08-20 00:00:00');

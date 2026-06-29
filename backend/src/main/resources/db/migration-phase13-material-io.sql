CREATE TABLE IF NOT EXISTS material_io_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_ledger_id BIGINT NOT NULL COMMENT '物料台账 ID',
    io_type VARCHAR(8) NOT NULL COMMENT 'IN=入库 OUT=出库',
    quantity INT NOT NULL COMMENT '数量',
    remark VARCHAR(255) NULL COMMENT '备注',
    operator_user_id BIGINT NOT NULL COMMENT '操作人用户 ID',
    operated_at DATETIME NOT NULL COMMENT '操作时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_material_ledger_id (material_ledger_id),
    INDEX idx_io_type (io_type),
    INDEX idx_operated_at (operated_at),
    CONSTRAINT fk_material_io_ledger FOREIGN KEY (material_ledger_id) REFERENCES material_ledger (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO sys_menu (id, parent_id, menu_type, name, permission, path, icon, visible, sort_order) VALUES
(256, NULL, 'MENU', '物料出入库写', 'warehouse:material-io:write', NULL, NULL, 0, 256);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 256);

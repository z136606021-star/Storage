CREATE TABLE IF NOT EXISTS safety_stock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_ledger_id BIGINT NOT NULL COMMENT '物料台账 ID',
    safety_quantity INT NOT NULL DEFAULT 0 COMMENT '安全库存数',
    warning_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '预警开关',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_safety_stock_ledger (material_ledger_id),
    CONSTRAINT fk_safety_stock_ledger FOREIGN KEY (material_ledger_id)
        REFERENCES material_ledger (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO sys_menu (id, parent_id, menu_type, name, permission, path, icon, visible, sort_order) VALUES
(257, NULL, 'MENU', '安全库存写', 'warehouse:safety-stock:write', NULL, NULL, 0, 257);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 257);

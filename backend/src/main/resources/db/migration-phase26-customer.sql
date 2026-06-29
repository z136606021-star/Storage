CREATE TABLE IF NOT EXISTS sys_customer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_code VARCHAR(64) NOT NULL COMMENT '客户编号',
    name VARCHAR(128) NOT NULL COMMENT '客户名称',
    contact_name VARCHAR(64) NULL COMMENT '联系人',
    phone VARCHAR(32) NULL COMMENT '电话',
    email VARCHAR(128) NULL COMMENT '邮箱',
    address VARCHAR(255) NULL COMMENT '地址',
    remark VARCHAR(255) NULL COMMENT '备注',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_customer_code (customer_code),
    INDEX idx_sys_customer_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO sys_customer (id, customer_code, name, contact_name, phone, email, address, remark, status) VALUES
(1, 'CUST-001', '华东制造有限公司', '张经理', '13800001001', 'zhang@east-mfg.example', '上海市浦东新区示例路 1 号', '重点客户', 1),
(2, 'CUST-002', '南方自动化科技', '李工', '13800001002', 'li@south-auto.example', '深圳市南山区科技园', NULL, 1),
(3, 'CUST-003', '北方装备集团', '王主任', '13800001003', NULL, '北京市海淀区工业大道 88 号', '试用客户', 1);

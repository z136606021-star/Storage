CREATE DATABASE IF NOT EXISTS storage DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE storage;

SET NAMES utf8mb4;

DROP TABLE IF EXISTS sys_file;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS material_ledger;

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL COMMENT '登录账号（NTID）',
    password_hash VARCHAR(128) NOT NULL COMMENT 'BCrypt 密码哈希',
    display_name VARCHAR(64) NOT NULL COMMENT '显示名称',
    email VARCHAR(128) NULL COMMENT '邮箱',
    phone VARCHAR(32) NULL COMMENT '手机号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL COMMENT '角色编码',
    name VARCHAR(64) NOT NULL COMMENT '角色名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT NULL COMMENT '父菜单 ID',
    menu_type VARCHAR(16) NOT NULL DEFAULT 'MENU' COMMENT 'CATALOG/MENU',
    name VARCHAR(64) NOT NULL COMMENT '菜单名称',
    permission VARCHAR(128) NULL COMMENT '权限标识',
    path VARCHAR(128) NULL COMMENT '前端路由',
    icon VARCHAR(64) NULL COMMENT 'Ant Design 图标名',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '1显示 0隐藏',
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_permission (permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id),
    CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sys_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    object_key VARCHAR(255) NOT NULL COMMENT 'MinIO 对象键',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    content_type VARCHAR(128) NULL COMMENT 'MIME 类型',
    size_bytes BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小',
    uploader_id BIGINT NULL COMMENT '上传用户 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_object_key (object_key),
    CONSTRAINT fk_file_uploader FOREIGN KEY (uploader_id) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE warehouse_bin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bin_code VARCHAR(32) NOT NULL COMMENT 'Bin位编号（排-列-层）',
    row_no INT NOT NULL COMMENT '排',
    col_no INT NOT NULL COMMENT '列',
    level_no INT NOT NULL COMMENT '层',
    remark VARCHAR(255) NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_bin_code (bin_code),
    UNIQUE KEY uk_row_col_level (row_no, col_no, level_no),
    INDEX idx_bin_code (bin_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE warehouse_bom (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(64) NOT NULL COMMENT '品类',
    generic_name VARCHAR(64) NOT NULL COMMENT '统称',
    brand VARCHAR(64) NULL COMMENT '品牌',
    name VARCHAR(128) NOT NULL COMMENT '名称',
    remark VARCHAR(255) NULL COMMENT '备注',
    image_object_key VARCHAR(512) NULL COMMENT 'MinIO 对象键',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_generic_name (generic_name),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE material_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(64) NOT NULL COMMENT '品类',
    generic_name VARCHAR(64) NOT NULL COMMENT '统称',
    brand VARCHAR(64) NULL COMMENT '品牌',
    name VARCHAR(128) NOT NULL COMMENT '名称',
    model VARCHAR(64) NOT NULL COMMENT '型号',
    bin_location VARCHAR(32) NOT NULL COMMENT 'Bin位',
    stock_quantity INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    unit_price DECIMAL(10, 2) NULL COMMENT '单价',
    remark VARCHAR(255) NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_generic_name (generic_name),
    INDEX idx_bin_location (bin_location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE material_io_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_ledger_id BIGINT NOT NULL COMMENT '物料台账 ID',
    io_type VARCHAR(8) NOT NULL COMMENT 'IN=入库 OUT=出库',
    quantity INT NOT NULL COMMENT '数量',
    remark VARCHAR(255) NULL COMMENT '备注',
    purpose VARCHAR(32) NULL COMMENT '用途码',
    project_ref VARCHAR(128) NULL COMMENT '项目编号/名称（项目领用）',
    operator_user_id BIGINT NOT NULL COMMENT '操作人用户 ID',
    operated_at DATETIME NOT NULL COMMENT '操作时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_material_ledger_id (material_ledger_id),
    INDEX idx_io_type (io_type),
    INDEX idx_material_io_purpose (purpose),
    INDEX idx_material_io_project_ref (project_ref),
    INDEX idx_operated_at (operated_at),
    CONSTRAINT fk_material_io_ledger FOREIGN KEY (material_ledger_id) REFERENCES material_ledger (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE safety_stock (
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

CREATE TABLE sys_customer (
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

-- admin / admin123 (BCrypt cost 10)
INSERT INTO sys_user (username, password_hash, display_name, email, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ5C', '系统管理员', 'admin@example.com', 1);

INSERT INTO sys_role (code, name) VALUES
('ADMIN', '系统管理员'),
('USER', '普通用户');

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

INSERT INTO sys_menu (id, parent_id, menu_type, name, permission, path, icon, visible, sort_order) VALUES
(2, NULL, 'MENU', '物料台账写', 'warehouse:material-ledger:write', NULL, NULL, 0, 2),
(3, NULL, 'MENU', '文件上传', 'platform:file:upload', NULL, NULL, 0, 3),
(10, NULL, 'MENU', '个人中心', 'platform:personal:read', '/platform/personal', 'HomeOutlined', 1, 1),
(20, NULL, 'CATALOG', '项目管理中心', NULL, NULL, 'ProjectOutlined', 1, 2),
(21, 20, 'MENU', '新建项目', 'platform:project:create', '/platform/project/create', NULL, 1, 10),
(22, 20, 'MENU', '项目集', 'platform:project:set', '/platform/project/set', NULL, 1, 20),
(23, NULL, 'MENU', '项目中心读', 'platform:project:read', '/platform/project', NULL, 0, 23),
(100, NULL, 'CATALOG', '资源管理', NULL, NULL, 'DatabaseOutlined', 1, 10),
(110, 100, 'CATALOG', '仓库管理', NULL, NULL, NULL, 1, 10),
(111, 110, 'MENU', '物料台账', 'warehouse:material-ledger:read', '/warehouse/material-ledger', NULL, 1, 10),
(112, 110, 'MENU', '物料出入库', 'warehouse:material-io:read', '/warehouse/material-io', NULL, 1, 20),
(113, 110, 'MENU', '安全库存管理', 'warehouse:safety-stock:read', '/warehouse/safety-stock', NULL, 1, 30),
(117, 110, 'MENU', '库存统计', 'warehouse:stats:read', '/warehouse/inventory-stats', NULL, 1, 25),
(114, 110, 'CATALOG', '配置管理', NULL, NULL, NULL, 1, 40),
(115, 114, 'MENU', 'Bin位管理', 'warehouse:bin:read', '/warehouse/config/bin', NULL, 1, 10),
(116, 114, 'MENU', '物料清单管理', 'warehouse:bom:read', '/warehouse/config/bom', NULL, 1, 20),
(120, 100, 'CATALOG', '采购管理', NULL, NULL, NULL, 1, 5),
(121, 120, 'MENU', '采购管理', 'procurement:read', '/platform/procurement', NULL, 1, 10),
(122, 120, 'MENU', '新增采购需求', 'procurement:create', '/platform/procurement/create', NULL, 1, 20),
(123, 120, 'MENU', '我的采购需求', 'procurement:mine', '/platform/procurement/mine', NULL, 1, 30),
(150, NULL, 'MENU', '设计指引', 'platform:design:read', '/platform/design', 'BulbOutlined', 1, 3),
(160, NULL, 'CATALOG', '技能中心', NULL, NULL, 'ToolOutlined', 1, 4),
(161, 160, 'MENU', '技能矩阵', 'skill:matrix:read', '/platform/skill/matrix', NULL, 1, 10),
(162, 160, 'MENU', '人才画像', 'skill:talent:read', '/platform/skill/talent', NULL, 1, 20),
(163, 160, 'MENU', '人才培训计划', 'skill:training:read', '/platform/skill/training', NULL, 1, 30),
(170, NULL, 'MENU', '经验库', 'platform:experience:read', '/platform/experience', 'BookOutlined', 1, 5),
(180, NULL, 'CATALOG', '财务中心', NULL, NULL, 'ShoppingOutlined', 1, 6),
(181, 180, 'MENU', '业务分析看板', 'finance:dashboard:read', '/platform/finance/dashboard', NULL, 1, 10),
(182, 180, 'MENU', '财务结算中心', 'finance:settlement:read', '/platform/finance/settlement', NULL, 1, 20),
(183, 180, 'MENU', '成本分析中心', 'finance:cost:read', '/platform/finance/cost', NULL, 1, 30),
(200, NULL, 'CATALOG', '系统管理', NULL, NULL, 'SettingOutlined', 1, 90),
(201, 200, 'MENU', '用户管理', 'system:user:read', '/system/users', NULL, 1, 10),
(202, 200, 'MENU', '角色管理', 'system:role:read', '/system/roles', NULL, 0, 20),
(203, 200, 'MENU', '菜单管理', 'system:menu:read', '/system/menus', NULL, 0, 30),
(204, 200, 'MENU', '客户管理', 'system:customer:read', '/system/customers', NULL, 1, 20),
(214, NULL, 'MENU', '用户写', 'system:user:write', NULL, NULL, 0, 214),
(224, NULL, 'MENU', '角色写', 'system:role:write', NULL, NULL, 0, 224),
(234, NULL, 'MENU', '菜单写', 'system:menu:write', NULL, NULL, 0, 234),
(244, NULL, 'MENU', '客户写', 'system:customer:write', NULL, NULL, 0, 244),
(254, NULL, 'MENU', 'Bin位写', 'warehouse:bin:write', NULL, NULL, 0, 254),
(255, NULL, 'MENU', '物料清单写', 'warehouse:bom:write', NULL, NULL, 0, 255),
(256, NULL, 'MENU', '物料出入库写', 'warehouse:material-io:write', NULL, NULL, 0, 256),
(257, NULL, 'MENU', '安全库存写', 'warehouse:safety-stock:write', NULL, NULL, 0, 257);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 2), (1, 3),
(1, 10), (1, 20), (1, 21), (1, 22), (1, 23),
(1, 100), (1, 110), (1, 111), (1, 112), (1, 113), (1, 117), (1, 114), (1, 115), (1, 116),
(1, 120), (1, 121), (1, 122), (1, 123),
(1, 150), (1, 160), (1, 161), (1, 162), (1, 163),
(1, 170), (1, 180), (1, 181), (1, 182), (1, 183),
(1, 200), (1, 201), (1, 202), (1, 203), (1, 204),
(1, 214), (1, 224), (1, 234), (1, 244), (1, 254), (1, 255), (1, 256), (1, 257),
(2, 111);

INSERT INTO warehouse_bin (bin_code, row_no, col_no, level_no, remark) VALUES
('1-1-1', 1, 1, 1, NULL),
('1-1-4', 1, 1, 4, NULL),
('1-2-1', 1, 2, 1, NULL),
('1-2-2', 1, 2, 2, NULL),
('1-2-3', 1, 2, 3, NULL),
('1-3-1', 1, 3, 1, NULL);

INSERT INTO warehouse_bom (category, generic_name, brand, name, remark) VALUES
('气路配件', '气管接头', NULL, 'SL/L型节流阀', NULL),
('气路配件', '气管接头', NULL, 'SPA/直通节流阀', NULL),
('气路配件', '气管接头', NULL, 'SPP/四通节流阀', NULL),
('气路配件', '气管接头', NULL, 'PC型/直通', NULL),
('气路配件', '电磁阀', 'SMC', '二位五通', NULL),
('气路配件', '电磁阀', 'SMC', '三位五通', NULL),
('耗材', '生料带', '冰禹', '生料带', NULL),
('耗材', '密封圈', '三环', 'O型密封圈', NULL),
('耗材', '润滑油', '美孚', '液压油', NULL);

INSERT INTO material_ledger (category, generic_name, brand, name, model, bin_location, stock_quantity, unit_price, remark) VALUES
('气路配件', '气管接头', NULL, 'SL/L型节流阀', 'SL8-01', '1-1-1', 200, NULL, '机加工用'),
('气路配件', '气管接头', NULL, 'SL/L型节流阀', 'SL4-02', '1-1-1', 220, NULL, '机加工用'),
('气路配件', '气管接头', NULL, 'SPA/直通节流阀', 'SPA-6', '1-1-1', 99, NULL, '机加工用'),
('气路配件', '气管接头', NULL, 'SPP/四通节流阀', 'SPP-6', '1-1-1', 85, NULL, '机加工用'),
('气路配件', '气管接头', NULL, 'PC型/直通', 'PC8-02', '1-1-4', 22, NULL, NULL),
('气路配件', '电磁阀', 'SMC', '二位五通', 'SY3120', '1-3-1', 12, 320.00, NULL),
('气路配件', '电磁阀', 'SMC', '三位五通', 'SY3320', '1-3-1', 8, 380.00, NULL),
('耗材', '生料带', '冰禹', '生料带', '18*20', '1-1-4', 40, NULL, '甘工项目'),
('耗材', '生料带', '冰禹', '生料带', '12*15', '1-1-4', 55, NULL, '甘工项目'),
('耗材', '密封圈', '三环', 'O型密封圈', 'OR-10', '1-2-2', 500, 0.50, '通用备件'),
('耗材', '密封圈', '三环', 'O型密封圈', 'OR-12', '1-2-2', 420, 0.55, '通用备件'),
('耗材', '润滑油', '美孚', '液压油', 'HM-46', '1-2-3', 24, 128.00, '设备保养'),
('气路配件', '气管接头', NULL, 'SL/L型节流阀', 'SL6-01', '1-2-1', 120, NULL, '机加工用'),
('气路配件', '气管接头', NULL, 'SPA/直通节流阀', 'SPA-8', '1-2-1', 88, NULL, '机加工用'),
('耗材', '生料带', '冰禹', '生料带', '25*30', '1-1-4', 30, NULL, '甘工项目');

INSERT INTO safety_stock (material_ledger_id, safety_quantity, warning_enabled)
SELECT id, 30, 1 FROM material_ledger WHERE model = 'PC8-02' LIMIT 1;

INSERT INTO safety_stock (material_ledger_id, safety_quantity, warning_enabled)
SELECT id, 15, 1 FROM material_ledger WHERE model = 'SY3120' LIMIT 1;

INSERT INTO safety_stock (material_ledger_id, safety_quantity, warning_enabled)
SELECT id, 10, 1 FROM material_ledger WHERE model = 'SY3320' LIMIT 1;

DROP TABLE IF EXISTS safety_stock;
DROP TABLE IF EXISTS material_io_record;
DROP TABLE IF EXISTS material_ledger;
DROP TABLE IF EXISTS warehouse_bom_image;
DROP TABLE IF EXISTS warehouse_bom;
DROP TABLE IF EXISTS warehouse_bin;
DROP TABLE IF EXISTS design_guide;
DROP TABLE IF EXISTS design_stage;
DROP TABLE IF EXISTS design_product_type;
DROP TABLE IF EXISTS experience_attachment;
DROP TABLE IF EXISTS experience_project_link;
DROP TABLE IF EXISTS experience_record;
DROP TABLE IF EXISTS experience_type;
DROP TABLE IF EXISTS sys_customer;
DROP TABLE IF EXISTS password_reset_token;
DROP TABLE IF EXISTS jwt_revoked_token;
DROP TABLE IF EXISTS sys_file;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    email VARCHAR(128) NULL,
    phone VARCHAR(32) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (username)
);

CREATE TABLE password_reset_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
);

CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (code)
);

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_test_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_test_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE
);

CREATE TABLE sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT NULL,
    menu_type VARCHAR(16) NOT NULL DEFAULT 'MENU',
    name VARCHAR(64) NOT NULL,
    permission VARCHAR(128) NULL,
    path VARCHAR(128) NULL,
    component_key VARCHAR(128) NULL,
    icon VARCHAR(64) NULL,
    visible TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id),
    CONSTRAINT fk_test_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_test_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu (id) ON DELETE CASCADE
);

CREATE TABLE sys_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    object_key VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NULL,
    size_bytes BIGINT NOT NULL DEFAULT 0,
    uploader_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_file_uploader FOREIGN KEY (uploader_id) REFERENCES sys_user (id) ON DELETE SET NULL
);

CREATE TABLE jwt_revoked_token (
    jti VARCHAR(64) NOT NULL PRIMARY KEY,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_customer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    contact_name VARCHAR(64) NULL,
    phone VARCHAR(32) NULL,
    email VARCHAR(128) NULL,
    address VARCHAR(255) NULL,
    remark VARCHAR(255) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (customer_code)
);

CREATE TABLE experience_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (name)
);

CREATE TABLE experience_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_id BIGINT NOT NULL,
    description CLOB NOT NULL,
    impact CLOB NULL,
    suggestion CLOB NULL,
    action_plan CLOB NULL,
    recorder_user_id BIGINT NULL,
    recorder_name VARCHAR(64) NOT NULL,
    recorded_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_experience_record_type FOREIGN KEY (type_id) REFERENCES experience_type (id),
    CONSTRAINT fk_test_experience_record_user FOREIGN KEY (recorder_user_id) REFERENCES sys_user (id) ON DELETE SET NULL
);

CREATE TABLE experience_project_link (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id BIGINT NOT NULL,
    project_name VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_test_experience_project_record FOREIGN KEY (record_id) REFERENCES experience_record (id) ON DELETE CASCADE
);

CREATE TABLE experience_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NULL,
    size_bytes BIGINT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_test_experience_attachment_record FOREIGN KEY (record_id) REFERENCES experience_record (id) ON DELETE CASCADE,
    CONSTRAINT fk_test_experience_attachment_file FOREIGN KEY (file_id) REFERENCES sys_file (id)
);

CREATE TABLE warehouse_bin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bin_code VARCHAR(32) NOT NULL,
    row_no INT NOT NULL,
    col_no INT NULL,
    level_no INT NULL,
    remark VARCHAR(255) NULL,
    operator_user_id BIGINT NULL,
    operator_name VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (bin_code),
    CONSTRAINT fk_test_warehouse_bin_operator FOREIGN KEY (operator_user_id) REFERENCES sys_user (id) ON DELETE SET NULL
);

CREATE TABLE warehouse_bom (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(64) NOT NULL,
    generic_name VARCHAR(64) NOT NULL,
    brand VARCHAR(64) NULL,
    name VARCHAR(128) NOT NULL,
    model VARCHAR(64) NULL,
    remark VARCHAR(999) NULL,
    image_object_key VARCHAR(512) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE warehouse_bom_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bom_id BIGINT NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bom_id) REFERENCES warehouse_bom(id) ON DELETE CASCADE
);

CREATE TABLE material_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(64) NOT NULL,
    generic_name VARCHAR(64) NOT NULL,
    brand VARCHAR(64) NOT NULL DEFAULT '',
    name VARCHAR(128) NOT NULL,
    model VARCHAR(64) NOT NULL,
    bin_location VARCHAR(32) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    unit_price DECIMAL(10, 2) NULL,
    last_operated_at TIMESTAMP NULL,
    remark VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (category, generic_name, brand, name, model, bin_location)
);

CREATE TABLE material_io_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_ledger_id BIGINT NOT NULL,
    io_type VARCHAR(8) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NULL,
    remark VARCHAR(255) NULL,
    purpose VARCHAR(32) NULL,
    project_ref VARCHAR(128) NULL,
    operator_user_id BIGINT NOT NULL,
    operated_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_material_io_ledger FOREIGN KEY (material_ledger_id) REFERENCES material_ledger (id)
);

CREATE TABLE safety_stock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_ledger_id BIGINT NOT NULL,
    safety_quantity INT NOT NULL DEFAULT 0,
    warning_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (material_ledger_id),
    CONSTRAINT fk_safety_stock_ledger FOREIGN KEY (material_ledger_id) REFERENCES material_ledger (id) ON DELETE CASCADE
);

CREATE TABLE design_product_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_code VARCHAR(32) NOT NULL,
    type_name VARCHAR(64) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    operator_user_id BIGINT NULL,
    operator_name VARCHAR(64) NULL,
    operated_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (type_code),
    CONSTRAINT fk_test_design_product_type_operator FOREIGN KEY (operator_user_id) REFERENCES sys_user (id) ON DELETE SET NULL
);

CREATE TABLE design_stage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sort_order INT NOT NULL,
    stage_name VARCHAR(64) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    operator_user_id BIGINT NULL,
    operator_name VARCHAR(64) NULL,
    operated_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (sort_order),
    CONSTRAINT fk_test_design_stage_operator FOREIGN KEY (operator_user_id) REFERENCES sys_user (id) ON DELETE SET NULL
);

CREATE TABLE design_guide (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_type_id BIGINT NOT NULL,
    product_type_code VARCHAR(32) NOT NULL,
    product_type_name VARCHAR(64) NOT NULL,
    stage_id BIGINT NOT NULL,
    stage_name VARCHAR(64) NOT NULL,
    scope VARCHAR(64) NOT NULL,
    check_item VARCHAR(500) NOT NULL,
    remark VARCHAR(500) NULL,
    recorder_user_id BIGINT NULL,
    recorder_name VARCHAR(64) NULL,
    recorded_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (product_type_id, stage_id, scope, check_item),
    CONSTRAINT fk_test_design_guide_product_type FOREIGN KEY (product_type_id) REFERENCES design_product_type (id),
    CONSTRAINT fk_test_design_guide_stage FOREIGN KEY (stage_id) REFERENCES design_stage (id),
    CONSTRAINT fk_test_design_guide_recorder FOREIGN KEY (recorder_user_id) REFERENCES sys_user (id) ON DELETE SET NULL
);

INSERT INTO sys_user (id, username, password_hash, display_name, status) VALUES
(1, 'tester', 'hash', '测试员', 1);

INSERT INTO sys_role (id, code, name, status) VALUES
(1, 'ADMIN', '系统管理员', 1),
(2, 'USER', '普通用户', 1);

INSERT INTO sys_menu (id, parent_id, name, permission, path, component_key, icon, sort_order, visible) VALUES
(110, NULL, '仓库管理', NULL, NULL, NULL, 'InboxOutlined', 10, 1),
(111, 110, '物料台账', 'warehouse:material-ledger:read', '/warehouse/material-ledger', 'views/warehouse/MaterialLedgerView.vue', NULL, 10, 1),
(2, NULL, '物料台账写', 'warehouse:material-ledger:write', NULL, NULL, NULL, 11, 0),
(3, NULL, '菜单管理', 'system:menu:read', '/system/menus', 'components/system/MenuManagePanel.vue', NULL, 30, 0),
(4, NULL, '菜单写', 'system:menu:write', NULL, NULL, NULL, 31, 0),
(5, NULL, '项目中心读', 'platform:project:read', '/platform/project', 'views/platform/ShellPlaceholderView.vue', NULL, 40, 0),
(6, 110, '菜单隐藏子页', 'system:menu:child', 'child', 'views/platform/ShellPlaceholderView.vue', NULL, 32, 0),
(7, NULL, '经验库', 'platform:experience:read', '/platform/experience', 'views/experience/ExperienceLibraryView.vue', NULL, 50, 0),
(8, NULL, '经验库写', 'platform:experience:write', NULL, NULL, NULL, 51, 0),
(9, NULL, '文件上传', 'platform:file:upload', NULL, NULL, NULL, 52, 0),
(10, NULL, '设计指引', 'platform:design:read', '/platform/design', 'views/design/DesignGuideView.vue', NULL, 60, 0),
(11, NULL, '设计指引写', 'platform:design:write', NULL, NULL, NULL, 61, 0),
(200, NULL, '系统管理', NULL, NULL, NULL, 'SettingOutlined', 20, 1),
(201, 200, '用户管理', 'system:user:read', '/system/users', 'views/system/UserManageView.vue', NULL, 10, 1),
(202, 200, '角色管理', 'system:role:read', '/system/roles', 'components/system/RoleManagePanel.vue', NULL, 20, 1),
(203, 200, '菜单管理', 'system:menu:read', '/system/menus', 'components/system/MenuManagePanel.vue', NULL, 30, 1),
(204, 200, '客户管理', 'system:customer:read', '/system/customers', 'views/system/CustomerManageView.vue', NULL, 40, 1),
(214, NULL, '用户写', 'system:user:write', NULL, NULL, NULL, 214, 0),
(224, NULL, '角色写', 'system:role:write', NULL, NULL, NULL, 224, 0),
(234, NULL, '菜单写', 'system:menu:write', NULL, NULL, NULL, 234, 0),
(244, NULL, '客户写', 'system:customer:write', NULL, NULL, NULL, 244, 0);

UPDATE sys_menu SET menu_type = 'CATALOG' WHERE id IN (110, 200);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 110),
(1, 111),
(1, 2),
(1, 3),
(1, 4),
(1, 5),
(1, 6),
(1, 7),
(1, 8),
(1, 9),
(1, 10),
(1, 11),
(1, 200),
(1, 201),
(1, 202),
(1, 203),
(1, 204),
(1, 214),
(1, 224),
(1, 234),
(1, 244),
(2, 111);

INSERT INTO experience_type (id, name, status, sort_order) VALUES
(1, '设计经验', 1, 10),
(2, '制造问题', 1, 20),
(3, '客户需求变更', 1, 30);

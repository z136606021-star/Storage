DROP TABLE IF EXISTS safety_stock;
DROP TABLE IF EXISTS material_io_record;
DROP TABLE IF EXISTS material_ledger;
DROP TABLE IF EXISTS warehouse_bom;
DROP TABLE IF EXISTS warehouse_bin;
DROP TABLE IF EXISTS sys_customer;
DROP TABLE IF EXISTS password_reset_token;
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

CREATE TABLE warehouse_bin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bin_code VARCHAR(32) NOT NULL,
    row_no INT NOT NULL,
    col_no INT NOT NULL,
    level_no INT NOT NULL,
    remark VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (bin_code),
    UNIQUE (row_no, col_no, level_no)
);

CREATE TABLE warehouse_bom (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(64) NOT NULL,
    generic_name VARCHAR(64) NOT NULL,
    brand VARCHAR(64) NULL,
    name VARCHAR(128) NOT NULL,
    remark VARCHAR(255) NULL,
    image_object_key VARCHAR(512) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE material_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(64) NOT NULL,
    generic_name VARCHAR(64) NOT NULL,
    brand VARCHAR(64) NULL,
    name VARCHAR(128) NOT NULL,
    model VARCHAR(64) NOT NULL,
    bin_location VARCHAR(32) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    unit_price DECIMAL(10, 2) NULL,
    remark VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE material_io_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_ledger_id BIGINT NOT NULL,
    io_type VARCHAR(8) NOT NULL,
    quantity INT NOT NULL,
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

INSERT INTO sys_user (id, username, password_hash, display_name, status) VALUES
(1, 'tester', 'hash', '测试员', 1);

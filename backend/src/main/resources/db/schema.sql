CREATE DATABASE IF NOT EXISTS storage DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE storage;

DROP TABLE IF EXISTS material_ledger;

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

INSERT INTO material_ledger (category, generic_name, brand, name, model, bin_location, stock_quantity, unit_price, remark) VALUES
('气路配件', '气管接头', NULL, 'SL/L型节流阀', 'SL8-01', '1-1-1', 200, NULL, '机加工用'),
('气路配件', '气管接头', NULL, 'SL/L型节流阀', 'SL4-02', '1-1-1', 220, NULL, '机加工用'),
('气路配件', '气管接头', NULL, 'SPA/直通节流阀', 'SPA-6', '1-1-1', 99, NULL, '机加工用'),
('气路配件', '气管接头', NULL, 'SPP/四通节流阀', 'Ø4、6、8', '1-1-1', 39, NULL, '机加工用'),
('耗材', '生料带', '冰禹', '生料带', '18*20', '1-1-4', 40, NULL, '甘工项目'),
('气路配件', '气管接头', NULL, 'PC型/直通', 'PC8-02', '1-1-4', 22, NULL, NULL),
('气路配件', '气管接头', NULL, 'SPP/四通节流阀', 'SPP-6', '1-1-1', 85, NULL, '机加工用'),
('气路配件', '气管接头', NULL, 'SPP/四通节流阀', 'SPP-8', '1-1-1', 76, NULL, '机加工用'),
('气路配件', '气管接头', NULL, 'SPP/四通节流阀', 'SPP-10', '1-1-1', 64, NULL, '机加工用'),
('气路配件', '气管接头', NULL, 'SPP/四通节流阀', 'SPP-12', '1-1-1', 58, NULL, '机加工用'),
('气路配件', '气管接头', NULL, 'SPP/四通节流阀', 'SPP-14', '1-1-1', 45, NULL, '机加工用'),
('气路配件', '气管接头', NULL, 'SPP/四通节流阀', 'SPP-16', '1-1-1', 33, NULL, '机加工用'),
('气路配件', '气管接头', NULL, 'SPP/四通节流阀', 'SPP-18', '1-1-1', 28, NULL, '机加工用'),
('气路配件', '气管接头', NULL, 'PC型/直通', 'PC6-01', '1-1-4', 18, NULL, NULL),
('气路配件', '气管接头', NULL, 'PC型/直通', 'PC10-02', '1-1-4', 15, NULL, NULL),
('耗材', '生料带', '冰禹', '生料带', '12*15', '1-1-4', 55, NULL, '甘工项目'),
('耗材', '生料带', '冰禹', '生料带', '25*30', '1-1-4', 30, NULL, '甘工项目'),
('气路配件', '气管接头', NULL, 'SL/L型节流阀', 'SL6-01', '1-2-1', 120, NULL, '机加工用'),
('气路配件', '气管接头', NULL, 'SPA/直通节流阀', 'SPA-8', '1-2-1', 88, NULL, '机加工用'),
('气路配件', '气管接头', NULL, 'SPA/直通节流阀', 'SPA-10', '1-2-1', 72, NULL, '机加工用'),
('耗材', '密封圈', '三环', 'O型密封圈', 'OR-10', '1-2-2', 500, 0.50, '通用备件'),
('耗材', '密封圈', '三环', 'O型密封圈', 'OR-12', '1-2-2', 420, 0.55, '通用备件'),
('耗材', '润滑油', '美孚', '液压油', 'HM-46', '1-2-3', 24, 128.00, '设备保养'),
('气路配件', '电磁阀', 'SMC', '二位五通', 'SY3120', '1-3-1', 12, 320.00, NULL),
('气路配件', '电磁阀', 'SMC', '三位五通', 'SY3320', '1-3-1', 8, 380.00, NULL);

INSERT INTO material_ledger (category, generic_name, brand, name, model, bin_location, stock_quantity, unit_price, remark)
SELECT
    category,
    generic_name,
    brand,
    CONCAT(name, '-批次', n.n),
    model,
    bin_location,
    stock_quantity + (n.n % 10),
    unit_price,
    remark
FROM material_ledger
CROSS JOIN (
    SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
) AS n
WHERE id <= 25;

INSERT INTO material_ledger (category, generic_name, brand, name, model, bin_location, stock_quantity, unit_price, remark)
SELECT
    category,
    generic_name,
    brand,
    CONCAT(name, '-扩展', n.n),
    model,
    bin_location,
    stock_quantity,
    unit_price,
    remark
FROM material_ledger
CROSS JOIN (
    SELECT 10 AS n UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14
    UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19
) AS n
WHERE id <= 25;

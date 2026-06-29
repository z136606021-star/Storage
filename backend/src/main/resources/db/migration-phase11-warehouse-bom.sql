CREATE TABLE IF NOT EXISTS warehouse_bom (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(64) NOT NULL COMMENT '品类',
    generic_name VARCHAR(64) NOT NULL COMMENT '统称',
    brand VARCHAR(64) NULL COMMENT '品牌',
    name VARCHAR(128) NOT NULL COMMENT '名称',
    remark VARCHAR(255) NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_generic_name (generic_name),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO warehouse_bom (category, generic_name, brand, name, remark)
SELECT DISTINCT ml.category, ml.generic_name, ml.brand, ml.name, NULL
FROM material_ledger ml
WHERE NOT EXISTS (
    SELECT 1 FROM warehouse_bom wb
    WHERE wb.category = ml.category
      AND wb.generic_name = ml.generic_name
      AND wb.name = ml.name
      AND (wb.brand <=> ml.brand)
);
